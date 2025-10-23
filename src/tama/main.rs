use arboard::Clipboard;
use clap::{Parser, Subcommand};
use crossterm::event::{self, KeyCode};
use std::io;
use std::thread;
use std::time::Duration;

use tama::ascii_art_converter::AsciiArtSheet;
use tama::channel::{Channel, FeedItem, FeedManager};
use tama::client::{auth_config::AuthConfig, config::TamaConfig, ApiClient};
use tama::content_parser;
use tama::midi_composer::MidiEngine;
use tama::ui::{LoadingAnimation, RemoteAnimation, UI};

#[derive(Parser)]
#[command(name = "tama")]
#[command(about = "Tama TV client", long_about = None)]
struct Cli {
    #[arg(help = "Endpoint path (e.g., /content/2 or /channel/hiddenmugs)")]
    endpoint: Option<String>,

    #[command(subcommand)]
    command: Option<Commands>,
}

#[derive(Subcommand)]
enum Commands {
    #[command(about = "Authenticate (login or create account)")]
    Auth,
    #[command(about = "Upload content to your channel")]
    Upload { file_path: String },
    #[command(about = "Preview local content file")]
    Preview { file_path: String },
}

enum EndpointType {
    Content(i64),
    Channel(String),
}

fn add_protocol_if_missing(endpoint: &str) -> String {
    // If it already has a protocol, return as is
    if endpoint.starts_with("http://") || endpoint.starts_with("https://") {
        return endpoint.to_string();
    }

    // Check if it's a relative path (starts with /)
    if endpoint.starts_with('/') {
        return endpoint.to_string();
    }

    // Check if it's a relative path without leading slash (content/... or channel/...)
    if endpoint.starts_with("content/") || endpoint.starts_with("channel/") {
        return endpoint.to_string();
    }

    // At this point, it's likely a URL without protocol
    // Extract the host part (before the first /)
    let host = endpoint.split('/').next().unwrap_or(endpoint);

    // If host is localhost (or starts with localhost), use http, otherwise https
    let protocol = if host.starts_with("localhost") {
        "http://"
    } else {
        "https://"
    };

    format!("{protocol}{endpoint}")
}

fn parse_endpoint(endpoint: &str) -> Result<(Option<String>, EndpointType), String> {
    // Add protocol if missing
    let endpoint_with_protocol = add_protocol_if_missing(endpoint);

    // Check if it's a full URL
    if endpoint_with_protocol.starts_with("http://") || endpoint_with_protocol.starts_with("https://") {
        // Parse as full URL
        let url_parts: Vec<&str> = endpoint_with_protocol.splitn(4, '/').collect();

        if url_parts.len() < 4 {
            return Err(format!("Invalid URL format: {endpoint}"));
        }

        // url_parts[0] = "http:" or "https:"
        // url_parts[1] = ""
        // url_parts[2] = "domain:port"
        // url_parts[3] = "endpoint/path"

        let server_url = format!("{}//{}", url_parts[0], url_parts[2]);
        let path = url_parts[3];

        let parts: Vec<&str> = path.split('/').collect();

        if parts.len() != 2 {
            return Err(format!("Invalid endpoint path in URL: {path}. Expected content/<id> or channel/<identifier>"));
        }

        let endpoint_type = match parts[0] {
            "content" => {
                let content_id = parts[1].parse::<i64>()
                    .map_err(|_| format!("Invalid content ID: {}", parts[1]))?;
                EndpointType::Content(content_id)
            }
            "channel" => {
                EndpointType::Channel(parts[1].to_string())
            }
            _ => return Err(format!("Unknown endpoint type: {}. Expected 'content' or 'channel'", parts[0]))
        };

        Ok((Some(server_url), endpoint_type))
    } else {
        // Parse as relative path
        let parts: Vec<&str> = endpoint_with_protocol.trim_start_matches('/').split('/').collect();

        if parts.len() != 2 {
            return Err(format!("Invalid endpoint format: {endpoint}. Expected /content/<id> or /channel/<identifier>"));
        }

        let endpoint_type = match parts[0] {
            "content" => {
                let content_id = parts[1].parse::<i64>()
                    .map_err(|_| format!("Invalid content ID: {}", parts[1]))?;
                EndpointType::Content(content_id)
            }
            "channel" => {
                EndpointType::Channel(parts[1].to_string())
            }
            _ => return Err(format!("Unknown endpoint type: {}. Expected 'content' or 'channel'", parts[0]))
        };

        Ok((None, endpoint_type))
    }
}

enum PlayMode {
    Feed(FeedManager),
    Channel(FeedManager),
    SingleContent(Channel),
}

#[tokio::main]
async fn main() -> io::Result<()> {
    dotenvy::dotenv().ok();

    let cli = Cli::parse();

    let server_url = std::env::var("SERVER_URL")
        .unwrap_or_else(|_| "http://localhost:3000".to_string());

    // Handle non-UI commands first
    match &cli.command {
        Some(Commands::Auth) => {
            return handle_auth(&server_url).await;
        }
        Some(Commands::Upload { file_path }) => {
            return handle_upload(&server_url, file_path).await;
        }
        Some(Commands::Preview { file_path }) => {
            return handle_preview(file_path).await;
        }
        _ => {}
    }

    // UI commands below
    let mut midi_engine = MidiEngine::new(120)
        .map_err(io::Error::other)?;

    UI::initialize()?;

    let api_client = ApiClient::new(server_url.clone());

    let mut _config = if TamaConfig::config_exists() {
        TamaConfig::load().unwrap_or_else(|_| TamaConfig {
            server_url: server_url.clone(),
            servers: vec![],
            server_override: false,
        })
    } else {
        TamaConfig {
            server_url: server_url.clone(),
            servers: vec![],
            server_override: false,
        }
    };

    // Try to fetch and update server list (unless server_override is true)
    if !_config.server_override {
        if let Ok(servers) = api_client.fetch_servers().await {
            _config.servers = servers;
            _config.save().ok();
        } else if _config.servers.is_empty() {
            _config.servers = vec![server_url.clone()];
            _config.save().ok();
        }
    } else if _config.servers.is_empty() {
        // If server_override is true but no servers are configured, use the main server
        _config.servers = vec![server_url.clone()];
    }

    let play_mode = if let Some(endpoint_str) = &cli.endpoint {
        let (custom_server_url, endpoint) = parse_endpoint(endpoint_str).map_err(|e| {
            UI::cleanup().ok();
            io::Error::other(e)
        })?;

        // Use custom server URL if provided, otherwise use default
        let endpoint_server_url = custom_server_url.unwrap_or_else(|| server_url.clone());
        let endpoint_api_client = ApiClient::new(endpoint_server_url);

        match endpoint {
            EndpointType::Content(content_id) => {
                let mut loading_animation = LoadingAnimation::new(30, 9, 15.0);
                let loading_handle = tokio::spawn(async move {
                    loop {
                        loading_animation.update(0.1);
                        let loading_frame = loading_animation.get_frame();
                        if UI::display_channel_ascii("Loading...", &loading_frame, None).is_err() {
                            break;
                        }
                        tokio::time::sleep(Duration::from_millis(100)).await;
                    }
                });

                let content_result = endpoint_api_client.fetch_content(content_id).await;
                loading_handle.abort();

                match content_result {
                    Ok(content_data) => {
                        match Channel::new(
                            content_id,
                            "Single Content".to_string(),
                            content_data.art,
                            content_data.midi_composition,
                            content_data.fps,
                            content_data.id,
                        ) {
                            Ok(channel) => PlayMode::SingleContent(channel),
                            Err(e) => {
                                UI::cleanup()?;
                                return Err(io::Error::other(format!("Failed to create channel: {e}")));
                            }
                        }
                    }
                    Err(e) => {
                        UI::cleanup()?;
                        return Err(io::Error::other(format!("Failed to fetch content: {e}")));
                    }
                }
            }
            EndpointType::Channel(channel_identifier) => {
                let channel_result = endpoint_api_client.fetch_channel(&channel_identifier).await;

                match channel_result {
                    Ok(channel_response) => {
                        let items: Result<Vec<FeedItem>, String> = channel_response.contents
                            .into_iter()
                            .map(|content| {
                                let channel = Channel::new(
                                    channel_response.id,
                                    channel_response.name.clone(),
                                    content.art,
                                    content.midi_composition,
                                    content.fps,
                                    content.id,
                                )?;
                                Ok(FeedItem { channel })
                            })
                            .collect();

                        match items {
                            Ok(items) => PlayMode::Channel(FeedManager::new(items)),
                            Err(e) => {
                                UI::cleanup()?;
                                return Err(io::Error::other(format!("Failed to create channel items: {e}")));
                            }
                        }
                    }
                    Err(e) => {
                        UI::cleanup()?;
                        return Err(io::Error::other(format!("Failed to fetch channel: {e}")));
                    }
                }
            }
        }
    } else {
        // Fetch feed from all servers in parallel
        let servers = if !_config.servers.is_empty() {
            _config.servers.clone()
        } else {
            vec![server_url.clone()]
        };

        println!("Fetching feed from {} server(s)...", servers.len());

        let mut tasks = Vec::new();
        for server_url in servers {
            let server_url_clone = server_url.clone();
            let task = tokio::spawn(async move {
                let client = ApiClient::new(server_url_clone.clone());
                let result = client.fetch_feed().await;
                (server_url_clone, result)
            });
            tasks.push(task);
        }

        // Collect results with a timeout
        let timeout_duration = Duration::from_secs(5);
        let mut all_items: Vec<FeedItem> = Vec::new();
        let mut at_least_one_success = false;

        for task in tasks {
            match tokio::time::timeout(timeout_duration, task).await {
                Ok(Ok((server_url, Ok(feed_response)))) => {
                    println!("✓ Received {} items from {}", feed_response.len(), server_url);
                    let items: Result<Vec<_>, _> = feed_response
                        .into_iter()
                        .map(|item| FeedItem::from_api_feed_item_with_server(item, server_url.clone()))
                        .collect();

                    match items {
                        Ok(items) => {
                            all_items.extend(items);
                            at_least_one_success = true;
                        }
                        Err(e) => {
                            println!("✗ Failed to process items from {server_url}: {e}");
                        }
                    }
                }
                Ok(Ok((server_url, Err(e)))) => {
                    println!("✗ Failed to fetch from {server_url}: {e}");
                }
                Ok(Err(e)) => {
                    println!("✗ Task error: {e}");
                }
                Err(_) => {
                    println!("✗ Timeout waiting for server response");
                }
            }
        }

        if !at_least_one_success && all_items.is_empty() {
            UI::cleanup()?;
            return Err(io::Error::other("Failed to fetch feed from any server"));
        }

        println!("Total items collected: {}", all_items.len());
        PlayMode::Feed(FeedManager::new(all_items))
    };

    let (mut feed_manager, is_single_content) = match play_mode {
        PlayMode::Feed(manager) => (manager, false),
        PlayMode::Channel(manager) => (manager, false),
        PlayMode::SingleContent(channel) => {
            let items = vec![FeedItem { channel }];
            (FeedManager::new(items), true)
        }
    };

    let current_channel = feed_manager.current();
    midi_engine.parse_and_play_looping(&current_channel.content.midi_composition)
        .map_err(io::Error::other)?;

    let result = tv_loop(&mut feed_manager, &mut midi_engine, is_single_content);

    UI::cleanup()?;

    if let Err(e) = &result {
        eprintln!("Error: {e}");
    }

    result
}

fn tv_loop(
    feed_manager: &mut FeedManager,
    midi_engine: &mut MidiEngine,
    is_single_content: bool,
) -> io::Result<()> {
    let mut remote = RemoteAnimation::new();
    let mut last_update = std::time::Instant::now();

    loop {
        let now = std::time::Instant::now();
        let delta_time = now.duration_since(last_update).as_secs_f32();
        last_update = now;

        remote.update(delta_time);
        let direction = remote.should_switch_channel();

        if !is_single_content && !feed_manager.is_empty_state() && direction.is_some() {
                if direction == Some(0) {
                    feed_manager.previous();
                } else {
                    feed_manager.next();
                }

                let current_channel = feed_manager.current();
                midi_engine.parse_and_play_looping(&current_channel.content.midi_composition)
                    .map_err(io::Error::other)?;
            }

        let ascii_art = {
            let current_channel = feed_manager.current();
            current_channel.render(delta_time).to_string()
        };

        let title = "Tama Tv"; // feed_manager.current().name.clone();
        let channel_id = feed_manager.current().id;
        let content_id = feed_manager.current().content_id;
        let server_url = feed_manager.current().server_url.as_deref().unwrap_or("unknown");

        let remote_frame = remote.get_frame();
        UI::display_channel_ascii(title, &ascii_art, remote_frame)?;

        if event::poll(Duration::from_millis(100))? {
            if let crossterm::event::Event::Key(key_event) = event::read()? {
                match key_event.code {
                    KeyCode::Char('q') | KeyCode::Char('Q') => {
                        break;
                    }
                    KeyCode::Char('s') | KeyCode::Char('S') => {
                        if let Ok(clipboard) = &mut Clipboard::new() {
                            let text = format!("{server_url}/content/{content_id} {server_url}/channel/{channel_id}");
                            clipboard.set_text(text).unwrap();
                        };
                    }
                    KeyCode::Char('1') if !remote.is_playing() && !is_single_content => {
                        remote.trigger(0);
                    }
                    KeyCode::Char('2') if !remote.is_playing() && !is_single_content => {
                        remote.trigger(1);
                    }
                    _ => {}
                }
            }
        } else {
            thread::sleep(Duration::from_millis(50));
        }
    }

    Ok(())
}

async fn handle_auth(server_url: &str) -> io::Result<()> {
    println!("=== Tama Authentication ===\n");

    // Check if already authenticated
    if AuthConfig::auth_exists() {
        match AuthConfig::load() {
            Ok(auth) => {
                if auth.validate().is_ok() {
                    println!("✓ Already authenticated as: {}", auth.channel_name);
                    println!("  Channel ID: {}", auth.channel_id);
                    println!("\nDo you want to re-authenticate? (y/N): ");
                    io::Write::flush(&mut io::stdout())?;
                    let mut response = String::new();
                    io::stdin().read_line(&mut response)?;
                    let response = response.trim().to_lowercase();

                    if response != "y" && response != "yes" {
                        println!("\n✨ Authentication unchanged.");
                        return Ok(());
                    }
                    println!();
                }
            }
            Err(_) => {
                println!("⚠ Found invalid auth file, creating new authentication...\n");
            }
        }
    }

    print!("Channel Name (no spaces, max 250 chars): ");
    io::Write::flush(&mut io::stdout())?;
    let mut channel_name = String::new();
    io::stdin().read_line(&mut channel_name)?;
    let channel_name = channel_name.trim().to_string();

    if channel_name.is_empty() {
        return Err(io::Error::other("Channel name cannot be empty"));
    }

    if channel_name.contains(' ') {
        return Err(io::Error::other("Channel name cannot contain spaces"));
    }

    if channel_name.len() > 250 {
        return Err(io::Error::other("Channel name is too long (max 250 characters)"));
    }

    let password = rpassword::prompt_password("Password: ")
        .map_err(|e| io::Error::other(format!("Failed to read password: {e}")))?;

    if password.is_empty() {
        return Err(io::Error::other("Password cannot be empty"));
    }

    println!("\nAuthenticating with server...");
    let mut api_client = ApiClient::new(server_url.to_string());

    match api_client.login_or_signup(channel_name.clone(), password).await {
        Ok(response) => {
            println!("✓ Authentication successful!");
            println!("  Channel ID: {}", response.channel.id);
            println!("  Channel Name: {}", response.channel.name);

            let auth = AuthConfig {
                channel_id: response.channel.id,
                channel_name: response.channel.name.clone(),
                jwt_token: response.token,
            };

            auth.save()
                .map_err(|e| io::Error::other(format!("Failed to save auth: {e}")))?;

            println!("✓ Auth saved to {}", AuthConfig::default_auth_path().display());

            let servers = api_client.fetch_servers().await
                .unwrap_or_else(|_| vec![server_url.to_string()]);

            let config = TamaConfig {
                server_url: server_url.to_string(),
                servers,
                server_override: false,
            };

            config.save()
                .map_err(|e| io::Error::other(format!("Failed to save config: {e}")))?;

            println!("✓ Config saved to {}", TamaConfig::default_config_path().display());
            println!("\n✨ You're all set! You can now upload content with:");
            println!("   cargo run --bin tama upload <content-file.txt>");
            Ok(())
        }
        Err(e) => {
            Err(io::Error::other(format!("Authentication failed: {e}")))
        }
    }
}

async fn handle_upload(server_url: &str, file_path: &str) -> io::Result<()> {
    println!("=== Tama Content Upload ===\n");

    // Check auth
    if !AuthConfig::auth_exists() {
        println!("✗ No account found");
        println!("\n💡 Please create a channel with: cargo run --bin tama auth");
        return Ok(());
    }

    let auth = AuthConfig::load()
        .map_err(|e| io::Error::other(format!("Failed to load auth: {e}")))?;

    // Validate auth
    if let Err(e) = auth.validate() {
        println!("✗ Authentication error: {e}");
        println!("\n💡 Please review your settings with: cargo run --bin tama auth");
        return Ok(());
    }

    // Parse content file
    println!("Parsing content file: {file_path}");
    let content = content_parser::parse_content_file(file_path)
        .map_err(|e| io::Error::other(format!("Failed to parse content file: {e:?}")))?;

    println!("✓ Content file structure parsed successfully");
    println!("  MIDI composition length: {} chars", content.midi_composition.len());
    println!("  Art length: {} chars", content.art.len());
    println!("  FPS: {}", content.fps);

    // Validate MIDI composition
    println!("\nValidating MIDI composition...");
    let midi_engine = MidiEngine::new(120)
        .map_err(|e| io::Error::other(format!("Failed to initialize MIDI engine: {e}")))?;

    let midi_valid = match midi_engine.parse_notes(&content.midi_composition) {
        Ok(notes) => {
            println!("✓ MIDI composition is valid ({} notes)", notes.len());
            true
        }
        Err(e) => {
            println!("✗ MIDI composition has errors: {e}");
            false
        }
    };

    // Validate ASCII art
    println!("\nValidating ASCII art...");
    let art_valid = match AsciiArtSheet::from_string(&content.art) {
        Ok(sheet) => {
            println!("✓ ASCII art is valid ({}x{}, {} frames)",
                sheet.width, sheet.height, sheet.frames.len());
            true
        }
        Err(e) => {
            println!("✗ ASCII art has errors: {e}");
            false
        }
    };

    // Check if there were any validation errors
    if !midi_valid || !art_valid {
        println!("\n✗ Content validation failed!");
        if !midi_valid && !art_valid {
            println!("💡 Please review both the MIDI composition and ASCII art");
        } else if !midi_valid {
            println!("💡 Please review the MIDI composition");
        } else {
            println!("💡 Please review the ASCII art");
        }
        return Ok(());
    }

    println!("\n✓ All content validation passed!");

    println!("\nPreparing upload...");
    let api_client = ApiClient::with_session_token(server_url.to_string(), auth.jwt_token.clone());
    println!("✓ Using stored authentication for: {}", auth.channel_name);

    // Extract name from file path
    let content_name = std::path::Path::new(file_path)
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Untitled")
        .to_string();

    // Upload
    println!("\nUploading content...");
    match api_client.upload_content(
        auth.channel_id,
        content_name.clone(),
        content.art,
        content.midi_composition,
        content.fps,
    ).await {
        Ok(response) => {
            println!("✓ {}", response.message);
            println!("  Content ID: {}", response.id);
            println!("\n✨ Upload complete!");
            Ok(())
        }
        Err(e) => {
            println!("✗ Upload failed: {e}");
            println!("\n💡 Please review your settings with: cargo run --bin tama auth");
            Ok(())
        }
    }
}

async fn handle_preview(file_path: &str) -> io::Result<()> {
    println!("=== Preview Content ===\n");

    println!("Parsing content file: {file_path}");
    let content = content_parser::parse_content_file(file_path)
        .map_err(|e| io::Error::other(format!("Failed to parse content file: {e:?}")))?;

    println!("✓ Content file parsed successfully");

    let channel_name = std::path::Path::new(file_path)
        .file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Preview")
        .to_string();

    let mut midi_engine = MidiEngine::new(120)
        .map_err(io::Error::other)?;

    UI::initialize()?;

    let channel = Channel::new(
        0,
        channel_name.clone(),
        content.art,
        content.midi_composition.clone(),
        content.fps,
        0,
    ).map_err(|e| {
        UI::cleanup().ok();
        io::Error::other(e)
    })?;

    midi_engine.parse_and_play_looping(&content.midi_composition).map_err(|e| {
        UI::cleanup().ok();
        io::Error::other(e)
    })?;

    let items = vec![FeedItem { channel }];
    let mut feed_manager = FeedManager::new(items);

    let result = tv_loop(&mut feed_manager, &mut midi_engine, true);

    UI::cleanup()?;

    if let Err(e) = &result {
        eprintln!("Error: {e}");
    }

    result
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_endpoint_content_id() {
        let result = parse_endpoint("/content/123");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert!(server_url.is_none());
        match endpoint {
            EndpointType::Content(id) => assert_eq!(id, 123),
            _ => panic!("Expected Content endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_content_without_slash() {
        let result = parse_endpoint("content/456");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert!(server_url.is_none());
        match endpoint {
            EndpointType::Content(id) => assert_eq!(id, 456),
            _ => panic!("Expected Content endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_channel_name() {
        let result = parse_endpoint("/channel/hiddenmugs");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert!(server_url.is_none());
        match endpoint {
            EndpointType::Channel(name) => assert_eq!(name, "hiddenmugs"),
            _ => panic!("Expected Channel endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_channel_id() {
        let result = parse_endpoint("/channel/42");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert!(server_url.is_none());
        match endpoint {
            EndpointType::Channel(name) => assert_eq!(name, "42"),
            _ => panic!("Expected Channel endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_full_url_http() {
        let result = parse_endpoint("http://localhost:8080/content/99");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert_eq!(server_url, Some("http://localhost:8080".to_string()));
        match endpoint {
            EndpointType::Content(id) => assert_eq!(id, 99),
            _ => panic!("Expected Content endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_full_url_https() {
        let result = parse_endpoint("https://someotherdomain.com/channel/hiddenmugs");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert_eq!(server_url, Some("https://someotherdomain.com".to_string()));
        match endpoint {
            EndpointType::Channel(name) => assert_eq!(name, "hiddenmugs"),
            _ => panic!("Expected Channel endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_full_url_with_port() {
        let result = parse_endpoint("https://example.org:3000/content/42");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert_eq!(server_url, Some("https://example.org:3000".to_string()));
        match endpoint {
            EndpointType::Content(id) => assert_eq!(id, 42),
            _ => panic!("Expected Content endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_invalid_format() {
        let result = parse_endpoint("/invalid");
        assert!(result.is_err());

        let result = parse_endpoint("/content/123/extra");
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_endpoint_invalid_type() {
        let result = parse_endpoint("/unknown/123");
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_endpoint_invalid_content_id() {
        let result = parse_endpoint("/content/notanumber");
        assert!(result.is_err());
    }

    #[test]
    fn test_add_protocol_localhost() {
        assert_eq!(add_protocol_if_missing("localhost:3001/channel/1"), "http://localhost:3001/channel/1");
        assert_eq!(add_protocol_if_missing("localhost/content/2"), "http://localhost/content/2");
    }

    #[test]
    fn test_add_protocol_remote() {
        assert_eq!(add_protocol_if_missing("curzel.it/tama/channel/1"), "https://curzel.it/tama/channel/1");
        assert_eq!(add_protocol_if_missing("example.com/content/42"), "https://example.com/content/42");
    }

    #[test]
    fn test_add_protocol_already_present() {
        assert_eq!(add_protocol_if_missing("http://localhost:3000/channel/1"), "http://localhost:3000/channel/1");
        assert_eq!(add_protocol_if_missing("https://example.com/content/2"), "https://example.com/content/2");
    }

    #[test]
    fn test_add_protocol_relative_path() {
        assert_eq!(add_protocol_if_missing("/content/123"), "/content/123");
        assert_eq!(add_protocol_if_missing("/channel/test"), "/channel/test");
        assert_eq!(add_protocol_if_missing("content/456"), "content/456");
        assert_eq!(add_protocol_if_missing("channel/myname"), "channel/myname");
    }

    #[test]
    fn test_parse_endpoint_localhost_without_protocol() {
        let result = parse_endpoint("localhost:3001/channel/1");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert_eq!(server_url, Some("http://localhost:3001".to_string()));
        match endpoint {
            EndpointType::Channel(name) => assert_eq!(name, "1"),
            _ => panic!("Expected Channel endpoint"),
        }
    }

    #[test]
    fn test_parse_endpoint_remote_without_protocol() {
        let result = parse_endpoint("curzel.it/channel/test");
        assert!(result.is_ok());
        let (server_url, endpoint) = result.unwrap();
        assert_eq!(server_url, Some("https://curzel.it".to_string()));
        match endpoint {
            EndpointType::Channel(name) => assert_eq!(name, "test"),
            _ => panic!("Expected Channel endpoint"),
        }
    }
}

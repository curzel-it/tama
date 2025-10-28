use crate::{auth_endpoints, channel_endpoints, middleware, rate_limiter, AppState, DbPool};
use axum::{
    extract::{Path, Query, State},
    http::{StatusCode, header},
    middleware as axum_middleware,
    response::{Html, Json, Response},
    routing::{get, post},
    Router,
};
use r2d2_sqlite::SqliteConnectionManager;
use rusqlite::params;
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tower_http::{cors::CorsLayer, services::ServeDir, trace::TraceLayer};
use axum_server::tls_rustls::RustlsConfig;

#[derive(Serialize, Deserialize)]
pub struct ChannelInfo {
    pub id: i64,
    pub name: String,
}

#[derive(Serialize, Deserialize)]
pub struct FeedItem {
    pub channel: ChannelInfo,
    pub content: ContentData,
}

#[derive(Serialize, Deserialize)]
pub struct ContentData {
    pub id: i64,
    pub art: String,
    pub midi_composition: String,
    pub fps: f32,
}

#[derive(Serialize, Deserialize)]
pub struct ChannelResponse {
    pub id: i64,
    pub name: String,
    pub contents: Vec<ContentData>,
}

#[derive(Deserialize)]
pub struct PaginationParams {
    #[serde(default = "default_limit")]
    pub limit: i64,
    #[serde(default)]
    pub offset: i64,
}

#[derive(Serialize, Deserialize)]
pub struct PlatformVersions {
    pub ios: String,
    pub android: String,
    pub cli: String,
}

#[derive(Serialize, Deserialize)]
pub struct VersionResponse {
    pub minimum: PlatformVersions,
    pub latest: PlatformVersions,
}

#[derive(Deserialize)]
pub struct FeedParams {
    /// Comma-separated list of content IDs to prioritize at the front of the feed
    pub ids: Option<String>,
}

fn default_limit() -> i64 {
    50
}


pub fn initialize_database(db_path: &str) -> Result<DbPool, String> {
    let manager = SqliteConnectionManager::file(db_path);
    let pool = r2d2::Pool::builder()
        .max_size(10)
        .build(manager)
        .map_err(|e| format!("Failed to create connection pool: {e}"))?;

    // Get a connection from the pool to initialize the tables
    let conn = pool.get()
        .map_err(|e| format!("Failed to get connection from pool: {e}"))?;

    conn.execute(
        "CREATE TABLE IF NOT EXISTS channels (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            password_hash TEXT NOT NULL,
            created_at INTEGER NOT NULL
        )",
        [],
    )
    .map_err(|e| format!("Failed to create channels table: {e}"))?;

    conn.execute(
        "CREATE TABLE IF NOT EXISTS contents (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            channel_id INTEGER NOT NULL,
            name TEXT NOT NULL DEFAULT '',
            art TEXT NOT NULL,
            midi_composition TEXT NOT NULL,
            fps REAL NOT NULL,
            created_at INTEGER NOT NULL,
            FOREIGN KEY (channel_id) REFERENCES channels(id)
        )",
        [],
    )
    .map_err(|e| format!("Failed to create contents table: {e}"))?;

    conn.execute(
        "CREATE TABLE IF NOT EXISTS servers (
            server_url TEXT PRIMARY KEY
        )",
        [],
    )
    .map_err(|e| format!("Failed to create servers table: {e}"))?;

    // Create indexes for better query performance
    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_channels_name ON channels(name)",
        [],
    )
    .map_err(|e| format!("Failed to create index on channels.name: {e}"))?;

    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_contents_channel_id ON contents(channel_id)",
        [],
    )
    .map_err(|e| format!("Failed to create index on contents.channel_id: {e}"))?;

    conn.execute(
        "CREATE INDEX IF NOT EXISTS idx_contents_created_at ON contents(created_at)",
        [],
    )
    .map_err(|e| format!("Failed to create index on contents.created_at: {e}"))?;

    // Drop the connection back to the pool
    drop(conn);

    Ok(pool)
}

async fn get_feed(
    Query(params): Query<FeedParams>,
    State(state): State<AppState>,
) -> Result<Json<Vec<FeedItem>>, StatusCode> {
    println!("[GET /feed] Request received");
    let db = state.db.get()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let mut feed_items = Vec::new();

    // Parse priority IDs if provided
    let priority_ids: Vec<i64> = params.ids
        .as_ref()
        .map(|ids_str| {
            ids_str
                .split(',')
                .filter_map(|s| s.trim().parse::<i64>().ok())
                .collect()
        })
        .unwrap_or_default();

    // If priority IDs provided, fetch them first
    if !priority_ids.is_empty() {
        println!("[GET /feed] Priority IDs: {:?}", priority_ids);

        for id in &priority_ids {
            let result = db.query_row(
                "SELECT c.id, c.name, co.id, co.art, co.midi_composition, co.fps
                 FROM channels c
                 JOIN contents co ON c.id = co.channel_id
                 WHERE co.id = ?1",
                params![id],
                |row| {
                    Ok(FeedItem {
                        channel: ChannelInfo {
                            id: row.get(0)?,
                            name: row.get(1)?,
                        },
                        content: ContentData {
                            id: row.get(2)?,
                            art: row.get(3)?,
                            midi_composition: row.get(4)?,
                            fps: row.get(5)?,
                        },
                    })
                },
            );

            if let Ok(item) = result {
                feed_items.push(item);
            }
        }
    }

    // Calculate how many more items we need to reach limit of 30
    let remaining_limit = 30 - feed_items.len();

    if remaining_limit > 0 {
        // Build NOT IN clause if we have priority IDs
        let (not_in_clause, not_in_params): (String, Vec<i64>) = if !priority_ids.is_empty() {
            let placeholders = priority_ids.iter()
                .enumerate()
                .map(|(i, _)| format!("?{}", i + 1))
                .collect::<Vec<_>>()
                .join(", ");
            (format!("WHERE co.id NOT IN ({})", placeholders), priority_ids.clone())
        } else {
            (String::new(), Vec::new())
        };

        let query = format!(
            "SELECT c.id, c.name, co.id, co.art, co.midi_composition, co.fps
             FROM channels c
             JOIN contents co ON c.id = co.channel_id
             {}
             ORDER BY co.created_at DESC
             LIMIT ?{}",
            not_in_clause,
            not_in_params.len() + 1
        );

        let mut stmt = db.prepare(&query)
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

        // Build parameter list
        let mut query_params: Vec<Box<dyn rusqlite::ToSql>> = not_in_params
            .iter()
            .map(|id| Box::new(*id) as Box<dyn rusqlite::ToSql>)
            .collect();
        query_params.push(Box::new(remaining_limit as i64));

        let params_refs: Vec<&dyn rusqlite::ToSql> = query_params
            .iter()
            .map(|b| b.as_ref())
            .collect();

        let remaining_items = stmt
            .query_map(params_refs.as_slice(), |row| {
                Ok(FeedItem {
                    channel: ChannelInfo {
                        id: row.get(0)?,
                        name: row.get(1)?,
                    },
                    content: ContentData {
                        id: row.get(2)?,
                        art: row.get(3)?,
                        midi_composition: row.get(4)?,
                        fps: row.get(5)?,
                    },
                })
            })
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
            .collect::<Result<Vec<_>, _>>()
            .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

        feed_items.extend(remaining_items);
    }

    println!("[GET /feed] Returning {} content items", feed_items.len());
    Ok(Json(feed_items))
}

async fn get_channel(
    Path(channel_identifier): Path<String>,
    Query(pagination): Query<PaginationParams>,
    State(state): State<AppState>,
) -> Result<Json<ChannelResponse>, StatusCode> {
    println!("[GET /channel/{channel_identifier}] Request received");
    let db = state.db.get()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    // Validate pagination parameters
    let limit = pagination.limit.clamp(1, 100);
    let offset = pagination.offset.max(0);

    // Try to parse as integer ID first, otherwise treat as channel name
    let (id, name): (i64, String) = if let Ok(channel_id) = channel_identifier.parse::<i64>() {
        // Query by ID
        db.query_row(
            "SELECT id, name FROM channels WHERE id = ?1",
            params![channel_id],
            |row| Ok((row.get(0)?, row.get(1)?)),
        )
        .map_err(|_| StatusCode::NOT_FOUND)?
    } else {
        // Query by name
        db.query_row(
            "SELECT id, name FROM channels WHERE name = ?1",
            params![channel_identifier],
            |row| Ok((row.get(0)?, row.get(1)?)),
        )
        .map_err(|_| StatusCode::NOT_FOUND)?
    };

    let mut stmt = db
        .prepare(
            "SELECT id, art, midi_composition, fps
             FROM contents
             WHERE channel_id = ?1
             ORDER BY id
             LIMIT ?2 OFFSET ?3",
        )
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let contents = stmt
        .query_map(params![id, limit, offset], |row| {
            Ok(ContentData {
                id: row.get(0)?,
                art: row.get(1)?,
                midi_composition: row.get(2)?,
                fps: row.get(3)?,
            })
        })
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
        .collect::<Result<Vec<_>, _>>()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Ok(Json(ChannelResponse { id, name, contents }))
}

async fn get_content(
    Path(content_id): Path<i64>,
    State(state): State<AppState>,
) -> Result<Json<ContentData>, StatusCode> {
    println!("[GET /content/{content_id}] Request received");
    let db = state.db.get()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let content = db
        .query_row(
            "SELECT id, art, midi_composition, fps
             FROM contents
             WHERE id = ?1",
            params![content_id],
            |row| {
                Ok(ContentData {
                    id: row.get(0)?,
                    art: row.get(1)?,
                    midi_composition: row.get(2)?,
                    fps: row.get(3)?,
                })
            },
        )
        .map_err(|_| StatusCode::NOT_FOUND)?;

    Ok(Json(content))
}

async fn get_servers(State(state): State<AppState>) -> Result<Json<Vec<String>>, StatusCode> {
    println!("[GET /servers] Request received");
    let db = state.db.get()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let mut stmt = db
        .prepare("SELECT server_url FROM servers")
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let servers = stmt
        .query_map([], |row| row.get(0))
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
        .collect::<Result<Vec<String>, _>>()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    println!("[GET /servers] Returning {} servers", servers.len());
    Ok(Json(servers))
}

async fn get_versions() -> Json<VersionResponse> {
    // Return version requirements as a structured response
    // Update these values when you want to enforce version updates
    Json(VersionResponse {
        minimum: PlatformVersions {
            ios: "0.1.0".to_string(),
            android: "0.1.0".to_string(),
            cli: "0.1.0".to_string(),
        },
        latest: PlatformVersions {
            ios: "0.1.0".to_string(),
            android: "0.1.0".to_string(),
            cli: "0.1.0".to_string(),
        },
    })
}

async fn spa_fallback() -> Result<Html<String>, StatusCode> {
    // Serve index.html for SPA routes
    tokio::fs::read_to_string("static/index.html")
        .await
        .map(Html)
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)
}

async fn apple_app_site_association() -> Result<Response, StatusCode> {
    // Serve apple-app-site-association file as application/octet-stream
    let content = tokio::fs::read("static/.well-known/apple-app-site-association")
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Response::builder()
        .status(StatusCode::OK)
        .header(header::CONTENT_TYPE, "application/octet-stream")
        .body(axum::body::Body::from(content))
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)
}

async fn digital_asset_links() -> Result<Response, StatusCode> {
    // Serve assetlinks.json file as application/json
    let content = tokio::fs::read("static/.well-known/assetlinks.json")
        .await
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    Response::builder()
        .status(StatusCode::OK)
        .header(header::CONTENT_TYPE, "application/json")
        .body(axum::body::Body::from(content))
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)
}


async fn load_tls_config(cert_path: &str, key_path: &str) -> Result<RustlsConfig, String> {
    RustlsConfig::from_pem_file(cert_path, key_path)
        .await
        .map_err(|e| format!("Failed to load TLS certificates: {e}"))
}

async fn http_redirect_handler(
    axum::extract::Host(host): axum::extract::Host,
    uri: axum::http::Uri,
) -> axum::response::Redirect {
    let host_without_port = host.split(':').next().unwrap_or(&host);
    let https_uri = format!("https://{host_without_port}{uri}");
    axum::response::Redirect::permanent(&https_uri)
}

async fn run_http_redirect_server() {
    use axum::routing::any;

    let app = Router::new()
        .fallback(any(http_redirect_handler));

    let addr = "0.0.0.0:80";
    match tokio::net::TcpListener::bind(addr).await {
        Ok(listener) => {
            println!("HTTP redirect server listening on {addr} -> HTTPS");
            if let Err(e) = axum::serve(listener, app).await {
                eprintln!("HTTP redirect server error: {e}");
            }
        }
        Err(e) => {
            eprintln!("Failed to bind HTTP redirect server to {addr}: {e}");
            eprintln!("Continuing without HTTP redirect (HTTPS only)");
        }
    }
}

pub async fn run_server(db_path: &str, port: u16, jwt_secret: String) -> Result<(), String> {
    let pool = initialize_database(db_path)?;

    // Rate limiters with different limits for different endpoint types
    // Auth: 5 requests per minute (stricter to prevent brute force)
    let auth_rate_limiter = Arc::new(rate_limiter::RateLimiter::new(5, 60));
    // API: 100 requests per minute (normal usage)
    let api_rate_limiter = Arc::new(rate_limiter::RateLimiter::new(100, 60));
    // Upload: 20 requests per minute (to prevent spam)
    let upload_rate_limiter = Arc::new(rate_limiter::RateLimiter::new(20, 60));

    // Cleanup task for auth rate limiter
    let auth_rate_limiter_clone = auth_rate_limiter.clone();
    tokio::spawn(async move {
        loop {
            tokio::time::sleep(std::time::Duration::from_secs(300)).await;
            auth_rate_limiter_clone.cleanup_expired().await;
        }
    });

    // Cleanup task for API rate limiter
    let api_rate_limiter_clone = api_rate_limiter.clone();
    tokio::spawn(async move {
        loop {
            tokio::time::sleep(std::time::Duration::from_secs(300)).await;
            api_rate_limiter_clone.cleanup_expired().await;
        }
    });

    // Cleanup task for upload rate limiter
    let upload_rate_limiter_clone = upload_rate_limiter.clone();
    tokio::spawn(async move {
        loop {
            tokio::time::sleep(std::time::Duration::from_secs(300)).await;
            upload_rate_limiter_clone.cleanup_expired().await;
        }
    });

    let state = AppState {
        db: pool,
        jwt_secret,
        auth_rate_limiter,
        api_rate_limiter,
        upload_rate_limiter,
    };

    // Public API routes with standard rate limiting
    let public_routes = Router::new()
        .route("/feed", get(get_feed))
        .route("/channel/:channel_id", get(get_channel))
        .route("/content/:content_id", get(get_content))
        .route("/servers", get(get_servers))
        .route("/versions", get(get_versions))
        .route_layer(axum_middleware::from_fn_with_state(
            state.clone(),
            middleware::rate_limit_api,
        ));

    // Auth routes with strict rate limiting
    let auth_routes = Router::new()
        .route("/auth/register", post(auth_endpoints::register))
        .route("/auth/login", post(auth_endpoints::login))
        .route("/auth/login-or-signup", post(auth_endpoints::login_or_signup))
        .route_layer(axum_middleware::from_fn_with_state(
            state.clone(),
            middleware::rate_limit_auth,
        ));

    // Upload routes with upload rate limiting and size validation
    let upload_routes = Router::new()
        .route("/content", post(channel_endpoints::create_content))
        .with_state(state.clone())
        .route_layer(axum_middleware::from_fn(middleware::validate_content_size))
        .route_layer(axum_middleware::from_fn_with_state(
            state.clone(),
            middleware::rate_limit_upload,
        ));

    // SPA routes - serve index.html for client-side routing
    let spa_routes = Router::new()
        .route("/view/*path", get(spa_fallback));

    // Apple App Site Association routes for universal links
    let apple_routes = Router::new()
        .route("/.well-known/apple-app-site-association", get(apple_app_site_association))
        .route("/apple-app-site-association", get(apple_app_site_association));

    // Android Digital Asset Links routes for App Links
    let android_routes = Router::new()
        .route("/.well-known/assetlinks.json", get(digital_asset_links));

    // Static file serving for the web UI
    let static_service = ServeDir::new("static")
        .append_index_html_on_directories(true);

    let app = Router::new()
        .merge(public_routes)
        .merge(auth_routes)
        .merge(upload_routes)
        .merge(spa_routes)
        .merge(apple_routes)
        .merge(android_routes)
        .nest_service("/", static_service)
        .layer(CorsLayer::permissive())
        .layer(TraceLayer::new_for_http())
        .with_state(state);

    // Check for SSL/TLS configuration
    let ssl_cert_path = std::env::var("SSL_CERT_PATH").ok();
    let ssl_key_path = std::env::var("SSL_KEY_PATH").ok();

    match (ssl_cert_path, ssl_key_path) {
        (Some(cert_path), Some(key_path)) => {
            // HTTPS mode - load TLS configuration
            println!("Loading TLS certificates...");
            println!("  Certificate: {cert_path}");
            println!("  Private key: {key_path}");

            let tls_config = load_tls_config(&cert_path, &key_path).await?;

            let addr = format!("0.0.0.0:{port}");
            println!("HTTPS server listening on {addr}");

            // Spawn HTTP->HTTPS redirect server on port 80 if we're on port 443
            if port == 443 {
                tokio::spawn(run_http_redirect_server());
            }

            // Start HTTPS server
            axum_server::bind_rustls(addr.parse().unwrap(), tls_config)
                .serve(app.into_make_service_with_connect_info::<std::net::SocketAddr>())
                .await
                .map_err(|e| format!("HTTPS server error: {e}"))?;
        }
        _ => {
            // HTTP mode (development)
            println!("No SSL certificates found - running in HTTP mode (development only)");
            let addr = format!("0.0.0.0:{port}");
            let listener = tokio::net::TcpListener::bind(&addr)
                .await
                .map_err(|e| format!("Failed to bind to {addr}: {e}"))?;

            println!("HTTP server listening on {addr}");
            println!("WARNING: HTTP-only mode is NOT secure for production!");

            axum::serve(
                listener,
                app.into_make_service_with_connect_info::<std::net::SocketAddr>(),
            )
            .await
            .map_err(|e| format!("HTTP server error: {e}"))?;
        }
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_initialize_database() {
        let conn = initialize_database(":memory:");
        assert!(conn.is_ok());
    }

}

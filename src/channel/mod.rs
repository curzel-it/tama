mod content;
mod config;
mod feed;

pub use content::ChannelContent;
pub use config::{ChannelConfig, ContentConfig};
pub use feed::{FeedItem, FeedManager};

use crate::ui::AsciiArtPlayer;
use std::fs;

pub struct Channel {
    pub id: i64,
    pub name: String,
    pub player: AsciiArtPlayer,
    pub content: ChannelContent,
    pub content_id: i64,
    pub server_url: Option<String>,
}

impl Channel {
    pub fn new(id: i64, name: String, art: String, midi_composition: String, fps: f32, content_id: i64) -> Result<Self, String> {
        let content = ChannelContent::new(art.clone(), midi_composition);
        let player = AsciiArtPlayer::from_string(art, fps)?;

        Ok(Self {
            id,
            name,
            player,
            content,
            content_id,
            server_url: None,
        })
    }

    pub fn with_server_url(mut self, server_url: String) -> Self {
        self.server_url = Some(server_url);
        self
    }

    pub fn render(&mut self, delta_time: f32) -> &str {
        self.player.update(delta_time);
        self.player.current_frame()
    }
}

pub struct ChannelManager {
    channels: Vec<Channel>,
    current_channel: usize,
}

impl Default for ChannelManager {
    fn default() -> Self {
        Self::new()
    }
}

impl ChannelManager {
    pub fn new() -> Self {
        use crate::content_parser;

        let sleeping_content = fs::read_to_string("sprites/neko_sleep.txt")
            .expect("Failed to read sleeping cat sprite");
        let idle_content = fs::read_to_string("sprites/neko_idle.txt")
            .expect("Failed to read idle cat sprite");

        let sleeping_parsed = content_parser::parse_content(&sleeping_content)
            .expect("Failed to parse sleeping cat sprite");
        let idle_parsed = content_parser::parse_content(&idle_content)
            .expect("Failed to parse idle cat sprite");

        let channels = vec![
            Channel::new(
                -2,
                "Sleeping Cat".to_string(),
                sleeping_parsed.art,
                sleeping_parsed.midi_composition,
                sleeping_parsed.fps,
                -1,
            ).expect("Failed to load sleeping cat"),
            Channel::new(
                -3,
                "Idle Cat".to_string(),
                idle_parsed.art,
                idle_parsed.midi_composition,
                idle_parsed.fps,
                -1,
            ).expect("Failed to load idle cat"),
        ];

        Self {
            channels,
            current_channel: 0,
        }
    }

    pub fn switch_to(&mut self, channel_index: usize) {
        if channel_index < self.channels.len() {
            self.current_channel = channel_index;
        }
    }

    pub fn switch_to_id(&mut self, id: i64) -> bool {
        if let Some(idx) = self.channels.iter().position(|c| c.id == id) {
            self.current_channel = idx;
            true
        } else {
            false
        }
    }

    pub fn current(&mut self) -> &mut Channel {
        &mut self.channels[self.current_channel]
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_switch_to_id_valid() {
        let mut manager = ChannelManager::new();
        assert!(manager.switch_to_id(-3));
        assert_eq!(manager.current().id, -3);
    }

    #[test]
    fn test_switch_to_id_invalid() {
        let mut manager = ChannelManager::new();
        assert!(!manager.switch_to_id(999));
        assert_eq!(manager.current().id, -2);
    }
}

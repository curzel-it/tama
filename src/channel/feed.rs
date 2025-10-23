use crate::channel::Channel;
use crate::client::FeedItem as ApiFeedItem;

pub struct FeedItem {
    pub channel: Channel,
}

impl FeedItem {
    pub fn from_api_feed_item(api_item: ApiFeedItem) -> Result<Self, String> {
        let channel = Channel::new(
            api_item.channel.id,
            api_item.channel.name,
            api_item.content.art,
            api_item.content.midi_composition,
            api_item.content.fps,
            api_item.content.id,
        )?;

        Ok(Self { channel })
    }

    pub fn from_api_feed_item_with_server(api_item: ApiFeedItem, server_url: String) -> Result<Self, String> {
        let channel = Channel::new(
            api_item.channel.id,
            api_item.channel.name,
            api_item.content.art,
            api_item.content.midi_composition,
            api_item.content.fps,
            api_item.content.id,
        )?
        .with_server_url(server_url);

        Ok(Self { channel })
    }
}

pub struct FeedManager {
    items: Vec<FeedItem>,
    current_index: usize,
}

impl FeedManager {
    pub fn new(items: Vec<FeedItem>) -> Self {
        let items = if items.is_empty() {
            vec![Self::create_empty_state_item()]
        } else {
            items
        };

        Self {
            items,
            current_index: 0,
        }
    }

    fn create_empty_state_item() -> FeedItem {
        let static_art = std::fs::read_to_string("sprites/static.txt")
            .unwrap_or_else(|_| "Error loading static".to_string());

        let channel = Channel::new(
            -1,
            "No content".to_string(),
            static_art,
            "16-".to_string(),
            10.0,
            -1, // No content ID for empty state
        )
        .expect("Failed to create empty state channel");

        FeedItem { channel }
    }

    pub fn is_empty_state(&self) -> bool {
        self.items.len() == 1 && self.items[0].channel.id == -1
    }

    pub fn current(&mut self) -> &mut Channel {
        &mut self.items[self.current_index].channel
    }

    pub fn next(&mut self) {
        if !self.items.is_empty() {
            self.current_index = (self.current_index + 1) % self.items.len();
        }
    }

    pub fn previous(&mut self) {
        if !self.items.is_empty() {
            if self.current_index == 0 {
                self.current_index = self.items.len() - 1;
            } else {
                self.current_index -= 1;
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::content_parser;

    fn create_test_item(id: i64) -> FeedItem {
        let file_content = std::fs::read_to_string("sprites/neko_sleep.txt").unwrap();
        let parsed = content_parser::parse_content(&file_content).unwrap();
        let channel = Channel::new(
            id,
            format!("Test {id}"),
            parsed.art,
            parsed.midi_composition,
            parsed.fps,
            id,
        )
        .unwrap();

        FeedItem { channel }
    }

    #[test]
    fn test_feed_manager_navigation() {
        let items = vec![
            create_test_item(1),
            create_test_item(2),
            create_test_item(3),
        ];

        let mut manager = FeedManager::new(items);
        assert_eq!(manager.current().id, 1);

        manager.next();
        assert_eq!(manager.current().id, 2);

        manager.next();
        assert_eq!(manager.current().id, 3);

        manager.next();
        assert_eq!(manager.current().id, 1);
    }

    #[test]
    fn test_feed_manager_previous() {
        let items = vec![
            create_test_item(1),
            create_test_item(2),
            create_test_item(3),
        ];

        let mut manager = FeedManager::new(items);
        assert_eq!(manager.current().id, 1);

        manager.previous();
        assert_eq!(manager.current().id, 3);

        manager.previous();
        assert_eq!(manager.current().id, 2);
    }

    #[test]
    fn test_empty_feed_creates_default() {
        let mut manager = FeedManager::new(vec![]);
        assert!(manager.is_empty_state());
        assert_eq!(manager.current().id, -1);
        assert_eq!(manager.current().name, "No content");
    }

    #[test]
    fn test_empty_state_navigation_does_nothing() {
        let mut manager = FeedManager::new(vec![]);
        let initial_id = manager.current().id;

        manager.next();
        assert_eq!(manager.current().id, initial_id);

        manager.previous();
        assert_eq!(manager.current().id, initial_id);
    }
}

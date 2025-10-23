use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct ChannelContent {
    pub art: String,
    pub midi_composition: String,
}

impl ChannelContent {
    pub fn new(art: String, midi_composition: String) -> Self {
        Self {
            art,
            midi_composition,
        }
    }
}

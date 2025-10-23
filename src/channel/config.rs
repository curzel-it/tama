use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct ChannelConfig {
    pub id: String,
    pub name: String,
    pub animation_path: String,
    pub midi_composition: String,
    pub fps: f32,
}

#[derive(Serialize, Deserialize)]
pub struct ContentConfig {
    pub channels: Vec<ChannelConfig>,
}

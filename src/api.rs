use serde::{Deserialize, Serialize};

pub const HEADER_AUTH: &str = "authorization";

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct RegisterRequest {
    pub channel_name: String,
    pub password: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct LoginRequest {
    pub channel_name: String,
    pub password: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct CreateContentRequest {
    pub channel_id: i64,
    pub name: String,
    pub art: String,
    pub midi: String,
    pub fps: f32,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct ChannelInfo {
    pub id: i64,
    pub name: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct AuthResponse {
    pub token: String,
    pub expires_at: i64,
    pub channel: ChannelInfo,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct CreateContentResponse {
    pub id: i64,
    pub channel_id: i64,
    pub message: String,
}

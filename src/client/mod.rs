pub mod auth;
pub mod auth_config;
pub mod config;

use crate::api::{
    AuthResponse, CreateContentRequest, CreateContentResponse, LoginRequest, RegisterRequest,
};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ChannelInfo {
    pub id: i64,
    pub name: String,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct FeedItem {
    pub channel: ChannelInfo,
    pub content: ContentData,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ContentData {
    pub id: i64,
    pub art: String,
    pub midi_composition: String,
    pub fps: f32,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ChannelResponse {
    pub id: i64,
    pub name: String,
    pub contents: Vec<ContentData>,
}

pub struct ApiClient {
    base_url: String,
    session_token: Option<String>,
}

impl ApiClient {
    pub fn new(base_url: String) -> Self {
        Self {
            base_url,
            session_token: None,
        }
    }

    pub fn with_session_token(base_url: String, token: String) -> Self {
        Self {
            base_url,
            session_token: Some(token),
        }
    }

    pub fn set_session_token(&mut self, token: String) {
        self.session_token = Some(token);
    }

    async fn handle_response<T: serde::de::DeserializeOwned>(
        response: reqwest::Response,
    ) -> Result<T, String> {
        let status = response.status();

        if status.is_success() {
            response.json::<T>().await
                .map_err(|e| format!("Failed to parse response: {e}"))
        } else {
            let error_text = response.text().await
                .unwrap_or_else(|_| String::from("Unknown error"));

            let error_msg = match status.as_u16() {
                404 => format!("Channel not found - it may have been deleted or never existed. Server response: {error_text}"),
                401 => format!("Authentication failed - invalid credentials or signature. Server response: {error_text}"),
                403 => format!("Access forbidden. Server response: {error_text}"),
                400 => format!("Bad request. Server response: {error_text}"),
                500 => format!("Server error. Server response: {error_text}"),
                _ => format!("Request failed with status: {status}. Server response: {error_text}"),
            };

            Err(error_msg)
        }
    }

    pub async fn fetch_feed(&self) -> Result<Vec<FeedItem>, String> {
        let url = format!("{}/feed", self.base_url);

        let response = reqwest::get(&url).await
            .map_err(|e| format!("Failed to fetch feed: {e}"))?;

        Self::handle_response(response).await
    }

    pub async fn fetch_channel(&self, channel_identifier: &str) -> Result<ChannelResponse, String> {
        let url = format!("{}/channel/{}", self.base_url, channel_identifier);

        let response = reqwest::get(&url).await
            .map_err(|e| format!("Failed to fetch channel: {e}"))?;

        Self::handle_response(response).await
    }

    pub async fn fetch_channel_by_id(&self, channel_id: i64) -> Result<ChannelResponse, String> {
        self.fetch_channel(&channel_id.to_string()).await
    }

    pub async fn fetch_content(&self, content_id: i64) -> Result<ContentData, String> {
        let url = format!("{}/content/{}", self.base_url, content_id);

        let response = reqwest::get(&url).await
            .map_err(|e| format!("Failed to fetch content: {e}"))?;

        Self::handle_response(response).await
    }

    pub async fn register(
        &mut self,
        channel_name: String,
        password: String,
    ) -> Result<AuthResponse, String> {
        let url = format!("{}/auth/register", self.base_url);
        let request = RegisterRequest {
            channel_name,
            password,
        };

        let client = reqwest::Client::new();
        let http_response = client
            .post(&url)
            .json(&request)
            .send().await
            .map_err(|e| format!("Failed to register: {e}"))?;

        let response: AuthResponse = Self::handle_response(http_response).await?;
        self.session_token = Some(response.token.clone());
        Ok(response)
    }

    pub async fn login(
        &mut self,
        channel_name: String,
        password: String,
    ) -> Result<AuthResponse, String> {
        let url = format!("{}/auth/login", self.base_url);
        let request = LoginRequest {
            channel_name,
            password,
        };

        let client = reqwest::Client::new();
        let http_response = client
            .post(&url)
            .json(&request)
            .send().await
            .map_err(|e| format!("Failed to login: {e}"))?;

        let response: AuthResponse = Self::handle_response(http_response).await?;
        self.session_token = Some(response.token.clone());
        Ok(response)
    }

    pub async fn login_or_signup(
        &mut self,
        channel_name: String,
        password: String,
    ) -> Result<AuthResponse, String> {
        let url = format!("{}/auth/login-or-signup", self.base_url);
        let request = LoginRequest {
            channel_name,
            password,
        };

        let client = reqwest::Client::new();
        let http_response = client
            .post(&url)
            .json(&request)
            .send().await
            .map_err(|e| format!("Failed to authenticate: {e}"))?;

        let response: AuthResponse = Self::handle_response(http_response).await?;
        self.session_token = Some(response.token.clone());
        Ok(response)
    }

    pub async fn upload_content(
        &self,
        channel_id: i64,
        name: String,
        art: String,
        midi: String,
        fps: f32,
    ) -> Result<CreateContentResponse, String> {
        let url = format!("{}/content", self.base_url);
        let request = CreateContentRequest {
            channel_id,
            name,
            art,
            midi,
            fps,
        };

        let token = self.session_token.as_ref()
            .ok_or("No session token available. Please authenticate first.")?;

        let client = reqwest::Client::new();
        let response = client
            .post(&url)
            .header("Authorization", format!("Bearer {token}"))
            .json(&request)
            .send().await
            .map_err(|e| format!("Failed to upload content: {e}"))?;

        Self::handle_response(response).await
    }

    pub async fn fetch_servers(&self) -> Result<Vec<String>, String> {
        let url = format!("{}/servers", self.base_url);

        let response = reqwest::get(&url).await
            .map_err(|e| format!("Failed to fetch servers: {e}"))?;

        Self::handle_response(response).await
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_api_client_creation() {
        let client = ApiClient::new("http://localhost:3000".to_string());
        assert_eq!(client.base_url, "http://localhost:3000");
    }
}

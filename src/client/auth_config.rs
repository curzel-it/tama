use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AuthConfig {
    pub channel_id: i64,
    pub channel_name: String,
    pub jwt_token: String,
}

impl AuthConfig {
    pub fn default_auth_path() -> PathBuf {
        PathBuf::from(".").join("auth.json")
    }

    pub fn load() -> Result<Self, String> {
        let path = Self::default_auth_path();
        Self::load_from_path(&path)
    }

    pub fn load_from_path(path: &Path) -> Result<Self, String> {
        if !path.exists() {
            return Err("Auth file not found".to_string());
        }

        let content = fs::read_to_string(path)
            .map_err(|e| format!("Failed to read auth file: {e}"))?;

        let auth: AuthConfig = serde_json::from_str(&content)
            .map_err(|e| format!("Failed to parse auth file: {e}"))?;

        Ok(auth)
    }

    pub fn save(&self) -> Result<(), String> {
        let path = Self::default_auth_path();
        self.save_to_path(&path)
    }

    pub fn save_to_path(&self, path: &Path) -> Result<(), String> {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)
                .map_err(|e| format!("Failed to create auth directory: {e}"))?;
        }

        let json = serde_json::to_string_pretty(self)
            .map_err(|e| format!("Failed to serialize auth: {e}"))?;

        fs::write(path, json)
            .map_err(|e| format!("Failed to write auth file: {e}"))?;

        Ok(())
    }

    pub fn auth_exists() -> bool {
        Self::default_auth_path().exists()
    }

    pub fn delete() -> Result<(), String> {
        let path = Self::default_auth_path();
        if path.exists() {
            fs::remove_file(&path)
                .map_err(|e| format!("Failed to delete auth file: {e}"))?;
        }
        Ok(())
    }

    pub fn validate(&self) -> Result<(), String> {
        if self.channel_id == 0 {
            return Err("Channel ID is not set".to_string());
        }

        if self.channel_name.is_empty() {
            return Err("Channel name is empty".to_string());
        }

        if self.jwt_token.is_empty() {
            return Err("JWT token is empty".to_string());
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_auth_serialization() {
        let auth = AuthConfig {
            channel_id: 123,
            channel_name: "Test Channel".to_string(),
            jwt_token: "test_token".to_string(),
        };

        let json = serde_json::to_string(&auth).unwrap();
        let deserialized: AuthConfig = serde_json::from_str(&json).unwrap();

        assert_eq!(auth.channel_id, deserialized.channel_id);
        assert_eq!(auth.channel_name, deserialized.channel_name);
        assert_eq!(auth.jwt_token, deserialized.jwt_token);
    }

    #[test]
    fn test_validate_valid_auth() {
        let auth = AuthConfig {
            channel_id: 1,
            channel_name: "test".to_string(),
            jwt_token: "test_token".to_string(),
        };

        assert!(auth.validate().is_ok());
    }

    #[test]
    fn test_validate_empty_fields() {
        let auth = AuthConfig {
            channel_id: 0,
            channel_name: "test".to_string(),
            jwt_token: "test_token".to_string(),
        };

        assert!(auth.validate().is_err());

        let auth = AuthConfig {
            channel_id: 1,
            channel_name: String::new(),
            jwt_token: "test_token".to_string(),
        };

        assert!(auth.validate().is_err());

        let auth = AuthConfig {
            channel_id: 1,
            channel_name: "test".to_string(),
            jwt_token: String::new(),
        };

        assert!(auth.validate().is_err());
    }
}

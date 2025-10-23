use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct TamaConfig {
    pub server_url: String,
    #[serde(default)]
    pub servers: Vec<String>,
    #[serde(default)]
    pub server_override: bool,
}

impl TamaConfig {
    pub fn default_config_path() -> PathBuf {
        PathBuf::from(".").join("config.json")
    }

    pub fn default_keys_dir() -> PathBuf {
        PathBuf::from(".")
    }

    pub fn load() -> Result<Self, String> {
        let path = Self::default_config_path();
        Self::load_from_path(&path)
    }

    pub fn load_from_path(path: &Path) -> Result<Self, String> {
        if !path.exists() {
            return Err("Config file not found".to_string());
        }

        let content = fs::read_to_string(path)
            .map_err(|e| format!("Failed to read config file: {e}"))?;

        let config: TamaConfig = serde_json::from_str(&content)
            .map_err(|e| format!("Failed to parse config file: {e}"))?;

        Ok(config)
    }

    pub fn save(&self) -> Result<(), String> {
        let path = Self::default_config_path();
        self.save_to_path(&path)
    }

    pub fn save_to_path(&self, path: &Path) -> Result<(), String> {
        if let Some(parent) = path.parent() {
            fs::create_dir_all(parent)
                .map_err(|e| format!("Failed to create config directory: {e}"))?;
        }

        let json = serde_json::to_string_pretty(self)
            .map_err(|e| format!("Failed to serialize config: {e}"))?;

        fs::write(path, json)
            .map_err(|e| format!("Failed to write config file: {e}"))?;

        Ok(())
    }

    pub fn config_exists() -> bool {
        Self::default_config_path().exists()
    }

    pub fn validate(&self) -> Result<(), String> {
        if self.server_url.is_empty() {
            return Err("Server URL is empty".to_string());
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_config_serialization() {
        let config = TamaConfig {
            server_url: "http://localhost:3000".to_string(),
            servers: vec![],
            server_override: false,
        };

        let json = serde_json::to_string(&config).unwrap();
        let deserialized: TamaConfig = serde_json::from_str(&json).unwrap();

        assert_eq!(config.server_url, deserialized.server_url);
        assert_eq!(config.servers, deserialized.servers);
        assert_eq!(config.server_override, deserialized.server_override);
    }

    #[test]
    fn test_validate_valid_config() {
        let config = TamaConfig {
            server_url: "http://localhost:3000".to_string(),
            servers: vec![],
            server_override: false,
        };

        assert!(config.validate().is_ok());
    }

    #[test]
    fn test_validate_empty_fields() {
        let config = TamaConfig {
            server_url: String::new(),
            servers: vec![],
            server_override: false,
        };

        assert!(config.validate().is_err());
    }
}

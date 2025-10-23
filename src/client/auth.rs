use super::auth_config::AuthConfig;

pub fn store_auth(channel_id: i64, channel_name: String, token: String) -> Result<(), String> {
    let auth = AuthConfig {
        channel_id,
        channel_name,
        jwt_token: token,
    };
    auth.save()
}

pub fn clear_auth() -> Result<(), String> {
    AuthConfig::delete()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_store_auth() {
        let auth = AuthConfig {
            channel_id: 1,
            channel_name: "test".to_string(),
            jwt_token: "test_token".to_string(),
        };

        assert_eq!(auth.channel_id, 1);
        assert_eq!(auth.channel_name, "test");
        assert_eq!(auth.jwt_token, "test_token");
    }

    #[test]
    fn test_auth_validation() {
        let auth = AuthConfig {
            channel_id: 1,
            channel_name: "test".to_string(),
            jwt_token: "test_token".to_string(),
        };

        assert!(auth.validate().is_ok());
    }
}

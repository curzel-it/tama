use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    pub sub: String,
    pub channel_name: String,
    pub exp: usize,
    pub iat: usize,
}

const TOKEN_EXPIRATION_DAYS: u64 = 30;

pub fn create_jwt(channel_id: i64, channel_name: &str, secret: &str) -> Result<String, String> {
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_err(|e| format!("System time error: {e}"))?
        .as_secs();

    let expiration = now + (TOKEN_EXPIRATION_DAYS * 24 * 60 * 60);

    let claims = Claims {
        sub: channel_id.to_string(),
        channel_name: channel_name.to_string(),
        exp: expiration as usize,
        iat: now as usize,
    };

    encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )
    .map_err(|e| format!("Failed to create JWT: {e}"))
}

pub fn verify_jwt(token: &str, secret: &str) -> Result<Claims, String> {
    let validation = Validation::default();

    decode::<Claims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &validation,
    )
    .map(|data| data.claims)
    .map_err(|e| format!("Failed to verify JWT: {e}"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_and_verify_jwt() {
        let secret = "test-secret";
        let channel_id = 123;
        let channel_name = "testchannel";

        let token = create_jwt(channel_id, channel_name, secret).unwrap();
        assert!(!token.is_empty());

        let claims = verify_jwt(&token, secret).unwrap();
        assert_eq!(claims.sub, "123");
        assert_eq!(claims.channel_name, "testchannel");
    }

    #[test]
    fn test_verify_jwt_with_wrong_secret() {
        let secret = "test-secret";
        let wrong_secret = "wrong-secret";
        let channel_id = 123;
        let channel_name = "testchannel";

        let token = create_jwt(channel_id, channel_name, secret).unwrap();
        let result = verify_jwt(&token, wrong_secret);

        assert!(result.is_err());
    }

    #[test]
    fn test_verify_invalid_jwt() {
        let secret = "test-secret";
        let invalid_token = "invalid.jwt.token";

        let result = verify_jwt(invalid_token, secret);
        assert!(result.is_err());
    }

    #[test]
    fn test_jwt_contains_correct_expiration() {
        let secret = "test-secret";
        let channel_id = 123;
        let channel_name = "testchannel";

        let token = create_jwt(channel_id, channel_name, secret).unwrap();
        let claims = verify_jwt(&token, secret).unwrap();

        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs() as usize;

        assert!(claims.exp > now);
        assert!(claims.iat <= now + 1);
    }
}

use axum::http::{HeaderMap, StatusCode};

pub fn authenticate_request(
    headers: &HeaderMap,
    jwt_secret: &str,
) -> Result<i64, StatusCode> {
    let token = headers
        .get(tama::api::HEADER_AUTH)
        .and_then(|h| h.to_str().ok())
        .and_then(|s| s.strip_prefix("Bearer "))
        .ok_or(StatusCode::UNAUTHORIZED)?;

    let claims = crate::jwt::verify_jwt(token, jwt_secret)
        .map_err(|_| StatusCode::UNAUTHORIZED)?;

    let channel_id = claims.sub.parse::<i64>()
        .map_err(|_| StatusCode::UNAUTHORIZED)?;

    Ok(channel_id)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jwt;

    #[test]
    fn test_authenticate_request_with_valid_jwt() {
        let secret = "test-secret";
        let channel_id = 123;
        let channel_name = "testchannel";

        let token = jwt::create_jwt(channel_id, channel_name, secret).unwrap();
        let mut headers = HeaderMap::new();
        headers.insert(
            tama::api::HEADER_AUTH,
            format!("Bearer {}", token).parse().unwrap(),
        );

        let result = authenticate_request(&headers, secret);
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), 123);
    }

    #[test]
    fn test_authenticate_request_with_invalid_jwt() {
        let secret = "test-secret";
        let mut headers = HeaderMap::new();
        headers.insert(
            tama::api::HEADER_AUTH,
            "Bearer invalid.jwt.token".parse().unwrap(),
        );

        let result = authenticate_request(&headers, secret);
        assert!(result.is_err());
        assert_eq!(result.unwrap_err(), StatusCode::UNAUTHORIZED);
    }

    #[test]
    fn test_authenticate_request_without_bearer_prefix() {
        let secret = "test-secret";
        let mut headers = HeaderMap::new();
        headers.insert(
            tama::api::HEADER_AUTH,
            "some_token".parse().unwrap(),
        );

        let result = authenticate_request(&headers, secret);
        assert!(result.is_err());
        assert_eq!(result.unwrap_err(), StatusCode::UNAUTHORIZED);
    }

    #[test]
    fn test_authenticate_request_without_auth_header() {
        let secret = "test-secret";
        let headers = HeaderMap::new();

        let result = authenticate_request(&headers, secret);
        assert!(result.is_err());
        assert_eq!(result.unwrap_err(), StatusCode::UNAUTHORIZED);
    }
}

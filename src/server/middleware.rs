use axum::{
    extract::{ConnectInfo, Request, State},
    http::StatusCode,
    middleware::Next,
    response::Response,
};
use std::net::SocketAddr;

use crate::AppState;

pub async fn rate_limit_auth(
    State(state): State<AppState>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    match state.auth_rate_limiter.check(addr.ip()).await {
        Ok(()) => Ok(next.run(request).await),
        Err(_retry_after) => {
            tracing::warn!("Rate limit exceeded for IP {} on auth endpoint", addr.ip());
            Err(StatusCode::TOO_MANY_REQUESTS)
        }
    }
}

pub async fn rate_limit_api(
    State(state): State<AppState>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    match state.api_rate_limiter.check(addr.ip()).await {
        Ok(()) => Ok(next.run(request).await),
        Err(_retry_after) => {
            tracing::warn!("Rate limit exceeded for IP {} on API endpoint", addr.ip());
            Err(StatusCode::TOO_MANY_REQUESTS)
        }
    }
}

pub async fn rate_limit_upload(
    State(state): State<AppState>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    match state.upload_rate_limiter.check(addr.ip()).await {
        Ok(()) => Ok(next.run(request).await),
        Err(_retry_after) => {
            tracing::warn!("Rate limit exceeded for IP {} on upload endpoint", addr.ip());
            Err(StatusCode::TOO_MANY_REQUESTS)
        }
    }
}

pub async fn validate_content_size(
    request: Request,
    next: Next,
) -> Result<Response, StatusCode> {
    const MAX_CONTENT_LENGTH: usize = 10 * 1024 * 1024; // 10 MB

    if let Some(content_length) = request.headers().get("content-length") {
        if let Ok(length_str) = content_length.to_str() {
            if let Ok(length) = length_str.parse::<usize>() {
                if length > MAX_CONTENT_LENGTH {
                    tracing::warn!("Request rejected: content-length {} exceeds max {}", length, MAX_CONTENT_LENGTH);
                    return Err(StatusCode::PAYLOAD_TOO_LARGE);
                }
            }
        }
    }

    Ok(next.run(request).await)
}

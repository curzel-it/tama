use axum::{
    extract::{Json, State},
    http::StatusCode,
};
use rusqlite::params;
use std::time::{SystemTime, UNIX_EPOCH};

use crate::AppState;
use tama::api::{AuthResponse, ChannelInfo, LoginRequest, RegisterRequest};

pub async fn register(
    State(state): State<AppState>,
    Json(request): Json<RegisterRequest>,
) -> Result<Json<AuthResponse>, StatusCode> {
    let channel_name = request.channel_name.trim().to_lowercase();

    if channel_name.is_empty() || channel_name.len() > 250 {
        return Err(StatusCode::BAD_REQUEST);
    }

    if channel_name.contains(char::is_whitespace) {
        return Err(StatusCode::BAD_REQUEST);
    }

    let password_hash = crate::password::hash_password(&request.password)
        .map_err(|e| {
            tracing::error!("Failed to hash password: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    let db = state.db.get()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let channel_exists: Result<i64, _> = db.query_row(
        "SELECT id FROM channels WHERE name = ?1",
        params![channel_name],
        |row| row.get(0),
    );

    if channel_exists.is_ok() {
        tracing::warn!("Channel registration failed: channel '{}' already exists", channel_name);
        return Err(StatusCode::CONFLICT);
    }

    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs() as i64;

    db.execute(
        "INSERT INTO channels (name, password_hash, created_at) VALUES (?1, ?2, ?3)",
        params![channel_name, password_hash, now],
    )
    .map_err(|e| {
        tracing::error!("Failed to insert channel: {}", e);
        StatusCode::INTERNAL_SERVER_ERROR
    })?;

    let channel_id = db.last_insert_rowid();

    let token = crate::jwt::create_jwt(channel_id, &channel_name, &state.jwt_secret)
        .map_err(|e| {
            tracing::error!("Failed to create JWT: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    let expires_at = now + (30 * 24 * 60 * 60);

    tracing::info!("Channel registered: id={}, name={}", channel_id, channel_name);

    Ok(Json(AuthResponse {
        token,
        expires_at,
        channel: ChannelInfo {
            id: channel_id,
            name: channel_name,
        },
    }))
}

pub async fn login(
    State(state): State<AppState>,
    Json(request): Json<LoginRequest>,
) -> Result<Json<AuthResponse>, StatusCode> {
    let channel_name = request.channel_name.trim().to_lowercase();

    let db = state.db.get()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let channel_record: Result<(i64, String, String), _> = db.query_row(
        "SELECT id, name, password_hash FROM channels WHERE name = ?1",
        params![channel_name],
        |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)),
    );

    let (channel_id, channel_name, password_hash) = channel_record.map_err(|_| {
        tracing::warn!("Login failed: channel '{}' not found", channel_name);
        StatusCode::UNAUTHORIZED
    })?;

    let password_valid = crate::password::verify_password(&request.password, &password_hash)
        .map_err(|e| {
            tracing::error!("Password verification error: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    if !password_valid {
        tracing::warn!("Login failed: invalid password for channel '{}'", channel_name);
        return Err(StatusCode::UNAUTHORIZED);
    }

    let token = crate::jwt::create_jwt(channel_id, &channel_name, &state.jwt_secret)
        .map_err(|e| {
            tracing::error!("Failed to create JWT: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs() as i64;
    let expires_at = now + (30 * 24 * 60 * 60);

    tracing::info!("Channel logged in: id={}, name={}", channel_id, channel_name);

    Ok(Json(AuthResponse {
        token,
        expires_at,
        channel: ChannelInfo {
            id: channel_id,
            name: channel_name,
        },
    }))
}

pub async fn login_or_signup(
    State(state): State<AppState>,
    Json(request): Json<LoginRequest>,
) -> Result<Json<AuthResponse>, StatusCode> {
    let channel_name = request.channel_name.trim().to_lowercase();

    if channel_name.is_empty() || channel_name.len() > 250 {
        return Err(StatusCode::BAD_REQUEST);
    }

    if channel_name.contains(char::is_whitespace) {
        return Err(StatusCode::BAD_REQUEST);
    }

    let db = state.db.get()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    let channel_record: Result<(i64, String, String), _> = db.query_row(
        "SELECT id, name, password_hash FROM channels WHERE name = ?1",
        params![channel_name],
        |row| Ok((row.get(0)?, row.get(1)?, row.get(2)?)),
    );

    let (channel_id, channel_name) = match channel_record {
        Ok((id, name, password_hash)) => {
            let password_valid = crate::password::verify_password(&request.password, &password_hash)
                .map_err(|e| {
                    tracing::error!("Password verification error: {}", e);
                    StatusCode::INTERNAL_SERVER_ERROR
                })?;

            if !password_valid {
                tracing::warn!("Login failed: invalid password for channel '{}'", name);
                return Err(StatusCode::UNAUTHORIZED);
            }

            tracing::info!("Channel logged in: id={}, name={}", id, name);
            (id, name)
        }
        Err(_) => {
            let password_hash = crate::password::hash_password(&request.password)
                .map_err(|e| {
                    tracing::error!("Failed to hash password: {}", e);
                    StatusCode::INTERNAL_SERVER_ERROR
                })?;

            let now = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs() as i64;

            db.execute(
                "INSERT INTO channels (name, password_hash, created_at) VALUES (?1, ?2, ?3)",
                params![channel_name, password_hash, now],
            )
            .map_err(|e| {
                tracing::error!("Failed to insert channel: {}", e);
                StatusCode::INTERNAL_SERVER_ERROR
            })?;

            let channel_id = db.last_insert_rowid();

            tracing::info!("Channel registered: id={}, name={}", channel_id, channel_name);
            (channel_id, channel_name)
        }
    };

    let token = crate::jwt::create_jwt(channel_id, &channel_name, &state.jwt_secret)
        .map_err(|e| {
            tracing::error!("Failed to create JWT: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs() as i64;
    let expires_at = now + (30 * 24 * 60 * 60);

    Ok(Json(AuthResponse {
        token,
        expires_at,
        channel: ChannelInfo {
            id: channel_id,
            name: channel_name,
        },
    }))
}

use axum::{
    extract::{Json, State},
    http::{HeaderMap, StatusCode},
};
use rusqlite::params;

use crate::{auth, AppState};
use tama::api::{CreateContentRequest, CreateContentResponse};

const MAX_CONTENT_NAME_LENGTH: usize = 200;
const MAX_ART_SIZE: usize = 100_000; // 100KB
const MAX_MIDI_SIZE: usize = 50_000; // 50KB
const MIN_FPS: f32 = 0.1;
const MAX_FPS: f32 = 120.0;

fn validate_content_upload(request: &CreateContentRequest) -> Result<(), &'static str> {
    // Validate content name
    if request.name.len() > MAX_CONTENT_NAME_LENGTH {
        return Err("Content name is too long");
    }

    // Validate FPS range
    if request.fps < MIN_FPS || request.fps > MAX_FPS {
        return Err("FPS must be between 0.1 and 120.0");
    }

    // Validate art size
    if request.art.len() > MAX_ART_SIZE {
        return Err("Art content is too large (max 100KB)");
    }

    if request.art.trim().is_empty() {
        return Err("Art content cannot be empty");
    }

    // Validate MIDI composition size
    if request.midi.len() > MAX_MIDI_SIZE {
        return Err("MIDI composition is too large (max 50KB)");
    }

    if request.midi.trim().is_empty() {
        return Err("MIDI composition cannot be empty");
    }

    // Validate that MIDI can be parsed (without requiring audio output)
    use tama::midi_composer::MidiEngine;
    MidiEngine::validate_midi_composition(&request.midi)
        .map_err(|_| "Invalid MIDI composition format")?;

    Ok(())
}

pub async fn create_content(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(request): Json<CreateContentRequest>,
) -> Result<Json<CreateContentResponse>, (StatusCode, String)> {
    let channel_id = auth::authenticate_request(&headers, &state.jwt_secret)
        .map_err(|status| (status, "Authentication failed".to_string()))?;

    if channel_id != request.channel_id {
        return Err((StatusCode::FORBIDDEN, "Channel ID mismatch".to_string()));
    }

    // Validate content before processing
    validate_content_upload(&request)
        .map_err(|e| (StatusCode::BAD_REQUEST, e.to_string()))?;

    let db = state.db.get()
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("Database connection error: {e}")))?;

    let channel_exists: Result<i64, _> = db.query_row(
        "SELECT id FROM channels WHERE id = ?1",
        params![request.channel_id],
        |row| row.get(0),
    );

    if channel_exists.is_err() {
        return Err((StatusCode::NOT_FOUND, "Channel not found".to_string()));
    }

    let now = chrono::Utc::now().timestamp();

    db.execute(
        "INSERT INTO contents (channel_id, name, art, midi_composition, fps, created_at)
         VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        params![
            request.channel_id,
            request.name,
            request.art,
            request.midi,
            request.fps,
            now
        ],
    )
    .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, format!("Failed to insert content: {e}")))?;

    let content_id = db.last_insert_rowid();

    Ok(Json(CreateContentResponse {
        id: content_id,
        channel_id: request.channel_id,
        message: format!("Content '{}' uploaded successfully", request.name),
    }))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_validate_content_upload_valid() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test Content".to_string(),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀\n⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "4c 4e 4g".to_string(),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_ok());
    }

    #[test]
    fn test_validate_content_upload_empty_name() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "".to_string(),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "4c 4e 4g".to_string(),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_name_too_long() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "a".repeat(MAX_CONTENT_NAME_LENGTH + 1),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "4c 4e 4g".to_string(),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_fps_too_low() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test".to_string(),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "4c 4e 4g".to_string(),
            fps: 0.05,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_fps_too_high() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test".to_string(),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "4c 4e 4g".to_string(),
            fps: 150.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_art_too_large() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test".to_string(),
            art: "a".repeat(MAX_ART_SIZE + 1),
            midi: "4c 4e 4g".to_string(),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_empty_art() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test".to_string(),
            art: "".to_string(),
            midi: "4c 4e 4g".to_string(),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_midi_too_large() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test".to_string(),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "4c ".repeat(MAX_MIDI_SIZE),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_empty_midi() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test".to_string(),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "".to_string(),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }

    #[test]
    fn test_validate_content_upload_invalid_midi() {
        let request = CreateContentRequest {
            channel_id: 1,
            name: "Test".to_string(),
            art: "⠀⠀⠀⠀⠀⠀⠀⠀".to_string(),
            midi: "invalid midi notes xyz".to_string(),
            fps: 10.0,
        };

        assert!(validate_content_upload(&request).is_err());
    }
}

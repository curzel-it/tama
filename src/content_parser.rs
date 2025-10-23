use std::fs;

#[derive(Debug, Clone)]
pub struct ContentFile {
    pub midi_composition: String,
    pub art: String,
    pub fps: f32,
}

#[derive(Debug)]
pub enum ParseError {
    IoError(String),
    MissingMidiSection,
    MissingArtSection,
    InvalidFormat(String),
}

impl From<std::io::Error> for ParseError {
    fn from(err: std::io::Error) -> Self {
        ParseError::IoError(err.to_string())
    }
}

pub fn parse_content_file(path: &str) -> Result<ContentFile, ParseError> {
    let content = fs::read_to_string(path)?;
    parse_content(&content)
}

pub fn parse_content(content: &str) -> Result<ContentFile, ParseError> {
    let lines: Vec<&str> = content.lines().collect();

    let midi_start = lines.iter().position(|&line| line.trim() == "--- MIDI ---")
        .ok_or(ParseError::MissingMidiSection)?;

    let art_start = lines.iter().position(|&line| line.trim() == "--- ART ---")
        .ok_or(ParseError::MissingArtSection)?;

    if midi_start >= art_start {
        return Err(ParseError::InvalidFormat(
            "MIDI section must come before ART section".to_string()
        ));
    }

    let midi_lines: Vec<&str> = lines[midi_start + 1..art_start]
        .iter()
        .map(|&s| s.trim())
        .filter(|s| !s.is_empty())
        .collect();

    let midi_composition = midi_lines.join(" ");

    if midi_composition.is_empty() {
        return Err(ParseError::InvalidFormat(
            "MIDI section cannot be empty".to_string()
        ));
    }

    let art_lines: Vec<&str> = lines[art_start + 1..].to_vec();

    let art = art_lines.join("\n");

    if art.is_empty() {
        return Err(ParseError::InvalidFormat(
            "ART section cannot be empty".to_string()
        ));
    }

    let first_art_line = art_lines.first()
        .ok_or_else(|| ParseError::InvalidFormat("ART section has no header".to_string()))?;

    let fps = if first_art_line.starts_with("Ascii Art Animation") {
        10.0
    } else {
        return Err(ParseError::InvalidFormat(
            "ART section must start with 'Ascii Art Animation' header".to_string()
        ));
    };

    Ok(ContentFile {
        midi_composition,
        art,
        fps,
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_valid_content() {
        let content = r#"--- MIDI ---
8c4t 8e4t 8g4t
8c5t 8g4t
--- ART ---
Ascii Art Animation, 16x11
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⢠⠳⡀⠀⡰⢣⠀⠀⠀⠀⠀
"#;

        let result = parse_content(content);
        assert!(result.is_ok());

        let parsed = result.unwrap();
        assert_eq!(parsed.midi_composition, "8c4t 8e4t 8g4t 8c5t 8g4t");
        assert!(parsed.art.contains("Ascii Art Animation"));
        assert_eq!(parsed.fps, 10.0);
    }

    #[test]
    fn test_parse_missing_midi_section() {
        let content = r#"--- ART ---
Ascii Art Animation, 16x11
⠀⠀⠀⠀
"#;

        let result = parse_content(content);
        assert!(matches!(result, Err(ParseError::MissingMidiSection)));
    }

    #[test]
    fn test_parse_missing_art_section() {
        let content = r#"--- MIDI ---
8c4t 8e4t
"#;

        let result = parse_content(content);
        assert!(matches!(result, Err(ParseError::MissingArtSection)));
    }

    #[test]
    fn test_parse_sections_in_wrong_order() {
        let content = r#"--- ART ---
Ascii Art Animation, 16x11
--- MIDI ---
8c4t 8e4t
"#;

        let result = parse_content(content);
        assert!(matches!(result, Err(ParseError::InvalidFormat(_))));
    }

    #[test]
    fn test_parse_empty_midi() {
        let content = r#"--- MIDI ---

--- ART ---
Ascii Art Animation, 16x11
⠀⠀⠀⠀
"#;

        let result = parse_content(content);
        assert!(matches!(result, Err(ParseError::InvalidFormat(_))));
    }
}

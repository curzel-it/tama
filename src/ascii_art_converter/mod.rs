mod converter;

pub use converter::{ImageConverter, TV_WIDTH, TV_HEIGHT, PIXEL_WIDTH, PIXEL_HEIGHT};
use std::fmt;

pub struct AsciiArtSheet {
    pub width: usize,
    pub height: usize,
    pub frames: Vec<String>,
}

impl AsciiArtSheet {
    pub fn new(width: usize, height: usize) -> Self {
        Self {
            width,
            height,
            frames: Vec::new(),
        }
    }

    pub fn add_frame(&mut self, frame: String) {
        self.frames.push(frame);
    }


    pub fn frame_count(&self) -> usize {
        self.frames.len()
    }

    pub fn from_string(content: &str) -> Result<Self, String> {
        let mut lines = content.lines();

        let header = lines.next().ok_or("Empty file")?;

        if !header.starts_with("Ascii Art Animation, ") {
            return Err("Invalid header".to_string());
        }

        let after_prefix = header
            .trim_start_matches("Ascii Art Animation, ")
            .trim();

        let parts: Vec<&str> = after_prefix
            .split(',')
            .map(|s| s.trim())
            .collect();

        if parts.is_empty() {
            return Err("Invalid dimensions in header".to_string());
        }

        let dimensions_part = parts[0];
        let dim_parts: Vec<&str> = dimensions_part.split('x').collect();

        if dim_parts.len() != 2 {
            return Err("Invalid dimensions format, expected WIDTHxHEIGHT".to_string());
        }

        let width: usize = dim_parts[0]
            .parse()
            .map_err(|_| "Invalid width")?;
        let mut height: usize = dim_parts[1]
            .parse()
            .map_err(|_| "Invalid height")?;

        let mut frames = Vec::new();
        let mut current_frame = String::new();
        let mut line_count = 0;

        for line in lines {
            current_frame.push_str(line);
            current_frame.push('\n');
            line_count += 1;

            if line_count == height {
                frames.push(current_frame.trim_end().to_string());
                current_frame.clear();
                line_count = 0;
            }
        }

        if frames.is_empty() {
            frames.push(current_frame.trim_end().to_string());
            height = line_count;
        }

        Ok(Self {
            width,
            height,
            frames,
        })
    }
}

impl fmt::Display for AsciiArtSheet {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "Ascii Art Animation, {}x{}", self.width, self.height)?;

        for frame in &self.frames {
            writeln!(f, "{frame}")?;
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_ascii_art_sheet_creation() {
        let sheet = AsciiArtSheet::new(10, 5);
        assert_eq!(sheet.width, 10);
        assert_eq!(sheet.height, 5);
        assert_eq!(sheet.frame_count(), 0);
    }

    #[test]
    fn test_add_frame() {
        let mut sheet = AsciiArtSheet::new(10, 5);
        sheet.add_frame("test frame".to_string());
        assert_eq!(sheet.frame_count(), 1);
    }

    #[test]
    fn test_to_string() {
        let mut sheet = AsciiArtSheet::new(5, 3);
        sheet.add_frame("⠀⠀⠀⠀⠀\n⠀⠀⠀⠀⠀\n⠀⠀⠀⠀⠀".to_string());
        let output = sheet.to_string();
        assert!(output.starts_with("Ascii Art Animation, 5x3\n"));
    }

    #[test]
    fn test_from_string_with_fps() {
        let content = "Ascii Art Animation, 5x3, 10fps\n⠀⠀⠀⠀⠀\n⠀⠀⠀⠀⠀\n⠀⠀⠀⠀⠀";
        let result = AsciiArtSheet::from_string(content);
        assert!(result.is_ok());

        let sheet = result.unwrap();
        assert_eq!(sheet.width, 5);
        assert_eq!(sheet.height, 3);
        assert_eq!(sheet.frames.len(), 1);
    }

    #[test]
    fn test_from_string_without_fps() {
        let content = "Ascii Art Animation, 5x3\n⠀⠀⠀⠀⠀\n⠀⠀⠀⠀⠀\n⠀⠀⠀⠀⠀";
        let result = AsciiArtSheet::from_string(content);
        assert!(result.is_ok());

        let sheet = result.unwrap();
        assert_eq!(sheet.width, 5);
        assert_eq!(sheet.height, 3);
        assert_eq!(sheet.frames.len(), 1);
    }

    #[test]
    fn test_multi_frame_parsing() {
        let content = "Ascii Art Animation, 5x2\n⠁⠁⠁⠁⠁\n⠁⠁⠁⠁⠁\n⠂⠂⠂⠂⠂\n⠂⠂⠂⠂⠂\n⠃⠃⠃⠃⠃\n⠃⠃⠃⠃⠃";
        let result = AsciiArtSheet::from_string(content);
        assert!(result.is_ok());

        let sheet = result.unwrap();
        assert_eq!(sheet.width, 5);
        assert_eq!(sheet.height, 2);
        assert_eq!(sheet.frames.len(), 3, "Should have 3 frames");

        // Verify frame contents
        assert!(sheet.frames[0].contains("⠁⠁⠁⠁⠁"), "Frame 1 should contain marker ⠁");
        assert!(sheet.frames[1].contains("⠂⠂⠂⠂⠂"), "Frame 2 should contain marker ⠂");
        assert!(sheet.frames[2].contains("⠃⠃⠃⠃⠃"), "Frame 3 should contain marker ⠃");
    }

    #[test]
    fn test_neko_idle_frame_count() {
        use std::fs;
        if let Ok(content) = fs::read_to_string("sprites/neko_idle.txt") {
            let art_section_start = content.find("--- ART ---").unwrap() + "--- ART ---".len();
            let art_content = &content[art_section_start..].trim();

            let result = AsciiArtSheet::from_string(art_content);
            assert!(result.is_ok(), "Should parse neko_idle.txt successfully");

            let sheet = result.unwrap();
            assert_eq!(sheet.width, 20, "Width should be 20");
            assert_eq!(sheet.height, 10, "Height should be 10");
            assert_eq!(sheet.frames.len(), 6, "Should have 6 frames");
        }
    }

    #[test]
    fn test_content_editor_format_round_trip() {
        // Simulate the exact format the content editor produces:
        // 1. Header line
        // 2. Each frame followed by \n (pixelsToBraille returns lines.join('\n'), then + '\n')
        // 3. When concatenated, no empty lines between frames

        let frame1 = "⠁⠁⠁⠁⠁\n⠁⠁⠁⠁⠁";
        let frame2 = "⠂⠂⠂⠂⠂\n⠂⠂⠂⠂⠂";
        let frame3 = "⠃⠃⠃⠃⠃\n⠃⠃⠃⠃⠃";

        // This is what content-editor.html produces in download/publish
        let content_editor_format = format!(
            "Ascii Art Animation, 5x2, 10fps\n{}\n{}\n{}\n",
            frame1, frame2, frame3
        );

        let result = AsciiArtSheet::from_string(&content_editor_format);
        assert!(result.is_ok(), "Should parse content editor format");

        let sheet = result.unwrap();
        assert_eq!(sheet.frames.len(), 3, "Should preserve all 3 frames from content editor");
        assert!(sheet.frames[0].contains("⠁"), "Frame 1 preserved");
        assert!(sheet.frames[1].contains("⠂"), "Frame 2 preserved");
        assert!(sheet.frames[2].contains("⠃"), "Frame 3 preserved");
    }
}

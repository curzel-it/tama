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
}

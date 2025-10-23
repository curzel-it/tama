use crate::ascii_art_converter::AsciiArtSheet;
use std::fs;
use std::path::Path;

pub struct AsciiArtPlayer {
    sheet: AsciiArtSheet,
    current_frame: usize,
    frame_timer: f32,
    fps: f32,
}

impl AsciiArtPlayer {
    pub fn from_string(content: String, fps: f32) -> Result<Self, String> {
        let sheet = AsciiArtSheet::from_string(&content)?;

        Ok(Self {
            sheet,
            current_frame: 0,
            frame_timer: 0.0,
            fps,
        })
    }

    pub fn load<P: AsRef<Path>>(path: P, fps: f32) -> Result<Self, String> {
        let content = fs::read_to_string(path)
            .map_err(|e| format!("Failed to read file: {e}"))?;

        Self::from_string(content, fps)
    }

    pub fn update(&mut self, delta_time: f32) {
        if self.sheet.frames.len() > 1 {
            self.frame_timer += delta_time;
            let frame_duration = 1.0 / self.fps;

            if self.frame_timer >= frame_duration {
                self.frame_timer -= frame_duration;
                self.current_frame = (self.current_frame + 1) % self.sheet.frames.len();
            }
        }
    }

    pub fn current_frame(&self) -> &str {
        &self.sheet.frames[self.current_frame]
    }

    pub fn width(&self) -> usize {
        self.sheet.width
    }

    pub fn height(&self) -> usize {
        self.sheet.height
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_player_update() {
        let content = "Ascii Art Animation, 2x2\n⠀⠀\n⠀⠀\n⠁⠁\n⠁⠁\n";
        let sheet = AsciiArtSheet::from_string(content).unwrap();

        let mut player = AsciiArtPlayer {
            sheet,
            current_frame: 0,
            frame_timer: 0.0,
            fps: 10.0,
        };

        assert_eq!(player.current_frame, 0);

        player.update(0.05);
        assert_eq!(player.current_frame, 0);

        player.update(0.06);
        assert_eq!(player.current_frame, 1);

        player.update(0.1);
        assert_eq!(player.current_frame, 0);
    }
}

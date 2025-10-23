use rand::Rng;

pub struct LoadingAnimation {
    width: usize,
    height: usize,
    frame_timer: f32,
    fps: f32,
}

impl LoadingAnimation {
    pub fn new(width: usize, height: usize, fps: f32) -> Self {
        Self {
            width,
            height,
            frame_timer: 0.0,
            fps,
        }
    }

    pub fn update(&mut self, delta_time: f32) {
        self.frame_timer += delta_time;
        let frame_duration = 1.0 / self.fps;

        if self.frame_timer >= frame_duration {
            self.frame_timer -= frame_duration;
        }
    }

    pub fn get_frame(&self) -> String {
        let mut rng = rand::thread_rng();
        let mut result = String::new();

        for row in 0..self.height {
            if row > 0 {
                result.push('\n');
            }
            for _ in 0..self.width {
                let braille_char = char::from_u32(rng.gen_range(0x2800..=0x28FF))
                    .unwrap_or('⠀');
                result.push(braille_char);
            }
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_loading_animation_frame_size() {
        let loading = LoadingAnimation::new(10, 5, 10.0);
        let frame = loading.get_frame();
        let lines: Vec<&str> = frame.lines().collect();

        assert_eq!(lines.len(), 5);
        assert_eq!(lines[0].chars().count(), 10);
    }

    #[test]
    fn test_loading_animation_update() {
        let mut loading = LoadingAnimation::new(10, 5, 10.0);
        loading.update(0.1);
        assert!(loading.frame_timer >= 0.0);
    }
}

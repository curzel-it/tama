use rand::Rng;
use std::f32::consts::PI;

const WIDTH: usize = 64;
const HEIGHT: usize = 40;

#[derive(Debug, Clone, Copy)]
pub enum VisualizationStyle {
    Bars,
    Waveform,
    Particles,
    Geometric,
    Ripples,
    Spiral,
}

impl VisualizationStyle {
    pub fn all() -> Vec<Self> {
        vec![
            Self::Bars,
            Self::Waveform,
            Self::Particles,
            Self::Geometric,
            Self::Ripples,
            Self::Spiral,
        ]
    }

    pub fn from_index(index: usize) -> Self {
        match index % 6 {
            0 => Self::Bars,
            1 => Self::Waveform,
            2 => Self::Particles,
            3 => Self::Geometric,
            4 => Self::Ripples,
            5 => Self::Spiral,
            _ => unreachable!(),
        }
    }
}

#[derive(Debug, Clone)]
pub struct VisualizerConfig {
    pub fps: u32,
    pub num_frames: usize,
    pub style: VisualizationStyle,
}

impl VisualizerConfig {
    pub fn random<R: Rng>(rng: &mut R, style: VisualizationStyle) -> Self {
        VisualizerConfig {
            fps: rng.gen_range(2..=10),
            num_frames: rng.gen_range(10..=20),
            style,
        }
    }
}

#[derive(Debug, Clone)]
struct PixelFrame {
    pixels: Vec<Vec<bool>>,
}

impl PixelFrame {
    fn new() -> Self {
        PixelFrame {
            pixels: vec![vec![false; WIDTH]; HEIGHT],
        }
    }

    fn set_pixel(&mut self, x: usize, y: usize, value: bool) {
        if x < WIDTH && y < HEIGHT {
            self.pixels[y][x] = value;
        }
    }

    fn draw_line(&mut self, x1: i32, y1: i32, x2: i32, y2: i32) {
        let dx = (x2 - x1).abs();
        let dy = (y2 - y1).abs();
        let sx = if x1 < x2 { 1 } else { -1 };
        let sy = if y1 < y2 { 1 } else { -1 };
        let mut err = dx - dy;
        let mut x = x1;
        let mut y = y1;

        loop {
            self.set_pixel(x as usize, y as usize, true);

            if x == x2 && y == y2 {
                break;
            }

            let e2 = 2 * err;
            if e2 > -dy {
                err -= dy;
                x += sx;
            }
            if e2 < dx {
                err += dx;
                y += sy;
            }
        }
    }

    fn draw_circle(&mut self, cx: i32, cy: i32, radius: i32) {
        let mut x = radius;
        let mut y = 0;
        let mut err = 0;

        while x >= y {
            self.set_pixel((cx + x) as usize, (cy + y) as usize, true);
            self.set_pixel((cx + y) as usize, (cy + x) as usize, true);
            self.set_pixel((cx - y) as usize, (cy + x) as usize, true);
            self.set_pixel((cx - x) as usize, (cy + y) as usize, true);
            self.set_pixel((cx - x) as usize, (cy - y) as usize, true);
            self.set_pixel((cx - y) as usize, (cy - x) as usize, true);
            self.set_pixel((cx + y) as usize, (cy - x) as usize, true);
            self.set_pixel((cx + x) as usize, (cy - y) as usize, true);

            y += 1;
            err += 1 + 2 * y;
            if 2 * (err - x) + 1 > 0 {
                x -= 1;
                err += 1 - 2 * x;
            }
        }
    }

    fn draw_filled_rect(&mut self, x: usize, y: usize, width: usize, height: usize) {
        for dy in 0..height {
            for dx in 0..width {
                self.set_pixel(x + dx, y + dy, true);
            }
        }
    }
}

pub struct Visualizer;

impl Visualizer {
    pub fn generate<R: Rng>(config: &VisualizerConfig, rng: &mut R) -> String {
        let frames = match config.style {
            VisualizationStyle::Bars => Self::generate_bars(config, rng),
            VisualizationStyle::Waveform => Self::generate_waveform(config, rng),
            VisualizationStyle::Particles => Self::generate_particles(config, rng),
            VisualizationStyle::Geometric => Self::generate_geometric(config, rng),
            VisualizationStyle::Ripples => Self::generate_ripples(config, rng),
            VisualizationStyle::Spiral => Self::generate_spiral(config, rng),
        };

        Self::frames_to_braille(&frames, config.fps)
    }

    fn generate_bars<R: Rng>(config: &VisualizerConfig, rng: &mut R) -> Vec<PixelFrame> {
        let num_bars = 12;
        let bar_width = WIDTH / num_bars;
        let mut frames = Vec::new();

        let bar_heights: Vec<Vec<usize>> = (0..num_bars)
            .map(|_| {
                (0..config.num_frames)
                    .map(|_| rng.gen_range(5..HEIGHT))
                    .collect()
            })
            .collect();

        for frame_idx in 0..config.num_frames {
            let mut frame = PixelFrame::new();

            for (bar_idx, bar_height_sequence) in bar_heights.iter().enumerate().take(num_bars) {
                let height = bar_height_sequence[frame_idx];
                let x_start = bar_idx * bar_width;
                let y_start = HEIGHT - height;

                frame.draw_filled_rect(x_start, y_start, bar_width - 1, height);
            }

            frames.push(frame);
        }

        frames
    }

    fn generate_waveform<R: Rng>(config: &VisualizerConfig, rng: &mut R) -> Vec<PixelFrame> {
        let mut frames = Vec::new();
        let amplitude = (HEIGHT as f32 / 3.0) as i32;
        let frequency = rng.gen_range(1.0..3.0);

        for frame_idx in 0..config.num_frames {
            let mut frame = PixelFrame::new();
            let phase = (frame_idx as f32 / config.num_frames as f32) * 2.0 * PI;

            for x in 0..(WIDTH - 1) {
                let t = (x as f32 / WIDTH as f32) * 2.0 * PI * frequency + phase;
                let y1 = (HEIGHT as f32 / 2.0 + amplitude as f32 * t.sin()) as i32;
                let y2 = (HEIGHT as f32 / 2.0
                    + amplitude as f32 * (t + PI / (WIDTH as f32)).sin()) as i32;

                frame.draw_line(x as i32, y1, (x + 1) as i32, y2);
            }

            frames.push(frame);
        }

        frames
    }

    fn generate_particles<R: Rng>(config: &VisualizerConfig, rng: &mut R) -> Vec<PixelFrame> {
        struct Particle {
            x: f32,
            y: f32,
            vx: f32,
            vy: f32,
        }

        let num_particles = 15;
        let mut particles: Vec<Particle> = (0..num_particles)
            .map(|_| Particle {
                x: rng.gen_range(0.0..WIDTH as f32),
                y: rng.gen_range(0.0..HEIGHT as f32),
                vx: rng.gen_range(-2.0..2.0),
                vy: rng.gen_range(-2.0..2.0),
            })
            .collect();

        let mut frames = Vec::new();

        for _ in 0..config.num_frames {
            let mut frame = PixelFrame::new();

            for particle in &mut particles {
                particle.x += particle.vx;
                particle.y += particle.vy;

                if particle.x <= 0.0 || particle.x >= WIDTH as f32 - 1.0 {
                    particle.vx = -particle.vx;
                    particle.x = particle.x.clamp(0.0, WIDTH as f32 - 1.0);
                }
                if particle.y <= 0.0 || particle.y >= HEIGHT as f32 - 1.0 {
                    particle.vy = -particle.vy;
                    particle.y = particle.y.clamp(0.0, HEIGHT as f32 - 1.0);
                }

                let x = particle.x as usize;
                let y = particle.y as usize;
                frame.draw_filled_rect(x.saturating_sub(1), y.saturating_sub(1), 3, 3);
            }

            frames.push(frame);
        }

        frames
    }

    fn generate_geometric<R: Rng>(config: &VisualizerConfig, rng: &mut R) -> Vec<PixelFrame> {
        let mut frames = Vec::new();
        let cx = WIDTH as i32 / 2;
        let cy = HEIGHT as i32 / 2;
        let size = rng.gen_range(8..16);

        for frame_idx in 0..config.num_frames {
            let mut frame = PixelFrame::new();
            let angle = (frame_idx as f32 / config.num_frames as f32) * 2.0 * PI;

            let num_sides = rng.gen_range(3..7);
            for i in 0..num_sides {
                let a1 = angle + (i as f32 * 2.0 * PI / num_sides as f32);
                let a2 = angle + ((i + 1) as f32 * 2.0 * PI / num_sides as f32);

                let x1 = cx + (size as f32 * a1.cos()) as i32;
                let y1 = cy + (size as f32 * a1.sin()) as i32;
                let x2 = cx + (size as f32 * a2.cos()) as i32;
                let y2 = cy + (size as f32 * a2.sin()) as i32;

                frame.draw_line(x1, y1, x2, y2);
            }

            frames.push(frame);
        }

        frames
    }

    fn generate_ripples<R: Rng>(config: &VisualizerConfig, _rng: &mut R) -> Vec<PixelFrame> {
        let mut frames = Vec::new();
        let cx = WIDTH as i32 / 2;
        let cy = HEIGHT as i32 / 2;
        let max_radius = 20;

        for frame_idx in 0..config.num_frames {
            let mut frame = PixelFrame::new();
            let progress = frame_idx as f32 / config.num_frames as f32;

            for ring in 0..3 {
                let ring_progress = (progress + ring as f32 / 3.0) % 1.0;
                let radius = (ring_progress * max_radius as f32) as i32;
                frame.draw_circle(cx, cy, radius);
            }

            frames.push(frame);
        }

        frames
    }

    fn generate_spiral<R: Rng>(config: &VisualizerConfig, rng: &mut R) -> Vec<PixelFrame> {
        let mut frames = Vec::new();
        let cx = WIDTH as f32 / 2.0;
        let cy = HEIGHT as f32 / 2.0;
        let num_points = 100;
        let max_radius = 18.0;
        let turns = rng.gen_range(2.0..4.0);

        for frame_idx in 0..config.num_frames {
            let mut frame = PixelFrame::new();
            let rotation = (frame_idx as f32 / config.num_frames as f32) * 2.0 * PI;

            for i in 0..num_points {
                let t = i as f32 / num_points as f32;
                let angle = t * turns * 2.0 * PI + rotation;
                let radius = t * max_radius;

                let x = (cx + radius * angle.cos()) as usize;
                let y = (cy + radius * angle.sin()) as usize;

                frame.set_pixel(x, y, true);
            }

            frames.push(frame);
        }

        frames
    }

    fn frames_to_braille(frames: &[PixelFrame], fps: u32) -> String {
        let mut result = format!("Ascii Art Animation, {WIDTH}x{HEIGHT}, {fps}fps\n");

        for frame in frames {
            for row_chunk in frame.pixels.chunks(4) {
                for col in 0..(WIDTH / 2) {
                    let mut braille_value: u32 = 0x2800;

                    for (row_in_chunk, row) in row_chunk.iter().enumerate() {
                        if col * 2 < WIDTH && row[col * 2] {
                            braille_value |= Self::braille_bit(0, row_in_chunk);
                        }
                        if col * 2 + 1 < WIDTH && row[col * 2 + 1] {
                            braille_value |= Self::braille_bit(1, row_in_chunk);
                        }
                    }

                    if let Some(c) = char::from_u32(braille_value) {
                        result.push(c);
                    }
                }
                result.push('\n');
            }
        }

        result
    }

    fn braille_bit(col: usize, row: usize) -> u32 {
        match (col, row) {
            (0, 0) => 0x01,
            (0, 1) => 0x02,
            (0, 2) => 0x04,
            (0, 3) => 0x40,
            (1, 0) => 0x08,
            (1, 1) => 0x10,
            (1, 2) => 0x20,
            (1, 3) => 0x80,
            _ => 0x00,
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use rand::SeedableRng;
    use rand_chacha::ChaCha8Rng;

    #[test]
    fn test_bars_generation() {
        let mut rng = ChaCha8Rng::seed_from_u64(42);
        let config = VisualizerConfig::random(&mut rng, VisualizationStyle::Bars);
        let result = Visualizer::generate(&config, &mut rng);

        assert!(result.contains("Ascii Art Animation"));
        assert!(result.contains(&format!("{}x{}", WIDTH, HEIGHT)));
        assert!(!result.is_empty());
    }

    #[test]
    fn test_all_styles() {
        let mut rng = ChaCha8Rng::seed_from_u64(42);

        for style in VisualizationStyle::all() {
            let config = VisualizerConfig::random(&mut rng, style);
            let result = Visualizer::generate(&config, &mut rng);

            assert!(result.contains("Ascii Art Animation"));
            assert!(!result.is_empty());
        }
    }

    #[test]
    fn test_deterministic_with_seed() {
        let mut rng1 = ChaCha8Rng::seed_from_u64(42);
        let config1 = VisualizerConfig::random(&mut rng1, VisualizationStyle::Waveform);
        let result1 = Visualizer::generate(&config1, &mut rng1);

        let mut rng2 = ChaCha8Rng::seed_from_u64(42);
        let config2 = VisualizerConfig::random(&mut rng2, VisualizationStyle::Waveform);
        let result2 = Visualizer::generate(&config2, &mut rng2);

        assert_eq!(result1, result2);
    }
}

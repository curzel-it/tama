#[derive(PartialEq)]
enum RemoteState {
    Idle,
    Animating,
}

pub struct RemoteAnimation {
    frames: Vec<RemoteFrame>,
    current_frame: usize,
    frame_timer: f32,
    fps: f32,
    is_playing: bool,
    target_channel: Option<usize>,
    channel_switched: bool,
    state: RemoteState,
}

struct RemoteFrame {
    art: &'static str,
    y_offset: i16,
}

impl Default for RemoteAnimation {
    fn default() -> Self {
        Self::new()
    }
}

impl RemoteAnimation {
    pub fn new() -> Self {
        let frames = vec![
            RemoteFrame { art: REMOTE_FRAME_0, y_offset: 0 },
            RemoteFrame { art: REMOTE_FRAME_1, y_offset: -1 },
            RemoteFrame { art: REMOTE_FRAME_2, y_offset: -2 },
            RemoteFrame { art: REMOTE_FRAME_3, y_offset: -2 },
            RemoteFrame { art: REMOTE_FRAME_4, y_offset: -2 },
            RemoteFrame { art: REMOTE_FRAME_0, y_offset: -1 },
            RemoteFrame { art: REMOTE_FRAME_0, y_offset: 0 },
        ];

        Self {
            frames,
            current_frame: 0,
            frame_timer: 0.0,
            fps: 10.0,
            is_playing: false,
            target_channel: None,
            channel_switched: false,
            state: RemoteState::Idle,
        }
    }

    pub fn trigger(&mut self, target_channel: usize) {
        self.state = RemoteState::Animating;
        self.is_playing = true;
        self.current_frame = 0;
        self.frame_timer = 0.0;
        self.target_channel = Some(target_channel);
        self.channel_switched = false;
    }

    pub fn update(&mut self, delta_time: f32) {
        if !self.is_playing {
            return;
        }

        self.frame_timer += delta_time;
        let frame_duration = 1.0 / self.fps;

        if self.frame_timer >= frame_duration {
            self.frame_timer -= frame_duration;
            self.current_frame += 1;

            if self.current_frame >= self.frames.len() {
                self.is_playing = false;
                self.current_frame = 0;
                self.target_channel = None;
                self.channel_switched = false;
                self.state = RemoteState::Idle;
            }
        }
    }

    pub fn is_playing(&self) -> bool {
        self.is_playing
    }

    pub fn get_frame(&self) -> Option<(&str, i16)> {
        if !self.is_playing {
            return None;
        }

        self.frames.get(self.current_frame)
            .map(|frame| (frame.art, frame.y_offset))
    }

    pub fn should_switch_channel(&mut self) -> Option<usize> {
        if self.is_playing && self.current_frame == 3 && !self.channel_switched {
            self.channel_switched = true;
            self.target_channel
        } else {
            None
        }
    }
}

const REMOTE_FRAME_0: &str = "╭───╮\n│:::│\n│:::│\n│ : │\n╰───╯";
const REMOTE_FRAME_1: &str = "╭───╮\n│.::│\n│:::│\n│ : │\n╰───╯";
const REMOTE_FRAME_2: &str = "╭───╮\n│*::│\n│:::│\n│ : │\n╰───╯";
const REMOTE_FRAME_3: &str = "╭───╮\n│o::│\n│:::│\n│ : │\n╰───╯";
const REMOTE_FRAME_4: &str = "╭───╮\n│:::│\n│:::│\n│ : │\n╰───╯";

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_remote_animation_trigger() {
        let mut remote = RemoteAnimation::new();
        assert!(!remote.is_playing());

        remote.trigger(1);
        assert!(remote.is_playing());
        assert_eq!(remote.target_channel, Some(1));
    }

    #[test]
    fn test_remote_animation_update() {
        let mut remote = RemoteAnimation::new();
        remote.trigger(0);

        for _ in 0..7 {
            remote.update(0.1);
        }

        assert!(!remote.is_playing());
    }

    #[test]
    fn test_channel_switch_timing() {
        let mut remote = RemoteAnimation::new();
        remote.trigger(1);

        for _ in 0..3 {
            assert!(remote.should_switch_channel().is_none(), "Should not switch before frame 3");
            remote.update(0.1);
        }

        assert_eq!(remote.should_switch_channel(), Some(1), "Should switch at frame 3");
        assert!(remote.should_switch_channel().is_none(), "Should only switch once");
    }
}

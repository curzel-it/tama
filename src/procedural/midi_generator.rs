use crate::procedural::music_theory::{MidiNote, Note, NoteDuration, Scale};
use rand::Rng;

#[derive(Debug, Clone, Copy)]
pub enum Complexity {
    Simple,
    Medium,
    Complex,
}

#[derive(Debug, Clone, Copy)]
pub enum Waveform {
    Square,
    Triangle,
    Sawtooth,
    Pulse,
}

impl Waveform {
    pub fn to_string(self) -> &'static str {
        match self {
            Waveform::Square => "s",
            Waveform::Triangle => "t",
            Waveform::Sawtooth => "w",
            Waveform::Pulse => "p",
        }
    }

    pub fn random<R: Rng>(rng: &mut R) -> Self {
        match rng.gen_range(0..4) {
            0 => Waveform::Square,
            1 => Waveform::Triangle,
            2 => Waveform::Sawtooth,
            3 => Waveform::Pulse,
            _ => unreachable!(),
        }
    }
}

#[derive(Debug, Clone)]
pub struct MidiGeneratorConfig {
    pub bpm: u32,
    pub scale: Scale,
    pub root_note: Note,
    pub complexity: Complexity,
    pub num_bars: u32,
}

impl MidiGeneratorConfig {
    pub fn random<R: Rng>(rng: &mut R, complexity: Complexity) -> Self {
        let all_notes = Note::all_notes();
        let root_note = all_notes[rng.gen_range(0..all_notes.len())];

        MidiGeneratorConfig {
            bpm: rng.gen_range(80..160),
            scale: Scale::random(rng),
            root_note,
            complexity,
            num_bars: rng.gen_range(2..5),
        }
    }
}

pub struct MidiGenerator;

impl MidiGenerator {
    pub fn generate<R: Rng>(config: &MidiGeneratorConfig, rng: &mut R) -> String {
        let scale_notes = config.scale.get_notes(config.root_note);

        let melody = Self::generate_melody(config, &scale_notes, rng);

        match config.complexity {
            Complexity::Simple => {
                let waveform = Waveform::random(rng);
                let channel = Self::notes_to_channel(&melody, waveform);
                format!("--bpm {} {}", config.bpm, channel)
            }
            Complexity::Medium => {
                let melody_waveform = Waveform::random(rng);
                let bass_waveform = Waveform::random(rng);

                let bass = Self::generate_bass(config, &scale_notes, &melody, rng);

                let melody_channel = Self::notes_to_channel(&melody, melody_waveform);
                let bass_channel = Self::notes_to_channel(&bass, bass_waveform);

                format!(
                    "--bpm {} --channel {} --channel --volume 0.6 {}",
                    config.bpm, melody_channel, bass_channel
                )
            }
            Complexity::Complex => {
                let melody_waveform = Waveform::random(rng);
                let bass_waveform = Waveform::random(rng);
                let harmony_waveform = Waveform::random(rng);

                let bass = Self::generate_bass(config, &scale_notes, &melody, rng);
                let harmony = Self::generate_harmony(config, &scale_notes, &melody, rng);

                let melody_channel = Self::notes_to_channel(&melody, melody_waveform);
                let bass_channel = Self::notes_to_channel(&bass, bass_waveform);
                let harmony_channel = Self::notes_to_channel(&harmony, harmony_waveform);

                format!(
                    "--bpm {} --channel {} --channel --volume 0.6 {} --channel --volume 0.5 {}",
                    config.bpm, melody_channel, bass_channel, harmony_channel
                )
            }
        }
    }

    fn generate_melody<R: Rng>(
        config: &MidiGeneratorConfig,
        scale_notes: &[Note],
        rng: &mut R,
    ) -> Vec<MidiNote> {
        let beats_per_bar = 4;
        let total_beats = config.num_bars * beats_per_bar;
        let mut melody = Vec::new();
        let mut current_beats = 0.0;

        let mut current_note_idx = rng.gen_range(0..scale_notes.len().min(5));
        let octave = rng.gen_range(3..6);

        while current_beats < total_beats as f32 {
            let duration = NoteDuration::random_rhythm(rng);
            let duration_beats = duration.to_beats();

            if current_beats + duration_beats > total_beats as f32 {
                break;
            }

            let step = rng.gen_range(-2..=2);
            current_note_idx = ((current_note_idx as i32 + step)
                .max(0)
                .min((scale_notes.len() - 1) as i32)) as usize;

            melody.push(MidiNote {
                note: scale_notes[current_note_idx],
                octave,
                duration,
            });

            current_beats += duration_beats;
        }

        if melody.is_empty() {
            melody.push(MidiNote {
                note: scale_notes[0],
                octave,
                duration: NoteDuration::Whole,
            });
        }

        melody
    }

    fn generate_bass<R: Rng>(
        config: &MidiGeneratorConfig,
        scale_notes: &[Note],
        _melody: &[MidiNote],
        rng: &mut R,
    ) -> Vec<MidiNote> {
        let beats_per_bar = 4;
        let total_beats = config.num_bars * beats_per_bar;
        let mut bass = Vec::new();
        let mut current_beats = 0.0;

        let octave = 2;

        while current_beats < total_beats as f32 {
            let duration = if rng.gen_bool(0.7) {
                NoteDuration::Quarter
            } else {
                NoteDuration::Half
            };
            let duration_beats = duration.to_beats();

            if current_beats + duration_beats > total_beats as f32 {
                break;
            }

            let note_idx = if rng.gen_bool(0.7) {
                0
            } else {
                rng.gen_range(0..scale_notes.len().min(3))
            };

            bass.push(MidiNote {
                note: scale_notes[note_idx],
                octave,
                duration,
            });

            current_beats += duration_beats;
        }

        if bass.is_empty() {
            bass.push(MidiNote {
                note: scale_notes[0],
                octave,
                duration: NoteDuration::Whole,
            });
        }

        bass
    }

    fn generate_harmony<R: Rng>(
        _config: &MidiGeneratorConfig,
        scale_notes: &[Note],
        melody: &[MidiNote],
        rng: &mut R,
    ) -> Vec<MidiNote> {
        let mut harmony = Vec::new();

        for midi_note in melody {
            let melody_idx = scale_notes
                .iter()
                .position(|&n| n == midi_note.note)
                .unwrap_or(0);

            let interval = if rng.gen_bool(0.6) { 2 } else { 4 };
            let harmony_idx = (melody_idx + interval) % scale_notes.len();

            harmony.push(MidiNote {
                note: scale_notes[harmony_idx],
                octave: midi_note.octave,
                duration: midi_note.duration,
            });
        }

        harmony
    }

    fn notes_to_channel(notes: &[MidiNote], waveform: Waveform) -> String {
        notes
            .iter()
            .map(|note| format!("{}{}", note, waveform.to_string()))
            .collect::<Vec<_>>()
            .join(" ")
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use rand::SeedableRng;
    use rand_chacha::ChaCha8Rng;

    #[test]
    fn test_simple_generation() {
        let mut rng = ChaCha8Rng::seed_from_u64(42);
        let config = MidiGeneratorConfig::random(&mut rng, Complexity::Simple);
        let composition = MidiGenerator::generate(&config, &mut rng);

        assert!(composition.contains("--bpm"));
        assert!(!composition.is_empty());
    }

    #[test]
    fn test_medium_generation() {
        let mut rng = ChaCha8Rng::seed_from_u64(42);
        let config = MidiGeneratorConfig::random(&mut rng, Complexity::Medium);
        let composition = MidiGenerator::generate(&config, &mut rng);

        assert!(composition.contains("--bpm"));
        assert!(composition.contains("--channel"));
        assert!(!composition.is_empty());
    }

    #[test]
    fn test_complex_generation() {
        let mut rng = ChaCha8Rng::seed_from_u64(42);
        let config = MidiGeneratorConfig::random(&mut rng, Complexity::Complex);
        let composition = MidiGenerator::generate(&config, &mut rng);

        assert!(composition.contains("--bpm"));
        assert!(composition.matches("--channel").count() >= 2);
        assert!(!composition.is_empty());
    }

    #[test]
    fn test_deterministic_with_seed() {
        let mut rng1 = ChaCha8Rng::seed_from_u64(42);
        let config1 = MidiGeneratorConfig::random(&mut rng1, Complexity::Simple);
        let composition1 = MidiGenerator::generate(&config1, &mut rng1);

        let mut rng2 = ChaCha8Rng::seed_from_u64(42);
        let config2 = MidiGeneratorConfig::random(&mut rng2, Complexity::Simple);
        let composition2 = MidiGenerator::generate(&config2, &mut rng2);

        assert_eq!(composition1, composition2);
    }
}

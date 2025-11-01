use rand::Rng;
use std::fmt;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Note {
    C,
    Cs,
    D,
    Ds,
    E,
    F,
    Fs,
    G,
    Gs,
    A,
    As,
    B,
}

impl Note {
    pub fn from_semitone(semitone: u8) -> Self {
        match semitone % 12 {
            0 => Note::C,
            1 => Note::Cs,
            2 => Note::D,
            3 => Note::Ds,
            4 => Note::E,
            5 => Note::F,
            6 => Note::Fs,
            7 => Note::G,
            8 => Note::Gs,
            9 => Note::A,
            10 => Note::As,
            11 => Note::B,
            _ => unreachable!(),
        }
    }

    pub fn to_semitone(self) -> u8 {
        match self {
            Note::C => 0,
            Note::Cs => 1,
            Note::D => 2,
            Note::Ds => 3,
            Note::E => 4,
            Note::F => 5,
            Note::Fs => 6,
            Note::G => 7,
            Note::Gs => 8,
            Note::A => 9,
            Note::As => 10,
            Note::B => 11,
        }
    }

    pub fn to_string(self) -> &'static str {
        match self {
            Note::C => "c",
            Note::Cs => "#c",
            Note::D => "d",
            Note::Ds => "#d",
            Note::E => "e",
            Note::F => "f",
            Note::Fs => "#f",
            Note::G => "g",
            Note::Gs => "#g",
            Note::A => "a",
            Note::As => "#a",
            Note::B => "b",
        }
    }

    pub fn all_notes() -> Vec<Note> {
        vec![
            Note::C,
            Note::Cs,
            Note::D,
            Note::Ds,
            Note::E,
            Note::F,
            Note::Fs,
            Note::G,
            Note::Gs,
            Note::A,
            Note::As,
            Note::B,
        ]
    }
}

#[derive(Debug, Clone)]
pub struct Scale {
    intervals: Vec<u8>,
}

impl Scale {
    pub fn major() -> Self {
        Scale {
            intervals: vec![2, 2, 1, 2, 2, 2, 1],
        }
    }

    pub fn minor() -> Self {
        Scale {
            intervals: vec![2, 1, 2, 2, 1, 2, 2],
        }
    }

    pub fn pentatonic_major() -> Self {
        Scale {
            intervals: vec![2, 2, 3, 2, 3],
        }
    }

    pub fn pentatonic_minor() -> Self {
        Scale {
            intervals: vec![3, 2, 2, 3, 2],
        }
    }

    pub fn blues() -> Self {
        Scale {
            intervals: vec![3, 2, 1, 1, 3, 2],
        }
    }

    pub fn dorian() -> Self {
        Scale {
            intervals: vec![2, 1, 2, 2, 2, 1, 2],
        }
    }

    pub fn get_notes(&self, root: Note) -> Vec<Note> {
        let mut notes = vec![root];
        let mut current_semitone = root.to_semitone();

        for &interval in &self.intervals {
            current_semitone = (current_semitone + interval) % 12;
            notes.push(Note::from_semitone(current_semitone));
        }

        notes
    }

    pub fn random<R: Rng>(rng: &mut R) -> Self {
        match rng.gen_range(0..6) {
            0 => Scale::major(),
            1 => Scale::minor(),
            2 => Scale::pentatonic_major(),
            3 => Scale::pentatonic_minor(),
            4 => Scale::blues(),
            5 => Scale::dorian(),
            _ => unreachable!(),
        }
    }
}

#[derive(Debug, Clone, Copy)]
pub enum NoteDuration {
    Whole,
    Half,
    Quarter,
    Eighth,
    Sixteenth,
}

impl NoteDuration {
    pub fn to_string(self) -> &'static str {
        match self {
            NoteDuration::Whole => "1",
            NoteDuration::Half => "2",
            NoteDuration::Quarter => "4",
            NoteDuration::Eighth => "8",
            NoteDuration::Sixteenth => "16",
        }
    }

    pub fn to_beats(self) -> f32 {
        match self {
            NoteDuration::Whole => 4.0,
            NoteDuration::Half => 2.0,
            NoteDuration::Quarter => 1.0,
            NoteDuration::Eighth => 0.5,
            NoteDuration::Sixteenth => 0.25,
        }
    }

    pub fn random<R: Rng>(rng: &mut R) -> Self {
        match rng.gen_range(0..5) {
            0 => NoteDuration::Whole,
            1 => NoteDuration::Half,
            2 => NoteDuration::Quarter,
            3 => NoteDuration::Eighth,
            4 => NoteDuration::Sixteenth,
            _ => unreachable!(),
        }
    }

    pub fn random_rhythm<R: Rng>(rng: &mut R) -> Self {
        match rng.gen_range(0..4) {
            0 => NoteDuration::Quarter,
            1 => NoteDuration::Eighth,
            2 => NoteDuration::Sixteenth,
            3 => NoteDuration::Half,
            _ => unreachable!(),
        }
    }
}

#[derive(Debug, Clone)]
pub struct MidiNote {
    pub note: Note,
    pub octave: u8,
    pub duration: NoteDuration,
}

impl fmt::Display for MidiNote {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "{}{}{}",
            self.duration.to_string(),
            self.note.to_string(),
            self.octave
        )
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_note_conversion() {
        assert_eq!(Note::C.to_semitone(), 0);
        assert_eq!(Note::Cs.to_semitone(), 1);
        assert_eq!(Note::B.to_semitone(), 11);
        assert_eq!(Note::from_semitone(0), Note::C);
        assert_eq!(Note::from_semitone(12), Note::C);
    }

    #[test]
    fn test_major_scale() {
        let c_major = Scale::major().get_notes(Note::C);
        assert_eq!(c_major.len(), 8);
        assert_eq!(c_major[0], Note::C);
        assert_eq!(c_major[1], Note::D);
        assert_eq!(c_major[2], Note::E);
        assert_eq!(c_major[3], Note::F);
        assert_eq!(c_major[4], Note::G);
        assert_eq!(c_major[5], Note::A);
        assert_eq!(c_major[6], Note::B);
        assert_eq!(c_major[7], Note::C);
    }

    #[test]
    fn test_minor_scale() {
        let a_minor = Scale::minor().get_notes(Note::A);
        assert_eq!(a_minor.len(), 8);
        assert_eq!(a_minor[0], Note::A);
        assert_eq!(a_minor[1], Note::B);
        assert_eq!(a_minor[2], Note::C);
    }

    #[test]
    fn test_pentatonic_scale() {
        let c_pent = Scale::pentatonic_major().get_notes(Note::C);
        assert_eq!(c_pent.len(), 6);
    }
}

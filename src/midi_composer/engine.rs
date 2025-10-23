use rodio::{buffer::SamplesBuffer, OutputStream, OutputStreamHandle, Sink, Source};

#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum Waveform {
    #[default]
    Square,
    Triangle,
    Sawtooth,
    Pulse,
}

#[derive(Debug, Clone, PartialEq)]
pub struct Note {
    pub pitch: Option<u8>,
    pub duration: f32,
    pub waveform: Waveform,
    pub volume: f32,
    pub arpeggio_notes: Vec<u8>,
    pub adsr: bool,
    pub vibrato: bool,
}

struct MidiParser {
    bpm: u16,
}

impl MidiParser {
    fn duration_to_seconds(&self, duration: u8) -> f32 {
        let quarter_note_duration = 60.0 / self.bpm as f32;
        (4.0 / duration as f32) * quarter_note_duration
    }

    fn parse_note(&self, input: &str) -> Result<Note, String> {
        let input = input.trim();
        if input.is_empty() {
            return Err("Empty note string".to_string());
        }

        let mut chars = input.chars().peekable();
        let mut duration_str = String::new();

        while let Some(&ch) = chars.peek() {
            if ch.is_ascii_digit() {
                duration_str.push(chars.next().unwrap());
            } else {
                break;
            }
        }

        if duration_str.is_empty() {
            return Err(format!("Missing duration in note: '{input}'"));
        }

        let duration_value: u8 = duration_str
            .parse()
            .map_err(|_| format!("Invalid duration number: '{duration_str}'"))?;

        if let Some(&'(') = chars.peek() {
            return self.parse_arpeggio(duration_value, &mut chars);
        }

        let mut is_sharp = false;
        if let Some(&'#') = chars.peek() {
            is_sharp = true;
            chars.next();
        }

        let note_char = chars
            .next()
            .ok_or_else(|| format!("Missing note letter in: '{input}'"))?
            .to_ascii_lowercase();

        if note_char == '-' {
            let duration = self.duration_to_seconds(duration_value);
            return Ok(Note {
                pitch: None,
                duration,
                waveform: Waveform::default(),
                volume: 1.0,
                arpeggio_notes: Vec::new(),
                adsr: false,
                vibrato: false,
            });
        }

        if !matches!(note_char, 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g') {
            return Err(format!("Invalid note letter: '{note_char}'"));
        }

        let remaining_chars: Vec<char> = chars.collect();
        let (octave, waveform, volume) = Self::parse_note_modifiers(&remaining_chars)?;

        let base_pitch = match note_char {
            'c' => 0,
            'd' => 2,
            'e' => 4,
            'f' => 5,
            'g' => 7,
            'a' => 9,
            'b' => 11,
            _ => unreachable!(),
        };

        let sharp_offset = if is_sharp { 1 } else { 0 };
        let midi_note = 12 * octave + base_pitch + sharp_offset;

        let duration = self.duration_to_seconds(duration_value);

        Ok(Note {
            pitch: Some(midi_note),
            duration,
            waveform,
            volume,
            arpeggio_notes: Vec::new(),
            adsr: false,
            vibrato: false,
        })
    }

    fn parse_arpeggio(&self, duration_value: u8, chars: &mut std::iter::Peekable<std::str::Chars>) -> Result<Note, String> {
        chars.next();

        let mut content = String::new();
        let mut found_close = false;

        for ch in chars.by_ref() {
            if ch == ')' {
                found_close = true;
                break;
            }
            content.push(ch);
        }

        if !found_close {
            return Err("Missing closing parenthesis in arpeggio".to_string());
        }

        if content.trim().is_empty() {
            return Err("Empty arpeggio".to_string());
        }

        let note_parts: Vec<&str> = content.split_whitespace().collect();
        let mut arpeggio_notes = Vec::new();

        for part in note_parts {
            let mut note_chars = part.chars().peekable();

            let mut is_sharp = false;
            if let Some(&'#') = note_chars.peek() {
                is_sharp = true;
                note_chars.next();
            }

            let note_char = note_chars
                .next()
                .ok_or_else(|| format!("Missing note letter in arpeggio: '{part}'"))?
                .to_ascii_lowercase();

            if !matches!(note_char, 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g') {
                return Err(format!("Invalid note letter in arpeggio: '{note_char}'"));
            }

            let remaining: Vec<char> = note_chars.collect();
            let (octave, _, _) = Self::parse_note_modifiers(&remaining)?;

            let base_pitch = match note_char {
                'c' => 0,
                'd' => 2,
                'e' => 4,
                'f' => 5,
                'g' => 7,
                'a' => 9,
                'b' => 11,
                _ => unreachable!(),
            };

            let sharp_offset = if is_sharp { 1 } else { 0 };
            let midi_note = 12 * octave + base_pitch + sharp_offset;
            arpeggio_notes.push(midi_note);
        }

        let duration = self.duration_to_seconds(duration_value);

        Ok(Note {
            pitch: None,
            duration,
            waveform: Waveform::default(),
            volume: 1.0,
            arpeggio_notes,
            adsr: false,
            vibrato: false,
        })
    }

    fn parse_note_modifiers(remaining_chars: &[char]) -> Result<(u8, Waveform, f32), String> {
        let mut octave_str = String::new();
        let mut waveform_char: Option<char> = None;
        let mut volume_char: Option<char> = None;
        let mut i = 0;

        while i < remaining_chars.len() {
            let ch = remaining_chars[i];

            if ch.is_ascii_digit() {
                octave_str.push(ch);
                i += 1;
            } else if matches!(ch, 'q' | 't' | 's' | 'p') {
                waveform_char = Some(ch);
                i += 1;
                break;
            } else if ch == '.' {
                if i + 1 < remaining_chars.len() && remaining_chars[i + 1].is_ascii_digit() {
                    volume_char = Some(remaining_chars[i + 1]);
                    i += 2;
                    break;
                } else {
                    return Err("Volume must be specified as .N where N is 0-9".to_string());
                }
            } else {
                return Err(format!("Unexpected character: '{ch}'"));
            }
        }

        if i < remaining_chars.len() {
            let ch = remaining_chars[i];

            if ch == '.' {
                if i + 1 < remaining_chars.len() && remaining_chars[i + 1].is_ascii_digit() {
                    volume_char = Some(remaining_chars[i + 1]);
                } else {
                    return Err("Volume must be specified as .N where N is 0-9".to_string());
                }
            } else {
                return Err(format!("Unexpected character after waveform: '{ch}'"));
            }
        }

        let octave: u8 = if octave_str.is_empty() {
            4
        } else {
            octave_str
                .parse()
                .map_err(|_| format!("Invalid octave number: '{octave_str}'"))?
        };

        if !(1..=8).contains(&octave) {
            return Err(format!("Octave out of range (1-8): {octave}"));
        }

        let waveform = match waveform_char {
            Some('q') => Waveform::Square,
            Some('t') => Waveform::Triangle,
            Some('s') => Waveform::Sawtooth,
            Some('p') => Waveform::Pulse,
            None => Waveform::default(),
            _ => return Err(format!("Invalid waveform: '{}'", waveform_char.unwrap())),
        };

        let volume = match volume_char {
            Some(ch) if ch.is_ascii_digit() => {
                let digit = ch.to_digit(10).unwrap() as f32;
                digit / 10.0
            }
            None => 1.0,
            _ => return Err(format!("Invalid volume: '{}'", volume_char.unwrap())),
        };

        Ok((octave, waveform, volume))
    }

    fn parse_notes(&self, input: &str) -> Result<Vec<Note>, String> {
        let mut notes = Vec::new();
        let mut current_token = String::new();
        let mut in_paren = false;
        let mut skip_next = false;

        for ch in input.chars() {
            if ch == '(' {
                in_paren = true;
                current_token.push(ch);
            } else if ch == ')' {
                in_paren = false;
                current_token.push(ch);
            } else if ch.is_whitespace() {
                if in_paren {
                    current_token.push(ch);
                } else if !current_token.is_empty() {
                    if current_token.starts_with("--") {
                        skip_next = true;
                    } else if skip_next {
                        skip_next = false;
                    } else {
                        notes.push(self.parse_note(&current_token)?);
                    }
                    current_token.clear();
                }
            } else {
                current_token.push(ch);
            }
        }

        if !current_token.is_empty() && !current_token.starts_with("--") && !skip_next {
            notes.push(self.parse_note(&current_token)?);
        }

        Ok(notes)
    }
}

pub struct MidiEngine {
    _stream: OutputStream,
    stream_handle: OutputStreamHandle,
    bpm: u16,
    current_sink: Option<Sink>,
}

impl MidiEngine {
    pub fn new(bpm: u16) -> Result<Self, String> {
        let (stream, stream_handle) = OutputStream::try_default()
            .map_err(|e| format!("Failed to create audio output stream: {e}"))?;

        Ok(Self {
            _stream: stream,
            stream_handle,
            bpm,
            current_sink: None,
        })
    }

    /// Validates MIDI composition syntax without requiring audio output.
    /// This is useful for server-side validation on headless systems.
    pub fn validate_midi_composition(input: &str) -> Result<Vec<Note>, String> {
        // Create a temporary parser with default BPM (doesn't need audio)
        let parser = MidiParser { bpm: 120 };
        parser.parse_notes(input)
    }

    pub fn parse_note(&self, input: &str) -> Result<Note, String> {
        let input = input.trim();
        if input.is_empty() {
            return Err("Empty note string".to_string());
        }

        let mut chars = input.chars().peekable();
        let mut duration_str = String::new();

        while let Some(&ch) = chars.peek() {
            if ch.is_ascii_digit() {
                duration_str.push(chars.next().unwrap());
            } else {
                break;
            }
        }

        if duration_str.is_empty() {
            return Err(format!("Missing duration in note: '{input}'"));
        }

        let duration_value: u8 = duration_str
            .parse()
            .map_err(|_| format!("Invalid duration number: '{duration_str}'"))?;

        if let Some(&'(') = chars.peek() {
            return self.parse_arpeggio(duration_value, &mut chars);
        }

        let mut is_sharp = false;
        if let Some(&'#') = chars.peek() {
            is_sharp = true;
            chars.next();
        }

        let note_char = chars
            .next()
            .ok_or_else(|| format!("Missing note letter in: '{input}'"))?
            .to_ascii_lowercase();

        if note_char == '-' {
            let duration = self.duration_to_seconds(duration_value);
            return Ok(Note {
                pitch: None,
                duration,
                waveform: Waveform::default(),
                volume: 1.0,
                arpeggio_notes: Vec::new(),
                adsr: false,
                vibrato: false,
            });
        }

        if !matches!(note_char, 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g') {
            return Err(format!("Invalid note letter: '{note_char}'"));
        }

        let remaining_chars: Vec<char> = chars.collect();
        let (octave, waveform, volume) = Self::parse_note_modifiers(&remaining_chars)?;

        let midi_note = self.note_to_midi_number(note_char, octave, is_sharp);
        let duration = self.duration_to_seconds(duration_value);

        Ok(Note {
            pitch: Some(midi_note),
            duration,
            waveform,
            volume,
            arpeggio_notes: Vec::new(),
            adsr: false,
            vibrato: false,
        })
    }

    pub fn parse_notes(&self, input: &str) -> Result<Vec<Note>, String> {
        let mut notes = Vec::new();
        let mut current_token = String::new();
        let mut in_paren = false;
        let mut skip_next = false;

        for ch in input.chars() {
            if ch == '(' {
                in_paren = true;
                current_token.push(ch);
            } else if ch == ')' {
                in_paren = false;
                current_token.push(ch);
            } else if ch.is_whitespace() {
                if in_paren {
                    current_token.push(ch);
                } else if !current_token.is_empty() {
                    // Skip flags (starting with --) and their arguments
                    if current_token.starts_with("--") {
                        skip_next = true;
                    } else if skip_next {
                        skip_next = false;
                    } else {
                        notes.push(self.parse_note(&current_token)?);
                    }
                    current_token.clear();
                }
            } else {
                current_token.push(ch);
            }
        }

        if !current_token.is_empty() && !current_token.starts_with("--") && !skip_next {
            notes.push(self.parse_note(&current_token)?);
        }

        Ok(notes)
    }

    fn parse_note_modifiers(remaining_chars: &[char]) -> Result<(u8, Waveform, f32), String> {
        let mut octave_str = String::new();
        let mut waveform_char: Option<char> = None;
        let mut volume_char: Option<char> = None;
        let mut i = 0;

        while i < remaining_chars.len() {
            let ch = remaining_chars[i];

            if ch.is_ascii_digit() {
                octave_str.push(ch);
                i += 1;
            } else if matches!(ch, 'q' | 't' | 's' | 'p') {
                waveform_char = Some(ch);
                i += 1;
                break;
            } else if ch == '.' {
                if i + 1 < remaining_chars.len() && remaining_chars[i + 1].is_ascii_digit() {
                    volume_char = Some(remaining_chars[i + 1]);
                    i += 2;
                    break;
                } else {
                    return Err("Volume must be specified as .N where N is 0-9".to_string());
                }
            } else {
                return Err(format!("Unexpected character: '{ch}'"));
            }
        }

        if i < remaining_chars.len() {
            let ch = remaining_chars[i];

            if ch == '.' {
                if i + 1 < remaining_chars.len() && remaining_chars[i + 1].is_ascii_digit() {
                    volume_char = Some(remaining_chars[i + 1]);
                } else {
                    return Err("Volume must be specified as .N where N is 0-9".to_string());
                }
            } else {
                return Err(format!("Unexpected character after waveform: '{ch}'"));
            }
        }

        let octave: u8 = if octave_str.is_empty() {
            4
        } else {
            octave_str
                .parse()
                .map_err(|_| format!("Invalid octave number: '{octave_str}'"))?
        };

        if !(1..=8).contains(&octave) {
            return Err(format!("Octave out of range (1-8): {octave}"));
        }

        let waveform = match waveform_char {
            Some('q') => Waveform::Square,
            Some('t') => Waveform::Triangle,
            Some('s') => Waveform::Sawtooth,
            Some('p') => Waveform::Pulse,
            None => Waveform::default(),
            _ => return Err(format!("Invalid waveform: '{}'", waveform_char.unwrap())),
        };

        let volume = match volume_char {
            Some(ch) if ch.is_ascii_digit() => {
                let digit = ch.to_digit(10).unwrap() as f32;
                digit / 10.0
            }
            None => 1.0,
            _ => return Err(format!("Invalid volume: '{}'", volume_char.unwrap())),
        };

        Ok((octave, waveform, volume))
    }

    fn parse_arpeggio(&self, duration_value: u8, chars: &mut std::iter::Peekable<std::str::Chars>) -> Result<Note, String> {
        chars.next();

        let mut content = String::new();
        let mut found_close = false;

        for ch in chars.by_ref() {
            if ch == ')' {
                found_close = true;
                break;
            }
            content.push(ch);
        }

        if !found_close {
            return Err("Missing closing parenthesis in arpeggio".to_string());
        }

        let note_strings: Vec<&str> = content.split_whitespace().collect();
        if note_strings.is_empty() {
            return Err("Empty arpeggio".to_string());
        }

        let mut arpeggio_notes = Vec::new();
        for note_str in note_strings {
            let mut note_chars = note_str.chars();

            if let Some(first_char) = note_chars.next() {
                if first_char == '#' {
                    if let Some(note_char) = note_chars.next() {
                        let midi_note = self.note_to_midi_number(note_char, 4, true);
                        arpeggio_notes.push(midi_note);
                    } else {
                        return Err(format!("Invalid arpeggio note: '{note_str}'"));
                    }
                } else {
                    let note_char = first_char.to_ascii_lowercase();
                    if !matches!(note_char, 'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g') {
                        return Err(format!("Invalid arpeggio note: '{note_char}'"));
                    }

                    let octave_str: String = note_chars.collect();
                    let octave: u8 = if octave_str.is_empty() {
                        4
                    } else {
                        octave_str.parse().map_err(|_| format!("Invalid octave in arpeggio: '{octave_str}'"))?
                    };

                    let midi_note = self.note_to_midi_number(note_char, octave, false);
                    arpeggio_notes.push(midi_note);
                }
            }
        }

        let remaining: Vec<char> = chars.collect();
        let mut waveform_char: Option<char> = None;
        let mut volume_char: Option<char> = None;
        let mut i = 0;

        while i < remaining.len() {
            let ch = remaining[i];
            if matches!(ch, 'q' | 't' | 's' | 'p') {
                waveform_char = Some(ch);
                i += 1;
            } else if ch == '.' {
                if i + 1 < remaining.len() && remaining[i + 1].is_ascii_digit() {
                    volume_char = Some(remaining[i + 1]);
                    break;
                } else {
                    return Err("Volume must be specified as .N where N is 0-9".to_string());
                }
            } else {
                return Err(format!("Unexpected character in arpeggio modifiers: '{ch}'"));
            }
        }

        let waveform = match waveform_char {
            Some('q') => Waveform::Square,
            Some('t') => Waveform::Triangle,
            Some('s') => Waveform::Sawtooth,
            Some('p') => Waveform::Pulse,
            None => Waveform::default(),
            _ => return Err(format!("Invalid waveform: '{}'", waveform_char.unwrap())),
        };

        let volume = match volume_char {
            Some(ch) if ch.is_ascii_digit() => {
                let digit = ch.to_digit(10).unwrap() as f32;
                digit / 10.0
            }
            None => 1.0,
            _ => return Err(format!("Invalid volume: '{}'", volume_char.unwrap())),
        };

        let duration = self.duration_to_seconds(duration_value);

        Ok(Note {
            pitch: None,
            duration,
            waveform,
            volume,
            arpeggio_notes,
            adsr: false,
            vibrato: false,
        })
    }

    fn note_to_midi_number(&self, note: char, octave: u8, sharp: bool) -> u8 {
        let base = match note {
            'c' => 0,
            'd' => 2,
            'e' => 4,
            'f' => 5,
            'g' => 7,
            'a' => 9,
            'b' => 11,
            _ => 0,
        };

        let sharp_offset = if sharp { 1 } else { 0 };
        let midi_note = 12 + (octave as i32 * 12) + base + sharp_offset;

        midi_note.clamp(0, 127) as u8
    }

    fn duration_to_seconds(&self, duration: u8) -> f32 {
        let quarter_note_duration = 60.0 / self.bpm as f32;
        (4.0 / duration as f32) * quarter_note_duration
    }

    fn midi_to_frequency(&self, midi_note: u8) -> f32 {
        440.0 * 2.0_f32.powf((midi_note as f32 - 69.0) / 12.0)
    }

    pub fn play_notes(&mut self, notes: &[Note]) -> Result<(), String> {
        let sample_rate = 48000_u32;
        let mut all_samples = Vec::new();

        for note in notes {
            let note_samples = self.generate_note_samples(note);
            all_samples.extend(note_samples);
        }

        let source = SamplesBuffer::new(1, sample_rate, all_samples);
        let sink = Sink::try_new(&self.stream_handle)
            .map_err(|e| format!("Failed to create audio sink: {e}"))?;

        sink.append(source);
        sink.sleep_until_end();

        Ok(())
    }

    pub fn play_notes_looping(&mut self, notes: &[Note]) -> Result<(), String> {
        self.stop();

        let sample_rate = 48000_u32;
        let mut all_samples = Vec::new();

        for note in notes {
            let note_samples = self.generate_note_samples(note);
            all_samples.extend(note_samples);
        }

        let source = SamplesBuffer::new(1, sample_rate, all_samples);
        let looping_source = source.repeat_infinite();

        let sink = Sink::try_new(&self.stream_handle)
            .map_err(|e| format!("Failed to create audio sink: {e}"))?;

        sink.append(looping_source);
        self.current_sink = Some(sink);

        Ok(())
    }

    pub fn stop(&mut self) {
        if let Some(sink) = self.current_sink.take() {
            sink.stop();
        }
    }

    fn apply_adsr_envelope(sample_index: usize, total_samples: usize) -> f32 {
        let t = sample_index as f32 / total_samples as f32;

        // ADSR parameters (as fraction of total duration)
        let attack_time = 0.05;   // 5% of note
        let decay_time = 0.15;    // 15% of note
        let sustain_level = 0.7;  // 70% volume
        let release_time = 0.20;  // 20% of note

        if t < attack_time {
            // Attack: fade in
            t / attack_time
        } else if t < attack_time + decay_time {
            // Decay: fade to sustain level
            let decay_progress = (t - attack_time) / decay_time;
            1.0 - (1.0 - sustain_level) * decay_progress
        } else if t < 1.0 - release_time {
            // Sustain: hold at sustain level
            sustain_level
        } else {
            // Release: fade out
            let release_progress = (t - (1.0 - release_time)) / release_time;
            sustain_level * (1.0 - release_progress)
        }
    }

    fn apply_vibrato(t: f32, frequency: f32) -> f32 {
        // Vibrato parameters
        let vibrato_rate = 5.0;  // 5 Hz wobble
        let vibrato_depth = 0.02; // 2% pitch variation

        let vibrato_offset = (t * vibrato_rate * 2.0 * std::f32::consts::PI).sin() * vibrato_depth;
        frequency * (1.0 + vibrato_offset)
    }

    pub fn generate_note_samples(&self, note: &Note) -> Vec<f32> {
        let sample_rate = 48000;

        if !note.arpeggio_notes.is_empty() {
            let mut all_samples = Vec::new();
            let note_duration = note.duration / note.arpeggio_notes.len() as f32;

            for &arp_pitch in &note.arpeggio_notes {
                let frequency = self.midi_to_frequency(arp_pitch);
                let samples = self.generate_waveform_samples(
                    frequency,
                    note_duration,
                    note.volume,
                    note.waveform,
                    note.adsr,
                    note.vibrato,
                );
                all_samples.extend(samples);
            }

            all_samples
        } else if let Some(pitch) = note.pitch {
            let frequency = self.midi_to_frequency(pitch);
            self.generate_waveform_samples(
                frequency,
                note.duration,
                note.volume,
                note.waveform,
                note.adsr,
                note.vibrato,
            )
        } else {
            let num_samples = (sample_rate as f32 * note.duration) as usize;
            vec![0.0_f32; num_samples]
        }
    }

    fn generate_waveform_samples(
        &self,
        frequency: f32,
        duration: f32,
        volume: f32,
        waveform: Waveform,
        adsr: bool,
        vibrato: bool,
    ) -> Vec<f32> {
        let sample_rate = 48000;
        let num_samples = (sample_rate as f32 * duration) as usize;
        let amplitude = 0.2 * volume;

        (0..num_samples)
            .map(|i| {
                let t = i as f32 / sample_rate as f32;
                let freq = if vibrato {
                    Self::apply_vibrato(t, frequency)
                } else {
                    frequency
                };
                let envelope = if adsr {
                    Self::apply_adsr_envelope(i, num_samples)
                } else {
                    1.0
                };

                let sample = match waveform {
                    Waveform::Square => {
                        let wave = (t * freq * 2.0 * std::f32::consts::PI).sin();
                        if wave >= 0.0 {
                            amplitude
                        } else {
                            -amplitude
                        }
                    }
                    Waveform::Triangle => {
                        let phase = (t * freq) % 1.0;
                        let wave = if phase < 0.5 {
                            4.0 * phase - 1.0
                        } else {
                            3.0 - 4.0 * phase
                        };
                        wave * amplitude
                    }
                    Waveform::Sawtooth => {
                        let phase = (t * freq) % 1.0;
                        let wave = 2.0 * phase - 1.0;
                        wave * amplitude
                    }
                    Waveform::Pulse => {
                        let duty_cycle = 0.25;
                        let phase = (t * freq) % 1.0;
                        let wave = if phase < duty_cycle { 1.0 } else { -1.0 };
                        wave * amplitude
                    }
                };

                sample * envelope
            })
            .collect()
    }

    pub fn play_channels(&mut self, channels: &[Vec<Note>]) -> Result<(), String> {
        if channels.is_empty() {
            return Err("No channels to play".to_string());
        }

        let sample_rate = 48000_u32;
        let mut channel_samples: Vec<Vec<f32>> = Vec::new();
        let mut max_length = 0;

        for channel_notes in channels {
            let mut channel_buffer = Vec::new();

            for note in channel_notes {
                let note_samples = self.generate_note_samples(note);
                channel_buffer.extend(note_samples);
            }

            max_length = max_length.max(channel_buffer.len());
            channel_samples.push(channel_buffer);
        }

        let mut mixed_samples = vec![0.0_f32; max_length];

        for channel_buffer in &channel_samples {
            for (i, &sample) in channel_buffer.iter().enumerate() {
                mixed_samples[i] += sample;
            }
        }

        let source = SamplesBuffer::new(1, sample_rate, mixed_samples);
        let sink = Sink::try_new(&self.stream_handle)
            .map_err(|e| format!("Failed to create audio sink: {e}"))?;

        sink.append(source);
        sink.sleep_until_end();

        Ok(())
    }

    pub fn parse_and_play(&mut self, input: &str) -> Result<(), String> {
        use crate::midi_composer::channels::parse_channels;

        let (channels, bpm) = parse_channels(input)?;

        if let Some(bpm_val) = bpm {
            *self = Self::new(bpm_val)?;
        }

        if channels.len() == 1 && !input.contains("--channel") {
            let channel = &channels[0];
            let mut notes = self.parse_notes(&channel.composition)?;

            for note in &mut notes {
                if channel.volume.is_some() && note.volume == 1.0 {
                    note.volume = channel.volume.unwrap_or(1.0);
                }
                note.adsr = channel.adsr;
                note.vibrato = channel.vibrato;
            }

            self.play_notes(&notes)
        } else {
            let mut all_channel_notes = Vec::new();

            for channel in &channels {
                let mut notes = self.parse_notes(&channel.composition)?;

                for note in &mut notes {
                    if channel.volume.is_some() && note.volume == 1.0 {
                        note.volume = channel.volume.unwrap_or(1.0);
                    }
                    note.adsr = channel.adsr;
                    note.vibrato = channel.vibrato;
                }
                all_channel_notes.push(notes);
            }

            self.play_channels(&all_channel_notes)
        }
    }

    pub fn parse_and_play_looping(&mut self, input: &str) -> Result<(), String> {
        use crate::midi_composer::channels::parse_channels;

        let (channels, bpm) = parse_channels(input)?;

        if let Some(bpm_val) = bpm {
            *self = Self::new(bpm_val)?;
        }

        if channels.len() == 1 && !input.contains("--channel") {
            let channel = &channels[0];
            let mut notes = self.parse_notes(&channel.composition)?;

            for note in &mut notes {
                if channel.volume.is_some() && note.volume == 1.0 {
                    note.volume = channel.volume.unwrap_or(1.0);
                }
                note.adsr = channel.adsr;
                note.vibrato = channel.vibrato;
            }

            self.play_notes_looping(&notes)
        } else {
            Err("Looping playback is only supported for single-channel compositions".to_string())
        }
    }

    pub fn generate_wav_bytes(&mut self, composition: &str) -> Result<Vec<u8>, String> {
        use crate::midi_composer::channels::parse_channels;

        let (channels, bpm) = parse_channels(composition)?;

        if let Some(bpm_val) = bpm {
            self.bpm = bpm_val;
        }

        if channels.len() == 1 && !composition.contains("--channel") {
            let channel = &channels[0];
            let mut notes = self.parse_notes(&channel.composition)?;

            for note in &mut notes {
                if channel.volume.is_some() && note.volume == 1.0 {
                    note.volume = channel.volume.unwrap_or(1.0);
                }
                note.adsr = channel.adsr;
                note.vibrato = channel.vibrato;
            }

            let sample_rate = 48000_u32;
            let mut all_samples = Vec::new();

            for note in &notes {
                let note_samples = self.generate_note_samples(note);
                all_samples.extend(note_samples);
            }

            let mut wav_buffer = std::io::Cursor::new(Vec::new());
            let spec = hound::WavSpec {
                channels: 1,
                sample_rate,
                bits_per_sample: 16,
                sample_format: hound::SampleFormat::Int,
            };

            let mut writer = hound::WavWriter::new(&mut wav_buffer, spec)
                .map_err(|e| format!("Failed to create WAV writer: {e}"))?;

            for sample in all_samples {
                let amplitude = i16::MAX as f32;
                let sample_i16 = (sample * amplitude) as i16;
                writer.write_sample(sample_i16)
                    .map_err(|e| format!("Failed to write sample: {e}"))?;
            }

            writer.finalize()
                .map_err(|e| format!("Failed to finalize WAV: {e}"))?;

            Ok(wav_buffer.into_inner())
        } else {
            Err("Multi-channel compositions are not supported for WAV generation".to_string())
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn test_engine() -> MidiEngine {
        MidiEngine::new(120).unwrap()
    }

    #[test]
    fn test_parse_note_simple() {
        let engine = test_engine();
        let note = engine.parse_note("4c").unwrap();
        assert_eq!(note.pitch, Some(60));
        assert_eq!(note.duration, 0.5);
    }

    #[test]
    fn test_parse_note_with_octave() {
        let engine = test_engine();
        let note = engine.parse_note("16e2").unwrap();
        assert_eq!(note.pitch, Some(40));
        assert_eq!(note.duration, 0.125);
    }

    #[test]
    fn test_parse_note_with_sharp() {
        let engine = test_engine();
        let note = engine.parse_note("8#f").unwrap();
        assert_eq!(note.pitch, Some(66));
        assert_eq!(note.duration, 0.25);
    }

    #[test]
    fn test_parse_note_with_sharp_and_octave() {
        let engine = test_engine();
        let note = engine.parse_note("8#g2").unwrap();
        assert_eq!(note.pitch, Some(44));
        assert_eq!(note.duration, 0.25);
    }

    #[test]
    fn test_parse_rest() {
        let engine = test_engine();
        let note = engine.parse_note("2-").unwrap();
        assert_eq!(note.pitch, None);
        assert_eq!(note.duration, 1.0);
    }

    #[test]
    fn test_note_to_midi_number_c1() {
        let engine = test_engine();
        assert_eq!(engine.note_to_midi_number('c', 1, false), 24);
    }

    #[test]
    fn test_note_to_midi_number_a1() {
        let engine = test_engine();
        assert_eq!(engine.note_to_midi_number('a', 1, false), 33);
    }

    #[test]
    fn test_note_to_midi_number_middle_c() {
        let engine = test_engine();
        assert_eq!(engine.note_to_midi_number('c', 4, false), 60);
    }

    #[test]
    fn test_note_to_midi_number_with_sharp() {
        let engine = test_engine();
        assert_eq!(engine.note_to_midi_number('c', 1, true), 25);
    }

    #[test]
    fn test_duration_calculation_quarter_note() {
        let engine = test_engine();
        assert_eq!(engine.duration_to_seconds(4), 0.5);
    }

    #[test]
    fn test_duration_calculation_eighth_note() {
        let engine = test_engine();
        assert_eq!(engine.duration_to_seconds(8), 0.25);
    }

    #[test]
    fn test_duration_calculation_whole_note() {
        let engine = test_engine();
        assert_eq!(engine.duration_to_seconds(1), 2.0);
    }

    #[test]
    fn test_parse_notes_sequence() {
        let engine = test_engine();
        let notes = engine.parse_notes("4c 8d 16e2").unwrap();
        assert_eq!(notes.len(), 3);
        assert_eq!(notes[0].pitch, Some(60));
        assert_eq!(notes[1].pitch, Some(62));
        assert_eq!(notes[2].pitch, Some(40));
    }

    #[test]
    fn test_parse_note_modifiers_default() {
        let result = MidiEngine::parse_note_modifiers(&[]).unwrap();
        assert_eq!(result, (4, Waveform::Square, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_octave_only() {
        let result = MidiEngine::parse_note_modifiers(&['2']).unwrap();
        assert_eq!(result, (2, Waveform::Square, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_octave_8() {
        let result = MidiEngine::parse_note_modifiers(&['8']).unwrap();
        assert_eq!(result, (8, Waveform::Square, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_octave_out_of_range_low() {
        let result = MidiEngine::parse_note_modifiers(&['0']);
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_note_modifiers_octave_out_of_range_high() {
        let result = MidiEngine::parse_note_modifiers(&['9']);
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_note_modifiers_waveform_square() {
        let result = MidiEngine::parse_note_modifiers(&['q']).unwrap();
        assert_eq!(result, (4, Waveform::Square, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_waveform_triangle() {
        let result = MidiEngine::parse_note_modifiers(&['t']).unwrap();
        assert_eq!(result, (4, Waveform::Triangle, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_waveform_sawtooth() {
        let result = MidiEngine::parse_note_modifiers(&['s']).unwrap();
        assert_eq!(result, (4, Waveform::Sawtooth, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_waveform_pulse() {
        let result = MidiEngine::parse_note_modifiers(&['p']).unwrap();
        assert_eq!(result, (4, Waveform::Pulse, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_volume_0() {
        let result = MidiEngine::parse_note_modifiers(&['.', '0']).unwrap();
        assert_eq!(result, (4, Waveform::Square, 0.0));
    }

    #[test]
    fn test_parse_note_modifiers_volume_5() {
        let result = MidiEngine::parse_note_modifiers(&['.', '5']).unwrap();
        assert_eq!(result, (4, Waveform::Square, 0.5));
    }

    #[test]
    fn test_parse_note_modifiers_volume_9() {
        let result = MidiEngine::parse_note_modifiers(&['.', '9']).unwrap();
        assert_eq!(result, (4, Waveform::Square, 0.9));
    }

    #[test]
    fn test_parse_note_modifiers_octave_and_waveform() {
        let result = MidiEngine::parse_note_modifiers(&['2', 't']).unwrap();
        assert_eq!(result, (2, Waveform::Triangle, 1.0));
    }

    #[test]
    fn test_parse_note_modifiers_octave_and_volume() {
        let result = MidiEngine::parse_note_modifiers(&['3', '.', '7']).unwrap();
        assert_eq!(result, (3, Waveform::Square, 0.7));
    }

    #[test]
    fn test_parse_note_modifiers_all_combined() {
        let result = MidiEngine::parse_note_modifiers(&['2', 't', '.', '5']).unwrap();
        assert_eq!(result, (2, Waveform::Triangle, 0.5));
    }

    #[test]
    fn test_parse_note_modifiers_all_combined_pulse() {
        let result = MidiEngine::parse_note_modifiers(&['6', 'p', '.', '3']).unwrap();
        assert_eq!(result, (6, Waveform::Pulse, 0.3));
    }

    #[test]
    fn test_parse_note_modifiers_invalid_character() {
        let result = MidiEngine::parse_note_modifiers(&['x']);
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_note_modifiers_invalid_waveform() {
        let result = MidiEngine::parse_note_modifiers(&['w']);
        assert!(result.is_err());
    }

    #[test]
    fn test_invalid_note_missing_duration() {
        let engine = test_engine();
        assert!(engine.parse_note("c").is_err());
    }

    #[test]
    fn test_invalid_note_letter() {
        let engine = test_engine();
        assert!(engine.parse_note("4x").is_err());
    }

    #[test]
    fn test_invalid_octave_range() {
        let engine = test_engine();
        assert!(engine.parse_note("4c9").is_err());
    }

    #[test]
    fn test_parse_notes_with_flags() {
        let engine = test_engine();
        let notes = engine.parse_notes("--bpm 150 4c 4e 4g --volume 80 4c").unwrap();
        assert_eq!(notes.len(), 4);
        assert_eq!(notes[0].pitch, Some(60));
        assert_eq!(notes[1].pitch, Some(64));
        assert_eq!(notes[2].pitch, Some(67));
        assert_eq!(notes[3].pitch, Some(60));
    }

    #[test]
    fn test_parse_arpeggio_basic() {
        let engine = test_engine();
        let note = engine.parse_note("4(c e g)").unwrap();
        assert_eq!(note.arpeggio_notes.len(), 3);
        assert_eq!(note.arpeggio_notes[0], 60);
        assert_eq!(note.arpeggio_notes[1], 64);
        assert_eq!(note.arpeggio_notes[2], 67);
    }

    #[test]
    fn test_parse_arpeggio_with_sharps() {
        let engine = test_engine();
        let note = engine.parse_note("4(#c #f a)").unwrap();
        assert_eq!(note.arpeggio_notes.len(), 3);
        assert_eq!(note.arpeggio_notes[0], 61);
        assert_eq!(note.arpeggio_notes[1], 66);
        assert_eq!(note.arpeggio_notes[2], 69);
    }

    #[test]
    fn test_parse_arpeggio_with_octaves() {
        let engine = test_engine();
        let note = engine.parse_note("4(c2 e3 g4)").unwrap();
        assert_eq!(note.arpeggio_notes.len(), 3);
        assert_eq!(note.arpeggio_notes[0], 36);
        assert_eq!(note.arpeggio_notes[1], 52);
        assert_eq!(note.arpeggio_notes[2], 67);
    }

    #[test]
    fn test_parse_arpeggio_duration_division() {
        let engine = test_engine();
        let note = engine.parse_note("4(c e g)").unwrap();
        let expected_duration = engine.duration_to_seconds(4);
        assert_eq!(note.duration, expected_duration);
    }

    #[test]
    fn test_parse_arpeggio_missing_closing_paren() {
        let engine = test_engine();
        let result = engine.parse_note("4(c e g");
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_arpeggio_empty() {
        let engine = test_engine();
        let result = engine.parse_note("4()");
        assert!(result.is_err());
    }

    #[test]
    fn test_parse_note_with_waveform_and_volume() {
        let engine = test_engine();
        let note = engine.parse_note("4ct.5").unwrap();
        assert_eq!(note.pitch, Some(60));
        assert_eq!(note.waveform, Waveform::Triangle);
        assert_eq!(note.volume, 0.5);
    }

    #[test]
    fn test_parse_note_with_all_features() {
        let engine = test_engine();
        let note = engine.parse_note("8#f3s.7").unwrap();
        assert_eq!(note.pitch, Some(54));
        assert_eq!(note.waveform, Waveform::Sawtooth);
        assert_eq!(note.volume, 0.7);
        let expected_duration = engine.duration_to_seconds(8);
        assert_eq!(note.duration, expected_duration);
    }

    #[test]
    fn test_nokia_ringtone() {
        let engine = test_engine();
        let input = "16e2 16d2 8#f 8#g";
        let notes = engine.parse_notes(input).unwrap();

        println!("\nNokia ringtone notes:");
        for (i, note) in notes.iter().enumerate() {
            if let Some(pitch) = note.pitch {
                let freq = 440.0 * 2.0_f32.powf((pitch as f32 - 69.0) / 12.0);
                println!("  Note {}: MIDI {} ({:.1} Hz)", i + 1, pitch, freq);
            }
        }

        assert_eq!(notes[0].pitch, Some(40));
        assert_eq!(notes[1].pitch, Some(38));
        assert_eq!(notes[2].pitch, Some(66));
        assert_eq!(notes[3].pitch, Some(68));
    }

}

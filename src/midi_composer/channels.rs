#[derive(Debug, Clone, PartialEq)]
pub struct ChannelConfig {
    pub composition: String,
    pub volume: Option<f32>,
    pub adsr: bool,
    pub vibrato: bool,
}

#[derive(Default)]
struct GlobalFlags {
    volume: Option<f32>,
    adsr: bool,
    vibrato: bool,
}

pub fn parse_channels(input: &str) -> Result<(Vec<ChannelConfig>, Option<u16>), String> {
    let tokens: Vec<&str> = input.split_whitespace().collect();

    if tokens.is_empty() {
        return Err("Empty input".to_string());
    }

    let mut global_flags = GlobalFlags::default();
    let mut current_flags = GlobalFlags::default();
    let mut bpm: Option<u16> = None;
    let mut channels = Vec::new();
    let mut i = 0;
    let mut found_channel = false;

    while i < tokens.len() {
        let token = tokens[i];

        match token {
            "--volume" => {
                if i + 1 >= tokens.len() {
                    return Err("Missing value for --volume".to_string());
                }
                let vol = tokens[i + 1]
                    .parse::<f32>()
                    .map_err(|_| format!("Invalid volume value: '{}'", tokens[i + 1]))?;

                if !(0.0..=1.0).contains(&vol) {
                    return Err(format!("Volume must be between 0.0 and 1.0, got {vol}"));
                }

                if found_channel {
                    current_flags.volume = Some(vol);
                } else {
                    global_flags.volume = Some(vol);
                }
                i += 2;
            }
            "--bpm" => {
                if i + 1 >= tokens.len() {
                    return Err("Missing value for --bpm".to_string());
                }
                let bpm_val = tokens[i + 1]
                    .parse::<u16>()
                    .map_err(|_| format!("Invalid BPM value: '{}'", tokens[i + 1]))?;

                if !(1..=300).contains(&bpm_val) {
                    return Err(format!("BPM must be between 1 and 300, got {bpm_val}"));
                }

                bpm = Some(bpm_val);
                i += 2;
            }
            "--adsr" => {
                if found_channel {
                    current_flags.adsr = true;
                } else {
                    global_flags.adsr = true;
                }
                i += 1;
            }
            "--vibrato" => {
                if found_channel {
                    current_flags.vibrato = true;
                } else {
                    global_flags.vibrato = true;
                }
                i += 1;
            }
            "--channel" => {
                if found_channel && channels.is_empty() {
                    return Err("--channel found but no composition provided".to_string());
                }

                found_channel = true;
                current_flags = GlobalFlags::default();
                i += 1;
            }
            _ => {
                if found_channel {
                    let mut composition = String::new();
                    while i < tokens.len() && !tokens[i].starts_with("--") {
                        if !composition.is_empty() {
                            composition.push(' ');
                        }
                        composition.push_str(tokens[i]);
                        i += 1;
                    }

                    if composition.is_empty() {
                        return Err("Empty composition for channel".to_string());
                    }

                    let volume = current_flags.volume.or(global_flags.volume);
                    let adsr = current_flags.adsr || global_flags.adsr;
                    let vibrato = current_flags.vibrato || global_flags.vibrato;

                    channels.push(ChannelConfig {
                        composition,
                        volume,
                        adsr,
                        vibrato,
                    });

                    found_channel = false;
                } else {
                    let mut composition = String::new();
                    while i < tokens.len() && !tokens[i].starts_with("--") {
                        if !composition.is_empty() {
                            composition.push(' ');
                        }
                        composition.push_str(tokens[i]);
                        i += 1;
                    }

                    if !composition.is_empty() {
                        channels.push(ChannelConfig {
                            composition,
                            volume: global_flags.volume,
                            adsr: global_flags.adsr,
                            vibrato: global_flags.vibrato,
                        });
                    }
                }
            }
        }
    }

    if found_channel {
        return Err("--channel found but no composition provided".to_string());
    }

    if channels.is_empty() {
        return Err("No channels or compositions found".to_string());
    }

    Ok((channels, bpm))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_single_composition_no_flags() {
        let (channels, bpm) = parse_channels("4c 4e 4g").unwrap();
        assert_eq!(channels.len(), 1);
        assert_eq!(channels[0].composition, "4c 4e 4g");
        assert_eq!(channels[0].volume, None);
        assert!(!channels[0].adsr);
        assert!(!channels[0].vibrato);
        assert_eq!(bpm, None);
    }

    #[test]
    fn test_single_channel() {
        let (channels, bpm) = parse_channels("--channel 4c 4e 4g").unwrap();
        assert_eq!(channels.len(), 1);
        assert_eq!(channels[0].composition, "4c 4e 4g");
        assert_eq!(bpm, None);
    }

    #[test]
    fn test_two_channels() {
        let (channels, _) = parse_channels("--channel 4c 4e --channel 4g 4b").unwrap();
        assert_eq!(channels.len(), 2);
        assert_eq!(channels[0].composition, "4c 4e");
        assert_eq!(channels[1].composition, "4g 4b");
    }

    #[test]
    fn test_global_volume() {
        let (channels, _) = parse_channels("--volume 0.5 --channel 4c --channel 4e").unwrap();
        assert_eq!(channels.len(), 2);
        assert_eq!(channels[0].volume, Some(0.5));
        assert_eq!(channels[1].volume, Some(0.5));
    }

    #[test]
    fn test_channel_specific_volume() {
        let (channels, _) = parse_channels("--volume 0.8 --channel 4c --channel --volume 0.3 4e").unwrap();
        assert_eq!(channels.len(), 2);
        assert_eq!(channels[0].volume, Some(0.8));
        assert_eq!(channels[1].volume, Some(0.3));
    }

    #[test]
    fn test_complex_flags() {
        let (channels, bpm) = parse_channels("--volume 0.9 --channel 4c --channel 4d --channel --volume 0.5 4e").unwrap();
        assert_eq!(channels.len(), 3);
        assert_eq!(channels[0].volume, Some(0.9));
        assert_eq!(channels[1].volume, Some(0.9));
        assert_eq!(channels[2].volume, Some(0.5));
        assert_eq!(bpm, None);
    }

    #[test]
    fn test_global_bpm() {
        let (channels, bpm) = parse_channels("--bpm 140 --channel 4c --channel 4e").unwrap();
        assert_eq!(bpm, Some(140));
        assert_eq!(channels.len(), 2);
    }

    #[test]
    fn test_adsr_and_vibrato() {
        let (channels, _) = parse_channels("--adsr --channel 4c --channel --vibrato 4e").unwrap();
        assert_eq!(channels.len(), 2);
        assert!(channels[0].adsr);
        assert!(!channels[0].vibrato);
        assert!(channels[1].adsr);
        assert!(channels[1].vibrato);
    }

    #[test]
    fn test_composition_without_channel_flag() {
        let (channels, _) = parse_channels("4c 4e 4g").unwrap();
        assert_eq!(channels.len(), 1);
        assert_eq!(channels[0].composition, "4c 4e 4g");
    }

    #[test]
    fn test_composition_with_global_flags() {
        let (channels, bpm) = parse_channels("--volume 0.7 --bpm 120 --adsr 4c 4e 4g").unwrap();
        assert_eq!(channels.len(), 1);
        assert_eq!(channels[0].volume, Some(0.7));
        assert!(channels[0].adsr);
        assert_eq!(bpm, Some(120));
    }

    #[test]
    fn test_invalid_volume_range() {
        assert!(parse_channels("--volume 1.5 --channel 4c").is_err());
        assert!(parse_channels("--volume -0.1 --channel 4c").is_err());
    }

    #[test]
    fn test_invalid_bpm_range() {
        assert!(parse_channels("--bpm 0 --channel 4c").is_err());
        assert!(parse_channels("--bpm 301 --channel 4c").is_err());
    }

    #[test]
    fn test_missing_flag_value() {
        assert!(parse_channels("--volume").is_err());
        assert!(parse_channels("--bpm").is_err());
    }

    #[test]
    fn test_channel_without_composition() {
        assert!(parse_channels("--channel").is_err());
        assert!(parse_channels("--channel --channel 4c").is_err());
    }

    #[test]
    fn test_empty_input() {
        assert!(parse_channels("").is_err());
    }
}

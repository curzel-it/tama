use std::io::{self, Write};
use tama::midi_composer::MidiEngine;

fn main() -> io::Result<()> {
    println!("╔════════════════════════════════════════════════════════╗");
    println!("║          Nokia 3310 MIDI Composer                     ║");
    println!("║          🎮 Retro Game Audio Edition 🎵               ║");
    println!("╚════════════════════════════════════════════════════════╝");
    println!();
    println!("Note Format: [duration][#][note][octave][wave][.volume]");
    println!();
    println!("┌──────────────────────────────────────────────────────┐");
    println!("│ Duration  │ 1, 2, 4, 8, 16, 32 (whole to 32nd note) │");
    println!("│ Sharp     │ # (optional, e.g., #f = F#)              │");
    println!("│ Note      │ c d e f g a b  OR  - (rest)              │");
    println!("│ Octave    │ 1-8 (optional, default=4, middle range)  │");
    println!("│ Waveform  │ q=square t=triangle s=sawtooth p=pulse   │");
    println!("│ Volume    │ .0-.9 (optional, default=1.0 full vol)   │");
    println!("│ Arpeggio  │ 4(c e g) = rapid C-E-G cycling           │");
    println!("└──────────────────────────────────────────────────────┘");
    println!();
    println!("Italian Note Names: do=c re=d mi=e fa=f sol=g la=a si=b");
    println!();
    println!("Examples:");
    println!("  Basic:           4e 4g 4a");
    println!("  Waveforms:       4et 4gs 4ap");
    println!("  With volume:     4e.9 4g.7 4a.5");
    println!("  Full syntax:     8#f2t.5 = F# octave 2, triangle, 50% vol");
    println!("  Arpeggio:        4(c e g) 4(d f a) 4(e g b)");
    println!();
    println!("Multi-Channel Support:");
    println!("  Two channels:    --channel \"4c 4e\" --channel \"4g 4b\"");
    println!("  Global volume:   --volume 0.5 --channel \"4c\" --channel \"4e\"");
    println!("  Per-channel:     --channel \"4c\" --channel --volume 0.3 \"4e\"");
    println!();
    println!("Flags:");
    println!("  --bpm 140        Set tempo (default 120, range 1-300)");
    println!("  --volume 0.5     Set default volume for all notes/channels");
    println!("  --adsr           Enable ADSR envelope (smooth attack/release)");
    println!("  --vibrato        Enable vibrato effect (pitch wobble)");
    println!("  --channel        Define a channel (can be repeated)");
    println!();
    println!("Commands:  q = quit");
    println!();

    let default_bpm = 120;
    let mut engine = MidiEngine::new(default_bpm).map_err(io::Error::other)?;

    loop {
        print!("♪ > ");
        io::stdout().flush()?;

        let mut input = String::new();
        io::stdin().read_line(&mut input)?;

        let input = input.trim();

        if input.is_empty() {
            continue;
        }

        if input.eq_ignore_ascii_case("q") || input.eq_ignore_ascii_case("quit") {
            println!("Goodbye!");
            break;
        }

        match engine.parse_and_play(input) {
            Ok(()) => {
                println!("Done!");
            }
            Err(e) => {
                println!("Error: {e}");
            }
        }
    }

    Ok(())
}

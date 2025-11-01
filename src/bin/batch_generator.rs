use clap::Parser;
use rand::SeedableRng;
use rand_chacha::ChaCha8Rng;
use std::fs;
use std::path::Path;
use tama::procedural::midi_generator::{Complexity, MidiGenerator, MidiGeneratorConfig};
use tama::procedural::visualizer::{VisualizationStyle, Visualizer, VisualizerConfig};

#[derive(Parser, Debug)]
#[command(name = "batch_generator")]
#[command(about = "Generate procedural MIDI compositions with visualizations", long_about = None)]
struct Args {
    #[arg(short, long, default_value = "10")]
    count: u32,

    #[arg(short, long, default_value = "42")]
    seed: u64,

    #[arg(short, long, default_value = "generated_content")]
    output_dir: String,
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args = Args::parse();

    println!("Batch Content Generator");
    println!("=======================");
    println!("Count: {}", args.count);
    println!("Seed: {}", args.seed);
    println!("Output directory: {}", args.output_dir);
    println!();

    if !Path::new(&args.output_dir).exists() {
        fs::create_dir_all(&args.output_dir)?;
        println!("Created output directory: {}", args.output_dir);
    }

    let mut rng = ChaCha8Rng::seed_from_u64(args.seed);

    for i in 0..args.count {
        let complexity = match i % 3 {
            0 => Complexity::Simple,
            1 => Complexity::Medium,
            2 => Complexity::Complex,
            _ => unreachable!(),
        };

        let style = VisualizationStyle::from_index(i as usize);

        let midi_config = MidiGeneratorConfig::random(&mut rng, complexity);
        let midi_composition = MidiGenerator::generate(&midi_config, &mut rng);

        let viz_config = VisualizerConfig::random(&mut rng, style);
        let art = Visualizer::generate(&viz_config, &mut rng);

        // Generate pattern-based title
        let complexity_str = match complexity {
            Complexity::Simple => "Simple",
            Complexity::Medium => "Medium",
            Complexity::Complex => "Complex",
        };
        let style_str = match style {
            VisualizationStyle::Bars => "Bars",
            VisualizationStyle::Waveform => "Waveform",
            VisualizationStyle::Particles => "Particles",
            VisualizationStyle::Geometric => "Geometric",
            VisualizationStyle::Ripples => "Ripples",
            VisualizationStyle::Spiral => "Spiral",
        };
        let title = format!("{} {} #{}", style_str, complexity_str, i + 1);

        let content = format!("--- TITLE ---\n{title}\n--- MIDI ---\n{midi_composition}\n--- ART ---\n{art}");

        let filename = format!("{}/content_{:03}.txt", args.output_dir, i + 1);
        fs::write(&filename, content)?;

        if (i + 1) % 10 == 0 {
            println!("Generated {}/{} items", i + 1, args.count);
        }
    }

    println!();
    println!("✓ Successfully generated {} content files in {}/", args.count, args.output_dir);
    println!();
    println!("Complexity distribution:");
    println!("  - Simple: {} items", args.count.div_ceil(3));
    println!("  - Medium: {} items", (args.count + 1) / 3);
    println!("  - Complex: {} items", args.count / 3);
    println!();
    println!("Visualization styles distributed evenly across 6 types:");
    println!("  Bars, Waveform, Particles, Geometric, Ripples, Spiral");

    Ok(())
}

use clap::{Parser, Subcommand};
use image::GenericImageView;
use std::fs;
use std::path::PathBuf;
use tama::ascii_art_converter::{AsciiArtSheet, ImageConverter, TV_WIDTH, TV_HEIGHT};

#[derive(Parser)]
#[command(name = "ascii_art_converter")]
#[command(about = "ASCII Art Converter - Convert images and GIFs to ASCII art", long_about = None)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    #[command(about = "Convert a single PNG image to ASCII art (32x12 characters)")]
    Png {
        #[arg(short, long, help = "Input image path")]
        input: PathBuf,

        #[arg(short, long, help = "Output file path")]
        output: PathBuf,

        #[arg(long, help = "Treat transparent pixels as black instead of white")]
        transparency_as_black: bool,
    },

    #[command(about = "Convert a sprite sheet to ASCII art (32x12 characters per frame)")]
    Sprite {
        #[arg(short, long, help = "Input sprite sheet path")]
        input: PathBuf,

        #[arg(short, long, help = "Output file path")]
        output: PathBuf,

        #[arg(long, help = "Width of each frame in source image (pixels)")]
        sprite_width: u32,

        #[arg(long, help = "Height of each frame in source image (pixels)")]
        sprite_height: u32,

        #[arg(long, default_value = "0", help = "Starting X position in pixels")]
        start_x: u32,

        #[arg(long, default_value = "0", help = "Starting Y position in pixels")]
        start_y: u32,

        #[arg(long, help = "Number of frames to extract (default: all frames in row)")]
        num_frames: Option<u32>,

        #[arg(long, help = "Treat transparent pixels as black instead of white")]
        transparency_as_black: bool,
    },

    #[command(about = "Convert a GIF to ASCII art (32x12 characters per frame)")]
    Gif {
        #[arg(short, long, help = "Input GIF path")]
        input: PathBuf,

        #[arg(short, long, help = "Output file path")]
        output: PathBuf,

        #[arg(long, help = "Treat transparent pixels as black instead of white")]
        transparency_as_black: bool,
    },
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let cli = Cli::parse();

    match cli.command {
        Commands::Png {
            input,
            output,
            transparency_as_black,
        } => convert_png(input, output, transparency_as_black)?,

        Commands::Sprite {
            input,
            output,
            sprite_width,
            sprite_height,
            start_x,
            start_y,
            num_frames,
            transparency_as_black,
        } => convert_sprite(SpriteParams {
            input,
            output,
            sprite_width,
            sprite_height,
            start_x,
            start_y,
            num_frames,
            transparency_as_black,
        })?,

        Commands::Gif {
            input,
            output,
            transparency_as_black,
        } => convert_gif(input, output, transparency_as_black)?,
    }

    Ok(())
}

struct SpriteParams {
    input: PathBuf,
    output: PathBuf,
    sprite_width: u32,
    sprite_height: u32,
    start_x: u32,
    start_y: u32,
    num_frames: Option<u32>,
    transparency_as_black: bool,
}

fn convert_png(
    input: PathBuf,
    output: PathBuf,
    transparency_as_black: bool,
) -> Result<(), Box<dyn std::error::Error>> {
    println!("Converting PNG: {input:?}");

    let img = image::open(&input)?;
    let processed = ImageConverter::preprocess_image_smart(img, transparency_as_black);
    let ascii_art = ImageConverter::image_to_braille_string(&processed, TV_WIDTH, TV_HEIGHT);

    let mut sheet = AsciiArtSheet::new(TV_WIDTH, TV_HEIGHT);
    sheet.add_frame(ascii_art);

    fs::write(&output, sheet.to_string())?;

    println!("Saved ASCII art to: {output:?}");
    println!("Size: {TV_WIDTH}x{TV_HEIGHT}, 1 frame");

    Ok(())
}

fn convert_sprite(params: SpriteParams) -> Result<(), Box<dyn std::error::Error>> {
    println!("Converting sprite sheet: {:?}", params.input);

    let img = image::open(&params.input)?;
    let (total_width, _total_height) = img.dimensions();

    let available_width = total_width.saturating_sub(params.start_x);
    let max_frames_in_row = available_width / params.sprite_width;
    let frames_to_extract = params.num_frames.unwrap_or(max_frames_in_row);

    if frames_to_extract > max_frames_in_row {
        return Err(format!(
            "Requested {frames_to_extract} frames but only {max_frames_in_row} frames available in row"
        )
        .into());
    }

    println!(
        "Extracting {frames_to_extract} frames from row at y={}, starting at x={}",
        params.start_y, params.start_x
    );

    let target_width = (params.sprite_width / 2) as usize;
    let target_height = (params.sprite_height / 4) as usize;

    let mut sheet = AsciiArtSheet::new(target_width, target_height);

    for frame_idx in 0..frames_to_extract {
        let x = params.start_x + (frame_idx * params.sprite_width);
        let y = params.start_y;

        let frame_img = img.crop_imm(x, y, params.sprite_width, params.sprite_height);
        let processed = ImageConverter::preprocess_image(
            frame_img,
            target_width,
            target_height,
            params.transparency_as_black,
        );
        let ascii_art = ImageConverter::image_to_braille_string(&processed, target_width, target_height);

        sheet.add_frame(ascii_art);
    }

    fs::write(&params.output, sheet.to_string())?;

    println!("Saved ASCII art to: {:?}", params.output);
    println!(
        "Size: {}x{}, {} frames",
        target_width,
        target_height,
        sheet.frame_count()
    );

    Ok(())
}

fn convert_gif(
    input: PathBuf,
    output: PathBuf,
    transparency_as_black: bool,
) -> Result<(), Box<dyn std::error::Error>> {
    println!("Converting GIF: {input:?}");

    let file = fs::File::open(&input)?;
    let mut decoder = gif::DecodeOptions::new();
    decoder.set_color_output(gif::ColorOutput::RGBA);

    let mut decoder = decoder.read_info(file)?;
    let mut sheet = AsciiArtSheet::new(TV_WIDTH, TV_HEIGHT);

    let gif_width = decoder.width();
    let gif_height = decoder.height();

    while let Some(frame) = decoder.read_next_frame()? {
        let buffer = frame.buffer.to_vec();

        let img = image::RgbaImage::from_raw(gif_width as u32, gif_height as u32, buffer)
            .ok_or("Failed to create image from GIF frame")?;

        let dynamic_img = image::DynamicImage::ImageRgba8(img);
        let processed = ImageConverter::preprocess_image_smart(dynamic_img, transparency_as_black);
        let ascii_art = ImageConverter::image_to_braille_string(&processed, TV_WIDTH, TV_HEIGHT);

        sheet.add_frame(ascii_art);
    }

    fs::write(&output, sheet.to_string())?;

    println!("Saved ASCII art to: {output:?}");
    println!(
        "Size: {}x{}, {} frames",
        TV_WIDTH,
        TV_HEIGHT,
        sheet.frame_count()
    );

    Ok(())
}

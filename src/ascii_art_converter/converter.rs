use image::{DynamicImage, GenericImageView, ImageBuffer, Luma, Rgba};

pub const TV_WIDTH: usize = 32;
pub const TV_HEIGHT: usize = 10;
pub const PIXEL_WIDTH: usize = TV_WIDTH * 2;
pub const PIXEL_HEIGHT: usize = TV_HEIGHT * 4;
const TARGET_ASPECT_RATIO: f32 = PIXEL_WIDTH as f32 / PIXEL_HEIGHT as f32;

pub struct ImageConverter;

impl ImageConverter {
    pub fn preprocess_image_smart(img: DynamicImage, transparency_as_black: bool) -> ImageBuffer<Luma<u8>, Vec<u8>> {
        let (img_width, img_height) = img.dimensions();
        let source_aspect = img_width as f32 / img_height as f32;

        let img_to_process = if (source_aspect - TARGET_ASPECT_RATIO).abs() < 0.01 {
            img
        } else {
            let (crop_x, crop_y, crop_w, crop_h) = Self::calculate_center_crop(img_width, img_height);
            img.crop_imm(crop_x, crop_y, crop_w, crop_h)
        };

        let img_rgba = img_to_process.to_rgba8();
        let img_gray = Self::handle_transparency(img_rgba, transparency_as_black);
        let img_thresholded = Self::apply_threshold(img_gray, 128);

        if img_to_process.width() == PIXEL_WIDTH as u32 && img_to_process.height() == PIXEL_HEIGHT as u32 {
            img_thresholded
        } else {
            image::DynamicImage::ImageLuma8(img_thresholded)
                .resize_exact(
                    PIXEL_WIDTH as u32,
                    PIXEL_HEIGHT as u32,
                    image::imageops::FilterType::Lanczos3,
                )
                .to_luma8()
        }
    }

    fn calculate_center_crop(source_width: u32, source_height: u32) -> (u32, u32, u32, u32) {
        let source_aspect = source_width as f32 / source_height as f32;

        let (crop_width, crop_height) = if source_aspect > TARGET_ASPECT_RATIO {
            let crop_width = (source_height as f32 * TARGET_ASPECT_RATIO) as u32;
            (crop_width, source_height)
        } else {
            let crop_height = (source_width as f32 / TARGET_ASPECT_RATIO) as u32;
            (source_width, crop_height)
        };

        let crop_x = (source_width - crop_width) / 2;
        let crop_y = (source_height - crop_height) / 2;

        (crop_x, crop_y, crop_width, crop_height)
    }

    pub fn preprocess_image(img: DynamicImage, target_width: usize, target_height: usize, transparency_as_black: bool) -> ImageBuffer<Luma<u8>, Vec<u8>> {
        let pixel_width = target_width * 2;
        let pixel_height = target_height * 4;

        let img_rgba = img.to_rgba8();
        let img_gray = Self::handle_transparency(img_rgba, transparency_as_black);

        let img_resized = image::DynamicImage::ImageLuma8(img_gray).resize_exact(
            pixel_width as u32,
            pixel_height as u32,
            image::imageops::FilterType::Lanczos3,
        );

        let gray_img = img_resized.to_luma8();
        Self::apply_threshold(gray_img, 128)
    }

    fn handle_transparency(img: ImageBuffer<Rgba<u8>, Vec<u8>>, transparency_as_black: bool) -> ImageBuffer<Luma<u8>, Vec<u8>> {
        let (width, height) = img.dimensions();
        let mut result = ImageBuffer::new(width, height);
        let transparent_value = if transparency_as_black { 0 } else { 255 };

        for y in 0..height {
            for x in 0..width {
                let pixel = img.get_pixel(x, y);
                let alpha = pixel[3];

                if alpha < 128 {
                    result.put_pixel(x, y, Luma([transparent_value]));
                } else {
                    let gray = ((pixel[0] as u32 + pixel[1] as u32 + pixel[2] as u32) / 3) as u8;
                    result.put_pixel(x, y, Luma([gray]));
                }
            }
        }

        result
    }

    fn apply_threshold(img: ImageBuffer<Luma<u8>, Vec<u8>>, threshold: u8) -> ImageBuffer<Luma<u8>, Vec<u8>> {
        let (width, height) = img.dimensions();
        let mut result = ImageBuffer::new(width, height);

        for y in 0..height {
            for x in 0..width {
                let pixel = img.get_pixel(x, y);
                let value = if pixel[0] < threshold { 0 } else { 255 };
                result.put_pixel(x, y, Luma([value]));
            }
        }

        result
    }

    pub fn image_to_braille_string(img: &ImageBuffer<Luma<u8>, Vec<u8>>, char_width: usize, char_height: usize) -> String {
        let mut result = String::new();

        for char_y in 0..char_height {
            for char_x in 0..char_width {
                let pixel_x = char_x * 2;
                let pixel_y = char_y * 4;

                let mut dots = [false; 8];

                for dy in 0..4 {
                    for dx in 0..2 {
                        let x = pixel_x + dx;
                        let y = pixel_y + dy;

                        if x < img.width() as usize && y < img.height() as usize {
                            let pixel = img.get_pixel(x as u32, y as u32);
                            dots[dy * 2 + dx] = pixel[0] == 0;
                        }
                    }
                }

                result.push(Self::dots_to_braille(dots));
            }

            if char_y < char_height - 1 {
                result.push('\n');
            }
        }

        result
    }

    fn dots_to_braille(dots: [bool; 8]) -> char {
        let mut code: u32 = 0x2800;

        const DOT_VALUES: [u32; 8] = [0x01, 0x08, 0x02, 0x10, 0x04, 0x20, 0x40, 0x80];

        for (i, &dot) in dots.iter().enumerate() {
            if dot {
                code |= DOT_VALUES[i];
            }
        }

        char::from_u32(code).unwrap_or('⠀')
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_dots_to_braille_empty() {
        let dots = [false; 8];
        let result = ImageConverter::dots_to_braille(dots);
        assert_eq!(result, '⠀');
    }

    #[test]
    fn test_dots_to_braille_full() {
        let dots = [true; 8];
        let result = ImageConverter::dots_to_braille(dots);
        assert_eq!(result, '⣿');
    }

    #[test]
    fn test_apply_threshold() {
        let img = ImageBuffer::from_fn(4, 4, |x, y| {
            if (x + y) % 2 == 0 {
                Luma([0u8])
            } else {
                Luma([255u8])
            }
        });

        let result = ImageConverter::apply_threshold(img, 128);

        assert_eq!(result.get_pixel(0, 0)[0], 0);
        assert_eq!(result.get_pixel(1, 0)[0], 255);
    }
}

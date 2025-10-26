// Shared braille conversion utilities

// Display constants
export const TV_WIDTH = 32;
export const TV_HEIGHT = 10;
export const PIXEL_WIDTH = TV_WIDTH * 2;
export const PIXEL_HEIGHT = TV_HEIGHT * 4;

// Braille conversion constants
const BRAILLE_BASE = 0x2800;
const BRAILLE_OFFSETS = [
    [0, 0, 0x01], [0, 1, 0x02], [0, 2, 0x04], [0, 3, 0x40],
    [1, 0, 0x08], [1, 1, 0x10], [1, 2, 0x20], [1, 3, 0x80]
];

// Convert pixel array to braille string
export function pixelsToBraille(pixels) {
    const lines = [];
    const charHeight = Math.ceil(pixels.length / 4);
    const charWidth = Math.ceil(pixels[0].length / 2);

    for (let charY = 0; charY < charHeight; charY++) {
        let line = '';
        for (let charX = 0; charX < charWidth; charX++) {
            const baseX = charX * 2;
            const baseY = charY * 4;

            let brailleValue = 0;

            for (const [dx, dy, bit] of BRAILLE_OFFSETS) {
                const x = baseX + dx;
                const y = baseY + dy;
                if (y < pixels.length && x < pixels[0].length && pixels[y][x]) {
                    brailleValue |= bit;
                }
            }

            const char = String.fromCharCode(BRAILLE_BASE + brailleValue);
            line += char;
        }
        lines.push(line.trimEnd());
    }

    return lines.join('\n');
}

// Convert braille string to pixel array
export function brailleToPixels(brailleText, charWidth, charHeight) {
    const lines = brailleText.trim().split('\n');
    const framesData = [];

    for (let frameStart = 0; frameStart < lines.length; frameStart += charHeight) {
        const frameLines = lines.slice(frameStart, frameStart + charHeight);
        const pixelHeight = charHeight * 4;
        const pixelWidth = charWidth * 2;
        const pixels = Array(pixelHeight).fill(null).map(() => Array(pixelWidth).fill(false));

        for (let charY = 0; charY < frameLines.length; charY++) {
            const line = frameLines[charY];
            for (let charX = 0; charX < Math.min(line.length, charWidth); charX++) {
                const char = line[charX];
                const brailleValue = char.charCodeAt(0) - BRAILLE_BASE;

                const baseX = charX * 2;
                const baseY = charY * 4;

                for (const [dx, dy, bit] of BRAILLE_OFFSETS) {
                    if (brailleValue & bit) {
                        const x = baseX + dx;
                        const y = baseY + dy;
                        if (x < pixelWidth && y < pixelHeight) {
                            pixels[y][x] = true;
                        }
                    }
                }
            }
        }

        framesData.push(pixels);
    }

    return framesData;
}

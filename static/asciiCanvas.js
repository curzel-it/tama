// Display constants
const TV_WIDTH = 32;
const TV_HEIGHT = 10;
const PIXEL_WIDTH = TV_WIDTH * 2;
const PIXEL_HEIGHT = TV_HEIGHT * 4;

// Color scheme
function isLightMode() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches;
}

function textColor() {
    return isLightMode() ? '#081820' : '#88c070';
}

function backgroundColor() {
    return isLightMode() ? '#f0faf0' : '#081820';
}

// Convert braille string to pixel array
function brailleToPixels(brailleText, charWidth, charHeight) {
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
                const brailleValue = char.charCodeAt(0) - 0x2800;

                const baseX = charX * 2;
                const baseY = charY * 4;

                const brailleOffsets = [
                    [0, 0, 0x01], [0, 1, 0x02], [0, 2, 0x04], [0, 3, 0x40],
                    [1, 0, 0x08], [1, 1, 0x10], [1, 2, 0x20], [1, 3, 0x80]
                ];

                for (const [dx, dy, bit] of brailleOffsets) {
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

// Main entry point
function renderContentFrame(pixelFrame) {
    renderPixelFrame(pixelFrame);
}

// Render pixel array to canvas at 1x scale
function renderPixelFrame(pixelFrame) {
    const canvas = document.getElementById('realCanvas');
    const ctx = canvas.getContext('2d');

    if (!pixelFrame || pixelFrame.length === 0) {
        console.error('renderPixelFrame: empty or invalid pixelFrame');
        return;
    }

    const displayWidth = canvas.clientWidth || canvas.offsetWidth;
    const displayHeight = canvas.clientHeight || canvas.offsetHeight;
    const dpr = window.devicePixelRatio || 1;

    canvas.width = displayWidth * dpr;
    canvas.height = displayHeight * dpr;

    const pixelSize = canvas.width / 76;

    const offsetX = 6 + (64 - pixelFrame[0].length) / 2;
    const offsetY = 18 + (40 - pixelFrame.length) / 2;

    // Clear background
    ctx.fillStyle = backgroundColor();
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Render content
    renderPixelsToCanvas(ctx, pixelFrame, offsetX, offsetY, pixelSize);

    // Render grid overlay
    renderPixelGrid(ctx, pixelFrame[0].length, pixelFrame.length, offsetX, offsetY, pixelSize);

    // Render tv
    renderTvFrame(ctx, pixelSize);
}

function renderTvFrame(ctx, pixelSize) {
    const strokeWidth = Math.max(Math.floor(pixelSize / 2), 1);

    ctx.strokeStyle = textColor();
    ctx.lineWidth = strokeWidth;
    
    // Outer frame
    ctx.beginPath();
    ctx.roundRect(strokeWidth, 12 * pixelSize + strokeWidth, pixelSize * 76 - pixelSize, pixelSize * 56 - pixelSize, pixelSize);
    ctx.stroke();

    // Inner frame
    ctx.beginPath();
    ctx.roundRect(4 * pixelSize + strokeWidth, 16 * pixelSize + strokeWidth, pixelSize * 68 - pixelSize, pixelSize * 44 - pixelSize, pixelSize);
    ctx.stroke();

    // Antenna
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    const antennaCenterX = 25 * pixelSize;
    const antennaCenterY = 12 * pixelSize;
    const antennaLeftEndX = 21 * pixelSize;
    const antennaLeftEndY = 4 * pixelSize;
    const antennaRightEndX = 31 * pixelSize;
    const antennaRightEndY = 0 * pixelSize;

    ctx.beginPath();
    ctx.moveTo(antennaLeftEndX, antennaLeftEndY);
    ctx.lineTo(antennaCenterX, antennaCenterY);
    ctx.lineTo(antennaRightEndX, antennaRightEndY);
    ctx.stroke();

    // Text
    ctx.font = `${3 * pixelSize}px "Fira Code", monospace`;
    ctx.fillStyle = textColor();
    ctx.textAlign = 'center';
    ctx.textBaseline = 'top';
    ctx.fillText('Tama Tv', 38 * pixelSize, 62 * pixelSize + strokeWidth);
}

// Draw individual pixels
function renderPixelsToCanvas(ctx, pixels, offsetX, offsetY, pixelSize) {
    ctx.fillStyle = textColor();

    for (let y = 0; y < pixels.length; y++) {
        for (let x = 0; x < pixels[y].length; x++) {
            if (pixels[y][x]) {
                ctx.fillRect(
                    (offsetX + x) * pixelSize,
                    (offsetY + y) * pixelSize,
                    pixelSize,
                    pixelSize
                );
            }
        }
    }
}

// Draw grid overlay
function renderPixelGrid(ctx, contentWidth, contentHeight, offsetX, offsetY, pixelSize) {
    if (pixelSize <= 1) return;

    ctx.strokeStyle = backgroundColor();
    ctx.lineWidth = 1;

    for (let x = 0; x <= contentWidth; x++) {
        ctx.beginPath();
        ctx.moveTo((offsetX + x) * pixelSize, offsetY * pixelSize);
        ctx.lineTo((offsetX + x) * pixelSize, (offsetY + contentHeight) * pixelSize);
        ctx.stroke();
    }

    for (let y = 0; y <= contentHeight; y++) {
        ctx.beginPath();
        ctx.moveTo(offsetX * pixelSize, (offsetY + y) * pixelSize);
        ctx.lineTo((offsetX + contentWidth) * pixelSize, (offsetY + y) * pixelSize);
        ctx.stroke();
    }
}

function calculatePixelSize() {
    const canvas = document.getElementById('realCanvas');
    const displayWidth = canvas.clientWidth || canvas.offsetWidth;
    const dpr = window.devicePixelRatio || 1;
    return Math.max(1, Math.floor(displayWidth * dpr / 76));
}
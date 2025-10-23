const TV_WIDTH = 32;
const TV_HEIGHT = 10;
const PIXEL_WIDTH = TV_WIDTH * 2;
const PIXEL_HEIGHT = TV_HEIGHT * 4;
const SPACE = ' ';
const CHAR_WIDTH = 6;
const CHAR_HEIGHT = CHAR_WIDTH * 2;
const CHARACTER_SPACING = CHAR_WIDTH / 6.0;
const LINE_SPACING = CHARACTER_SPACING * 2;

function isLightMode() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches;
}

function textColor() {
    return isLightMode() ? '#081820' : '#88c070';
}

function backgroundColor() {
    return isLightMode() ? '#f0faf0' : '#081820';
}

function renderContentFrame(contentFrame) {
    const text = renderContentToString(contentFrame);
    renderStringToCanvas(CHAR_WIDTH, CHAR_HEIGHT, CHARACTER_SPACING, LINE_SPACING, text)
}

function renderStringToCanvas(charWidth, charHeight, characterSpacing, lineSpacing, content) {
    const canvas = document.getElementById('realCanvas');
    const ctx = canvas.getContext('2d');

    // Split content into lines
    const lines = content.split('\n');
    const width = Math.max(...lines.map(line => line.length));
    const height = lines.length;

    // Set canvas size
    canvas.width = width * (charWidth + characterSpacing);
    canvas.height = height * (charHeight + lineSpacing);

    // Clear background
    ctx.fillStyle = backgroundColor();
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Set font
    ctx.font = `${charHeight}px 'Fira Code', monospace`;
    ctx.textBaseline = 'top';
    ctx.fillStyle = textColor();

    // Draw characters
    for (let y = 0; y < height; y++) {
        const line = lines[y];
        for (let x = 0; x < line.length; x++) {
            const char = line[x] || ' ';
            ctx.fillText(char, x * (charWidth + characterSpacing), y * (charHeight + lineSpacing));
        }
    }
}

function renderContentToString(content) {
    const lines = content.replaceAll('⠀', SPACE).replaceAll(' ', SPACE).split('\n');
    const contentWidth = Math.max(...lines.map(line => line.length));

    const formattedLines = lines
        .map(x => paddedRight(x, contentWidth))
        .map(x => padded(x, TV_WIDTH));

    // ensure we always have TV_HEIGHT lines
    while (formattedLines.length < TV_HEIGHT) {
        formattedLines.push(SPACE.repeat(TV_WIDTH));
    }

    return `            ╱
        ╲  ╱
         ╲╱
╭──────────────────────────────────╮
│╭────────────────────────────────╮│
││${formattedLines[0]}││
││${formattedLines[1]}││
││${formattedLines[2]}││
││${formattedLines[3]}││
││${formattedLines[4]}││
││${formattedLines[5]}││
││${formattedLines[6]}││
││${formattedLines[7]}││
││${formattedLines[8]}││
││${formattedLines[9]}││
│╰────────────────────────────────╯│
│              Tama Tv             │
╰──────────────────────────────────╯`;    
}

function padded(text, count) {
    if (count <= text.length) return text;
    const totalPadding = count - text.length;
    const left = Math.floor(totalPadding / 2);
    const right = Math.ceil(totalPadding / 2);
    return SPACE.repeat(left) + text + SPACE.repeat(right);
}

function paddedRight(text, count) {
    if (count <= text.length) return text;
    const padding = count - text.length;
    return text + SPACE.repeat(padding);
}

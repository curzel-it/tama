import { midiPlayer } from '/midi.js';
import { TV_WIDTH, TV_HEIGHT, pixelsToBraille, brailleToPixels } from '/braille.js';

function toggleSection(sectionId) {
    const content = document.getElementById(sectionId);
    const header = content.previousElementSibling;
    const toggle = header.querySelector('.section-toggle');

    content.classList.toggle('collapsed');
    toggle.classList.toggle('collapsed');
}

window.toggleSection = toggleSection;

const CELL_SIZE = 10;
let canvasWidth = TV_WIDTH;
let canvasHeight = TV_HEIGHT;
let PIXEL_WIDTH = canvasWidth * 2;
let PIXEL_HEIGHT = canvasHeight * 4;

let frames = [];
let currentFrameIndex = -1;
let previewAnimationInterval = null;
let previewFrameIndex = 0;
const previewZoom = 1;

const canvas = document.getElementById('pixelCanvas');
const ctx = canvas.getContext('2d');
const previewCanvas = document.getElementById('previewCanvas');
const previewCtx = previewCanvas.getContext('2d');

let isDrawing = false;
let drawMode = true;

function getPixelColor() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches
        ? '#081820'
        : '#88c070';
}

function getBgColor() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches
        ? '#f0faf0'
        : '#081820';
}

function updateCanvasSize() {
    PIXEL_WIDTH = canvasWidth * 2;
    PIXEL_HEIGHT = canvasHeight * 4;
    canvas.width = PIXEL_WIDTH * CELL_SIZE;
    canvas.height = PIXEL_HEIGHT * CELL_SIZE;
    updatePreviewCanvasSize();
}

function updatePreviewCanvasSize() {
    // Size will be calculated in renderPreview based on braille text dimensions
}

function resizeFrames(newWidth, newHeight) {
    const newPixelWidth = newWidth * 2;
    const newPixelHeight = newHeight * 4;

    const resizedFrames = frames.map(frame => {
        const newFrame = Array(newPixelHeight).fill(null).map(() => Array(newPixelWidth).fill(false));

        const offsetX = Math.floor((newPixelWidth - PIXEL_WIDTH) / 2);
        const offsetY = Math.floor((newPixelHeight - PIXEL_HEIGHT) / 2);

        for (let y = 0; y < Math.min(PIXEL_HEIGHT, newPixelHeight); y++) {
            for (let x = 0; x < Math.min(PIXEL_WIDTH, newPixelWidth); x++) {
                const targetY = y + offsetY;
                const targetX = x + offsetX;
                if (targetY >= 0 && targetY < newPixelHeight && targetX >= 0 && targetX < newPixelWidth) {
                    newFrame[targetY][targetX] = frame[y][x];
                }
            }
        }

        return newFrame;
    });

    frames = resizedFrames;
    canvasWidth = newWidth;
    canvasHeight = newHeight;
    updateCanvasSize();
    render();
}

function getCurrentFrame() {
    if (currentFrameIndex >= 0 && currentFrameIndex < frames.length) {
        return frames[currentFrameIndex];
    }
    return null;
}

function addFrame() {
    const newFrame = Array(PIXEL_HEIGHT).fill(null).map(() => Array(PIXEL_WIDTH).fill(false));
    frames.push(newFrame);
    currentFrameIndex = frames.length - 1;
    updateFramesList();
    render();
}

function deleteFrame(index) {
    if (frames.length === 0) return;
    frames.splice(index, 1);
    if (currentFrameIndex >= frames.length) {
        currentFrameIndex = frames.length - 1;
    }
    updateFramesList();
    render();
}

function selectFrame(index) {
    currentFrameIndex = index;
    updateFramesList();
    render();
}

function renderFrameThumbnail(frame) {
    const thumbnailSize = 64;
    const scale = Math.min(thumbnailSize / PIXEL_WIDTH, thumbnailSize / PIXEL_HEIGHT);
    const thumbWidth = Math.floor(PIXEL_WIDTH * scale);
    const thumbHeight = Math.floor(PIXEL_HEIGHT * scale);

    const thumbCanvas = document.createElement('canvas');
    thumbCanvas.width = thumbWidth;
    thumbCanvas.height = thumbHeight;
    thumbCanvas.className = 'frame-preview';
    const thumbCtx = thumbCanvas.getContext('2d');

    thumbCtx.fillStyle = getBgColor();
    thumbCtx.fillRect(0, 0, thumbWidth, thumbHeight);

    thumbCtx.fillStyle = getPixelColor();
    for (let y = 0; y < PIXEL_HEIGHT; y++) {
        for (let x = 0; x < PIXEL_WIDTH; x++) {
            if (frame[y][x]) {
                const px = Math.floor(x * scale);
                const py = Math.floor(y * scale);
                const size = Math.ceil(scale);
                thumbCtx.fillRect(px, py, size, size);
            }
        }
    }

    return thumbCanvas;
}

function updateFramesList() {
    const framesList = document.getElementById('framesList');
    framesList.innerHTML = '';

    frames.forEach((frame, index) => {
        const frameItem = document.createElement('div');
        frameItem.className = 'frame-item';
        if (index === currentFrameIndex) {
            frameItem.classList.add('selected');
        }
        frameItem.draggable = true;

        const frameNumber = document.createElement('div');
        frameNumber.className = 'frame-number';
        frameNumber.textContent = index + 1;

        const thumbnail = renderFrameThumbnail(frame);

        frameItem.appendChild(frameNumber);
        frameItem.appendChild(thumbnail);

        frameItem.addEventListener('click', () => selectFrame(index));

        frameItem.addEventListener('dragstart', (e) => {
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', index.toString());
            frameItem.classList.add('dragging');
        });

        frameItem.addEventListener('dragend', () => {
            frameItem.classList.remove('dragging');
        });

        frameItem.addEventListener('dragover', (e) => {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
        });

        frameItem.addEventListener('dragenter', (e) => {
            e.preventDefault();
            frameItem.classList.add('drag-over');
        });

        frameItem.addEventListener('dragleave', () => {
            frameItem.classList.remove('drag-over');
        });

        frameItem.addEventListener('drop', (e) => {
            e.preventDefault();
            frameItem.classList.remove('drag-over');

            const fromIndex = parseInt(e.dataTransfer.getData('text/plain'));
            const toIndex = index;

            if (fromIndex !== toIndex) {
                const [movedFrame] = frames.splice(fromIndex, 1);
                frames.splice(toIndex, 0, movedFrame);

                if (currentFrameIndex === fromIndex) {
                    currentFrameIndex = toIndex;
                } else if (fromIndex < currentFrameIndex && toIndex >= currentFrameIndex) {
                    currentFrameIndex--;
                } else if (fromIndex > currentFrameIndex && toIndex <= currentFrameIndex) {
                    currentFrameIndex++;
                }

                updateFramesList();
            }
        });

        framesList.appendChild(frameItem);

        if (index === currentFrameIndex) {
            const deleteButton = document.createElement('div');
            deleteButton.className = 'delete-frame-button';
            deleteButton.textContent = '×';
            deleteButton.title = 'Delete Frame';
            deleteButton.addEventListener('click', () => deleteFrame(index));
            framesList.appendChild(deleteButton);
        }
    });

    const addButton = document.createElement('div');
    addButton.className = 'add-frame-button';
    addButton.textContent = '+';
    addButton.title = 'Add Empty Frame';
    addButton.addEventListener('click', addFrame);
    framesList.appendChild(addButton);

    document.getElementById('frameCount').textContent = frames.length;
}

function render() {
    ctx.fillStyle = getBgColor();
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    const frame = getCurrentFrame();
    if (frame) {
        ctx.fillStyle = getPixelColor();
        for (let y = 0; y < PIXEL_HEIGHT; y++) {
            for (let x = 0; x < PIXEL_WIDTH; x++) {
                if (frame[y][x]) {
                    ctx.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }
    }

    ctx.strokeStyle = getPixelColor();
    ctx.globalAlpha = 0.2;
    for (let x = 0; x <= PIXEL_WIDTH; x++) {
        ctx.beginPath();
        ctx.moveTo(x * CELL_SIZE, 0);
        ctx.lineTo(x * CELL_SIZE, canvas.height);
        ctx.stroke();
    }
    for (let y = 0; y <= PIXEL_HEIGHT; y++) {
        ctx.beginPath();
        ctx.moveTo(0, y * CELL_SIZE);
        ctx.lineTo(canvas.width, y * CELL_SIZE);
        ctx.stroke();
    }
    ctx.globalAlpha = 1;
}

function renderPreview(frameIndex) {
    if (frames.length === 0 || frameIndex < 0 || frameIndex >= frames.length) {
        return;
    }

    const frame = frames[frameIndex];
    const pixelSize = 1 * previewZoom;

    const contentWidth = frame[0].length;
    const contentHeight = frame.length;
    previewCanvas.width = contentWidth * pixelSize;
    previewCanvas.height = contentHeight * pixelSize;

    previewCtx.fillStyle = getBgColor();
    previewCtx.fillRect(0, 0, previewCanvas.width, previewCanvas.height);

    previewCtx.fillStyle = getPixelColor();
    for (let y = 0; y < contentHeight; y++) {
        for (let x = 0; x < contentWidth; x++) {
            if (frame[y][x]) {
                previewCtx.fillRect(
                    x * pixelSize,
                    y * pixelSize,
                    pixelSize,
                    pixelSize
                );
            }
        }
    }

    if (frames.length > 0) {
        document.getElementById('previewFrameLabel').textContent = `Frame ${frameIndex + 1} / ${frames.length}`;
    }
}

function getPixelCoords(event) {
    const rect = canvas.getBoundingClientRect();
    const x = Math.floor((event.clientX - rect.left) / CELL_SIZE);
    const y = Math.floor((event.clientY - rect.top) / CELL_SIZE);
    return { x, y };
}

function setPixel(x, y, value) {
    const frame = getCurrentFrame();
    if (frame && x >= 0 && x < PIXEL_WIDTH && y >= 0 && y < PIXEL_HEIGHT) {
        frame[y][x] = value;
        render();
    }
}

canvas.addEventListener('mousedown', (e) => {
    const frame = getCurrentFrame();
    if (!frame) return;

    isDrawing = true;
    const { x, y } = getPixelCoords(e);
    drawMode = !frame[y][x];
    setPixel(x, y, drawMode);
});

canvas.addEventListener('mousemove', (e) => {
    if (isDrawing) {
        const { x, y } = getPixelCoords(e);
        setPixel(x, y, drawMode);
    }
});

canvas.addEventListener('mouseup', () => {
    isDrawing = false;
    updateFramesList();
});

canvas.addEventListener('mouseleave', () => {
    isDrawing = false;
    updateFramesList();
});

document.getElementById('clearButton').addEventListener('click', () => {
    const frame = getCurrentFrame();
    if (frame) {
        for (let y = 0; y < PIXEL_HEIGHT; y++) {
            for (let x = 0; x < PIXEL_WIDTH; x++) {
                frame[y][x] = false;
            }
        }
        render();
        updateFramesList();
    }
});

document.getElementById('fillButton').addEventListener('click', () => {
    const frame = getCurrentFrame();
    if (frame) {
        for (let y = 0; y < PIXEL_HEIGHT; y++) {
            for (let x = 0; x < PIXEL_WIDTH; x++) {
                frame[y][x] = true;
            }
        }
        render();
        updateFramesList();
    }
});

document.getElementById('resizeButton').addEventListener('click', () => {
    const newPixelWidth = parseInt(document.getElementById('canvasWidth').value);
    const newPixelHeight = parseInt(document.getElementById('canvasHeight').value);

    const newWidth = newPixelWidth / 2;
    const newHeight = newPixelHeight / 4;

    if (newPixelWidth < 2 || newPixelWidth > 64 || newPixelHeight < 4 || newPixelHeight > 40 ||
        newPixelWidth % 2 !== 0 || newPixelHeight % 4 !== 0) {
        alert('Canvas dimensions must be 2-64 pixels wide (multiples of 2) and 4-40 pixels tall (multiples of 4)');
        return;
    }

    resizeFrames(newWidth, newHeight);
});

document.getElementById('togglePreviewButton').addEventListener('click', () => {
    const panel = document.getElementById('previewPanel');
    const button = document.getElementById('togglePreviewButton');

    if (panel.classList.contains('visible')) {
        panel.classList.remove('visible');
        button.textContent = 'Show Preview';
        stopPreviewAnimation();
    } else {
        panel.classList.add('visible');
        button.textContent = 'Hide Preview';
        if (frames.length > 0) {
            previewFrameIndex = 0;
            renderPreview(previewFrameIndex);
            playPreviewAnimation();
        }
    }
});

document.getElementById('closePreviewButton').addEventListener('click', () => {
    document.getElementById('previewPanel').classList.remove('visible');
    document.getElementById('togglePreviewButton').textContent = 'Show Preview';
    stopPreviewAnimation();
});

function playPreviewAnimation() {
    if (frames.length === 0) return;
    if (previewAnimationInterval) return;

    const fps = parseFloat(document.getElementById('fps').value) || 10;
    const interval = 1000 / fps;

    previewAnimationInterval = setInterval(() => {
        previewFrameIndex = (previewFrameIndex + 1) % frames.length;
        renderPreview(previewFrameIndex);
    }, interval);
}

function stopPreviewAnimation() {
    if (previewAnimationInterval) {
        clearInterval(previewAnimationInterval);
        previewAnimationInterval = null;
    }
}

let isDraggingPreview = false;
let previewDragOffsetX = 0;
let previewDragOffsetY = 0;

document.querySelector('.preview-header').addEventListener('mousedown', (e) => {
    isDraggingPreview = true;
    const panel = document.getElementById('previewPanel');
    const rect = panel.getBoundingClientRect();
    previewDragOffsetX = e.clientX - rect.left;
    previewDragOffsetY = e.clientY - rect.top;
});

document.addEventListener('mousemove', (e) => {
    if (isDraggingPreview) {
        const panel = document.getElementById('previewPanel');
        panel.style.left = (e.clientX - previewDragOffsetX) + 'px';
        panel.style.top = (e.clientY - previewDragOffsetY) + 'px';
        panel.style.right = 'auto';
    }
});

document.addEventListener('mouseup', () => {
    isDraggingPreview = false;
});

function parseContentFile(fileContent) {
    const lines = fileContent.split('\n');
    let midi = '';
    let art = '';
    let width = 20;
    let height = 10;
    let fps = 10;
    let inMidi = false;
    let inArt = false;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i];

        if (line.includes('--- MIDI ---')) {
            inMidi = true;
            inArt = false;
            continue;
        }

        if (line.includes('--- ART ---')) {
            inMidi = false;
            inArt = true;
            continue;
        }

        if (inMidi && line.trim()) {
            const midiContent = line.replace(/^--bpm\s+\d+\s*/, '').trim();
            if (midiContent) {
                midi = midiContent;
            }
        }

        if (inArt) {
            const match = line.match(/Ascii Art Animation,\s*(\d+)x(\d+),\s*(\d+(?:\.\d+)?)fps/);
            if (match) {
                width = parseInt(match[1]);
                height = parseInt(match[2]);
                fps = parseFloat(match[3]);
            } else if (line.trim()) {
                art += line + '\n';
            }
        }
    }

    return { midi, art, width, height, fps };
}

document.getElementById('loadFile').addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
        const content = event.target.result;
        const parsed = parseContentFile(content);

        document.getElementById('midiComposition').value = parsed.midi;
        document.getElementById('fps').value = parsed.fps;
        document.getElementById('canvasWidth').value = parsed.width * 2;
        document.getElementById('canvasHeight').value = parsed.height * 4;

        canvasWidth = parsed.width;
        canvasHeight = parsed.height;
        updateCanvasSize();

        if (parsed.art.trim()) {
            frames = brailleToPixels(parsed.art, parsed.width, parsed.height);
            if (frames.length > 0) {
                currentFrameIndex = 0;
            }
        } else {
            frames = [];
            currentFrameIndex = -1;
        }

        updateFramesList();
        render();
    };
    reader.readAsText(file);
});

document.getElementById('downloadButton').addEventListener('click', () => {
    const fps = parseFloat(document.getElementById('fps').value);
    const midiComposition = document.getElementById('midiComposition').value.trim();

    let content = '--- MIDI ---\n';
    if (midiComposition) {
        content += `--bpm 150 ${midiComposition}\n`;
    } else {
        content += '--bpm 150\n';
    }
    content += '--- ART ---\n';
    content += `Ascii Art Animation, ${canvasWidth}x${canvasHeight}, ${fps}fps\n`;

    frames.forEach(frame => {
        const brailleArt = pixelsToBraille(frame);
        content += brailleArt + '\n';
    });

    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'content.txt';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
});

midiPlayer.initAudioContext();

document.getElementById('playButton').addEventListener('click', async () => {
    const composition = document.getElementById('midiComposition').value.trim();

    if (!composition) {
        alert('Please enter a MIDI composition');
        return;
    }

    try {
        await midiPlayer.play(composition);
    } catch (error) {
        alert(`Failed to play composition: ${error.message}`);
        console.error('Play error:', error);
    }
});

document.getElementById('stopButton').addEventListener('click', () => {
    midiPlayer.stop();
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        midiPlayer.stop();
    }
});

document.getElementById('publishButton').addEventListener('click', async () => {
    const publishError = document.getElementById('publishError');
    publishError.textContent = '';

    const auth = window.authManager;
    if (!auth.isAuthenticated()) {
        publishError.textContent = 'Please login first';
        window.location.href = '/';
        return;
    }

    const channel = auth.getChannel();
    if (!channel || !channel.id) {
        publishError.textContent = 'Invalid authentication. Please login again.';
        return;
    }

    const fps = parseFloat(document.getElementById('fps').value);

    if (isNaN(fps) || fps < 1 || fps > 30) {
        publishError.textContent = 'FPS must be between 1 and 30';
        return;
    }

    let brailleArt = `Ascii Art Animation, ${canvasWidth}x${canvasHeight}, ${fps}fps\n`;
    frames.forEach(frame => {
        brailleArt += pixelsToBraille(frame) + '\n';
    });

    const midiComposition = document.getElementById('midiComposition').value.trim();

    try {
        const payload = {
            channel_id: channel.id,
            name: '',
            art: brailleArt.trim(),
            midi: midiComposition,
            fps: fps
        };

        const response = await httpPost('/content', payload, auth.getToken());
        const result = await handleResponse(response);

        if (result && result.id) {
            window.location.href = `/view/content/${result.id}`;
        } else {
            publishError.textContent = 'Content published successfully!';
            setTimeout(() => {
                window.location.href = '/';
            }, 1000);
        }
    } catch (error) {
        publishError.textContent = `Failed to publish: ${error.message}`;
        console.error('Publish error:', error);
    }
});

window.matchMedia('(prefers-color-scheme: light)').addEventListener('change', () => {
    render();
    if (frames.length > 0) {
        renderPreview(previewFrameIndex);
    }
});

updateCanvasSize();
addFrame();
render();

// Parse ASCII art sprite-sheet format
// Format: "Ascii Art Animation, {width}x{height}\n" followed by frames
function parseFrames(artString) {
    const lines = artString.split('\n');

    if (lines.length === 0) {
        return [artString];
    }

    // Parse header: "Ascii Art Animation, WxH"
    const header = lines[0];
    const dimensionMatch = header.match(/(\d+)x(\d+)/);

    if (!dimensionMatch) {
        // No valid header, treat as single frame
        return [artString];
    }

    const width = parseInt(dimensionMatch[1]);
    const height = parseInt(dimensionMatch[2]);

    // Extract frame lines (skip header)
    const frameLines = lines.slice(1);

    // Split into individual frames
    const frames = [];
    for (let i = 0; i < frameLines.length; i += height) {
        const frameContent = frameLines.slice(i, i + height).join('\n');
        if (frameContent.trim()) {
            frames.push(frameContent);
        }
    }

    return frames.length > 0 ? frames : [artString];
}

// Animation controller
class AnimationController {
    constructor() {
        this.animationInterval = null;
        this.currentFrame = 0;
        this.frames = [];
    }

    start(frames, fps) {
        this.stop();
        this.frames = frames;
        this.currentFrame = 0;

        const frameTime = 1000 / fps;

        // Render first frame immediately
        if (this.frames[this.currentFrame]) {
            renderContentFrame(this.frames[this.currentFrame]);
        }

        // Set up interval for animation
        this.animationInterval = setInterval(() => {
            this.currentFrame = (this.currentFrame + 1) % this.frames.length;
            if (this.frames[this.currentFrame]) {
                renderContentFrame(this.frames[this.currentFrame]);
            }
        }, frameTime);
    }

    stop() {
        if (this.animationInterval) {
            clearInterval(this.animationInterval);
            this.animationInterval = null;
        }
    }
}

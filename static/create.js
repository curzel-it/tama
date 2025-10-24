function handleCreateClick() {
    if (authManager.isAuthenticated()) {
        showCreateChoiceModal();
    } else {
        showAuthModal();
    }
}

function showCreateChoiceModal() {
    const modal = document.getElementById('createChoiceModal');
    modal.style.display = 'flex';
}

function hideCreateChoiceModal() {
    const modal = document.getElementById('createChoiceModal');
    modal.style.display = 'none';
}

function showUploadModal() {
    hideCreateChoiceModal();
    const modal = document.getElementById('uploadModal');
    modal.style.display = 'flex';
}

function hideUploadModal() {
    const modal = document.getElementById('uploadModal');
    modal.style.display = 'none';

    document.getElementById('uploadForm').reset();
    document.getElementById('uploadError').textContent = '';
    document.getElementById('previewSection').style.display = 'none';
}

async function handleUpload(event) {
    event.preventDefault();
    console.log('handleUpload called - v2');

    const errorDiv = document.getElementById('uploadError');
    errorDiv.textContent = '';

    try {
        const spriteFileInput = document.getElementById('spriteFile');
        console.log('spriteFileInput:', spriteFileInput);

        if (!spriteFileInput) {
            errorDiv.textContent = 'File input not found. Please refresh the page.';
            return;
        }

        const spriteFile = spriteFileInput.files[0];
        console.log('spriteFile:', spriteFile);

        if (!spriteFile) {
            errorDiv.textContent = 'Please select a sprite file';
            return;
        }

        const fileContent = await readFileAsText(spriteFile);
        console.log('File content length:', fileContent.length);

        const parsed = parseSpriteFile(fileContent);
        console.log('Parsed:', parsed);

        if (!parsed) {
            errorDiv.textContent = 'Invalid sprite file format. Expected --- MIDI --- and --- ART --- sections.';
            return;
        }

        const { midi, art, fps, name } = parsed;

        const channel = authManager.getChannel();
        const token = authManager.getToken();

        if (!channel || !token) {
            errorDiv.textContent = 'Not authenticated. Please log in.';
            return;
        }

        const response = await httpPost('/content', {
            channel_id: channel.id,
            name: name || spriteFile.name.replace('.txt', ''),
            art: art,
            midi: midi,
            fps: fps
        }, token);

        const data = await handleResponse(response);

        hideUploadModal();

        window.location.href = `/view/content/${data.id}`;

    } catch (error) {
        errorDiv.textContent = error.message || 'Failed to upload content';
        console.error('Upload error:', error);
    }
}

function parseSpriteFile(content) {
    const lines = content.split('\n');

    let midiSection = [];
    let artSection = [];
    let currentSection = null;
    let fps = 10;
    let name = '';
    let foundArtHeader = false;

    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();

        if (line === '--- MIDI ---') {
            currentSection = 'midi';
            continue;
        } else if (line === '--- ART ---') {
            currentSection = 'art';
            continue;
        }

        if (currentSection === 'midi' && line) {
            midiSection.push(line);
        } else if (currentSection === 'art') {
            const originalLine = lines[i];

            if (!foundArtHeader) {
                const headerMatch = originalLine.match(/(\d+(?:\.\d+)?)fps/);
                if (headerMatch) {
                    fps = parseFloat(headerMatch[1]);
                    const namePart = originalLine.split(',')[0].replace('Ascii Art Animation', '').trim();
                    name = namePart;
                    foundArtHeader = true;
                }
            }
            artSection.push(originalLine);
        }
    }

    if (midiSection.length === 0 || artSection.length === 0) {
        return null;
    }

    return {
        midi: midiSection.join('\n'),
        art: artSection.join('\n'),
        fps: fps,
        name: name
    };
}

function readFileAsText(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => resolve(e.target.result);
        reader.onerror = (e) => reject(new Error('Failed to read file'));
        reader.readAsText(file);
    });
}

function showPreview(artContent, fps, name) {
    const previewSection = document.getElementById('previewSection');
    previewSection.style.display = 'block';

    const firstFrame = artContent.split('\n---\n')[0];
    renderStringToCanvas(6, 12, 1, 2, firstFrame);

    const previewCanvas = document.getElementById('previewCanvas');
    const mainCanvas = document.getElementById('realCanvas');

    const ctx = previewCanvas.getContext('2d');
    previewCanvas.width = mainCanvas.width;
    previewCanvas.height = mainCanvas.height;

    ctx.drawImage(mainCanvas, 0, 0);

    const fileInfo = document.getElementById('fileInfo');
    if (fileInfo) {
        fileInfo.textContent = `FPS: ${fps}${name ? ` | Name: ${name}` : ''}`;
    }
}

// Add file change listener to auto-preview
document.addEventListener('DOMContentLoaded', () => {
    const spriteFileInput = document.getElementById('spriteFile');
    if (spriteFileInput) {
        spriteFileInput.addEventListener('change', async (event) => {
            const file = event.target.files[0];
            if (!file) return;

            const errorDiv = document.getElementById('uploadError');
            const fileInfo = document.getElementById('fileInfo');
            const previewSection = document.getElementById('previewSection');

            try {
                const fileContent = await readFileAsText(file);
                const parsed = parseSpriteFile(fileContent);

                if (!parsed) {
                    errorDiv.textContent = 'Invalid sprite file format';
                    previewSection.style.display = 'none';
                    return;
                }

                errorDiv.textContent = '';
                const { midi, art, fps, name } = parsed;

                if (fileInfo && previewSection) {
                    previewSection.style.display = 'block';
                    fileInfo.innerHTML = `
                        <strong>✓ File parsed successfully!</strong><br>
                        FPS: ${fps}<br>
                        ${name ? `Name: ${name}<br>` : ''}
                        MIDI notes: ${midi.split('\n').length} line(s)<br>
                        Art content ready<br>
                        <br>
                        <em>Click "Publish" to upload.</em>
                    `;
                }
            } catch (error) {
                errorDiv.textContent = 'Error reading file';
                previewSection.style.display = 'none';
            }
        });
    }
});

window.handleCreateClick = handleCreateClick;
window.showCreateChoiceModal = showCreateChoiceModal;
window.hideCreateChoiceModal = hideCreateChoiceModal;
window.showUploadModal = showUploadModal;
window.hideUploadModal = hideUploadModal;
window.handleUpload = handleUpload;

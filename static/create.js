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

    const errorDiv = document.getElementById('uploadError');
    errorDiv.textContent = '';

    try {
        const contentName = document.getElementById('contentName').value;
        const artFile = document.getElementById('artFile').files[0];
        const midiFile = document.getElementById('midiFile').files[0];
        const fps = parseFloat(document.getElementById('fps').value);

        if (!artFile || !midiFile) {
            errorDiv.textContent = 'Please select both art and MIDI files';
            return;
        }

        const artContent = await readFileAsText(artFile);
        const midiContent = await readFileAsText(midiFile);

        showPreview(artContent);

        const channel = authManager.getChannel();
        const token = authManager.getToken();

        if (!channel || !token) {
            errorDiv.textContent = 'Not authenticated. Please log in.';
            return;
        }

        const response = await httpPost('/content', {
            channel_id: channel.id,
            name: contentName,
            art: artContent,
            midi: midiContent,
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

function readFileAsText(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (e) => resolve(e.target.result);
        reader.onerror = (e) => reject(new Error('Failed to read file'));
        reader.readAsText(file);
    });
}

function showPreview(artContent) {
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
}

window.handleCreateClick = handleCreateClick;
window.showCreateChoiceModal = showCreateChoiceModal;
window.hideCreateChoiceModal = hideCreateChoiceModal;
window.showUploadModal = showUploadModal;
window.hideUploadModal = hideUploadModal;
window.handleUpload = handleUpload;

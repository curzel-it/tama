import { midiPlayer } from '/midi.js';

let feedData = null;
let currentIndex = 0;
let isSingleContentMode = false;
const animationController = new AnimationController();

async function fetchFeed() {
    try {
        const response = await httpGet('/feed');
        const data = await handleResponse(response);
        return data;
    } catch (error) {
        handleError(error, 'fetchFeed');
    }
}

async function fetchContent(contentId) {
    try {
        const response = await httpGet(`/content/${contentId}`);
        const data = await handleResponse(response);
        return data;
    } catch (error) {
        handleError(error, 'fetchContent');
    }
}

function parseContentIdFromUrl() {
    const match = window.location.pathname.match(/\/view\/content\/(\d+)/);
    return match ? parseInt(match[1], 10) : null;
}

export async function loadFeed() {
    const container = document.getElementById('contentContainer');

    try {
        feedData = await fetchFeed();

        if (!feedData || feedData.length === 0) {
            container.innerHTML = '<div class="error">No content in feed yet. Upload some content!</div>';
            return;
        }

        currentIndex = 0;
        isSingleContentMode = false;
        renderContent();
    } catch (error) {
        container.innerHTML = `<div class="error">Failed to load feed: ${error.message}</div>`;
    }
}

export async function loadSingleContent(contentId) {
    const container = document.getElementById('contentContainer');

    try {
        // Load the feed in the background for navigation
        feedData = await fetchFeed();

        if (!feedData || feedData.length === 0) {
            // Fallback: if feed is empty, just load the single content
            const contentData = await fetchContent(contentId);

            if (!contentData) {
                container.innerHTML = '<div class="error">Content not found</div>';
                return;
            }

            feedData = [{
                channel: {
                    id: 0,
                    name: "Shared Content"
                },
                content: contentData
            }];
            currentIndex = 0;
            isSingleContentMode = true;
        } else {
            // Find the content in the feed
            const contentIndexInFeed = feedData.findIndex(
                item => item.content.id === contentId
            );

            if (contentIndexInFeed !== -1) {
                // Content found in feed, navigate to it
                currentIndex = contentIndexInFeed;
                isSingleContentMode = false;
            } else {
                // Content not in feed, fetch it separately and prepend to feed
                const contentData = await fetchContent(contentId);

                if (contentData) {
                    feedData.unshift({
                        channel: {
                            id: 0,
                            name: "Shared Content"
                        },
                        content: contentData
                    });
                    currentIndex = 0;
                    isSingleContentMode = false;
                } else {
                    container.innerHTML = '<div class="error">Content not found</div>';
                    return;
                }
            }
        }

        renderContent();
    } catch (error) {
        container.innerHTML = `<div class="error">Failed to load content: ${error.message}</div>`;
    }
}

export async function initializePage() {
    const contentId = parseContentIdFromUrl();

    if (contentId) {
        await loadSingleContent(contentId);
    }
}

function renderContent() {
    if (!feedData || feedData.length === 0) return;

    const item = feedData[currentIndex];
    const container = document.getElementById('contentContainer');
    const frames = parseFrames(item.content.art);

    if (!isSingleContentMode) {
        updatePageDetailsForItem(item);
    }

    const controlsHtml = isSingleContentMode
        ? ''
        : `<div class="controls">
            <button onclick="previousContent()" ${currentIndex === 0 ? 'disabled' : ''}>← Previous</button>
            <div class="counter">${currentIndex + 1} / ${feedData.length}</div>
            <button onclick="nextContent()" ${currentIndex === feedData.length - 1 ? 'disabled' : ''}>Next →</button>
        </div>`;

    container.innerHTML = `
        <div class="content-header">
            <h2>${escapeHtml(item.channel.name)}</h2>
            <div class="content-info">Channel ID: ${item.channel.id}</div>
            <div class="content-info">Content ID: ${item.content.id}</div>
            <div class="content-info">FPS: ${item.content.fps}</div>
            <div class="content-info">Frames: ${frames.length}</div>
        </div>

        ${controlsHtml}
    `;

    animationController.start(frames, item.content.fps);

    if (item.content.midi_composition) {
        midiPlayer.play(item.content.midi_composition);
    }
}

window.previousContent = function() {
    if (currentIndex > 0) {
        currentIndex--;
        renderContent();
    }
}

window.nextContent = function() {
    if (feedData && currentIndex < feedData.length - 1) {
        currentIndex++;
        renderContent();
    }
}

function updatePageDetailsForItem(item) {
    window.history.pushState({}, "", `/view/content/${item.content.id}`);
}

// Utility
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

window.addEventListener('beforeunload', () => {
    animationController.stop();
    midiPlayer.stop();
});

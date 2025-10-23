import { midiPlayer } from '/midi.js';

let feedData = null;
let currentIndex = 0;
let isSingleContentMode = false;
const animationController = new AnimationController();

async function fetchServers() {
    try {
        const response = await httpGet('/servers');
        const servers = await handleResponse(response);
        return servers || [];
    } catch (error) {
        console.error('Failed to fetch servers:', error);
        return [];
    }
}

async function fetchFeedFromServer(serverUrl) {
    try {
        const url = serverUrl.endsWith('/feed') ? serverUrl : `${serverUrl}/feed`;
        const response = await httpGet(url);
        const data = await handleResponse(response);
        return { success: true, data: data || [], serverUrl };
    } catch (error) {
        console.error(`Failed to fetch from ${serverUrl}:`, error);
        return { success: false, serverUrl, error };
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
    container.innerHTML = '<div class="loading">Loading feed...</div>';

    try {
        feedData = [];
        currentIndex = 0;
        isSingleContentMode = false;
        let hasRenderedFirst = false;

        const currentOrigin = window.location.origin;
        let servers = await fetchServers();

        servers = [currentOrigin, ...servers.filter(s => s !== currentOrigin)];

        console.log(`Fetching feed from ${servers.length} server(s):`, servers);

        const fetchPromises = servers.map(serverUrl =>
            fetchFeedFromServer(serverUrl).then(result => {
                if (result.success && result.data && result.data.length > 0) {
                    console.log(`Received ${result.data.length} items from ${result.serverUrl}`);
                    feedData.push(...result.data);

                    if (!hasRenderedFirst) {
                        hasRenderedFirst = true;
                        renderContent();
                    }
                }
            })
        );

        await Promise.allSettled(fetchPromises);

        if (feedData.length === 0) {
            container.innerHTML = '<div class="error">No content in feed yet. Upload some content!</div>';
        }
    } catch (error) {
        console.error('Error loading feed:', error);
        container.innerHTML = `<div class="error">Failed to load feed: ${error.message}</div>`;
    }
}

export async function loadSingleContent(contentId) {
    const container = document.getElementById('contentContainer');

    try {
        await loadFeed();

        if (!feedData || feedData.length === 0) {
            container.innerHTML = '<div class="error">Content not found</div>';
            return;
        }

        const contentIndexInFeed = feedData.findIndex(
            item => item.content.id === contentId
        );

        if (contentIndexInFeed !== -1) {
            currentIndex = contentIndexInFeed;
            renderContent();
        } else {
            container.innerHTML = '<div class="error">Content not found in feed</div>';
        }
    } catch (error) {
        console.error('Error loading content:', error);
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
    const navigationControls = document.getElementById('navigationControls');
    const art = item.content.art || '';
    const frames = parseFrames(art);

    if (!isSingleContentMode) {
        updatePageDetailsForItem(item);
    }

    if (navigationControls) {
        navigationControls.style.display = isSingleContentMode ? 'none' : 'block';
    }

    container.innerHTML = `
        <div class="content-header">
            <h2>${escapeHtml(item.channel.name)}</h2>
            <div class="content-info">Channel ID: ${item.channel.id}</div>
            <div class="content-info">Content ID: ${item.content.id}</div>
            <div class="content-info">FPS: ${item.content.fps}</div>
            <div class="content-info">Frames: ${frames.length}</div>
        </div>
    `;

    animationController.start(frames, item.content.fps);

    midiPlayer.stop();
    if (item.content.midi_composition && item.content.midi_composition.trim()) {
        midiPlayer.play(item.content.midi_composition);
    }

    if (typeof window.updateVolumeDisplay === 'function') {
        window.updateVolumeDisplay();
    }
}

window.previousContent = function() {
    if (!feedData || feedData.length === 0) return;

    currentIndex--;
    if (currentIndex < 0) {
        currentIndex = feedData.length - 1;
    }
    renderContent();
}

window.nextContent = function() {
    if (!feedData || feedData.length === 0) return;

    currentIndex++;
    if (currentIndex >= feedData.length) {
        currentIndex = 0;
    }
    renderContent();
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

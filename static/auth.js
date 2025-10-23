const AUTH_STORAGE_KEY = 'tama_auth';
const AUTH_CHANNEL_KEY = 'tama_channel';

class AuthManager {
    constructor() {
        this.loadAuth();
    }

    loadAuth() {
        const authData = localStorage.getItem(AUTH_STORAGE_KEY);
        const channelData = localStorage.getItem(AUTH_CHANNEL_KEY);

        if (authData && channelData) {
            try {
                this.token = authData;
                this.channel = JSON.parse(channelData);
            } catch (e) {
                console.error('Failed to parse stored auth data:', e);
                this.clearAuth();
            }
        }
    }

    saveAuth(token, channel) {
        this.token = token;
        this.channel = channel;
        localStorage.setItem(AUTH_STORAGE_KEY, token);
        localStorage.setItem(AUTH_CHANNEL_KEY, JSON.stringify(channel));
    }

    clearAuth() {
        this.token = null;
        this.channel = null;
        localStorage.removeItem(AUTH_STORAGE_KEY);
        localStorage.removeItem(AUTH_CHANNEL_KEY);
    }

    isAuthenticated() {
        return !!this.token && !!this.channel;
    }

    getToken() {
        return this.token;
    }

    getChannel() {
        return this.channel;
    }

    async register(channelName, password) {
        try {
            const response = await httpPost('/auth/register', {
                channel_name: channelName,
                password: password
            });

            const data = await handleResponse(response);
            this.saveAuth(data.token, data.channel);
            return data;
        } catch (error) {
            handleError(error, 'register');
        }
    }

    async login(channelName, password) {
        try {
            const response = await httpPost('/auth/login', {
                channel_name: channelName,
                password: password
            });

            const data = await handleResponse(response);
            this.saveAuth(data.token, data.channel);
            return data;
        } catch (error) {
            handleError(error, 'login');
        }
    }

    async loginOrSignup(channelName, password) {
        try {
            const response = await httpPost('/auth/login-or-signup', {
                channel_name: channelName,
                password: password
            });

            const data = await handleResponse(response);
            this.saveAuth(data.token, data.channel);
            return data;
        } catch (error) {
            handleError(error, 'auth');
        }
    }

    logout() {
        this.clearAuth();
        window.location.reload();
    }
}

const authManager = new AuthManager();

function showAuthModal() {
    const modal = document.getElementById('authModal');
    modal.style.display = 'flex';
}

function hideAuthModal() {
    const modal = document.getElementById('authModal');
    modal.style.display = 'none';

    document.getElementById('authChannelName').value = '';
    document.getElementById('authPassword').value = '';
    document.getElementById('authError').textContent = '';
}

async function handleAuth(event) {
    event.preventDefault();

    const channelName = document.getElementById('authChannelName').value;
    const password = document.getElementById('authPassword').value;
    const errorDiv = document.getElementById('authError');

    if (channelName.includes(' ')) {
        errorDiv.textContent = 'Channel name cannot contain spaces';
        return;
    }

    if (channelName.length > 250) {
        errorDiv.textContent = 'Channel name is too long (max 250 characters)';
        return;
    }

    try {
        errorDiv.textContent = '';
        await authManager.loginOrSignup(channelName, password);
        hideAuthModal();
        updateAuthUI();

        if (typeof showCreateChoiceModal === 'function') {
            showCreateChoiceModal();
        }
    } catch (error) {
        errorDiv.textContent = error.message;
    }
}

function handleLogout() {
    authManager.logout();
}

function updateAuthUI() {
    const createButton = document.getElementById('createButton');
    const userInfo = document.getElementById('userInfo');
    const channelNameSpan = document.getElementById('channelName');
    const logoutButton = document.getElementById('logoutButton');
    const devModeMessage = document.getElementById('devModeMessage');

    const urlParams = new URLSearchParams(window.location.search);
    const isDevMode = urlParams.get('dev') === 'true';

    if (authManager.isAuthenticated()) {
        const channel = authManager.getChannel();
        createButton.style.display = 'none';
        userInfo.style.display = 'flex';
        channelNameSpan.textContent = channel.name;
        if (devModeMessage) {
            devModeMessage.style.display = 'none';
        }
    } else {
        if (isDevMode) {
            createButton.style.display = 'block';
            if (devModeMessage) {
                devModeMessage.style.display = 'none';
            }
        } else {
            createButton.style.display = 'none';
            if (devModeMessage) {
                devModeMessage.style.display = 'block';
            }
        }
        userInfo.style.display = 'none';
    }
}

window.showAuthModal = showAuthModal;
window.hideAuthModal = hideAuthModal;
window.handleAuth = handleAuth;
window.handleLogout = handleLogout;
window.updateAuthUI = updateAuthUI;
window.authManager = authManager;

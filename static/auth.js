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
    const mobileNotice = document.getElementById('mobileNotice');

    if (isMobileDevice() && mobileNotice) {
        mobileNotice.style.display = 'block';
    } else if (mobileNotice) {
        mobileNotice.style.display = 'none';
    }

    modal.style.display = 'flex';
}

function hideAuthModal() {
    const modal = document.getElementById('authModal');
    modal.style.display = 'none';

    document.getElementById('authChannelName').value = '';
    document.getElementById('authPassword').value = '';
    const errorDiv = document.getElementById('authError');
    errorDiv.textContent = '';
    errorDiv.style.display = 'none';
}

async function handleAuth(event) {
    event.preventDefault();

    const channelName = document.getElementById('authChannelName').value;
    const password = document.getElementById('authPassword').value;
    const errorDiv = document.getElementById('authError');

    if (channelName.includes(' ')) {
        errorDiv.textContent = 'Channel name cannot contain spaces';
        errorDiv.style.display = 'block';
        return;
    }

    if (channelName.length > 250) {
        errorDiv.textContent = 'Channel name is too long (max 250 characters)';
        errorDiv.style.display = 'block';
        return;
    }

    try {
        errorDiv.textContent = '';
        errorDiv.style.display = 'none';
        await authManager.loginOrSignup(channelName, password);
        hideAuthModal();
        updateAuthUI();
    } catch (error) {
        errorDiv.textContent = error.message;
        errorDiv.style.display = 'block';
    }
}

function handleLogout() {
    authManager.logout();
}

function isMobileDevice() {
    return window.innerWidth < 720;
}

function updateAuthUI() {
    const createButton = document.getElementById('createButton');
    const loginButton = document.getElementById('loginButton');
    const logoutButton = document.getElementById('logoutButton');
    const channelNameSpan = document.getElementById('channelName');
    const isMobile = isMobileDevice();

    if (authManager.isAuthenticated()) {
        const channel = authManager.getChannel();
        channelNameSpan.style.display = 'inline';
        channelNameSpan.textContent = channel.name;
        if (logoutButton) {
            logoutButton.style.display = 'inline-block';
        }
        if (createButton) {
            if (isMobile) {
                createButton.style.display = 'none';
            } else {
                createButton.style.display = 'inline-block';
            }
        }
        if (loginButton) {
            loginButton.style.display = 'none';
        }
    } else {
        channelNameSpan.style.display = 'none';
        if (logoutButton) {
            logoutButton.style.display = 'none';
        }
        if (createButton) {
            createButton.style.display = 'none';
        }
        if (loginButton) {
            loginButton.style.display = 'inline-block';
        }
    }
}

window.showAuthModal = showAuthModal;
window.hideAuthModal = hideAuthModal;
window.handleAuth = handleAuth;
window.handleLogout = handleLogout;
window.updateAuthUI = updateAuthUI;
window.authManager = authManager;

async function httpRequest(url, options = {}) {
    const { method = 'GET', headers = {}, body, token } = options;

    const config = {
        method,
        headers: {
            ...headers
        }
    };

    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }

    if (body) {
        if (body instanceof FormData) {
            config.body = body;
        } else {
            config.headers['Content-Type'] = 'application/json';
            config.body = JSON.stringify(body);
        }
    }

    try {
        const response = await fetch(url, config);
        return response;
    } catch (error) {
        throw new Error(`Network error: ${error.message}`);
    }
}

async function httpGet(url, token = null) {
    return httpRequest(url, { method: 'GET', token });
}

async function httpPost(url, body, token = null) {
    return httpRequest(url, { method: 'POST', body, token });
}

async function handleResponse(response) {
    const contentType = response.headers.get('content-type');

    if (!response.ok) {
        let errorMessage = `HTTP ${response.status}: ${response.statusText}`;

        if (contentType && contentType.includes('application/json')) {
            try {
                const errorData = await response.json();
                errorMessage = errorData.message || errorData.error || errorMessage;
            } catch (e) {}
        }

        throw new Error(errorMessage);
    }

    if (contentType && contentType.includes('application/json')) {
        return await response.json();
    }

    return response;
}

function handleError(error, context) {
    console.error(`Error in ${context}:`, error);
    throw new Error(`${context} failed: ${error.message}`);
}
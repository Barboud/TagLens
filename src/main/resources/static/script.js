const BASE = '/auth'; // change this if the backend runs on a different port/domain, e.g. http://localhost:8080/auth

function log(message, isError = false) {
    const body = document.getElementById('logBody');
    const line = document.createElement('div');
    line.className = 'log-line';
    const time = new Date().toLocaleTimeString('en-GB');
    line.innerHTML = `<span class="time">${time}</span><span class="${isError ? 'err' : 'ok'}">${message}</span>`;
    body.prepend(line);
}

function showView(view) {
    document.getElementById('loadingView').classList.add('hidden');
    document.getElementById('guestView').classList.add('hidden');
    document.getElementById('userView').classList.add('hidden');
    document.getElementById(view).classList.remove('hidden');
}

function renderUser(user) {
    document.getElementById('userEmail').textContent = user.email;
    document.getElementById('userId').textContent = user.id;
    document.getElementById('userCreatedAt').textContent = user.created_at || user.createdAt || '—';
    showView('userView');
}

// Run on every page load: ask the backend if we're logged in, and render the right view.
async function checkSession() {
    try {
        const res = await fetch(`${BASE}/me`, {
            method: 'GET',
            credentials: 'include'
        });
        if (res.ok) {
            const data = await res.json();
            log(`Session active (${data.email})`);
            renderUser(data);
        } else {
            log('No active session');
            showView('guestView');
        }
    } catch (e) {
        log(`Connection error: ${e.message}`, true);
        showView('guestView');
    }
}

async function register() {
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    try {
        const res = await fetch(`${BASE}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        if (res.status === 201) {
            log(`Account created (${email})`);
        } else {
            const text = await res.text();
            log(`Registration failed [${res.status}]: ${text}`, true);
        }
    } catch (e) {
        log(`Connection error: ${e.message}`, true);
    }
}

async function login() {
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;
    try {
        const res = await fetch(`${BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include', // required so the cookie gets stored and sent
            body: JSON.stringify({ email, password })
        });
        if (res.ok) {
            log(`Login successful (${email})`);
            checkSession(); // refresh view with real user data from /me
        } else {
            const text = await res.text();
            log(`Login failed [${res.status}]: ${text}`, true);
        }
    } catch (e) {
        log(`Connection error: ${e.message}`, true);
    }
}

async function logout() {
    try {
        const res = await fetch(`${BASE}/logout`, {
            method: 'POST',
            credentials: 'include'
        });
        if (res.ok) {
            log('Logged out successfully');
            showView('guestView');
        } else {
            log(`Logout failed [${res.status}]`, true);
        }
    } catch (e) {
        log(`Connection error: ${e.message}`, true);
    }
}

// Kick things off as soon as the page loads
checkSession();
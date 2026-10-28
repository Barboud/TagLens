/* TagLens frontend — vanilla JS, no build step.
   Served by Spring Boot from /static, so every call is same-origin. */

const $ = (id) => document.getElementById(id);

const MAX_BYTES = 10 * 1024 * 1024;
const URL_TTL_MS = 14 * 60 * 1000;      // backend presigned links last 15 min
const POLL_EVERY_MS = 3000;
const POLL_MAX_TRIES = 25;               // ~75 s, then stop waiting for the AI

// The AI is restricted to these 11 colour names (see TaggingService prompt)
const COLORS = {
    black: "#111111", blue: "#2f6bff", brown: "#8a5a35", gray: "#9a9aa5", green: "#2fb36b",
    orange: "#ff8a1f", pink: "#ff6fb1", purple: "#8b4dff", red: "#ee3b3b", white: "#f7f7f7", yellow: "#ffd426",
};

const state = {
    me: null,
    search: "",
    explore: [],
    library: [],
    myIds: new Set(),
    urls: new Map(),        // id -> { url, at } | Promise
    pollTimer: null,
    pollTries: 0,
    authMode: "login",
    openId: null,
};

/* ---------- tiny helpers ---------- */

// Build DOM without innerHTML — tags come from an AI and search text from users.
function h(tag, props = {}, ...kids) {
    const el = document.createElement(tag);
    for (const [k, v] of Object.entries(props)) {
        if (v == null || v === false) continue;
        if (k === "class") el.className = v;
        else if (k === "style") el.style.cssText = v;
        else if (k.startsWith("on")) el.addEventListener(k.slice(2), v);
        else el.setAttribute(k, v === true ? "" : v);
    }
    for (const kid of kids.flat()) {
        if (kid == null || kid === false) continue;
        el.append(kid.nodeType ? kid : document.createTextNode(kid));
    }
    return el;
}

function toast(msg, type = "") {
    const t = h("div", { class: "toast " + type }, msg);
    $("toasts").append(t);
    setTimeout(() => t.remove(), 3800);
}

function fmtSize(bytes) {
    if (bytes == null) return "—";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + " KB";
    return (bytes / 1024 / 1024).toFixed(1) + " MB";
}

const list = (v) => (Array.isArray(v) ? v : []);
const objectsOf = (img) => list(img.tags?.objects);
const sceneOf = (img) => list(img.tags?.tags);
const colorsOf = (img) => list(img.tags?.colors);
const hasTags = (img) => objectsOf(img).length + sceneOf(img).length + colorsOf(img).length > 0;

async function api(path, opts = {}) {
    const res = await fetch(path, { credentials: "same-origin", ...opts });
    return res;
}

// Calls that need a login: a dead session shows up as 401 (or 403 from Spring's default entry point)
async function authed(path, opts) {
    const res = await api(path, opts);
    if (res.status === 401 || res.status === 403) {
        sessionExpired();
        throw new Error("auth");
    }
    return res;
}

function sessionExpired() {
    if (!state.me) return;
    setUser(null);
    toast("Session expired — please log in again", "error");
    openAuth("login");
}

/* ---------- theme ---------- */

function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
    try { localStorage.setItem("taglens-theme", theme); } catch { /* private mode */ }
}
(function initTheme() {
    let saved = null;
    try { saved = localStorage.getItem("taglens-theme"); } catch { /* ignore */ }
    const prefersLight = matchMedia("(prefers-color-scheme: light)").matches;
    document.documentElement.dataset.theme = saved || (prefersLight ? "light" : "dark");
})();
$("themeBtn").onclick = () =>
    applyTheme(document.documentElement.dataset.theme === "dark" ? "light" : "dark");

/* ---------- routing ---------- */

function showView(name) {
    if (name === "library" && !state.me) {
        openAuth("login");
        name = "explore";
    }
    document.querySelectorAll(".view").forEach((v) => (v.hidden = v.id !== "view-" + name));
    document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("active", t.dataset.view === name));
    if (location.hash !== "#" + name) history.replaceState(null, "", "#" + name);
    if (name === "library") loadLibrary();
}
document.querySelectorAll(".tab").forEach((t) => (t.onclick = () => showView(t.dataset.view)));

/* ---------- AI visuals ---------- */

// A gradient made from the colours the AI detected — used where no photo is available.
function paletteArt(img) {
    const hexes = colorsOf(img).map((c) => COLORS[String(c).toLowerCase()]).filter(Boolean);
    let bg;
    if (hexes.length === 0) bg = "linear-gradient(135deg, var(--surface-2), var(--border))";
    else if (hexes.length === 1) bg = `linear-gradient(135deg, ${hexes[0]}, color-mix(in srgb, ${hexes[0]} 40%, #000))`;
    else bg = `linear-gradient(135deg, ${hexes.join(", ")})`;
    return h("div", { class: "palette-art", style: `background:${bg}` });
}

function chip(label, kind) {
    const text = String(label);
    const kids = [];
    if (kind === "color") kids.push(h("i", { class: "dot", style: `background:${COLORS[text.toLowerCase()] || "#888"}` }));
    kids.push(text);
    return h("button", { class: "chip " + kind, type: "button", title: `Search “${text}”`, onclick: (e) => { e.stopPropagation(); searchFor(text); } }, ...kids);
}

function chipsFor(img, limit = Infinity) {
    const all = [
        ...objectsOf(img).map((t) => [t, "object"]),
        ...sceneOf(img).map((t) => [t, "scene"]),
        ...colorsOf(img).map((t) => [t, "color"]),
    ];
    const shown = all.slice(0, limit).map(([t, k]) => chip(t, k));
    if (all.length > limit) shown.push(h("span", { class: "chip more" }, `+${all.length - limit}`));
    return h("div", { class: "chips" }, ...shown);
}

/* ---------- image URLs (owner only) ---------- */

function getUrl(id) {
    const hit = state.urls.get(id);
    if (hit instanceof Promise) return hit;
    if (hit && Date.now() - hit.at < URL_TTL_MS) return Promise.resolve(hit.url);
    const p = authed("/images/" + id)
        .then((r) => (r.ok ? r.json() : Promise.reject(new Error("no url"))))
        .then((j) => {
            state.urls.set(id, { url: j.url, at: Date.now() });
            return j.url;
        })
        .catch((e) => { state.urls.delete(id); throw e; });
    state.urls.set(id, p);
    return p;
}

const lazy = new IntersectionObserver((entries) => {
    for (const e of entries) {
        if (!e.isIntersecting) continue;
        lazy.unobserve(e.target);
        const slot = e.target;
        getUrl(slot.dataset.id).then((url) => {
            const im = h("img", { alt: "", src: url, loading: "lazy" });
            im.onload = () => slot.querySelector(".palette-art")?.remove();
            slot.prepend(im);
        }).catch(() => {});
    }
}, { rootMargin: "200px" });

/* ---------- cards ---------- */

function card(img, mine) {
    const tagged = hasTags(img);
    const thumb = h("div", { class: "thumb", "data-id": img.id }, paletteArt(img));

    if (mine) lazy.observe(thumb);
    else thumb.append(h("span", { class: "thumb-label" }, "Preview is owner-only"));

    if (tagged) thumb.append(h("span", { class: "corner" }, h("i", { class: "spark" }), "AI tagged"));

    const body = h("div", { class: "body" },
        tagged
            ? chipsFor(img, 6)
            : h("div", { class: "analyzing-line" }, h("i", { class: "spark" }),
                mine && state.pollTries < POLL_MAX_TRIES ? "AI is analyzing your image…" : "No AI tags yet"),
        h("div", { class: "row" }, h("span", {}, (img.contentType || "").replace("image/", "").toUpperCase()), h("span", {}, fmtSize(img.fileSize))),
    );

    return h("article", {
        class: "card" + (!tagged && mine && state.pollTries < POLL_MAX_TRIES ? " analyzing" : ""),
        tabindex: "0",
        onclick: () => openDetail(img, mine),
        onkeydown: (e) => { if (e.key === "Enter") openDetail(img, mine); },
    }, thumb, body);
}

function renderGrid(el, images, mine, emptyTitle, emptyText) {
    el.replaceChildren();
    if (!images.length) {
        el.append(h("div", { class: "empty" }, h("b", {}, emptyTitle), emptyText));
        return;
    }
    images.forEach((img, i) => {
        const c = card(img, mine);
        c.style.animationDelay = Math.min(i, 12) * 25 + "ms";
        el.append(c);
    });
}

function skeletons(el, n = 8) {
    el.replaceChildren(...Array.from({ length: n }, () => h("div", { class: "skeleton" })));
}

/* ---------- explore (GET /images, public) ---------- */

async function loadExplore() {
    skeletons($("exploreGrid"));
    const q = state.search.trim();
    if (state.me) await loadLibrary({ quiet: true });   // need my ids: only the owner can view a photo
    try {
        const res = await api("/images" + (q ? "?search=" + encodeURIComponent(q) : ""));
        if (!res.ok) throw new Error(res.status);
        // Only show images whose photo the viewer is allowed to see (their own)
        state.explore = (await res.json()).filter((i) => state.myIds.has(i.id));
    } catch {
        $("exploreGrid").replaceChildren(h("div", { class: "empty" }, h("b", {}, "Couldn't load images"), "Check your connection and try again."));
        return;
    }
    $("exploreTitle").textContent = q ? `AI tag matches for “${q}” (${state.explore.length})` : "Latest uploads";
    $("clearSearch").hidden = !q;
    renderGrid($("exploreGrid"), state.explore, true,
        !state.me ? "Log in to see your images" : q ? "Nothing matches that tag" : "No images yet",
        !state.me ? "Photos are private — only their owner can view them." : q ? "Try a simpler word, like an object or colour." : "Upload your first image from My Library.");
    // mark ones that are mine so their photo shows
    if (state.me) markMine();
    if (!q) renderTrending();
}

// Public cards for images I own can show the real photo
function markMine() {
    document.querySelectorAll("#exploreGrid .thumb").forEach((t) => {
        if (state.myIds.has(t.dataset.id) && !t.querySelector("img")) {
            t.querySelector(".thumb-label")?.remove();
            lazy.observe(t);
        }
    });
}

function renderTrending() {
    const counts = new Map();
    for (const img of state.explore)
        for (const t of [...objectsOf(img), ...sceneOf(img)]) counts.set(t, (counts.get(t) || 0) + 1);
    const top = [...counts].sort((a, b) => b[1] - a[1]).slice(0, 8);
    $("trending").hidden = top.length === 0;
    $("trendingChips").replaceChildren(...top.map(([t]) => chip(t, "object")));
}

function searchFor(term) {
    state.search = term;
    $("searchInput").value = term;
    closeDetail();
    showView("explore");
    loadExplore();
    window.scrollTo({ top: 0, behavior: "smooth" });
}
$("searchForm").onsubmit = (e) => { e.preventDefault(); state.search = $("searchInput").value; loadExplore(); };
$("clearSearch").onclick = () => { $("searchInput").value = ""; state.search = ""; loadExplore(); };

/* ---------- library (GET /images/my) ---------- */

async function loadLibrary({ quiet = false } = {}) {
    if (!state.me) return;
    if (!quiet) skeletons($("libraryGrid"), 4);
    try {
        const res = await authed("/images/my");
        if (!res.ok) throw new Error(res.status);
        state.library = await res.json();
    } catch (e) {
        if (e.message !== "auth") $("libraryGrid").replaceChildren(h("div", { class: "empty" }, h("b", {}, "Couldn't load your library"), "Try again in a moment."));
        return;
    }
    state.myIds = new Set(state.library.map((i) => i.id));
    renderLibrary();
    managePolling();
    markMine();
}

function renderLibrary() {
    renderGrid($("libraryGrid"), state.library, true, "Your library is empty", "Drop your first image above — the AI will tag it in seconds.");
    renderStats();
    // keep an open detail panel in sync with fresh tags
    if (state.openId && $("detail").open) {
        const img = state.library.find((i) => i.id === state.openId);
        if (img) fillDetail(img, true);
    }
}

function renderStats() {
    const tagged = state.library.filter(hasTags);
    const tagCount = tagged.reduce((n, i) => n + objectsOf(i).length + sceneOf(i).length, 0);
    const colorFreq = new Map();
    tagged.forEach((i) => colorsOf(i).forEach((c) => colorFreq.set(c, (colorFreq.get(c) || 0) + 1)));
    const topColors = [...colorFreq].sort((a, b) => b[1] - a[1]).slice(0, 3);
    const total = state.library.reduce((n, i) => n + (i.fileSize || 0), 0);

    $("stats").replaceChildren(
        h("span", { class: "stat" }, h("b", {}, state.library.length), " images"),
        h("span", { class: "stat" }, h("i", { class: "spark" }), " ", h("b", {}, tagCount), " AI tags"),
        h("span", { class: "stat" }, h("b", {}, fmtSize(total))),
        topColors.length && h("span", { class: "stat" }, "Palette ",
            ...topColors.map(([c]) => h("i", { class: "dot", title: c, style: `background:${COLORS[c] || "#888"}` }))),
    );
}

// The AI tags arrive asynchronously after upload (204 returns immediately) — poll until they land.
function managePolling() {
    const waiting = state.library.some((i) => !hasTags(i));
    clearTimeout(state.pollTimer);
    if (waiting && state.pollTries < POLL_MAX_TRIES && state.me) {
        state.pollTries++;
        state.pollTimer = setTimeout(async () => {
            const before = state.library.filter(hasTags).length;
            await loadLibrary({ quiet: true });
            if (state.library.filter(hasTags).length > before) toast("✨ AI tags are ready", "ai");
        }, POLL_EVERY_MS);
    }
}

/* ---------- upload (POST /images/upload) ---------- */

async function upload(file) {
    if (!file) return;
    if (!file.type.startsWith("image/")) return toast("Only image files are allowed", "error");
    if (file.size > MAX_BYTES) return toast("File is too large — the limit is 10 MB", "error");

    const dz = $("dropzone");
    dz.classList.add("busy");
    $("dzTitle").textContent = "Uploading…";
    try {
        const body = new FormData();
        body.append("file", file);
        const res = await authed("/images/upload", { method: "POST", body });
        if (res.status === 413) throw new Error("File is too large");
        if (!res.ok) throw new Error("Upload failed — please try another image");
        toast("Uploaded — AI is tagging it now", "ai");
        state.pollTries = 0;
        await loadLibrary({ quiet: true });
    } catch (e) {
        if (e.message !== "auth") toast(e.message, "error");
    } finally {
        dz.classList.remove("busy");
        $("dzTitle").textContent = "Drop an image or click to upload";
        $("fileInput").value = "";
    }
}
$("fileInput").onchange = (e) => upload(e.target.files[0]);
const dz = $("dropzone");
["dragenter", "dragover"].forEach((ev) => dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.add("over"); }));
["dragleave", "drop"].forEach((ev) => dz.addEventListener(ev, (e) => { e.preventDefault(); dz.classList.remove("over"); }));
dz.addEventListener("drop", (e) => upload(e.dataTransfer.files[0]));
dz.addEventListener("keydown", (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); $("fileInput").click(); } });

/* ---------- detail modal (GET /images/{id}, DELETE /images/{id}) ---------- */

function openDetail(img, mine) {
    state.openId = img.id;
    fillDetail(img, mine || state.myIds.has(img.id));
    if (!$("detail").open) $("detail").showModal();
}

function fillDetail(img, mine) {
    const media = $("detailMedia");
    media.replaceChildren(paletteArt(img));
    if (mine) {
        getUrl(img.id).then((url) => {
            if (state.openId !== img.id) return;
            const im = h("img", { alt: "Uploaded image", src: url });
            im.onload = () => media.querySelector(".palette-art")?.remove();
            media.prepend(im);
        }).catch(() => {});
    } else {
        media.append(h("span", { class: "thumb-label", style: "position:relative" }, "Only the owner can view the photo"));
    }

    const tags = $("detailTags");
    tags.replaceChildren();
    if (!hasTags(img)) {
        tags.append(h("div", { class: "analyzing-line" }, h("i", { class: "spark" }), "Waiting for AI tags…"));
    } else {
        const group = (title, items, kind) => items.length && h("div", { class: "tag-group" },
            h("h4", {}, title), h("div", { class: "chips" }, ...items.map((t) => chip(t, kind))));
        tags.append(
            group("Objects detected", objectsOf(img), "object"),
            group("Scene & mood", sceneOf(img), "scene"),
            group("Dominant colours", colorsOf(img), "color"),
        );
    }

    $("detailMeta").replaceChildren(
        h("dt", {}, "Type"), h("dd", {}, img.contentType || "—"),
        h("dt", {}, "Size"), h("dd", {}, fmtSize(img.fileSize)),
        h("dt", {}, "ID"), h("dd", {}, img.id),
    );

    const actions = $("detailActions");
    actions.replaceChildren();
    if (mine) {
        actions.append(
            h("button", { class: "btn", onclick: async () => {
                try { window.open(await getUrl(img.id), "_blank", "noopener"); } catch { toast("Couldn't get the image link", "error"); }
            } }, "Open full size"),
            h("button", { class: "btn", onclick: async () => {
                try { await navigator.clipboard.writeText(await getUrl(img.id)); toast("Link copied — valid for 15 minutes"); }
                catch { toast("Couldn't copy the link", "error"); }
            } }, "Copy link"),
        );
        let armed = false;
        const del = h("button", { class: "btn danger", onclick: async () => {
            if (!armed) { armed = true; del.textContent = "Click again to delete"; setTimeout(() => { armed = false; del.textContent = "Delete"; }, 3000); return; }
            await deleteImage(img.id);
        } }, "Delete");
        actions.append(del);
    }
}

async function deleteImage(id) {
    try {
        const res = await authed("/images/" + id, { method: "DELETE" });
        if (!res.ok) throw new Error("Couldn't delete this image");
        state.urls.delete(id);
        closeDetail();
        toast("Image deleted");
        await loadLibrary({ quiet: true });
        if (!$("view-explore").hidden) loadExplore();
    } catch (e) {
        if (e.message !== "auth") toast(e.message, "error");
    }
}

function closeDetail() { state.openId = null; if ($("detail").open) $("detail").close(); }

// close dialogs on ✕ and on backdrop click
document.querySelectorAll("dialog").forEach((d) => {
    d.addEventListener("click", (e) => { if (e.target === d || e.target.closest("[data-close]")) d.close(); });
});
$("detail").addEventListener("close", () => (state.openId = null));

/* ---------- auth (POST /auth/register, /auth/login, /auth/logout, GET /auth/me) ---------- */

function setUser(user) {
    state.me = user;
    $("guestActions").hidden = !!user;
    $("userActions").hidden = !user;
    $("userChip").textContent = user ? user.email : "";
    document.querySelectorAll("[data-auth]").forEach((b) => (b.hidden = !user));
    if (!user) {
        clearTimeout(state.pollTimer);
        state.library = [];
        state.myIds = new Set();
        state.urls.clear();
        if (!$("view-library").hidden) showView("explore");
    }
}

function openAuth(mode) {
    state.authMode = mode;
    const login = mode === "login";
    document.querySelectorAll(".seg-btn").forEach((b) => b.classList.toggle("active", b.dataset.mode === mode));
    $("authTitle").textContent = login ? "Welcome back" : "Create your account";
    $("authSubmit").textContent = login ? "Log in" : "Sign up";
    $("authPassword").autocomplete = login ? "current-password" : "new-password";
    $("authError").hidden = true;
    if (!$("authModal").open) $("authModal").showModal();
    $("authEmail").focus();
}
document.querySelectorAll(".seg-btn").forEach((b) => (b.onclick = () => openAuth(b.dataset.mode)));
$("loginBtn").onclick = () => openAuth("login");
$("registerBtn").onclick = () => openAuth("register");

function authError(msg) { $("authError").textContent = msg; $("authError").hidden = false; }

async function postJson(path, data) {
    return api(path, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(data) });
}

$("authForm").onsubmit = async (e) => {
    e.preventDefault();
    const email = $("authEmail").value.trim();
    const password = $("authPassword").value;
    if (!/^\S+@\S+\.\S+$/.test(email)) return authError("Enter a valid email address");
    if (password.length < 8) return authError("Password must be at least 8 characters");

    const btn = $("authSubmit");
    btn.disabled = true;
    try {
        if (state.authMode === "register") {
            const reg = await postJson("/auth/register", { email, password });
            if (reg.status === 400) return authError("Please check your email and password");
            if (!reg.ok) return authError("Couldn't create the account — that email may already be registered");
        }
        const res = await postJson("/auth/login", { email, password });
        if (res.status === 401) return authError("Invalid email or password");
        if (!res.ok) return authError("Something went wrong — please try again");

        const me = await api("/auth/me");
        if (!me.ok) return authError("Logged in, but couldn't load your profile");
        setUser(await me.json());
        $("authModal").close();
        $("authForm").reset();
        toast(state.authMode === "register" ? "Welcome to TagLens 🎉" : "Logged in");
        await loadLibrary({ quiet: true });
        markMine();
    } catch {
        authError("Can't reach the server");
    } finally {
        btn.disabled = false;
    }
};

$("logoutBtn").onclick = async () => {
    await api("/auth/logout", { method: "POST" }).catch(() => {});
    setUser(null);
    toast("Logged out");
    loadExplore();
};

/* ---------- boot ---------- */

(async function init() {
    try {
        const res = await api("/auth/me");
        setUser(res.ok ? await res.json() : null);
    } catch { setUser(null); }

    const start = location.hash.replace("#", "") === "library" ? "library" : "explore";
    showView(start);
    await loadExplore();
})();

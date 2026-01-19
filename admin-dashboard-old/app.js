console.log("App.js Loaded");
import { auth, db } from './firebase-config.js';
import { onAuthStateChanged, signInWithEmailAndPassword, signOut, createUserWithEmailAndPassword } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { collection, getDocs, addDoc, serverTimestamp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

// --- State Management ---
const state = {
    user: null,
    currentRoute: 'overview',
    data: {
        products: [],
        devices: []
    }
};

// --- DOM Elements ---
const views = {
    auth: document.getElementById('auth-container'),
    dashboard: document.getElementById('dashboard-container')
};
const loginForm = document.getElementById('login-form');
const contentArea = document.getElementById('content-area');
const pageTitle = document.getElementById('page-title');
const loginError = document.getElementById('login-error');

// --- Product Logic ---
const modal = document.getElementById('product-modal');
const closeBtn = document.querySelector('.close-btn');
const addProductForm = document.getElementById('add-product-form');

function attachProductListeners() {
    const addBtn = document.getElementById('add-product-btn');
    if (addBtn) {
        addBtn.addEventListener('click', () => {
            modal.classList.remove('hidden');
        });
    }
}

if (closeBtn) {
    closeBtn.addEventListener('click', () => modal.classList.add('hidden'));
}

if (addProductForm) {
    addProductForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const name = document.getElementById('p-name').value;
        const price = parseFloat(document.getElementById('p-price').value);
        const stock = parseInt(document.getElementById('p-stock').value);
        const category = document.getElementById('p-category').value;

        try {
            await addDoc(collection(db, "products"), {
                name, price, stock, category,
                createdAt: serverTimestamp(),
                imageUrl: 'https://via.placeholder.com/150'
            });
            alert("Product Added!");
            modal.classList.add('hidden');
            fetchData();
            addProductForm.reset();
        } catch (err) {
            console.error(err);
            alert("Error adding product: " + err.message);
        }
    });
}

// --- Navigation Logic ---
function navigate(route) {
    state.currentRoute = route;
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.querySelector(`.nav-item[data-route="${route}"]`)?.classList.add('active');
    if (pageTitle) pageTitle.textContent = route.charAt(0).toUpperCase() + route.slice(1);

    renderContent(route);

    if (route === 'products') attachProductListeners();
}

// --- Rendering Logic ---
function renderContent(route) {
    if (!contentArea) return;
    contentArea.innerHTML = '';
    switch (route) {
        case 'overview': contentArea.innerHTML = getOverviewHTML(); break;
        case 'products': contentArea.innerHTML = getProductsHTML(); break;
        case 'orders': contentArea.innerHTML = '<h2>Order Management (Coming Soon)</h2>'; break;
        case 'devices': contentArea.innerHTML = getDevicesHTML(); break;
        case 'customers': contentArea.innerHTML = '<h2>Customer Management (Coming Soon)</h2>'; break;
        default: contentArea.innerHTML = '<h1>Page not found</h1>';
    }
}

function getOverviewHTML() {
    return `
        <div class="grid-stats">
            <div class="stat-card">
                <h3>${state.data.products.length}</h3>
                <p>Total Products</p>
            </div>
            <div class="stat-card">
                <h3>${state.data.devices.length}</h3>
                <p>Active Devices</p>
            </div>
        </div>
        <div class="table-container">
             <h3>Recent Devices</h3>
             <table class="data-table">
                <thead><tr><th>Device ID</th><th>Name</th><th>Last Seen</th></tr></thead>
                <tbody>
                    ${state.data.devices.slice(0, 5).map(d => `
                        <tr>
                            <td>${d.id}</td>
                            <td>${d.name || 'Unknown'}</td>
                            <td>${d.lastSeen ? new Date(d.lastSeen.seconds * 1000).toLocaleDateString() : 'Never'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

function getProductsHTML() {
    return `
        <div class="table-container">
            <div style="padding: 1.5rem; display: flex; justify-content: space-between; align-items: center;">
                <h3>Products Inventory</h3>
                <button id="add-product-btn" class="primary-btn" style="background: var(--primary); color: white; padding: 0.5rem 1rem; border:none; border-radius:4px; cursor:pointer;">+ Add Product</button>
            </div>
            <table class="data-table">
                <thead><tr><th>Name</th><th>Price</th><th>Stock</th><th>Category</th></tr></thead>
                <tbody>
                    ${state.data.products.map(p => `
                        <tr>
                            <td>${p.name}</td>
                            <td>₹${p.price}</td>
                            <td>${p.stock !== undefined ? p.stock : 'N/A'}</td>
                            <td>${p.category}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

function getDevicesHTML() {
    return `
        <div class="table-container">
            <h3>Registered Devices</h3>
            <table class="data-table">
                <thead><tr><th>Device ID</th><th>Name</th><th>Owner</th></tr></thead>
                <tbody>
                    ${state.data.devices.map(d => `
                        <tr>
                            <td>${d.id}</td>
                            <td>${d.name || 'Unknown'}</td>
                            <td>${d.ownerId || 'Unclaimed'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

// --- Data Fetching ---
async function fetchData() {
    try {
        console.log("Fetching Data...");
        const productsSnap = await getDocs(collection(db, "products"));
        state.data.products = productsSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));

        const devicesSnap = await getDocs(collection(db, "devices"));
        state.data.devices = devicesSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));

        renderContent(state.currentRoute);
    } catch (e) {
        console.error("Fetch Error:", e);
    }
}

// --- Auth Logic ---
onAuthStateChanged(auth, (user) => {
    state.user = user;
    if (user) {
        views.auth.classList.add('hidden');
        views.dashboard.classList.remove('hidden');
        fetchData();
    } else {
        views.auth.classList.remove('hidden');
        views.dashboard.classList.add('hidden');
    }
});

if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        if (loginError) loginError.textContent = 'Logging in...';

        try {
            await signInWithEmailAndPassword(auth, email, password);
        } catch (error) {
            console.error(error);
            if (loginError) loginError.textContent = 'Invalid email or password.';
        }
    });
}

const signupBtn = document.getElementById('signup-btn');
if (signupBtn) {
    signupBtn.addEventListener('click', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        try {
            await createUserWithEmailAndPassword(auth, email, password);
            alert("Admin Account Created! Logging in...");
        } catch (error) {
            if (loginError) loginError.textContent = error.message;
        }
    });
}

if (document.getElementById('logout-btn')) {
    document.getElementById('logout-btn').addEventListener('click', () => signOut(auth));
}

// Navigation Listeners
document.querySelectorAll('.nav-item[data-route]').forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        navigate(item.dataset.route);
    });
});


// --- Rendering Logic ---
function renderContent(route) {
    contentArea.innerHTML = '';

    switch (route) {
        case 'overview': contentArea.innerHTML = getOverviewHTML(); break;
        case 'products': contentArea.innerHTML = getProductsHTML(); break;
        case 'orders': contentArea.innerHTML = getOrdersHTML(); break;
        case 'devices': contentArea.innerHTML = getDevicesHTML(); break; // New Route
        case 'customers': contentArea.innerHTML = getCustomersHTML(); break;
        default: contentArea.innerHTML = '<h1>Page not found</h1>';
    }
}

// --- View Templates ---
function getOverviewHTML() {
    return `
        <div class="grid-stats">
            <div class="stat-card">
                <div class="header">
                    <div class="icon-box" style="background: rgba(59, 130, 246, 0.1); color: #3b82f6;">
                        <span class="material-icons-round">category</span>
                    </div>
                </div>
                <h3>${state.data.products.length}</h3>
                <p>Total Products</p>
            </div>
            <div class="stat-card">
                <div class="header">
                    <div class="icon-box" style="background: rgba(139, 92, 246, 0.1); color: #8b5cf6;">
                        <span class="material-icons-round">router</span>
                    </div>
                </div>
                <h3>${state.data.devices.length}</h3>
                <p>Active Devices</p>
            </div>
        </div>

        <div class="table-container">
            <div style="padding: 1.5rem; border-bottom: 1px solid var(--border);">
                <h3>Recent Devices</h3>
            </div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Device ID</th>
                        <th>Name</th>
                        <th>Last Seen</th>
                    </tr>
                </thead>
                <tbody>
                    ${state.data.devices.slice(0, 5).map(d => `
                        <tr>
                            <td>${d.id}</td>
                            <td>${d.name || 'Unknown'}</td>
                            <td>${d.lastSeen ? new Date(d.lastSeen.seconds * 1000).toLocaleDateString() : 'Never'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}



function getDevicesHTML() {
    return `
        <div class="table-container">
            <h3>Registered Devices</h3>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Device ID</th>
                        <th>Name</th>
                        <th>Owner</th>
                    </tr>
                </thead>
                 <tbody>
                    ${state.data.devices.map(d => `
                        <tr>
                            <td>${d.id}</td>
                            <td>${d.name || 'Unknown'}</td>
                            <td>${d.ownerId || 'Unclaimed'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

function getOrdersHTML() {
    return '<h2>Order Management Module (Coming Soon)</h2>';
}

function getCustomersHTML() {
    return '<h2>Customer Management Module (Coming Soon)</h2>';
}

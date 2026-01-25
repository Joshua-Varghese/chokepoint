import { auth, db } from './firebase-config.js';
import { onAuthStateChanged, signInWithEmailAndPassword, signOut } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";

// --- State Management ---
const state = {
    user: null,
    currentRoute: 'overview',
    data: {
        revenue: 254000,
        orders: [
            { id: 'ORD-001', customer: 'Alice Johnson', amount: 1250, status: 'Completed' },
            { id: 'ORD-002', customer: 'Bob Smith', amount: 850, status: 'Pending' },
            { id: 'ORD-003', customer: 'Charlie Brown', amount: 3200, status: 'Processing' },
            { id: 'ORD-004', customer: 'Diana Prince', amount: 4500, status: 'Completed' },
            { id: 'ORD-005', customer: 'Evan Wright', amount: 150, status: 'Cancelled' }
        ],
        products: [
            { id: 1, name: 'Air Purifier X1', price: 12000, stock: 45 },
            { id: 2, name: 'Filter Replacement', price: 800, stock: 120 },
            { id: 3, name: 'Smart Sensor', price: 3500, stock: 12 }
        ]
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

// --- Auth Logic ---
onAuthStateChanged(auth, (user) => {
    state.user = user;
    if (user) {
        showDashboard();
    } else {
        showLogin();
    }
});

loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    loginError.textContent = '';

    try {
        await signInWithEmailAndPassword(auth, email, password);
    } catch (error) {
        // --- MOCK LOGIN BYPASS (For testing without live Firebase Config) ---
        if (email === 'admin@chokepoint.com' && password === 'admin123') {
            console.warn("Using Mock Login Bypass");
            state.user = { email: email, displayName: 'Mock Admin' };
            showDashboard();
            return;
        }

        loginError.textContent = 'Invalid email or password.';
        console.error(error);
    }
});

document.getElementById('logout-btn').addEventListener('click', () => signOut(auth));

// --- Navigation Logic ---
function showLogin() {
    views.auth.classList.remove('hidden');
    views.dashboard.classList.add('hidden');
}

function showDashboard() {
    views.auth.classList.add('hidden');
    views.dashboard.classList.remove('hidden');
    navigate('overview');
}

document.querySelectorAll('.nav-item[data-route]').forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        const route = item.dataset.route;
        navigate(route);
    });
});

function navigate(route) {
    state.currentRoute = route;

    // Update Sidebar Active State
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.querySelector(`.nav-item[data-route="${route}"]`)?.classList.add('active');

    // Update Title
    pageTitle.textContent = route.charAt(0).toUpperCase() + route.slice(1);

    // Render Content
    renderContent(route);
}

// --- Rendering Logic ---
function renderContent(route) {
    contentArea.innerHTML = '';

    switch (route) {
        case 'overview': contentArea.innerHTML = getOverviewHTML(); break;
        case 'products': contentArea.innerHTML = getProductsHTML(); break;
        case 'orders': contentArea.innerHTML = getOrdersHTML(); break;
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
                        <span class="material-icons-round">attach_money</span>
                    </div>
                    <span class="badge success">+12.5%</span>
                </div>
                <h3>₹${(state.data.revenue / 1000).toFixed(1)}k</h3>
                <p>Total Revenue</p>
            </div>
            <div class="stat-card">
                <div class="header">
                    <div class="icon-box" style="background: rgba(139, 92, 246, 0.1); color: #8b5cf6;">
                        <span class="material-icons-round">shopping_bag</span>
                    </div>
                    <span class="badge success">+5</span>
                </div>
                <h3>${state.data.orders.filter(o => o.status !== 'Completed').length}</h3>
                <p>Active Orders</p>
            </div>
            <div class="stat-card">
                <div class="header">
                    <div class="icon-box" style="background: rgba(16, 185, 129, 0.1); color: #10b981;">
                        <span class="material-icons-round">trending_up</span>
                    </div>
                     <span class="badge success">+2.4%</span>
                </div>
                <h3>₹2,450</h3>
                <p>Avg. Order Value</p>
            </div>
        </div>

        <div class="table-container">
            <div style="padding: 1.5rem; border-bottom: 1px solid var(--border);">
                <h3>Recent Orders</h3>
            </div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Order ID</th>
                        <th>Customer</th>
                        <th>Amount</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${state.data.orders.map(order => `
                        <tr>
                            <td>${order.id}</td>
                            <td>${order.customer}</td>
                            <td>₹${order.amount}</td>
                            <td>
                                <span class="badge ${order.status === 'Completed' ? 'success' : 'warning'}">
                                    ${order.status}
                                </span>
                            </td>
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
            <div style="padding: 1.5rem; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
                <h3>Products Inventory</h3>
                <button style="padding: 0.5rem 1rem; background: var(--primary); color: white; border: none; border-radius: 4px; cursor: pointer;">
                    + Add Product
                </button>
            </div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Product Name</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${state.data.products.map(p => `
                        <tr>
                            <td>#${p.id}</td>
                            <td>${p.name}</td>
                            <td>₹${p.price}</td>
                            <td>${p.stock}</td>
                            <td>
                                <span class="badge ${p.stock > 20 ? 'success' : 'warning'}">
                                    ${p.stock > 20 ? 'In Stock' : 'Low Stock'}
                                </span>
                            </td>
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

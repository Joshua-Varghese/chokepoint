import { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase-config';
import { LayoutDashboard, Package, Warehouse, ShoppingBag, Router, Users, LogOut, ChevronLeft, Menu } from 'lucide-react';

export default function DashboardLayout() {
    const navigate = useNavigate();
    const [isCollapsed, setIsCollapsed] = useState(false);

    const handleLogout = async () => {
        await signOut(auth);
        navigate('/login');
    };

    const navItems = [
        { label: 'Overview', path: '/', icon: <LayoutDashboard size={20} /> },
        { label: 'Products', path: '/products', icon: <Package size={20} /> },
        { label: 'Inventory', path: '/inventory', icon: <Warehouse size={20} /> },
        { label: 'Orders', path: '/orders', icon: <ShoppingBag size={20} /> },
        { label: 'Devices', path: '/devices', icon: <Router size={20} /> },
        { label: 'Customers', path: '/customers', icon: <Users size={20} /> },
    ];

    return (
        <div className="layout">
            <aside className={`sidebar ${isCollapsed ? 'collapsed' : ''}`}>
                <button
                    className="sidebar-toggle"
                    onClick={() => setIsCollapsed(!isCollapsed)}
                    title={isCollapsed ? "Expand Sidebar" : "Collapse Sidebar"}
                >
                    {isCollapsed ? <Menu size={14} /> : <ChevronLeft size={14} />}
                </button>

                <div style={{ display: 'flex', alignItems: 'center', justifyContent: isCollapsed ? 'center' : 'flex-start', gap: '0.75rem', marginBottom: '2rem' }}>
                    <div style={{ width: '24px', height: '24px', background: '#000', borderRadius: '4px', flexShrink: 0 }}></div>
                    {!isCollapsed && <h2 style={{ fontSize: '1.25rem', fontWeight: '700', whiteSpace: 'nowrap' }}>Chokepoint</h2>}
                </div>

                <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', flex: 1 }}>
                    {navItems.map(item => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            title={isCollapsed ? item.label : undefined}
                            className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-ghost'}`}
                            style={({ isActive }) => ({
                                justifyContent: isCollapsed ? 'center' : 'flex-start',
                                padding: isCollapsed ? '0.5rem' : '0.5rem 1rem',
                                background: isActive ? '#000' : 'transparent',
                                color: isActive ? '#fff' : '#000',
                                border: isActive ? 'none' : '1px solid transparent'
                            })}
                        >
                            <div style={{ flexShrink: 0, display: 'flex' }}>{item.icon}</div>
                            <span className="nav-label">{item.label}</span>
                        </NavLink>
                    ))}
                </nav>

                <button
                    onClick={handleLogout}
                    className="btn btn-outline"
                    title="Logout"
                    style={{
                        marginTop: 'auto',
                        borderRadius: '6px',
                        justifyContent: isCollapsed ? 'center' : 'flex-start',
                        padding: isCollapsed ? '0.5rem' : '0.5rem 1rem'
                    }}
                >
                    <div style={{ flexShrink: 0, display: 'flex' }}><LogOut size={18} /></div>
                    <span className="nav-label">Logout</span>
                </button>
            </aside>

            <main className="main">
                <Outlet />
            </main>
        </div>
    );
}

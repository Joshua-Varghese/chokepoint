import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { signOut } from 'firebase/auth';
import { auth } from '../firebase-config';
import { LayoutDashboard, Package, ShoppingBag, Router, Users, LogOut } from 'lucide-react';

export default function DashboardLayout() {
    const navigate = useNavigate();

    const handleLogout = async () => {
        await signOut(auth);
        navigate('/login');
    };

    const navItems = [
        { label: 'Overview', path: '/', icon: <LayoutDashboard size={20} /> },
        { label: 'Products', path: '/products', icon: <Package size={20} /> },
        { label: 'Orders', path: '/orders', icon: <ShoppingBag size={20} /> },
        { label: 'Devices', path: '/devices', icon: <Router size={20} /> },
        { label: 'Customers', path: '/customers', icon: <Users size={20} /> },
    ];

    return (
        <div className="layout">
            <aside className="sidebar">
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '2rem' }}>
                    <div style={{ width: '24px', height: '24px', background: '#000', borderRadius: '4px' }}></div>
                    <h2 style={{ fontSize: '1.25rem', fontWeight: '700' }}>Chokepoint</h2>
                </div>

                <nav style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', flex: 1 }}>
                    {navItems.map(item => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) => `btn ${isActive ? 'btn-primary' : 'btn-ghost'}`}
                            style={({ isActive }) => ({
                                justifyContent: 'flex-start',
                                background: isActive ? '#000' : 'transparent',
                                color: isActive ? '#fff' : '#000',
                                border: isActive ? 'none' : '1px solid transparent'
                            })}
                        >
                            {item.icon}
                            {item.label}
                        </NavLink>
                    ))}
                </nav>

                <button onClick={handleLogout} className="btn btn-outline" style={{ marginTop: 'auto', borderRadius: '6px' }}>
                    <LogOut size={18} /> Logout
                </button>
            </aside>

            <main className="main">
                <Outlet />
            </main>
        </div>
    );
}

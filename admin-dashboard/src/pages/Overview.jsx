import { useEffect, useState } from 'react';
import { collection, getDocs } from 'firebase/firestore';
import { db } from '../firebase-config';

export default function Overview() {
    const [stats, setStats] = useState({ products: 0, devices: 0 });
    const [recentDevices, setRecentDevices] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchData() {
            try {
                const prodSnap = await getDocs(collection(db, 'products'));
                const devSnap = await getDocs(collection(db, 'devices'));

                setStats({
                    products: prodSnap.size,
                    devices: devSnap.size
                });

                const devices = devSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
                setRecentDevices(devices.slice(0, 5));
            } catch (error) {
                console.error("Error fetching overview data:", error);
            } finally {
                setLoading(false);
            }
        }
        fetchData();
    }, []);

    if (loading) return <div>Loading data...</div>;

    return (
        <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1.5rem' }}>Overview</h1>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
                <div className="card">
                    <h3 style={{ fontSize: '2rem', fontWeight: '700', marginBottom: '0.5rem' }}>{stats.products}</h3>
                    <p style={{ color: 'var(--text-muted)' }}>Total Products</p>
                </div>
                <div className="card">
                    <h3 style={{ fontSize: '2rem', fontWeight: '700', marginBottom: '0.5rem' }}>{stats.devices}</h3>
                    <p style={{ color: 'var(--text-muted)' }}>Active Devices</p>
                </div>
            </div>

            <div className="card" style={{ padding: '0' }}>
                <div style={{ padding: '1rem 1.5rem', borderBottom: '1px solid var(--border)' }}>
                    <h3 style={{ fontWeight: '600' }}>Recent Devices</h3>
                </div>
                <div className="table-container" style={{ border: 'none', borderRadius: '0' }}>
                    <table>
                        <thead>
                            <tr>
                                <th>Device ID</th>
                                <th>Name</th>
                                <th>Last Seen</th>
                            </tr>
                        </thead>
                        <tbody>
                            {recentDevices.map(d => (
                                <tr key={d.id}>
                                    <td>{d.id}</td>
                                    <td>{d.name || 'Unknown'}</td>
                                    <td>{d.lastSeen ? new Date(d.lastSeen.seconds * 1000).toLocaleDateString() : 'Never'}</td>
                                </tr>
                            ))}
                            {recentDevices.length === 0 && <tr><td colSpan="3" style={{ textAlign: 'center' }}>No devices found</td></tr>}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

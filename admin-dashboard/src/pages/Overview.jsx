import { useEffect, useState } from 'react';
import { collection, getDocs, query, orderBy, limit } from 'firebase/firestore';
import { db } from '../firebase-config';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function Overview() {
    const [stats, setStats] = useState({ products: 0, devices: 0 });
    const [recentDevices, setRecentDevices] = useState([]);
    const [chartData, setChartData] = useState([]);
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

                // Fetch recent sensor readings for chart
                const readingsQ = query(collection(db, 'sensor_readings'), orderBy('timestamp', 'desc'), limit(30));
                const readingsSnap = await getDocs(readingsQ);

                const rawReadings = readingsSnap.docs.map(doc => doc.data()).reverse(); // chronological
                const data = rawReadings.map((r, i) => ({
                    time: r.timestamp ? new Date(r.timestamp.seconds * 1000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : i,
                    co2: r.co2 || 0,
                    nh3: r.nh3 || 0
                }));
                setChartData(data);

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

            <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 2fr) minmax(0, 1fr)', gap: '1.5rem' }}>
                <div className="card" style={{ padding: '1.5rem' }}>
                    <h3 style={{ fontWeight: '600', marginBottom: '1.5rem', color: '#111827' }}>System Air Quality (CO₂)</h3>
                    <div style={{ height: '300px' }}>
                        {chartData.length > 0 ? (
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={chartData}>
                                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                                    <XAxis dataKey="time" tick={{ fontSize: 12, fill: '#9ca3af' }} axisLine={false} tickLine={false} dy={10} />
                                    <YAxis tick={{ fontSize: 12, fill: '#9ca3af' }} axisLine={false} tickLine={false} dx={-10} />
                                    <Tooltip contentStyle={{ borderRadius: '8px', border: '1px solid #e5e7eb', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }} />
                                    <Line type="monotone" dataKey="co2" name="CO₂ (ppm)" stroke="#10b981" strokeWidth={3} dot={false} activeDot={{ r: 6, fill: '#10b981', stroke: '#fff', strokeWidth: 2 }} />
                                </LineChart>
                            </ResponsiveContainer>
                        ) : (
                            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%', color: '#9ca3af', background: '#f9fafb', borderRadius: '8px' }}>
                                No sensor data points available yet.
                            </div>
                        )}
                    </div>
                </div>

                <div className="card" style={{ padding: '0', display: 'flex', flexDirection: 'column' }}>
                    <div style={{ padding: '1.25rem 1.5rem', borderBottom: '1px solid var(--border)' }}>
                        <h3 style={{ fontWeight: '600', color: '#111827' }}>Recent Devices</h3>
                    </div>
                    <div className="table-container" style={{ border: 'none', borderRadius: '0', flex: 1 }}>
                        <table style={{ margin: 0 }}>
                            <thead style={{ background: '#f9fafb' }}>
                                <tr>
                                    <th style={{ padding: '0.75rem 1.5rem' }}>Device ID</th>
                                    <th style={{ padding: '0.75rem 1.5rem' }}>Name</th>
                                    <th style={{ padding: '0.75rem 1.5rem' }}>Last Seen</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentDevices.map(d => (
                                    <tr key={d.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                                        <td style={{ padding: '0.75rem 1.5rem', fontSize: '0.875rem', fontFamily: 'monospace' }}>{d.id.substring(0, 8)}...</td>
                                        <td style={{ padding: '0.75rem 1.5rem', fontSize: '0.875rem', fontWeight: 500 }}>{d.name || 'Unknown'}</td>
                                        <td style={{ padding: '0.75rem 1.5rem', fontSize: '0.875rem', color: '#6b7280' }}>
                                            {d.lastSeen ? new Date(d.lastSeen.seconds * 1000).toLocaleDateString() : 'Never'}
                                        </td>
                                    </tr>
                                ))}
                                {recentDevices.length === 0 && <tr><td colSpan="3" style={{ textAlign: 'center', padding: '2rem', color: '#9ca3af' }}>No devices found</td></tr>}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    );
}

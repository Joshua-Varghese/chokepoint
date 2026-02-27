import { useEffect, useState, Fragment } from 'react';
import { collection, getDocs, doc, getDoc, onSnapshot } from 'firebase/firestore';
import { db } from '../firebase-config';
import { useNavigate } from 'react-router-dom';
import { Terminal, Search, Wifi, WifiOff, Activity, ChevronDown, ChevronUp } from 'lucide-react';

export default function Devices() {
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [expandedDevice, setExpandedDevice] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchDevicesAndUsers = async () => {
            try {
                // 1. Fetch Devices
                const snap = await getDocs(collection(db, 'devices'));
                const deviceList = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));

                // 2. Fetch User Names for unique adminIds
                const ownerIds = [...new Set(deviceList.map(d => d.adminId).filter(Boolean))];
                const userMap = {};

                // Note: Ideally use 'where(documentId(), "in", ownerIds)' but it has limits (max 10/30).
                // For simplicity in this admin dashboard, we'll fetch them individually or use a smart lookup.
                // Given the scale, fetching individual docs in parallel is acceptable for now.

                await Promise.all(ownerIds.map(async (uid) => {
                    try {
                        const userDoc = await getDoc(doc(db, "users", uid));
                        if (userDoc.exists()) {
                            userMap[uid] = userDoc.data().name;
                        }
                    } catch (e) { console.error("Failed to fetch user", uid, e); }
                }));

                // 3. Attach names
                const enrichedDevices = deviceList.map(d => ({
                    ...d,
                    ownerName: userMap[d.adminId] || (d.adminId ? `User ${d.adminId.slice(0, 5)}` : 'Unclaimed')
                }));

                setDevices(enrichedDevices);
            } catch (error) {
                console.error("Error fetching data:", error);
            } finally {
                setLoading(false);
            }
        };
        fetchDevicesAndUsers();
    }, []);

    // Calculate if device is online (seen in last 5 minutes)
    const isOnline = (lastSeen) => {
        if (!lastSeen) return false;
        const seenDate = new Date(lastSeen.seconds * 1000);
        const now = new Date();
        const diffMinutes = (now - seenDate) / 1000 / 60;
        return diffMinutes < 5;
    };

    const filteredDevices = devices.filter(d => {
        const term = (searchTerm || '').toLowerCase();
        return (d.id && d.id.toLowerCase().includes(term)) ||
            (d.name && d.name.toLowerCase().includes(term)) ||
            (d.ownerName && typeof d.ownerName === 'string' && d.ownerName.toLowerCase().includes(term));
    });

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: '700' }}>Device Management</h1>

                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'stretch' }}>
                    <div style={{ position: 'relative', width: '260px' }}>
                        <Search style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: '#9ca3af' }} size={16} />
                        <input
                            type="text"
                            placeholder="Search by ID, Name, or Username..."
                            className="input"
                            style={{ paddingLeft: '2.5rem', height: '100%', width: '100%' }}
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                </div>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm" style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <thead className="bg-gray-50 text-gray-500 font-medium border-b border-gray-100">
                            <tr>
                                <th style={{ padding: '1rem' }}>Device ID</th>
                                <th style={{ padding: '1rem' }}>Device Name</th>
                                <th style={{ padding: '1rem' }}>Assigned Owner</th>
                                <th style={{ padding: '1rem' }}>Status</th>
                                <th style={{ padding: '1rem' }}>Last Ping</th>
                                <th style={{ padding: '1rem', textAlign: 'right' }}>More</th>
                            </tr>
                        </thead>
                        <tbody style={{ divideY: '1px solid #f3f4f6' }}>
                            {loading ? (
                                <tr>
                                    <td colSpan="6" style={{ padding: '2rem', textAlign: 'center', color: '#6b7280' }}>
                                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto mb-2"></div>
                                        Discovering network devices...
                                    </td>
                                </tr>
                            ) : filteredDevices.length === 0 ? (
                                <tr>
                                    <td colSpan="6" style={{ padding: '2rem', textAlign: 'center', color: '#6b7280' }}>
                                        No devices found matching your search.
                                    </td>
                                </tr>
                            ) : filteredDevices.map(d => {
                                const online = isOnline(d.lastSeen);
                                return (
                                    <Fragment key={d.id}>
                                        <tr style={{ transition: 'background-color 0.2s', borderBottom: '1px solid #f3f4f6' }} onMouseOver={e => e.currentTarget.style.backgroundColor = '#f9fafb'} onMouseOut={e => e.currentTarget.style.backgroundColor = 'transparent'}>
                                            <td style={{ padding: '1rem', fontFamily: 'monospace', fontSize: '0.75rem', color: '#4b5563' }}>
                                                {d.id}
                                            </td>
                                            <td style={{ padding: '1rem', fontWeight: '500', color: '#111827' }}>
                                                {d.name || 'Unnamed Sentinel'}
                                            </td>
                                            <td style={{ padding: '1rem', color: '#4b5563' }}>
                                                {d.ownerName}
                                            </td>
                                            <td style={{ padding: '1rem' }}>
                                                <div style={{
                                                    display: 'inline-flex', alignItems: 'center', gap: '0.375rem',
                                                    padding: '0.25rem 0.75rem', borderRadius: '9999px', fontSize: '0.75rem', fontWeight: '600',
                                                    backgroundColor: online ? '#dcfce7' : '#f3f4f6',
                                                    color: online ? '#166534' : '#6b7280',
                                                    border: `1px solid ${online ? '#bbf7d0' : '#e5e7eb'}`
                                                }}>
                                                    {online ? <Wifi size={12} /> : <WifiOff size={12} />}
                                                    {online ? 'Online' : 'Offline'}
                                                </div>
                                            </td>
                                            <td style={{ padding: '1rem', color: '#6b7280', fontSize: '0.8rem' }}>
                                                {d.lastSeen ? new Date(d.lastSeen.seconds * 1000).toLocaleString() : 'Never'}
                                            </td>
                                            <td style={{ padding: '1rem', textAlign: 'right' }}>
                                                <button
                                                    onClick={() => setExpandedDevice(expandedDevice === d.id ? null : d.id)}
                                                    className="btn btn-ghost"
                                                    style={{ padding: '0.35rem 0.5rem', border: '1px solid #e5e7eb', borderRadius: '6px', background: '#f9fafb', cursor: 'pointer' }}
                                                >
                                                    {expandedDevice === d.id ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                                </button>
                                            </td>
                                        </tr>

                                        {expandedDevice === d.id && (
                                            <tr style={{ background: '#fafafa', borderBottom: '1px solid #e5e7eb' }}>
                                                <td colSpan="6" style={{ padding: 0 }}>
                                                    <div style={{ padding: '1.5rem', display: 'flex', gap: '2rem', alignItems: 'flex-start' }}>

                                                        {/* Metadata Card */}
                                                        <div style={{ flex: 1, backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '1rem' }}>
                                                            <h4 style={{ fontSize: '0.875rem', fontWeight: '600', color: '#374151', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                                <Activity size={16} /> Diagnostic Overview
                                                            </h4>
                                                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.8rem' }}>
                                                                <div>
                                                                    <div style={{ color: '#9ca3af', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Network Binding</div>
                                                                    <div style={{ color: '#111827', fontWeight: '500' }}>{d.lastIp || 'DHCP Unresolved'}</div>
                                                                </div>
                                                                <div>
                                                                    <div style={{ color: '#9ca3af', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Firmware Config</div>
                                                                    <div style={{ color: '#111827', fontWeight: '500' }}>OTA Enabled</div>
                                                                </div>
                                                                <div>
                                                                    <div style={{ color: '#9ca3af', fontSize: '0.7rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>Provisioned Date</div>
                                                                    <div style={{ color: '#111827', fontWeight: '500' }}>
                                                                        {d.provisionedAt ? new Date(d.provisionedAt.seconds * 1000).toLocaleDateString() : 'Legacy Device'}
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>

                                                        {/* Action Card */}
                                                        <div style={{ flex: 1, backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px', padding: '1rem', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', textAlign: 'center' }}>
                                                            <div style={{ marginBottom: '1rem', color: '#6b7280', fontSize: '0.875rem' }}>
                                                                Access direct MQTT WebSockets terminal routing and file manipulation matrix for this specific module.
                                                            </div>
                                                            <button
                                                                onClick={() => navigate(`/devices/${d.id}`)}
                                                                style={{ backgroundColor: '#111827', color: '#fff', border: 'none', padding: '0.75rem 1.5rem', borderRadius: '6px', fontSize: '0.875rem', fontWeight: '600', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem', transition: 'background-color 0.2s' }}
                                                                onMouseOver={e => e.currentTarget.style.backgroundColor = '#374151'}
                                                                onMouseOut={e => e.currentTarget.style.backgroundColor = '#111827'}
                                                            >
                                                                <Terminal size={16} /> Open Device Terminal & Logs
                                                            </button>
                                                        </div>

                                                    </div>
                                                </td>
                                            </tr>
                                        )}
                                    </Fragment>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

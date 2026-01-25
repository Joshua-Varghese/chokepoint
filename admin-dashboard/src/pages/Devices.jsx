import { useEffect, useState } from 'react';
import { collection, getDocs, doc, getDoc } from 'firebase/firestore';
import { db } from '../firebase-config';
import { useNavigate } from 'react-router-dom';
import { Router } from 'lucide-react';

export default function Devices() {
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
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

    return (
        <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1.5rem' }}>Device Management</h1>
            <div className="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Device ID</th>
                            <th>Name</th>
                            <th>Owner</th>
                            <th>Last Seen</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="5">Loading...</td></tr>
                        ) : devices.map(d => (
                            <tr key={d.id}>
                                <td>{d.id}</td>
                                <td>{d.name || 'Unknown'}</td>
                                <td>{d.ownerName}</td>
                                <td>{d.lastSeen ? new Date(d.lastSeen.seconds * 1000).toLocaleString() : 'Never'}</td>
                                <td>
                                    <button
                                        className="btn btn-primary"
                                        style={{ padding: '0.25rem 0.75rem', fontSize: '0.75rem', background: '#000' }}
                                        onClick={() => navigate(`/devices/${d.id}`)}
                                    >
                                        Manage
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

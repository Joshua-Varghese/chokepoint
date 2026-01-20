import { useEffect, useState } from 'react';
import { collection, getDocs } from 'firebase/firestore';
import { db } from '../firebase-config';
import { useNavigate } from 'react-router-dom';
import { Router } from 'lucide-react';

export default function Devices() {
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchDevices = async () => {
            try {
                const snap = await getDocs(collection(db, 'devices'));
                setDevices(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
            } catch (error) {
                console.error("Error fetching devices:", error);
            } finally {
                setLoading(false);
            }
        };
        fetchDevices();
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
                                <td>{d.ownerId || 'Unclaimed'}</td>
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

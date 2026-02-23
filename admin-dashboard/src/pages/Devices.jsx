import { useEffect, useState } from 'react';
import { collection, getDocs, doc, getDoc, setDoc, onSnapshot } from 'firebase/firestore';
import { db } from '../firebase-config';
import { useNavigate } from 'react-router-dom';
import { Upload } from 'lucide-react';

export default function Devices() {
    const [devices, setDevices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [firmware, setFirmware] = useState({ version: '', url: '' });
    const [savingFirmware, setSavingFirmware] = useState(false);
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

        // Listen to current firmware
        const unsub = onSnapshot(doc(db, 'system_config', 'firmware'), (docSnap) => {
            if (docSnap.exists()) {
                setFirmware(docSnap.data());
            }
        });

        return () => unsub();
    }, []);

    const handlePushFirmware = async () => {
        if (!firmware.version || !firmware.url) return alert('Enter version and URL');
        setSavingFirmware(true);
        try {
            await setDoc(doc(db, 'system_config', 'firmware'), {
                version: firmware.version,
                url: firmware.url,
                updatedAt: new Date()
            });
            alert('Firmware update pushed to devices!');
        } catch (e) {
            console.error(e);
            alert('Failed to push firmware');
        } finally {
            setSavingFirmware(false);
        }
    };

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: '700' }}>Device Management</h1>
            </div>

            <div className="card" style={{ marginBottom: '2rem', padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.125rem', fontWeight: '600', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Upload size={20} /> OTA Firmware Update
                </h3>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr auto', gap: '1rem', alignItems: 'end' }}>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                        <label>Target Version</label>
                        <input
                            type="text"
                            className="input"
                            placeholder="e.g., 1.0.2"
                            value={firmware.version}
                            onChange={(e) => setFirmware({ ...firmware, version: e.target.value })}
                        />
                    </div>
                    <div className="form-group" style={{ marginBottom: 0 }}>
                        <label>Firmware File URL (.py or .bin)</label>
                        <input
                            type="text"
                            className="input"
                            placeholder="https://raw.githubusercontent.com/..."
                            value={firmware.url}
                            onChange={(e) => setFirmware({ ...firmware, url: e.target.value })}
                        />
                    </div>
                    <button
                        className="btn btn-primary"
                        onClick={handlePushFirmware}
                        disabled={savingFirmware}
                    >
                        {savingFirmware ? 'Pushing...' : 'Push Update'}
                    </button>
                </div>
                <p style={{ fontSize: '0.85rem', color: '#71717a', marginTop: '1rem' }}>
                    Devices will automatically download and apply this firmware upon their next boot or polling cycle. Ensure the URL points directly to the RAW file content.
                </p>
            </div>
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

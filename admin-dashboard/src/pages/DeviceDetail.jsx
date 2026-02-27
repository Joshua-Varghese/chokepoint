import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { doc, getDoc } from 'firebase/firestore';
import { db } from '../firebase-config';
import { ArrowLeft, Activity, Terminal, FileCode, RefreshCw, Save, Trash } from 'lucide-react';
import toast from 'react-hot-toast';

export default function DeviceDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const [device, setDevice] = useState(null);
    const [wsConnected, setWsConnected] = useState(false);
    const [consoleLogs, setConsoleLogs] = useState([]);

    // File Manager State
    const [files, setFiles] = useState([]);
    const [editingFile, setEditingFile] = useState(null); // { path: '', content: '' }
    const [editorContent, setEditorContent] = useState('');

    // WebSocket Client State
    const [wsClient, setWsClient] = useState(null);

    useEffect(() => {
        // Fetch Device Metadata
        const fetchDevice = async () => {
            const docRef = doc(db, 'devices', id);
            const docSnap = await getDoc(docRef);
            if (docSnap.exists()) {
                setDevice({ id: docSnap.id, ...docSnap.data() });
            }
        };
        fetchDevice();

        // Connect to Local Node.js WebSocket Proxy
        const ws = new WebSocket('ws://localhost:8080');

        ws.onopen = () => {
            console.log("WebSocket Proxy Connected");
            setWsConnected(true);
            ws.send(JSON.stringify({ action: 'subscribe', deviceId: id }));
        };

        ws.onmessage = (event) => {
            try {
                const payload = JSON.parse(event.data);
                handleMqttResponse(payload);
            } catch (e) {
                console.error("Parse Error from Proxy", e);
            }
        };

        ws.onerror = (err) => {
            console.error("WebSocket Error:", err);
            setWsConnected(false);
        };

        ws.onclose = () => {
            console.log("WebSocket Proxy Disconnected");
            setWsConnected(false);
        };

        setWsClient(ws);

        return () => {
            if (ws) ws.close();
        };
    }, [id]);

    const handleMqttResponse = (res) => {
        if (res.cmd === 'ls') {
            setFiles(res.files);
            setConsoleLogs(prev => [`[RES] Listed ${res.files.length} files`, ...prev]);
        } else if (res.cmd === 'read') {
            setEditingFile(res.path);
            setEditorContent(res.content);
            setConsoleLogs(prev => [`[RES] Read ${res.path} (${res.content.length} bytes)`, ...prev]);
        } else if (res.cmd === 'write') {
            setConsoleLogs(prev => [`[RES] Write ${res.path}: ${res.status}`, ...prev]);
            if (res.status === 'ok') toast.success('File Saved Successfully!');
        } else if (res.cmd === 'rm') {
            setConsoleLogs(prev => [`[RES] Deleted ${res.path}: ${res.status}`, ...prev]);
            refreshFiles(); // Refresh list
        }
    };

    const sendCmd = (payload) => {
        if (!wsClient || wsClient.readyState !== WebSocket.OPEN) return;
        wsClient.send(JSON.stringify({ action: 'publish', deviceId: id, payload: payload }));
        setConsoleLogs(prev => [`[CMD] ${payload.cmd} ${payload.path || ''}`, ...prev]);
    };

    const refreshFiles = () => sendCmd({ cmd: 'ls' });
    const readFile = (filename) => sendCmd({ cmd: 'read', path: filename });
    const deleteFile = (filename) => {
        if (confirm(`Delete ${filename}?`)) sendCmd({ cmd: 'rm', path: filename });
    };
    const saveFile = () => {
        if (!editingFile) return;
        sendCmd({ cmd: 'write', path: editingFile, content: editorContent });
    };
    const restartDevice = () => sendCmd({ cmd: 'restart' });

    if (!device) return <div style={{ padding: '2rem' }}>Loading...</div>;

    // Calculate actual device status based on last heartbeat (5 minute threshold)
    const isOnline = () => {
        if (!device?.lastSeen) return false;
        const lastPingSeconds = device.lastSeen.seconds || device.lastSeen._seconds;
        if (!lastPingSeconds) return false;
        return (Date.now() / 1000) - lastPingSeconds < 300;
    };

    const deviceStatus = isOnline() ? 'connected' : 'offline';

    return (
        <div>
            <button onClick={() => navigate('/devices')} className="btn btn-ghost" style={{ marginBottom: '1rem', paddingLeft: 0 }}>
                <ArrowLeft size={18} /> Back to Devices
            </button>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                <div>
                    <h1 style={{ fontSize: '2rem', fontWeight: '700', marginBottom: '0.5rem' }}>{device.name || 'Unknown Device'}</h1>
                    <p style={{ color: 'var(--text-muted)' }}>ID: {device.id}</p>
                </div>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                    {!wsConnected && (
                        <span style={{ fontSize: '0.75rem', color: '#dc2626', fontWeight: '500' }}>Proxy Disconnected</span>
                    )}
                    <div className="badge" style={{ background: deviceStatus === 'connected' ? '#dcfce7' : '#fee2e2', color: deviceStatus === 'connected' ? '#166534' : '#991b1b', padding: '0.25rem 0.75rem', borderRadius: '99px', fontSize: '0.875rem', fontWeight: '500' }}>
                        MQTT: {deviceStatus}
                    </div>
                    <button onClick={restartDevice} className="btn btn-outline" style={{ fontSize: '0.8rem' }} disabled={deviceStatus !== 'connected'}>Reboot Device</button>
                </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 1fr) 2fr', gap: '2rem' }}>
                {/* Left Column: List */}
                <div className="card">
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <FileCode size={20} /> <h3 style={{ fontWeight: '600' }}>File Manager</h3>
                        </div>
                        <button onClick={refreshFiles} title="Refresh File List" style={{ background: 'none', border: 'none', cursor: 'pointer' }}><RefreshCw size={16} /></button>
                    </div>

                    {files.length === 0 ? (
                        <div style={{ textAlign: 'center', padding: '2rem', color: '#999' }}>
                            <p>No files listed.</p>
                            <button onClick={refreshFiles} className="btn-link">Click refresh to fetch</button>
                        </div>
                    ) : (
                        <ul style={{ listStyle: 'none', padding: 0 }}>
                            {files.map(f => (
                                <li key={f} style={{ padding: '0.5rem', borderBottom: '1px solid #eee', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <span style={{ fontFamily: 'monospace' }}>{f}</span>
                                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                                        <button onClick={() => readFile(f)} className="btn btn-xs btn-outline">Edit</button>
                                        <button onClick={() => deleteFile(f)} style={{ color: 'red', background: 'none', border: 'none', cursor: 'pointer' }}><Trash size={14} /></button>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>

                {/* Right Column: Editor */}
                <div className="card" style={{ display: 'flex', flexDirection: 'column', height: '500px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem', borderBottom: '1px solid #eee', paddingBottom: '0.5rem' }}>
                        <h3 style={{ fontWeight: '600' }}>{editingFile ? editingFile : 'No File Selected'}</h3>
                        <div style={{ display: 'flex', gap: '0.5rem' }}>
                            {editingFile && (
                                <button onClick={saveFile} className="btn btn-primary" style={{ background: '#000', fontSize: '0.8rem' }}>
                                    <Save size={14} /> Save
                                </button>
                            )}
                        </div>
                    </div>
                    <textarea
                        style={{ flex: 1, width: '100%', fontFamily: 'monospace', fontSize: '14px', border: 'none', resize: 'none', outline: 'none', background: '#fafafa', padding: '1rem' }}
                        value={editorContent}
                        onChange={(e) => setEditorContent(e.target.value)}
                        placeholder="Select a file to view/edit content..."
                    />
                </div>
            </div>

            {/* Console Log */}
            <div className="card" style={{ marginTop: '2rem', background: '#000', color: '#0f0', fontFamily: 'monospace', maxHeight: '150px', overflowY: 'auto' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <Terminal size={14} /> <span style={{ fontSize: '0.75rem', fontWeight: '600' }}>SYSTEM LOG</span>
                </div>
                {consoleLogs.map((log, i) => (
                    <div key={i} style={{ fontSize: '0.8rem', opacity: 0.8 }}>{log}</div>
                ))}
            </div>
        </div>
    );
}

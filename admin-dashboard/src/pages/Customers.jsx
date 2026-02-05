import { useEffect, useState } from 'react';
import { collection, getDocs } from 'firebase/firestore';
import { db } from '../firebase-config';
import { User, ShoppingCart } from 'lucide-react';

export default function Customers() {
    const [customers, setCustomers] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchCustomers = async () => {
            try {
                // Determine users that have presence in Firestore (e.g. have a cart or devices)
                const snap = await getDocs(collection(db, 'users'));
                setCustomers(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
            } catch (error) {
                console.error("Error fetching customers:", error);
            } finally {
                setLoading(false);
            }
        };
        fetchCustomers();
    }, []);

    return (
        <div>
            <h1 style={{ fontSize: '1.5rem', fontWeight: '700', marginBottom: '1.5rem' }}>Customers</h1>

            <div className="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>User ID (UID)</th>
                            <th>Status</th>
                            <th>Shopping</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="4" style={{ textAlign: 'center' }}>Loading...</td></tr>
                        ) : customers.length === 0 ? (
                            <tr><td colSpan="4" style={{ textAlign: 'center', color: '#71717a' }}>No customers found in Firestore yet.</td></tr>
                        ) : (
                            customers.map(c => (
                                <tr key={c.id}>
                                    <td style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{c.id}</td>
                                    <td>
                                        <span className="badge success">Active</span>
                                    </td>
                                    <td>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.85rem' }}>
                                            <ShoppingCart size={14} />
                                            <span>Cart Active</span>
                                        </div>
                                    </td>
                                    <td>
                                        <button className="btn btn-ghost" style={{ fontSize: '0.75rem' }} disabled>
                                            View Details
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            <div style={{ marginTop: '2rem', padding: '1rem', background: '#f4f4f5', borderRadius: '8px', fontSize: '0.875rem', color: '#71717a' }}>
                <strong>Note:</strong> Currently listing users who have triggered Firestore writes (e.g. added items to Cart).
                Full user profiles (Email, Name) require Firestore syncing on Sign Up.
            </div>
        </div>
    );
}

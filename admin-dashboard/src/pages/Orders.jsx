import React, { useState, useEffect, Fragment } from 'react';
import { collection, onSnapshot, query, orderBy, doc, updateDoc, arrayUnion } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Package, ChevronDown, ChevronUp, Search, Filter, Printer, MessageSquare, Truck } from 'lucide-react';
import toast from 'react-hot-toast';

export default function Orders() {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expandedOrder, setExpandedOrder] = useState(null);
    const [filterStatus, setFilterStatus] = useState('all');
    const [searchTerm, setSearchTerm] = useState('');

    // Enterprise Handling States
    const [trackingInput, setTrackingInput] = useState({ provider: '', number: '' });
    const [noteInput, setNoteInput] = useState('');

    // Confirmation Modal State
    const [confirmModal, setConfirmModal] = useState({
        isOpen: false,
        orderId: null,
        newStatus: null
    });

    useEffect(() => {
        const q = query(collection(db, 'orders'), orderBy('createdAt', 'desc'));
        const unsubscribe = onSnapshot(q, (snapshot) => {
            const ordersData = snapshot.docs.map(doc => ({
                id: doc.id,
                ...doc.data()
            }));
            setOrders(ordersData);
            setLoading(false);
        });

        return () => unsubscribe();
    }, []);

    const handleSaveTracking = async (orderId) => {
        if (!trackingInput.provider || !trackingInput.provider.trim() || !trackingInput.number || !trackingInput.number.trim()) {
            toast.error('Both Courier Provider and Tracking Number are required.');
            return;
        }

        try {
            const orderRef = doc(db, 'orders', orderId);
            await updateDoc(orderRef, {
                tracking: {
                    provider: trackingInput.provider.trim(),
                    number: trackingInput.number.trim(),
                    updatedAt: new Date().toISOString()
                }
            });
            toast.success('Tracking details updated successfully');
        } catch (error) {
            console.error('Error saving tracking:', error);
            toast.error('Failed to update tracking details');
        }
    };

    const handleAddNote = async (orderId) => {
        if (!noteInput || !noteInput.trim()) {
            toast.error('Note cannot be empty.');
            return;
        }

        try {
            await updateDoc(doc(db, 'orders', orderId), {
                notes: arrayUnion({
                    text: noteInput,
                    timestamp: new Date().toISOString(),
                    author: "Admin"
                })
            });
            setNoteInput('');
            toast.success("Note added");
        } catch (error) {
            toast.error("Failed to add note");
        }
    };

    const handlePrintInvoice = (orderId) => {
        window.print();
    };

    const confirmStatusUpdate = (orderId, newStatus) => {
        if (newStatus === 'cancelled' || newStatus === 'refunded') {
            setConfirmModal({ isOpen: true, orderId, newStatus });
        } else {
            handleStatusUpdate(orderId, newStatus);
        }
    };

    const handleStatusUpdate = async (orderId, newStatus) => {
        try {
            await updateDoc(doc(db, 'orders', orderId), { status: newStatus });
            toast.success(`Order status updated to ${newStatus}`);
            setConfirmModal({ isOpen: false, orderId: null, newStatus: null });
        } catch (error) {
            console.error("Error updating status:", error);
            toast.error("Failed to update status");
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'paid': return 'bg-green-100 text-green-800';
            case 'pending_payment': return 'bg-yellow-100 text-yellow-800';
            case 'shipped': return 'bg-blue-100 text-blue-800';
            case 'delivered': return 'bg-purple-100 text-purple-800';
            case 'cancelled': return 'bg-red-100 text-red-800';
            case 'refunded': return 'bg-orange-100 text-orange-800';
            default: return 'bg-gray-100 text-gray-800';
        }
    };

    const filteredOrders = orders.filter(order => {
        const matchesStatus = filterStatus === 'all' || order.status === filterStatus;
        const matchesSearch = searchTerm === '' ||
            order.id?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            order.customer?.firstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            order.customer?.lastName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            order.customer?.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
            (order.paymentId && order.paymentId.toLowerCase().includes(searchTerm.toLowerCase()));

        return matchesStatus && matchesSearch;
    });

    if (loading) {
        return (
            <div className="flex items-center justify-center h-full">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
            </div>
        );
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <h1 className="text-2xl font-bold text-gray-900">Orders</h1>

                <div className="flex gap-2">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input
                            type="text"
                            placeholder="Search orders..."
                            className="input input-sm pl-9 border border-gray-300 rounded-lg"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    <select
                        className="select select-bordered select-sm"
                        value={filterStatus}
                        onChange={(e) => setFilterStatus(e.target.value)}
                    >
                        <option value="all">All Statuses</option>
                        <option value="pending_payment">Pending Payment</option>
                        <option value="paid">Paid</option>
                        <option value="processing">Processing</option>
                        <option value="shipped">Shipped</option>
                        <option value="delivered">Delivered</option>
                        <option value="cancelled">Cancelled</option>
                        <option value="refunded">Refunded</option>
                    </select>
                </div>
            </div>

            <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm">
                        <thead className="bg-gray-50 text-gray-500 font-medium border-b border-gray-100">
                            <tr>
                                <th className="p-4">Order ID</th>
                                <th className="p-4">Date</th>
                                <th className="p-4">Customer</th>
                                <th className="p-4">Total</th>
                                <th className="p-4">Status</th>
                                <th className="p-4">Actions</th>
                                <th className="p-4"></th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {filteredOrders.map((order) => (
                                <Fragment key={order.id}>
                                    <tr className="hover:bg-gray-50 transition-colors">
                                        <td className="p-4 font-mono text-xs">{order.id.slice(0, 8)}...</td>
                                        <td className="p-4 text-gray-500">
                                            {order.createdAt?.toDate ? order.createdAt.toDate().toLocaleDateString() : 'N/A'}
                                        </td>
                                        <td className="p-4">
                                            <div className="font-medium text-gray-900">
                                                {order.customer?.firstName || 'Unknown'} {order.customer?.lastName || ''}
                                            </div>
                                            <div className="text-xs text-gray-500">{order.customer?.email || 'N/A'}</div>
                                        </td>
                                        <td className="p-4 font-medium">₹{order.total?.toLocaleString() || '0'}</td>
                                        <td className="p-4">
                                            <span className={`px-2 py-1 rounded-full text-xs font-semibold ${getStatusColor(order.status)}`}>
                                                {order.status ? order.status.replace('_', ' ').toUpperCase() : 'UNKNOWN'}
                                            </span>
                                        </td>
                                        <td className="p-4">
                                            <select
                                                className="text-xs border border-gray-200 rounded p-1"
                                                value={order.status}
                                                onChange={(e) => confirmStatusUpdate(order.id, e.target.value)}
                                            >
                                                <option value="pending_payment">Pending Payment</option>
                                                <option value="paid">Paid</option>
                                                <option value="processing">Processing</option>
                                                <option value="shipped">Shipped</option>
                                                <option value="delivered">Delivered</option>
                                                <option value="cancelled">Cancelled</option>
                                                <option value="refunded">Refunded</option>
                                            </select>
                                        </td>
                                        <td className="p-4 text-right">
                                            <button
                                                onClick={() => setExpandedOrder(expandedOrder === order.id ? null : order.id)}
                                                className="text-gray-400 hover:text-gray-600"
                                            >
                                                {expandedOrder === order.id ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                            </button>
                                        </td>
                                    </tr>

                                    {expandedOrder === order.id && (
                                        <tr className="expanded-row">
                                            <td colSpan="7" style={{ padding: 0 }}>
                                                <div className="expanded-container">

                                                    <div className="expanded-header">
                                                        <div>
                                                            <div className="expanded-title">Order #{order.id}</div>
                                                            <div className="expanded-subtitle">Placed on {order.createdAt?.toDate ? order.createdAt.toDate().toLocaleString() : 'N/A'}</div>
                                                        </div>
                                                        <button onClick={() => handlePrintInvoice(order.id)} className="btn btn-outline">
                                                            <Printer size={16} /> Print Slip
                                                        </button>
                                                    </div>

                                                    <div className="expanded-grid">

                                                        {/* Left Column: Items & Timeline */}
                                                        <div className="expanded-col-main">
                                                            <div className="panel-card">
                                                                <div className="panel-title">
                                                                    <Package size={16} /> Products
                                                                </div>

                                                                <div>
                                                                    {order.items?.map((item, idx) => (
                                                                        <div key={idx} className="item-row">
                                                                            <img src={item.baseProduct?.image || ''} alt="" className="item-image" />
                                                                            <div className="item-details">
                                                                                <div className="item-name">{item.baseProduct?.name || 'Unknown Product'}</div>
                                                                                <div className="item-meta">Variant: {item.variant?.name || 'Default'}</div>
                                                                            </div>
                                                                            <div className="item-price">
                                                                                <div className="item-price-val">₹{(item.totalPrice * (item.quantity || 1))?.toLocaleString() || '0'}</div>
                                                                                <div className="item-meta">Qty: {item.quantity || 1}</div>
                                                                            </div>
                                                                        </div>
                                                                    ))}
                                                                </div>

                                                                <div style={{ marginTop: '1.5rem', paddingTop: '1rem', borderTop: '1px solid var(--border)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                                                    <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-muted)', letterSpacing: '0.05em' }}>TOTAL AMOUNT</span>
                                                                    <span style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-main)' }}>₹{order.total?.toLocaleString() || '0'}</span>
                                                                </div>
                                                            </div>
                                                        </div>

                                                        {/* Right Column: Fulfillment & Logistics */}
                                                        <div className="expanded-col-side">

                                                            <div className="panel-card">
                                                                <div className="panel-title">
                                                                    Customer Profile
                                                                </div>
                                                                <div className="info-text">
                                                                    <strong>{order.customer?.firstName} {order.customer?.lastName}</strong>
                                                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', marginBottom: '1rem', fontSize: '0.8rem' }}>
                                                                        <span>✉ {order.customer?.email || 'N/A'}</span>
                                                                        <span>📞 {order.customer?.phone || 'N/A'}</span>
                                                                    </div>
                                                                    <div style={{ paddingTop: '0.75rem', borderTop: '1px solid var(--border)', fontSize: '0.8rem' }}>
                                                                        <div>{order.customer?.address}</div>
                                                                        <div>{order.customer?.city}, {order.customer?.state} {order.customer?.zip}</div>
                                                                        <div style={{ textTransform: 'uppercase', fontSize: '0.7rem', fontWeight: 600, marginTop: '4px' }}>{order.customer?.country}</div>
                                                                    </div>
                                                                </div>
                                                            </div>

                                                            <div className="panel-card">
                                                                <div className="panel-title">
                                                                    <Truck size={16} /> Logistics
                                                                </div>
                                                                {order.status === 'shipped' || order.status === 'delivered' ? (
                                                                    order.tracking?.number ? (
                                                                        <div>
                                                                            <div className="tracking-box">
                                                                                <div className="tracking-provider">{order.tracking.provider}</div>
                                                                                <div className="tracking-number">{order.tracking.number}</div>
                                                                            </div>
                                                                            <button onClick={() => setTrackingInput(order.tracking)} style={{ backgroundColor: 'transparent', border: 'none', color: 'var(--primary)', fontSize: '0.75rem', fontWeight: 600, cursor: 'pointer', padding: 0 }}>Update Dispatch Details</button>
                                                                        </div>
                                                                    ) : (
                                                                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                                                                            <input type="text" placeholder="Courier Provider" className="input" value={trackingInput.provider} onChange={(e) => setTrackingInput({ ...trackingInput, provider: e.target.value })} />
                                                                            <input type="text" placeholder="Tracking Number" className="input" style={{ fontFamily: 'monospace' }} value={trackingInput.number} onChange={(e) => setTrackingInput({ ...trackingInput, number: e.target.value })} />
                                                                            <button onClick={() => handleSaveTracking(order.id)} className="btn btn-primary" style={{ width: '100%' }}>Assign Tracking</button>
                                                                        </div>
                                                                    )
                                                                ) : (
                                                                    <div style={{ padding: '1rem', backgroundColor: '#fffbeb', border: '1px solid #fde68a', borderRadius: '6px', color: '#92400e', fontSize: '0.875rem' }}>
                                                                        Tracking assignment unlocks when order is escalated to <b>Shipped</b>.
                                                                    </div>
                                                                )}
                                                            </div>

                                                            <div className="panel-card" style={{ padding: 0, display: 'flex', flexDirection: 'column', maxHeight: '320px' }}>
                                                                <div className="panel-title" style={{ margin: 0, padding: '1rem', borderBottom: '1px solid var(--border)', backgroundColor: 'var(--bg-hover)', borderTopLeftRadius: '8px', borderTopRightRadius: '8px' }}>
                                                                    <MessageSquare size={16} /> Admin Thread
                                                                </div>
                                                                <div style={{ flex: 1, overflowY: 'auto', padding: '1rem', backgroundColor: '#fafafa' }}>
                                                                    {order.notes?.length > 0 ? order.notes.map((note, idx) => (
                                                                        <div key={idx} className="note-bubble">
                                                                            <div>{note.text}</div>
                                                                            <div className="note-bubble-meta">{note.author} • {new Date(note.timestamp).toLocaleDateString()}</div>
                                                                        </div>
                                                                    )) : (
                                                                        <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                                                                            No internal logs.
                                                                        </div>
                                                                    )}
                                                                </div>
                                                                <div style={{ padding: '0.75rem', borderTop: '1px solid var(--border)', backgroundColor: 'var(--bg-panel)', borderBottomLeftRadius: '8px', borderBottomRightRadius: '8px' }}>
                                                                    <input
                                                                        type="text"
                                                                        placeholder="Drop a private note..."
                                                                        className="input"
                                                                        value={noteInput}
                                                                        onChange={(e) => setNoteInput(e.target.value)}
                                                                        onKeyDown={(e) => {
                                                                            if (e.key === 'Enter') {
                                                                                e.preventDefault();
                                                                                handleAddNote(order.id);
                                                                            }
                                                                        }}
                                                                    />
                                                                </div>
                                                            </div>

                                                        </div>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    )}
                                </Fragment>
                            ))}

                            {filteredOrders.length === 0 && (
                                <tr>
                                    <td colSpan="7" className="p-8 text-center text-gray-500">
                                        No orders found.
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Confirmation Modal */}
            {confirmModal.isOpen && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
                    <div className="bg-white rounded-xl shadow-xl max-w-sm w-full p-6 text-center">
                        <h3 className="text-xl font-bold text-gray-900 mb-2">Confirm Action</h3>
                        <p className="text-gray-600 mb-6">
                            Are you sure you want to mark this order as <span className="font-bold text-red-600">{confirmModal.newStatus.toUpperCase()}</span>?
                            This action may have financial implications and notify the customer.
                        </p>
                        <div className="flex gap-3 justify-center">
                            <button
                                onClick={() => setConfirmModal({ isOpen: false, orderId: null, newStatus: null })}
                                className="px-4 py-2 text-gray-600 font-medium hover:bg-gray-100 rounded-lg transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={() => handleStatusUpdate(confirmModal.orderId, confirmModal.newStatus)}
                                className="px-4 py-2 bg-red-600 text-white font-medium hover:bg-red-700 rounded-lg transition-colors"
                            >
                                Yes, Confirm
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

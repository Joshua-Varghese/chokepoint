import { useState, useEffect } from 'react';
import { collection, onSnapshot, query, orderBy, doc, updateDoc } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Package, ChevronDown, ChevronUp, Search, Filter } from 'lucide-react';
import toast from 'react-hot-toast';

export default function Orders() {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [expandedOrder, setExpandedOrder] = useState(null);
    const [filterStatus, setFilterStatus] = useState('all');

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

    const handleStatusUpdate = async (orderId, newStatus) => {
        try {
            await updateDoc(doc(db, 'orders', orderId), { status: newStatus });
            toast.success(`Order status updated to ${newStatus}`);
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
            default: return 'bg-gray-100 text-gray-800';
        }
    };

    const filteredOrders = filterStatus === 'all'
        ? orders
        : orders.filter(order => order.status === filterStatus);

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
                                <>
                                    <tr key={order.id} className="hover:bg-gray-50 transition-colors">
                                        <td className="p-4 font-mono text-xs">{order.id.slice(0, 8)}...</td>
                                        <td className="p-4 text-gray-500">
                                            {order.createdAt?.toDate().toLocaleDateString()}
                                        </td>
                                        <td className="p-4">
                                            <div className="font-medium text-gray-900">
                                                {order.customer.firstName} {order.customer.lastName}
                                            </div>
                                            <div className="text-xs text-gray-500">{order.customer.email}</div>
                                        </td>
                                        <td className="p-4 font-medium">₹{order.total.toLocaleString()}</td>
                                        <td className="p-4">
                                            <span className={`px-2 py-1 rounded-full text-xs font-semibold ${getStatusColor(order.status)}`}>
                                                {order.status.replace('_', ' ').toUpperCase()}
                                            </span>
                                        </td>
                                        <td className="p-4">
                                            <select
                                                className="text-xs border border-gray-200 rounded p-1"
                                                value={order.status}
                                                onChange={(e) => handleStatusUpdate(order.id, e.target.value)}
                                            >
                                                <option value="pending_payment">Pending Payment</option>
                                                <option value="paid">Paid</option>
                                                <option value="processing">Processing</option>
                                                <option value="shipped">Shipped</option>
                                                <option value="delivered">Delivered</option>
                                                <option value="cancelled">Cancelled</option>
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
                                        <tr className="bg-gray-50">
                                            <td colSpan="7" className="p-4">
                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                                    {/* Order Items */}
                                                    <div>
                                                        <h4 className="font-semibold text-gray-900 mb-2 flex items-center gap-2">
                                                            <Package size={16} /> Items
                                                        </h4>
                                                        <div className="space-y-2">
                                                            {order.items.map((item, idx) => (
                                                                <div key={idx} className="flex gap-3 text-sm bg-white p-2 rounded border border-gray-100">
                                                                    <div className="w-10 h-10 bg-gray-100 rounded overflow-hidden">
                                                                        <img src={item.baseProduct.image} alt="" className="w-full h-full object-cover" />
                                                                    </div>
                                                                    <div>
                                                                        <div className="font-medium">{item.baseProduct.name}</div>
                                                                        <div className="text-gray-500 text-xs">
                                                                            {item.variant.name} • {item.modules.length} Modules
                                                                        </div>
                                                                    </div>
                                                                    <div className="ml-auto font-medium">
                                                                        ₹{item.totalPrice.toLocaleString()}
                                                                    </div>
                                                                </div>
                                                            ))}
                                                        </div>
                                                    </div>

                                                    {/* Shipping Details */}
                                                    <div className="text-sm">
                                                        <h4 className="font-semibold text-gray-900 mb-2">Shipping Address</h4>
                                                        <div className="bg-white p-3 rounded border border-gray-100 text-gray-600 space-y-1">
                                                            <p>{order.customer.address}</p>
                                                            <p>{order.customer.city}, {order.customer.state}</p>
                                                            <p>{order.customer.country} - {order.customer.zip}</p>
                                                            <div className="mt-2 pt-2 border-t border-gray-100">
                                                                <p>📞 {order.customer.phone}</p>
                                                                <p>✉️ {order.customer.email}</p>
                                                            </div>
                                                            {order.paymentId && (
                                                                <div className="mt-2 pt-2 border-t border-gray-100 text-xs text-gray-400">
                                                                    Payment ID: <span className="font-mono">{order.paymentId}</span>
                                                                </div>
                                                            )}
                                                        </div>
                                                    </div>
                                                </div>
                                            </td>
                                        </tr>
                                    )}
                                </>
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
        </div>
    );
}

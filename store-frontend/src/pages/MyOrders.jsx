import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { collection, query, where, getDocs, orderBy } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Package, Clock, CheckCircle, Truck, XCircle, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function MyOrders() {
    const { currentUser } = useAuth();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchOrders = async () => {
            if (!currentUser) {
                setLoading(false);
                return;
            }
            try {
                // Fetch orders linked to this user's Firebase UID
                const q = query(
                    collection(db, 'orders'),
                    where('customer.userId', '==', currentUser.uid),
                    orderBy('createdAt', 'desc')
                );
                const querySnapshot = await getDocs(q);
                const fetchedOrders = querySnapshot.docs.map(doc => ({
                    id: doc.id,
                    ...doc.data()
                }));
                setOrders(fetchedOrders);
            } catch (error) {
                console.error("Error fetching orders:", error);
                // Creating an index might be required for where + orderBy.
                // If it fails due to missing index, Firebase will output a link in the console.
            } finally {
                setLoading(false);
            }
        };

        fetchOrders();
    }, [currentUser]);

    const getStatusInfo = (status) => {
        switch (status) {
            case 'pending_payment': return { icon: <Clock className="text-yellow-500" />, label: 'Payment Pending', bg: 'bg-yellow-50' };
            case 'paid': return { icon: <CheckCircle className="text-green-500" />, label: 'Order Confirmed', bg: 'bg-green-50' };
            case 'processing': return { icon: <RefreshCw className="text-blue-500" />, label: 'Processing', bg: 'bg-blue-50' };
            case 'shipped': return { icon: <Truck className="text-purple-500" />, label: 'Shipped', bg: 'bg-purple-50' };
            case 'delivered': return { icon: <CheckCircle className="text-teal-500" />, label: 'Delivered', bg: 'bg-teal-50' };
            case 'cancelled': return { icon: <XCircle className="text-red-500" />, label: 'Cancelled', bg: 'bg-red-50' };
            case 'refunded': return { icon: <RefreshCw className="text-orange-500" />, label: 'Refunded', bg: 'bg-orange-50' };
            default: return { icon: <Package className="text-gray-500" />, label: 'Unknown', bg: 'bg-gray-50' };
        }
    };

    if (!currentUser) {
        return (
            <div className="min-h-[60vh] flex flex-col items-center justify-center p-4">
                <Package size={48} className="text-gray-300 mb-4" />
                <h2 className="text-2xl font-bold mb-2">Track Your Orders</h2>
                <p className="text-gray-500 mb-6">Please log in to view your order history.</p>
            </div>
        );
    }

    if (loading) {
        return (
            <div className="min-h-[60vh] flex items-center justify-center">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-black"></div>
            </div>
        );
    }

    return (
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
            <h1 className="text-3xl font-bold mb-8">My Orders</h1>

            {orders.length === 0 ? (
                <div className="text-center py-12 bg-white rounded-xl shadow-sm border border-gray-100">
                    <Package size={48} className="text-gray-300 mx-auto mb-4" />
                    <h2 className="text-xl font-bold mb-2">No orders yet</h2>
                    <p className="text-gray-500 mb-6">When you place an order, it will appear here.</p>
                    <Link to="/" className="inline-block bg-black text-white px-6 py-2 rounded-lg font-medium hover:bg-gray-900 transition-colors">
                        Start Shopping
                    </Link>
                </div>
            ) : (
                <div className="space-y-6">
                    {orders.map(order => {
                        const statusInfo = getStatusInfo(order.status);
                        return (
                            <div key={order.id} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                                <div className={`p-4 ${statusInfo.bg} border-b border-gray-100 flex flex-wrap items-center justify-between gap-4`}>
                                    <div className="flex items-center gap-3">
                                        {statusInfo.icon}
                                        <div>
                                            <p className="font-bold text-gray-900">{statusInfo.label}</p>
                                            <p className="text-xs text-gray-500 font-mono">Order #{order.id}</p>
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <p className="font-bold">₹{order.total?.toLocaleString()}</p>
                                        <p className="text-xs text-gray-500">
                                            {order.createdAt?.toDate ? order.createdAt.toDate().toLocaleDateString() : 'Date Unknown'}
                                        </p>
                                    </div>
                                </div>
                                <div className="p-4 sm:p-6 divide-y divide-gray-50">
                                    {order.items?.map((item, idx) => (
                                        <div key={idx} className="py-4 first:pt-0 last:pb-0 flex items-center gap-4">
                                            <div className="w-16 h-16 bg-gray-50 rounded-lg overflow-hidden shrink-0 border border-gray-100">
                                                <img src={item.baseProduct?.image || ''} alt={item.baseProduct?.name} className="w-full h-full object-cover" />
                                            </div>
                                            <div className="flex-1 min-w-0">
                                                <h4 className="font-bold text-gray-900 truncate">{item.baseProduct?.name}</h4>
                                                <p className="text-sm text-gray-500 truncate">{item.variant?.name}</p>
                                            </div>
                                            <div className="text-sm font-medium whitespace-nowrap">
                                                Qty: {item.quantity || 1}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

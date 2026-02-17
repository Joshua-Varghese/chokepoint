import { useNavigate } from 'react-router-dom';
import { Trash2, ShoppingBag, ArrowRight } from 'lucide-react';
import { useCart } from '../context/CartContext';

export default function Cart() {
    const { cart, removeFromCart, cartTotal } = useCart();
    const navigate = useNavigate();

    if (cart.length === 0) {
        return (
            <div className="min-h-screen bg-white flex flex-col items-center justify-center p-4">
                <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                    <ShoppingBag size={24} className="text-gray-400" />
                </div>
                <h1 className="text-2xl font-bold mb-2">Your cart is empty</h1>
                <p className="text-gray-500 mb-6">Looks like you haven't added any systems yet.</p>
                <button
                    onClick={() => navigate('/')}
                    className="bg-black text-white px-8 py-3 rounded-lg font-bold hover:bg-gray-900 transition-colors"
                >
                    Start Configuring
                </button>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 py-12">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <h1 className="text-3xl font-bold mb-8">Shopping Cart</h1>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                    {/* Cart Items */}
                    <div className="lg:col-span-8 flex flex-col gap-4">
                        {cart.map((item, index) => (
                            <div key={item.id} className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm flex flex-col sm:flex-row gap-6">
                                {/* Image */}
                                <div className="w-full sm:w-32 h-32 bg-gray-100 rounded-lg flex items-center justify-center p-4">
                                    {item.baseProduct.image ? (
                                        <img src={item.baseProduct.image} alt={item.baseProduct.name} className="w-full h-full object-contain mix-blend-multiply" />
                                    ) : (
                                        <ShoppingBag className="text-gray-400" />
                                    )}
                                </div>

                                {/* Details */}
                                <div className="flex-1">
                                    <div className="flex justify-between items-start">
                                        <div>
                                            <h2 className="text-xl font-bold text-gray-900">{item.baseProduct.name}</h2>
                                            <p className="text-sm text-gray-500 mb-2">Custom Configuration</p>
                                        </div>
                                        <p className="text-lg font-bold">₹{item.totalPrice.toLocaleString()}</p>
                                    </div>

                                    {/* Breakdown */}
                                    <div className="mt-4 space-y-1">
                                        <div className="text-sm text-gray-600 flex justify-between">
                                            <span>Platform: <span className="font-medium text-gray-900">{item.variant?.name || 'Standard'}</span></span>
                                        </div>
                                        {item.modules.length > 0 && (
                                            <div className="text-sm text-gray-600">
                                                <span className="block mb-1">Modules:</span>
                                                <div className="flex flex-wrap gap-1">
                                                    {item.modules.map(mod => (
                                                        <span key={mod.id} className="inline-block bg-gray-100 px-2 py-0.5 rounded text-xs text-gray-700 border border-gray-200">
                                                            {mod.name}
                                                        </span>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    <div className="mt-4 pt-4 border-t border-gray-100 flex justify-end">
                                        <button
                                            onClick={() => removeFromCart(index)}
                                            className="text-red-500 text-sm font-medium flex items-center gap-1 hover:text-red-600"
                                        >
                                            <Trash2 size={16} /> Remove
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Summary */}
                    <div className="lg:col-span-4">
                        <div className="bg-white p-6 rounded-xl border border-gray-200 shadow-sm sticky top-24">
                            <h2 className="text-lg font-bold mb-4">Order Summary</h2>
                            <div className="flex justify-between items-center mb-2">
                                <span className="text-gray-600">Subtotal</span>
                                <span className="font-medium">₹{cartTotal.toLocaleString()}</span>
                            </div>
                            <div className="flex justify-between items-center mb-4">
                                <span className="text-gray-600">Shipping</span>
                                <span className="text-green-600 font-medium">Free</span>
                            </div>
                            <div className="border-t border-gray-100 pt-4 mb-6 flex justify-between items-center">
                                <span className="text-lg font-bold">Total</span>
                                <span className="text-2xl font-bold">₹{cartTotal.toLocaleString()}</span>
                            </div>
                            <button
                                onClick={() => navigate('/checkout')}
                                className="w-full bg-blue-600 text-white py-4 rounded-lg font-bold hover:bg-blue-700 transition-colors flex items-center justify-center gap-2"
                            >
                                Proceed to Checkout <ArrowRight size={20} />
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

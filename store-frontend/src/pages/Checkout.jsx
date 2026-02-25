import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { collection, addDoc, updateDoc, doc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase-config';
import { CheckCircle, ArrowLeft } from 'lucide-react';
import toast from 'react-hot-toast';
import { useRazorpay } from "react-razorpay";
import { Country, State, City } from 'country-state-city';

export default function Checkout() {
    const { cart, cartTotal, clearCart } = useCart();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [orderId, setOrderId] = useState(null);
    const { currentUser } = useAuth();
    const { Razorpay } = useRazorpay();

    const [formData, setFormData] = useState({
        firstName: '', lastName: '', email: '', phone: '',
        address: '', country: 'IN', state: '', city: '', zip: ''
    });

    useEffect(() => {
        if (currentUser) {
            const nameParts = (currentUser.displayName || '').split(' ');
            setFormData(prev => ({
                ...prev,
                email: prev.email || currentUser.email || '',
                firstName: prev.firstName || nameParts[0] || '',
                lastName: prev.lastName || (nameParts.length > 1 ? nameParts.slice(1).join(' ') : '')
            }));
        }
    }, [currentUser]);

    if (cart.length === 0 && !success) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <p>Your cart is empty. <button onClick={() => navigate('/')} className="text-blue-600 underline">Go shopping</button></p>
            </div>
        );
    }

    const [errors, setErrors] = useState({});

    // Reset dependent fields when country changes
    useEffect(() => {
        setFormData(prev => ({ ...prev, state: '', city: '' }));
    }, [formData.country]);

    // Reset city when state changes
    useEffect(() => {
        setFormData(prev => ({ ...prev, city: '' }));
    }, [formData.state]);

    const validateForm = () => {
        const newErrors = {};
        if (!formData.firstName.trim()) newErrors.firstName = "First name is required";
        if (!formData.lastName.trim()) newErrors.lastName = "Last name is required";
        if (!formData.email.trim()) {
            newErrors.email = "Email is required";
        } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
            newErrors.email = "Email is invalid";
        }
        if (!formData.phone.trim()) {
            newErrors.phone = "Phone is required";
        } else if (formData.phone.length < 10) {
            newErrors.phone = "Phone number must be at least 10 digits";
        }
        if (!formData.address.trim()) newErrors.address = "Address is required";
        if (!formData.country) newErrors.country = "Country is required";
        if (!formData.state) newErrors.state = "State is required";
        if (!formData.city) newErrors.city = "City is required";
        if (!formData.zip.trim()) {
            newErrors.zip = "ZIP code is required";
        } else if (!/^\d{6}$/.test(formData.zip)) { // Assuming Indian PIN code format (6 digits) as default since country is IN
            newErrors.zip = "Invalid ZIP/PIN code (6 digits required)";
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handlePayment = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            toast.error("Please fix the errors in the form");
            return;
        }

        setLoading(true);

        try {
            // 1. Create Order in Firestore (Pending)
            const orderData = {
                items: cart,
                total: cartTotal,
                customer: {
                    ...formData,
                    userId: currentUser ? currentUser.uid : null,
                    country: Country.getCountryByCode(formData.country)?.name || formData.country,
                    state: State.getStateByCodeAndCountry(formData.state, formData.country)?.name || formData.state
                },
                status: 'pending_payment',
                createdAt: serverTimestamp()
            };

            const docRef = await addDoc(collection(db, 'orders'), orderData);
            const currentOrderId = docRef.id;

            // 2. Options for Razorpay
            const options = {
                key: import.meta.env.VITE_RAZORPAY_KEY_ID, // Use Environment Variable
                amount: cartTotal * 100, // Amount is in currency subunits. Default currency is INR.
                currency: "INR",
                name: "Chokepoint Store",
                description: "Order #" + currentOrderId,
                image: "https://via.placeholder.com/150",
                order_id: "", // Generate order_id on server for better security in production
                handler: async function (response) {
                    // 3. On Payment Success
                    // alert(response.razorpay_payment_id);
                    // alert(response.razorpay_order_id);
                    // alert(response.razorpay_signature);

                    // Update Firestore status
                    await updateDoc(doc(db, 'orders', currentOrderId), {
                        status: 'paid',
                        paymentId: response.razorpay_payment_id,
                        paymentSignature: response.razorpay_signature
                    });

                    setOrderId(currentOrderId);
                    setSuccess(true);
                    clearCart();
                    toast.success("Payment Successful!");
                },
                prefill: {
                    name: `${formData.firstName} ${formData.lastName}`,
                    email: formData.email,
                    contact: formData.phone // Ensure phone is captured
                },
                notes: {
                    address: formData.address
                },
                theme: {
                    color: "#000000"
                }
            };

            const rzp1 = new Razorpay(options);

            rzp1.on("payment.failed", function (response) {
                toast.error("Payment Failed: " + response.error.description);
                // Optionally update order status to 'failed'
            });

            rzp1.open();

        } catch (error) {
            console.error("Error initiating payment detailed:", error);
            console.log("Razorpay Key:", import.meta.env.VITE_RAZORPAY_KEY_ID);
            console.log("Razorpay Object:", Razorpay);
            toast.error("Failed to initiate payment: " + (error.message || "Unknown Error"));
        } finally {
            setLoading(false);
        }
    };

    if (success) {
        return (
            <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
                <div className="bg-white p-8 rounded-xl shadow-lg max-w-md w-full text-center">
                    <div className="w-16 h-16 bg-green-100 text-green-600 rounded-full flex items-center justify-center mx-auto mb-4">
                        <CheckCircle size={32} />
                    </div>
                    <h1 className="text-2xl font-bold mb-2">Order Confirmed!</h1>
                    <p className="text-gray-600 mb-6">
                        Thank you for your order. We'll send a confirmation email to <b>{formData.email}</b> shortly.
                    </p>
                    <div className="bg-gray-50 p-4 rounded-lg mb-6 text-sm text-gray-500">
                        Order ID: <span className="font-mono text-gray-900">{orderId}</span>
                    </div>
                    <button
                        onClick={() => navigate('/')}
                        className="w-full bg-black text-white py-3 rounded-lg font-bold hover:bg-gray-900 transition-colors"
                    >
                        Return to Store
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 py-12">
            <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8">
                <button onClick={() => navigate('/cart')} className="flex items-center gap-2 text-gray-600 mb-6 hover:text-black transition-colors">
                    <ArrowLeft size={16} /> Back to Cart
                </button>

                <h1 className="text-3xl font-bold mb-8">Checkout</h1>

                <div className="bg-white rounded-xl shadow-sm overflow-hidden">
                    <div className="p-6 md:p-8 border-b border-gray-100">
                        <h2 className="text-xl font-bold mb-4">Contact & Shipping</h2>
                        <form id="checkout-form" onSubmit={handlePayment} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                                <input
                                    className={`input w-full p-2 border rounded ${errors.firstName ? 'border-red-500' : ''}`}
                                    value={formData.firstName}
                                    onChange={e => {
                                        setFormData({ ...formData, firstName: e.target.value });
                                        if (errors.firstName) setErrors({ ...errors, firstName: null });
                                    }}
                                />
                                {errors.firstName && <p className="text-red-500 text-xs mt-1">{errors.firstName}</p>}
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                                <input
                                    className={`input w-full p-2 border rounded ${errors.lastName ? 'border-red-500' : ''}`}
                                    value={formData.lastName}
                                    onChange={e => {
                                        setFormData({ ...formData, lastName: e.target.value });
                                        if (errors.lastName) setErrors({ ...errors, lastName: null });
                                    }}
                                />
                                {errors.lastName && <p className="text-red-500 text-xs mt-1">{errors.lastName}</p>}
                            </div>
                            <div className="md:col-span-2">
                                <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                                <input
                                    type="email"
                                    className={`input w-full p-2 border rounded ${errors.email ? 'border-red-500' : ''}`}
                                    value={formData.email}
                                    onChange={e => {
                                        setFormData({ ...formData, email: e.target.value });
                                        if (errors.email) setErrors({ ...errors, email: null });
                                    }}
                                />
                                {errors.email && <p className="text-red-500 text-xs mt-1">{errors.email}</p>}
                            </div>
                            <div className="md:col-span-2">
                                <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
                                <input
                                    type="tel"
                                    className={`input w-full p-2 border rounded ${errors.phone ? 'border-red-500' : ''}`}
                                    value={formData.phone}
                                    onChange={e => {
                                        setFormData({ ...formData, phone: e.target.value });
                                        if (errors.phone) setErrors({ ...errors, phone: null });
                                    }}
                                />
                                {errors.phone && <p className="text-red-500 text-xs mt-1">{errors.phone}</p>}
                            </div>
                            <div className="md:col-span-2">
                                <label className="block text-sm font-medium text-gray-700 mb-1">Address</label>
                                <input
                                    className={`input w-full p-2 border rounded ${errors.address ? 'border-red-500' : ''}`}
                                    value={formData.address}
                                    onChange={e => {
                                        setFormData({ ...formData, address: e.target.value });
                                        if (errors.address) setErrors({ ...errors, address: null });
                                    }}
                                />
                                {errors.address && <p className="text-red-500 text-xs mt-1">{errors.address}</p>}
                            </div>

                            {/* Country Dropdown */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Country</label>
                                <select
                                    className={`input w-full p-2 border rounded ${errors.country ? 'border-red-500' : ''}`}
                                    value={formData.country}
                                    onChange={e => {
                                        setFormData({ ...formData, country: e.target.value });
                                        if (errors.country) setErrors({ ...errors, country: null });
                                    }}
                                >
                                    <option value="">Select Country</option>
                                    {Country.getAllCountries().map(country => (
                                        <option key={country.isoCode} value={country.isoCode}>{country.name}</option>
                                    ))}
                                </select>
                                {errors.country && <p className="text-red-500 text-xs mt-1">{errors.country}</p>}
                            </div>

                            {/* State Dropdown */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">State</label>
                                <select
                                    className={`input w-full p-2 border rounded ${errors.state ? 'border-red-500' : ''}`}
                                    value={formData.state}
                                    disabled={!formData.country}
                                    onChange={e => {
                                        setFormData({ ...formData, state: e.target.value });
                                        if (errors.state) setErrors({ ...errors, state: null });
                                    }}
                                >
                                    <option value="">Select State</option>
                                    {State.getStatesOfCountry(formData.country).map(state => (
                                        <option key={state.isoCode} value={state.isoCode}>{state.name}</option>
                                    ))}
                                </select>
                                {errors.state && <p className="text-red-500 text-xs mt-1">{errors.state}</p>}
                            </div>

                            {/* City Dropdown */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                                <select
                                    className={`input w-full p-2 border rounded ${errors.city ? 'border-red-500' : ''}`}
                                    value={formData.city}
                                    disabled={!formData.state}
                                    onChange={e => {
                                        setFormData({ ...formData, city: e.target.value });
                                        if (errors.city) setErrors({ ...errors, city: null });
                                    }}
                                >
                                    <option value="">Select City</option>
                                    {City.getCitiesOfState(formData.country, formData.state).map(city => (
                                        <option key={city.name} value={city.name}>{city.name}</option>
                                    ))}
                                </select>
                                {errors.city && <p className="text-red-500 text-xs mt-1">{errors.city}</p>}
                            </div>


                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">ZIP</label>
                                <input
                                    className={`input w-full p-2 border rounded ${errors.zip ? 'border-red-500' : ''}`}
                                    value={formData.zip}
                                    onChange={e => {
                                        setFormData({ ...formData, zip: e.target.value });
                                        if (errors.zip) setErrors({ ...errors, zip: null });
                                    }}
                                />
                                {errors.zip && <p className="text-red-500 text-xs mt-1">{errors.zip}</p>}
                            </div>

                        </form>
                    </div>

                    <div className="p-6 md:p-8 bg-gray-50 flex flex-col md:flex-row justify-between items-center gap-6">
                        <div className="text-center md:text-left">
                            <p className="text-sm text-gray-500 mb-1">Total to Pay</p>
                            <p className="text-3xl font-bold">₹{cartTotal.toLocaleString()}</p>
                        </div>
                        <button
                            type="submit"
                            form="checkout-form"
                            disabled={loading}
                            className="bg-black text-white px-8 py-3 rounded-lg font-bold hover:bg-gray-900 transition-colors disabled:opacity-50 min-w-[200px]"
                        >
                            {loading ? 'Processing...' : 'Pay Now'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

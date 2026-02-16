import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { doc, getDoc, collection, getDocs, query, where } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Check, Plus, AlertCircle, ShoppingCart } from 'lucide-react';

export default function Configurator() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [baseProduct, setBaseProduct] = useState(null);
    const [modules, setModules] = useState([]);
    const [loading, setLoading] = useState(true);

    // Selection State
    const [selectedVariant, setSelectedVariant] = useState(null);
    const [selectedModules, setSelectedModules] = useState(new Set()); // Set of IDs

    useEffect(() => {
        async function fetchData() {
            try {
                // 1. Fetch Base Product
                const docRef = doc(db, 'products', id);
                const docSnap = await getDoc(docRef);

                if (docSnap.exists()) {
                    const data = docSnap.data();
                    setBaseProduct({ id: docSnap.id, ...data });
                    // Default to first variant
                    if (data.variants && data.variants.length > 0) {
                        setSelectedVariant(data.variants[0]);
                    }
                } else {
                    navigate('/'); // Invalid ID
                    return;
                }

                // 2. Fetch Compatible Modules (Sensors & Accessories)
                // In a real app, we'd filter based on compatibility tags.
                // For now, fetch all non-base items.
                const q = query(collection(db, 'products'), where('type', '!=', 'base'));
                const modulesSnap = await getDocs(q);
                setModules(modulesSnap.docs.map(d => ({ id: d.id, ...d.data() })));

            } catch (e) {
                console.error(e);
            } finally {
                setLoading(false);
            }
        }
        fetchData();
    }, [id, navigate]);

    const toggleModule = (moduleId) => {
        const next = new Set(selectedModules);
        if (next.has(moduleId)) {
            next.delete(moduleId);
        } else {
            next.add(moduleId);
        }
        setSelectedModules(next);
    };

    // Calculate Total Price
    const calculateTotal = () => {
        if (!baseProduct) return 0;
        let total = baseProduct.basePrice;
        if (selectedVariant) total += (selectedVariant.priceMod || 0);

        modules.forEach(m => {
            if (selectedModules.has(m.id)) {
                total += m.price;
            }
        });
        return total;
    };

    if (loading) return <div className="min-h-screen flex items-center justify-center">Loading Configurator...</div>;
    if (!baseProduct) return <div>Product not found</div>;

    const totalPrice = calculateTotal();

    const Section = ({ title, isOpen, onToggle, children, required }) => (
        <div className="border-b border-gray-200 py-6">
            <button
                onClick={onToggle}
                className="w-full flex items-center justify-between text-left group"
            >
                <div>
                    <h3 className="text-lg font-bold text-gray-900 group-hover:text-blue-700 transition-colors flex items-center gap-2">
                        {title}
                        {required && <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full font-normal">Required</span>}
                    </h3>
                </div>
                <div className={`transform transition-transform ${isOpen ? 'rotate-180' : ''}`}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M6 9l6 6 6-6" />
                    </svg>
                </div>
            </button>

            {isOpen && (
                <div className="mt-6 animate-in slide-in-from-top-2 duration-200">
                    {children}
                </div>
            )}
        </div>
    );

    const OptionCard = ({ title, price, description, selected, onClick, type = 'radio', disabled, disabledReason }) => (
        <div
            onClick={!disabled ? onClick : undefined}
            className={`
                relative p-5 rounded-lg border-2 transition-all
                ${disabled
                    ? 'border-gray-100 bg-gray-50 cursor-not-allowed opacity-60'
                    : 'cursor-pointer hover:shadow-md'
                }
                ${selected && !disabled
                    ? 'border-blue-700 bg-blue-50/10 ring-1 ring-blue-700'
                    : (!disabled ? 'border-gray-300 hover:border-gray-400' : '')
                }
            `}
        >
            <div className="flex justify-between items-start mb-2">
                <span className={`font-bold ${selected ? 'text-blue-900' : 'text-gray-900'}`}>{title}</span>
                <span className="text-sm font-medium">
                    {price === 0 ? 'Included' : `+₹${price.toLocaleString()}`}
                </span>
            </div>
            <p className="text-sm text-gray-500 mb-4 line-clamp-2">{description}</p>

            {disabled && (
                <div className="flex items-center gap-2 text-xs font-bold text-red-500 mt-2">
                    <AlertCircle size={14} />
                    {disabledReason}
                </div>
            )}

            {selected && !disabled && (
                <div className="absolute bottom-5 left-5 flex items-center gap-1.5 text-xs font-bold text-blue-800 uppercase tracking-wide">
                    <Check size={14} strokeWidth={3} />
                    {type === 'radio' ? 'Selected' : 'Added'}
                </div>
            )}
        </div>
    );



    return (
        <div className="min-h-screen bg-white">
            <div className="max-w-[1600px] mx-auto">
                <div className="grid grid-cols-1 lg:grid-cols-12 min-h-screen">

                    {/* LEFT: Visual (Fixed on Desktop) */}
                    <div className="lg:col-span-7 bg-gray-50 lg:sticky lg:top-0 lg:h-screen p-8 flex flex-col">
                        <div className="flex-1 flex items-center justify-center p-8 lg:p-16">
                            <div className="relative w-full max-w-2xl aspect-[16/10]">
                                <img
                                    src={baseProduct.imageUrl}
                                    alt={baseProduct.name}
                                    className="w-full h-full object-contain mix-blend-multiply"
                                />
                            </div>
                        </div>

                        {/* Summary Footer on Left Side */}
                        <div className="bg-white rounded-xl p-6 shadow-lg border border-gray-100 hidden lg:block">
                            <div className="flex justify-between items-end mb-4">
                                <div>
                                    <h2 className="text-2xl font-bold">{baseProduct.name}</h2>
                                    <p className="text-gray-500 text-sm mt-1">Custom Configuration</p>
                                </div>
                                <div className="text-right">
                                    <div className="text-sm text-gray-500 mb-1">Total Price</div>
                                    <div className="text-3xl font-bold">₹{totalPrice.toLocaleString()}</div>
                                </div>
                            </div>
                            <button className="w-full bg-blue-700 text-white py-4 rounded-lg font-bold hover:bg-blue-800 transition-colors uppercase tracking-wide text-sm">
                                Add to Cart
                            </button>
                        </div>
                    </div>

                    {/* RIGHT: Scrollable Options */}
                    <div className="lg:col-span-5 bg-white p-6 lg:p-12 overflow-y-auto">

                        <div className="mb-8 border-b border-gray-200 pb-8">
                            <h1 className="text-3xl font-bold mb-2">Configure Your System</h1>
                            <p className="text-gray-500">Customize capability, power, and sensors.</p>
                        </div>

                        {/* 1. Base Variant */}
                        <Section
                            title="Platform Edition"
                            isOpen={true}
                            onToggle={() => { }}
                            required
                        >
                            <div className="grid grid-cols-1 gap-4">
                                {baseProduct.variants?.map(variant => (
                                    <OptionCard
                                        key={variant.id}
                                        title={variant.name}
                                        price={variant.priceMod}
                                        description="Industrial grade enclosure rating with reinforced mounting points."
                                        selected={selectedVariant?.id === variant.id}
                                        onClick={() => setSelectedVariant(variant)}
                                    />
                                ))}
                            </div>
                        </Section>

                        {/* 2. Sensors */}
                        <Section
                            title="Sensors & Modules"
                            isOpen={true}
                            onToggle={() => { }}
                        >
                            <div className="grid grid-cols-1 gap-4">
                                {modules.filter(m => m.type === 'module').map(mod => {
                                    const { valid, reason } = checkCompatibility(mod);
                                    return (
                                        <OptionCard
                                            key={mod.id}
                                            title={mod.name}
                                            price={mod.price}
                                            description={mod.description}
                                            selected={selectedModules.has(mod.id)}
                                            onClick={() => toggleModule(mod.id)}
                                            type="checkbox"
                                            disabled={!valid && !selectedModules.has(mod.id)} // Don't disable if already selected (allows unselecting)
                                            disabledReason={reason}
                                        />
                                    );
                                })}
                            </div>
                        </Section>

                        {/* 3. Power */}
                        <Section
                            title="Power & Accessories"
                            isOpen={true}
                            onToggle={() => { }}
                        >
                            <div className="grid grid-cols-1 gap-4">
                                {modules.filter(m => m.type === 'accessory').map(mod => (
                                    <OptionCard
                                        key={mod.id}
                                        title={mod.name}
                                        price={mod.price}
                                        description={mod.description}
                                        selected={selectedModules.has(mod.id)}
                                        onClick={() => toggleModule(mod.id)}
                                        type="checkbox"
                                    />
                                ))}
                            </div>
                        </Section>

                        {/* Mobile Sticky Footer */}
                        <div className="lg:hidden sticky bottom-0 left-0 right-0 bg-white border-t border-gray-200 p-4 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.1)]">
                            <div className="flex justify-between items-center mb-3">
                                <span className="font-bold">Total</span>
                                <span className="text-xl font-bold">₹{totalPrice.toLocaleString()}</span>
                            </div>
                            <button className="w-full bg-blue-700 text-white py-3 rounded-lg font-bold">
                                Add to Cart
                            </button>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    );
}

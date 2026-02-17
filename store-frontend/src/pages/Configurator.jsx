import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { doc, getDoc, collection, getDocs, query, where } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Check, Plus, AlertCircle, ShoppingCart, ChevronDown } from 'lucide-react';
import { useCart } from '../context/CartContext';

export default function Configurator() {
    const { id } = useParams();
    const navigate = useNavigate();

    const [baseProduct, setBaseProduct] = useState(null);
    const [modules, setModules] = useState([]);
    const [loading, setLoading] = useState(true);

    // Selection State
    const [selectedVariant, setSelectedVariant] = useState(null);
    const [selectedModules, setSelectedModules] = useState(new Set()); // Set of IDs
    const [openSections, setOpenSections] = useState({ variants: true, sensors: true, power: false });

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

                    // Initialize Default Modules
                    if (data.defaultModules && data.defaultModules.length > 0) {
                        setSelectedModules(new Set(data.defaultModules));
                    }

                } else {
                    navigate('/'); // Invalid ID
                    return;
                }

                // 2. Fetch Compatible Modules (Sensors & Accessories)
                // In a real app, we'd filter based on compatibility tags.
                // For now, fetch all non-base items.
                // NOTE: Fetching ALL products and filtering locally to catch items with missing 'type' field
                const q = query(collection(db, 'products'));
                const modulesSnap = await getDocs(q);
                let fetchedModules = modulesSnap.docs
                    .map(d => {
                        const data = d.data();
                        return {
                            id: d.id,
                            ...data,
                            type: data.type || 'module' // DEFAULT TO MODULE IF MISSING
                        };
                    })
                    .filter(m => m.id !== id && m.type !== 'base');

                // Filter by Whitelist if exists
                /*
                if (data.compatibleModules && data.compatibleModules.length > 0) {
                    fetchedModules = fetchedModules.filter(m => data.compatibleModules.includes(m.id));
                }
                */

                setModules(fetchedModules);

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
        let total = baseProduct.basePrice || baseProduct.price; // Fallback to main price if basePrice missing
        if (selectedVariant) total += (selectedVariant.priceMod || 0);

        modules.forEach(m => {
            if (selectedModules.has(m.id)) {
                total += m.price;
            }
        });
        return total;
    };

    const checkCompatibility = (mod) => {
        // 1. Check if incompatible with any SELECTED modules
        if (mod.constraints?.incompatible_with) {
            for (const badId of mod.constraints.incompatible_with) {
                if (selectedModules.has(badId)) {
                    const badItem = modules.find(m => m.id === badId);
                    return { valid: false, reason: `Incompatible with ${badItem?.name || 'selection'}` };
                }
            }
        }

        // 2. Check if any SELECTED module is incompatible with THIS module
        for (const selectedId of selectedModules) {
            const selectedMod = modules.find(m => m.id === selectedId);
            if (selectedMod?.constraints?.incompatible_with?.includes(mod.id)) {
                return { valid: false, reason: `Incompatible with ${selectedMod.name}` };
            }
        }

        // 3. Check Slot Requirements (Simple existence check)
        if (mod.specs?.requires_slot_type) {
            const reqType = mod.specs.requires_slot_type;
            const hasSlot = baseProduct.slots?.some(s => s.type === reqType);
            if (!hasSlot && reqType !== 'power') { // Ignore power for now as it's not always in slots
                return { valid: false, reason: `Requires ${reqType.toUpperCase()} port` };
            }
        }

        return { valid: true };
    };

    const { addToCart } = useCart();

    const handleAddToCart = () => {
        if (!baseProduct) return;

        const cartItem = {
            id: crypto.randomUUID(), // Unique ID for this specific configuration
            baseProduct: {
                id: baseProduct.id,
                name: baseProduct.name,
                image: baseProduct.imageUrl,
                basePrice: baseProduct.basePrice || baseProduct.price
            },
            variant: selectedVariant,
            modules: modules.filter(m => selectedModules.has(m.id)), // Get full module objects
            totalPrice: calculateTotal()
        };

        addToCart(cartItem);
        navigate('/cart');
    };

    if (loading) return <div className="min-h-screen flex items-center justify-center">Loading Configurator...</div>;
    if (!baseProduct) return <div>Product not found</div>;

    const totalPrice = calculateTotal();

    const Section = ({ title, isOpen, onToggle, children, required, isComplete }) => (
        <div className="border border-gray-200 rounded-xl overflow-hidden mb-4 bg-white shadow-sm">
            <button
                onClick={onToggle}
                className={`w-full flex items-center justify-between text-left p-6 transition-colors ${isOpen ? 'bg-gray-50' : 'hover:bg-gray-50'}`}
            >
                <div className="flex items-center gap-3">
                    {isComplete ? (
                        <div className="bg-green-100 text-green-600 rounded-full p-1">
                            <Check size={16} strokeWidth={3} />
                        </div>
                    ) : (
                        <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center text-xs font-bold ${required ? 'border-blue-600 text-blue-600' : 'border-gray-300 text-gray-400'}`}>
                            {required ? '!' : ''}
                        </div>
                    )}
                    <h3 className="text-lg font-bold text-gray-900">
                        {title}
                        {required && <span className="ml-2 text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-full font-medium">Required</span>}
                    </h3>
                </div>
                <div className={`transform transition-transform duration-300 ${isOpen ? 'rotate-180' : ''} text-gray-400`}>
                    <ChevronDown size={20} />
                </div>
            </button>

            <div className={`transition-all duration-300 ease-in-out ${isOpen ? 'max-h-[2000px] opacity-100' : 'max-h-0 opacity-0 overflow-hidden'}`}>
                <div className="p-6 pt-0 border-t border-gray-100">
                    <div className="mt-6">
                        {children}
                    </div>
                </div>
            </div>
        </div>
    );

    const OptionCard = ({ title, price, description, selected, onClick, type = 'radio', disabled, disabledReason, tags }) => (
        <div
            onClick={!disabled ? onClick : undefined}
            className={`
                relative p-5 rounded-lg border-2 transition-all duration-200
                ${disabled
                    ? 'border-gray-100 bg-gray-50 cursor-not-allowed opacity-60'
                    : 'cursor-pointer hover:border-gray-300 hover:shadow-md'
                }
                ${selected && !disabled
                    ? 'border-blue-600 bg-blue-50/30'
                    : (!disabled ? 'border-gray-200 bg-white' : '')
                }
            `}
        >
            <div className="flex justify-between items-start mb-2">
                <div className="flex flex-col gap-1 pr-8">
                    <span className={`font-bold text-lg ${selected ? 'text-blue-900' : 'text-gray-900'}`}>{title}</span>
                    {/* Tags */}
                    {tags && tags.length > 0 && (
                        <div className="flex gap-1 flex-wrap mt-1">
                            {tags.map(tag => (
                                <span key={tag} className={`text-[10px] uppercase tracking-wider font-bold px-1.5 py-0.5 rounded border ${tag === 'Industrial' ? 'bg-slate-800 text-white border-slate-800' :
                                    tag === 'Commercial' ? 'bg-blue-100 text-blue-800 border-blue-200' :
                                        'bg-gray-100 text-gray-600 border-gray-200'
                                    }`}>
                                    {tag}
                                </span>
                            ))}
                        </div>
                    )}
                </div>
                <div className={`text-right whitespace-nowrap ${selected ? 'mr-8' : ''}`}>
                    <span className={`text-sm font-bold ${selected ? 'text-blue-700' : 'text-gray-700'}`}>
                        {price === 0 ? 'Included' : `+₹${price.toLocaleString()}`}
                    </span>
                </div>
            </div>
            <p className="text-sm text-gray-500 mb-4 leading-relaxed">{description}</p>

            {disabled && (
                <div className="flex items-center gap-2 text-xs font-bold text-red-500 mt-3 bg-red-50 p-2 rounded">
                    <AlertCircle size={14} />
                    {disabledReason}
                </div>
            )}

            {selected && !disabled && (
                <div className="absolute top-4 right-4 text-blue-600">
                    <div className="bg-blue-600 text-white rounded-full p-1">
                        <Check size={12} strokeWidth={4} />
                    </div>
                </div>
            )}
        </div>
    );

    return (
        <div className="min-h-screen bg-gray-50/50 pb-32 lg:pb-0"> {/* Add padding bottom for mobile sticky bar */}
            <div className="max-w-[1600px] mx-auto">
                <div className="grid grid-cols-1 lg:grid-cols-12 min-h-screen">

                    {/* LEFT: Visual & Summary (Desktop: Sticky Sidebar, Mobile: Top Image) */}
                    <div className="lg:col-span-7 bg-white lg:h-screen lg:sticky lg:top-0 border-r border-gray-200 flex flex-col z-10 overflow-hidden">

                        {/* Header / Back Button */}
                        <div className="p-6 lg:p-8 pb-0 shrink-0">
                            <button onClick={() => navigate('/')} className="text-sm font-medium text-gray-500 hover:text-black flex items-center gap-2 transition-colors">
                                &larr; Back to Products
                            </button>
                        </div>

                        {/* Main Content Area (Restricted to available height) */}
                        <div className="flex-1 flex flex-col justify-center p-6 lg:p-8 min-h-0">

                            {/* Product Image (Flex-1 to take available space, object-contain to fit) */}
                            <div className="flex-1 relative flex items-center justify-center min-h-0 mb-6">
                                <img
                                    src={baseProduct.imageUrl}
                                    alt={baseProduct.name}
                                    className="w-full h-full object-contain mix-blend-multiply drop-shadow-xl p-4 max-h-[50vh]"
                                />
                            </div>

                            {/* Desktop Summary Card (Hidden on Mobile) */}
                            <div className="hidden lg:block bg-gray-50 rounded-2xl p-6 shadow-sm border border-gray-100 shrink-0">
                                <div className="mb-4">
                                    <h2 className="text-2xl font-bold text-gray-900 mb-1">{baseProduct.name}</h2>
                                    <p className="text-gray-500 text-sm">Custom Configuration</p>
                                </div>

                                {/* Compact Selected Items List */}
                                <div className="space-y-2 mb-6 text-sm max-h-[120px] overflow-y-auto pr-2 custom-scrollbar">
                                    {selectedVariant && (
                                        <div className="flex justify-between items-center group">
                                            <span className="text-gray-600 font-medium">Platform</span>
                                            <span className="text-gray-900">{selectedVariant.name}</span>
                                        </div>
                                    )}
                                    {modules.filter(m => selectedModules.has(m.id)).map(m => (
                                        <div key={m.id} className="flex justify-between items-center group">
                                            <span className="text-gray-500 truncate mr-4">{m.name}</span>
                                            <span className="text-gray-900 font-medium whitespace-nowrap">
                                                {m.price > 0 ? `+₹${m.price.toLocaleString()}` : 'Incl.'}
                                            </span>
                                        </div>
                                    ))}
                                </div>

                                <div className="border-t border-gray-200 pt-4 flex justify-between items-end gap-4">
                                    <div>
                                        <div className="text-xs text-gray-400 uppercase tracking-wide font-bold mb-1">Total</div>
                                        <div className="text-3xl font-bold text-gray-900 tracking-tight">₹{totalPrice.toLocaleString()}</div>
                                    </div>
                                    <button
                                        onClick={handleAddToCart}
                                        className="bg-black text-white px-8 py-3 rounded-xl font-bold hover:bg-gray-800 transition-all transform hover:scale-[1.02] active:scale-95 flex items-center gap-2 shadow-lg shadow-gray-200"
                                    >
                                        <ShoppingCart size={18} />
                                        <span>Add to Cart</span>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* RIGHT: Scrollable Options */}
                    <div className="lg:col-span-5 bg-gray-50 p-6 lg:p-10">

                        <div className="mb-8 lg:mb-10 lg:pt-8">
                            <h1 className="text-2xl lg:text-3xl font-bold mb-3 text-gray-900">Build Your Device</h1>
                            <p className="text-gray-500 leading-relaxed text-sm lg:text-base">
                                Configure your {baseProduct.name}. All modules are hot-swappable and verified for compatibility.
                            </p>
                        </div>

                        <div className="space-y-4"> {/* Accordion Container */}
                            {/* 1. Base Variant */}
                            <Section
                                title="Platform Edition"
                                isOpen={openSections.variants}
                                onToggle={() => setOpenSections(prev => ({ ...prev, variants: !prev.variants }))}
                                required
                                isComplete={!!selectedVariant}
                            >
                                <div className="grid grid-cols-1 gap-3">
                                    {baseProduct.variants?.map(variant => (
                                        <OptionCard
                                            key={variant.id}
                                            title={variant.name}
                                            price={variant.priceMod}
                                            description={variant.description || "Standard platform edition."}
                                            selected={selectedVariant?.id === variant.id}
                                            onClick={() => setSelectedVariant(variant)}
                                        />
                                    ))}
                                </div>
                            </Section>

                            {/* 2. Sensors */}
                            <Section
                                title="Sensors & Modules"
                                isOpen={openSections.sensors}
                                onToggle={() => setOpenSections(prev => ({ ...prev, sensors: !prev.sensors }))}
                                isComplete={selectedModules.size > 0}
                            >
                                <div className="grid grid-cols-1 gap-3">
                                    {modules.filter(m => m.type === 'module').map(mod => {
                                        const { valid, reason } = checkCompatibility(mod);
                                        return (
                                            <OptionCard
                                                key={mod.id}
                                                title={mod.name}
                                                price={mod.price}
                                                description={mod.description}
                                                tags={mod.tags}
                                                selected={selectedModules.has(mod.id)}
                                                onClick={() => toggleModule(mod.id)}
                                                type="checkbox"
                                                disabled={!valid && !selectedModules.has(mod.id)}
                                                disabledReason={reason}
                                            />
                                        );
                                    })}
                                </div>
                            </Section>

                            {/* 3. Power */}
                            <Section
                                title="Power & Accessories"
                                isOpen={openSections.power}
                                onToggle={() => setOpenSections(prev => ({ ...prev, power: !prev.power }))}
                                isComplete={false}
                            >
                                <div className="grid grid-cols-1 gap-3">
                                    {modules.filter(m => m.type === 'accessory').map(mod => (
                                        <OptionCard
                                            key={mod.id}
                                            title={mod.name}
                                            price={mod.price}
                                            description={mod.description}
                                            tags={mod.tags}
                                            selected={selectedModules.has(mod.id)}
                                            onClick={() => toggleModule(mod.id)}
                                            type="checkbox"
                                        />
                                    ))}
                                </div>
                            </Section>
                        </div>
                    </div>
                </div>
            </div>

            {/* Mobile Sticky Footer */}
            <div className="lg:hidden fixed bottom-0 left-0 right-0 bg-white/80 backdrop-blur-md border-t border-gray-200 p-4 shadow-[0_-4px_20px_rgba(0,0,0,0.1)] z-50 safe-area-bottom">
                <div className="max-w-[1600px] mx-auto flex justify-between items-center">
                    <div>
                        <div className="text-xs text-gray-500 font-bold uppercase tracking-wider">Total</div>
                        <div className="text-xl font-bold text-gray-900">₹{totalPrice.toLocaleString()}</div>
                    </div>
                    <button
                        onClick={handleAddToCart}
                        className="bg-black text-white px-6 py-3 rounded-xl font-bold shadow-lg hover:bg-gray-800 transition-all flex items-center gap-2 active:scale-95"
                    >
                        <span>Add to Cart</span>
                        <ChevronDown className="rotate-[-90deg]" size={16} />
                    </button>
                </div>
            </div>

        </div>
    );
}


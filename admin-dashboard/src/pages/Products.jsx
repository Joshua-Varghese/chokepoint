import { useEffect, useState } from 'react';
import { collection, getDocs, addDoc, updateDoc, deleteDoc, doc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Plus, X, Pencil, Trash2, AlertCircle, Box, Layers, Settings2, Search } from 'lucide-react';
import toast from 'react-hot-toast';

// Helper for Slot Row
const SlotRow = ({ slot, onChange, onRemove }) => (
    <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
        <input
            placeholder="Slot ID (e.g. sensor_1)"
            className="input"
            value={slot.id}
            onChange={e => onChange('id', e.target.value)}
            style={{ flex: 1 }}
        />
        <select
            className="input"
            value={slot.type}
            onChange={e => onChange('type', e.target.value)}
            style={{ width: '100px' }}
        >
            <option value="uart">UART</option>
            <option value="i2c">I2C</option>
            <option value="usb">USB</option>
            <option value="power">Power</option>
        </select>
        <button type="button" onClick={onRemove} style={{ color: 'red' }}><Trash2 size={16} /></button>
    </div>
);

// Helper for Variant Row
const VariantRow = ({ variant, onChange, onRemove }) => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '1rem', padding: '0.5rem', border: '1px solid #eee', borderRadius: '4px', background: '#fff' }}>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
            <input
                placeholder="Variant Name (e.g. Standard ESP32)"
                className="input"
                value={variant.name}
                onChange={e => onChange('name', e.target.value)}
                style={{ flex: 2 }}
            />
            <input
                type="number"
                placeholder="Price Mod (+₹)"
                className="input"
                value={variant.priceMod}
                onChange={e => onChange('priceMod', Number(e.target.value))}
                style={{ flex: 1 }}
            />
            <button type="button" onClick={onRemove} style={{ color: 'red' }}><Trash2 size={16} /></button>
        </div>
        <input
            placeholder="Description (e.g. Industrial grade enclosure...)"
            className="input"
            value={variant.description || ''}
            onChange={e => onChange('description', e.target.value)}
            style={{ width: '100%', fontSize: '0.85rem' }}
        />
    </div>
);

export default function Products() {
    const [allProducts, setAllProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [moduleSearch, setModuleSearch] = useState('');

    // Form State — focused on base/featured products
    const [formData, setFormData] = useState({
        name: '', price: '', stock: '', category: 'monitors',
        type: 'base', imageUrl: '',
        slots: [], compatibleModules: [], defaultModules: [], tags: [], variants: [],
        visibility: 'featured',
        badges: []
    });

    const fetchData = async () => {
        setLoading(true);
        try {
            const snap = await getDocs(collection(db, 'products'));
            setAllProducts(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
        } catch (error) {
            console.error("Error fetching products:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    // Filter: Only show featured / base products
    const products = allProducts.filter(p =>
        p.visibility === 'featured' || (!p.visibility && p.type === 'base')
    );

    // Parts for the whitelist (sensors + accessories from inventory)
    const inventoryParts = allProducts.filter(p =>
        p.visibility === 'part' || (!p.visibility && p.type !== 'base')
    );

    const handleEdit = (product) => {
        setFormData({
            name: product.name,
            price: product.price,
            stock: product.stock,
            category: product.category,
            type: product.type || 'base',
            imageUrl: product.imageUrl || '',
            slots: product.slots || [],
            compatibleModules: product.compatibleModules || [],
            defaultModules: product.defaultModules || [],
            variants: product.variants || [],
            tags: product.tags || [],
            visibility: product.visibility || 'featured',
            badges: product.badges || []
        });
        setEditingId(product.id);
        setIsModalOpen(true);
    };

    const [deleteId, setDeleteId] = useState(null);

    const confirmDelete = async () => {
        if (!deleteId) return;
        try {
            await deleteDoc(doc(db, 'products', deleteId));
            toast.success("Product Deleted");
            fetchData();
        } catch (error) {
            toast.error("Failed to delete product.");
        } finally {
            setDeleteId(null);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            const productData = {
                name: formData.name,
                price: Number(formData.price),
                stock: Number(formData.stock),
                category: formData.category,
                type: formData.type,
                imageUrl: formData.imageUrl || 'https://via.placeholder.com/150',
                tags: formData.tags || [],
                badges: formData.badges || [],
                visibility: 'featured',
                updatedAt: serverTimestamp(),
                slots: formData.slots || [],
                compatibleModules: formData.compatibleModules || [],
                defaultModules: formData.defaultModules || [],
                variants: formData.variants || []
            };

            if (editingId) {
                await updateDoc(doc(db, 'products', editingId), productData);
                toast.success("Product Updated!");
            } else {
                await addDoc(collection(db, 'products'), {
                    ...productData,
                    createdAt: serverTimestamp()
                });
                toast.success("Product Added!");
            }
            closeModal();
            fetchData();
        } catch (error) {
            toast.error("Failed to save product.");
        }
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setEditingId(null);
        setModuleSearch('');
        setFormData({ name: '', price: '', stock: '', category: 'monitors', type: 'base', imageUrl: '', slots: [], compatibleModules: [], defaultModules: [], tags: [], variants: [], visibility: 'featured', badges: [] });
    };

    return (
        <div>
            {/* Stats Bar */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
                <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{ width: 40, height: 40, borderRadius: 8, background: '#e0f2fe', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Box size={20} color="#0369a1" />
                    </div>
                    <div>
                        <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{products.length}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Products</div>
                    </div>
                </div>
                <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{ width: 40, height: 40, borderRadius: 8, background: '#f0fdf4', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Layers size={20} color="#15803d" />
                    </div>
                    <div>
                        <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>
                            {products.reduce((sum, p) => sum + (p.variants?.length || 0), 0)}
                        </div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Total Variants</div>
                    </div>
                </div>
                <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{ width: 40, height: 40, borderRadius: 8, background: '#fefce8', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Settings2 size={20} color="#854d0e" />
                    </div>
                    <div>
                        <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{inventoryParts.length}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Available Parts</div>
                    </div>
                </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: '700' }}>Products</h1>
                <button onClick={() => setIsModalOpen(true)} className="btn btn-primary" style={{ background: '#000' }}>
                    <Plus size={16} /> Add Product
                </button>
            </div>

            <div className="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Base Price</th>
                            <th>Variants</th>
                            <th>Modules</th>
                            <th>Tags</th>
                            <th>Badges</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="6" style={{ textAlign: 'center' }}>Loading...</td></tr>
                        ) : products.length === 0 ? (
                            <tr><td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>No products yet. Add your first base module!</td></tr>
                        ) : products.map(p => (
                            <tr key={p.id}>
                                <td style={{ fontWeight: 500 }}>{p.name}</td>
                                <td>₹{p.price?.toLocaleString()}</td>
                                <td>
                                    <span style={{ padding: '0.15rem 0.5rem', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 600, background: '#e0f2fe', color: '#0369a1' }}>
                                        {p.variants?.length || 0} editions
                                    </span>
                                </td>
                                <td>
                                    <span style={{ padding: '0.15rem 0.5rem', borderRadius: '999px', fontSize: '0.75rem', fontWeight: 600, background: '#f0fdf4', color: '#15803d' }}>
                                        {p.compatibleModules?.length || 0} compatible
                                    </span>
                                </td>
                                <td>
                                    <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                                        {(p.tags || []).map(tag => (
                                            <span key={tag} style={{ padding: '0.1rem 0.4rem', borderRadius: '999px', fontSize: '0.65rem', background: '#f1f5f9', color: '#475569', border: '1px solid #e2e8f0' }}>
                                                {tag}
                                            </span>
                                        ))}
                                    </div>
                                </td>
                                <td>
                                    <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                                        {(p.badges || []).map(badge => {
                                            const badgeStyles = {
                                                'New Arrival': { bg: '#fef9c3', color: '#854d0e' },
                                                'Best Seller': { bg: '#fce7f3', color: '#9d174d' },
                                                'Featured': { bg: '#e0f2fe', color: '#075985' },
                                                'Staff Pick': { bg: '#f0fdf4', color: '#166534' },
                                            };
                                            const s = badgeStyles[badge] || { bg: '#f1f5f9', color: '#475569' };
                                            return (
                                                <span key={badge} style={{ padding: '0.1rem 0.5rem', borderRadius: '999px', fontSize: '0.65rem', fontWeight: 600, background: s.bg, color: s.color }}>
                                                    {badge}
                                                </span>
                                            );
                                        })}
                                    </div>
                                </td>
                                <td style={{ textAlign: 'right' }}>
                                    <button onClick={() => handleEdit(p)} className="btn btn-ghost" style={{ padding: '0.25rem 0.5rem', marginRight: '0.5rem' }}>
                                        <Pencil size={16} />
                                    </button>
                                    <button onClick={() => setDeleteId(p.id)} className="btn btn-ghost" style={{ padding: '0.25rem 0.5rem', color: 'var(--danger)' }}>
                                        <Trash2 size={16} />
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Product Edit/Add Modal */}
            {isModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 1000
                }}>
                    <div className="card" style={{ width: '100%', maxWidth: '600px', background: '#fff', maxHeight: '90vh', overflowY: 'auto' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                            <h2 style={{ fontSize: '1.25rem', fontWeight: '600' }}>{editingId ? 'Edit Product' : 'Add New Product'}</h2>
                            <button onClick={closeModal} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                                <X size={20} />
                            </button>
                        </div>
                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

                            {/* Basics */}
                            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr', gap: '1rem' }}>
                                <div>
                                    <label className="label">Name</label>
                                    <input className="input" required value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} />
                                </div>
                                <div>
                                    <label className="label">Base Price (₹)</label>
                                    <input type="number" className="input" required value={formData.price} onChange={e => setFormData({ ...formData, price: e.target.value })} />
                                </div>
                                <div>
                                    <label className="label">Stock</label>
                                    <input type="number" className="input" required value={formData.stock} onChange={e => setFormData({ ...formData, stock: e.target.value })} />
                                </div>
                            </div>

                            <hr style={{ borderColor: 'var(--border)' }} />

                            {/* Platform Editions (Variants) */}
                            <div style={{ background: '#f8fafc', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                                <label className="label" style={{ fontWeight: 'bold', color: '#0f172a' }}>Platform Editions (Variants)</label>
                                <p style={{ fontSize: '0.8rem', color: '#64748b', marginBottom: '0.5rem' }}>
                                    Define the core versions of this platform (e.g., Standard, High-Power, Industrial).
                                </p>

                                {(formData.variants || []).map((variant, idx) => (
                                    <VariantRow
                                        key={idx}
                                        variant={variant}
                                        onChange={(k, v) => {
                                            const newVariants = [...(formData.variants || [])];
                                            newVariants[idx][k] = v;
                                            setFormData({ ...formData, variants: newVariants });
                                        }}
                                        onRemove={() => {
                                            setFormData({ ...formData, variants: formData.variants.filter((_, i) => i !== idx) });
                                        }}
                                    />
                                ))}
                                <button
                                    type="button"
                                    className="btn btn-outline"
                                    style={{ background: 'white' }}
                                    onClick={() => setFormData({
                                        ...formData,
                                        variants: [...(formData.variants || []), { id: `var_${Date.now()}`, name: '', priceMod: 0, description: '' }]
                                    })}
                                >
                                    <Plus size={14} /> Add Edition
                                </button>
                            </div>

                            {/* Compatible Modules Whitelist */}
                            <div style={{ marginBottom: '0.5rem' }}>
                                <label className="label" style={{ fontWeight: 'bold' }}>Compatible Modules (from Inventory)</label>
                                <p style={{ fontSize: '0.8rem', color: '#666', marginBottom: '0.5rem' }}>
                                    Select which parts from Inventory are valid add-ons for this product.
                                </p>

                                <div style={{ position: 'relative', marginBottom: '0.5rem' }}>
                                    <Search size={14} style={{ position: 'absolute', left: '0.5rem', top: '50%', transform: 'translateY(-50%)', color: '#a1a1aa' }} />
                                    <input
                                        className="input"
                                        placeholder="Search modules..."
                                        value={moduleSearch}
                                        onChange={e => setModuleSearch(e.target.value)}
                                        style={{ paddingLeft: '1.75rem', fontSize: '0.8rem' }}
                                    />
                                </div>

                                <div style={{ maxHeight: '150px', overflowY: 'auto', border: '1px solid #e4e4e7', borderRadius: '6px', padding: '0.5rem' }}>
                                    {inventoryParts.length > 0 ? inventoryParts.filter(p => !moduleSearch || p.name.toLowerCase().includes(moduleSearch.toLowerCase())).map(p => (
                                        <div key={p.id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.25rem 0' }}>
                                            <label style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', cursor: 'pointer' }}>
                                                <input
                                                    type="checkbox"
                                                    checked={(formData.compatibleModules || []).includes(p.id)}
                                                    onChange={e => {
                                                        const current = formData.compatibleModules || [];
                                                        if (e.target.checked) {
                                                            setFormData({ ...formData, compatibleModules: [...current, p.id] });
                                                        } else {
                                                            const currentDefaults = formData.defaultModules || [];
                                                            setFormData({
                                                                ...formData,
                                                                compatibleModules: current.filter(id => id !== p.id),
                                                                defaultModules: currentDefaults.filter(id => id !== p.id)
                                                            });
                                                        }
                                                    }}
                                                />
                                                <span style={{ fontSize: '0.9rem' }}>{p.name}</span>
                                                <span style={{
                                                    fontSize: '0.7rem', padding: '0.1rem 0.35rem', borderRadius: '4px',
                                                    background: p.type === 'module' ? '#f0fdf4' : '#fefce8',
                                                    color: p.type === 'module' ? '#15803d' : '#854d0e'
                                                }}>
                                                    {p.type === 'module' ? 'Sensor' : 'Accessory'}
                                                </span>
                                            </label>

                                            {(formData.compatibleModules || []).includes(p.id) && (
                                                <label style={{ display: 'flex', gap: '0.25rem', alignItems: 'center', cursor: 'pointer', fontSize: '0.75rem', color: '#666' }}>
                                                    <input
                                                        type="checkbox"
                                                        checked={(formData.defaultModules || []).includes(p.id)}
                                                        onChange={e => {
                                                            const currentDefaults = formData.defaultModules || [];
                                                            if (e.target.checked) {
                                                                setFormData({ ...formData, defaultModules: [...currentDefaults, p.id] });
                                                            } else {
                                                                setFormData({ ...formData, defaultModules: currentDefaults.filter(id => id !== p.id) });
                                                            }
                                                        }}
                                                    />
                                                    Default?
                                                </label>
                                            )}
                                        </div>
                                    )) : (
                                        <div style={{ padding: '1rem', textAlign: 'center', color: '#999', fontSize: '0.8rem' }}>
                                            No parts in Inventory. Add sensors/accessories there first!
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Physical Limits (Slots) */}
                            <div>
                                <label className="label" style={{ fontWeight: 'bold' }}>Physical Connectivity Limits</label>
                                <p style={{ fontSize: '0.8rem', color: '#666', marginBottom: '0.5rem' }}>
                                    Define the actual hardware ports available (e.g., 2 UART ports).
                                </p>

                                {(formData.slots || []).map((slot, idx) => (
                                    <SlotRow
                                        key={idx}
                                        slot={slot}
                                        onChange={(k, v) => {
                                            const newSlots = [...(formData.slots || [])];
                                            newSlots[idx][k] = v;
                                            setFormData({ ...formData, slots: newSlots });
                                        }}
                                        onRemove={() => {
                                            setFormData({ ...formData, slots: formData.slots.filter((_, i) => i !== idx) });
                                        }}
                                    />
                                ))}
                                <button
                                    type="button"
                                    className="btn btn-outline"
                                    onClick={() => setFormData({
                                        ...formData,
                                        slots: [...(formData.slots || []), { id: `slot_${(formData.slots?.length || 0) + 1}`, type: 'uart' }]
                                    })}
                                >
                                    <Plus size={14} /> Add Port Limit
                                </button>
                            </div>

                            {/* Usage Tags */}
                            <div style={{ background: '#f0f9ff', padding: '1rem', borderRadius: '8px' }}>
                                <label className="label" style={{ fontWeight: 'bold', color: '#0369a1' }}>Usage Tags</label>
                                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                                    {['Personal', 'Commercial', 'Industrial'].map(tag => (
                                        <button
                                            key={tag}
                                            type="button"
                                            onClick={() => {
                                                const currentTags = formData.tags || [];
                                                if (currentTags.includes(tag)) {
                                                    setFormData({ ...formData, tags: currentTags.filter(t => t !== tag) });
                                                } else {
                                                    setFormData({ ...formData, tags: [...currentTags, tag] });
                                                }
                                            }}
                                            style={{
                                                padding: '0.25rem 0.75rem', borderRadius: '999px', border: '1px solid',
                                                borderColor: (formData.tags || []).includes(tag) ? '#0284c7' : '#cbd5e1',
                                                background: (formData.tags || []).includes(tag) ? '#0284c7' : 'white',
                                                color: (formData.tags || []).includes(tag) ? 'white' : '#64748b',
                                                fontSize: '0.85rem', cursor: 'pointer', transition: 'all 0.2s'
                                            }}
                                        >
                                            {tag}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* App Badges */}
                            <div style={{ background: '#fffbeb', padding: '1rem', borderRadius: '8px', border: '1px solid #fde68a' }}>
                                <label className="label" style={{ fontWeight: 'bold', color: '#92400e' }}>📱 App Badges</label>
                                <p style={{ fontSize: '0.8rem', color: '#78350f', marginBottom: '0.75rem' }}>
                                    Products with at least one badge will appear in the Android app. Leave empty to hide from app.
                                </p>
                                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                                    {[
                                        { label: '🆕 New Arrival', value: 'New Arrival', active: '#f59e0b', activeBg: '#fef3c7', activeTxt: '#92400e' },
                                        { label: '🔥 Best Seller', value: 'Best Seller', active: '#ec4899', activeBg: '#fce7f3', activeTxt: '#9d174d' },
                                        { label: '⭐ Featured', value: 'Featured', active: '#0284c7', activeBg: '#e0f2fe', activeTxt: '#075985' },
                                        { label: '✅ Staff Pick', value: 'Staff Pick', active: '#16a34a', activeBg: '#f0fdf4', activeTxt: '#166534' },
                                    ].map(({ label, value, active, activeBg, activeTxt }) => {
                                        const isActive = (formData.badges || []).includes(value);
                                        return (
                                            <button
                                                key={value}
                                                type="button"
                                                onClick={() => {
                                                    const current = formData.badges || [];
                                                    if (isActive) {
                                                        setFormData({ ...formData, badges: current.filter(b => b !== value) });
                                                    } else {
                                                        setFormData({ ...formData, badges: [...current, value] });
                                                    }
                                                }}
                                                style={{
                                                    padding: '0.35rem 0.9rem', borderRadius: '999px', border: '1.5px solid',
                                                    borderColor: isActive ? active : '#d1d5db',
                                                    background: isActive ? activeBg : 'white',
                                                    color: isActive ? activeTxt : '#6b7280',
                                                    fontSize: '0.85rem', fontWeight: isActive ? 600 : 400,
                                                    cursor: 'pointer', transition: 'all 0.2s'
                                                }}
                                            >
                                                {label}
                                            </button>
                                        );
                                    })}
                                </div>
                                {(formData.badges || []).length === 0 && (
                                    <p style={{ fontSize: '0.75rem', color: '#b45309', marginTop: '0.5rem' }}>
                                        ⚠️ No badges selected — this product will be hidden from the Android app.
                                    </p>
                                )}
                            </div>

                            {/* Image */}
                            <div>
                                <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Product Image</label>
                                <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                                    <div style={{
                                        width: '60px', height: '60px', borderRadius: '8px',
                                        background: '#f4f4f5', border: '1px solid #e4e4e7',
                                        display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden'
                                    }}>
                                        {formData.imageUrl ? (
                                            <img src={formData.imageUrl} alt="Preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                        ) : (
                                            <span style={{ fontSize: '0.75rem', color: '#a1a1aa' }}>No Img</span>
                                        )}
                                    </div>
                                    <input
                                        type="file"
                                        accept="image/*"
                                        onChange={(e) => {
                                            const file = e.target.files[0];
                                            if (file) {
                                                const reader = new FileReader();
                                                reader.onloadend = () => {
                                                    setFormData({ ...formData, imageUrl: reader.result, imageFile: file });
                                                };
                                                reader.readAsDataURL(file);
                                            }
                                        }}
                                        style={{ fontSize: '0.875rem' }}
                                    />
                                </div>
                            </div>

                            <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem', background: '#000' }}>
                                {editingId ? 'Update Product' : 'Save Product'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Modal */}
            {deleteId && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 1100
                }}>
                    <div className="card" style={{ width: '100%', maxWidth: '400px', background: '#fff', padding: '1.5rem', textAlign: 'center' }}>
                        <div style={{ marginBottom: '1rem', color: 'var(--danger)', display: 'flex', justifyContent: 'center' }}>
                            <AlertCircle size={48} />
                        </div>
                        <h2 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>Delete Product?</h2>
                        <p style={{ color: '#666', marginBottom: '1.5rem' }}>
                            This will remove the product and all its configuration. This action cannot be undone.
                        </p>
                        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                            <button onClick={() => setDeleteId(null)} className="btn btn-ghost" style={{ border: '1px solid #e4e4e7' }}>
                                Cancel
                            </button>
                            <button onClick={confirmDelete} className="btn btn-primary" style={{ background: 'var(--danger)', border: 'none' }}>
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

import { useEffect, useState } from 'react';
import { collection, getDocs, addDoc, updateDoc, deleteDoc, doc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Plus, X, Pencil, Trash2, AlertCircle } from 'lucide-react';
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
    <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
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
);

export default function Products() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);

    // Form State
    const [formData, setFormData] = useState({
        name: '', price: '', stock: '', category: 'monitors',
        type: 'module', imageUrl: '',
        slots: [], compatibleModules: [], defaultModules: [], tags: [], variants: [], specs: {}, constraints: {}
    });

    const fetchData = async () => {
        setLoading(true);
        try {
            const snap = await getDocs(collection(db, 'products'));
            setProducts(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
        } catch (error) {
            console.error("Error fetching products:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleEdit = (product) => {
        setFormData({
            name: product.name,
            price: product.price,
            stock: product.stock,
            category: product.category,
            type: product.type || 'module',
            imageUrl: product.imageUrl || '',
            slots: product.slots || [],
            compatibleModules: product.compatibleModules || [],
            defaultModules: product.defaultModules || [],
            variants: product.variants || [],
            tags: product.tags || [],
            specs: product.specs || {},
            constraints: product.constraints || {}
        });
        setEditingId(product.id);
        setIsModalOpen(true);
    };

    const [deleteId, setDeleteId] = useState(null);

    const handleDeleteClick = (id) => {
        setDeleteId(id);
    };

    const confirmDelete = async () => {
        if (!deleteId) return;
        try {
            await deleteDoc(doc(db, 'products', deleteId));
            toast.success("Product Deleted Successfully");
            fetchData();
        } catch (error) {
            console.error("Error deleting product:", error);
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
                updatedAt: serverTimestamp(),

                // Base Station Logic
                ...(formData.type === 'base' && {
                    slots: formData.slots || [],
                    compatibleModules: formData.compatibleModules || [],
                    variants: formData.variants || []
                }),

                // Module Logic
                ...(formData.type !== 'base' && {
                    specs: {
                        requires_slot_type: formData.specs?.requires_slot_type || 'uart',
                        power_draw: parseFloat(formData.specs?.power_draw || 0)
                    },
                    constraints: {
                        incompatible_with: formData.constraints?.incompatible_with || [] // Array of IDs
                    }
                })
            };

            if (editingId) {
                await updateDoc(doc(db, 'products', editingId), productData);
                toast.success("Product Updated Successfully!");
            } else {
                await addDoc(collection(db, 'products'), {
                    ...productData,
                    createdAt: serverTimestamp()
                });
                toast.success("Product Added Successfully!");
            }
            closeModal();
            fetchData();
        } catch (error) {
            console.error("Error saving product:", error);
            toast.error("Failed to save product.");
        }
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setEditingId(null);
        setFormData({ name: '', price: '', stock: '', category: 'monitors', type: 'module', slots: [], variants: [], specs: {}, constraints: {} });
    };

    return (
        <div>
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
                            <th>Price</th>
                            <th>Stock</th>
                            <th>Type</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="5" style={{ textAlign: 'center' }}>Loading...</td></tr>
                        ) : products.map(p => (
                            <tr key={p.id}>
                                <td>{p.name}</td>
                                <td>₹{p.price}</td>
                                <td>{p.stock !== undefined ? p.stock : 'N/A'}</td>
                                <td>
                                    <span style={{
                                        padding: '0.25rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 'bold',
                                        background: p.type === 'base' ? '#e0f2fe' : p.type === 'module' ? '#f0fdf4' : '#fefce8',
                                        color: p.type === 'base' ? '#0369a1' : p.type === 'module' ? '#15803d' : '#854d0e'
                                    }}>
                                        {p.type === 'base' ? 'Platform' : p.type === 'module' ? 'Sensor' : 'Accessory'}
                                    </span>
                                </td>
                                <td style={{ textAlign: 'right' }}>
                                    <button onClick={() => handleEdit(p)} className="btn btn-ghost" style={{ padding: '0.25rem 0.5rem', marginRight: '0.5rem' }}>
                                        <Pencil size={16} />
                                    </button>
                                    <button onClick={() => handleDeleteClick(p.id)} className="btn btn-ghost" style={{ padding: '0.25rem 0.5rem', color: 'var(--danger)' }}>
                                        <Trash2 size={16} />
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Modal */}
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
                                    <label className="label">Price (₹)</label>
                                    <input type="number" className="input" required value={formData.price} onChange={e => setFormData({ ...formData, price: e.target.value })} />
                                </div>
                                <div>
                                    <label className="label">Stock</label>
                                    <input type="number" className="input" required value={formData.stock} onChange={e => setFormData({ ...formData, stock: e.target.value })} />
                                </div>
                            </div>

                            {/* Type Selector */}
                            <div>
                                <label className="label">Product Type</label>
                                <select
                                    className="input"
                                    value={formData.type || 'module'}
                                    onChange={e => setFormData({ ...formData, type: e.target.value })}
                                >
                                    <option value="base">Platform (Hub)</option>
                                    <option value="module">Sensor / Module</option>
                                    <option value="accessory">Accessory (Power/Antenna)</option>
                                </select>
                            </div>

                            <hr style={{ borderColor: 'var(--border)' }} />

                            {/* CONDITIONAL: Base Station Slots */}
                            {/* CONDITIONAL: Base Station / Platform Logic */}
                            {formData.type === 'base' && (
                                <>
                                    {/* 0. Platform Variants */}
                                    <div style={{ marginBottom: '1.5rem', background: '#f8fafc', padding: '1rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                                        <label className="label" style={{ fontWeight: 'bold', color: '#0f172a' }}>Platform Editions (Variants)</label>
                                        <p style={{ fontSize: '0.8rem', color: '#64748b', marginBottom: '0.5rem' }}>
                                            Define the core versions of this platform (e.g., Standard ESP32, High-Power, Industrial).
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
                                                    const newVariants = formData.variants.filter((_, i) => i !== idx);
                                                    setFormData({ ...formData, variants: newVariants });
                                                }}
                                            />
                                        ))}
                                        <button
                                            type="button"
                                            className="btn btn-outline"
                                            style={{ background: 'white' }}
                                            onClick={() => setFormData({
                                                ...formData,
                                                variants: [...(formData.variants || []), { id: `var_${Date.now()}`, name: '', priceMod: 0 }]
                                            })}
                                        >
                                            <Plus size={14} /> Add Edition
                                        </button>
                                    </div>

                                    {/* 1. Whitelist: Supported Accessories */}
                                    <div style={{ marginBottom: '1.5rem' }}>
                                        <label className="label" style={{ fontWeight: 'bold' }}>Supported Accessories (Whitelist)</label>
                                        <p style={{ fontSize: '0.8rem', color: '#666', marginBottom: '0.5rem' }}>
                                            Select which modules are valid for this platform.
                                        </p>

                                        <div style={{ maxHeight: '150px', overflowY: 'auto', border: '1px solid #e4e4e7', borderRadius: '6px', padding: '0.5rem' }}>
                                            {products.filter(p => p.type !== 'base' && p.id !== editingId).map(p => (
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
                                                                    // Also remove from defaults if unchecked
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
                                                    </label>

                                                    {/* Default Toggle (Only if compatible) */}
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
                                                            Pre-select Default?
                                                        </label>
                                                    )}
                                                </div>
                                            ))}
                                            {products.filter(p => p.type !== 'base').length === 0 && (
                                                <div style={{ padding: '1rem', textAlign: 'center', color: '#999', fontSize: '0.8rem' }}>
                                                    No accessories found. Creates some Modules first!
                                                </div>
                                            )}
                                        </div>
                                    </div>

                                    <hr style={{ borderColor: 'var(--border)', marginBottom: '1.5rem' }} />

                                    {/* 2. Physical Limits (Slots) */}
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
                                                    const newSlots = formData.slots.filter((_, i) => i !== idx);
                                                    setFormData({ ...formData, slots: newSlots });
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
                                </>
                            )}

                            {/* Usage Tags (All Products) */}
                            <div style={{ marginTop: '1.5rem', background: '#f0f9ff', padding: '1rem', borderRadius: '8px' }}>
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
                                                padding: '0.25rem 0.75rem',
                                                borderRadius: '999px',
                                                border: '1px solid',
                                                borderColor: (formData.tags || []).includes(tag) ? '#0284c7' : '#cbd5e1',
                                                background: (formData.tags || []).includes(tag) ? '#0284c7' : 'white',
                                                color: (formData.tags || []).includes(tag) ? 'white' : '#64748b',
                                                fontSize: '0.85rem',
                                                cursor: 'pointer',
                                                transition: 'all 0.2s'
                                            }}
                                        >
                                            {tag}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* CONDITIONAL: Module Requirements */}
                            {formData.type !== 'base' && (
                                <div style={{ background: '#f9f9f9', padding: '1rem', borderRadius: '8px' }}>
                                    <label className="label" style={{ fontWeight: 'bold' }}>Compatibility Rules</label>

                                    <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem' }}>
                                        <div style={{ flex: 1 }}>
                                            <label className="label text-xs">Requires Slot Type</label>
                                            <select
                                                className="input"
                                                value={formData.specs?.requires_slot_type || 'uart'}
                                                onChange={e => setFormData({
                                                    ...formData,
                                                    specs: { ...formData.specs, requires_slot_type: e.target.value }
                                                })}
                                            >
                                                <option value="uart">UART (Serial)</option>
                                                <option value="i2c">I2C (Sensor)</option>
                                                <option value="usb">USB</option>
                                                <option value="power">Power Only</option>
                                            </select>
                                        </div>
                                        <div style={{ flex: 1 }}>
                                            <label className="label text-xs">Power Draw (Watts)</label>
                                            <input
                                                type="number"
                                                className="input"
                                                step="0.1"
                                                value={formData.specs?.power_draw || 0}
                                                onChange={e => setFormData({
                                                    ...formData,
                                                    specs: { ...formData.specs, power_draw: e.target.value }
                                                })}
                                            />
                                        </div>
                                    </div>

                                    <div style={{ marginTop: '1rem' }}>
                                        <label className="label text-xs">Incompatible With (Select items that clash)</label>
                                        <div style={{ maxHeight: '100px', overflowY: 'auto', border: '1px solid #ddd', padding: '0.5rem', borderRadius: '4px' }}>
                                            {products.filter(p => p.id !== editingId).map(p => (
                                                <label key={p.id} style={{ display: 'flex', gap: '0.5rem', fontSize: '0.8rem', alignItems: 'center' }}>
                                                    <input
                                                        type="checkbox"
                                                        checked={(formData.constraints?.incompatible_with || []).includes(p.id)}
                                                        onChange={e => {
                                                            const current = formData.constraints?.incompatible_with || [];
                                                            if (e.target.checked) {
                                                                setFormData({
                                                                    ...formData,
                                                                    constraints: { ...formData.constraints, incompatible_with: [...current, p.id] }
                                                                });
                                                            } else {
                                                                setFormData({
                                                                    ...formData,
                                                                    constraints: { ...formData.constraints, incompatible_with: current.filter(id => id !== p.id) }
                                                                });
                                                            }
                                                        }}
                                                    />
                                                    {p.name}
                                                </label>
                                            ))}
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* Image Upload Placeholder */}
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
                                                // Temporary: Create local URL for preview
                                                const url = URL.createObjectURL(file);
                                                setFormData({ ...formData, imageUrl: url, imageFile: file });
                                            }
                                        }}
                                        style={{ fontSize: '0.875rem' }}
                                    />
                                </div>
                                <p style={{ fontSize: '0.75rem', color: '#71717a', marginTop: '0.25rem' }}>
                                    * Storage integration coming soon.
                                </p>
                            </div>

                            <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem', background: '#000' }}>
                                {editingId ? 'Update Product' : 'Save Product'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* DELETE CONFIRMATION MODAL */}
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
                            Are you sure you want to delete this product? This action cannot be undone.
                        </p>
                        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
                            <button
                                onClick={() => setDeleteId(null)}
                                className="btn btn-ghost"
                                style={{ border: '1px solid #e4e4e7' }}
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmDelete}
                                className="btn btn-primary"
                                style={{ background: 'var(--danger)', border: 'none' }}
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}


import { useEffect, useState } from 'react';
import { collection, getDocs, addDoc, updateDoc, deleteDoc, doc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Plus, X, Pencil, Trash2, AlertCircle, AlertTriangle, Search } from 'lucide-react';
import toast from 'react-hot-toast';

const TYPE_COLORS = {
    module: { bg: '#f0fdf4', color: '#15803d', label: 'Sensor' },
    accessory: { bg: '#fefce8', color: '#854d0e', label: 'Accessory' }
};

export default function Inventory() {
    const [allProducts, setAllProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [deleteId, setDeleteId] = useState(null);
    const [activeTab, setActiveTab] = useState('all'); // all | module | accessory
    const [searchQuery, setSearchQuery] = useState('');

    const [formData, setFormData] = useState({
        name: '', price: '', stock: '', type: 'module', imageUrl: '',
        category: 'monitors',
        specs: { requires_slot_type: 'uart', power_draw: 0 },
        constraints: { incompatible_with: [] },
        visibility: 'part'
    });

    const fetchData = async () => {
        setLoading(true);
        try {
            const snap = await getDocs(collection(db, 'products'));
            setAllProducts(snap.docs.map(d => ({ id: d.id, ...d.data() })));
        } catch (error) {
            console.error("Error fetching inventory:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { fetchData(); }, []);

    // Filter: Only show parts (sensors + accessories, not base)
    const inventoryItems = allProducts.filter(p =>
        p.visibility === 'part' || (!p.visibility && p.type !== 'base')
    );

    // Apply tab filter
    const tabFiltered = activeTab === 'all'
        ? inventoryItems
        : inventoryItems.filter(p => p.type === activeTab);

    // Apply search
    const filtered = searchQuery
        ? tabFiltered.filter(p => p.name.toLowerCase().includes(searchQuery.toLowerCase()))
        : tabFiltered;

    const totalStock = inventoryItems.reduce((sum, p) => sum + (p.stock || 0), 0);

    const handleEdit = (product) => {
        setFormData({
            name: product.name,
            price: product.price,
            stock: product.stock,
            type: product.type || 'module',
            imageUrl: product.imageUrl || '',
            category: product.category || 'monitors',
            specs: product.specs || { requires_slot_type: 'uart', power_draw: 0 },
            constraints: product.constraints || { incompatible_with: [] },
            visibility: 'part'
        });
        setEditingId(product.id);
        setIsModalOpen(true);
    };

    const confirmDelete = async () => {
        if (!deleteId) return;
        try {
            await deleteDoc(doc(db, 'products', deleteId));
            toast.success("Part Deleted");
            fetchData();
        } catch (error) {
            toast.error("Failed to delete part.");
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
                type: formData.type,
                category: formData.category,
                imageUrl: formData.imageUrl || 'https://via.placeholder.com/150',
                visibility: 'part',
                updatedAt: serverTimestamp(),
                specs: {
                    requires_slot_type: formData.specs?.requires_slot_type || 'uart',
                    power_draw: parseFloat(formData.specs?.power_draw || 0)
                },
                constraints: {
                    incompatible_with: formData.constraints?.incompatible_with || []
                }
            };

            if (editingId) {
                await updateDoc(doc(db, 'products', editingId), productData);
                toast.success("Part Updated!");
            } else {
                await addDoc(collection(db, 'products'), {
                    ...productData,
                    createdAt: serverTimestamp()
                });
                toast.success("Part Added!");
            }
            closeModal();
            fetchData();
        } catch (error) {
            toast.error("Failed to save part.");
        }
    };

    // Quick inline stock update
    const handleQuickStockUpdate = async (productId, newStock) => {
        try {
            await updateDoc(doc(db, 'products', productId), {
                stock: Number(newStock),
                updatedAt: serverTimestamp()
            });
            // Update local state immediately
            setAllProducts(prev => prev.map(p => p.id === productId ? { ...p, stock: Number(newStock) } : p));
        } catch (error) {
            toast.error("Failed to update stock.");
        }
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setEditingId(null);
        setFormData({
            name: '', price: '', stock: '', type: 'module', imageUrl: '',
            category: 'monitors',
            specs: { requires_slot_type: 'uart', power_draw: 0 },
            constraints: { incompatible_with: [] }, visibility: 'part'
        });
    };

    // Other parts for incompatibility selection
    const otherParts = allProducts.filter(p => p.id !== editingId);

    return (
        <div>
            {/* Stats Bar */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
                <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{ width: 40, height: 40, borderRadius: 8, background: '#f0fdf4', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <Plus size={20} color="#15803d" />
                    </div>
                    <div>
                        <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{inventoryItems.length}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Total Parts</div>
                    </div>
                </div>
                <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                    <div style={{ width: 40, height: 40, borderRadius: 8, background: '#e0f2fe', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                        <span style={{ fontWeight: 700, color: '#0369a1', fontSize: '0.85rem' }}>{totalStock}</span>
                    </div>
                    <div>
                        <div style={{ fontSize: '1.5rem', fontWeight: 700 }}>{totalStock}</div>
                        <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Total Stock</div>
                    </div>
                </div>
            </div>

            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h1 style={{ fontSize: '1.5rem', fontWeight: '700' }}>Inventory</h1>
                <button onClick={() => setIsModalOpen(true)} className="btn btn-primary" style={{ background: '#000' }}>
                    <Plus size={16} /> Add Part
                </button>
            </div>

            {/* Category Tabs + Search */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', gap: '1rem' }}>
                <div style={{ display: 'flex', gap: '0.25rem', background: '#f4f4f5', borderRadius: '8px', padding: '0.25rem' }}>
                    {[
                        { key: 'all', label: 'All' },
                        { key: 'module', label: 'Sensors' },
                        { key: 'accessory', label: 'Accessories' }
                    ].map(tab => (
                        <button
                            key={tab.key}
                            onClick={() => setActiveTab(tab.key)}
                            style={{
                                padding: '0.4rem 0.8rem', borderRadius: '6px', border: 'none',
                                background: activeTab === tab.key ? '#fff' : 'transparent',
                                boxShadow: activeTab === tab.key ? '0 1px 3px rgba(0,0,0,0.1)' : 'none',
                                fontWeight: activeTab === tab.key ? 600 : 400,
                                cursor: 'pointer', fontSize: '0.85rem',
                                transition: 'all 0.15s'
                            }}
                        >
                            {tab.label}
                        </button>
                    ))}
                </div>

                <div style={{ position: 'relative', flex: 1, maxWidth: '250px' }}>
                    <Search size={16} style={{ position: 'absolute', left: '0.6rem', top: '50%', transform: 'translateY(-50%)', color: '#a1a1aa' }} />
                    <input
                        className="input"
                        placeholder="Search parts..."
                        value={searchQuery}
                        onChange={e => setSearchQuery(e.target.value)}
                        style={{ paddingLeft: '2rem' }}
                    />
                </div>
            </div>

            {/* Table */}
            <div className="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Type</th>
                            <th>Slot</th>
                            <th>Power</th>
                            <th>Price</th>
                            <th>Stock</th>
                            <th style={{ textAlign: 'right' }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr><td colSpan="7" style={{ textAlign: 'center' }}>Loading...</td></tr>
                        ) : filtered.length === 0 ? (
                            <tr><td colSpan="7" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                                {searchQuery ? 'No parts match your search.' : 'No parts in inventory. Add your first sensor or accessory!'}
                            </td></tr>
                        ) : filtered.map(p => {
                            const typeInfo = TYPE_COLORS[p.type] || TYPE_COLORS.module;
                            return (
                                <tr key={p.id}>
                                    <td style={{ fontWeight: 500 }}>
                                        {p.name}
                                    </td>
                                    <td>
                                        <span style={{
                                            padding: '0.15rem 0.5rem', borderRadius: '4px', fontSize: '0.75rem', fontWeight: 'bold',
                                            background: typeInfo.bg, color: typeInfo.color
                                        }}>
                                            {typeInfo.label}
                                        </span>
                                    </td>
                                    <td style={{ fontSize: '0.8rem', color: '#666', textTransform: 'uppercase' }}>
                                        {p.specs?.requires_slot_type || '—'}
                                    </td>
                                    <td style={{ fontSize: '0.8rem', color: '#666' }}>
                                        {p.specs?.power_draw ? `${p.specs.power_draw}W` : '—'}
                                    </td>
                                    <td>₹{p.price?.toLocaleString()}</td>
                                    <td>
                                        <input
                                            type="number"
                                            value={p.stock || 0}
                                            onChange={e => handleQuickStockUpdate(p.id, e.target.value)}
                                            style={{
                                                width: '60px', padding: '0.2rem 0.4rem', borderRadius: '4px',
                                                border: '1px solid #e4e4e7',
                                                fontSize: '0.85rem', textAlign: 'center',
                                                background: '#fff'
                                            }}
                                        />
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
                            );
                        })}
                    </tbody>
                </table>
            </div>

            {/* Add/Edit Modal */}
            {isModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    zIndex: 1000
                }}>
                    <div className="card" style={{ width: '100%', maxWidth: '550px', background: '#fff', maxHeight: '90vh', overflowY: 'auto' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                            <h2 style={{ fontSize: '1.25rem', fontWeight: '600' }}>{editingId ? 'Edit Part' : 'Add New Part'}</h2>
                            <button onClick={closeModal} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                                <X size={20} />
                            </button>
                        </div>
                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>

                            {/* Basic Info */}
                            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '1rem' }}>
                                <div>
                                    <label className="label">Part Name</label>
                                    <input className="input" required value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} />
                                </div>
                                <div>
                                    <label className="label">Type</label>
                                    <select className="input" value={formData.type} onChange={e => setFormData({ ...formData, type: e.target.value })}>
                                        <option value="module">Sensor / Module</option>
                                        <option value="accessory">Accessory</option>
                                    </select>
                                </div>
                            </div>

                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                <div>
                                    <label className="label">Price (₹)</label>
                                    <input type="number" className="input" required value={formData.price} onChange={e => setFormData({ ...formData, price: e.target.value })} />
                                </div>
                                <div>
                                    <label className="label">Stock</label>
                                    <input type="number" className="input" required value={formData.stock} onChange={e => setFormData({ ...formData, stock: e.target.value })} />
                                </div>
                            </div>

                            <hr style={{ borderColor: 'var(--border)' }} />

                            {/* Hardware Specs */}
                            <div style={{ background: '#f9f9f9', padding: '1rem', borderRadius: '8px' }}>
                                <label className="label" style={{ fontWeight: 'bold' }}>Hardware Compatibility</label>

                                <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem' }}>
                                    <div style={{ flex: 1 }}>
                                        <label className="label" style={{ fontSize: '0.75rem' }}>Requires Slot Type</label>
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
                                        <label className="label" style={{ fontSize: '0.75rem' }}>Power Draw (Watts)</label>
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
                                    <label className="label" style={{ fontSize: '0.75rem' }}>Incompatible With</label>
                                    <div style={{ maxHeight: '100px', overflowY: 'auto', border: '1px solid #ddd', padding: '0.5rem', borderRadius: '4px', background: '#fff' }}>
                                        {otherParts.filter(p => p.type !== 'base').length > 0 ? otherParts.filter(p => p.type !== 'base').map(p => (
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
                                        )) : (
                                            <span style={{ fontSize: '0.75rem', color: '#999' }}>No other parts to select.</span>
                                        )}
                                    </div>
                                </div>
                            </div>

                            {/* Image */}
                            <div>
                                <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Part Image</label>
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
                                                const url = URL.createObjectURL(file);
                                                setFormData({ ...formData, imageUrl: url, imageFile: file });
                                            }
                                        }}
                                        style={{ fontSize: '0.875rem' }}
                                    />
                                </div>
                            </div>

                            <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem', background: '#000' }}>
                                {editingId ? 'Update Part' : 'Save Part'}
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
                        <h2 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '0.5rem' }}>Delete Part?</h2>
                        <p style={{ color: '#666', marginBottom: '1.5rem' }}>
                            This part may be referenced by products. Deleting it may break configurations.
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

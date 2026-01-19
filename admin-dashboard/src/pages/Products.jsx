import { useEffect, useState } from 'react';
import { collection, getDocs, addDoc, updateDoc, deleteDoc, doc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Plus, X, Pencil, Trash2 } from 'lucide-react';

export default function Products() {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingId, setEditingId] = useState(null);

    // Form State
    const [formData, setFormData] = useState({
        name: '', price: '', stock: '', category: 'monitors'
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
            category: product.category
        });
        setEditingId(product.id);
        setIsModalOpen(true);
    };

    const handleDelete = async (id) => {
        if (!window.confirm("Are you sure you want to delete this product?")) return;
        try {
            await deleteDoc(doc(db, 'products', id));
            fetchData();
        } catch (error) {
            console.error("Error deleting product:", error);
            alert("Failed to delete product");
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            if (editingId) {
                // Update
                await updateDoc(doc(db, 'products', editingId), {
                    name: formData.name,
                    price: parseFloat(formData.price),
                    stock: parseInt(formData.stock),
                    category: formData.category
                });
                alert("Product Updated!");
            } else {
                // Create
                await addDoc(collection(db, 'products'), {
                    name: formData.name,
                    price: parseFloat(formData.price),
                    stock: parseInt(formData.stock),
                    category: formData.category,
                    createdAt: serverTimestamp(),
                    imageUrl: 'https://via.placeholder.com/150'
                });
                alert("Product Added!");
            }
            closeModal();
            fetchData();
        } catch (error) {
            console.error("Error saving product:", error);
            alert("Failed to save product");
        }
    };

    const closeModal = () => {
        setIsModalOpen(false);
        setEditingId(null);
        setFormData({ name: '', price: '', stock: '', category: 'monitors' });
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
                            <th>Category</th>
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
                                <td>{p.category}</td>
                                <td style={{ textAlign: 'right' }}>
                                    <button onClick={() => handleEdit(p)} className="btn btn-ghost" style={{ padding: '0.25rem 0.5rem', marginRight: '0.5rem' }}>
                                        <Pencil size={16} />
                                    </button>
                                    <button onClick={() => handleDelete(p.id)} className="btn btn-ghost" style={{ padding: '0.25rem 0.5rem', color: 'var(--danger)' }}>
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
                    <div className="card" style={{ width: '100%', maxWidth: '400px', background: '#fff' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                            <h2 style={{ fontSize: '1.25rem', fontWeight: '600' }}>{editingId ? 'Edit Product' : 'Add New Product'}</h2>
                            <button onClick={closeModal} style={{ background: 'none', border: 'none', cursor: 'pointer' }}>
                                <X size={20} />
                            </button>
                        </div>
                        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                            <div>
                                <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Name</label>
                                <input className="input" required value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} />
                            </div>
                            <div style={{ display: 'flex', gap: '1rem' }}>
                                <div style={{ flex: 1 }}>
                                    <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Price (₹)</label>
                                    <input type="number" className="input" required value={formData.price} onChange={e => setFormData({ ...formData, price: e.target.value })} />
                                </div>
                                <div style={{ flex: 1 }}>
                                    <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Stock</label>
                                    <input type="number" className="input" required value={formData.stock} onChange={e => setFormData({ ...formData, stock: e.target.value })} />
                                </div>
                            </div>
                            <div>
                                <label style={{ display: 'block', fontSize: '0.875rem', marginBottom: '0.25rem' }}>Category</label>
                                <select className="input" value={formData.category} onChange={e => setFormData({ ...formData, category: e.target.value })}>
                                    <option value="monitors">Monitors</option>
                                    <option value="filters">Filters</option>
                                    <option value="masks">Masks</option>
                                    <option value="plants">Plants</option>
                                </select>
                            </div>
                            <button type="submit" className="btn btn-primary" style={{ marginTop: '0.5rem', background: '#000' }}>
                                {editingId ? 'Update Product' : 'Save Product'}
                            </button>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

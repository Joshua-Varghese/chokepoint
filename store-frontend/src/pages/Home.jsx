import { useEffect, useState } from 'react';
import { collection, getDocs, query, where } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Link } from 'react-router-dom';
import { ArrowRight, Cpu, Wifi } from 'lucide-react';

export default function Home() {
    const [bases, setBases] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        async function fetchBases() {
            try {
                const q = query(collection(db, 'products'), where('type', '==', 'base'));
                const snap = await getDocs(q);
                setBases(snap.docs.map(d => ({ id: d.id, ...d.data() })));
            } catch (e) {
                console.error(e);
            } finally {
                setLoading(false);
            }
        }
        fetchBases();
    }, []);

    return (
        <div>
            {/* Hero */}
            <section className="bg-black text-white py-20 px-6 text-center">
                <div className="max-w-4xl mx-auto">
                    <h1 className="text-5xl md:text-7xl font-bold mb-6 tracking-tight">
                        Build Your System.
                    </h1>
                    <p className="text-xl text-gray-400 mb-8 max-w-2xl mx-auto">
                        Professional grade IoT monitoring. Start with a core unit and customize it with the sensors you need.
                    </p>
                </div>
            </section>

            {/* Product Grid */}
            <section className="max-w-7xl mx-auto px-6 py-20">
                <h2 className="text-2xl font-bold mb-8">Select a Base Unit</h2>

                {loading ? (
                    <div>Loading...</div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                        {bases.map(product => (
                            <div key={product.id} className="border border-gray-200 rounded-2xl overflow-hidden hover:shadow-xl transition-shadow bg-white">
                                <div className="aspect-[4/3] bg-gray-100 flex items-center justify-center relative group">
                                    <img
                                        src={product.imageUrl}
                                        alt={product.name}
                                        className="w-full h-full object-cover mix-blend-multiply opacity-90 group-hover:scale-105 transition-transform duration-500"
                                    />
                                </div>
                                <div className="p-6">
                                    <h3 className="text-xl font-bold mb-2">{product.name}</h3>
                                    <p className="text-gray-500 text-sm mb-4 min-h-[40px]">{product.description}</p>

                                    {/* Specs Mini */}
                                    <div className="flex gap-4 mb-6 text-xs font-medium text-gray-600">
                                        <div className="flex items-center gap-1 bg-gray-100 px-2 py-1 rounded">
                                            <Cpu size={14} /> {product.specs?.cpu || 'ESP32'}
                                        </div>
                                        <div className="flex items-center gap-1 bg-gray-100 px-2 py-1 rounded">
                                            <Wifi size={14} /> {product.specs?.connectivity?.[0] || 'WiFi'}
                                        </div>
                                    </div>

                                    <div className="flex items-center justify-between mt-auto">
                                        <div>
                                            <span className="text-xs text-gray-500 uppercase font-bold">Starting at</span>
                                            <div className="text-xl font-bold">₹{product.basePrice.toLocaleString()}</div>
                                        </div>
                                        <Link
                                            to={`/configure/${product.id}`}
                                            className="bg-black text-white px-6 py-2 rounded-lg font-medium hover:bg-gray-800 transition-colors flex items-center gap-2"
                                        >
                                            Configure <ArrowRight size={16} />
                                        </Link>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>
        </div>
    );
}

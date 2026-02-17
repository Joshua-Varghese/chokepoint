import { useEffect, useState } from 'react';
import { collection, getDocs, query, where } from 'firebase/firestore';
import { db } from '../firebase-config';
import { Link } from 'react-router-dom';
import { MapContainer, TileLayer, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { ArrowRight, Wind, ShieldCheck, Activity } from 'lucide-react';

// Fix Leaflet marker icons if needed (often an issue with Vite/Webpack)
// For this simple map, we are just using tiles, so might not need markers.

export default function Home() {
    const [platforms, setPlatforms] = useState([]);
    const [loading, setLoading] = useState(true);

    // Map State
    const [activeCity, setActiveCity] = useState({ name: 'Beijing', lat: 39.9042, lng: 116.4074, aqi: 152, level: 'Unhealthy' });

    const CITIES = [
        { name: 'Beijing', lat: 39.9042, lng: 116.4074, aqi: 152, level: 'Unhealthy' },
        { name: 'New York', lat: 40.7128, lng: -74.0060, aqi: 45, level: 'Good' },
        { name: 'London', lat: 51.5074, lng: -0.1278, aqi: 68, level: 'Moderate' },
        { name: 'New Delhi', lat: 28.6139, lng: 77.2090, aqi: 305, level: 'Hazardous' },
        { name: 'Tokyo', lat: 35.6762, lng: 139.6503, aqi: 42, level: 'Good' }
    ];

    // Leaflet Helper to fly to coordinates
    function MapController({ center }) {
        const map = useMap();
        useEffect(() => {
            map.setView(center, 10, { animate: true, duration: 2 });
        }, [center, map]);
        return null;
    }

    useEffect(() => {
        async function fetchPlatforms() {
            try {
                // Fetch only 'base' products (Platforms)
                const q = query(collection(db, 'products'), where('type', '==', 'base'));
                const snap = await getDocs(q);
                setPlatforms(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
            } catch (error) {
                console.error("Error fetching platforms:", error);
            } finally {
                setLoading(false);
            }
        }
        fetchPlatforms();
    }, []);

    return (
        <div className="flex flex-col min-h-screen">

            {/* 1. HERO SECTION */}
            <section className="relative h-[80vh] flex items-center justify-center bg-black text-white overflow-hidden">
                {/* Background Video/Image Placeholder */}
                <div className="absolute inset-0 z-0 opacity-40">
                    <img
                        src="https://images.unsplash.com/photo-1550751827-4bd374c3f58b"
                        alt="Cyberpunk City"
                        className="w-full h-full object-cover"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-black" />
                </div>

                <div className="relative z-10 text-center px-4 max-w-4xl mx-auto">
                    {/* Badge Removed */}

                    <h1 className="text-5xl md:text-7xl font-bold tracking-tighter mb-6 bg-clip-text text-transparent bg-gradient-to-b from-white to-gray-400">
                        Breathe The Future.
                    </h1>
                    <p className="text-xl md:text-2xl text-gray-400 mb-10 max-w-2xl mx-auto leading-relaxed">
                        Modular environmental defense systems. <br />
                        Monitor. Analyze. Survive.
                    </p>
                    <div className="flex flex-col sm:flex-row gap-4 justify-center">
                        <Link
                            to="/configure/first" // Checks first available or scroll to grid
                            onClick={(e) => {
                                e.preventDefault();
                                document.getElementById('products').scrollIntoView({ behavior: 'smooth' });
                            }}
                            className="px-8 py-4 bg-white text-black font-bold rounded-lg hover:bg-gray-200 transition-all flex items-center justify-center gap-2 group"
                        >
                            Configure ChokeUnit
                            <ArrowRight className="group-hover:translate-x-1 transition-transform" />
                        </Link>
                        <a
                            href="#map"
                            className="px-8 py-4 bg-white/10 backdrop-blur-md border border-white/20 text-white font-bold rounded-lg hover:bg-white/20 transition-all"
                        >
                            View Live Data
                        </a>
                    </div>
                </div>
            </section>

            {/* 2. LIVE DATA MAP (WAQI) */}
            <section id="map" className="py-24 bg-zinc-900 border-y border-zinc-800">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="flex flex-col md:flex-row justify-between items-end mb-12 gap-6">
                        <div>
                            <h2 className="text-3xl font-bold text-white mb-2 flex items-center gap-3">
                                <Activity className="text-blue-500" />
                                Global Threat Level
                            </h2>
                            <p className="text-gray-400">Real-time Air Quality Index (AQI) monitoring network.</p>
                        </div>

                        {/* City Selector */}
                        <div className="flex gap-2 overflow-x-auto pb-2 md:pb-0 w-full md:w-auto">
                            {CITIES.map(city => (
                                <button
                                    key={city.name}
                                    onClick={() => setActiveCity(city)}
                                    className={`px-4 py-2 rounded-full text-sm font-medium transition-all whitespace-nowrap ${activeCity.name === city.name
                                        ? 'bg-blue-600 text-white'
                                        : 'bg-zinc-800 text-gray-400 hover:bg-zinc-700'
                                        }`}
                                >
                                    {city.name}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="h-[500px] w-full rounded-2xl overflow-hidden border border-zinc-700 shadow-2xl relative">
                        {/* Leaflet Map */}
                        <MapContainer center={[activeCity.lat, activeCity.lng]} zoom={10} scrollWheelZoom={false} style={{ height: '100%', width: '100%', background: '#1a1a1a' }}>
                            <MapController center={[activeCity.lat, activeCity.lng]} />
                            {/* Dark Base Map */}
                            <TileLayer
                                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                            />
                            {/* WAQI Overlay */}
                            <TileLayer
                                attribution='Air Quality Tiles &copy; <a href="http://waqi.info">waqi.info</a>'
                                url="https://tiles.waqi.info/tiles/usepa-aqi/{z}/{x}/{y}.png?token=demo"
                            />
                        </MapContainer>

                        {/* Overlay Card */}
                        <div className="absolute top-6 right-6 z-[500] bg-black/80 backdrop-blur-md p-6 rounded-xl border border-zinc-700 w-64 shadow-2xl">
                            <h4 className="text-white font-bold mb-4 flex items-center justify-between">
                                Live Analysis
                                <span className="text-xs font-normal text-gray-400 animate-pulse">● LIVE</span>
                            </h4>
                            <div className="space-y-4">
                                <div>
                                    <div className="text-xs text-gray-500 uppercase tracking-wider mb-1">Location</div>
                                    <div className="text-xl text-white font-mono">{activeCity.name}</div>
                                </div>
                                <div className="grid grid-cols-2 gap-4">
                                    <div>
                                        <div className="text-xs text-gray-500 uppercase tracking-wider mb-1">AQI</div>
                                        <div className={`text-2xl font-bold font-mono ${activeCity.aqi > 150 ? 'text-red-500' :
                                            activeCity.aqi > 100 ? 'text-yellow-500' : 'text-green-500'
                                            }`}>
                                            {activeCity.aqi}
                                        </div>
                                    </div>
                                    <div>
                                        <div className="text-xs text-gray-500 uppercase tracking-wider mb-1">Status</div>
                                        <div className="text-sm text-gray-300 font-medium mt-1">{activeCity.level}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            {/* 3. FEATURED PRODUCTS GRID */}
            <section id="products" className="py-24 bg-gray-50">
                <div className="max-w-7xl mx-auto px-6">
                    <div className="text-center mb-16">
                        <h2 className="text-4xl font-bold text-gray-900 mb-4">Choose Your ChokeUnit</h2>
                        <p className="text-gray-600 max-w-2xl mx-auto">
                            Start with a base unit and customize it with industrial-grade sensors and power modules.
                        </p>
                    </div>

                    {loading ? (
                        <div className="text-center py-20">Loading Systems...</div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                            {platforms.map(platform => (
                                <div key={platform.id} className="group bg-white rounded-2xl border border-gray-100 overflow-hidden hover:shadow-xl transition-all hover:-translate-y-1">
                                    <div className="aspect-[4/3] bg-gray-100 relative overflow-hidden">
                                        <img
                                            src={platform.imageUrl || 'https://via.placeholder.com/400'}
                                            alt={platform.name}
                                            className="w-full h-full object-cover mix-blend-multiply group-hover:scale-105 transition-transform duration-500"
                                        />
                                        <div className="absolute top-4 right-4 bg-white/90 backdrop-blur-sm px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide">
                                            Base Unit
                                        </div>
                                    </div>
                                    <div className="p-8">
                                        <div className="flex justify-between items-start mb-4">
                                            <h3 className="text-xl font-bold text-gray-900">{platform.name}</h3>
                                            <span className="text-lg font-bold text-blue-600">₹{platform.price}</span>
                                        </div>
                                        <p className="text-gray-500 text-sm mb-6 line-clamp-2">
                                            {platform.description || "High-performance base station capable of supporting multiple sensor arrays."}
                                        </p>

                                        <div className="space-y-3 mb-8">
                                            {/* Feature Pills */}
                                            <div className="flex flex-wrap gap-2">
                                                {(platform.slots || []).slice(0, 3).map((slot, i) => (
                                                    <span key={i} className="text-xs bg-gray-50 border border-gray-200 px-2 py-1 rounded text-gray-600">
                                                        {slot.type.toUpperCase()} Slot
                                                    </span>
                                                ))}
                                                {(platform.slots || []).length > 3 && (
                                                    <span className="text-xs bg-gray-50 border border-gray-200 px-2 py-1 rounded text-gray-600">
                                                        +{(platform.slots || []).length - 3} more
                                                    </span>
                                                )}
                                            </div>
                                        </div>

                                        <Link
                                            to={`/configure/${platform.id}`}
                                            className="block w-full text-center bg-black text-white font-bold py-3 rounded-lg hover:bg-gray-800 transition-colors"
                                        >
                                            Configure Now
                                        </Link>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </section>

        </div>
    );
}

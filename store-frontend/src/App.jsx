import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { ShoppingCart, Package } from 'lucide-react';
import Home from './pages/Home';
import Configurator from './pages/Configurator';

function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50 text-gray-900 font-sans">
        {/* Navbar */}
        <nav className="bg-white border-b border-gray-200 sticky top-0 z-50">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
            <Link to="/" className="flex items-center gap-2 font-bold text-xl tracking-tight">
              <div className="w-8 h-8 bg-black text-white flex items-center justify-center rounded-lg">
                <Package size={20} />
              </div>
              Chokepoint Store
            </Link>

            <div className="flex items-center gap-4">
              <button className="relative p-2 hover:bg-gray-100 rounded-full transition-colors">
                <ShoppingCart size={24} />
                <span className="absolute top-0 right-0 bg-black text-white text-xs font-bold w-5 h-5 flex items-center justify-center rounded-full">
                  0
                </span>
              </button>
            </div>
          </div>
        </nav>

        {/* content */}
        <main>
          <main>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/configure/:id" element={<Configurator />} />
            </Routes>
          </main>
        </main>

        {/* Footer */}
        <footer className="bg-black text-white py-12 mt-20">
          <div className="max-w-7xl mx-auto px-6 text-center">
            <p className="opacity-50 text-sm">© 2026 Chokepoint Systems. Build Your Own.</p>
          </div>
        </footer>
      </div>
    </BrowserRouter>
  );
}

export default App;

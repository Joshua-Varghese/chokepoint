import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { ShoppingCart, Package } from 'lucide-react';
import Home from './pages/Home';
import Configurator from './pages/Configurator';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import { CartProvider, useCart } from './context/CartContext';
import { useAuth } from './context/AuthContext';
import { Toaster } from 'react-hot-toast';
import AuthModal from './components/AuthModal';
import MyOrders from './pages/MyOrders';

function Navbar() {
  const { cartCount } = useCart();
  const { currentUser, logout } = useAuth();
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  return (
    <>
      <nav className="bg-white border-b border-gray-200 sticky top-0 z-[9999]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2 font-bold text-xl tracking-tight">
            <div className="w-8 h-8 bg-black text-white flex items-center justify-center rounded-lg">
              <Package size={20} />
            </div>
            Chokepoint Store
          </Link>
          <div className="flex items-center gap-4">

            {currentUser ? (
              <div className="flex items-center gap-4">
                <Link to="/orders" className="text-sm font-medium hover:text-black text-gray-600 transition-colors">
                  My Orders
                </Link>
                <div className="h-4 w-px bg-gray-300"></div>
                <button onClick={logout} className="text-sm font-medium hover:text-red-600 text-gray-600 transition-colors">
                  Log Out
                </button>
              </div>
            ) : (
              <button
                onClick={() => setIsAuthModalOpen(true)}
                className="text-sm font-medium bg-black text-white px-4 py-2 rounded-lg hover:bg-gray-900 transition-colors"
              >
                Log In
              </button>
            )}

            <Link to="/cart" className="relative p-2 hover:bg-gray-100 rounded-full transition-colors ml-2">
              <ShoppingCart size={24} />
              {cartCount > 0 && (
                <span className="absolute top-0 right-0 bg-black text-white text-xs font-bold w-5 h-5 flex items-center justify-center rounded-full">
                  {cartCount}
                </span>
              )}
            </Link>
          </div>
        </div>
      </nav>

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
      />
    </>
  );
}

function App() {
  return (
    <CartProvider>
      <Toaster position="bottom-right" />
      <BrowserRouter>
        <div className="min-h-screen bg-gray-50 text-gray-900 font-sans">
          <Navbar />
          <main>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/configure/:id" element={<Configurator />} />
              <Route path="/cart" element={<Cart />} />
              <Route path="/checkout" element={<Checkout />} />
              <Route path="/orders" element={<MyOrders />} />
            </Routes>
          </main>
          <footer className="bg-black text-white py-12 mt-20">
            <div className="max-w-7xl mx-auto px-6 text-center">
              <p className="opacity-50 text-sm">© 2026 Chokepoint Systems. Build Your Own.</p>
            </div>
          </footer>
        </div>
      </BrowserRouter>
    </CartProvider>
  );
}

export default App;

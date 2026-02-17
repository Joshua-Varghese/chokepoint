import { createContext, useContext, useState, useEffect } from 'react';

const CartContext = createContext();

export function useCart() {
    return useContext(CartContext);
}

export function CartProvider({ children }) {
    const [cart, setCart] = useState(() => {
        // Load from local storage on init
        const saved = localStorage.getItem('chokepoint_cart');
        return saved ? JSON.parse(saved) : [];
    });

    useEffect(() => {
        // Save to local storage on change
        localStorage.setItem('chokepoint_cart', JSON.stringify(cart));
    }, [cart]);

    const addToCart = (product) => {
        setCart(prev => [...prev, product]);
    };

    const removeFromCart = (index) => {
        setCart(prev => prev.filter((_, i) => i !== index));
    };

    const clearCart = () => {
        setCart([]);
    };

    const cartTotal = cart.reduce((total, item) => total + (item.totalPrice || 0), 0);
    const cartCount = cart.length;

    const value = {
        cart,
        addToCart,
        removeFromCart,
        clearCart,
        cartTotal,
        cartCount
    };

    return (
        <CartContext.Provider value={value}>
            {children}
        </CartContext.Provider>
    );
}

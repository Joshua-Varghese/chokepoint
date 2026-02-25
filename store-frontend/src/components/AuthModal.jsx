import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { X } from 'lucide-react';
import toast from 'react-hot-toast';

export default function AuthModal({ isOpen, onClose }) {
    const [isLogin, setIsLogin] = useState(true);
    const [isResetMode, setIsResetMode] = useState(false);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [loading, setLoading] = useState(false);

    const { login, signup, resetPassword } = useAuth();

    if (!isOpen) return null;

    const getFriendlyErrorMessage = (error) => {
        const code = error.code || '';
        if (code === 'auth/invalid-credential') return 'Incorrect email or password.';
        if (code === 'auth/user-not-found') return 'No account found with this email.';
        if (code === 'auth/wrong-password') return 'Incorrect password.';
        if (code === 'auth/email-already-in-use') return 'An account with this email already exists.';
        if (code === 'auth/weak-password') return 'Password should be at least 6 characters.';
        if (code === 'auth/invalid-email') return 'Please enter a valid email address.';
        if (code.includes('password-does-not-meet-requirements')) return 'Password does not meet requirements. Please include an uppercase letter, lowercase letter, number, and special character.';

        return error.message || 'Authentication failed. Please try again.';
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            if (isResetMode) {
                if (!email.trim()) throw new Error("Please enter your email to reset the password.");
                await resetPassword(email);
                toast.success('Password reset email sent! Check your inbox.');
                setIsResetMode(false);
            } else if (isLogin) {
                await login(email, password);
                toast.success('Successfully logged in!');
                onClose();
            } else {
                if (!name.trim()) throw new Error("Name is required");
                await signup(email, password, name);
                toast.success('Account created successfully!');
                onClose();
            }
        } catch (error) {
            console.error("Auth error:", error);
            toast.error(getFriendlyErrorMessage(error));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 z-[10000] flex items-center justify-center bg-black/50 p-4">
            <div className="bg-white rounded-xl shadow-xl max-w-md w-full overflow-hidden">
                <div className="flex justify-between items-center p-4 border-b border-gray-100">
                    <h2 className="text-xl font-bold">
                        {isResetMode ? 'Reset Password' : (isLogin ? 'Welcome Back' : 'Create Account')}
                    </h2>
                    <button onClick={() => {
                        setIsResetMode(false);
                        onClose();
                    }} className="text-gray-400 hover:text-black transition-colors">
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-6 space-y-4">
                    {!isLogin && !isResetMode && (
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
                            <input
                                type="text"
                                required
                                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-black outline-none"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                            />
                        </div>
                    )}

                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                        <input
                            type="email"
                            required
                            className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-black outline-none"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                        />
                    </div>

                    {!isResetMode && (
                        <div>
                            <div className="flex justify-between items-center mb-1">
                                <label className="block text-sm font-medium text-gray-700">Password</label>
                                {isLogin && (
                                    <button
                                        type="button"
                                        onClick={() => setIsResetMode(true)}
                                        className="text-xs text-gray-500 hover:text-black hover:underline"
                                    >
                                        Forgot password?
                                    </button>
                                )}
                            </div>
                            <input
                                type="password"
                                required
                                minLength={6}
                                className="w-full p-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-black outline-none"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full bg-black text-white py-3 rounded-lg font-bold hover:bg-gray-900 transition-colors disabled:opacity-50 mt-4"
                    >
                        {loading ? 'Processing...' : (isResetMode ? 'Send Reset Link' : (isLogin ? 'Log In' : 'Sign Up'))}
                    </button>

                    <div className="text-center mt-4">
                        {isResetMode ? (
                            <button
                                type="button"
                                onClick={() => setIsResetMode(false)}
                                className="text-sm text-gray-600 hover:text-black underline"
                            >
                                Back to Log In
                            </button>
                        ) : (
                            <button
                                type="button"
                                onClick={() => setIsLogin(!isLogin)}
                                className="text-sm text-gray-600 hover:text-black underline"
                            >
                                {isLogin ? "Don't have an account? Sign Up" : "Already have an account? Log In"}
                            </button>
                        )}
                    </div>
                </form>
            </div>
        </div>
    );
}

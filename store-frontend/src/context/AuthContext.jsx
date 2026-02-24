import React, { createContext, useContext, useEffect, useState } from 'react';
import {
    createUserWithEmailAndPassword,
    signInWithEmailAndPassword,
    signOut,
    onAuthStateChanged,
    updateProfile,
    sendPasswordResetEmail
} from 'firebase/auth';
import { auth, db } from '../firebase-config';
import { doc, setDoc, getDoc, serverTimestamp } from 'firebase/firestore';

const AuthContext = createContext();

export function useAuth() {
    return useContext(AuthContext);
}

export function AuthProvider({ children }) {
    const [currentUser, setCurrentUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Sign up function
    async function signup(email, password, displayName) {
        const userCredential = await createUserWithEmailAndPassword(auth, email, password);
        await updateProfile(userCredential.user, { displayName });

        // Also create a user document in Firestore to match Android app behavior
        const userRef = doc(db, 'users', userCredential.user.uid);
        const docSnap = await getDoc(userRef);
        if (!docSnap.exists()) {
            await setDoc(userRef, {
                email: email,
                name: displayName || `User ${userCredential.user.uid.substring(0, 5)}`,
                id: userCredential.user.uid,
                createdAt: serverTimestamp()
            });
        }
        return userCredential;
    }

    // Login function
    function login(email, password) {
        return signInWithEmailAndPassword(auth, email, password);
    }

    // Logout function
    function logout() {
        return signOut(auth);
    }

    // Password reset function
    function resetPassword(email) {
        return sendPasswordResetEmail(auth, email);
    }

    // Setup listener
    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, async (user) => {
            setCurrentUser(user);
            setLoading(false);
        });

        return unsubscribe;
    }, []);

    const value = {
        currentUser,
        login,
        signup,
        logout,
        resetPassword
    };

    return (
        <AuthContext.Provider value={value}>
            {!loading && children}
        </AuthContext.Provider>
    );
}

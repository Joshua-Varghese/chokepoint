import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getFirestore } from "firebase/firestore";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
    apiKey: "AIzaSyCBUbYW2keKbZVRriiXVk8z7-Liy42uiec",
    authDomain: "chokepoint-android.firebaseapp.com",
    projectId: "chokepoint-android",
    storageBucket: "chokepoint-android.firebasestorage.app",
    messagingSenderId: "164679848850",
    appId: "1:164679848850:web:e842d055e7490c87d6a74b",
    measurementId: "G-RW095JPN38"
};

const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);
export const db = getFirestore(app);
export const auth = getAuth(app);

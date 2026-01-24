import { initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
    apiKey: "AIzaSyDyoKR0xLGDOUU2cNhphu95bjS-0OKWwdM",
    authDomain: "chokepoint-android.firebaseapp.com",
    projectId: "chokepoint-android",
    storageBucket: "chokepoint-android.firebasestorage.app",
    messagingSenderId: "164679848850",
    appId: "1:164679848850:web:86de06076356783d254580"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const db = getFirestore(app);

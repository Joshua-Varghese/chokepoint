import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js";

const firebaseConfig = {
    apiKey: "AIzaSyDyoKR0xLGDOUU2cNhphu95bjS-0OKWwdM",
    authDomain: "chokepoint-android.firebaseapp.com",
    projectId: "chokepoint-android",
    storageBucket: "chokepoint-android.firebasestorage.app",
    messagingSenderId: "164679848850",
    // appId: "1:164679848850:web:..." // Optional for Auth/Firestore usually
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

export { auth, db };

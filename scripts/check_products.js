console.log('Fetching products...');
const admin = require('firebase-admin');
const serviceAccount = require('./service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}
const db = admin.firestore();

async function run() {
    const snapshot = await db.collection('products').get();
    snapshot.forEach(doc => {
        console.log(doc.id, '=>', doc.data());
    });
    process.exit(0);
}
run();

const admin = require('firebase-admin');
const mqtt = require('mqtt');
const WebSocket = require('ws');
const serviceAccount = require('./service-account.json');

// Initialize Firebase Admin
if (!admin.apps.length) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
}
const db = admin.firestore();

// MQTT Credentials
const MQTT_URL = 'mqtt://puffin.rmq2.cloudamqp.com:1883'; // Use standard TCP MQTT instead of WebSockets
const MQTT_USERNAME = 'lztdkevt:lztdkevt';
const MQTT_PASSWORD = 'vG7j8gUsE9yG5Li7Mb8qaAcpExZLgdUS';

console.log("Starting MQTT-to-Firestore Bridge Node.js Service...");

const client = mqtt.connect(MQTT_URL, {
    username: MQTT_USERNAME,
    password: MQTT_PASSWORD
});

client.on('connect', () => {
    console.log("Connected to CloudAMQP Broker via standard TCP.");
    client.subscribe(['chokepoint/devices/+/data', 'chokepoint/devices/+/res'], (err) => {
        if (!err) {
            console.log("Subscribed to all device telemetry and response streams.");
            console.log("Listening for device pings and commands...");
        } else {
            console.error("Subscription error:", err);
        }
    });
});

// --- Local WebSocket Proxy for React Dashboard ---
const wss = new WebSocket.Server({ port: 8080 });

wss.on('connection', (ws) => {
    let subscribedDeviceId = null;
    console.log("[WS Dashboard] Connected to local proxy.");

    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message.toString());
            if (data.action === 'subscribe') {
                subscribedDeviceId = data.deviceId;
                console.log(`[WS Proxy] Dashboard subscribed to responses for: ${subscribedDeviceId}`);
            } else if (data.action === 'publish' && data.deviceId) {
                console.log(`[WS Proxy] Forwarding CMD to: ${data.deviceId} ->`, data.payload);
                client.publish(`chokepoint/devices/${data.deviceId}/cmd`, JSON.stringify(data.payload));
            }
        } catch (e) {
            console.error("[WS Proxy] Parse error from React client", e.message);
        }
    });

    // Forward matching MQTT responses to this WebSocket client
    const onMqttProxyMessage = (topic, msgBuffer) => {
        if (subscribedDeviceId && topic === `chokepoint/devices/${subscribedDeviceId}/res`) {
            if (ws.readyState === WebSocket.OPEN) {
                ws.send(msgBuffer.toString());
            }
        }
    };

    client.on('message', onMqttProxyMessage);

    ws.on('close', () => {
        console.log("[WS Dashboard] Disconnected.");
        client.removeListener('message', onMqttProxyMessage);
    });
});


client.on('message', async (topic, message) => {
    try {
        const payload = JSON.parse(message.toString());
        const deviceId = payload.device_id;

        if (!deviceId) return;

        console.log(`[${new Date().toLocaleTimeString()}] Ping caught from device: ${deviceId}`);

        // 1. Update the 'lastSeen' heartbeat on the main device document
        const deviceRef = db.collection('devices').doc(deviceId);
        await deviceRef.set({
            lastSeen: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });

        // 2. Save the reading to the subcollection so the Dashboard has historical data
        const readingData = {
            co2: payload.co2 || 0,
            nh3: payload.nh3 || 0,
            smoke: payload.smoke || 0,
            gasRaw: payload.gas_raw || Math.floor(payload.co2 || 0),
            timestamp: payload.timestamp || Math.floor(Date.now() / 1000),
            deviceId: deviceId,
            airQuality: (payload.smoke > 0.5) ? "Hazardous" : ((payload.co2 > 1000) ? "Poor" : "Good")
        };

        await deviceRef.collection('readings').add(readingData);

    } catch (e) {
        console.error("Error processing MQTT message:", e.message);
    }
});

client.on('error', (err) => {
    console.error("MQTT Client Error:", err);
});

const mqtt = require('mqtt');
const fs = require('fs');
const path = require('path');

// --- CONFIGURATION ---
// Read secrets from local.properties
const localPropsPath = path.join(__dirname, '..', 'local.properties');
let localProps = {};

try {
    const props = fs.readFileSync(localPropsPath, 'utf8');
    props.split('\n').forEach(line => {
        const [key, value] = line.split('=');
        if (key && value) {
            localProps[key.trim()] = value.trim();
        }
    });
} catch (err) {
    console.error('Failed to read local.properties:', err);
    process.exit(1);
}

const BROKER_URL = localProps['mqtt_broker_url']?.replace('tcp://', 'mqtt://');
const USERNAME = localProps['mqtt_username'];
const PASSWORD = localProps['mqtt_password'];
const DEVICE_ID = "simulated_device_001";
const TOPIC = `chokepoint/devices/${DEVICE_ID}/data`;

if (!BROKER_URL || !USERNAME || !PASSWORD) {
    console.error('Missing MQTT credentials in local.properties');
    process.exit(1);
}

console.log('Connecting to MQTT Broker...');

const client = mqtt.connect(BROKER_URL, {
    username: USERNAME,
    password: PASSWORD
});

client.on('connect', () => {
    console.log('✅ Connected! Starting simulation...');

    // Publish data every 5 seconds (matched with Android rate limit)
    setInterval(() => {
        const data = generateFakeData();
        const payload = JSON.stringify(data);

        client.publish(TOPIC, payload, { qos: 0 }, (err) => {
            if (err) {
                console.error('❌ Publish Failed:', err);
            } else {
                console.log(`📡 Sent: ${payload}`);
            }
        });
    }, 5000);
});

client.on('error', (err) => {
    console.error('❌ Connection Error:', err);
    client.end();
});

function generateFakeData() {
    // FORCE BAD DATA FOR TESTING
    // 100% chance of spike
    const isSpike = true;

    return {
        device_id: "simulated_device_001", // Match a known ID or provisioned ID
        co2: isSpike ? Math.floor(1600 + Math.random() * 1000) : Math.floor(400 + Math.random() * 800),
        nh3: parseFloat((0.1 + Math.random() * 2.0).toFixed(2)),
        smoke: isSpike ? parseFloat((0.6 + Math.random()).toFixed(2)) : 0.0, // Smoke > 0.5 triggers alert
        gas_raw: isSpike ? Math.floor(2600 + Math.random() * 1000) : Math.floor(100 + Math.random() * 1000),
        air_quality: isSpike ? "Hazardous" : "Good"
    };
}

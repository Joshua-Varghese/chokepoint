FROM node:20-alpine

WORKDIR /app

# Copy all scripts (package.json, package-lock.json, mqtt_bridge.js, etc.)
COPY scripts/ .

# Install production dependencies only
RUN npm install --omit=dev

# Expose WebSocket proxy port
EXPOSE 8080

# Run the MQTT bridge
CMD ["node", "mqtt_bridge.js"]

FROM node:20-alpine

WORKDIR /app

# Copy package files first for layer caching
COPY scripts/package*.json ./

# Install production dependencies only
RUN npm ci --omit=dev

# Copy the rest of the scripts (service-account.json injected at build time by CI)
COPY scripts/ .

# Expose WebSocket proxy port
EXPOSE 8080

# Run the MQTT bridge
CMD ["node", "mqtt_bridge.js"]

# Debian/glibc (not Alpine/musl): fewer WASM/TLS edge cases; optional native build tools if SDK supports your distro.
FROM node:20-bookworm-slim

WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends python3 make g++ \
  && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
RUN npm ci --omit=dev

COPY . .

EXPOSE 8080

ENV NODE_ENV=production

CMD ["node", "src/server.js"]

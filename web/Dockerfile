# --- build the Angular app ---
FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build

# --- serve it with Caddy (HTTPS, custom domains, reverse proxy to the API) ---
FROM caddy:2.10-alpine
COPY --from=build /src/dist/web/browser /srv
# The Caddyfile is mounted from infra/caddy/Caddyfile by docker-compose.prod.yml

# =============================================================================
# AI Software Engineering Assistant frontend — multi-stage image.
#
# Stage 1 builds the Vite production bundle with Node. Stage 2 serves the static
# SPA with nginx, which also proxies /api to the app service (see
# docker/frontend/nginx.conf). Runs as nginx's built-in non-root worker.
# =============================================================================

# --- build stage -------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies against the lockfile first for better layer caching.
COPY package.json package-lock.json* ./
RUN npm ci

# Build the production bundle.
COPY . .
RUN npm run build

# --- runtime stage -----------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# SPA + API-proxy config.
COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=5 \
    CMD wget -qO- http://localhost:8080/ >/dev/null 2>&1 || exit 1

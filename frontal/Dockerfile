# Multi-stage Dockerfile for SvelteKit (Vite) app - static export served by Nginx

# ===== Build stage =====
FROM node:20-alpine AS build

WORKDIR /app

ENV CI=true

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .

# Build to static assets; for SvelteKit, we rely on adapter-auto which produces client assets via Vite
RUN npm run build

# ===== Runtime stage =====
FROM nginx:1.27-alpine AS runtime

WORKDIR /usr/share/nginx/html

# Remove default Nginx static content
RUN rm -rf ./*

# Copy built static assets
COPY --from=build /app/dist ./

# Basic Nginx config for single-page app routing
COPY <<EOF /etc/nginx/conf.d/default.conf
server {
    listen 80;
    server_name _;

    root   /usr/share/nginx/html;
    index  index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~* \.(?:ico|css|js|gif|jpe?g|png|svg|woff2?)$ {
        expires 1y;
        access_log off;
        add_header Cache-Control "public, immutable";
    }
}
EOF

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 CMD wget -qO- http://localhost/ > /dev/null || exit 1



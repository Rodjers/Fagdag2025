# Multi-stage Dockerfile for SvelteKit (Vite) app using preview server

# ===== Build stage =====
FROM node:20-alpine AS build

WORKDIR /app

ENV CI=true

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .

# Build application (produces .svelte-kit/output)
RUN npm run build

# ===== Runtime stage (Node + vite preview) =====
FROM node:20-alpine AS runtime

WORKDIR /app

# Copy built output and dependencies from build stage
COPY --from=build /app/.svelte-kit ./.svelte-kit
COPY --from=build /app/node_modules ./node_modules
COPY package.json package-lock.json ./

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 CMD wget -qO- http://localhost:8080/ > /dev/null || exit 1

# Use vite preview to serve the production build
CMD ["npm", "run", "preview", "--", "--host", "0.0.0.0", "--port", "8080"]



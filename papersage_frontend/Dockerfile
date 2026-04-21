# What: Build stage using Node to compile the Vite production bundle.
# Why: Keeps build tooling out of final runtime image.
FROM node:22-alpine AS builder

# What: Set working directory for frontend build.
# Why: Ensures predictable paths for dependency install and build output.
WORKDIR /app

# What: Copy dependency manifests first.
# Why: Improves Docker layer caching for faster rebuilds.
COPY package.json package-lock.json ./

# What: Install exact dependency tree from lockfile.
# Why: Reproducible, CI-friendly installs.
RUN npm ci

# What: Copy frontend source files.
# Why: Required to build the Vite app.
COPY . .

# What: Optional build-time API base URL for Vite.
# Why: Vite injects env vars at build time.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# What: Build optimized static assets.
# Why: Produces production-ready files in /app/dist.
RUN npm run build

# What: Runtime stage using unprivileged Nginx.
# Why: Small, secure image for serving static assets.
FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

# What: Copy built static assets into Nginx web root.
# Why: Serves the Vite bundle without Node runtime.
COPY --from=builder /app/dist /usr/share/nginx/html

# What: Expose unprivileged Nginx port.
# Why: nginx-unprivileged listens on 8080 by default.
EXPOSE 8080

# What: Start Nginx in foreground.
# Why: Required container entrypoint behavior.
CMD ["nginx", "-g", "daemon off;"]
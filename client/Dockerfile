FROM oven/bun:1.2.10-alpine AS build-client

WORKDIR /app

ARG VITE_API=/api/v1
ARG VITE_WS_HOST=localhost:8080
ARG VITE_COMMIT_HASH=unknown

ENV VITE_API=${VITE_API}
ENV VITE_WS_HOST=${VITE_WS_HOST}
ENV VITE_COMMIT_HASH=${VITE_COMMIT_HASH}

COPY ./package.json ./bun.lockb ./
COPY . .

RUN bun install --prod
RUN bun run build

FROM nginx:alpine

ENV BACKEND_URL=http://server:8080
COPY --from=build-client /app/dist /usr/share/nginx/html

RUN printf "server {\n\
    listen 80;\n\
\n\
    location /api/ {\n\
        proxy_pass \${BACKEND_URL}/api/;\n\
        proxy_http_version 1.1;\n\
        proxy_set_header Upgrade \$http_upgrade;\n\
        proxy_set_header Connection 'upgrade';\n\
        proxy_set_header Host \$host;\n\
        proxy_set_header X-Real-IP \$remote_addr;\n\
    }\n\
\n\
    location / {\n\
        root /usr/share/nginx/html;\n\
        try_files \$uri \$uri/ /index.html;\n\
    }\n\
}\n\
\n\
server {\n\
    listen 443;\n\
    return 301 http://\$host:80\$request_uri;\n\
}" > /etc/nginx/conf.d/default.conf.template

CMD ["/bin/sh", "-c", "envsubst '$BACKEND_URL' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]

EXPOSE 80 443





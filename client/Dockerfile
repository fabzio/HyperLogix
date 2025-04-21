FROM oven/bun:1.2.10-alpine AS build-backend

WORKDIR /app

COPY ./package.json ./bun.lockb ./
COPY . .

RUN bun install --prod
RUN bun run build

FROM nginx:alpine

COPY --from=build-backend /app/dist /usr/share/nginx/html

RUN printf "server {\n\
    listen 80;\n\
\n\
    location /api/ {\n\
        proxy_pass http://backend:8080/api/;\n\
        proxy_set_header Host \$host;\n\
        proxy_set_header X-Real-IP \$remote_addr;\n\
    }\n\
\n\
    location / {\n\
        root /usr/share/nginx/html;\n\
        try_files \$uri \$uri/ /index.html;\n\
    }\n\
}" > /etc/nginx/conf.d/default.conf

EXPOSE 80





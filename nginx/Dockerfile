# ---- 第一階段：建置 Vue 前端 ----
# 用 Node 映像只做 build，最終映像不包含 Node 與 node_modules，體積小、攻擊面也小
FROM node:22-alpine AS frontend-build
WORKDIR /app
# 先只複製 package*.json 再安裝依賴：原始碼變動時可重用這一層的 Docker 快取
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 第二階段：Nginx 託管靜態檔 ----
FROM nginx:1.29-alpine
# 移除官方映像預設的 default.conf，改用我們的設定
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/conf.d/ /etc/nginx/conf.d/
COPY nginx/snippets/ /etc/nginx/snippets/
COPY --from=frontend-build /app/dist /usr/share/nginx/html
EXPOSE 80

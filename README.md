# SplitMate 多人分帳

朋友出遊、室友合租時記錄共同支出，自動計算「誰該付給誰多少錢」。

> 本專案同時是 Vue 3 與 Nginx 的練習專案：新增支出、收據上傳、即時通知刻意保留給練習者實作（階段 5 會提供 `PRACTICE.md`）。
> 完整的本機開發、VPS 部署與 HTTPS 步驟會在階段 5 補齊。

## 技術版本（2026-10 撰寫時的穩定版）

| 類別 | 套件 | 版本 |
| --- | --- | --- |
| 後端 | Java | 17 |
| | Spring Boot | 3.5.16 |
| | Flyway | 11.7.2（由 Spring Boot 管理） |
| | PostgreSQL JDBC | 42.7.11（由 Spring Boot 管理） |
| | Maven（Wrapper） | 3.9.16 |
| 前端 | Node.js | 22 LTS（>= 22.12） |
| | Vue | 3.5.43 |
| | Vite | 8.3.2 |
| | TypeScript | 5.9.3 |
| | vue-tsc | 3.3.11 |
| | Pinia | 4.0.3 |
| | Vue Router | 5.3.1 |
| | Axios | 1.20.0 |
| | Element Plus | 2.14.7 |
| 基礎設施 | Nginx | 1.29（nginx:1.29-alpine） |
| | PostgreSQL | 17（postgres:17-alpine） |

版本選擇說明：
- **TypeScript 刻意停在 5.9**：TypeScript 7（原生 Go 版）已發布，但 vue-tsc 仰賴 TypeScript 的 JS API 做 `.vue` 型別檢查，相容性尚待確認，因此先用 5.x。
- **Spring Boot 停在 3.x**：依需求使用 3.x 最新版；Spring Boot 4.x 已發布，升級需另行評估。
- **PostgreSQL 用 17 而非 18**：postgres:18 官方映像的資料目錄掛載路徑改為 `/var/lib/postgresql`，與網路上多數教學的 `/var/lib/postgresql/data` 不同，容易踩雷。

## 專案結構

```
SplitMate/
├── docker-compose.yml          # nginx + backend + postgres
├── .env.example                # 機敏設定範本（複製為 .env）
├── nginx/
│   ├── Dockerfile              # 多階段：Node 建置前端 → Nginx 託管
│   ├── conf.d/                 # http 層級設定（gzip）與 HTTP server
│   ├── snippets/               # HTTP / HTTPS 共用的 location 設定
│   └── https/                  # HTTPS（Let's Encrypt）設定範例
├── backend/                    # Spring Boot（controller / service / repository / dto / entity / config / exception）
└── frontend/                   # Vue 3 + Vite + TypeScript
```

## 快速啟動（Docker Compose）

```bash
cp .env.example .env        # 視需要修改密碼
docker compose up -d --build
```

- 首頁：http://localhost/
- 健康檢查：http://localhost/api/health → `{"code":"OK","message":"success","data":{"status":"OK"}}`

若本機 80 port 被占用，把 `.env` 的 `NGINX_HTTP_PORT` 改成 8000 等。

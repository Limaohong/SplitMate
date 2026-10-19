# SplitMate 多人分帳

朋友出遊、室友合租時記錄共同支出，自動計算「誰該付給誰多少錢」。

> 本專案同時是 **Vue 3 與 Nginx 的練習專案**：新增支出、收據上傳、即時通知刻意保留給練習者實作，
> 規格、驗收條件與除錯方向見 [PRACTICE.md](PRACTICE.md)。

## 目錄

- [功能](#功能)
- [技術版本](#技術版本)
- [架構](#架構)
- [專案結構](#專案結構)
- [快速啟動（Docker Compose）](#快速啟動docker-compose)
- [開發模式](#開發模式)
- [本機範例資料](#本機範例資料選用)
- [測試](#測試)
- [環境變數](#環境變數)
- [API 一覽](#api-一覽)
- [VPS 部署](#vps-部署)
- [HTTPS 設定](#https-設定lets-encrypt)
- [維運](#維運)
- [已知限制](#已知限制)

## 功能

| 功能 | 狀態 |
|---|---|
| 註冊、登入（JWT）、登出 | ✅ 完成 |
| 建立群組（每群組一種幣別）、邀請連結、透過連結加入 | ✅ 完成 |
| 支出清單（唯讀） | ✅ 完成 |
| 結算：每位成員淨額 + 轉帳建議 | ✅ 完成 |
| 新增支出、收據上傳、即時通知 | 🧩 練習項目（骨架與規格已備妥，見 [PRACTICE.md](PRACTICE.md)） |
| 支出分頁、檢視收據（X-Accel-Redirect） | 🧩 第二階段練習項目（後端骨架與驗收測試 `Phase2AcceptanceTest` 已備妥） |

## 技術版本

以下為 2026-10 撰寫時的穩定版。

| 類別 | 套件 | 版本 |
| --- | --- | --- |
| 後端 | Java | 17 |
| | Spring Boot | 3.5.16（Spring Security 6.5.11） |
| | Flyway | 11.7.2（由 Spring Boot 管理） |
| | PostgreSQL JDBC | 42.7.11（由 Spring Boot 管理） |
| | Testcontainers | 1.21.4（由 Spring Boot 管理） |
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
| | @stomp/stompjs | 7.5.0 |
| 基礎設施 | Nginx | 1.29（nginx:1.29-alpine） |
| | PostgreSQL | 17（postgres:17-alpine） |
| | Certbot | 5.8.0（certbot/certbot:v5.8.0） |

版本選擇說明：

- **TypeScript 刻意停在 5.9**：TypeScript 7（原生 Go 版）已發布，但 vue-tsc 依賴 TypeScript 的 JS API 做 `.vue` 型別檢查，相容性尚待確認，因此先用 5.x。
- **Spring Boot 停在 3.x**：依需求使用 3.x 最新版；Spring Boot 4.x 已發布，升級需另行評估。
- **PostgreSQL 用 17 而非 18**：postgres:18 官方映像的資料目錄掛載路徑改為 `/var/lib/postgresql`，與網路上多數教學的 `/var/lib/postgresql/data` 不同，容易踩雷。
- 版本會隨時間更新，升級前請先閱讀各專案的 release notes。

## 架構

```
瀏覽器
  │  http://（本機） / https://（正式）
  ▼
┌──────────────────────────── nginx ────────────────────────────┐
│  /            → Vue build 產物（history fallback 到 index.html）│
│  /assets/*    → 帶 hash 的靜態檔（快取一年）                   │
│  /api/*       → 反向代理到 backend:8080                         │
│  /ws          → （練習項目：WebSocket 代理）                    │
└───────────────────────────────┬───────────────────────────────┘
                                ▼
                    backend（Spring Boot :8080，不對外開放）
                                │
                                ▼
                    postgres（:5432，只綁定 127.0.0.1）
```

- 前端與 API 同網域，不會有 CORS 問題，也不需要對外開放後端 port。
- 資料表由 Flyway migration 建立（`backend/src/main/resources/db/migration`），Hibernate 只做 `validate`。

## 專案結構

```
SplitMate/
├── docker-compose.yml           # nginx + backend + postgres（本機與正式共用）
├── docker-compose.prod.yml      # 正式環境覆寫：443、憑證、certbot
├── .env.example                 # 機敏設定範本（複製為 .env，不進版控）
├── PRACTICE.md                  # 練習規格書
├── scripts/
│   └── seed-demo-data.sql       # 本機範例資料（勿在正式環境執行）
├── nginx/
│   ├── Dockerfile               # 多階段：Node 建置前端 → Nginx 託管
│   ├── conf.d/                  # http 層級設定（gzip）與 HTTP server
│   ├── snippets/                # HTTP / HTTPS 共用的 location 設定
│   └── https/                   # HTTPS（Let's Encrypt）設定範例
├── backend/                     # Spring Boot
│   └── src/main/java/com/splitmate/
│       ├── controller/          # REST API
│       ├── service/             # 業務邏輯（含結算演算法 SettlementCalculator）
│       ├── repository/          # Spring Data JPA
│       ├── dto/                 # Request / Response
│       ├── entity/              # JPA Entity
│       ├── config/              # Security、JWT、WebSocket
│       └── exception/           # 統一錯誤碼與例外處理
└── frontend/                    # Vue 3 + Vite + TypeScript
    └── src/
        ├── api/                 # 所有 API 呼叫（Axios 攔截器統一處理 token 與 401）
        ├── stores/              # Pinia（auth、group、expense）
        ├── views/ components/   # 頁面與元件
        ├── router/              # 路由與登入守衛
        └── composables/ utils/ types/ constants/
```

## 快速啟動（Docker Compose）

需求：Docker Desktop（Windows / macOS）或 Docker Engine + Compose plugin（Linux）。

```bash
cp .env.example .env          # Windows cmd 用：copy .env.example .env
docker compose up -d --build
```

- 首頁：http://localhost/
- 健康檢查：http://localhost/api/health → `{"code":"OK","message":"success","data":{"status":"OK"}}`
- 第一次建置需要下載依賴，約需數分鐘。
- 若 80 port 被占用，把 `.env` 的 `NGINX_HTTP_PORT` 改成 `8000`，再改用 http://localhost:8000/ 開啟。

常用指令：

```bash
docker compose ps                  # 查看服務狀態
docker compose logs -f backend     # 查看後端 log
docker compose down                # 停止（保留資料）
docker compose down -v             # 停止並刪除資料庫與收據（無法復原）
```

## 開發模式

改前端時不必每次重建 Docker 映像：資料庫用 Docker，前後端直接在本機執行。

**1. 只啟動資料庫**

```bash
docker compose up -d postgres
```

**2. 啟動後端**（需要 JDK 17 以上）

```powershell
# Windows PowerShell（在 backend 目錄）
$env:DB_USERNAME="splitmate"; $env:DB_PASSWORD="<.env 的 POSTGRES_PASSWORD>"; $env:JWT_SECRET="<.env 的 JWT_SECRET>"
.\mvnw.cmd spring-boot:run
```

```bash
# macOS / Linux（在 backend 目錄）
DB_USERNAME=splitmate DB_PASSWORD='<...>' JWT_SECRET='<...>' ./mvnw spring-boot:run
```

**3. 啟動前端**（需要 Node.js 22.12 以上）

```bash
cd frontend
npm install
npm run dev
```

開啟 http://localhost:5173/ 。Vite 會把 `/api` 代理到 `http://localhost:8080`，前端程式碼不需區分環境。

## 本機範例資料（選用）

新增支出是練習項目，完成前可以先匯入範例資料，檢查支出清單與結算畫面：

```bash
docker compose cp scripts/seed-demo-data.sql postgres:/tmp/seed-demo-data.sql
docker compose exec postgres psql -U splitmate -d splitmate -f /tmp/seed-demo-data.sql
```

- **範例帳號**（密碼皆為 `password123`）：`alice@demo.splitmate.local`、`bob@demo.splitmate.local`、`carol@demo.splitmate.local`
- **用自己的帳號加入範例群組**：開啟 http://localhost/join/demo-tokyo-trip 或 http://localhost/join/demo-roommates
- 重複執行是安全的，不會重複建立。
- **只用於本機**：這些帳號的密碼是公開的，請勿在正式環境執行。

**大量資料（第二階段效能練習用）**：在上面的範例資料之後執行，會建立一個有 5,000 筆支出的群組（邀請連結 `/join/demo-large-dataset`）：

```bash
docker compose cp scripts/seed-large-dataset.sql postgres:/tmp/seed-large-dataset.sql
docker compose exec postgres psql -U splitmate -d splitmate -f /tmp/seed-large-dataset.sql
```

## 測試

```bash
cd backend
./mvnw test          # Windows：.\mvnw.cmd test
```

- 整合測試使用 Testcontainers 啟動真正的 PostgreSQL，**需要 Docker 正在執行**。
- 測試涵蓋：
  - 註冊 / 登入 / JWT
  - 群組與邀請
  - 支出清單與結算
  - WebSocket 驗證與訂閱權限
  - 結算演算法單元測試（含隨機資料的性質測試）
- 練習項目的驗收測試 `PracticeAcceptanceTest`（第一階段）與 `Phase2AcceptanceTest`（第二階段）預設標記為 `@Disabled`，使用方式見 [PRACTICE.md](PRACTICE.md)。

前端型別檢查與建置：

```bash
cd frontend
npm run build        # 包含 vue-tsc 型別檢查
```

## 環境變數

設定於 `.env`（範本：`.env.example`）。

| 變數 | 必填 | 說明 |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` | ✅ | 資料庫名稱與帳號 |
| `POSTGRES_PASSWORD` | ✅ | 資料庫密碼，正式環境請用 `openssl rand -base64 24` 產生 |
| `POSTGRES_HOST_PORT` | | 本機連 DB 的 port（只綁定 127.0.0.1），預設 5432 |
| `JWT_SECRET` | ✅ | JWT 簽章金鑰，至少 32 bytes，請用 `openssl rand -base64 48` 產生；更換後所有人需重新登入 |
| `JWT_ACCESS_TOKEN_TTL` | | token 有效時間，預設 `2h` |
| `NGINX_HTTP_PORT` | | 對外 HTTP port，預設 80（正式環境請維持 80） |
| `NGINX_SITE_CONF` | | 僅正式環境：設為 `./nginx/https/splitmate-https.conf` 啟用 HTTPS |

## API 一覽

除了 `/api/health`、`/api/auth/*`，所有 API 都需要 `Authorization: Bearer <token>`。
回應統一為 `{ "code": "OK" | 錯誤碼, "message": "...", "data": ... }`。

| 方法 | 路徑 | 說明 |
|---|---|---|
| GET | `/api/health` | 健康檢查 |
| POST | `/api/auth/register` | 註冊（回傳 token） |
| POST | `/api/auth/login` | 登入（回傳 token） |
| GET | `/api/users/me` | 目前登入者 |
| POST | `/api/groups` | 建立群組 |
| GET | `/api/groups` | 我的群組 |
| GET | `/api/groups/{groupId}` | 群組詳細（僅成員） |
| GET | `/api/invitations/{inviteCode}` | 邀請預覽 |
| POST | `/api/invitations/{inviteCode}/join` | 加入群組（冪等） |
| GET | `/api/groups/{groupId}/expenses` | 支出清單 |
| GET | `/api/groups/{groupId}/settlement` | 結算 |
| POST | `/api/groups/{groupId}/expenses` | 🧩 新增支出（練習） |
| POST | `/api/groups/{groupId}/expenses/{expenseId}/receipt` | 🧩 上傳收據（練習） |
| WS | `/ws`（STOMP） | 🧩 即時通知（練習，連線驗證已完成） |
| GET | `/api/groups/{groupId}/expenses/paged?size=&cursor=` | 🧩 支出 cursor 分頁（第二階段練習） |
| GET | `/api/groups/{groupId}/expenses/{expenseId}/receipt` | 🧩 檢視收據，交由 nginx 以 X-Accel-Redirect 傳檔（第二階段練習） |

## VPS 部署

以下以 Ubuntu 24.04 為例，其他發行版指令可能不同。

### 1. 事前準備

- 一台 Linux VPS，建議至少 2GB RAM。
  - 1GB 也能跑，但建置時 Maven 與 npm 可能因記憶體不足失敗，需先加 swap（見下方）。
- 一個網域，在 DNS 新增 **A 記錄**指向 VPS 的 IP（例如 `splitmate.example.com`）。
  - DNS 生效可能需要數分鐘到數小時，可用 `nslookup splitmate.example.com` 確認。

### 2. 安裝 Docker

依官方文件安裝 Docker Engine 與 Compose plugin：https://docs.docker.com/engine/install/ubuntu/

```bash
docker --version && docker compose version    # 確認安裝成功
```

（1GB RAM 的主機）加 2GB swap：

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 3. 防火牆

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

> ⚠️ Docker 對外發布的 port 會繞過 ufw 規則（Docker 直接修改 iptables，見 Docker 官方文件「Packet filtering and firewalls」）。
> 本專案只發布 nginx 的 80 / 443，PostgreSQL 綁定在 `127.0.0.1`，因此不會意外對外開放。
> 新增服務時請記得：`ports` 寫 `"5432:5432"` 就等於對全世界開放。

### 4. 取得程式碼與設定

```bash
sudo mkdir -p /opt/splitmate && sudo chown $USER /opt/splitmate
git clone -b <你的分支> https://github.com/Limaohong/SplitMate.git /opt/splitmate
cd /opt/splitmate

cp .env.example .env
chmod 600 .env          # 只有自己可讀
nano .env               # 設定 POSTGRES_PASSWORD 與 JWT_SECRET（指令產生的值貼進來）
openssl rand -base64 24 # 產生 DB 密碼
openssl rand -base64 48 # 產生 JWT_SECRET
```

### 5. 啟動（先用 HTTP）

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

- 開啟 `http://splitmate.example.com/`，確認可以看到登入頁。
- 之後所有指令都要帶兩個 `-f`。可以設定別名省打字：

  ```bash
  echo "alias dcp='docker compose -f docker-compose.yml -f docker-compose.prod.yml'" >> ~/.bashrc && source ~/.bashrc
  ```

## HTTPS 設定（Let's Encrypt）

**原理**
1. certbot 把驗證檔放到 `certbot/www`。
2. nginx 在 `/.well-known/acme-challenge/` 提供這些檔案（HTTP 設定已內建這個 location）。
3. Let's Encrypt 透過 **HTTP 80 port** 讀取驗證檔，確認你擁有網域後發出憑證。

以下指令使用上一節的 `dcp` 別名，並把 `splitmate.example.com` 換成你的網域。

### 1. 準備含網域的 HTTPS 設定

```bash
cp nginx/https/splitmate-https.conf.example nginx/https/splitmate-https.conf
sed -i 's/splitmate.example.com/你的網域/g' nginx/https/splitmate-https.conf
```

`splitmate-https.conf` 已列入 `.gitignore`。

### 2. 先用測試環境確認流程

Let's Encrypt 正式環境有申請次數限制（rate limit），失敗太多次會被暫時封鎖，所以先用 `--dry-run` 測試：

```bash
dcp run --rm certbot certonly --webroot -w /var/www/certbot \
  -d 你的網域 --email 你的信箱 --agree-tos --no-eff-email --dry-run
```

看到 `The dry run was successful` 再進行下一步。

### 3. 正式申請憑證

```bash
dcp run --rm certbot certonly --webroot -w /var/www/certbot \
  -d 你的網域 --email 你的信箱 --agree-tos --no-eff-email
```

憑證會存在 `certbot/conf/live/你的網域/`（已列入 `.gitignore`，私鑰絕不能進版控）。

### 4. 切換成 HTTPS 設定

在 `.env` 加上：

```
NGINX_SITE_CONF=./nginx/https/splitmate-https.conf
```

然後重新啟動 nginx：

```bash
dcp up -d
dcp exec nginx nginx -t        # 確認設定語法正確
```

開啟 `https://你的網域/`，確認網址列出現鎖頭；`http://` 會自動轉到 `https://`。

### 5. 自動續期

Let's Encrypt 憑證有效期為 90 天。`certbot renew` 只會在到期前 30 天內才真的續期，因此可以每天執行。

```bash
crontab -e
```

加入下面這行（每天 03:17 檢查；續期後 reload nginx 讓新憑證生效）：

```
17 3 * * * cd /opt/splitmate && docker compose -f docker-compose.yml -f docker-compose.prod.yml run --rm certbot renew --quiet && docker compose -f docker-compose.yml -f docker-compose.prod.yml exec -T nginx nginx -s reload
```

手動測試續期流程：`dcp run --rm certbot renew --dry-run`。

### HTTPS 常見問題

| 症狀 | 原因與處理 |
|---|---|
| certbot 回報 `Timeout during connect` / `Connection refused` | 80 port 沒開（防火牆或雲端主機的安全群組）、DNS 尚未指向這台主機 |
| certbot 回報 `404` / `unauthorized` | nginx 沒有正確提供 `/.well-known/acme-challenge/`。確認是用 `docker-compose.prod.yml` 啟動，`certbot/www` 有掛載進 nginx |
| 切換 HTTPS 後 nginx 不斷重啟 | `NGINX_SITE_CONF` 指向的設定找不到憑證檔：網域名稱是否一致？`dcp logs nginx` 會顯示缺少哪個檔案 |
| 首次申請成功，90 天後網站憑證過期 | cron 沒有執行、或續期後沒有 reload nginx。用 `renew --dry-run` 檢查 |

## 維運

### 更新版本

```bash
cd /opt/splitmate
git pull
dcp up -d --build      # Flyway 會在後端啟動時自動執行新的 migration
```

### 備份與還原

```bash
# 備份資料庫
dcp exec -T postgres pg_dump -U splitmate -d splitmate > backup-$(date +%F).sql

# 備份收據圖片（具名 volume）
docker run --rm -v splitmate_receipt-data:/data -v "$PWD":/backup alpine \
  tar czf /backup/receipts-$(date +%F).tgz -C /data .

# 還原資料庫（會覆蓋現有資料，請先確認）
dcp exec -T postgres psql -U splitmate -d splitmate < backup-2026-10-19.sql
```

建議把備份檔另外複製到 VPS 以外的地方；只放在同一台主機，等於沒有備份。

### 查看 log

```bash
dcp logs -f nginx       # 存取紀錄與錯誤（413、502 等）
dcp logs -f backend     # Spring Boot log
```

### 常見問題

| 症狀 | 原因與處理 |
|---|---|
| 剛啟動時 API 回 `502 Bad Gateway` | 後端還在啟動（約 10～20 秒），稍等即可 |
| 只重建 backend 後一直 `502` | nginx 啟動時就解析了 backend 的 IP，backend 換了 IP 後 nginx 仍連舊的：`dcp restart nginx` |
| 首頁仍是舊版前端 | 前端是 build 進 nginx 映像的，需要 `--build`；index.html 已設定不快取，不需清瀏覽器快取 |
| 「複製邀請連結」按鈕沒反應 | 瀏覽器只允許在 HTTPS 或 localhost 使用剪貼簿 API。設定 HTTPS 後即正常 |
| 後端啟動失敗：`app.jwt.secret ... 至少需要 32 bytes` | `.env` 的 `JWT_SECRET` 太短 |

## 已知限制

- **結算採貪婪法**：轉帳筆數最多為「人數 − 1」，但不保證是理論上的最少筆數。求最少筆數屬於 NP-hard 問題；貪婪法在實務上已足夠，詳見 `SettlementCalculator` 的註解。
- **WebSocket 使用 Spring 內建的 simple broker**：只適用單台後端。若要水平擴充成多台，需改用 RabbitMQ 等外部 broker。
- **JWT 存在 localStorage**：重新整理不會登出，但遇到 XSS 時可能被讀取。更安全的做法是 HttpOnly cookie（需一併處理 CSRF）。
- **JWT 為無狀態**：登出只清除前端 token，token 在到期前（預設 2 小時）仍有效。
- **支出清單沒有分頁**：單一群組支出量很大時需改為分頁查詢，見 `ExpenseRepository` 的註解。
- **邀請碼不可重新產生**：邀請連結外流時，只能另開新群組。

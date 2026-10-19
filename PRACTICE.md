# SplitMate 練習規格書：新增支出、收據上傳、即時通知

這份文件是你的練習題目。專案其他部分（帳號、群組、支出清單、結算、部署）都已完成，
只剩下這三個功能讓你從前端、後端到 Nginx 自己做完一輪。

> 原則：本文件描述「要做到什麼」與「遇到問題往哪裡找」，**不提供解答**。
> 卡關時先看「5. 可能遇到的錯誤」，裡面只給方向。

---

## 0. 總覽

| 部分 | 你要完成的事 | 骨架位置 |
|---|---|---|
| 前端 1 | 新增支出表單（金額、說明、付款人、分攤成員、平均分攤） | `frontend/src/views/CreateExpenseView.vue` |
| 前端 2 | 收據圖片上傳 | `frontend/src/components/ReceiptUpload.vue`、`frontend/src/api/expense.ts` 的 `uploadReceipt` |
| 前端 3 | 新增後更新 Pinia store | `frontend/src/stores/expense.ts` 的 `addExpense`、`handleExpenseCreated` |
| 前端 4 | 訂閱 WebSocket，其他成員新增支出時即時刷新 | `frontend/src/composables/useExpenseSubscription.ts` |
| 後端 1 | 新增支出 API | `ExpenseService.createExpense` |
| 後端 2 | 收據上傳 API | `ExpenseService.attachReceipt`、`ReceiptStorageService.storeReceipt` |
| 後端 3 | 支出建立後推播 WebSocket 訊息 | `ExpenseNotificationService.notifyExpenseCreated` |
| Nginx 1 | 讓收據上傳不被 nginx 擋下 | `nginx/snippets/splitmate-locations.conf` |
| Nginx 2 | 讓 `/ws` 的 WebSocket 連線通過 nginx | `nginx/snippets/splitmate-locations.conf` |

### 已經幫你準備好的（不需要修改）

- **資料庫**：`V3__create_expenses.sql`（`expenses`、`expense_shares`，含 `receipt_path` 欄位）
- **Entity / Repository**：`Expense`（含 `addShare`、`attachReceipt`）、`ExpenseShare`、`ExpenseRepository`
- **DTO**：`CreateExpenseRequest`、`ExpenseShareRequest`（含格式驗證註解）、`ExpenseResponse`、`ExpenseCreatedMessage`
- **Controller**：`ExpenseController` 的 `createExpense`、`uploadReceipt` 路徑與參數都已定好
- **WebSocket 基礎設施**：`WebSocketConfig`（端點 `/ws`、broker `/topic`）、`StompAuthChannelInterceptor`（CONNECT 驗證 JWT、SUBSCRIBE 檢查成員資格）
- **收據存放**：`app.receipt.storage-directory` 設定、docker volume `receipt-data`、Dockerfile 已建立可寫入的目錄
- **Spring 上傳上限**：`spring.servlet.multipart.max-file-size=5MB`
- **前端**：`@stomp/stompjs` 已安裝、型別 `CreateExpenseRequest` / `ExpenseCreatedMessage`、路由 `/groups/:groupId/expenses/new`、群組頁的「新增支出」按鈕
- **驗收測試**：`backend/src/test/java/com/splitmate/practice/PracticeAcceptanceTest.java`

骨架方法目前會丟出 `NOT_IMPLEMENTED`（後端回 HTTP 501）或 `throwNotImplemented`（前端），實作完成後移除即可。

---

## 1. 開始之前

1. **啟動整套服務**：`docker compose up -d --build`
2. **匯入範例資料**（可選）：方式見 README「本機範例資料」
3. **開發模式**：前端改用 `npm run dev`，改完存檔立即生效。啟動方式見 README「開發模式」
4. **後端驗收測試**：
   1. 移除 `PracticeAcceptanceTest` class 上的 `@Disabled`。也可以只針對單一方法逐步開啟
   2. 在 `backend` 目錄執行：
      ```powershell
      .\mvnw.cmd test -Dtest=PracticeAcceptanceTest    # Windows
      ./mvnw test -Dtest=PracticeAcceptanceTest        # macOS / Linux
      ```
   3. 需要 Docker 正在執行，因為測試會啟動 PostgreSQL 容器
   4. 測試只檢查「行為」（HTTP 狀態碼、回應內容、錯誤碼），不限制你的實作方式

---

## 2. 功能需求

### 前端 1：新增支出表單

- **進入方式**：群組頁「支出」分頁的「新增支出」按鈕 → `/groups/{groupId}/expenses/new`
- **欄位**

  | 欄位 | 規則 |
  |---|---|
  | 金額 | 必填，大於 0。JPY、KRW 只能是整數；其他幣別最多兩位小數 |
  | 說明 | 必填，最多 100 字 |
  | 付款人 | 必填，從群組成員中選一位，預設為自己 |
  | 分攤成員 | 至少一位，預設全選 |
  | 平均分攤 | 開關，預設開啟 |
  | 每人分攤金額 | 開啟平均分攤時自動計算且唯讀；關閉時可手動輸入 |
  | 收據 | 選填，見前端 2 |

- **平均分攤規則（必須照這個規則，後端驗收測試與結算都依賴「加總剛好等於金額」）**
  1. 以幣別的「最小單位」計算：JPY、KRW 為 1，其他幣別為 0.01
  2. 每人基本金額 = 金額 ÷ 人數，**無條件捨去**到最小單位
  3. 剩下的餘數，依「分攤成員清單的順序」從第一位開始，每人多分 1 個最小單位，直到分完
  4. 範例：
     - TWD 1000 ÷ 3 → 333.34、333.33、333.33
     - JPY 1000 ÷ 3 → 334、333、333
     - TWD 100 ÷ 4 → 25.00 × 4
- **手動分攤**：送出前檢查每人金額加總是否等於金額，不相等時不可送出，並顯示差額
- **送出流程**：建立支出 →（有選收據時）上傳收據 → 成功訊息 → 回到群組頁，清單已出現新支出
- **防呆**：送出中按鈕顯示 loading 並禁止重複送出；直接開啟網址或重新整理頁面時也要能正常顯示成員

### 前端 2：收據上傳

- 只接受 JPEG、PNG、WebP，單檔上限 5MB。前端先檢查一次，後端也會再檢查
- 選擇後顯示縮圖預覽，並可移除重選
- 支出建立成功、拿到 `expenseId` 後才上傳
- 上傳失敗時顯示易懂的訊息（例如「檔案太大」），已建立的支出不受影響
- 上傳成功後，支出清單中該筆顯示「有收據」標籤

### 前端 3：Pinia store 更新

- `addExpense`：建立成功後更新 `expenses` 與 `settlement`，回到群組頁不需重新整理就能看到最新資料
- `handleExpenseCreated`：處理 WebSocket 推播，規則同上
- 同一筆支出不可在清單出現兩次。想想自己新增的那一筆，推播也會送到自己這裡

### 前端 4：即時刷新

- 進入群組頁時連線並訂閱該群組；離開頁面時斷線
- 同一群組的其他成員新增支出時，畫面不需重新整理就出現新支出，結算也同步更新
- 在不同群組間切換時，只會收到目前群組的通知
- 連線中斷後會自動重連，重連後仍能收到通知
- HTTP 與 HTTPS 環境都要能連線

### 後端 1：新增支出 API

- 只有群組成員可以新增
- 付款人、每位分攤成員都必須是群組成員；分攤成員不可重複
- 分攤金額加總必須「剛好」等於支出金額
- 說明欄位去除前後空白後儲存
- 回傳建立好的支出，格式與支出清單的單筆相同
- 建立成功後觸發即時通知（後端 3）

### 後端 2：收據上傳 API

- 只有群組成員可以上傳，且這筆支出必須屬於路徑中的群組
- **以檔案內容判斷**是否為 JPEG、PNG、WebP，不能只看副檔名或 Content-Type
- 存檔名稱由伺服器產生，**不可**直接使用使用者上傳的檔名
- `expenses.receipt_path` 存相對路徑
- 同一筆支出重複上傳時，以最新一張為準

### 後端 3：即時通知

- 支出建立成功後，推播 `ExpenseCreatedMessage` 到 `/topic/groups/{groupId}/expenses`
- 訂閱者收到訊息時，資料庫中必須已經查得到這筆支出

### Nginx 1：上傳大小

- 經過 nginx 上傳 5MB 以內的收據要能成功
- 上限應與後端設定一致，避免使用者遇到「前端說可以、nginx 說不行」

### Nginx 2：WebSocket 代理

- `wss://你的網域/ws`（本機為 `ws://localhost/ws`）可以成功建立 WebSocket 連線
- 連線閒置數分鐘後不會被 nginx 斷開

---

## 3. API 規格

所有 API 都需要 `Authorization: Bearer <token>`，回應使用統一格式 `{ "code", "message", "data" }`。

### 3.1 新增支出

`POST /api/groups/{groupId}/expenses`

**Request**

```json
{
  "amount": 1000,
  "description": "十月電費",
  "payerUserId": 1,
  "shares": [
    { "userId": 1, "shareAmount": 333.34 },
    { "userId": 2, "shareAmount": 333.33 },
    { "userId": 3, "shareAmount": 333.33 }
  ]
}
```

**Response `201 Created`**

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": 42,
    "description": "十月電費",
    "amount": 1000.00,
    "payerUserId": 1,
    "payerDisplayName": "Alice",
    "shares": [
      { "userId": 1, "displayName": "Alice", "shareAmount": 333.34 },
      { "userId": 2, "displayName": "Bob", "shareAmount": 333.33 },
      { "userId": 3, "displayName": "Carol", "shareAmount": 333.33 }
    ],
    "hasReceipt": false,
    "createdAt": "2026-10-19T08:30:00Z"
  }
}
```

**錯誤**（`code` 欄位的值。標 ★ 的錯誤碼需要你自己加到 `ErrorCode`）

| HTTP | code | 情況 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | 格式錯誤：金額 ≤ 0、超過兩位小數、說明空白或超過 100 字、shares 為空（已由 DTO 註解處理） |
| 400 | `EXPENSE_SHARE_SUM_MISMATCH` ★ | 分攤加總 ≠ 金額 |
| 400 | `EXPENSE_MEMBER_INVALID` ★ | 付款人或分攤成員不是群組成員 |
| 400 | `EXPENSE_SHARE_DUPLICATED` ★ | 同一位成員出現兩次 |
| 401 | `UNAUTHORIZED` | 未登入或 token 失效 |
| 404 | `GROUP_NOT_FOUND` | 群組不存在，或目前使用者不是成員 |

### 3.2 上傳收據

`POST /api/groups/{groupId}/expenses/{expenseId}/receipt`

- `Content-Type: multipart/form-data`，檔案欄位名稱為 `file`
- 範例（curl）：

  ```bash
  curl -X POST http://localhost/api/groups/1/expenses/42/receipt \
    -H "Authorization: Bearer <token>" \
    -F "file=@receipt.jpg"
  ```

**Response `200 OK`**：更新後的支出，格式同 3.1，`hasReceipt` 為 `true`。

**錯誤**

| HTTP | code | 情況 |
|---|---|---|
| 400 | `RECEIPT_FILE_INVALID` ★ | 空檔案，或內容不是 JPEG / PNG / WebP |
| 404 | `GROUP_NOT_FOUND` | 群組不存在，或目前使用者不是成員 |
| 404 | `EXPENSE_NOT_FOUND` ★ | 支出不存在，或不屬於這個群組 |
| 413 | `RECEIPT_TOO_LARGE` ★ | 超過 5MB（後端那一層的錯誤；nginx 擋下時不會是這個 JSON 格式） |

### 3.3 WebSocket（STOMP）

| 項目 | 值 |
|---|---|
| 連線網址 | 與網頁同源的 `/ws`。HTTP 頁面用 `ws://`，HTTPS 頁面用 `wss://` |
| 協定 | 原生 WebSocket + STOMP（**沒有** SockJS） |
| CONNECT header | `Authorization: Bearer <token>`，放在 STOMP 的 connect headers，不是 HTTP header |
| 訂閱目的地 | `/topic/groups/{groupId}/expenses`，只能訂閱自己是成員的群組 |

**推播訊息（`ExpenseCreatedMessage`）**

```json
{
  "groupId": 1,
  "expense": { "id": 42, "description": "十月電費", "amount": 1000.00, "...": "格式同 3.1 的 data" }
}
```

**伺服器回傳的 STOMP ERROR**（`message` header 內容，已實作）

| message | 原因 |
|---|---|
| `缺少 Authorization header（格式：Bearer <token>）` | CONNECT 沒帶 token |
| `Token 無效或已過期` | token 錯誤或過期 |
| `你不是此群組的成員` | 訂閱別的群組 |
| `不允許訂閱此頻道：...` | 目的地格式不對 |

---

## 4. 驗收條件

### 後端（`PracticeAcceptanceTest` 全部通過）

- [ ] 建立支出後，清單與結算都反映新資料
- [ ] 分攤加總不符、成員不屬於群組、成員重複時回傳對應錯誤碼
- [ ] 非成員新增支出得到 `404 GROUP_NOT_FOUND`
- [ ] 可上傳 PNG 收據，上傳後 `hasReceipt` 為 `true`
- [ ] 空檔案、假裝成 PNG 的文字檔得到 `400 RECEIPT_FILE_INVALID`
- [ ] 不能透過別的群組路徑上傳收據到這筆支出（`404 EXPENSE_NOT_FOUND`）
- [ ] 超過 5MB 得到 `413 RECEIPT_TOO_LARGE`
- [ ] 已訂閱的成員在新增支出後收到 `ExpenseCreatedMessage`

### 前端（手動驗證）

- [ ] TWD 1000 三人平均分攤顯示 333.34 / 333.33 / 333.33；JPY 1000 顯示 334 / 333 / 333
- [ ] 手動分攤加總不符時無法送出，並看得到差額
- [ ] 連點兩次「送出」只會建立一筆支出
- [ ] 新增後回到群組頁，清單與結算立即更新，不需重新整理
- [ ] 上傳 3MB 的 JPG 成功；上傳 6MB 的圖片看到易懂的錯誤訊息，支出仍保留
- [ ] 開兩個瀏覽器（不同帳號、同一群組），A 新增支出後 B 的畫面幾秒內自動出現，且 A 的清單沒有重複
- [ ] 從群組 1 切到群組 2，只會收到群組 2 的通知
- [ ] 進出群組頁 10 次後，瀏覽器開發者工具中只剩 1 條 WebSocket 連線

### Nginx（`docker compose up -d --build` 後，透過 http://localhost 驗證）

- [ ] 經過 nginx 上傳 3MB 收據成功
- [ ] 開發者工具 Network → WS 的 `/ws` 狀態為 `101 Switching Protocols`
- [ ] 停在群組頁 5 分鐘不操作，連線沒有斷開（或斷開後自動重連成功）
- [ ] 設定 HTTPS 後，`wss://` 連線同樣成功

---

## 5. 可能遇到的錯誤（只給方向）

| 症狀 | 往哪裡找 |
|---|---|
| 上傳約 1MB 以上的檔案得到 `413 Request Entity Too Large`，回應是 HTML 而不是 JSON | 回應是 HTML，代表請求根本沒到 Spring。查 nginx 對請求 body 大小的**預設值** |
| 上傳超過 5MB 得到 `500 INTERNAL_ERROR` 而不是 413 | 看後端 log 拋出的是哪一個 Exception 類別，再看 `GlobalExceptionHandler` 有沒有處理它 |
| 上傳得到 `400`，訊息提到 `Required part 'file' is not present` | 表單欄位名稱；以及你是否手動設定了 `Content-Type`（multipart 的 boundary 從哪來？） |
| WebSocket 連不上，Network 面板中 `/ws` 是 `200` 且回應是 HTML | 請求被哪個 location 接走了？`Upgrade`、`Connection` 屬於「hop-by-hop」header，代理預設不會轉發；也查 `proxy_http_version` |
| WebSocket 連上約 60 秒後斷線 | nginx 代理的讀取逾時預設值；STOMP 的心跳（heartbeat）機制 |
| `npm run dev` 時 WebSocket 連不上，docker 版卻可以 | Vite 的 proxy 是否也代理了 `/ws`？需要特別打開 WebSocket 支援 |
| HTTPS 頁面上 WebSocket 被瀏覽器直接擋下（Mixed Content） | 連線網址的協定是否跟著頁面切換 |
| STOMP 回 `缺少 Authorization header` | token 要放在 STOMP 的 connect headers；瀏覽器原生 WebSocket 不能自訂 HTTP header |
| 訂閱時收到 `你不是此群組的成員` | 訂閱目的地中的 groupId 是否正確、是否在切換群組後仍用舊 groupId |
| 後端 `LazyInitializationException` | 讀取 LAZY 關聯（payer、shares、user）時交易是否還開著？專案設定了 `open-in-view: false` |
| 收到推播後立刻重新查詢，卻查不到剛新增的支出 | 推播是在交易「提交前」還是「提交後」送出的？查 Spring 在交易提交後執行動作的機制 |
| 結算頁出現 `SETTLEMENT_UNBALANCED` | 某筆支出的分攤加總不等於金額。檢查平均分攤的尾數處理，以及後端是否真的驗證了加總 |
| 前端加總 333.34 + 333.33 + 333.33 結果不是 1000 | 浮點數誤差。用什麼「單位」計算可以只處理整數？ |
| 後端比較 `100` 與 `100.00` 判斷為不相等 | `BigDecimal.equals` 與 `compareTo` 的差別 |
| 每進出一次群組頁，WebSocket 連線就多一條 | 元件卸載時有沒有斷線 |
| 同一筆支出在自己的清單出現兩次 | 自己新增的那筆也會收到推播 |
| docker 中存檔失敗 `AccessDeniedException` | 容器以非 root 執行；如果你改了存放路徑，目錄擁有者是誰？（看 `backend/Dockerfile`） |
| 上傳檔名像 `../../x.jpg` 時檔案跑到奇怪的地方 | 路徑穿越（Path Traversal）。檔名應該由誰決定？ |
| 改了 nginx 設定卻沒有效果 | 設定是 build 進映像的，需要 `--build`；或用 `docker compose exec nginx nginx -t` 先檢查語法 |

查 nginx 問題的好用指令：

```bash
docker compose logs -f nginx          # 即時看 nginx 的 access / error log
docker compose exec nginx nginx -t    # 檢查設定語法
```

---

## 6. 建議實作順序

1. **後端 1：新增支出 API**
   - 先開啟驗收測試中「後端 1」的幾個方法，讓它們通過
   - 再用 curl 或 Postman 實際打一次，並到結算頁確認數字正確
2. **前端 1 + 3：表單與 store**
   - 用 `npm run dev` 開發，先完成平均分攤的計算
   - 可以先寫成獨立的純函式，並用幾個範例驗證
3. **整合驗證**：`docker compose up -d --build`，用瀏覽器完整新增一筆支出
4. **後端 2 → 前端 2 → Nginx 1：收據上傳**
   - 先用小檔案（< 1MB）確認整條流程通
   - 再換 3MB 的檔案，你會遇到 413，接著修 nginx
5. **後端 3 → 前端 4 → Nginx 2：即時通知**
   - 先在 `npm run dev` 下讓兩個瀏覽器互相收到通知
   - 再到 docker 版，你會發現連不上，接著修 nginx
6. **HTTPS**：照 README 設定 HTTPS 後，確認上傳與 `wss://` 都正常

---

## 7. 進階挑戰（選做）

- **幣別小數位數**：後端驗證 JPY / KRW 的金額與分攤不可有小數
  - 提示：`java.util.Currency.getDefaultFractionDigits()`。但 ISO 4217 對 TWD 的規定是 2 位，要不要照用？
- **檢視收據**：新增 `GET /api/groups/{groupId}/expenses/{expenseId}/receipt` 回傳圖片，前端點「有收據」標籤可預覽
  - 注意權限：不能讓非成員猜網址看到別人的收據
- **刪除支出**：刪除後也推播通知，並一併刪除收據檔案
- **HSTS**：在 HTTPS 設定加上 `Strict-Transport-Security`
  - 留意 `nginx/snippets/splitmate-locations.conf` 底部提到的 `add_header` 繼承陷阱
- **圖片壓縮**：上傳前在前端用 canvas 把大照片縮到 1600px 以內，減少上傳時間與儲存空間

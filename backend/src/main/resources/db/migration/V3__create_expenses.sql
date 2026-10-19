-- 支出。金額一律 NUMERIC(12,2)：精確十進位，不會有浮點數誤差（最大 9,999,999,999.99）
CREATE TABLE expenses (
    id                 BIGSERIAL     PRIMARY KEY,
    group_id           BIGINT        NOT NULL REFERENCES expense_groups (id),
    payer_user_id      BIGINT        NOT NULL REFERENCES users (id),
    amount             NUMERIC(12,2) NOT NULL,
    description        VARCHAR(100)  NOT NULL,
    -- 收據圖片在伺服器上的相對路徑；沒有上傳收據時為 NULL（上傳功能為練習項目，見 PRACTICE.md）
    receipt_path       VARCHAR(255),
    created_by_user_id BIGINT        NOT NULL REFERENCES users (id),
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    -- 資料庫層最後一道防線：即使應用程式驗證有漏洞，也不會寫入 0 或負數金額
    CONSTRAINT ck_expenses_amount_positive CHECK (amount > 0)
);

-- 支出清單依群組查詢並依時間倒序排列
CREATE INDEX idx_expenses_group_id_created_at ON expenses (group_id, created_at DESC);

-- 每筆支出由哪些成員分攤、各應付多少。所有 share_amount 加總必須等於 expenses.amount（由應用程式保證）
CREATE TABLE expense_shares (
    id           BIGSERIAL     PRIMARY KEY,
    -- 刪除支出時一併刪除分攤明細
    expense_id   BIGINT        NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    user_id      BIGINT        NOT NULL REFERENCES users (id),
    share_amount NUMERIC(12,2) NOT NULL,
    CONSTRAINT uk_expense_shares_expense_user UNIQUE (expense_id, user_id),
    CONSTRAINT ck_expense_shares_amount_not_negative CHECK (share_amount >= 0)
);

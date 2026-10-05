-- 分帳群組。表名不用 groups：GROUPS 在 SQL 標準與 MySQL 8 是保留字，避免日後移植或撰寫原生 SQL 時踩雷
CREATE TABLE expense_groups (
    id                 BIGSERIAL   PRIMARY KEY,
    name               VARCHAR(50) NOT NULL,
    -- ISO 4217 幣別代碼，例如 TWD、JPY；一個群組只用一種幣別，結算時不需處理匯率
    currency_code      VARCHAR(3)  NOT NULL,
    -- 邀請碼：隨機且不可猜測，持有者即可加入群組
    invite_code        VARCHAR(32) NOT NULL,
    created_by_user_id BIGINT      NOT NULL REFERENCES users (id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_expense_groups_invite_code UNIQUE (invite_code)
);

CREATE TABLE group_members (
    id        BIGSERIAL   PRIMARY KEY,
    group_id  BIGINT      NOT NULL REFERENCES expense_groups (id),
    user_id   BIGINT      NOT NULL REFERENCES users (id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 同一人不能重複加入同一群組；唯一索引的第一欄是 group_id，也同時加速「查群組成員」
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id)
);

-- 加速「查某位使用者加入的所有群組」（上面的唯一索引以 group_id 開頭，無法用在只依 user_id 的查詢）
CREATE INDEX idx_group_members_user_id ON group_members (user_id);

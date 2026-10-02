-- 使用者帳號。表名用 users 而非 user：user 是 PostgreSQL 保留字，當表名需要每次加雙引號
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    -- 應用層一律轉小寫後再存，搭配 UNIQUE 即可達到「不分大小寫唯一」
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

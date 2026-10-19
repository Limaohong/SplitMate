-- =====================================================================
-- 本機開發用範例資料（僅供本機測試，請勿在正式環境執行）
-- =====================================================================
-- 為什麼需要：新增支出是練習項目，完成前 UI 沒有辦法產生支出資料；
--            這份資料讓你先確認「支出清單」與「結算」畫面是否正確。
-- 為什麼不放進 Flyway migration：migration 會在每個環境（包含正式環境）自動執行，
--            而這裡建立的是密碼公開的帳號，放到正式環境會變成任何人都能登入的後門。
--
-- 執行方式（在專案根目錄，Windows PowerShell / macOS / Linux 皆可）：
--   docker compose cp scripts/seed-demo-data.sql postgres:/tmp/seed-demo-data.sql
--   docker compose exec postgres psql -U splitmate -d splitmate -f /tmp/seed-demo-data.sql
--   （-U 與 -d 請對應 .env 的 POSTGRES_USER、POSTGRES_DB）
--
-- 建立的內容：
--   帳號（密碼皆為 password123）：alice@demo.splitmate.local、bob@demo.splitmate.local、carol@demo.splitmate.local
--   群組 1「東京五日遊（範例）」JPY，邀請連結 /join/demo-tokyo-trip
--     預期結算：Bob → Alice ¥11,000、Bob → Carol ¥4,900
--   群組 2「室友合租（範例）」TWD，邀請連結 /join/demo-roommates（含除不盡的小數尾數）
--     預期結算：Carol → Alice $333.66、Carol → Bob $332.67
--   用自己的帳號開啟邀請連結即可加入範例群組（加入後你的淨額為 0）。
--
-- 重複執行是安全的：偵測到範例帳號已存在就直接結束，不會重複建立。
-- =====================================================================

DO $$
DECLARE
    -- password123 的 BCrypt hash（由 Spring Security BCryptPasswordEncoder 產生）
    demo_password_hash CONSTANT VARCHAR := '$2a$10$o19l9TliM4d3z6OHPiXrD.f6ve1H0QX2BDkk9E9BDEhVoN02cafUG';
    alice_id    BIGINT;
    bob_id      BIGINT;
    carol_id    BIGINT;
    tokyo_id    BIGINT;
    roommate_id BIGINT;
    expense_id  BIGINT;
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email = 'alice@demo.splitmate.local') THEN
        RAISE NOTICE '範例資料已存在，略過';
        RETURN;
    END IF;

    INSERT INTO users (email, password_hash, display_name)
    VALUES ('alice@demo.splitmate.local', demo_password_hash, 'Alice') RETURNING id INTO alice_id;
    INSERT INTO users (email, password_hash, display_name)
    VALUES ('bob@demo.splitmate.local', demo_password_hash, 'Bob') RETURNING id INTO bob_id;
    INSERT INTO users (email, password_hash, display_name)
    VALUES ('carol@demo.splitmate.local', demo_password_hash, 'Carol') RETURNING id INTO carol_id;

    -- ---------------- 群組 1：東京五日遊（JPY） ----------------
    INSERT INTO expense_groups (name, currency_code, invite_code, created_by_user_id)
    VALUES ('東京五日遊（範例）', 'JPY', 'demo-tokyo-trip', alice_id) RETURNING id INTO tokyo_id;
    INSERT INTO group_members (group_id, user_id) VALUES (tokyo_id, alice_id), (tokyo_id, bob_id), (tokyo_id, carol_id);

    INSERT INTO expenses (group_id, payer_user_id, amount, description, created_by_user_id, created_at)
    VALUES (tokyo_id, alice_id, 18000, '築地壽司晚餐', alice_id, now() - INTERVAL '3 days') RETURNING id INTO expense_id;
    INSERT INTO expense_shares (expense_id, user_id, share_amount)
    VALUES (expense_id, alice_id, 6000), (expense_id, bob_id, 6000), (expense_id, carol_id, 6000);

    INSERT INTO expenses (group_id, payer_user_id, amount, description, created_by_user_id, created_at)
    VALUES (tokyo_id, bob_id, 4500, '機場計程車', bob_id, now() - INTERVAL '2 days') RETURNING id INTO expense_id;
    INSERT INTO expense_shares (expense_id, user_id, share_amount)
    VALUES (expense_id, alice_id, 1500), (expense_id, bob_id, 1500), (expense_id, carol_id, 1500);

    -- 只有 Bob 與 Carol 去環球影城：分攤成員不一定是全部成員
    INSERT INTO expenses (group_id, payer_user_id, amount, description, created_by_user_id, created_at)
    VALUES (tokyo_id, carol_id, 25800, '環球影城門票（Bob、Carol）', carol_id, now() - INTERVAL '1 day') RETURNING id INTO expense_id;
    INSERT INTO expense_shares (expense_id, user_id, share_amount)
    VALUES (expense_id, bob_id, 12900), (expense_id, carol_id, 12900);

    INSERT INTO expenses (group_id, payer_user_id, amount, description, created_by_user_id, created_at)
    VALUES (tokyo_id, alice_id, 1000, '便利商店宵夜', alice_id, now() - INTERVAL '2 hours') RETURNING id INTO expense_id;
    INSERT INTO expense_shares (expense_id, user_id, share_amount)
    VALUES (expense_id, alice_id, 500), (expense_id, carol_id, 500);

    -- ---------------- 群組 2：室友合租（TWD） ----------------
    INSERT INTO expense_groups (name, currency_code, invite_code, created_by_user_id)
    VALUES ('室友合租（範例）', 'TWD', 'demo-roommates', bob_id) RETURNING id INTO roommate_id;
    INSERT INTO group_members (group_id, user_id) VALUES (roommate_id, bob_id), (roommate_id, alice_id), (roommate_id, carol_id);

    -- 1000 ÷ 3 除不盡：多出的 0.01 由第一位負擔，確保加總剛好等於 1000
    INSERT INTO expenses (group_id, payer_user_id, amount, description, created_by_user_id, created_at)
    VALUES (roommate_id, alice_id, 1000, '十月電費', alice_id, now() - INTERVAL '5 days') RETURNING id INTO expense_id;
    INSERT INTO expense_shares (expense_id, user_id, share_amount)
    VALUES (expense_id, alice_id, 333.34), (expense_id, bob_id, 333.33), (expense_id, carol_id, 333.33);

    INSERT INTO expenses (group_id, payer_user_id, amount, description, created_by_user_id, created_at)
    VALUES (roommate_id, bob_id, 999, '十月網路費', bob_id, now() - INTERVAL '4 days') RETURNING id INTO expense_id;
    INSERT INTO expense_shares (expense_id, user_id, share_amount)
    VALUES (expense_id, alice_id, 333), (expense_id, bob_id, 333), (expense_id, carol_id, 333);

    RAISE NOTICE '範例資料建立完成';
END $$;

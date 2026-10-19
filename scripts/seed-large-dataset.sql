-- =====================================================================
-- 本機開發用：大量支出資料（僅供本機測試效能，請勿在正式環境執行）
-- =====================================================================
-- 用途：練習「cursor 分頁、無限捲動、虛擬清單」時，需要一個支出很多的群組，
--       才看得出「一次載入全部」的問題（回應變大、畫面卡頓）。
--
-- 前置：先執行 scripts/seed-demo-data.sql（需要 Alice、Bob、Carol 三個範例帳號）。
--
-- 執行方式（在專案根目錄）：
--   docker compose cp scripts/seed-large-dataset.sql postgres:/tmp/seed-large-dataset.sql
--   docker compose exec postgres psql -U splitmate -d splitmate -f /tmp/seed-large-dataset.sql
--
-- 建立的內容：
--   群組「大量資料測試（範例）」TWD，邀請連結 /join/demo-large-dataset
--   5,000 筆支出，時間平均分布在過去 365 天，每筆由三人平分（金額為 3 的倍數，分攤加總必定等於金額）
--
-- 可調整：修改下方 expense_count 即可改變筆數。重複執行是安全的（群組已存在就略過）。
-- =====================================================================

DO $$
DECLARE
    expense_count CONSTANT INTEGER := 5000;
    alice_id BIGINT;
    bob_id   BIGINT;
    carol_id BIGINT;
    large_group_id BIGINT;
BEGIN
    SELECT id INTO alice_id FROM users WHERE email = 'alice@demo.splitmate.local';
    SELECT id INTO bob_id FROM users WHERE email = 'bob@demo.splitmate.local';
    SELECT id INTO carol_id FROM users WHERE email = 'carol@demo.splitmate.local';
    IF alice_id IS NULL OR bob_id IS NULL OR carol_id IS NULL THEN
        RAISE EXCEPTION '找不到範例帳號，請先執行 scripts/seed-demo-data.sql';
    END IF;

    IF EXISTS (SELECT 1 FROM expense_groups WHERE invite_code = 'demo-large-dataset') THEN
        RAISE NOTICE '大量資料群組已存在，略過';
        RETURN;
    END IF;

    INSERT INTO expense_groups (name, currency_code, invite_code, created_by_user_id)
    VALUES ('大量資料測試（範例）', 'TWD', 'demo-large-dataset', alice_id) RETURNING id INTO large_group_id;
    INSERT INTO group_members (group_id, user_id)
    VALUES (large_group_id, alice_id), (large_group_id, bob_id), (large_group_id, carol_id);

    -- 先產生支出：付款人輪流、金額為 3 的倍數（30～3000）、時間由新到舊平均分布
    WITH generated_expenses AS (
        INSERT INTO expenses (group_id, payer_user_id, amount, description, created_by_user_id, created_at)
        SELECT large_group_id,
               (ARRAY[alice_id, bob_id, carol_id])[1 + sequence_number % 3],
               3 * (10 + (sequence_number * 37) % 991),
               '測試支出 #' || sequence_number,
               alice_id,
               now() - make_interval(secs => sequence_number * (365 * 24 * 3600.0 / expense_count))
        FROM generate_series(1, expense_count) AS sequence_number
        RETURNING id, amount
    )
    -- 每筆支出三人平分（金額是 3 的倍數，除得盡）
    INSERT INTO expense_shares (expense_id, user_id, share_amount)
    SELECT generated_expenses.id, member_ids.user_id, generated_expenses.amount / 3
    FROM generated_expenses
    CROSS JOIN (VALUES (alice_id), (bob_id), (carol_id)) AS member_ids (user_id);

    RAISE NOTICE '已建立 % 筆支出（群組 ID %）', expense_count, large_group_id;
END $$;

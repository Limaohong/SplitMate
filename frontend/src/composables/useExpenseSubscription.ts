import type { MaybeRefOrGetter } from 'vue'
import { throwNotImplemented } from '@/utils/notImplemented'

/**
 * 訂閱群組的即時支出通知（練習項目，規格見 PRACTICE.md「前端 4：即時刷新」）。
 * 在 GroupDetailView 的 setup 中呼叫：useExpenseSubscription(() => props.groupId)
 *
 * TODO：
 * 1. 用 @stomp/stompjs 的 Client 連到同源的 /ws（頁面是 https 時要用哪個協定？）
 * 2. CONNECT 時在 connectHeaders 帶上 Authorization: Bearer <token>（token 可從 utils/tokenStorage 取得）
 * 3. 連線成功後訂閱 /topic/groups/{groupId}/expenses，收到訊息交給 expenseStore.handleExpenseCreated
 * 4. groupId 改變時：取消舊訂閱、訂閱新群組（watch）
 * 5. 離開頁面時（onUnmounted）斷線，否則每進出一次頁面就多一條連線
 * 6. 斷線重連：stompjs 的 reconnectDelay；重連後要重新訂閱嗎？
 */
export function useExpenseSubscription(groupId: MaybeRefOrGetter<number>): void {
  throwNotImplemented('useExpenseSubscription', groupId)
}

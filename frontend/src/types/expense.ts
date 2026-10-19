/** 對應後端 ExpenseShareResponse */
export interface ExpenseShare {
  userId: number
  displayName: string
  shareAmount: number
}

/**
 * 對應後端 ExpenseResponse。
 * 注意：後端的 BigDecimal 在 JSON 中是數字，到了 JavaScript 會變成浮點數；
 * 顯示沒問題，但不要直接拿來做加減乘除（0.1 + 0.2 !== 0.3），金額運算請以後端為準。
 */
export interface Expense {
  id: number
  description: string
  amount: number
  payerUserId: number
  payerDisplayName: string
  shares: ExpenseShare[]
  hasReceipt: boolean
  createdAt: string
}

/** 對應後端 ExpenseShareRequest（練習項目） */
export interface ExpenseShareRequest {
  userId: number
  shareAmount: number
}

/** 對應後端 CreateExpenseRequest（練習項目），規格見 PRACTICE.md */
export interface CreateExpenseRequest {
  amount: number
  description: string
  payerUserId: number
  shares: ExpenseShareRequest[]
}

/** 對應後端 ExpenseCreatedMessage：WebSocket 推播的訊息內容（練習項目） */
export interface ExpenseCreatedMessage {
  groupId: number
  expense: Expense
}

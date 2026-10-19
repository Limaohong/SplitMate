import { sendRequest } from './http'
import type { CreateExpenseRequest, Expense } from '@/types/expense'
import { throwNotImplemented } from '@/utils/notImplemented'

export function getGroupExpenses(groupId: number): Promise<Expense[]> {
  return sendRequest<Expense[]>({ method: 'GET', url: `/groups/${groupId}/expenses` })
}

/**
 * 新增支出（練習項目）：POST /api/groups/{groupId}/expenses
 * TODO：參考上方 getGroupExpenses 的寫法，使用 sendRequest 送出請求
 */
export function createExpense(groupId: number, createExpenseRequest: CreateExpenseRequest): Promise<Expense> {
  return throwNotImplemented('createExpense', groupId, createExpenseRequest)
}

/**
 * 上傳收據（練習項目）：POST /api/groups/{groupId}/expenses/{expenseId}/receipt，multipart/form-data，欄位名稱 "file"
 * TODO：用 FormData 包裝檔案。想一想：需要自己設定 Content-Type header 嗎？（提示：boundary）
 */
export function uploadReceipt(groupId: number, expenseId: number, receiptFile: File): Promise<Expense> {
  return throwNotImplemented('uploadReceipt', groupId, expenseId, receiptFile)
}

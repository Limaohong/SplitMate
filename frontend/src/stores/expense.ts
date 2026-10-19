import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getGroupExpenses } from '@/api/expense'
import { getGroupSettlement } from '@/api/settlement'
import type { CreateExpenseRequest, Expense, ExpenseCreatedMessage } from '@/types/expense'
import type { Settlement } from '@/types/settlement'
import { throwNotImplemented } from '@/utils/notImplemented'

/**
 * 目前群組的支出與結算。兩者放在同一個 store：新增支出後（練習項目）需要同時更新清單與結算。
 */
export const useExpenseStore = defineStore('expense', () => {
  const expenses = ref<Expense[]>([])
  const settlement = ref<Settlement | null>(null)
  // 資料屬於哪個群組；切換群組時清空舊資料，並丟棄較晚回來的舊群組回應
  let activeGroupId: number | null = null

  function selectGroup(groupId: number): void {
    if (activeGroupId !== groupId) {
      activeGroupId = groupId
      expenses.value = []
      settlement.value = null
    }
  }

  async function loadExpenses(groupId: number): Promise<void> {
    selectGroup(groupId)
    const groupExpenses = await getGroupExpenses(groupId)
    if (activeGroupId === groupId) {
      expenses.value = groupExpenses
    }
  }

  async function loadSettlement(groupId: number): Promise<void> {
    selectGroup(groupId)
    const groupSettlement = await getGroupSettlement(groupId)
    if (activeGroupId === groupId) {
      settlement.value = groupSettlement
    }
  }

  /**
   * 新增支出（練習項目）。
   * TODO：
   * 1. 呼叫 api/expense.ts 的 createExpense
   * 2. 更新 expenses：要整個重新載入，還是把新支出插到最前面？各有什麼優缺點？
   * 3. 結算結果也會因此改變，記得更新 settlement
   * 4. 回傳新建立的支出（上傳收據時需要它的 id）
   */
  async function addExpense(groupId: number, createExpenseRequest: CreateExpenseRequest): Promise<Expense> {
    return throwNotImplemented('addExpense', groupId, createExpenseRequest)
  }

  /**
   * 收到 WebSocket 推播的新支出時呼叫（練習項目）。
   * TODO：
   * 1. 確認訊息屬於目前顯示的群組（activeGroupId）
   * 2. 自己新增的支出也會收到推播：如何避免清單出現重複的同一筆？
   * 3. 同樣要更新 settlement
   */
  function handleExpenseCreated(expenseCreatedMessage: ExpenseCreatedMessage): void {
    throwNotImplemented('handleExpenseCreated', expenseCreatedMessage)
  }

  /** 登出時呼叫，避免下一位登入者看到上一位的資料 */
  function clearExpenses(): void {
    activeGroupId = null
    expenses.value = []
    settlement.value = null
  }

  return {
    expenses,
    settlement,
    loadExpenses,
    loadSettlement,
    addExpense,
    handleExpenseCreated,
    clearExpenses,
  }
})

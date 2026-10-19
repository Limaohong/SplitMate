import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getGroupExpenses } from '@/api/expense'
import { getGroupSettlement } from '@/api/settlement'
import type { Expense } from '@/types/expense'
import type { Settlement } from '@/types/settlement'

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

  /** 登出時呼叫，避免下一位登入者看到上一位的資料 */
  function clearExpenses(): void {
    activeGroupId = null
    expenses.value = []
    settlement.value = null
  }

  return { expenses, settlement, loadExpenses, loadSettlement, clearExpenses }
})

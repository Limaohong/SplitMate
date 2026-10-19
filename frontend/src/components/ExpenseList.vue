<script setup lang="ts">
import { ref, watch } from 'vue'
import { getErrorMessage } from '@/api/http'
import type { CurrencyCode } from '@/constants/currency'
import { useExpenseStore } from '@/stores/expense'
import { formatDateTime, formatMoney } from '@/utils/formatters'

const props = defineProps<{
  groupId: number
  currencyCode: CurrencyCode
}>()

const expenseStore = useExpenseStore()

const isLoading = ref(true)
const loadErrorMessage = ref('')

async function loadExpenses(): Promise<void> {
  isLoading.value = true
  loadErrorMessage.value = ''
  try {
    await expenseStore.loadExpenses(props.groupId)
  } catch (error) {
    loadErrorMessage.value = getErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

watch(() => props.groupId, loadExpenses, { immediate: true })
</script>

<template>
  <el-alert v-if="loadErrorMessage" type="error" :title="loadErrorMessage" :closable="false" show-icon />
  <div v-else v-loading="isLoading" class="expense-list">
    <el-empty v-if="!isLoading && expenseStore.expenses.length === 0" description="還沒有任何支出" />
    <el-card v-for="expense in expenseStore.expenses" :key="expense.id" shadow="never" class="expense-card">
      <div class="expense-header">
        <div>
          <div class="expense-description">
            {{ expense.description }}
            <el-tag v-if="expense.hasReceipt" size="small" type="success">有收據</el-tag>
          </div>
          <div class="expense-meta">{{ expense.payerDisplayName }} 付款・{{ formatDateTime(expense.createdAt) }}</div>
        </div>
        <div class="expense-amount">{{ formatMoney(expense.amount, currencyCode) }}</div>
      </div>
      <div class="share-list">
        <el-tag v-for="share in expense.shares" :key="share.userId" type="info" size="small">
          {{ share.displayName }} {{ formatMoney(share.shareAmount, currencyCode) }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.expense-list {
  display: grid;
  gap: 8px;
  min-height: 120px;
}

.expense-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.expense-description {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.expense-meta {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.expense-amount {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
}

.share-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
</style>

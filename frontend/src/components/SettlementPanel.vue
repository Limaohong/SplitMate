<script setup lang="ts">
import { ref, watch } from 'vue'
import { getErrorMessage } from '@/api/http'
import { useExpenseStore } from '@/stores/expense'
import { formatMoney } from '@/utils/formatters'

const props = defineProps<{
  groupId: number
}>()

const expenseStore = useExpenseStore()

const isLoading = ref(true)
const loadErrorMessage = ref('')

async function loadSettlement(): Promise<void> {
  isLoading.value = true
  loadErrorMessage.value = ''
  try {
    await expenseStore.loadSettlement(props.groupId)
  } catch (error) {
    loadErrorMessage.value = getErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

function getNetAmountClass(netAmount: number): string {
  if (netAmount > 0) {
    return 'amount-receivable'
  }
  return netAmount < 0 ? 'amount-payable' : ''
}

watch(() => props.groupId, loadSettlement, { immediate: true })
</script>

<template>
  <el-alert v-if="loadErrorMessage" type="error" :title="loadErrorMessage" :closable="false" show-icon />
  <div v-else v-loading="isLoading" class="settlement-panel">
    <template v-if="expenseStore.settlement">
      <el-card shadow="never">
        <template #header>轉帳建議</template>
        <el-empty v-if="expenseStore.settlement.transfers.length === 0" description="大家都已結清，不需要轉帳" />
        <ol v-else class="transfer-list">
          <li v-for="transfer in expenseStore.settlement.transfers" :key="`${transfer.fromUserId}-${transfer.toUserId}`">
            <strong>{{ transfer.fromDisplayName }}</strong>
            付給
            <strong>{{ transfer.toDisplayName }}</strong>
            <span class="transfer-amount">
              {{ formatMoney(transfer.amount, expenseStore.settlement.currencyCode) }}
            </span>
          </li>
        </ol>
        <p class="hint-text">依「欠最多的人先付給被欠最多的人」計算，轉帳筆數最多為「人數 − 1」。</p>
      </el-card>

      <el-card shadow="never">
        <template #header>成員收支</template>
        <el-table :data="expenseStore.settlement.memberBalances" row-key="userId">
          <el-table-column prop="displayName" label="成員" min-width="90" />
          <el-table-column label="已付" align="right" min-width="100">
            <template #default="{ row }">{{ formatMoney(row.paidAmount, expenseStore.settlement.currencyCode) }}</template>
          </el-table-column>
          <el-table-column label="應分攤" align="right" min-width="100">
            <template #default="{ row }">{{ formatMoney(row.owedAmount, expenseStore.settlement.currencyCode) }}</template>
          </el-table-column>
          <el-table-column label="淨額" align="right" min-width="100">
            <template #default="{ row }">
              <span :class="getNetAmountClass(row.netAmount)">
                {{ formatMoney(row.netAmount, expenseStore.settlement.currencyCode) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <p class="hint-text">淨額 = 已付 − 應分攤。正數代表應收回，負數代表應付出。</p>
      </el-card>
    </template>
  </div>
</template>

<style scoped>
.settlement-panel {
  display: grid;
  gap: 16px;
  min-height: 120px;
}

.transfer-list {
  margin: 0;
  padding-left: 20px;
  line-height: 2;
}

.transfer-amount {
  margin-left: 8px;
  font-weight: 600;
}

.hint-text {
  margin-bottom: 0;
  color: #909399;
  font-size: 13px;
}

.amount-receivable {
  color: #67c23a;
}

.amount-payable {
  color: #f56c6c;
}
</style>

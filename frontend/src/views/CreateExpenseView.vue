<script setup lang="ts">
/**
 * 新增支出頁（練習項目，規格見 PRACTICE.md「前端 1：新增支出表單」）。
 *
 * TODO：
 * 1. 從 groupStore 取得群組成員與幣別（直接開啟此網址時 currentGroup 可能是 null，要先載入）
 * 2. 表單欄位：金額、說明、付款人（預設為自己）、分攤成員（預設全選）
 * 3. 「平均分攤」：依分攤成員自動計算每人金額
 *    - 除不盡時尾數怎麼分配，才能讓加總剛好等於金額？
 *    - JavaScript 的 0.1 + 0.2 !== 0.3，金額要用什麼單位計算才安全？
 *    - JPY / KRW 沒有小數，TWD 等幣別有兩位小數
 * 4. 關閉平均分攤時可手動輸入每人金額，送出前檢查加總是否等於金額
 * 5. 送出：expenseStore.addExpense → （有選收據時）uploadReceipt → 導回群組頁
 * 6. 防止重複送出（連點兩次送出按鈕會發生什麼事？）
 */
import ReceiptUpload from '@/components/ReceiptUpload.vue'

defineProps<{
  groupId: number
}>()
</script>

<template>
  <div class="page-container">
    <el-page-header @back="$router.push({ name: 'group-detail', params: { groupId } })">
      <template #content>新增支出</template>
    </el-page-header>
    <el-card class="form-card">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="練習項目：新增支出表單尚未實作"
        description="請依 PRACTICE.md 的規格完成這個頁面。"
      />
      <ReceiptUpload class="receipt-upload" />
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 720px;
  margin: 0 auto;
}

.form-card {
  margin-top: 16px;
}

.receipt-upload {
  margin-top: 16px;
}
</style>

<script setup lang="ts">
/**
 * 收據圖片上傳元件（練習項目，規格見 PRACTICE.md「前端 2：收據上傳」）。
 *
 * TODO：
 * 1. 讓使用者選擇一張圖片（el-upload 或 <input type="file">），只接受 JPEG / PNG / WebP
 * 2. 選擇後先在前端檢查大小（上限 5MB）並顯示預覽
 * 3. 新增支出成功、拿到 expenseId 之後才上傳（el-upload 預設選完就自動上傳，要怎麼關掉？）
 * 4. 上傳失敗時（例如 413）顯示易懂的錯誤訊息，但已建立的支出不應因此消失
 *
 * 建議的元件介面（可自行調整）：
 *   v-model:file → File | null，由父元件（CreateExpenseView）在支出建立後呼叫 uploadReceipt
 */
</script>

<template>
  <el-alert type="info" :closable="false" title="收據上傳（練習項目，尚未實作）" />
</template>

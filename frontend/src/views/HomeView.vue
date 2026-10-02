<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getHealth } from '@/api/health'
import { getErrorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const isLoading = ref(true)
const backendStatus = ref('')
const errorMessage = ref('')

async function loadBackendStatus(): Promise<void> {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const healthResponse = await getHealth()
    backendStatus.value = healthResponse.status
  } catch (error) {
    backendStatus.value = ''
    errorMessage.value = getErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadBackendStatus)
</script>

<template>
  <el-card class="home-card">
    <h1>歡迎，{{ authStore.currentUser?.displayName }}</h1>
    <p>朋友出遊、室友合租，記錄共同支出，自動算出誰該付給誰多少錢。</p>
    <div class="status-row" v-loading="isLoading">
      <span>後端狀態：</span>
      <el-tag v-if="backendStatus" type="success">{{ backendStatus }}</el-tag>
      <el-tag v-else-if="errorMessage" type="danger">{{ errorMessage }}</el-tag>
      <el-button size="small" @click="loadBackendStatus">重新檢查</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.home-card {
  max-width: 640px;
  margin: 40px auto;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>

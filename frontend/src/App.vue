<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

async function logout(): Promise<void> {
  authStore.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <RouterLink to="/" class="app-title">SplitMate</RouterLink>
      <div v-if="authStore.currentUser" class="user-menu">
        <span>{{ authStore.currentUser.displayName }}</span>
        <el-button size="small" @click="logout">登出</el-button>
      </div>
    </el-header>
    <el-main>
      <RouterView />
    </el-main>
  </el-container>
</template>

<style>
body {
  margin: 0;
  font-family: system-ui, -apple-system, 'Segoe UI', 'Noto Sans TC', sans-serif;
  background-color: #f5f7fa;
}
</style>

<style scoped>
.app-layout {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.app-title {
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
  text-decoration: none;
}

.user-menu {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>

<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <el-container class="app-layout">
    <el-header class="app-header">
      <RouterLink to="/" class="app-title">SplitMate</RouterLink>
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
  background-color: #ffffff;
  border-bottom: 1px solid #e4e7ed;
}

.app-title {
  font-size: 20px;
  font-weight: 600;
  color: #409eff;
  text-decoration: none;
}
</style>

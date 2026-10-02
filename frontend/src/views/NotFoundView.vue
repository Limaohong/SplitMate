<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome(): void {
  router.push('/')
}
</script>

<template>
  <el-result icon="warning" title="404" sub-title="找不到這個頁面">
    <template #extra>
      <el-button type="primary" @click="goHome">回首頁</el-button>
    </template>
  </el-result>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/api/http'
import CreateGroupDialog from '@/components/CreateGroupDialog.vue'
import { CURRENCY_LABELS } from '@/constants/currency'
import { useGroupStore } from '@/stores/group'

const groupStore = useGroupStore()

const isLoading = ref(true)
const isCreateDialogVisible = ref(false)

async function loadMyGroups(): Promise<void> {
  isLoading.value = true
  try {
    await groupStore.loadMyGroups()
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    isLoading.value = false
  }
}

onMounted(loadMyGroups)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的群組</h2>
      <el-button type="primary" @click="isCreateDialogVisible = true">建立群組</el-button>
    </div>

    <div v-loading="isLoading" class="group-list">
      <el-empty
        v-if="!isLoading && groupStore.groupSummaries.length === 0"
        description="還沒有群組，建立一個，或請朋友傳邀請連結給你"
      />
      <RouterLink
        v-for="groupSummary in groupStore.groupSummaries"
        :key="groupSummary.id"
        :to="{ name: 'group-detail', params: { groupId: groupSummary.id } }"
        class="group-link"
      >
        <el-card shadow="hover">
          <div class="group-name">{{ groupSummary.name }}</div>
          <div class="group-meta">
            <el-tag size="small">{{ groupSummary.currencyCode }} {{ CURRENCY_LABELS[groupSummary.currencyCode] }}</el-tag>
            <span>{{ groupSummary.memberCount }} 位成員</span>
          </div>
        </el-card>
      </RouterLink>
    </div>

    <CreateGroupDialog v-model:visible="isCreateDialogVisible" />
  </div>
</template>

<style scoped>
.page-container {
  max-width: 720px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.group-list {
  display: grid;
  gap: 12px;
  min-height: 120px;
}

.group-link {
  text-decoration: none;
}

.group-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.group-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #909399;
  font-size: 14px;
}
</style>

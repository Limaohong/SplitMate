<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getErrorMessage } from '@/api/http'
import { CURRENCY_LABELS } from '@/constants/currency'
import { useGroupStore } from '@/stores/group'

const props = defineProps<{
  groupId: number
}>()

const groupStore = useGroupStore()

const isLoading = ref(true)
const loadErrorMessage = ref('')

const inviteLink = computed(() => {
  const inviteCode = groupStore.currentGroup?.inviteCode
  // 用目前網址的 origin 組出連結，本機（http://localhost）與正式網域都不需額外設定
  return inviteCode ? `${window.location.origin}/join/${inviteCode}` : ''
})

async function loadGroupDetail(): Promise<void> {
  isLoading.value = true
  loadErrorMessage.value = ''
  try {
    await groupStore.loadGroupDetail(props.groupId)
  } catch (error) {
    loadErrorMessage.value = getErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

/**
 * navigator.clipboard 只在「安全來源」可用（HTTPS 或 localhost）。
 * VPS 上若還沒設定 HTTPS，這個 API 會是 undefined，因此提供手動複製的退路。
 */
async function copyInviteLink(): Promise<void> {
  if (!navigator.clipboard) {
    ElMessage.warning('目前連線不是 HTTPS，瀏覽器不允許自動複製，請手動選取連結複製')
    return
  }
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    ElMessage.success('已複製邀請連結')
  } catch {
    ElMessage.warning('複製失敗，請手動選取連結複製')
  }
}

function selectAllText(event: FocusEvent): void {
  ;(event.target as HTMLInputElement).select()
}

// 同一個元件在不同群組間切換時（/groups/1 → /groups/2）不會重新建立，必須監聽 props 變化
watch(() => props.groupId, loadGroupDetail, { immediate: true })
</script>

<template>
  <div class="page-container">
    <el-page-header @back="$router.push({ name: 'home' })">
      <template #content>
        <span v-if="groupStore.currentGroup" class="group-title">
          {{ groupStore.currentGroup.name }}
          <el-tag size="small">
            {{ groupStore.currentGroup.currencyCode }} {{ CURRENCY_LABELS[groupStore.currentGroup.currencyCode] }}
          </el-tag>
        </span>
      </template>
    </el-page-header>

    <el-result v-if="loadErrorMessage" icon="error" :title="loadErrorMessage">
      <template #extra>
        <el-button type="primary" @click="$router.push({ name: 'home' })">回我的群組</el-button>
      </template>
    </el-result>

    <div v-else v-loading="isLoading" class="section-list">
      <template v-if="groupStore.currentGroup">
        <el-card>
          <template #header>邀請朋友加入</template>
          <p class="hint-text">把連結傳給朋友，對方登入後即可加入此群組。持有連結的人都能加入，請只分享給信任的人。</p>
          <div class="invite-row">
            <el-input :model-value="inviteLink" readonly @focus="selectAllText" />
            <el-button type="primary" @click="copyInviteLink">複製</el-button>
          </div>
        </el-card>

        <el-card>
          <template #header>成員（{{ groupStore.currentGroup.members.length }}）</template>
          <div class="member-list">
            <el-tag v-for="member in groupStore.currentGroup.members" :key="member.userId" type="info">
              {{ member.displayName }}
            </el-tag>
          </div>
        </el-card>
      </template>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 720px;
  margin: 0 auto;
}

.group-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.section-list {
  display: grid;
  gap: 16px;
  margin-top: 16px;
  min-height: 120px;
}

.hint-text {
  margin-top: 0;
  color: #909399;
  font-size: 14px;
}

.invite-row {
  display: flex;
  gap: 8px;
}

.member-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

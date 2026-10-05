<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getInvitation } from '@/api/invitation'
import { getErrorMessage } from '@/api/http'
import { CURRENCY_LABELS } from '@/constants/currency'
import { useGroupStore } from '@/stores/group'
import type { Invitation } from '@/types/group'

const props = defineProps<{
  inviteCode: string
}>()

const router = useRouter()
const groupStore = useGroupStore()

const invitation = ref<Invitation | null>(null)
const isLoading = ref(true)
const isJoining = ref(false)
const loadErrorMessage = ref('')

async function loadInvitation(): Promise<void> {
  isLoading.value = true
  try {
    invitation.value = await getInvitation(props.inviteCode)
  } catch (error) {
    loadErrorMessage.value = getErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function joinGroup(): Promise<void> {
  isJoining.value = true
  try {
    const joinedGroup = await groupStore.joinGroup(props.inviteCode)
    ElMessage.success(`已加入「${joinedGroup.name}」`)
    await goToGroup(joinedGroup.id)
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    isJoining.value = false
  }
}

async function goToGroup(groupId: number): Promise<void> {
  // replace：加入後按上一頁不會回到邀請頁
  await router.replace({ name: 'group-detail', params: { groupId } })
}

onMounted(loadInvitation)
</script>

<template>
  <el-card v-loading="isLoading" class="invitation-card">
    <el-result v-if="loadErrorMessage" icon="error" :title="loadErrorMessage" sub-title="請向朋友確認連結是否完整">
      <template #extra>
        <el-button type="primary" @click="router.push({ name: 'home' })">回我的群組</el-button>
      </template>
    </el-result>

    <template v-else-if="invitation">
      <p class="invitation-label">你被邀請加入</p>
      <h2>{{ invitation.groupName }}</h2>
      <p class="invitation-meta">
        幣別：{{ invitation.currencyCode }} {{ CURRENCY_LABELS[invitation.currencyCode] }}
        ・目前 {{ invitation.memberCount }} 位成員
      </p>
      <el-button v-if="invitation.isAlreadyMember" type="primary" @click="goToGroup(invitation.groupId)">
        你已是成員，前往群組
      </el-button>
      <el-button v-else type="primary" :loading="isJoining" @click="joinGroup">加入群組</el-button>
    </template>
  </el-card>
</template>

<style scoped>
.invitation-card {
  max-width: 420px;
  margin: 40px auto;
  min-height: 160px;
  text-align: center;
}

.invitation-label,
.invitation-meta {
  color: #909399;
}
</style>

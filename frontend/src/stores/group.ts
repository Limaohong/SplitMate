import { ref } from 'vue'
import { defineStore } from 'pinia'
import { createGroup as createGroupApi, getGroupDetail, getMyGroups } from '@/api/group'
import { joinGroup as joinGroupApi } from '@/api/invitation'
import type { CreateGroupRequest, GroupDetail, GroupSummary } from '@/types/group'

export const useGroupStore = defineStore('group', () => {
  const groupSummaries = ref<GroupSummary[]>([])
  const currentGroup = ref<GroupDetail | null>(null)
  // 快速切換群組時，較早送出的請求可能較晚回來；只採用最後一次請求的結果
  let latestRequestedGroupId: number | null = null

  async function loadMyGroups(): Promise<void> {
    groupSummaries.value = await getMyGroups()
  }

  async function loadGroupDetail(groupId: number): Promise<void> {
    // 切換群組時先清空，避免畫面短暫顯示上一個群組的資料
    if (currentGroup.value?.id !== groupId) {
      currentGroup.value = null
    }
    latestRequestedGroupId = groupId
    const groupDetail = await getGroupDetail(groupId)
    if (latestRequestedGroupId === groupId) {
      currentGroup.value = groupDetail
    }
  }

  async function createGroup(createGroupRequest: CreateGroupRequest): Promise<GroupDetail> {
    const createdGroup = await createGroupApi(createGroupRequest)
    currentGroup.value = createdGroup
    return createdGroup
  }

  async function joinGroup(inviteCode: string): Promise<GroupDetail> {
    const joinedGroup = await joinGroupApi(inviteCode)
    currentGroup.value = joinedGroup
    return joinedGroup
  }

  /** 登出時呼叫，避免下一位登入者看到上一位的群組資料（setup store 沒有內建 $reset） */
  function clearGroups(): void {
    groupSummaries.value = []
    currentGroup.value = null
  }

  return { groupSummaries, currentGroup, loadMyGroups, loadGroupDetail, createGroup, joinGroup, clearGroups }
})

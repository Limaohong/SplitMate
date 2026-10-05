import { sendRequest } from './http'
import type { CreateGroupRequest, GroupDetail, GroupSummary } from '@/types/group'

export function getMyGroups(): Promise<GroupSummary[]> {
  return sendRequest<GroupSummary[]>({ method: 'GET', url: '/groups' })
}

export function getGroupDetail(groupId: number): Promise<GroupDetail> {
  return sendRequest<GroupDetail>({ method: 'GET', url: `/groups/${groupId}` })
}

export function createGroup(createGroupRequest: CreateGroupRequest): Promise<GroupDetail> {
  return sendRequest<GroupDetail>({ method: 'POST', url: '/groups', data: createGroupRequest })
}

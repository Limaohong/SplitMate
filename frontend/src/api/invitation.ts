import { sendRequest } from './http'
import type { GroupDetail, Invitation } from '@/types/group'

// 邀請碼是 Base64URL 字元（A-Z a-z 0-9 - _），仍用 encodeURIComponent 防止使用者手動改網址帶入特殊字元
export function getInvitation(inviteCode: string): Promise<Invitation> {
  return sendRequest<Invitation>({ method: 'GET', url: `/invitations/${encodeURIComponent(inviteCode)}` })
}

export function joinGroup(inviteCode: string): Promise<GroupDetail> {
  return sendRequest<GroupDetail>({ method: 'POST', url: `/invitations/${encodeURIComponent(inviteCode)}/join` })
}

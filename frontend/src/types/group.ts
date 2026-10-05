import type { CurrencyCode } from '@/constants/currency'

/** 對應後端 GroupSummaryResponse */
export interface GroupSummary {
  id: number
  name: string
  currencyCode: CurrencyCode
  memberCount: number
}

/** 對應後端 GroupMemberResponse */
export interface GroupMember {
  userId: number
  displayName: string
}

/** 對應後端 GroupDetailResponse */
export interface GroupDetail {
  id: number
  name: string
  currencyCode: CurrencyCode
  inviteCode: string
  createdAt: string
  members: GroupMember[]
}

export interface CreateGroupRequest {
  name: string
  currencyCode: CurrencyCode
}

/** 對應後端 InvitationResponse */
export interface Invitation {
  groupId: number
  groupName: string
  currencyCode: CurrencyCode
  memberCount: number
  isAlreadyMember: boolean
}

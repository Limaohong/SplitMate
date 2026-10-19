import type { CurrencyCode } from '@/constants/currency'

/** 對應後端 MemberBalanceResponse；netAmount 正數代表應收回、負數代表應付出 */
export interface MemberBalance {
  userId: number
  displayName: string
  paidAmount: number
  owedAmount: number
  netAmount: number
}

/** 對應後端 SettlementTransferResponse */
export interface SettlementTransfer {
  fromUserId: number
  fromDisplayName: string
  toUserId: number
  toDisplayName: string
  amount: number
}

/** 對應後端 SettlementResponse */
export interface Settlement {
  currencyCode: CurrencyCode
  memberBalances: MemberBalance[]
  transfers: SettlementTransfer[]
}

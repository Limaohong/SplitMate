import { sendRequest } from './http'
import type { Expense } from '@/types/expense'

export function getGroupExpenses(groupId: number): Promise<Expense[]> {
  return sendRequest<Expense[]>({ method: 'GET', url: `/groups/${groupId}/expenses` })
}

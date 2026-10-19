import { sendRequest } from './http'
import type { Settlement } from '@/types/settlement'

export function getGroupSettlement(groupId: number): Promise<Settlement> {
  return sendRequest<Settlement>({ method: 'GET', url: `/groups/${groupId}/settlement` })
}

import { sendRequest } from './http'
import type { HealthResponse } from '@/types/api'

export function getHealth(): Promise<HealthResponse> {
  return sendRequest<HealthResponse>({ method: 'GET', url: '/health' })
}

import { sendRequest } from './http'
import type { UserProfile } from '@/types/auth'

export function getCurrentUser(): Promise<UserProfile> {
  return sendRequest<UserProfile>({ method: 'GET', url: '/users/me' })
}

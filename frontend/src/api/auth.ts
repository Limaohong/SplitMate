import { sendRequest } from './http'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types/auth'

export function register(registerRequest: RegisterRequest): Promise<AuthResponse> {
  return sendRequest<AuthResponse>({ method: 'POST', url: '/auth/register', data: registerRequest })
}

export function login(loginRequest: LoginRequest): Promise<AuthResponse> {
  return sendRequest<AuthResponse>({ method: 'POST', url: '/auth/login', data: loginRequest })
}

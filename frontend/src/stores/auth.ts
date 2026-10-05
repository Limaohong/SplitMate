import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, register as registerApi } from '@/api/auth'
import { getCurrentUser } from '@/api/user'
import { useGroupStore } from '@/stores/group'
import type { AuthResponse, LoginRequest, RegisterRequest, UserProfile } from '@/types/auth'
import { clearStoredAccessToken, getStoredAccessToken, saveAccessToken } from '@/utils/tokenStorage'

export const useAuthStore = defineStore('auth', () => {
  // 初始值從 localStorage 還原，重新整理頁面後仍保持登入
  const accessToken = ref<string | null>(getStoredAccessToken())
  const currentUser = ref<UserProfile | null>(null)

  const isAuthenticated = computed(() => accessToken.value !== null)

  async function login(loginRequest: LoginRequest): Promise<void> {
    applyAuthResponse(await loginApi(loginRequest))
  }

  async function register(registerRequest: RegisterRequest): Promise<void> {
    applyAuthResponse(await registerApi(registerRequest))
  }

  /** 有 token 但還沒有使用者資料時（例如重新整理頁面）呼叫，順便驗證 token 是否仍有效 */
  async function loadCurrentUser(): Promise<void> {
    currentUser.value = await getCurrentUser()
  }

  /** JWT 無狀態，登出只需清掉前端保存的 token，並清除其他與使用者相關的 store */
  function logout(): void {
    clearStoredAccessToken()
    accessToken.value = null
    currentUser.value = null
    useGroupStore().clearGroups()
  }

  function applyAuthResponse(authResponse: AuthResponse): void {
    saveAccessToken(authResponse.accessToken)
    accessToken.value = authResponse.accessToken
    currentUser.value = authResponse.user
  }

  return { accessToken, currentUser, isAuthenticated, login, register, loadCurrentUser, logout }
})

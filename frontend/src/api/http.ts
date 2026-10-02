import axios, { type AxiosRequestConfig, HttpStatusCode, isAxiosError } from 'axios'
import type { ApiResponse } from '@/types/api'
import { getStoredAccessToken } from '@/utils/tokenStorage'

const REQUEST_TIMEOUT_MS = 10_000
const NETWORK_ERROR_CODE = 'NETWORK_ERROR'

/** 統一的 API 錯誤，畫面只需處理這一種型別 */
export class ApiRequestError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly httpStatus?: number,
  ) {
    super(message)
    this.name = 'ApiRequestError'
  }
}

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | null = null

/**
 * 由 main.ts 註冊「token 失效時要做什麼」（清除登入狀態、導向登入頁）。
 * 用註冊的方式而不是在這裡直接 import router / store，可以避免 http → router → store → api → http 的循環依賴。
 */
export function setUnauthorizedHandler(handler: UnauthorizedHandler): void {
  unauthorizedHandler = handler
}

/**
 * 所有 API 共用的 Axios 實例。baseURL 用相對路徑 /api：
 * 正式環境由 nginx 反向代理、開發環境由 Vite proxy 轉發，前端不需知道後端位址。
 */
const httpClient = axios.create({
  baseURL: '/api',
  timeout: REQUEST_TIMEOUT_MS,
})

httpClient.interceptors.request.use((config) => {
  const accessToken = getStoredAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (isExpiredSessionError(error)) {
      unauthorizedHandler?.()
    }
    return Promise.reject(toApiRequestError(error))
  },
)

/**
 * 只有「有帶 token 卻收到 401」才代表登入已失效。
 * 登入時密碼錯誤同樣是 401，但請求沒帶 token，不應觸發登出與跳轉，交給登入頁自己顯示錯誤。
 */
function isExpiredSessionError(error: unknown): boolean {
  return (
    isAxiosError(error) &&
    error.response?.status === HttpStatusCode.Unauthorized &&
    Boolean(error.config?.headers?.Authorization)
  )
}

function toApiRequestError(error: unknown): ApiRequestError {
  if (isAxiosError<ApiResponse<unknown>>(error) && error.response) {
    const responseBody = error.response.data
    return new ApiRequestError(
      responseBody?.code ?? `HTTP_${error.response.status}`,
      responseBody?.message ?? error.message,
      error.response.status,
    )
  }
  return new ApiRequestError(NETWORK_ERROR_CODE, '無法連線到伺服器，請稍後再試')
}

/** 發送請求並取出統一回應格式中的 data，呼叫端直接拿到業務資料 */
export async function sendRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.request<ApiResponse<T>>(config)
  return response.data.data
}

/** 畫面顯示錯誤訊息用：ApiRequestError 用後端訊息，其他未預期錯誤給通用訊息 */
export function getErrorMessage(error: unknown): string {
  return error instanceof ApiRequestError ? error.message : '發生未預期的錯誤'
}

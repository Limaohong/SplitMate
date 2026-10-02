import axios, { type AxiosRequestConfig, isAxiosError } from 'axios'
import type { ApiResponse } from '@/types/api'

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

/**
 * 所有 API 共用的 Axios 實例。baseURL 用相對路徑 /api：
 * 正式環境由 nginx 反向代理、開發環境由 Vite proxy 轉發，前端不需知道後端位址。
 */
const httpClient = axios.create({
  baseURL: '/api',
  timeout: REQUEST_TIMEOUT_MS,
})

// 階段 2 會在這裡加入 request 攔截器（帶入 JWT）與 401 處理

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiRequestError(error)),
)

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

/** 對應後端 com.splitmate.dto.ApiResponse */
export interface ApiResponse<T> {
  code: string
  message: string
  data: T
}

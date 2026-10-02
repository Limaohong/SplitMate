/** 對應後端 com.splitmate.dto.UserResponse */
export interface UserProfile {
  id: number
  email: string
  displayName: string
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
}

export interface LoginRequest {
  email: string
  password: string
}

/** 對應後端 com.splitmate.dto.AuthResponse */
export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: UserProfile
}

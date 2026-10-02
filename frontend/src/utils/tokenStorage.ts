/**
 * Access token 的持久化集中在這裡：http.ts 與 auth store 都只透過這組函式存取，
 * 之後若要改存 sessionStorage 或 cookie，只需修改這個檔案。
 *
 * 取捨：存在 localStorage 重新整理不會登出，但遇到 XSS 時可被腳本讀取；
 * 更安全的做法是後端改發 HttpOnly cookie（需同時處理 CSRF），此專案為練習用途先採用較簡單的方案。
 */
const ACCESS_TOKEN_STORAGE_KEY = 'splitmate.accessToken'

export function getStoredAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY)
}

export function saveAccessToken(accessToken: string): void {
  localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, accessToken)
}

export function clearStoredAccessToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_STORAGE_KEY)
}

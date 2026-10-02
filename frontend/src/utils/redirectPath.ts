const DEFAULT_REDIRECT_PATH = '/'

/**
 * 只接受站內相對路徑，避免 /login?redirect=https://evil.example 這類開放式重新導向（Open Redirect）攻擊。
 * "//evil.example" 會被瀏覽器視為其他網域，因此也要排除。
 */
export function resolveSafeRedirectPath(redirectQuery: unknown): string {
  if (typeof redirectQuery !== 'string') {
    return DEFAULT_REDIRECT_PATH
  }
  const isSameSitePath = redirectQuery.startsWith('/') && !redirectQuery.startsWith('//')
  return isSameSitePath ? redirectQuery : DEFAULT_REDIRECT_PATH
}

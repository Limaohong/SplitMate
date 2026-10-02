import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 不需登入即可瀏覽；未設定時預設需要登入（預設安全） */
    isPublic?: boolean
    /** 只給未登入者瀏覽（登入、註冊頁），已登入者會被導回首頁 */
    isGuestOnly?: boolean
  }
}

export {}

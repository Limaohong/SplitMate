import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  // history 模式網址沒有 #，但伺服器必須把未知路徑 fallback 到 index.html（見 nginx try_files）
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue'),
    },
    {
      // (\d+) 限制只能是數字，/groups/abc 會落到 404 頁而不是送出無效的 API 請求
      path: '/groups/:groupId(\\d+)',
      name: 'group-detail',
      component: () => import('@/views/GroupDetailView.vue'),
      props: (route) => ({ groupId: Number(route.params.groupId) }),
    },
    {
      // 邀請連結。需要登入：未登入者會先被導向登入 / 註冊，完成後依 redirect 回到這裡
      path: '/join/:inviteCode',
      name: 'join-group',
      component: () => import('@/views/JoinGroupView.vue'),
      props: true,
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { isPublic: true, isGuestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { isPublic: true, isGuestOnly: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { isPublic: true },
    },
  ],
})

/**
 * 路由守衛：
 * 1. 有 token 但尚未載入使用者（重新整理頁面）→ 先打 /users/me，失敗代表 token 已失效，清除登入狀態
 * 2. 需要登入的頁面而未登入 → 導向登入頁，並記住原本要去的頁面，登入後導回
 * 3. 已登入卻進入登入 / 註冊頁 → 導回首頁
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (authStore.isAuthenticated && !authStore.currentUser) {
    try {
      await authStore.loadCurrentUser()
    } catch {
      authStore.logout()
    }
  }

  if (!to.meta.isPublic && !authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.isGuestOnly && authStore.isAuthenticated) {
    return { name: 'home' }
  }
  return true
})

export default router

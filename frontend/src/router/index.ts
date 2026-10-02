import { createRouter, createWebHistory } from 'vue-router'

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
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

export default router

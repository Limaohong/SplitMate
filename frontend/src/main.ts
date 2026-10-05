import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { START_LOCATION } from 'vue-router'
import ElementPlus, { ElMessage } from 'element-plus'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { setUnauthorizedHandler } from './api/http'
import { useAuthStore } from './stores/auth'

const app = createApp(App)

app.use(createPinia())
app.use(router)
// 設定元件內建文字（返回、無資料、確定 / 取消等）為繁體中文，預設是英文
app.use(ElementPlus, { locale: zhTw })

const authStore = useAuthStore()

// 已登入狀態下 API 回 401（token 過期或被竄改）：清除登入狀態並導向登入頁
setUnauthorizedHandler(() => {
  authStore.logout()
  const currentRoute = router.currentRoute.value
  // 首次載入頁面時由路由守衛負責導向，這裡不重複處理，避免兩次導航互相覆蓋
  if (currentRoute === START_LOCATION || currentRoute.meta.isPublic) {
    return
  }
  ElMessage.warning('登入已過期，請重新登入')
  router.replace({ name: 'login', query: { redirect: currentRoute.fullPath } })
})

app.mount('#app')

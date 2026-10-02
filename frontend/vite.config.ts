import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const BACKEND_DEV_URL = 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 本機開發（npm run dev）時由 Vite 代理 /api，扮演正式環境中 nginx 的角色，前端程式碼不需區分環境
    proxy: {
      '/api': {
        target: BACKEND_DEV_URL,
        changeOrigin: true,
      },
    },
  },
})

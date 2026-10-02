<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getErrorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest } from '@/types/auth'
import { resolveSafeRedirectPath } from '@/utils/redirectPath'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref<FormInstance>()
const isSubmitting = ref(false)
const loginForm = reactive<LoginRequest>({
  email: '',
  password: '',
})

const loginFormRules: FormRules<LoginRequest> = {
  email: [
    { required: true, message: '請輸入電子郵件', trigger: 'blur' },
    { type: 'email', message: '電子郵件格式不正確', trigger: 'blur' },
  ],
  password: [{ required: true, message: '請輸入密碼', trigger: 'blur' }],
}

async function submitLogin(): Promise<void> {
  const isFormValid = await loginFormRef.value?.validate().catch(() => false)
  if (!isFormValid) {
    return
  }
  isSubmitting.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success(`歡迎回來，${authStore.currentUser?.displayName}`)
    await router.replace(resolveSafeRedirectPath(route.query.redirect))
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <el-card class="auth-card">
    <h2>登入 SplitMate</h2>
    <el-form
      ref="loginFormRef"
      :model="loginForm"
      :rules="loginFormRules"
      label-position="top"
      @submit.prevent="submitLogin"
    >
      <el-form-item label="電子郵件" prop="email">
        <el-input v-model="loginForm.email" autocomplete="email" />
      </el-form-item>
      <el-form-item label="密碼" prop="password">
        <el-input v-model="loginForm.password" type="password" show-password autocomplete="current-password" />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="isSubmitting" class="submit-button">登入</el-button>
    </el-form>
    <p class="switch-link">
      還沒有帳號？
      <!-- 保留 redirect，註冊完成後一樣能回到原本要去的頁面（例如邀請連結） -->
      <RouterLink :to="{ name: 'register', query: route.query }">立即註冊</RouterLink>
    </p>
  </el-card>
</template>

<style scoped>
.auth-card {
  max-width: 400px;
  margin: 40px auto;
}

.submit-button {
  width: 100%;
}

.switch-link {
  text-align: center;
}
</style>

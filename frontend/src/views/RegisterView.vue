<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormItemRule, type FormRules } from 'element-plus'
import { getErrorMessage } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { resolveSafeRedirectPath } from '@/utils/redirectPath'

// 與後端 RegisterRequest 的驗證規則保持一致（BCrypt 只取前 72 bytes，因此設上限）
const PASSWORD_MIN_LENGTH = 8
const PASSWORD_MAX_LENGTH = 72
const DISPLAY_NAME_MAX_LENGTH = 50

interface RegisterForm {
  email: string
  displayName: string
  password: string
  confirmPassword: string
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const registerFormRef = ref<FormInstance>()
const isSubmitting = ref(false)
const registerForm = reactive<RegisterForm>({
  email: '',
  displayName: '',
  password: '',
  confirmPassword: '',
})

const confirmPasswordValidator: FormItemRule['validator'] = (_rule, confirmPassword, callback) => {
  if (confirmPassword !== registerForm.password) {
    callback(new Error('兩次輸入的密碼不一致'))
    return
  }
  callback()
}

const registerFormRules: FormRules<RegisterForm> = {
  email: [
    { required: true, message: '請輸入電子郵件', trigger: 'blur' },
    { type: 'email', message: '電子郵件格式不正確', trigger: 'blur' },
  ],
  displayName: [
    { required: true, whitespace: true, message: '請輸入顯示名稱', trigger: 'blur' },
    { max: DISPLAY_NAME_MAX_LENGTH, message: `顯示名稱最多 ${DISPLAY_NAME_MAX_LENGTH} 個字`, trigger: 'blur' },
  ],
  password: [
    { required: true, message: '請輸入密碼', trigger: 'blur' },
    {
      min: PASSWORD_MIN_LENGTH,
      max: PASSWORD_MAX_LENGTH,
      message: `密碼長度需為 ${PASSWORD_MIN_LENGTH}～${PASSWORD_MAX_LENGTH} 個字元`,
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '請再次輸入密碼', trigger: 'blur' },
    { validator: confirmPasswordValidator, trigger: 'blur' },
  ],
}

async function submitRegister(): Promise<void> {
  const isFormValid = await registerFormRef.value?.validate().catch(() => false)
  if (!isFormValid) {
    return
  }
  isSubmitting.value = true
  try {
    await authStore.register({
      email: registerForm.email,
      displayName: registerForm.displayName,
      password: registerForm.password,
    })
    ElMessage.success('註冊成功')
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
    <h2>註冊 SplitMate</h2>
    <el-form
      ref="registerFormRef"
      :model="registerForm"
      :rules="registerFormRules"
      label-position="top"
      @submit.prevent="submitRegister"
    >
      <el-form-item label="電子郵件" prop="email">
        <el-input v-model="registerForm.email" autocomplete="email" />
      </el-form-item>
      <el-form-item label="顯示名稱（分帳時其他成員看到的名字）" prop="displayName">
        <el-input v-model="registerForm.displayName" :maxlength="DISPLAY_NAME_MAX_LENGTH" autocomplete="nickname" />
      </el-form-item>
      <el-form-item label="密碼" prop="password">
        <el-input v-model="registerForm.password" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-form-item label="確認密碼" prop="confirmPassword">
        <el-input v-model="registerForm.confirmPassword" type="password" show-password autocomplete="new-password" />
      </el-form-item>
      <el-button type="primary" native-type="submit" :loading="isSubmitting" class="submit-button">註冊</el-button>
    </el-form>
    <p class="switch-link">
      已經有帳號？
      <RouterLink :to="{ name: 'login', query: route.query }">前往登入</RouterLink>
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

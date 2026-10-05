<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getErrorMessage } from '@/api/http'
import { CURRENCY_CODES, CURRENCY_LABELS } from '@/constants/currency'
import { useGroupStore } from '@/stores/group'
import type { CreateGroupRequest } from '@/types/group'

// 與後端 CreateGroupRequest 的驗證規則保持一致
const GROUP_NAME_MAX_LENGTH = 50

const isDialogVisible = defineModel<boolean>('visible', { required: true })

const router = useRouter()
const groupStore = useGroupStore()

const groupFormRef = ref<FormInstance>()
const isSubmitting = ref(false)
const groupForm = reactive<CreateGroupRequest>({
  name: '',
  currencyCode: 'TWD',
})

const groupFormRules: FormRules<CreateGroupRequest> = {
  name: [
    { required: true, whitespace: true, message: '請輸入群組名稱', trigger: 'blur' },
    { max: GROUP_NAME_MAX_LENGTH, message: `群組名稱最多 ${GROUP_NAME_MAX_LENGTH} 個字`, trigger: 'blur' },
  ],
  currencyCode: [{ required: true, message: '請選擇幣別', trigger: 'change' }],
}

async function submitCreateGroup(): Promise<void> {
  const isFormValid = await groupFormRef.value?.validate().catch(() => false)
  if (!isFormValid) {
    return
  }
  isSubmitting.value = true
  try {
    const createdGroup = await groupStore.createGroup(groupForm)
    ElMessage.success('群組已建立')
    isDialogVisible.value = false
    await router.push({ name: 'group-detail', params: { groupId: createdGroup.id } })
  } catch (error) {
    ElMessage.error(getErrorMessage(error))
  } finally {
    isSubmitting.value = false
  }
}

function resetForm(): void {
  groupFormRef.value?.resetFields()
}
</script>

<template>
  <el-dialog v-model="isDialogVisible" title="建立群組" width="min(420px, 92vw)" @closed="resetForm">
    <el-form
      ref="groupFormRef"
      :model="groupForm"
      :rules="groupFormRules"
      label-position="top"
      @submit.prevent="submitCreateGroup"
    >
      <el-form-item label="群組名稱" prop="name">
        <el-input v-model="groupForm.name" :maxlength="GROUP_NAME_MAX_LENGTH" placeholder="例如：東京五日遊" />
      </el-form-item>
      <el-form-item label="幣別（建立後不可更改，所有支出都以此幣別記帳）" prop="currencyCode">
        <el-select v-model="groupForm.currencyCode">
          <el-option
            v-for="currencyCode in CURRENCY_CODES"
            :key="currencyCode"
            :label="`${currencyCode} ${CURRENCY_LABELS[currencyCode]}`"
            :value="currencyCode"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="isDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="isSubmitting" @click="submitCreateGroup">建立</el-button>
    </template>
  </el-dialog>
</template>

<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑看板' : '新建看板' }}</span>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="看板名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入看板名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
        
        <el-form-item label="刷新频率" prop="refreshInterval">
          <el-input-number
            v-model="form.refreshInterval"
            :min="0"
            :max="1440"
            placeholder="分钟（0表示不自动刷新）"
            style="width: 200px"
          />
          <span style="margin-left: 10px; color: #909399;">
            {{ form.refreshInterval > 0 ? `每${form.refreshInterval}分钟自动刷新` : '不自动刷新' }}
          </span>
        </el-form-item>
        
        <el-form-item label="是否公开" prop="isPublic">
          <el-radio-group v-model="form.isPublic">
            <el-radio :label="true">是</el-radio>
            <el-radio :label="false">否</el-radio>
          </el-radio-group>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            公开的看板可以被其他用户查看
          </span>
        </el-form-item>
        
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注（可选）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      
      <div class="form-actions">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新并打开设计器' : '创建并打开设计器' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup name="DashboardEdit">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentInstance } from 'vue'
import { getDashboard, addDashboard, updateDashboard } from '@/api/dashboard'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)

const form = reactive({
  id: null,
  name: '',
  refreshInterval: 0,
  isPublic: false,
  remark: ''
})

const rules = {
  name: [
    { required: true, message: '请输入看板名称', trigger: 'blur' },
    { min: 1, max: 50, message: '看板名称长度在 1 到 50 个字符', trigger: 'blur' }
  ]
}

function openDesigner(id) {
  const resolved = router.resolve({ path: '/dashboard/designer', query: { id } })
  window.open(resolved.href, '_blank')
}

async function loadDashboard() {
  const id = route.query.id
  if (!id) return

  isEdit.value = true
  try {
    const response = await getDashboard(id)
    if (response.code === 200) {
      const data = response.data.dashboard || response.data
      form.id = data.id
      form.name = data.name
      form.refreshInterval = data.refreshInterval || 0
      form.isPublic = data.isPublic || false
      form.remark = data.remark || ''
    } else {
      proxy.$modal.msgError(response.msg || '加载看板数据失败')
    }
  } catch (error) {
    proxy.$modal.msgError('加载看板数据失败: ' + (error.msg || error.message))
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        let response
        if (isEdit.value) {
          response = await updateDashboard(form)
        } else {
          response = await addDashboard(form)
        }
        
        if (response.code === 200) {
          proxy.$modal.msgSuccess(isEdit.value ? '更新成功' : '创建成功')
          const newId = isEdit.value ? form.id : (response.data?.id ?? response.data)
          if (newId != null && newId !== '') {
            openDesigner(newId)
          }
        } else {
          proxy.$modal.msgError(response.msg || '操作失败')
        }
      } catch (error) {
        proxy.$modal.msgError('操作失败: ' + (error.msg || error.message))
      } finally {
        submitting.value = false
      }
    }
  })
}

function handleCancel() {
  router.back()
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-actions {
  margin-top: 20px;
  text-align: right;
}
</style>

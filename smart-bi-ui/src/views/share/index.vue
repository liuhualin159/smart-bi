<template>
  <div class="share-view">
    <el-card v-if="loading" class="loading-card">
      <el-skeleton :rows="5" animated />
    </el-card>
    
    <el-card v-else-if="error" class="error-card">
      <el-result
        :icon="getErrorIcon()"
        :title="errorTitle"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="handleRetry">重试</el-button>
        </template>
      </el-result>
    </el-card>
    
    <div v-else-if="shareData">
      <!-- 看板分享 -->
      <div v-if="shareData.resourceType === 'DASHBOARD'">
        <DashboardView 
          :dashboard-id="shareData.resource.id"
          :show-refresh="false"
          :show-edit="false"
        />
      </div>
      
      <!-- 卡片分享 -->
      <div v-else-if="shareData.resourceType === 'CARD'">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ shareData.resource.name || '未命名卡片' }}</span>
            </div>
          </template>
          <CardRenderer :card="shareData.resource" />
        </el-card>
      </div>
    </div>
    
    <!-- 密码输入对话框 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="请输入访问密码"
      width="400px"
    >
      <el-form>
        <el-form-item label="访问密码">
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入分享链接的访问密码"
            @keyup.enter="handlePasswordSubmit"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePasswordSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ShareView">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCurrentInstance } from 'vue'
import { accessShareLink } from '@/api/share'
import DashboardView from '@/views/dashboard/components/DashboardView.vue'
import CardRenderer from '@/views/dashboard/components/CardRenderer.vue'

const { proxy } = getCurrentInstance()
const route = useRoute()

const loading = ref(true)
const error = ref(false)
const errorTitle = ref('')
const errorMessage = ref('')
const shareData = ref(null)
const showPasswordDialog = ref(false)
const password = ref('')
const shareKey = ref('')

// 加载分享数据
async function loadShareData(key, pwd) {
  loading.value = true
  error.value = false
  
  try {
    const response = await accessShareLink(key, pwd)
    
    if (response.code === 200) {
      shareData.value = response.data
      loading.value = false
    } else {
      // 检查是否需要密码
      if (response.data && response.data.errorCode === 'PASSWORD_REQUIRED') {
        showPasswordDialog.value = true
        loading.value = false
      } else {
        error.value = true
        errorTitle.value = '访问失败'
        errorMessage.value = response.data?.errorMessage || response.msg || '访问分享链接失败'
        loading.value = false
      }
    }
  } catch (error) {
    console.error('访问分享链接失败:', error)
    error.value = true
    errorTitle.value = '访问失败'
    errorMessage.value = error.msg || error.message || '访问分享链接失败'
    loading.value = false
  }
}

// 密码提交
function handlePasswordSubmit() {
  if (!password.value) {
    proxy.$modal.msgWarning('请输入访问密码')
    return
  }
  
  showPasswordDialog.value = false
  loadShareData(shareKey.value, password.value)
}

// 重试
function handleRetry() {
  loadShareData(shareKey.value, null)
}

// 获取错误图标
function getErrorIcon() {
  if (errorMessage.value.includes('过期')) {
    return 'error'
  } else if (errorMessage.value.includes('密码')) {
    return 'warning'
  }
  return 'error'
}

// 组件挂载
onMounted(() => {
  shareKey.value = route.params.shareKey || route.query.shareKey
  if (shareKey.value) {
    loadShareData(shareKey.value, null)
  } else {
    error.value = true
    errorTitle.value = '参数错误'
    errorMessage.value = '分享链接参数缺失'
    loading.value = false
  }
})
</script>

<style scoped>
.share-view {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20px;
}

.loading-card,
.error-card {
  max-width: 800px;
  margin: 0 auto;
}
</style>

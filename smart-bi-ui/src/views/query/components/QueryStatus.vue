<template>
  <div class="query-status">
    <el-card v-if="task" shadow="never">
      <div class="status-content">
        <div class="status-header">
          <el-icon class="status-icon" :class="getStatusClass()">
            <Loading v-if="isRunning" />
            <CircleCheck v-else-if="isSuccess" />
            <CircleClose v-else-if="isFailed" />
            <Warning v-else />
          </el-icon>
          <span class="status-text">{{ getStatusText() }}</span>
        </div>
        
        <div v-if="isRunning" class="progress-section">
          <el-progress 
            :percentage="task.progress || 0" 
            :status="getProgressStatus()"
            :stroke-width="8"
          />
          <div class="progress-info">
            <span>执行中... {{ task.progress || 0 }}%</span>
          </div>
        </div>
        
        <div v-if="task.errorMessage" class="error-section">
          <el-alert
            :title="task.errorMessage"
            type="error"
            :closable="false"
            show-icon
          />
        </div>
        
        <div v-if="task.executionTime" class="execution-info">
          <span>执行时长: {{ formatExecutionTime(task.executionTime) }}</span>
        </div>
        
        <div v-if="isRunning" class="action-section">
          <el-button 
            type="danger" 
            size="small"
            @click="handleCancel"
            :loading="cancelling"
          >
            取消任务
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup name="QueryStatus">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getCurrentInstance } from 'vue'
import { getTaskStatus, cancelTask } from '@/api/query'
import { Loading, CircleCheck, CircleClose, Warning } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()

const props = defineProps({
  taskId: {
    type: [Number, String],
    default: null
  },
  queryId: {
    type: [Number, String],
    default: null
  },
  autoPoll: {
    type: Boolean,
    default: true
  },
  pollInterval: {
    type: Number,
    default: 2000 // 2秒轮询一次
  }
})

const emit = defineEmits(['completed', 'failed', 'cancelled'])

const task = ref(null)
const polling = ref(false)
const pollTimer = ref(null)
const cancelling = ref(false)

const isRunning = computed(() => {
  return task.value && (task.value.status === 'PENDING' || task.value.status === 'RUNNING')
})

const isSuccess = computed(() => {
  return task.value && task.value.status === 'SUCCESS'
})

const isFailed = computed(() => {
  return task.value && task.value.status === 'FAILED'
})

// 加载任务状态
async function loadTaskStatus() {
  try {
    let response
    if (props.taskId) {
      response = await getTaskStatus(props.taskId)
    } else if (props.queryId) {
      response = await getTaskStatusByQueryId(props.queryId)
    } else {
      return
    }
    
    if (response.code === 200) {
      task.value = response.data
      
      // 如果任务完成，触发事件
      if (isSuccess.value) {
        emit('completed', task.value)
        stopPolling()
      } else if (isFailed.value) {
        emit('failed', task.value)
        stopPolling()
      }
    }
  } catch (error) {
    console.error('加载任务状态失败:', error)
  }
}

// 开始轮询
function startPolling() {
  if (!props.autoPoll || polling.value) {
    return
  }
  
  polling.value = true
  pollTimer.value = setInterval(() => {
    if (isRunning.value) {
      loadTaskStatus()
    } else {
      stopPolling()
    }
  }, props.pollInterval)
}

// 停止轮询
function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = null
  }
  polling.value = false
}

// 取消任务
async function handleCancel() {
  if (!props.taskId) {
    return
  }
  
  cancelling.value = true
  try {
    const response = await cancelTask(props.taskId)
    if (response.code === 200) {
      proxy.$modal.msgSuccess('任务已取消')
      emit('cancelled', task.value)
      stopPolling()
      await loadTaskStatus()
    } else {
      proxy.$modal.msgError(response.msg || '取消任务失败')
    }
  } catch (error) {
    proxy.$modal.msgError('取消任务失败: ' + (error.msg || error.message))
  } finally {
    cancelling.value = false
  }
}

// 获取状态样式类
function getStatusClass() {
  if (isRunning.value) return 'status-running'
  if (isSuccess.value) return 'status-success'
  if (isFailed.value) return 'status-failed'
  return 'status-pending'
}

// 获取状态文本
function getStatusText() {
  if (!task.value) return '未知'
  
  const statusMap = {
    'PENDING': '等待中',
    'RUNNING': '执行中',
    'SUCCESS': '已完成',
    'FAILED': '执行失败',
    'CANCELLED': '已取消'
  }
  return statusMap[task.value.status] || task.value.status
}

// 获取进度条状态
function getProgressStatus() {
  if (isFailed.value) return 'exception'
  if (isSuccess.value) return 'success'
  return null
}

// 格式化执行时长
function formatExecutionTime(ms) {
  if (ms < 1000) {
    return ms + 'ms'
  } else if (ms < 60000) {
    return (ms / 1000).toFixed(2) + 's'
  } else {
    const minutes = Math.floor(ms / 60000)
    const seconds = Math.floor((ms % 60000) / 1000)
    return minutes + 'm ' + seconds + 's'
  }
}

// 组件挂载
onMounted(() => {
  loadTaskStatus()
  if (props.autoPoll) {
    startPolling()
  }
})

// 组件卸载
onBeforeUnmount(() => {
  stopPolling()
})
</script>

<style scoped>
.query-status {
  margin-bottom: 20px;
}

.status-content {
  padding: 10px;
}

.status-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.status-icon {
  font-size: 24px;
  margin-right: 10px;
}

.status-icon.status-running {
  color: #409EFF;
  animation: rotate 1s linear infinite;
}

.status-icon.status-success {
  color: #67C23A;
}

.status-icon.status-failed {
  color: #F56C6C;
}

.status-icon.status-pending {
  color: #E6A23C;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.status-text {
  font-size: 16px;
  font-weight: 500;
}

.progress-section {
  margin: 15px 0;
}

.progress-info {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.error-section {
  margin: 15px 0;
}

.execution-info {
  margin-top: 15px;
  font-size: 14px;
  color: #909399;
}

.action-section {
  margin-top: 15px;
  text-align: right;
}
</style>

<template>
  <div class="etl-monitor-dashboard">
    <el-row :gutter="20" class="mb20">
      <!-- 任务状态概览 -->
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>任务状态概览</span>
              <el-button type="text" icon="Refresh" @click="refreshOverview">刷新</el-button>
            </div>
          </template>
          <el-row :gutter="20" v-loading="overviewLoading">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">总任务数</div>
                <div class="stat-value">{{ overviewData.totalTasks || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">启用任务</div>
                <div class="stat-value success">{{ overviewData.activeTasks || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">运行中</div>
                <div class="stat-value warning">{{ overviewData.runningExecutions || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">成功率</div>
                <div class="stat-value" :class="getSuccessRateClass()">
                  {{ overviewData.successRate || '0.00' }}%
                </div>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="20" class="mt20">
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-label">成功执行</div>
                <div class="stat-value success">{{ overviewData.successExecutions || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-label">失败执行</div>
                <div class="stat-value danger">{{ overviewData.failedExecutions || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-label">暂停任务</div>
                <div class="stat-value warning">{{ overviewData.pausedTasks || 0 }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 执行趋势图表 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>执行趋势（最近7天）</span>
              <el-select v-model="trendDays" style="width: 120px" @change="loadTrendData">
                <el-option label="最近3天" :value="3" />
                <el-option label="最近7天" :value="7" />
                <el-option label="最近30天" :value="30" />
              </el-select>
            </div>
          </template>
          <div ref="trendChartRef" style="width: 100%; height: 300px;" v-loading="trendLoading"></div>
        </el-card>
      </el-col>

      <!-- 运行中的任务 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>运行中的任务</span>
              <el-button type="text" icon="Refresh" @click="loadRunningTasks">刷新</el-button>
            </div>
          </template>
          <div v-loading="runningTasksLoading">
            <el-empty v-if="runningTasks.length === 0" description="暂无运行中的任务" :image-size="80" />
            <el-timeline v-else>
              <el-timeline-item
                v-for="task in runningTasks"
                :key="task.id"
                :timestamp="parseTime(task.lastRunTime)"
                placement="top"
              >
                <el-card>
                  <h4>{{ task.name }}</h4>
                  <p>任务ID: {{ task.id }}</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 监控数据详情 -->
    <el-row :gutter="20" class="mt20" v-if="selectedTaskId">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>任务监控详情 - {{ selectedTaskName }}</span>
              <el-button type="text" icon="Close" @click="clearSelection">关闭</el-button>
            </div>
          </template>
          <el-row :gutter="20" v-loading="monitorDataLoading">
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">总执行次数</div>
                <div class="stat-value">{{ monitorData.totalExecutions || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">成功次数</div>
                <div class="stat-value success">{{ monitorData.successCount || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">失败次数</div>
                <div class="stat-value danger">{{ monitorData.failedCount || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="stat-card">
                <div class="stat-label">成功率</div>
                <div class="stat-value" :class="getMonitorSuccessRateClass()">
                  {{ monitorData.successRate || '0.00' }}%
                </div>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="20" class="mt20">
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-label">总数据量</div>
                <div class="stat-value">{{ formatNumber(monitorData.totalDataCount) || 0 }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-label">平均执行时间</div>
                <div class="stat-value">{{ formatDuration(monitorData.avgDuration) || '0ms' }}</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-card">
                <div class="stat-label">最大执行时间</div>
                <div class="stat-value">{{ formatDuration(monitorData.maxDuration) || '0ms' }}</div>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { getEtlTaskStatusOverview, getEtlExecutionTrend, getRunningEtlTasks, getEtlMonitorData } from '@/api/etl'
import { parseTime } from '@/utils/ruoyi'
import * as echarts from 'echarts'

const props = defineProps({
  taskId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['task-selected'])

// 数据
const overviewData = reactive({
  totalTasks: 0,
  activeTasks: 0,
  pausedTasks: 0,
  runningExecutions: 0,
  successExecutions: 0,
  failedExecutions: 0,
  successRate: '0.00'
})

const monitorData = reactive({
  totalExecutions: 0,
  successCount: 0,
  failedCount: 0,
  runningCount: 0,
  successRate: '0.00',
  totalDataCount: 0,
  avgDuration: 0,
  maxDuration: 0,
  minDuration: 0
})

const runningTasks = ref([])
const selectedTaskId = ref(null)
const selectedTaskName = ref('')

// 加载状态
const overviewLoading = ref(false)
const trendLoading = ref(false)
const runningTasksLoading = ref(false)
const monitorDataLoading = ref(false)

// 趋势图表
const trendChartRef = ref(null)
let trendChart = null
const trendDays = ref(7)

// 定时刷新
let refreshTimer = null

// 加载状态概览
function loadOverview() {
  overviewLoading.value = true
  getEtlTaskStatusOverview().then(response => {
    Object.assign(overviewData, response.data)
    overviewLoading.value = false
  }).catch(() => {
    overviewLoading.value = false
  })
}

// 加载趋势数据
function loadTrendData() {
  if (!trendChartRef.value) return
  
  trendLoading.value = true
  getEtlExecutionTrend(props.taskId, trendDays.value).then(response => {
    const trendData = response.data || []
    renderTrendChart(trendData)
    trendLoading.value = false
  }).catch(() => {
    trendLoading.value = false
  })
}

// 渲染趋势图表
function renderTrendChart(data) {
  if (!trendChartRef.value) return
  
  nextTick(() => {
    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }
    
    const dates = data.map(item => item.date)
    const successData = data.map(item => item.success || 0)
    const failedData = data.map(item => item.failed || 0)
    
    const option = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross'
        }
      },
      legend: {
        data: ['成功', '失败']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: dates
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '成功',
          type: 'line',
          stack: 'Total',
          data: successData,
          itemStyle: {
            color: '#67C23A'
          }
        },
        {
          name: '失败',
          type: 'line',
          stack: 'Total',
          data: failedData,
          itemStyle: {
            color: '#F56C6C'
          }
        }
      ]
    }
    
    trendChart.setOption(option)
  })
}

// 加载运行中的任务
function loadRunningTasks() {
  runningTasksLoading.value = true
  getRunningEtlTasks().then(response => {
    runningTasks.value = response.data || []
    runningTasksLoading.value = false
  }).catch(() => {
    runningTasksLoading.value = false
  })
}

// 加载监控数据
function loadMonitorData() {
  if (!selectedTaskId.value) return
  
  monitorDataLoading.value = true
  getEtlMonitorData(selectedTaskId.value, 7).then(response => {
    Object.assign(monitorData, response.data)
    monitorDataLoading.value = false
  }).catch(() => {
    monitorDataLoading.value = false
  })
}

// 刷新概览
function refreshOverview() {
  loadOverview()
  loadRunningTasks()
  if (selectedTaskId.value) {
    loadMonitorData()
  }
}

// 选择任务
function selectTask(taskId, taskName) {
  selectedTaskId.value = taskId
  selectedTaskName.value = taskName
  loadMonitorData()
  emit('task-selected', taskId)
}

// 清除选择
function clearSelection() {
  selectedTaskId.value = null
  selectedTaskName.value = ''
  emit('task-selected', null)
}

// 格式化数字
function formatNumber(num) {
  if (!num) return '0'
  if (num >= 1000000) {
    return (num / 1000000).toFixed(2) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(2) + 'K'
  }
  return num.toString()
}

// 格式化时长
function formatDuration(ms) {
  if (!ms) return '0ms'
  if (ms < 1000) {
    return ms + 'ms'
  } else if (ms < 60000) {
    return (ms / 1000).toFixed(2) + 's'
  } else {
    return (ms / 60000).toFixed(2) + 'min'
  }
}

// 获取成功率样式类
function getSuccessRateClass() {
  const rate = parseFloat(overviewData.successRate || 0)
  if (rate >= 95) return 'success'
  if (rate >= 80) return 'warning'
  return 'danger'
}

// 获取监控成功率样式类
function getMonitorSuccessRateClass() {
  const rate = parseFloat(monitorData.successRate || 0)
  if (rate >= 95) return 'success'
  if (rate >= 80) return 'warning'
  return 'danger'
}

// 暴露方法供父组件调用
defineExpose({
  selectTask,
  refreshOverview
})

onMounted(() => {
  loadOverview()
  loadTrendData()
  loadRunningTasks()
  
  // 定时刷新（每30秒）
  refreshTimer = setInterval(() => {
    loadOverview()
    loadRunningTasks()
    if (selectedTaskId.value) {
      loadMonitorData()
    }
  }, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  if (trendChart) {
    trendChart.dispose()
  }
})
</script>

<style scoped lang="scss">
.etl-monitor-dashboard {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .stat-card {
    text-align: center;
    padding: 20px;
    background: #f5f7fa;
    border-radius: 4px;
    
    .stat-label {
      font-size: 14px;
      color: #606266;
      margin-bottom: 10px;
    }
    
    .stat-value {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      
      &.success {
        color: #67C23A;
      }
      
      &.warning {
        color: #E6A23C;
      }
      
      &.danger {
        color: #F56C6C;
      }
    }
  }
  
  .mb20 {
    margin-bottom: 20px;
  }
  
  .mt20 {
    margin-top: 20px;
  }
}
</style>

<template>
  <div class="execution-list">
    <el-form :model="queryParams" ref="queryRef" :inline="true" class="mb20 aether-filter-card">
      <el-form-item label="任务ID" prop="taskId">
        <el-input v-model="queryParams.taskId" placeholder="请输入任务ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 200px">
          <el-option label="运行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="executionList" stripe class="aether-table">
      <el-table-column label="执行ID" align="center" prop="id" width="100" />
      <el-table-column label="任务ID" align="center" prop="taskId" width="100" />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'RUNNING'" type="warning">运行中</el-tag>
          <el-tag v-else-if="scope.row.status === 'SUCCESS'" type="success">成功</el-tag>
          <el-tag v-else-if="scope.row.status === 'FAILED'" type="danger">失败</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.startTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.endTime) || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="执行耗时" align="center" prop="duration" width="120">
        <template #default="scope">
          <span>{{ formatDuration(scope.row.duration) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="数据量" align="center" prop="dataCount" width="120">
        <template #default="scope">
          <span>{{ formatNumber(scope.row.dataCount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="错误信息" align="center" prop="errorMessage" min-width="200" :show-overflow-tooltip="true">
        <template #default="scope">
          <span v-if="scope.row.errorMessage" style="color: #F56C6C;">
            {{ scope.row.errorMessage }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleViewDetail(scope.row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0" class="aether-pagination"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 执行详情对话框 -->
    <el-dialog title="执行详情" v-model="detailDialogVisible" width="800px" append-to-body>
      <el-descriptions :column="2" border v-if="currentExecution">
        <el-descriptions-item label="执行ID">{{ currentExecution.id }}</el-descriptions-item>
        <el-descriptions-item label="任务ID">{{ currentExecution.taskId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag v-if="currentExecution.status === 'RUNNING'" type="warning">运行中</el-tag>
          <el-tag v-else-if="currentExecution.status === 'SUCCESS'" type="success">成功</el-tag>
          <el-tag v-else-if="currentExecution.status === 'FAILED'" type="danger">失败</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="数据量">{{ formatNumber(currentExecution.dataCount) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">
          {{ parseTime(currentExecution.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间" :span="2">
          {{ parseTime(currentExecution.endTime) || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时" :span="2">
          {{ formatDuration(currentExecution.duration) }}
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentExecution.errorMessage">
          <el-alert type="error" :closable="false">
            {{ currentExecution.errorMessage }}
          </el-alert>
        </el-descriptions-item>
        <el-descriptions-item label="断点信息" :span="2" v-if="currentExecution.checkpoint">
          <pre style="background: #f5f7fa; padding: 10px; border-radius: 4px; overflow-x: auto;">{{ currentExecution.checkpoint }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, toRefs, onMounted } from 'vue'
import { listEtlTaskExecutions } from '@/api/etl'
import { parseTime } from '@/utils/ruoyi'

const executionList = ref([])
const loading = ref(true)
const total = ref(0)
const detailDialogVisible = ref(false)
const currentExecution = ref(null)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    taskId: undefined,
    status: undefined,
    limit: undefined
  }
})

const { queryParams } = toRefs(data)

/** 查询执行记录列表 */
function getList() {
  loading.value = true
  const params = {
    ...queryParams.value
  }
  // 移除分页参数，使用limit
  params.limit = queryParams.value.pageSize * queryParams.value.pageNum
  delete params.pageNum
  delete params.pageSize
  
  listEtlTaskExecutions(params).then(response => {
    executionList.value = response.rows || response.data || []
    total.value = response.total || executionList.value.length
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.taskId = undefined
  queryParams.value.status = undefined
  queryParams.value.pageNum = 1
  getList()
}

/** 查看详情 */
function handleViewDetail(row) {
  currentExecution.value = row
  detailDialogVisible.value = true
}

/** 格式化数字 */
function formatNumber(num) {
  if (!num && num !== 0) return '-'
  if (num >= 1000000) {
    return (num / 1000000).toFixed(2) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(2) + 'K'
  }
  return num.toString()
}

/** 格式化时长 */
function formatDuration(ms) {
  if (!ms && ms !== 0) return '-'
  if (ms < 1000) {
    return ms + 'ms'
  } else if (ms < 60000) {
    return (ms / 1000).toFixed(2) + 's'
  } else if (ms < 3600000) {
    return (ms / 60000).toFixed(2) + 'min'
  } else {
    return (ms / 3600000).toFixed(2) + 'h'
  }
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.execution-list {
  .mb20 {
    margin-bottom: 20px;
  }
}
</style>

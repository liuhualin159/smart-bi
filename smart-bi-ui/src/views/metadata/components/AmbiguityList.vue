<template>
  <div>
    <el-form ref="queryRef" :inline="true" v-show="showSearch" class="mb2 aether-filter-card">
      <el-form-item label="错误类型">
        <el-select v-model="queryParams.errorCategory" placeholder="全部" clearable style="width:140px">
          <el-option v-for="dict in bi_error_category" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="涉及表名">
        <el-input v-model="queryParams.tableName" placeholder="表名" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
          value-format="YYYY-MM-DD"
          style="width:240px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.processStatus" placeholder="全部" clearable style="width:120px">
          <el-option label="未处理" value="PENDING" />
          <el-option label="已处理" value="RESOLVED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-collapse class="mb2">
      <el-collapse-item title="按表汇总（错误类型分布，默认最近90天）" name="summary">
        <div v-loading="summaryLoading">
          <el-table :data="summaryList" border size="small" max-height="280" class="aether-table">
            <el-table-column label="表名" prop="tableName" min-width="140" show-overflow-tooltip />
            <el-table-column label="错误次数" prop="totalCount" width="100" align="center" sortable />
            <el-table-column label="错误类型分布" min-width="260">
              <template #default="scope">
                <template v-for="(c, i) in scope.row.categories" :key="i">
                  <dict-tag v-if="bi_error_category && bi_error_category.length" :options="bi_error_category" :value="c.errorCategory" />
                  <span v-else>{{ c.errorCategory }}</span>
                  <span class="summary-count">×{{ c.count }}</span>
                  <span v-if="i < scope.row.categories.length - 1" class="summary-sep"> </span>
                </template>
                <span v-if="!scope.row.categories || !scope.row.categories.length">—</span>
              </template>
            </el-table-column>
          </el-table>
          <el-button v-if="summaryList.length === 0 && !summaryLoading" text type="primary" @click="loadSummary">加载汇总</el-button>
          <el-button v-else text type="primary" @click="loadSummary">刷新汇总</el-button>
        </div>
      </el-collapse-item>
    </el-collapse>

    <el-table v-loading="loading" :data="list" border class="aether-table">
      <el-table-column label="用户问题" prop="originalQuestion" min-width="200" show-overflow-tooltip />
      <el-table-column label="错误SQL" width="80" align="center">
        <template #default="scope">
          <el-popover placement="left" :width="400" trigger="click">
            <template #reference>
              <el-button link type="primary">展开</el-button>
            </template>
            <pre class="sql-preview">{{ scope.row.generatedSql || '—' }}</pre>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="错误类型" width="110" align="center">
        <template #default="scope">
          <dict-tag v-if="bi_error_category && bi_error_category.length" :options="bi_error_category" :value="scope.row.errorCategory" />
          <span v-else>{{ scope.row.errorCategory || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="涉及表" prop="involvedTables" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag v-if="scope.row.processStatus === 'RESOLVED'" type="success">已处理</el-tag>
          <el-tag v-else type="info">未处理</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" width="165" />
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="copySql(scope.row.generatedSql)"
            v-hasPermi="['metadata:ambiguity:query']"
          >复制 SQL</el-button>
          <el-button
            link
            type="primary"
            @click="linkToFeedback(scope.row)"
            v-hasPermi="['query:feedback']"
          >关联到问题</el-button>
          <el-button
            link
            type="primary"
            icon="List"
            @click="goToTable(scope.row)"
            v-hasPermi="['metadata:table:list']"
          >去标注表</el-button>
          <el-button
            link
            type="primary"
            icon="Edit"
            @click="goToField(scope.row)"
            v-hasPermi="['metadata:field:list']"
          >去标注字段</el-button>
          <el-button
            v-if="scope.row.processStatus !== 'RESOLVED'"
            link
            type="success"
            icon="Check"
            @click="handleResolve(scope.row)"
            v-hasPermi="['metadata:ambiguity:edit']"
          >标记已处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentInstance } from 'vue'
import { listAmbiguity, resolveAmbiguity, getAmbiguitySummary } from '@/api/metadata'
import { submitFeedback } from '@/api/query'
import { ElMessage } from 'element-plus'

const props = defineProps({
  /** 从表管理页跳转时传入的表名，用于初始筛选 */
  initialTableName: { type: String, default: null }
})

const { proxy } = getCurrentInstance()
const router = useRouter()
const { bi_error_category } = proxy.useDict('bi_error_category')

const loading = ref(false)
const summaryLoading = ref(false)
const showSearch = ref(true)
const list = ref([])
const summaryList = ref([])
const total = ref(0)
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  errorCategory: undefined,
  tableName: undefined,
  startTime: undefined,
  endTime: undefined,
  processStatus: undefined
})

watch(() => props.initialTableName, (val) => {
  if (val != null && val !== '') {
    queryParams.tableName = val
    queryParams.pageNum = 1
    getList()
  }
}, { immediate: true })

onMounted(() => {
  if (!props.initialTableName) getList()
  loadSummary()
})

function loadSummary() {
  summaryLoading.value = true
  const params = {}
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  }
  getAmbiguitySummary(params).then(response => {
    summaryList.value = response.data || []
    summaryLoading.value = false
  }).catch(() => {
    summaryLoading.value = false
  })
}

function getList() {
  const params = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  } else {
    params.startTime = undefined
    params.endTime = undefined
  }
  loading.value = true
  listAmbiguity(params).then(response => {
    list.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  queryParams.errorCategory = undefined
  queryParams.tableName = undefined
  queryParams.startTime = undefined
  queryParams.endTime = undefined
  queryParams.processStatus = undefined
  queryParams.pageNum = 1
  handleQuery()
}

/** 跳转到表管理并筛选涉及表 */
function goToTable(row) {
  const firstTable = (row.involvedTables || '').split(',')[0]?.trim()
  router.push({
    path: '/bi/metadata',
    query: { tab: 'table', tableName: firstTable || undefined }
  })
}

/** 跳转到字段管理：先到表管理并带表名，用户可点「管理字段」 */
function goToField(row) {
  const firstTable = (row.involvedTables || '').split(',')[0]?.trim()
  router.push({
    path: '/bi/metadata',
    query: { tab: 'table', tableName: firstTable || undefined, openField: '1' }
  })
}

function copySql(sql) {
  if (!sql) {
    ElMessage.warning('无 SQL 可复制')
    return
  }
  navigator.clipboard?.writeText(sql).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => ElMessage.error('复制失败'))
}

function linkToFeedback(row) {
  const queryId = row.queryId
  if (!queryId) {
    ElMessage.warning('该记录无关联查询，无法关联到问题')
    return
  }
  submitFeedback({
    queryId,
    feedbackType: 'INCORRECT',
    content: '来自歧义列表：' + (row.originalQuestion || ''),
    suggestedSql: row.generatedSql
  }).then(res => {
    if (res.code === 200) {
      ElMessage.success('已关联到问题反馈')
    } else {
      ElMessage.error(res.msg || '关联失败')
    }
  }).catch(() => ElMessage.error('关联失败'))
}

function handleResolve(row) {
  proxy.$modal.confirm('确认将该条歧义记录标记为已处理？').then(() => {
    return resolveAmbiguity(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('已标记为已处理')
    getList()
  })
}

defineExpose({
  getList
})
</script>

<style scoped>
.sql-preview {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 300px;
  overflow: auto;
}
.summary-tag { margin-right: 8px; }
.summary-count { margin-left: 2px; margin-right: 6px; color: var(--el-text-color-secondary); }
.summary-sep { margin-right: 4px; }
</style>

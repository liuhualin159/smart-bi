<template>
  <div class="app-container feedback-container">
    <el-form :inline="true" class="search-form mb2">
      <el-form-item label="反馈类型">
        <el-select v-model="queryParams.feedbackType" placeholder="全部" clearable style="width: 120px" @change="loadList">
          <el-option label="正确" value="CORRECT" />
          <el-option label="错误" value="INCORRECT" />
          <el-option label="建议" value="SUGGESTION" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态">
        <el-select v-model="queryParams.reviewStatus" placeholder="全部" clearable style="width: 120px" @change="loadList">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border class="aether-table">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="查询ID" prop="queryId" width="90" />
      <el-table-column label="反馈类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.feedbackType === 'CORRECT' ? 'success' : row.feedbackType === 'INCORRECT' ? 'danger' : 'info'" size="small">
            {{ feedbackTypeLabel(row.feedbackType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="反馈内容" prop="content" min-width="160" show-overflow-tooltip />
      <el-table-column label="建议SQL" width="80" align="center">
        <template #default="{ row }">
          <el-popover v-if="row.suggestedSql" placement="left" :width="420" trigger="click">
            <template #reference>
              <el-button link type="primary" size="small">展开</el-button>
            </template>
            <pre class="sql-preview">{{ row.suggestedSql }}</pre>
          </el-popover>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.reviewStatus === 'APPROVED' ? 'success' : row.reviewStatus === 'REJECTED' ? 'danger' : 'warning'" size="small">
            {{ reviewStatusLabel(row.reviewStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核人" prop="reviewer" width="100" />
      <el-table-column label="创建时间" prop="createTime" width="160" />
      <el-table-column label="操作" width="160" fixed="right" align="center">
        <template #default="{ row }">
          <template v-if="row.reviewStatus === 'PENDING'">
            <el-button link type="success" size="small" @click="handleApprove(row)" v-hasPermi="['bi:feedback:approve']">通过</el-button>
            <el-button link type="danger" size="small" @click="handleReject(row)" v-hasPermi="['bi:feedback:approve']">驳回</el-button>
          </template>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="loadList" />

    <!-- 驳回理由弹窗 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回反馈" width="400px" :close-on-click-modal="false">
      <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="请输入驳回理由（可选）" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Feedback">
import { ref, reactive, onMounted } from 'vue'
import { getFeedbackList, approveFeedback, rejectFeedback } from '@/api/query'
import { getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const rejectDialogVisible = ref(false)
const rejectComment = ref('')
const rejectingRow = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  feedbackType: undefined,
  reviewStatus: undefined
})

function feedbackTypeLabel(v) {
  const m = { CORRECT: '正确', INCORRECT: '错误', SUGGESTION: '建议' }
  return m[v] || v
}

function reviewStatusLabel(v) {
  const m = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return m[v] || v
}

async function loadList() {
  loading.value = true
  try {
    const res = await getFeedbackList(queryParams)
    list.value = res.rows || res.data || []
    total.value = res.total ?? 0
  } catch (e) {
    proxy.$modal.msgError('加载反馈列表失败')
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  queryParams.feedbackType = undefined
  queryParams.reviewStatus = undefined
  queryParams.pageNum = 1
  loadList()
}

function handleApprove(row) {
  proxy.$modal.confirm('确认通过该反馈？通过后将写入 bi_feedback_correction 供 NL2SQL 学习。').then(async () => {
    try {
      await approveFeedback(row.id)
      proxy.$modal.msgSuccess('审核通过')
      loadList()
    } catch (e) {
      proxy.$modal.msgError(e?.msg || e?.message || '操作失败')
    }
  })
}

function handleReject(row) {
  rejectingRow.value = row
  rejectComment.value = ''
  rejectDialogVisible.value = true
}

async function confirmReject() {
  if (!rejectingRow.value) return
  try {
    await rejectFeedback(rejectingRow.value.id, rejectComment.value || undefined)
    proxy.$modal.msgSuccess('已驳回')
    rejectDialogVisible.value = false
    rejectingRow.value = null
    loadList()
  } catch (e) {
    proxy.$modal.msgError(e?.msg || e?.message || '操作失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.feedback-container {
  padding: 16px 20px;
}
.sql-preview {
  margin: 0;
  padding: 8px;
  font-size: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

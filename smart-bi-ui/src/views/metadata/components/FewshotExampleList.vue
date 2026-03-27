<template>
  <div class="fewshot-example-list">
    <el-form ref="queryRef" :inline="true" v-show="showSearch" class="mb2 aether-filter-card">
      <el-form-item label="数据源">
        <el-select
          v-model="queryParams.datasourceId"
          placeholder="全部"
          clearable
          filterable
          style="width: 200px"
          @keyup.enter="handleQuery"
        >
          <el-option
            v-for="ds in datasourceOptions"
            :key="ds.id"
            :label="ds.name || ds.datasourceName || '数据源 ' + ds.id"
            :value="ds.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.enabled" placeholder="全部" clearable style="width: 120px" @keyup.enter="handleQuery">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="问题关键词">
        <el-input v-model="queryParams.question" placeholder="模糊搜索" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['metadata:fewshot:add']">新增示例</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Upload" @click="openImportDialog" v-hasPermi="['metadata:fewshot:import']">从反馈导入</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" border class="aether-table">
      <el-table-column label="问题" prop="question" min-width="200" show-overflow-tooltip />
      <el-table-column label="SQL" width="88" align="center">
        <template #default="scope">
          <el-popover placement="left" :width="480" trigger="click">
            <template #reference>
              <el-button link type="primary" size="small">展开</el-button>
            </template>
            <pre class="sql-preview">{{ scope.row.sqlText || '—' }}</pre>
          </el-popover>
        </template>
      </el-table-column>
      <el-table-column label="数据源" prop="datasourceName" width="120" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.datasourceName || (scope.row.datasourceId ? 'ID ' + scope.row.datasourceId : '—') }}
        </template>
      </el-table-column>
      <el-table-column label="启用" width="80" align="center">
        <template #default="scope">
          <el-switch
            v-model="scope.row.enabled"
            :active-value="1"
            :inactive-value="0"
            @change="(v) => handleEnabledChange(scope.row, v)"
            v-hasPermi="['metadata:fewshot:edit']"
          />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createdAt" width="165" />
      <el-table-column label="操作" width="200" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['metadata:fewshot:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['metadata:fewshot:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑表单 -->
    <el-dialog :title="formTitle" v-model="formOpen" width="640px" @close="cancelForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="问题" prop="question">
          <el-input v-model="form.question" type="textarea" :rows="3" placeholder="用户自然语言问题" />
        </el-form-item>
        <el-form-item label="SQL" prop="sqlText">
          <el-input v-model="form.sqlText" type="textarea" :rows="5" placeholder="对应 SQL" />
        </el-form-item>
        <el-form-item label="数据源" prop="datasourceId">
          <el-select v-model="form.datasourceId" placeholder="可选，不选则对所有数据源生效" clearable filterable style="width: 100%">
            <el-option
              v-for="ds in datasourceOptions"
              :key="ds.id"
              :label="ds.name || ds.datasourceName || '数据源 ' + ds.id"
              :value="ds.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelForm">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 从反馈导入 -->
    <el-dialog v-model="importOpen" title="从反馈导入为 Few-shot 示例" width="720px" @close="closeImportDialog">
      <p class="import-tip">选择一条包含「问题 + 建议SQL」的反馈，将生成一条 Few-shot 示例。</p>
      <el-table v-loading="importLoading" :data="feedbackList" border max-height="360" highlight-current-row @current-change="onFeedbackSelect" class="aether-table">
        <el-table-column type="index" width="50" label="#" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="content" label="反馈内容" min-width="160" show-overflow-tooltip />
        <el-table-column label="建议SQL" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.suggestedSql" type="success" size="small">有</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
      </el-table>
      <template #footer>
        <el-button @click="closeImportDialog">取消</el-button>
        <el-button type="primary" :disabled="!selectedFeedbackId" :loading="importSubmitting" @click="confirmImport">导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import {
  listFewshotExample,
  getFewshotExample,
  addFewshotExample,
  updateFewshotExample,
  delFewshotExample,
  updateFewshotEnabled,
  importFewshotFromFeedback
} from '@/api/metadata'
import { listDataSource } from '@/api/datasource'
import { getFeedbackList } from '@/api/query'

const { proxy } = getCurrentInstance()
const queryRef = ref(null)
const formRef = ref(null)

const loading = ref(false)
const showSearch = ref(true)
const list = ref([])
const total = ref(0)
const datasourceOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  datasourceId: undefined,
  enabled: undefined,
  question: undefined
})

const formOpen = ref(false)
const formTitle = ref('新增 Few-shot 示例')
const form = reactive({
  id: undefined,
  question: '',
  sqlText: '',
  datasourceId: undefined,
  enabled: 1
})

const formRules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  sqlText: [{ required: true, message: '请输入 SQL', trigger: 'blur' }]
}

const importOpen = ref(false)
const importLoading = ref(false)
const importSubmitting = ref(false)
const feedbackList = ref([])
const selectedFeedbackId = ref(null)

onMounted(() => {
  loadDatasourceOptions()
  getList()
})

function loadDatasourceOptions() {
  listDataSource({ pageNum: 1, pageSize: 500 }).then((res) => {
    const rows = res.rows || res.data || []
    datasourceOptions.value = Array.isArray(rows) ? rows : []
  }).catch(() => {
    datasourceOptions.value = []
  })
}

function getList() {
  loading.value = true
  const params = { ...queryParams }
  if (params.enabled === undefined || params.enabled === '') delete params.enabled
  listFewshotExample(params).then((res) => {
    list.value = res.rows || []
    total.value = res.total ?? 0
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
  queryParams.datasourceId = undefined
  queryParams.enabled = undefined
  queryParams.question = undefined
  queryParams.pageNum = 1
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAdd() {
  formTitle.value = '新增 Few-shot 示例'
  form.id = undefined
  form.question = ''
  form.sqlText = ''
  form.datasourceId = undefined
  form.enabled = 1
  formOpen.value = true
}

async function handleUpdate(row) {
  formTitle.value = '编辑 Few-shot 示例'
  const res = await getFewshotExample(row.id)
  const data = res.data || res
  form.id = data.id
  form.question = data.question || ''
  form.sqlText = data.sqlText || ''
  form.datasourceId = data.datasourceId ?? undefined
  form.enabled = data.enabled != null ? data.enabled : 1
  formOpen.value = true
}

function handleEnabledChange(row, value) {
  updateFewshotEnabled(row.id, value).then(() => {
    proxy.$modal.msgSuccess(value === 1 ? '已启用' : '已禁用')
    getList()
  }).catch(() => {
    row.enabled = row.enabled === 1 ? 0 : 1
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除该 Few-shot 示例？').then(() => {
    return delFewshotExample(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  })
}

function submitForm() {
  formRef.value.validate((valid) => {
    if (!valid) return
    if (form.id) {
      updateFewshotExample(form).then(() => {
        proxy.$modal.msgSuccess('修改成功')
        formOpen.value = false
        getList()
      })
    } else {
      addFewshotExample(form).then(() => {
        proxy.$modal.msgSuccess('新增成功')
        formOpen.value = false
        getList()
      })
    }
  })
}

function cancelForm() {
  formOpen.value = false
  proxy.resetForm('formRef')
}

function openImportDialog() {
  importOpen.value = true
  selectedFeedbackId.value = null
  feedbackList.value = []
  importLoading.value = true
  getFeedbackList({ pageNum: 1, pageSize: 100, feedbackType: 'INCORRECT' }).then((res) => {
    feedbackList.value = res.rows || res.data || []
    importLoading.value = false
  }).catch(() => {
    importLoading.value = false
  })
}

function onFeedbackSelect(row) {
  selectedFeedbackId.value = row ? row.id : null
}

function confirmImport() {
  if (!selectedFeedbackId.value) return
  importSubmitting.value = true
  importFewshotFromFeedback(selectedFeedbackId.value).then(() => {
    proxy.$modal.msgSuccess('导入成功')
    importOpen.value = false
    getList()
    importSubmitting.value = false
  }).catch(() => {
    importSubmitting.value = false
  })
}

function closeImportDialog() {
  importOpen.value = false
  feedbackList.value = []
  selectedFeedbackId.value = null
}

defineExpose({
  getList
})
</script>

<style scoped>
.fewshot-example-list {
  padding: 0;
}
.mb2 { margin-bottom: 12px; }
.mb8 { margin-bottom: 16px; }
.sql-preview {
  margin: 0;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 320px;
  overflow: auto;
}
.import-tip {
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>

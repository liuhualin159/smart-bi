<template>
  <div>
    <el-form ref="queryRef" :inline="true" v-show="showSearch" class="mb2 aether-filter-card">
      <el-form-item label="表名">
        <el-input v-model="queryParams.tableName" placeholder="表名" clearable style="width:160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="表注释">
        <el-input v-model="queryParams.tableComment" placeholder="表注释" clearable style="width:160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="业务描述">
        <el-input v-model="queryParams.businessDescription" placeholder="业务描述" clearable style="width:160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="用途">
        <el-select v-model="queryParams.tableUsage" placeholder="全部" clearable style="width:120px">
          <el-option v-for="dict in bi_table_usage" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="可见性">
        <el-select v-model="queryParams.nl2sqlVisibilityLevel" placeholder="全部" clearable style="width:120px">
          <el-option v-for="dict in bi_nl2sql_visibility" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['metadata:table:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5" v-if="selectedIds.length > 0">
        <el-button type="warning" plain icon="Edit" @click="openBatchUsage" v-hasPermi="['metadata:table:edit']">批量改用途</el-button>
        <el-button type="warning" plain icon="Edit" @click="openBatchVisibility" v-hasPermi="['metadata:table:edit']">批量改可见性</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="Setting" @click="openProblemConfig" v-hasPermi="['metadata:table:edit']">问题表配置</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="tableList" @selection-change="handleSelectionChange" class="aether-table">
      <el-table-column type="selection" width="45" />
      <el-table-column label="表名" prop="tableName" min-width="120" />
      <el-table-column label="表注释" prop="tableComment" min-width="100" show-overflow-tooltip />
      <el-table-column label="业务描述" prop="businessDescription" min-width="140" show-overflow-tooltip />
      <el-table-column label="用途" width="100" align="center">
        <template #default="scope">
          <el-select
            v-model="scope.row.tableUsage"
            placeholder="—"
            clearable
            size="small"
            style="width:100%"
            :loading="scope.row._saving"
            @change="(v) => saveRowUsage(scope.row, v)"
          >
            <el-option v-for="dict in bi_table_usage" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="可见性" width="100" align="center">
        <template #default="scope">
          <el-select
            v-model="scope.row.nl2sqlVisibilityLevel"
            placeholder="—"
            clearable
            size="small"
            style="width:100%"
            :loading="scope.row._saving"
            @change="(v) => saveRowVisibility(scope.row, v)"
          >
            <el-option v-for="dict in bi_nl2sql_visibility" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="粒度" prop="grainDesc" width="120" show-overflow-tooltip />
      <el-table-column label="问题" width="120" align="center">
        <template #default="scope">
          <template v-if="isProblemTable(scope.row)">
            <el-tooltip placement="top" effect="light">
              <template #content>
                <div>错误次数：{{ scope.row.errorCount }}</div>
                <div v-if="scope.row.lastErrorTime">最近错误：{{ formatTime(scope.row.lastErrorTime) }}</div>
                <div v-if="scope.row.lastErrorCategories && scope.row.lastErrorCategories.length">错误类型：{{ scope.row.lastErrorCategories.join('、') }}</div>
              </template>
              <el-link type="primary" :href="getAmbiguityLink(scope.row)" @click.prevent="goToAmbiguity(scope.row)">
                查看最近错误 SQL
              </el-link>
            </el-tooltip>
          </template>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="List" @click.stop="handleManageFields(scope.row)" v-hasPermi="['metadata:field:list']">管理字段</el-button>
          <el-button link type="primary" icon="Edit" @click.stop="handleUpdate(scope.row)" v-hasPermi="['metadata:table:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click.stop="handleDelete(scope.row)" v-hasPermi="['metadata:table:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 批量改用途 -->
    <el-dialog v-model="batchUsageVisible" title="批量改用途" width="360px">
      <el-form label-width="80px">
        <el-form-item label="用途">
          <el-select v-model="batchUsage" placeholder="请选择" style="width:100%">
            <el-option v-for="dict in bi_table_usage" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchUsageVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchUsage">确定</el-button>
      </template>
    </el-dialog>
    <!-- 批量改可见性 -->
    <el-dialog v-model="batchVisibilityVisible" title="批量改可见性" width="360px">
      <el-form label-width="80px">
        <el-form-item label="可见性">
          <el-select v-model="batchVisibility" placeholder="请选择" style="width:100%">
            <el-option v-for="dict in bi_nl2sql_visibility" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisibilityVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchVisibility">确定</el-button>
      </template>
    </el-dialog>

    <!-- 问题表高亮配置 -->
    <el-dialog v-model="problemConfigVisible" title="问题表高亮配置" width="400px">
      <el-form label-width="140px">
        <el-form-item label="统计时间窗口(天)">
          <el-input-number v-model="problemConfigForm.windowDays" :min="1" :max="365" />
        </el-form-item>
        <el-form-item label="错误次数阈值">
          <el-input-number v-model="problemConfigForm.errorCountThreshold" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="problemConfigVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProblemConfig">确定</el-button>
      </template>
    </el-dialog>

    <!-- 表元数据表单对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" @close="cancel">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="表名" prop="tableName">
          <el-select
            v-model="form.tableName"
            placeholder="请选择本地库中的表（ETL抽取后的表）"
            clearable
            filterable
            style="width: 100%"
            :loading="tableOptionsLoading"
            @change="handleTableChange"
          >
            <el-option
              v-for="t in tableOptions"
              :key="t.tableName"
              :label="t.tableName + (t.tableComment ? ' - ' + t.tableComment : '')"
              :value="t.tableName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="表注释" prop="tableComment">
          <el-input v-model="form.tableComment" placeholder="可从数据源自动带出" />
        </el-form-item>
        <el-form-item label="业务描述" prop="businessDescription">
          <el-input v-model="form.businessDescription" type="textarea" :rows="4" placeholder="请输入业务描述（最多2000字）" />
        </el-form-item>
        <el-form-item label="用途" prop="tableUsage">
          <el-select v-model="form.tableUsage" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="dict in bi_table_usage" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见性" prop="nl2sqlVisibilityLevel">
          <el-select v-model="form.nl2sqlVisibilityLevel" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="dict in bi_nl2sql_visibility" :key="dict.value" :label="dict.label" :value="dict.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="粒度描述" prop="grainDesc">
          <el-input v-model="form.grainDesc" placeholder="如：订单行粒度、用户-日粒度" clearable />
        </el-form-item>
        <el-form-item label="业务域" prop="domainId">
          <el-input v-model="form.domainId" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 字段管理弹窗 -->
    <el-dialog
      v-model="fieldDialogVisible"
      :title="'字段管理 - ' + (fieldDialogTableName || '')"
      width="1100px"
      top="3vh"
      destroy-on-close
      class="field-management-dialog"
      @close="handleFieldDialogClose"
    >
      <FieldList
        v-if="fieldDialogTableId"
        ref="fieldListRef"
        :table-id="fieldDialogTableId"
        :table-row="fieldDialogTableRow"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { listTableMetadata, getTableMetadata, addTableMetadata, updateTableMetadata, updateTableMetadataById, batchUpdateTableMetadata, delTableMetadata, getTableProblemConfig, updateTableProblemConfig } from '@/api/metadata'
import { getLocalTableList } from '@/api/datasource'
import FieldList from './FieldList.vue'
import { WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  domainId: {
    type: Number,
    default: null
  },
  /** 从歧义页跳转时传入的表名，用于初始筛选 */
  initialTableName: {
    type: String,
    default: null
  }
})

const { proxy } = getCurrentInstance()
const router = useRouter()
const formRef = ref(null)

const { bi_table_usage, bi_nl2sql_visibility } = proxy.useDict('bi_table_usage', 'bi_nl2sql_visibility')

const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const tableList = ref([])
const selectedIds = ref([])
const problemConfig = ref({ windowDays: 30, errorCountThreshold: 3 })

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  tableName: undefined,
  tableComment: undefined,
  businessDescription: undefined,
  tableUsage: undefined,
  nl2sqlVisibilityLevel: undefined,
  domainId: null
})

const batchUsageVisible = ref(false)
const batchVisibilityVisible = ref(false)
const batchUsage = ref('')
const batchVisibility = ref('')

const problemConfigVisible = ref(false)
const problemConfigForm = reactive({ windowDays: 30, errorCountThreshold: 3 })

const form = reactive({
  id: undefined,
  tableName: '',
  tableComment: '',
  businessDescription: '',
  tableUsage: undefined,
  nl2sqlVisibilityLevel: undefined,
  grainDesc: '',
  domainId: null
})

const tableOptions = ref([])
const tableOptionsLoading = ref(false)

const fieldDialogVisible = ref(false)
const fieldDialogTableId = ref(null)
const fieldDialogTableName = ref('')
const fieldDialogTableRow = ref(null)
const fieldListRef = ref(null)

const rules = {
  tableName: [{ required: true, message: '请选择表名', trigger: 'change' }],
  domainId: [{ required: true, message: '业务域不能为空', trigger: 'change' }]
}

onMounted(() => {
  getTableProblemConfig().then(res => {
    if (res.data && res.data.windowDays != null) problemConfig.value = res.data
  }).catch(() => {})
})

function isProblemTable(row) {
  const n = row.errorCount || 0
  const th = problemConfig.value.errorCountThreshold ?? 3
  return n >= th
}

function formatTime(t) {
  if (!t) return '—'
  if (typeof t === 'string') return t
  const d = new Date(t)
  return isNaN(d.getTime()) ? '—' : d.toLocaleString('zh-CN')
}

function getAmbiguityLink(row) {
  const tableName = row?.tableName || ''
  return `#/bi/metadata?tab=ambiguity&tableName=${encodeURIComponent(tableName)}`
}

function goToAmbiguity(row) {
  const tableName = row?.tableName || ''
  router.push({ path: '/bi/metadata', query: { tab: 'ambiguity', tableName: tableName || undefined } })
}

function handleSelectionChange(selection) {
  selectedIds.value = selection.map(r => r.id).filter(Boolean)
}

function openBatchUsage() {
  batchUsage.value = ''
  batchUsageVisible.value = true
}

function openBatchVisibility() {
  batchVisibility.value = ''
  batchVisibilityVisible.value = true
}

function submitBatchUsage() {
  if (!batchUsage.value) {
    proxy.$modal.msgWarning('请选择用途')
    return
  }
  batchUpdateTableMetadata({ ids: selectedIds.value, tableUsage: batchUsage.value }).then(() => {
    proxy.$modal.msgSuccess('已更新，将影响后续 NL2SQL 推荐')
    batchUsageVisible.value = false
    getList()
  }).catch(() => {})
}

function submitBatchVisibility() {
  if (!batchVisibility.value) {
    proxy.$modal.msgWarning('请选择可见性')
    return
  }
  batchUpdateTableMetadata({ ids: selectedIds.value, nl2sqlVisibilityLevel: batchVisibility.value }).then(() => {
    proxy.$modal.msgSuccess('已更新，将影响后续 NL2SQL 推荐')
    batchVisibilityVisible.value = false
    getList()
  }).catch(() => {})
}

function openProblemConfig() {
  problemConfigForm.windowDays = problemConfig.value.windowDays ?? 30
  problemConfigForm.errorCountThreshold = problemConfig.value.errorCountThreshold ?? 3
  problemConfigVisible.value = true
}

function submitProblemConfig() {
  updateTableProblemConfig({
    windowDays: problemConfigForm.windowDays,
    errorCountThreshold: problemConfigForm.errorCountThreshold
  }).then(() => {
    proxy.$modal.msgSuccess('配置已更新')
    problemConfig.value = { ...problemConfigForm }
    problemConfigVisible.value = false
    getList()
  }).catch(() => {})
}

function saveRowUsage(row, value) {
  row._saving = true
  updateTableMetadataById(row.id, { tableUsage: value, updateTime: row.updateTime }).then(() => {
    proxy.$modal.msgSuccess('已更新，将影响后续 NL2SQL 推荐')
    row._saving = false
    getList()
  }).catch(err => {
    row._saving = false
    const msg = (err && err.msg) || (err && err.message) || ''
    if (String(msg).indexOf('已被他人修改') >= 0 || (err && err.code === 409))
      proxy.$modal.msgError('数据已被他人修改，请刷新后重试')
    else
      proxy.$modal.msgError(msg || '保存失败')
  })
}

function saveRowVisibility(row, value) {
  row._saving = true
  updateTableMetadataById(row.id, { nl2sqlVisibilityLevel: value, updateTime: row.updateTime }).then(() => {
    proxy.$modal.msgSuccess('已更新，将影响后续 NL2SQL 推荐')
    row._saving = false
    getList()
  }).catch(err => {
    row._saving = false
    const msg = (err && err.msg) || (err && err.message) || ''
    if (String(msg).indexOf('已被他人修改') >= 0 || (err && err.code === 409))
      proxy.$modal.msgError('数据已被他人修改，请刷新后重试')
    else
      proxy.$modal.msgError(msg || '保存失败')
  })
}

function loadTableOptions() {
  tableOptionsLoading.value = true
  return getLocalTableList().then(response => {
    tableOptions.value = response.data || []
    tableOptionsLoading.value = false
  }).catch(() => {
    tableOptionsLoading.value = false
  })
}

function handleTableChange(tableName) {
  const table = tableOptions.value.find(t => t.tableName === tableName)
  if (table && table.tableComment) {
    form.tableComment = table.tableComment
  }
}

watch(() => props.domainId, (newVal) => {
  queryParams.domainId = newVal
  form.domainId = newVal
  if (newVal) {
    getList()
  }
}, { immediate: true })

watch(() => props.initialTableName, (val) => {
  if (val != null && val !== '') {
    queryParams.tableName = val
    queryParams.pageNum = 1
    getList()
  }
}, { immediate: true })

function getList() {
  loading.value = true
  listTableMetadata(queryParams).then(response => {
    tableList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.tableName = undefined
  queryParams.tableComment = undefined
  queryParams.businessDescription = undefined
  queryParams.tableUsage = undefined
  queryParams.nl2sqlVisibilityLevel = undefined
  queryParams.domainId = props.domainId
  queryParams.pageNum = 1
  handleQuery()
}

function handleManageFields(row) {
  fieldDialogTableId.value = row.id
  fieldDialogTableName.value = row.tableName
  fieldDialogTableRow.value = row
  fieldDialogVisible.value = true
}

function handleFieldDialogClose() {
  fieldDialogTableId.value = null
  fieldDialogTableName.value = ''
  fieldDialogTableRow.value = null
  if (fieldListRef.value) {
    fieldListRef.value = null
  }
}

function handleAdd() {
  reset()
  form.domainId = props.domainId
  open.value = true
  title.value = '新增表元数据'
  loadTableOptions()
}

function handleUpdate(row) {
  reset()
  getTableMetadata(row.id).then(response => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '修改表元数据'
    loadTableOptions().then(() => {
      if (form.tableName && !tableOptions.value.some(t => t.tableName === form.tableName)) {
        tableOptions.value = [{ tableName: form.tableName, tableComment: form.tableComment }].concat(tableOptions.value)
      }
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除表元数据"' + row.tableName + '"？').then(() => {
    return delTableMetadata([row.id])
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.id) {
        updateTableMetadata(form).then(() => {
          proxy.$modal.msgSuccess('已更新，将影响后续 NL2SQL 推荐')
          open.value = false
          getList()
        }).catch(err => {
          const msg = (err && err.msg) || ''
          if (String(msg).indexOf('已被他人修改') >= 0 || (err && err.code === 409))
            proxy.$modal.msgError('数据已被他人修改，请刷新后重试')
        })
      } else {
        addTableMetadata(form).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        }).catch(() => {})
      }
    }
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.id = undefined
  form.tableName = ''
  form.tableComment = ''
  form.businessDescription = ''
  form.tableUsage = undefined
  form.nl2sqlVisibilityLevel = undefined
  form.grainDesc = ''
  form.domainId = props.domainId
  tableOptions.value = []
  proxy.resetForm('formRef')
}

defineExpose({
  getList
})
</script>

<style scoped>
.field-management-dialog {
  max-width: 1200px;
}
.field-management-dialog :deep(.el-dialog__body) {
  max-height: 70vh;
  overflow-y: auto;
  padding-top: 10px;
}
</style>

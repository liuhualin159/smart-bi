<template>
  <div>
    <el-form :inline="true" v-show="showSearch" class="aether-filter-card">
      <el-form-item>
        <el-input v-model="queryParams.name" placeholder="指标名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['metadata:metric:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="metricList" class="aether-table">
      <el-table-column label="指标名称" prop="name" />
      <el-table-column label="指标编码" prop="code" />
      <el-table-column label="指标表达式" prop="expression" />
      <el-table-column label="指标描述" prop="description" :show-overflow-tooltip="true" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['metadata:metric:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['metadata:metric:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 指标表单对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" @close="cancel">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="指标名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入指标名称" />
        </el-form-item>
        <el-form-item label="指标编码" prop="code" v-if="form.id">
          <el-input v-model="form.code" disabled placeholder="新增时自动生成" />
        </el-form-item>
        <el-form-item label="指标表达式" prop="expression">
          <div class="expression-config">
            <el-select v-model="exprConfig.tableId" placeholder="选择表" clearable filterable style="width: 180px" @change="onTableChange">
              <el-option v-for="t in tableOptions" :key="t.id" :label="t.tableName + (t.tableComment ? ' - ' + t.tableComment : '')" :value="t.id" />
            </el-select>
            <el-select v-model="exprConfig.aggFunc" placeholder="聚合方式" clearable style="width: 120px" @change="buildExpression">
              <el-option label="求和 SUM" value="SUM" />
              <el-option label="平均值 AVG" value="AVG" />
              <el-option label="计数 COUNT" value="COUNT" />
              <el-option label="最大值 MAX" value="MAX" />
              <el-option label="最小值 MIN" value="MIN" />
            </el-select>
            <el-select v-model="exprConfig.fieldName" placeholder="选择字段" clearable filterable style="width: 180px" @change="buildExpression">
              <el-option v-if="exprConfig.aggFunc === 'COUNT'" label="* (全部行)" value="*" />
              <el-option v-for="f in fieldOptions" :key="f.id" :label="f.fieldName + (f.fieldComment ? ' - ' + f.fieldComment : '')" :value="f.fieldName" />
            </el-select>
            <span class="expr-preview" v-if="form.expression">→ {{ form.expression }}</span>
          </div>
          <div class="expr-tip">基于当前业务域的表/字段元数据进行配置，请先在表管理中维护元数据</div>
        </el-form-item>
        <el-form-item label="指标描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入指标描述" />
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
  </div>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { getCurrentInstance } from 'vue'
import { listAtomicMetric, getAtomicMetric, addAtomicMetric, updateAtomicMetric, delAtomicMetric, listTableMetadata, listFieldMetadataByTable } from '@/api/metadata'

const props = defineProps({
  domainId: {
    type: Number,
    default: null
  }
})

const { proxy } = getCurrentInstance()
const formRef = ref(null)

const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const metricList = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  name: undefined,
  domainId: null
})

const form = reactive({
  id: undefined,
  name: '',
  code: '',
  expression: '',
  domainId: null,
  description: ''
})

const exprConfig = reactive({
  tableId: null,
  aggFunc: '',
  fieldName: ''
})

const tableOptions = ref([])
const fieldOptions = ref([])

const rules = {
  name: [{ required: true, message: '指标名称不能为空', trigger: 'blur' }],
  expression: [{ required: true, message: '请通过表/聚合/字段配置指标表达式', trigger: 'change' }],
  domainId: [{ required: true, message: '业务域不能为空', trigger: 'change' }]
}

function loadTableOptions() {
  if (!props.domainId) {
    tableOptions.value = []
    return
  }
  listTableMetadata({ domainId: props.domainId, pageNum: 1, pageSize: 500 }).then(res => {
    tableOptions.value = res.rows || []
  })
}

function onTableChange(tableId) {
  exprConfig.fieldName = ''
  fieldOptions.value = []
  if (tableId) {
    listFieldMetadataByTable(tableId).then(res => {
      fieldOptions.value = res.data || []
    })
  }
  buildExpression()
}

function buildExpression() {
  const { aggFunc, fieldName } = exprConfig
  if (!aggFunc || !fieldName) {
    form.expression = ''
    return
  }
  form.expression = aggFunc + '(' + fieldName + ')'
}

async function parseExpression(expr) {
  if (!expr) return false
  const m = expr.match(/^(SUM|AVG|COUNT|MAX|MIN)\((\*|[a-zA-Z0-9_]+)\)$/)
  if (!m) return false
  const [, aggFunc, fieldName] = m
  exprConfig.aggFunc = aggFunc
  exprConfig.fieldName = fieldName
  if (fieldName === '*') {
    exprConfig.tableId = tableOptions.value[0]?.id || null
    if (exprConfig.tableId) {
      const res = await listFieldMetadataByTable(exprConfig.tableId)
      fieldOptions.value = res.data || []
    }
    return true
  }
  for (const t of tableOptions.value) {
    const res = await listFieldMetadataByTable(t.id)
    const fields = res.data || []
    if (fields.some(f => f.fieldName === fieldName)) {
      exprConfig.tableId = t.id
      fieldOptions.value = fields
      return true
    }
  }
  exprConfig.tableId = null
  fieldOptions.value = []
  return true
}

watch(() => props.domainId, (newVal) => {
  queryParams.domainId = newVal
  form.domainId = newVal
  if (newVal) {
    getList()
  }
}, { immediate: true })

function getList() {
  loading.value = true
  listAtomicMetric(queryParams).then(response => {
    metricList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAdd() {
  reset()
  form.domainId = props.domainId
  open.value = true
  title.value = '新增原子指标'
  loadTableOptions()
}

async function handleUpdate(row) {
  reset()
  form.domainId = props.domainId
  open.value = true
  title.value = '修改原子指标'
  const tablesRes = await listTableMetadata({ domainId: props.domainId || form.domainId, pageNum: 1, pageSize: 500 })
  tableOptions.value = tablesRes.rows || []
  const metricRes = await getAtomicMetric(row.id)
  Object.assign(form, metricRes.data)
  const parsed = await parseExpression(form.expression)
  if (!parsed) {
    exprConfig.aggFunc = ''
    exprConfig.tableId = null
    exprConfig.fieldName = ''
    fieldOptions.value = []
  }
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除原子指标"' + row.name + '"？').then(() => {
    return delAtomicMetric([row.id])
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.id) {
        updateAtomicMetric(form).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          open.value = false
          getList()
        }).catch(() => {
          // 错误已在API层处理
        })
      } else {
        addAtomicMetric(form).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          open.value = false
          getList()
        }).catch(() => {
          // 错误已在API层处理
        })
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
  form.name = ''
  form.code = ''
  form.expression = ''
  form.domainId = props.domainId
  form.description = ''
  exprConfig.tableId = null
  exprConfig.aggFunc = ''
  exprConfig.fieldName = ''
  tableOptions.value = []
  fieldOptions.value = []
  proxy.resetForm('formRef')
}

defineExpose({
  getList
})
</script>

<style scoped>
.expression-config {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.expression-config .el-select {
  flex-shrink: 0;
}
.expr-preview {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.expr-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 6px;
}
</style>

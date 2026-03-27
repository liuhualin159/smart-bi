<template>
  <div>
    <el-form ref="queryRef" :inline="true" v-show="showSearch" class="aether-filter-card">
      <el-form-item>
        <el-input v-model="queryParams.fieldName" placeholder="字段名" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['metadata:field:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="fieldList" class="aether-table">
      <el-table-column label="字段名" prop="fieldName" min-width="100" />
      <el-table-column label="类型" prop="fieldType" width="90" show-overflow-tooltip />
      <el-table-column label="注释" prop="fieldComment" width="100" show-overflow-tooltip />
      <el-table-column label="用途" width="100" align="center">
        <template #default="scope">
          <el-select v-model="scope.row.usageType" placeholder="—" clearable size="small" style="width:100%" :loading="scope.row._saving" @change="v => saveRow(scope.row, 'usageType', v)">
            <el-option v-for="d in bi_field_usage_type" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="语义类型" width="100" align="center">
        <template #default="scope">
          <el-select v-model="scope.row.semanticType" placeholder="—" clearable size="small" style="width:100%" :loading="scope.row._saving" @change="v => saveRow(scope.row, 'semanticType', v)">
            <el-option v-for="o in semanticOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="单位" prop="unit" width="70" show-overflow-tooltip />
      <el-table-column label="默认聚合" width="100" align="center">
        <template #default="scope">
          <el-select v-model="scope.row.defaultAggFunc" placeholder="—" clearable size="small" style="width:100%" :loading="scope.row._saving" @change="v => saveRow(scope.row, 'defaultAggFunc', v)">
            <el-option v-for="o in aggOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="优先级" width="80" align="center">
        <template #default="scope">
          <el-input-number v-model="scope.row.nl2sqlPriority" :min="1" :max="10" size="small" controls-position="right" style="width:100%" @change="v => saveRow(scope.row, 'nl2sqlPriority', v)" />
        </template>
      </el-table-column>
      <el-table-column label="敏感" width="80" align="center">
        <template #default="scope">
          <el-select v-model="scope.row.sensitiveLevel" placeholder="—" clearable size="small" style="width:100%" :loading="scope.row._saving" @change="v => saveRow(scope.row, 'sensitiveLevel', v)">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="曝光策略" width="100" align="center">
        <template #default="scope">
          <el-select v-model="scope.row.exposurePolicy" placeholder="—" clearable size="small" style="width:100%" :loading="scope.row._saving" @change="v => saveRow(scope.row, 'exposurePolicy', v)">
            <el-option v-for="d in bi_exposure_policy" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="别名" min-width="160">
        <template #default="scope">
          <div class="alias-cell">
            <el-tag v-for="a in (scope.row.aliases || [])" :key="a.id" size="small" closable @close="removeAlias(scope.row, a)">
              {{ a.alias }}<span v-if="a.source" class="alias-source">{{ a.source }}</span>
            </el-tag>
            <el-dropdown trigger="click" @command="cmd => addAliasFromSuggest(scope.row, cmd)">
              <el-button size="small" type="primary" link icon="Plus">添加</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="__input__">手动输入</el-dropdown-item>
                  <el-dropdown-item v-for="s in (aliasSuggestions[scope.row.id] || [])" :key="s.value" :command="s.value">{{ s.value }} <span v-if="s.source" class="text-secondary">({{ s.source }})</span></el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-tooltip v-if="(conflictByFieldId[scope.row.id] || 0) > 0" content="存在别名冲突，点击查看" placement="top">
              <el-icon class="conflict-icon" color="var(--el-color-danger)" @click="openConflictPopup(scope.row)"><WarningFilled /></el-icon>
            </el-tooltip>
            <el-button v-else-if="(scope.row.aliases || []).length > 0" link type="primary" size="small" @click="openConflictPopup(scope.row)">查冲突</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="业务描述" prop="businessDescription" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['metadata:field:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['metadata:field:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 手动输入别名 -->
    <el-dialog v-model="aliasInputVisible" title="添加别名" width="400px">
      <el-form label-width="80px">
        <el-form-item label="别名">
          <el-input v-model="aliasInputValue" placeholder="请输入别名" @keyup.enter="submitAliasInput" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aliasInputVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAliasInput">确定</el-button>
      </template>
    </el-dialog>

    <!-- 别名冲突列表 -->
    <el-dialog v-model="conflictPopupVisible" title="别名冲突" width="500px">
      <p class="conflict-desc">以下表/字段也使用了相同别名，可能影响 NL2SQL 理解：</p>
      <el-table :data="conflictList" size="small" class="aether-table">
        <el-table-column property="tableName" label="表名" />
        <el-table-column property="fieldName" label="字段名" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="goToTable(scope.row)">去表管理</el-button>
            <el-button link type="primary" size="small" @click="goToField(scope.row)">去字段</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 字段元数据表单对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" @close="cancel">
      <div class="field-form-scroll">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="字段名" prop="fieldName">
          <el-select
            v-model="form.fieldName"
            placeholder="请选择字段"
            clearable
            filterable
            style="width: 100%"
            :loading="columnOptionsLoading"
            :disabled="!canLoadColumns"
            @change="handleFieldChange"
          >
            <el-option
              v-for="col in columnOptions"
              :key="col.columnName"
              :label="col.columnName + (col.columnComment ? ' - ' + col.columnComment : '') + ' (' + (col.dataType || '') + ')'"
              :value="col.columnName"
            />
          </el-select>
          <div v-if="!canLoadColumns" class="form-tip">请确保已选择表</div>
        </el-form-item>
        <el-form-item label="字段类型" prop="fieldType">
          <el-input v-model="form.fieldType" placeholder="可从数据源自动带出" />
        </el-form-item>
        <el-form-item label="字段注释" prop="fieldComment">
          <el-input v-model="form.fieldComment" placeholder="请输入字段注释" />
        </el-form-item>
        <el-form-item label="业务别名" prop="businessAlias">
          <el-input v-model="form.businessAlias" placeholder="请输入业务别名" />
        </el-form-item>
        <el-form-item label="业务描述" prop="businessDescription">
          <el-input v-model="form.businessDescription" type="textarea" :rows="4" placeholder="请输入业务描述（最多1000字）" />
        </el-form-item>
        <el-form-item label="枚举释义" prop="enumValues">
          <el-input
            v-model="form.enumValues"
            type="textarea"
            :rows="3"
            placeholder='可选。JSON 格式：显示值→存储值映射，供 NL2SQL 将用户说的文本转为 SQL 条件。如：[{"label":"已支付","value":"1"},{"label":"待支付","value":"0"}]'
          />
        </el-form-item>
        <el-form-item label="是否敏感" prop="isSensitive">
          <el-radio-group v-model="form.isSensitive">
            <el-radio :label="true">是</el-radio>
            <el-radio :label="false">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="脱敏规则" prop="desensitizeRule" v-if="form.isSensitive">
          <el-input v-model="form.desensitizeRule" placeholder="请输入脱敏规则" />
        </el-form-item>
        <el-form-item label="用途" prop="usageType">
          <el-select v-model="form.usageType" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="d in bi_field_usage_type" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="语义类型" prop="semanticType">
          <el-select v-model="form.semanticType" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="o in semanticOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：元、次、人" clearable />
        </el-form-item>
        <el-form-item label="默认聚合" prop="defaultAggFunc">
          <el-select v-model="form.defaultAggFunc" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="o in aggOptions" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="NL2SQL优先级" prop="nl2sqlPriority">
          <el-input-number v-model="form.nl2sqlPriority" :min="1" :max="10" placeholder="1-10" style="width:100%" />
        </el-form-item>
        <el-form-item label="敏感级别" prop="sensitiveLevel">
          <el-select v-model="form.sensitiveLevel" placeholder="请选择" clearable style="width:100%">
            <el-option label="低" value="LOW" />
            <el-option label="中" value="MEDIUM" />
            <el-option label="高" value="HIGH" />
          </el-select>
        </el-form-item>
        <el-form-item label="曝光策略" prop="exposurePolicy">
          <el-select v-model="form.exposurePolicy" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="d in bi_exposure_policy" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
      </el-form>
      </div>
      <template #footer>
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import { listFieldMetadataByTable, getFieldMetadata, addFieldMetadata, updateFieldMetadata, updateFieldMetadataById, delFieldMetadata, getTableMetadata, addFieldAlias, removeFieldAlias, getFieldAliasSuggestions, getAliasConflicts } from '@/api/metadata'
import { getLocalColumnList } from '@/api/datasource'
import { WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  tableId: {
    type: Number,
    required: true
  },
  tableRow: {
    type: Object,
    default: null
  }
})

const { proxy } = getCurrentInstance()
const formRef = ref(null)
const { bi_field_usage_type, bi_exposure_policy } = proxy.useDict('bi_field_usage_type', 'bi_exposure_policy')

const semanticOptions = [
  { label: 'ID', value: 'ID' },
  { label: '名称', value: 'NAME' },
  { label: '编码', value: 'CODE' },
  { label: '状态/枚举', value: 'STATUS' },
  { label: '时间', value: 'TIME' },
  { label: '金额', value: 'AMOUNT' },
  { label: '数量', value: 'COUNT' },
  { label: '比率', value: 'RATIO' },
  { label: '标志', value: 'FLAG' }
]
const aggOptions = [
  { label: 'SUM', value: 'SUM' },
  { label: 'COUNT', value: 'COUNT' },
  { label: 'COUNT_DISTINCT', value: 'COUNT_DISTINCT' },
  { label: 'AVG', value: 'AVG' },
  { label: 'MAX', value: 'MAX' },
  { label: 'MIN', value: 'MIN' }
]

const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const fieldList = ref([])
const aliasSuggestions = ref({})
const aliasInputVisible = ref(false)
const aliasInputValue = ref('')
const aliasInputRow = ref(null)
const conflictPopupVisible = ref(false)
const conflictList = ref([])
const conflictCurrentAlias = ref('')
const conflictExcludeFieldId = ref(null)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  fieldName: undefined,
  tableId: null
})

const form = reactive({
  id: undefined,
  tableId: null,
  fieldName: '',
  fieldType: '',
  fieldComment: '',
  businessAlias: '',
  businessDescription: '',
  isSensitive: false,
  desensitizeRule: '',
  usageType: undefined,
  semanticType: undefined,
  enumValues: '',
  unit: '',
  defaultAggFunc: undefined,
  nl2sqlPriority: undefined,
  sensitiveLevel: undefined,
  exposurePolicy: undefined
})

const columnOptions = ref([])
const columnOptionsLoading = ref(false)

const effectiveTableInfo = ref(null)

const canLoadColumns = computed(() => {
  const row = props.tableRow || effectiveTableInfo.value
  return row && row.tableName
})

const rules = {
  fieldName: [{ required: true, message: '请选择字段名', trigger: 'change' }],
  tableId: [{ required: true, message: '表ID不能为空', trigger: 'change' }]
}

function loadColumnOptions() {
  const row = props.tableRow || effectiveTableInfo.value
  if (!row || !row.tableName) {
    if (props.tableId && !effectiveTableInfo.value) {
      getTableMetadata(props.tableId).then(res => {
        effectiveTableInfo.value = res.data
        doLoadColumns(res.data)
      })
    } else {
      columnOptions.value = []
    }
    return
  }
  doLoadColumns(row)
}

function doLoadColumns(row) {
  if (!row || !row.tableName) {
    columnOptions.value = []
    return
  }
  columnOptionsLoading.value = true
  getLocalColumnList(row.tableName).then(response => {
    columnOptions.value = response.data || []
    columnOptionsLoading.value = false
  }).catch(() => {
    columnOptionsLoading.value = false
  })
}

function handleFieldChange(fieldName) {
  const col = columnOptions.value.find(c => c.columnName === fieldName)
  if (col) {
    form.fieldType = col.dataType || ''
    form.fieldComment = col.columnComment || ''
  }
}

watch(() => props.tableId, (newVal) => {
  queryParams.tableId = newVal
  form.tableId = newVal
  effectiveTableInfo.value = props.tableRow || null
  if (newVal) {
    getList()
  }
}, { immediate: true })

watch(() => props.tableRow, (newVal) => {
  effectiveTableInfo.value = newVal || null
}, { immediate: true })

function getList() {
  loading.value = true
  listFieldMetadataByTable(props.tableId).then(response => {
    fieldList.value = response.data || []
    total.value = fieldList.value.length
    loading.value = false
    fieldList.value.forEach(row => {
      if (row.id) loadSuggestions(row.id)
    })
  })
}

function loadSuggestions(fieldId) {
  getFieldAliasSuggestions(fieldId).then(res => {
    const list = res.data || []
    aliasSuggestions.value = { ...aliasSuggestions.value, [fieldId]: list }
  }).catch(() => {})
}

function saveRow(row, field, value) {
  row._saving = true
  const payload = { updateTime: row.updateTime, [field]: value }
  updateFieldMetadataById(row.id, payload).then(() => {
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

function removeAlias(row, a) {
  removeFieldAlias(row.id, a.id).then(() => {
    proxy.$modal.msgSuccess('已删除别名')
    getList()
  }).catch(() => {})
}

function addAliasFromSuggest(row, cmd) {
  if (cmd === '__input__') {
    aliasInputRow.value = row
    aliasInputValue.value = ''
    aliasInputVisible.value = true
    return
  }
  if (!cmd) return
  addFieldAlias(row.id, { alias: cmd, source: 'HUMAN' }).then(() => {
    proxy.$modal.msgSuccess('已添加别名')
    getList()
  }).catch(e => {
    proxy.$modal.msgError((e && e.msg) || e.message || '添加失败')
  })
}

function submitAliasInput() {
  const row = aliasInputRow.value
  const v = (aliasInputValue.value || '').trim()
  if (!row || !v) {
    proxy.$modal.msgWarning('请输入别名')
    return
  }
  aliasInputVisible.value = false
  addFieldAlias(row.id, { alias: v, source: 'HUMAN' }).then(() => {
    proxy.$modal.msgSuccess('已添加别名')
    getList()
  }).catch(e => {
    proxy.$modal.msgError((e && e.msg) || e.message || '添加失败，可能已存在相同别名')
  })
}

const conflictByFieldId = ref({})
function hasAliasConflict(row) {
  return (conflictByFieldId.value[row.id] || 0) > 0
}

function openConflictPopup(row) {
  if (!row.aliases || row.aliases.length === 0) return
  const firstAlias = row.aliases[0].alias
  getAliasConflicts(firstAlias, row.id).then(res => {
    conflictList.value = res.data || []
    conflictByFieldId.value[row.id] = (res.data || []).length
    conflictCurrentAlias.value = firstAlias
    conflictExcludeFieldId.value = row.id
    conflictPopupVisible.value = true
  })
}

function goToTable(conflictRow) {
  conflictPopupVisible.value = false
  proxy.$router.push({ path: '/bi/metadata', query: { highlightTableId: conflictRow.tableId } })
}

function goToField(conflictRow) {
  conflictPopupVisible.value = false
  proxy.$router.push({ path: '/bi/metadata', query: { highlightTableId: conflictRow.tableId, highlightFieldId: conflictRow.fieldId } })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.fieldName = undefined
  queryParams.tableId = props.tableId
  queryParams.pageNum = 1
  handleQuery()
}

function handleAdd() {
  reset()
  form.tableId = props.tableId
  open.value = true
  title.value = '新增字段元数据'
  loadColumnOptions()
}

function handleUpdate(row) {
  reset()
  getFieldMetadata(row.id).then(response => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '修改字段元数据'
    loadColumnOptions()
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除字段元数据"' + row.fieldName + '"？').then(() => {
    return delFieldMetadata([row.id])
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.id) {
        updateFieldMetadata(form).then(() => {
          proxy.$modal.msgSuccess('已更新，将影响后续 NL2SQL 推荐')
          open.value = false
          getList()
        }).catch(err => {
          const msg = (err && err.msg) || ''
          if (String(msg).indexOf('已被他人修改') >= 0 || (err && err.code === 409))
            proxy.$modal.msgError('数据已被他人修改，请刷新后重试')
        })
      } else {
        addFieldMetadata(form).then(() => {
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
  form.tableId = props.tableId
  form.fieldName = ''
  form.fieldType = ''
  form.fieldComment = ''
  form.businessAlias = ''
  form.businessDescription = ''
  form.isSensitive = false
  form.desensitizeRule = ''
  form.usageType = undefined
  form.semanticType = undefined
  form.enumValues = ''
  form.unit = ''
  form.defaultAggFunc = undefined
  form.nl2sqlPriority = undefined
  form.sensitiveLevel = undefined
  form.exposurePolicy = undefined
  columnOptions.value = []
  proxy.resetForm('formRef')
}

defineExpose({
  getList
})
</script>

<style scoped>
.field-form-scroll {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
}
.form-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.alias-source {
  font-size: 10px;
  color: var(--el-text-color-secondary);
  margin-left: 4px;
}
.conflict-icon {
  cursor: pointer;
  margin-left: 4px;
  vertical-align: middle;
}
.text-secondary {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.conflict-desc {
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.alias-cell {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}
</style>

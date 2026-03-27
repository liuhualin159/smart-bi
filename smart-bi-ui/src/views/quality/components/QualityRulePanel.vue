<template>
  <div class="quality-rule-panel">
    <el-form :inline="true" class="search-form">
      <el-form-item label="表">
        <el-select v-model="queryParams.tableId" placeholder="全部" clearable style="width: 200px" @change="loadList">
          <el-option
            v-for="t in tableList"
            :key="t.id"
            :label="t.tableComment || t.tableName"
            :value="t.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="规则类型">
        <el-select v-model="queryParams.ruleType" placeholder="全部" clearable style="width: 140px" @change="loadList">
          <el-option label="完整性" value="COMPLETENESS" />
          <el-option label="准确性" value="ACCURACY" />
          <el-option label="一致性" value="CONSISTENCY" />
          <el-option label="唯一性" value="UNIQUENESS" />
          <el-option label="及时性" value="TIMELINESS" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">查询</el-button>
        <el-button icon="Plus" @click="openDrawer()" v-hasPermi="['bi:quality:add']">新增规则</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="ruleList" border class="aether-table">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="表" width="140">
        <template #default="{ row }">
          {{ getTableName(row.tableId) }}
        </template>
      </el-table-column>
      <el-table-column label="规则类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ ruleTypeLabel(row.ruleType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="配置" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatConfigSummary(row.ruleType, row.ruleConfig) }}
        </template>
      </el-table-column>
      <el-table-column label="优先级" prop="priority" width="80" />
      <el-table-column label="严重性权重" prop="severityWeight" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === '0' ? 'success' : 'info'" size="small">
            {{ row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="executeRule(row)" v-hasPermi="['bi:quality:list']">执行</el-button>
          <el-button link type="primary" size="small" @click="openDrawer(row)" v-hasPermi="['bi:quality:edit']">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)" v-hasPermi="['bi:quality:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="loadList" />

    <el-drawer v-model="drawerVisible" :title="editingRule?.id ? '编辑规则' : '新增规则'" size="480px" destroy-on-close @close="drawerClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="表" prop="tableId">
          <el-select v-model="form.tableId" placeholder="请选择表" style="width: 100%" :disabled="!!editingRule?.id" @change="onTableChange">
            <el-option v-for="t in tableList" :key="t.id" :label="t.tableComment || t.tableName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择" style="width: 100%">
            <el-option label="完整性（非空）" value="COMPLETENESS" />
            <el-option label="准确性（格式/范围）" value="ACCURACY" />
            <el-option label="一致性（引用）" value="CONSISTENCY" />
            <el-option label="唯一性" value="UNIQUENESS" />
            <el-option label="及时性" value="TIMELINESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则配置">
          <RuleConfigEditor
            ref="configEditorRef"
            v-model:rule-config="form.ruleConfig"
            :table-id="form.tableId"
            :rule-type="form.ruleType"
            :table-list="tableList"
          />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" />
        </el-form-item>
        <el-form-item label="严重性权重" prop="severityWeight">
          <el-input-number v-model="form.severityWeight" :min="1" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, getCurrentInstance } from 'vue'
import { listQualityRule, addQualityRule, updateQualityRule, delQualityRule, executeQualityRule } from '@/api/quality'
import RuleConfigEditor from './RuleConfigEditor.vue'

const { proxy } = getCurrentInstance()
const props = defineProps({ tableList: { type: Array, default: () => [] } })
const emit = defineEmits(['refresh'])

const loading = ref(false)
const configEditorRef = ref(null)
const ruleList = ref([])
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, tableId: null, ruleType: null })
const drawerVisible = ref(false)
const editingRule = ref(null)
const formRef = ref(null)
const form = reactive({
  tableId: null,
  ruleType: 'COMPLETENESS',
  ruleConfig: '{}',
  priority: 0,
  severityWeight: 1,
  status: '0'
})
const rules = {
  tableId: [{ required: true, message: '请选择表', trigger: 'change' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }]
}

function onTableChange() {
  form.ruleConfig = '{}'
}

const RULE_LABELS = { COMPLETENESS: '完整性', ACCURACY: '准确性', CONSISTENCY: '一致性', UNIQUENESS: '唯一性', TIMELINESS: '及时性' }
function ruleTypeLabel(t) { return RULE_LABELS[t] || t }

function formatConfigSummary(ruleType, ruleConfig) {
  if (!ruleConfig) return '—'
  try {
    const c = typeof ruleConfig === 'string' ? JSON.parse(ruleConfig) : ruleConfig
    const fieldArr = Array.isArray(c.fields) ? c.fields : (c.field ? [c.field] : [])
    const fieldStr = fieldArr.length > 0 ? `字段: ${fieldArr.join('、')}` : ''
    if (ruleType === 'COMPLETENESS' || ruleType === 'UNIQUENESS') return fieldStr || '—'
    const field = c.field ? `字段: ${c.field}` : ''
    if (ruleType === 'ACCURACY') return [field, c.pattern ? `正则: ${c.pattern}` : null, (c.min != null || c.max != null) ? `范围: [${c.min ?? '∞'}, ${c.max ?? '∞'}]` : null].filter(Boolean).join(' · ') || '—'
    if (ruleType === 'CONSISTENCY') return [field, c.refTable ? `参考: ${c.refTable}.${c.refField || '?'}` : null].filter(Boolean).join(' → ') || '—'
    if (ruleType === 'TIMELINESS') return [field, c.maxAgeHours != null ? `${c.maxAgeHours}h 内` : null].filter(Boolean).join(' · ') || '—'
  } catch (_) {}
  return ruleConfig
}
function getTableName(id) { return props.tableList.find(t => t.id === id)?.tableComment || props.tableList.find(t => t.id === id)?.tableName || '-' }

async function loadList() {
  loading.value = true
  try {
    const res = await listQualityRule(queryParams)
    ruleList.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function openDrawer(row) {
  editingRule.value = row || null
  if (row) {
    Object.assign(form, { ...row, status: row.status || '0' })
  } else {
    Object.assign(form, { tableId: null, ruleType: 'COMPLETENESS', ruleConfig: '{}', priority: 0, severityWeight: 1, status: '0' })
  }
  drawerVisible.value = true
}

function drawerClose() {
  editingRule.value = null
}

async function submitForm() {
  // 提交前从配置器同步最新配置，避免 v-model 未及时更新
  if (configEditorRef.value) {
    if (!configEditorRef.value.validate()) {
      proxy.$modal.msgWarning('请完成规则配置（选择校验字段等）')
      return
    }
    form.ruleConfig = configEditorRef.value.buildConfig()
  }
  await formRef.value?.validate()
  try {
    if (editingRule.value?.id) {
      await updateQualityRule(form)
      proxy.$modal.msgSuccess('修改成功')
    } else {
      await addQualityRule(form)
      proxy.$modal.msgSuccess('新增成功')
    }
    drawerVisible.value = false
    loadList()
    emit('refresh')
  } catch (e) {
    proxy.$modal.msgError(e.msg || e.message || '操作失败')
  }
}

async function executeRule(row) {
  try {
    const res = await executeQualityRule(row.id)
    const r = res.data || res
    proxy.$modal.msg(r.passed ? '通过' : `未通过：${r.message || ''}`)
    loadList()
  } catch (e) {
    proxy.$modal.msgError(e.msg || e.message || '执行失败')
  }
}

async function handleDelete(row) {
  try {
    await proxy.$modal.confirm('确定删除该规则？')
    await delQualityRule(row.id)
    proxy.$modal.msgSuccess('删除成功')
    loadList()
    emit('refresh')
  } catch (e) {
    if (e !== 'cancel') proxy.$modal.msgError(e.msg || '删除失败')
  }
}

onMounted(loadList)
</script>

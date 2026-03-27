<template>
  <div class="rule-config-editor">
    <div class="config-card" :class="{ 'config-card--empty': !tableId }">
      <template v-if="!tableId">
        <el-empty description="请先选择要校验的表" :image-size="48" />
      </template>
      <template v-else>
        <!-- 完整性 / 唯一性：支持多字段 -->
        <template v-if="ruleType === 'COMPLETENESS' || ruleType === 'UNIQUENESS'">
          <div class="config-hint">
            <span v-if="ruleType === 'COMPLETENESS'">检查所选字段是否为空，任一字段为空则视为不合格；空字符串视为不合格</span>
            <span v-else>检查所选字段组合是否唯一，可多选实现联合唯一约束</span>
          </div>
          <el-form-item :label="ruleType === 'COMPLETENESS' ? '校验字段' : '唯一性字段'" prop="fields">
            <el-select
              v-model="config.fields"
              multiple
              placeholder="可多选，至少选一个"
              filterable
              collapse-tags
              collapse-tags-tooltip
              style="width: 100%"
              @change="emitConfig"
            >
              <el-option v-for="f in fieldList" :key="f.fieldName" :label="f.fieldComment || f.fieldName" :value="f.fieldName" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 准确性：格式或范围 -->
        <template v-else-if="ruleType === 'ACCURACY'">
          <div class="config-hint">校验字段值是否符合格式或数值范围</div>
          <el-form-item label="校验字段" prop="field">
            <el-select v-model="config.field" placeholder="请选择字段" filterable style="width: 100%" @change="emitConfig">
              <el-option v-for="f in fieldList" :key="f.fieldName" :label="f.fieldComment || f.fieldName" :value="f.fieldName" />
            </el-select>
          </el-form-item>
          <el-form-item label="校验方式">
            <el-radio-group v-model="accuracyMode" @change="onAccuracyModeChange">
              <el-radio-button value="pattern">正则格式</el-radio-button>
              <el-radio-button value="range">数值范围</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="accuracyMode === 'pattern'" label="正则表达式" prop="pattern">
            <el-input
              v-model="config.pattern"
              placeholder="如 ^\d+$ 表示纯数字"
              clearable
              @input="emitConfig"
            >
              <template #append>
                <el-tooltip content="MySQL REGEXP 语法" placement="top">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </template>
            </el-input>
          </el-form-item>
          <template v-else-if="accuracyMode === 'range'">
            <el-form-item label="最小值" prop="min">
              <el-input-number v-model="config.min" :precision="4" placeholder="可选" clearable style="width: 100%" @change="emitConfig" />
            </el-form-item>
            <el-form-item label="最大值" prop="max">
              <el-input-number v-model="config.max" :precision="4" placeholder="可选" clearable style="width: 100%" @change="emitConfig" />
            </el-form-item>
          </template>
        </template>

        <!-- 一致性：外键引用 -->
        <template v-else-if="ruleType === 'CONSISTENCY'">
          <div class="config-hint">检查本表字段的值是否都存在于参考表中</div>
          <el-form-item label="本表字段" prop="field">
            <el-select v-model="config.field" placeholder="请选择" filterable style="width: 100%" @change="emitConfig">
              <el-option v-for="f in fieldList" :key="f.fieldName" :label="f.fieldComment || f.fieldName" :value="f.fieldName" />
            </el-select>
          </el-form-item>
          <el-form-item label="参考表" prop="refTable">
            <el-select v-model="config.refTable" placeholder="请选择" filterable style="width: 100%" @change="onRefTableChange">
              <el-option v-for="t in tableList" :key="t.id" :label="t.tableComment || t.tableName" :value="t.tableName" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="config.refTable" label="参考字段" prop="refField">
            <el-select v-model="config.refField" placeholder="请选择" filterable style="width: 100%" @change="emitConfig">
              <el-option v-for="f in refFieldList" :key="f.fieldName" :label="f.fieldComment || f.fieldName" :value="f.fieldName" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 及时性 -->
        <template v-else-if="ruleType === 'TIMELINESS'">
          <div class="config-hint">检查时间字段的数据是否在指定时间内更新</div>
          <el-form-item label="时间字段" prop="field">
            <el-select v-model="config.field" placeholder="请选择日期/时间字段" filterable style="width: 100%" @change="emitConfig">
              <el-option v-for="f in fieldList" :key="f.fieldName" :label="f.fieldComment || f.fieldName" :value="f.fieldName" />
            </el-select>
          </el-form-item>
          <el-form-item label="最大时效(小时)" prop="maxAgeHours">
            <el-input-number v-model="config.maxAgeHours" :min="1" :max="8760" placeholder="如 24 表示 24 小时内" style="width: 100%" @change="emitConfig" />
          </el-form-item>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { listFieldMetadataByTable } from '@/api/metadata'
import { QuestionFilled } from '@element-plus/icons-vue'

const props = defineProps({
  tableId: { type: Number, default: null },
  ruleType: { type: String, default: 'COMPLETENESS' },
  ruleConfig: { type: String, default: '' },
  tableList: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:ruleConfig', 'change'])

const fieldList = ref([])
const refFieldList = ref([])
const accuracyMode = ref('pattern')

const config = ref({
  field: '',
  fields: [],
  pattern: '',
  min: undefined,
  max: undefined,
  refTable: '',
  refField: '',
  maxAgeHours: 24
})

const currentTable = computed(() => props.tableList.find(t => t.id === props.tableId))

function parseConfig(str) {
  if (!str || typeof str !== 'string') return {}
  try {
    return JSON.parse(str) || {}
  } catch {
    return {}
  }
}

function buildConfig() {
  const cfg = { ...config.value }
  const out = {}
  if (props.ruleType === 'COMPLETENESS' || props.ruleType === 'UNIQUENESS') {
    const arr = Array.isArray(cfg.fields) ? cfg.fields.filter(Boolean) : []
    if (arr.length > 0) out.fields = arr
    else if (cfg.field) out.field = cfg.field
  } else if (cfg.field) out.field = cfg.field
  if (props.ruleType === 'ACCURACY') {
    if (accuracyMode.value === 'pattern' && cfg.pattern) out.pattern = cfg.pattern
    else if (accuracyMode.value === 'range') {
      if (cfg.min != null && cfg.min !== '') out.min = Number(cfg.min)
      if (cfg.max != null && cfg.max !== '') out.max = Number(cfg.max)
    }
  } else if (props.ruleType === 'CONSISTENCY') {
    if (cfg.refTable) out.refTable = cfg.refTable
    if (cfg.refField) out.refField = cfg.refField
  } else if (props.ruleType === 'TIMELINESS') {
    if (cfg.maxAgeHours != null) out.maxAgeHours = Number(cfg.maxAgeHours)
  }
  return JSON.stringify(out)
}

function emitConfig() {
  const json = buildConfig()
  emit('update:ruleConfig', json)
  emit('change', json)
}

async function loadFields(tableId) {
  if (!tableId) {
    fieldList.value = []
    return
  }
  try {
    const res = await listFieldMetadataByTable(tableId)
    fieldList.value = res.data || res || []
  } catch {
    fieldList.value = []
  }
}

async function loadRefFields(refTableName) {
  if (!refTableName) {
    refFieldList.value = []
    return
  }
  const t = props.tableList.find(x => x.tableName === refTableName)
  if (!t?.id) {
    refFieldList.value = []
    return
  }
  try {
    const res = await listFieldMetadataByTable(t.id)
    refFieldList.value = res.data || res || []
  } catch {
    refFieldList.value = []
  }
}

function onRefTableChange() {
  config.value.refField = ''
  loadRefFields(config.value.refTable).then(() => emitConfig())
}

function onAccuracyModeChange() {
  if (accuracyMode.value === 'pattern') {
    config.value.min = undefined
    config.value.max = undefined
  } else {
    config.value.pattern = ''
  }
  emitConfig()
}

function initFromProp() {
  const parsed = parseConfig(props.ruleConfig)
  const fields = parsed.fields
  const fieldArr = Array.isArray(fields) ? fields : (parsed.field ? [parsed.field] : [])
  config.value = {
    field: parsed.field || '',
    fields: [...fieldArr],
    pattern: parsed.pattern || '',
    min: parsed.min,
    max: parsed.max,
    refTable: parsed.refTable || '',
    refField: parsed.refField || '',
    maxAgeHours: parsed.maxAgeHours ?? 24
  }
  if (props.ruleType === 'ACCURACY') {
    accuracyMode.value = parsed.pattern != null && parsed.pattern !== '' ? 'pattern' : (parsed.min != null || parsed.max != null ? 'range' : 'pattern')
  }
  if (parsed.refTable) loadRefFields(parsed.refTable)
}

watch(() => props.tableId, (id) => {
  loadFields(id)
  if (!id) {
    config.value.field = ''
    config.value.fields = []
  }
}, { immediate: true })

watch(() => props.ruleType, () => {
  config.value = { field: '', fields: [], pattern: '', min: undefined, max: undefined, refTable: '', refField: '', maxAgeHours: 24 }
  accuracyMode.value = 'pattern'
  initFromProp()
  emitConfig()
}, { immediate: false })

watch(() => props.ruleConfig, () => initFromProp(), { immediate: true })

onMounted(() => {
  loadFields(props.tableId)
  if (props.ruleConfig) initFromProp()
})

function hasValidFields() {
  const c = config.value
  const arr = Array.isArray(c.fields) ? c.fields.filter(Boolean) : []
  return arr.length > 0 || !!c.field
}

defineExpose({
  buildConfig,
  validate: () => hasValidFields()
})
</script>

<style scoped>
.rule-config-editor {
  width: 100%;
}
.config-card {
  padding: 12px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border: 1px solid var(--el-border-color-lighter);
}
.config-card--empty {
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.config-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
  line-height: 1.4;
}
.rule-config-editor :deep(.el-form-item) {
  margin-bottom: 14px;
}
.rule-config-editor :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
</style>

<template>
  <div class="datasource-property-editor">
    <!-- Section: Basic Info -->
    <div class="ds-section" :class="{ collapsed: !sections.basic }">
      <div class="ds-section-header" @click="sections.basic = !sections.basic">
        <svg class="ds-section-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        <span>基础信息</span>
      </div>
      <div v-show="sections.basic" class="ds-section-body">
        <div class="prop-group">
          <label class="prop-label">图表类型</label>
          <select class="prop-input prop-select" v-model="local.chartType" @change="emitUpdate">
            <option v-for="ct in chartTypes" :key="ct.value" :value="ct.value">{{ ct.label }}</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Section: Data Source -->
    <div class="ds-section" :class="{ collapsed: !sections.datasource }">
      <div class="ds-section-header" @click="sections.datasource = !sections.datasource">
        <svg class="ds-section-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        <span>数据源</span>
      </div>
      <div v-show="sections.datasource" class="ds-section-body">
        <div class="prop-group">
          <label class="prop-label">数据源</label>
          <select class="prop-input prop-select" v-model="datasourceIdModel" @change="emitUpdate">
            <option value="">请选择</option>
            <option v-for="ds in datasourceList" :key="ds.id" :value="String(ds.id)">{{ ds.name }}</option>
          </select>
        </div>
        <div class="prop-group">
          <label class="prop-label">查询类型</label>
          <div class="prop-radio-row">
            <label class="radio-label"><input type="radio" v-model="local.queryType" value="SQL" @change="emitUpdate" /> SQL</label>
            <label class="radio-label"><input type="radio" v-model="local.queryType" value="API" @change="emitUpdate" /> API</label>
          </div>
        </div>
      </div>
    </div>

    <!-- Section: Query Editor -->
    <div class="ds-section" :class="{ collapsed: !sections.query }">
      <div class="ds-section-header" @click="sections.query = !sections.query">
        <svg class="ds-section-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        <span>查询编辑</span>
      </div>
      <div v-show="sections.query" class="ds-section-body">
        <template v-if="local.queryType === 'SQL'">
          <div class="prop-group">
            <div class="sql-editor-header">
              <label class="prop-label">SQL 语句</label>
              <button type="button" class="sql-expand-btn" @click="openSqlDialog" title="在弹窗中编辑">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 3 21 3 21 9"/><polyline points="9 21 3 21 3 15"/><line x1="21" y1="3" x2="14" y2="10"/><line x1="3" y1="21" x2="10" y2="14"/></svg>
                <span>展开编辑</span>
              </button>
            </div>
            <SqlEditor v-model="local.sqlTemplate" height="140px" @update:modelValue="emitUpdate" />
          </div>
          <div v-if="validationErrors.sqlTemplate" class="prop-error">{{ validationErrors.sqlTemplate }}</div>
        </template>
        <template v-else>
          <div class="prop-group">
            <label class="prop-label">API 地址</label>
            <input class="prop-input" v-model="local.apiUrl" @input="emitUpdate" placeholder="https://..." />
          </div>
          <div class="prop-group">
            <label class="prop-label">请求方法</label>
            <select class="prop-input prop-select" v-model="local.apiMethod" @change="emitUpdate">
              <option value="GET">GET</option>
              <option value="POST">POST</option>
            </select>
          </div>
          <div class="prop-group">
            <label class="prop-label">请求头(JSON)</label>
            <input class="prop-input" v-model="local.apiHeaders" @input="emitUpdate" placeholder='{"Authorization":"Bearer xxx"}' />
          </div>
          <div class="prop-group" v-if="local.apiMethod === 'POST'">
            <label class="prop-label">请求体</label>
            <textarea class="prop-input prop-textarea" v-model="local.apiBody" @input="emitUpdate" rows="3" placeholder="JSON" />
          </div>
          <div class="prop-group">
            <label class="prop-label">数据路径</label>
            <input class="prop-input" v-model="local.responseDataPath" @input="emitUpdate" placeholder="如 data.records" />
          </div>
          <div v-if="validationErrors.apiUrl" class="prop-error">{{ validationErrors.apiUrl }}</div>
        </template>

        <div class="prop-group">
          <button type="button" class="prop-preview-btn" :disabled="previewLoading || !canPreview" @click="handlePreview">
            <svg v-if="!previewLoading" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"/></svg>
            <span v-if="previewLoading" class="ds-spinner"></span>
            {{ previewLoading ? '执行中...' : '执行预览' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Section: Data Preview -->
    <div v-if="previewData" class="ds-section" :class="{ collapsed: !sections.preview }">
      <div class="ds-section-header" @click="sections.preview = !sections.preview">
        <svg class="ds-section-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        <span>数据预览</span>
        <span class="ds-section-badge">{{ previewData.rows?.length || 0 }} 行</span>
      </div>
      <div v-show="sections.preview" class="ds-section-body">
        <div class="ds-preview-table-wrap">
          <table class="ds-preview-table">
            <thead>
              <tr>
                <th v-for="col in previewData.columns" :key="col.name">{{ col.name }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, ri) in previewData.rows.slice(0, 10)" :key="ri">
                <td v-for="col in previewData.columns" :key="col.name">{{ row[col.name] }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="previewError" class="prop-error">{{ previewError }}</div>
      </div>
    </div>

    <!-- Section: Column Mapping -->
    <div class="ds-section" :class="{ collapsed: !sections.mapping }">
      <div class="ds-section-header" @click="sections.mapping = !sections.mapping">
        <svg class="ds-section-arrow" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
        <span>列映射</span>
      </div>
      <div v-show="sections.mapping" class="ds-section-body">
        <div class="prop-group">
          <label class="prop-label">X 轴(维度)</label>
          <select v-if="availableColumns.length" class="prop-input prop-select" v-model="columnMapping.xAxis" @change="onColumnMappingInput">
            <option value="">请选择</option>
            <option v-for="col in availableColumns" :key="col" :value="col">{{ col }}</option>
          </select>
          <input v-else class="prop-input" v-model="columnMapping.xAxis" @input="onColumnMappingInput" placeholder="列名" />
        </div>
        <div class="prop-group">
          <label class="prop-label">Y 轴(度量)</label>
          <input class="prop-input" v-model="yAxisDisplay" placeholder="多个列用逗号分隔" />
        </div>
        <div class="prop-group">
          <label class="prop-label">系列分组</label>
          <select v-if="availableColumns.length" class="prop-input prop-select" v-model="columnMapping.category" @change="onColumnMappingInput">
            <option value="">不分组</option>
            <option v-for="col in availableColumns" :key="col" :value="col">{{ col }}</option>
          </select>
          <input v-else class="prop-input" v-model="columnMapping.category" @input="onColumnMappingInput" placeholder="可选" />
        </div>
      </div>
    </div>
    <!-- SQL Editor Dialog -->
    <Teleport to="body">
      <Transition name="sql-dialog">
        <div v-if="sqlDialogVisible" class="sql-dialog-mask" @click.self="cancelSqlDialog">
          <div class="sql-dialog">
            <div class="sql-dialog-header">
              <h3 class="sql-dialog-title">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
                SQL 查询编辑器
              </h3>
              <button class="sql-dialog-close" @click="cancelSqlDialog">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            <div class="sql-dialog-body">
              <div class="sql-dialog-editor">
                <SqlEditor v-model="dialogSql" height="100%" />
              </div>
              <div class="sql-dialog-examples">
                <div class="sql-examples-title">SQL 示例参考</div>
                <div class="sql-examples-hint">根据图表类型选择合适的 SQL 模式，点击即可填入编辑器</div>
                <div
                  v-for="(ex, i) in sqlExamples"
                  :key="i"
                  class="sql-example-card"
                  @click="applySqlExample(ex.sql)"
                >
                  <div class="sql-example-header">
                    <span class="sql-example-tag">{{ ex.tag }}</span>
                    <span class="sql-example-name">{{ ex.name }}</span>
                  </div>
                  <div class="sql-example-desc">{{ ex.desc }}</div>
                  <pre class="sql-example-code">{{ ex.sql }}</pre>
                </div>
              </div>
            </div>
            <div class="sql-dialog-footer">
              <button type="button" class="sql-dialog-btn sql-dialog-btn-cancel" @click="cancelSqlDialog">取消</button>
              <button type="button" class="sql-dialog-btn sql-dialog-btn-confirm" @click="confirmSqlDialog">确认</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import request from '@/utils/request'
import { previewDatasourceQuery } from '@/api/dashboard'
import SqlEditor from './SqlEditor.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  },
  showRefreshPreview: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'refresh'])

const chartTypes = [
  { value: 'bar', label: '柱状图' },
  { value: 'line', label: '折线图' },
  { value: 'pie', label: '饼图' },
  { value: 'groupedBar', label: '分组柱状图' },
  { value: 'kpi', label: '指标卡' },
  { value: 'table', label: '表格' }
]

const datasourceList = ref([])
const previewLoading = ref(false)
const previewData = ref(null)
const previewError = ref(null)
const validationErrors = reactive({ sqlTemplate: '', apiUrl: '' })

const sqlDialogVisible = ref(false)
const dialogSql = ref('')

const sqlExamples = [
  {
    tag: '柱状图 / 折线图',
    name: '按维度聚合统计',
    desc: 'X 轴为分类字段，Y 轴为聚合值，适用于柱状图和折线图',
    sql: `SELECT category AS 类别,
       SUM(amount) AS 总金额,
       COUNT(*) AS 数量
FROM your_table
GROUP BY category
ORDER BY 总金额 DESC`
  },
  {
    tag: '饼图',
    name: '占比分布统计',
    desc: '第一列作为名称，第二列作为数值，用于饼图展示各项占比',
    sql: `SELECT type AS 类型,
       SUM(amount) AS 金额
FROM your_table
GROUP BY type
ORDER BY 金额 DESC`
  },
  {
    tag: '分组柱状图',
    name: '多维度交叉分析',
    desc: '需要 X 轴、系列分组字段和数值列，用于多组对比',
    sql: `SELECT month AS 月份,
       product AS 产品,
       SUM(sales) AS 销售额
FROM your_table
GROUP BY month, product
ORDER BY month`
  },
  {
    tag: '指标卡',
    name: '汇总指标查询',
    desc: '返回一行多列的聚合数据，每一列作为一个指标卡展示',
    sql: `SELECT SUM(amount) AS 总收入,
       COUNT(*) AS 订单数,
       AVG(amount) AS 平均客单价
FROM your_table`
  },
  {
    tag: '表格',
    name: '明细数据查询',
    desc: '查询明细数据直接以表格形式展示，建议用 LIMIT 限制行数',
    sql: `SELECT id, name AS 名称,
       category AS 分类,
       amount AS 金额,
       created_at AS 创建时间
FROM your_table
ORDER BY created_at DESC
LIMIT 100`
  },
  {
    tag: '通用',
    name: '日期趋势分析',
    desc: '按日期维度统计趋势，适合折线图和柱状图的时间序列展示',
    sql: `SELECT DATE_FORMAT(created_at, '%Y-%m') AS 月份,
       SUM(amount) AS 总金额
FROM your_table
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 12 MONTH)
GROUP BY 月份
ORDER BY 月份`
  }
]

function openSqlDialog() {
  dialogSql.value = local.sqlTemplate || ''
  sqlDialogVisible.value = true
}

function cancelSqlDialog() {
  sqlDialogVisible.value = false
}

function confirmSqlDialog() {
  local.sqlTemplate = dialogSql.value
  sqlDialogVisible.value = false
  emitUpdate()
}

function applySqlExample(sql) {
  dialogSql.value = sql
}

const sections = reactive({
  basic: true,
  datasource: true,
  query: false,
  preview: false,
  mapping: false
})

function initSectionState(val) {
  const cfg = val || {}
  const hasQuery = cfg.queryType === 'SQL'
    ? !!(cfg.sqlTemplate || '').trim()
    : !!(cfg.apiUrl || '').trim()
  const isNew = !cfg.datasourceId && !hasQuery
  sections.basic = true
  sections.datasource = true
  sections.query = !isNew
  sections.preview = false
  sections.mapping = !isNew
}

function ensureConfig(val) {
  const v = val || {}
  const mapping = typeof v.columnMapping === 'string'
    ? (() => { try { return JSON.parse(v.columnMapping) || {} } catch { return {} } })()
    : (v.columnMapping || {})
  return {
    datasourceId: v.datasourceId ?? null,
    queryType: v.queryType || 'SQL',
    sqlTemplate: v.sqlTemplate || '',
    apiUrl: v.apiUrl || '',
    apiMethod: v.apiMethod || 'GET',
    apiHeaders: v.apiHeaders || '',
    apiBody: v.apiBody || '',
    responseDataPath: v.responseDataPath || '',
    chartType: v.chartType || 'bar',
    columnMapping: {
      xAxis: mapping.xAxis || '',
      yAxis: Array.isArray(mapping.yAxis) ? mapping.yAxis : (mapping.yAxis ? [mapping.yAxis] : []),
      category: mapping.category || ''
    }
  }
}

const local = reactive(ensureConfig(props.modelValue))

initSectionState(props.modelValue)

const datasourceIdModel = computed({
  get: () => (local.datasourceId == null || local.datasourceId === '') ? '' : String(local.datasourceId),
  set: (v) => {
    local.datasourceId = (v === '' || v == null) ? null : Number(v)
  }
})

const columnMapping = reactive({
  xAxis: local.columnMapping.xAxis,
  yAxis: [...(local.columnMapping.yAxis || [])],
  category: local.columnMapping.category
})

const availableColumns = ref([])

const yAxisDisplay = computed({
  get: () => (columnMapping.yAxis && columnMapping.yAxis.length) ? columnMapping.yAxis.join(', ') : '',
  set: (val) => {
    columnMapping.yAxis = val ? val.split(',').map(s => s.trim()).filter(Boolean) : []
    syncColumnMappingToLocal()
    emitUpdate()
  }
})

function syncColumnMappingToLocal() {
  local.columnMapping = {
    xAxis: columnMapping.xAxis,
    yAxis: [...columnMapping.yAxis],
    category: columnMapping.category
  }
}

function onColumnMappingInput() {
  syncColumnMappingToLocal()
  emitUpdate()
}

watch(() => props.modelValue, (val) => {
  const next = ensureConfig(val)
  Object.assign(local, next)
  columnMapping.xAxis = next.columnMapping.xAxis
  columnMapping.yAxis = [...(next.columnMapping.yAxis || [])]
  columnMapping.category = next.columnMapping.category
}, { deep: true })

async function loadDatasources() {
  try {
    const res = await request({ url: '/api/datasource/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } })
    datasourceList.value = (res.rows || res.data || []).map(d => ({ ...d, id: d.id || d.datasourceId }))
  } catch (e) {
    console.warn('加载数据源列表失败:', e)
  }
}

loadDatasources()

function emitUpdate() {
  syncColumnMappingToLocal()
  const payload = {
    ...props.modelValue,
    ...local,
    columnMapping: JSON.stringify(local.columnMapping)
  }
  validate(payload)
  emit('update:modelValue', payload)
}

function validate(c) {
  validationErrors.sqlTemplate = ''
  validationErrors.apiUrl = ''
  if (c.queryType === 'SQL' && !(c.sqlTemplate || '').trim()) {
    validationErrors.sqlTemplate = 'SQL 模板为必填'
  }
  if (c.queryType === 'API' && !(c.apiUrl || '').trim()) {
    validationErrors.apiUrl = 'API 地址为必填'
  }
}

const canPreview = computed(() => {
  const c = local
  if (c.queryType === 'SQL') return !!(c.sqlTemplate || '').trim() && c.datasourceId
  return !!(c.apiUrl || '').trim() && c.datasourceId
})

async function handlePreview() {
  if (!canPreview.value || previewLoading.value) return
  previewLoading.value = true
  previewError.value = null
  try {
    const payload = {
      datasourceId: local.datasourceId,
      queryType: local.queryType,
      sqlTemplate: local.queryType === 'SQL' ? local.sqlTemplate : undefined,
      apiUrl: local.queryType === 'API' ? local.apiUrl : undefined,
      apiMethod: local.queryType === 'API' ? local.apiMethod : undefined,
      apiHeaders: local.queryType === 'API' ? local.apiHeaders : undefined,
      apiBody: local.queryType === 'API' ? local.apiBody : undefined,
      responseDataPath: local.queryType === 'API' ? local.responseDataPath : undefined
    }
    const res = await previewDatasourceQuery(payload)
    const data = res.data || res
    if (data && data.columns && data.rows) {
      previewData.value = data
      availableColumns.value = data.columns.map(c => c.name)
      sections.preview = true
      sections.mapping = true
      if (!columnMapping.xAxis && availableColumns.value.length > 0) {
        columnMapping.xAxis = availableColumns.value[0]
        if (availableColumns.value.length > 1) {
          columnMapping.yAxis = [availableColumns.value[1]]
        }
        syncColumnMappingToLocal()
        emitUpdate()
      }
      emit('refresh')
    } else {
      previewError.value = '查询未返回数据'
    }
  } catch (e) {
    previewError.value = e.message || '查询执行失败'
  } finally {
    previewLoading.value = false
  }
}
</script>

<style scoped>
.datasource-property-editor {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ds-section {
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  overflow: hidden;
  transition: border-color 0.2s;

  &:hover {
    border-color: rgba(255, 255, 255, 0.08);
  }

  &.collapsed .ds-section-arrow {
    transform: rotate(-90deg);
  }
}

.ds-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--designer-text-secondary, #a8aabe);
  cursor: pointer;
  user-select: none;
  transition: background 0.15s, color 0.15s;
  background: rgba(255, 255, 255, 0.02);

  &:hover {
    background: rgba(255, 255, 255, 0.04);
    color: var(--designer-text-primary, #f0f1f5);
  }
}

.ds-section-arrow {
  transition: transform 0.2s;
  flex-shrink: 0;
}

.ds-section-badge {
  margin-left: auto;
  font-size: 11px;
  font-weight: 400;
  color: var(--designer-accent-light, #b0a8ff);
  background: rgba(124, 108, 240, 0.12);
  padding: 1px 6px;
  border-radius: 4px;
  letter-spacing: 0;
  text-transform: none;
}

.ds-section-body {
  padding: 8px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.prop-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prop-label {
  font-size: 11px;
  font-weight: 500;
  color: var(--designer-text-secondary, #a8aabe);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.prop-input {
  background: var(--designer-bg-medium, #181922);
  border: 1px solid var(--designer-border-color, rgba(255,255,255,0.07));
  border-radius: 6px;
  color: var(--designer-text-primary, #f0f1f5);
  font-family: inherit;
  font-size: 13px;
  padding: 7px 10px;
  outline: none;
  width: 100%;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.prop-input:focus {
  border-color: var(--designer-accent, #7c6cf0);
  box-shadow: 0 0 0 2px rgba(124, 108, 240, 0.15);
}

.prop-select {
  cursor: pointer;
  appearance: auto;
}

.prop-textarea {
  min-height: 60px;
  resize: vertical;
}

.prop-radio-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--designer-text-primary, #f0f1f5);
  cursor: pointer;
}

.radio-label input[type="radio"] {
  margin: 0;
}

.prop-error {
  font-size: 12px;
  color: #ff6b6b;
  margin-top: -2px;
}

.prop-preview-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid rgba(124, 108, 240, 0.3);
  background: rgba(124, 108, 240, 0.1);
  color: var(--designer-accent-light, #b0a8ff);
  font-size: 12px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.prop-preview-btn:hover:not(:disabled) {
  background: rgba(124, 108, 240, 0.2);
  border-color: rgba(124, 108, 240, 0.5);
}

.prop-preview-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ds-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 1.5px solid rgba(176, 168, 255, 0.3);
  border-top-color: rgba(176, 168, 255, 0.9);
  border-radius: 50%;
  animation: ds-spin 0.6s linear infinite;
}

@keyframes ds-spin {
  to { transform: rotate(360deg); }
}

.ds-preview-table-wrap {
  max-height: 180px;
  overflow: auto;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.05);

  &::-webkit-scrollbar {
    width: 3px;
    height: 3px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }
}

.ds-preview-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;

  th {
    position: sticky;
    top: 0;
    padding: 5px 8px;
    background: rgba(255, 255, 255, 0.04);
    color: rgba(255, 255, 255, 0.5);
    font-weight: 600;
    text-align: left;
    white-space: nowrap;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  }

  td {
    padding: 4px 8px;
    color: rgba(255, 255, 255, 0.7);
    border-bottom: 1px solid rgba(255, 255, 255, 0.03);
    white-space: nowrap;
    font-family: 'JetBrains Mono', monospace;
    font-size: 11px;
  }

  tbody tr:hover {
    background: rgba(124, 108, 240, 0.04);
  }
}

/* ===== SQL Editor Header ===== */
.sql-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sql-expand-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 4px;
  border: 1px solid rgba(124, 108, 240, 0.2);
  background: rgba(124, 108, 240, 0.06);
  color: var(--designer-accent-light, #b0a8ff);
  font-size: 11px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(124, 108, 240, 0.15);
    border-color: rgba(124, 108, 240, 0.4);
  }
}

/* ===== SQL Dialog ===== */
.sql-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sql-dialog {
  width: min(900px, 90vw);
  max-height: 85vh;
  background: #12131a;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sql-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.sql-dialog-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #f0f1f5;

  svg {
    color: var(--designer-accent-light, #b0a8ff);
  }
}

.sql-dialog-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    color: rgba(255, 255, 255, 0.8);
  }
}

.sql-dialog-body {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.sql-dialog-editor {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(255, 255, 255, 0.06);

  :deep(.sql-editor) {
    flex: 1;
    display: flex;
    flex-direction: column;
    border: none;
    border-radius: 0;
  }

  :deep(.cm-editor) {
    flex: 1;
    height: 100% !important;
  }
}

.sql-dialog-examples {
  width: 280px;
  flex-shrink: 0;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &::-webkit-scrollbar {
    width: 3px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.08);
    border-radius: 2px;
  }
}

.sql-examples-title {
  font-size: 13px;
  font-weight: 600;
  color: #f0f1f5;
  letter-spacing: 0.02em;
}

.sql-examples-hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  line-height: 1.5;
  margin-bottom: 4px;
}

.sql-example-card {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &:hover {
    border-color: rgba(124, 108, 240, 0.3);
    background: rgba(124, 108, 240, 0.06);
  }
}

.sql-example-header {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sql-example-tag {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 6px;
  border-radius: 3px;
  background: rgba(124, 108, 240, 0.15);
  color: var(--designer-accent-light, #b0a8ff);
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.sql-example-name {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
}

.sql-example-desc {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  line-height: 1.4;
}

.sql-example-code {
  margin: 0;
  padding: 8px 10px;
  border-radius: 5px;
  background: rgba(0, 0, 0, 0.3);
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 10.5px;
  line-height: 1.5;
  color: rgba(176, 168, 255, 0.7);
  white-space: pre-wrap;
  word-break: break-all;
  overflow: hidden;
}

.sql-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.sql-dialog-btn {
  padding: 7px 20px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;
}

.sql-dialog-btn-cancel {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: transparent;
  color: rgba(255, 255, 255, 0.6);

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    color: rgba(255, 255, 255, 0.8);
  }
}

.sql-dialog-btn-confirm {
  border: 1px solid rgba(124, 108, 240, 0.4);
  background: rgba(124, 108, 240, 0.2);
  color: var(--designer-accent-light, #b0a8ff);

  &:hover {
    background: rgba(124, 108, 240, 0.35);
    border-color: rgba(124, 108, 240, 0.6);
  }
}

/* ===== Dialog Transition ===== */
.sql-dialog-enter-active,
.sql-dialog-leave-active {
  transition: opacity 0.2s ease;
}

.sql-dialog-enter-active .sql-dialog,
.sql-dialog-leave-active .sql-dialog {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.sql-dialog-enter-from,
.sql-dialog-leave-to {
  opacity: 0;
}

.sql-dialog-enter-from .sql-dialog {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}

.sql-dialog-leave-to .sql-dialog {
  transform: scale(0.97);
  opacity: 0;
}
</style>

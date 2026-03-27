<template>
  <div class="ds-renderer">
    <!-- Demo preview for unconfigured cards -->
    <div v-if="isUnconfigured" class="ds-demo-wrapper">
      <div v-if="chartType === 'kpi'" class="ds-chart-area">
        <div class="ds-kpi-grid">
          <div v-for="item in demoKpiItems" :key="item.name" class="ds-kpi-item">
            <span class="ds-kpi-value">{{ item.displayValue }}</span>
            <span class="ds-kpi-label">{{ item.name }}</span>
          </div>
        </div>
      </div>
      <div v-else-if="chartType === 'table'" class="ds-chart-area">
        <div class="ds-table-wrap">
          <table class="ds-table">
            <thead>
              <tr>
                <th v-for="col in demoTableData.columns" :key="col">{{ col }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, ri) in demoTableData.rows" :key="ri">
                <td v-for="col in demoTableData.columns" :key="col">{{ row[col] }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <div v-else class="ds-chart-area">
        <div ref="demoChartContainer" class="ds-echarts" />
      </div>
      <div class="ds-demo-overlay">
        <span class="ds-demo-badge">示例数据 · 点击配置</span>
      </div>
    </div>

    <template v-else>
    <div class="ds-toolbar">
      <button class="ds-refresh-btn" :disabled="loading" @click="fetchData" :title="loading ? '加载中...' : '刷新数据'">
        <svg :class="{ spinning: loading }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
        </svg>
      </button>
    </div>

    <div v-if="loading" class="ds-state">
      <div class="ds-pulse-ring"></div>
      <span class="ds-state-text">数据加载中</span>
    </div>

    <div v-else-if="error" class="ds-state ds-error">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
      </svg>
      <span class="ds-state-text">{{ error }}</span>
      <button class="ds-retry-btn" @click="fetchData">重试</button>
    </div>

    <div v-else-if="!queryResult" class="ds-state ds-empty">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
        <polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>
      </svg>
      <span class="ds-state-text">暂无数据</span>
    </div>

    <div v-else class="ds-chart-area">
      <div v-if="chartType === 'kpi'" class="ds-kpi-grid">
        <div v-for="item in kpiItems" :key="item.name" class="ds-kpi-item">
          <span class="ds-kpi-value">{{ item.displayValue }}</span>
          <span class="ds-kpi-label">{{ item.name }}</span>
        </div>
        <div v-if="kpiItems.length === 0" class="ds-state ds-empty" style="position: absolute; inset: 0;">
          <span class="ds-state-text">无数值数据</span>
        </div>
      </div>

      <div v-else-if="chartType === 'table'" class="ds-table-wrap">
        <table class="ds-table">
          <thead>
            <tr>
              <th v-for="col in queryResult.columns" :key="col.name">{{ col.name }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, ri) in queryResult.rows" :key="ri">
              <td v-for="col in queryResult.columns" :key="col.name">{{ row[col.name] }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else ref="chartContainer" class="ds-echarts" />
    </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { executeDatasourceCard, previewDatasourceQuery } from '@/api/dashboard'
import * as echarts from 'echarts'

// ── ECharts Theme ──────────────────────────────────────

const DARK_THEME = {
  color: ['#7c6cf0', '#34d9a8', '#ff6b6b', '#ffd93d', '#6ec6ff', '#ff9f43', '#a29bfe', '#55efc4'],
  textStyle: { color: 'rgba(255,255,255,0.65)', fontFamily: "'DM Sans', system-ui, sans-serif", fontSize: 12 },
  tooltip: {
    backgroundColor: 'rgba(18, 19, 26, 0.95)',
    borderColor: 'rgba(255,255,255,0.08)',
    textStyle: { color: 'rgba(255,255,255,0.85)', fontSize: 12 }
  },
  legend: { textStyle: { color: 'rgba(255,255,255,0.55)', fontSize: 12 } },
  categoryAxis: {
    axisLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
    axisTick: { lineStyle: { color: 'rgba(255,255,255,0.06)' } },
    axisLabel: { color: 'rgba(255,255,255,0.45)', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } }
  },
  valueAxis: {
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 11 },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)', type: 'dashed' } }
  }
}

// ── Chart Constants ────────────────────────────────────

const NON_ECHARTS_TYPES = ['kpi', 'table']
const PIE_RADIUS = ['35%', '60%']
const BAR_BORDER_RADIUS = [3, 3, 0, 0]
const GRID_DEFAULT = { left: '3%', right: '4%', bottom: '3%', top: '8%', containLabel: true }
const GRID_WITH_LEGEND = { ...GRID_DEFAULT, top: '14%' }
const FORMAT_THRESHOLDS = { M: 1_000_000, K: 1_000 }
const DECIMAL_PLACES = 2

// ── Props & State ──────────────────────────────────────

const props = defineProps({
  card: { type: Object, required: true }
})

const loading = ref(false)
const error = ref(null)
const queryResult = ref(null)
const chartContainer = ref(null)
const demoChartContainer = ref(null)

const chartType = computed(() => props.card.datasourceConfig?.chartType || 'bar')
const needsEchartsRender = computed(() => !NON_ECHARTS_TYPES.includes(chartType.value))

const isUnconfigured = computed(() => {
  const cfg = props.card.datasourceConfig
  if (!cfg) return true
  const hasQuery = cfg.queryType === 'SQL'
    ? !!(cfg.sqlTemplate || '').trim()
    : !!(cfg.apiUrl || '').trim()
  return !cfg.datasourceId && !hasQuery
})

// ── Chart Renderer ─────────────────────────────────────

function useChartRenderer(containerRef, buildOptionFn) {
  let instance = null
  const onResize = () => instance?.resize()

  function render() {
    if (!containerRef.value) return
    const option = buildOptionFn()
    if (!option) return
    if (!instance) {
      instance = echarts.init(containerRef.value, null, { renderer: 'canvas' })
      window.addEventListener('resize', onResize)
    }
    option.backgroundColor = 'transparent'
    instance.setOption(option, true)
  }

  function dispose() {
    window.removeEventListener('resize', onResize)
    instance?.dispose()
    instance = null
  }

  return { render, dispose }
}

const demoRenderer = useChartRenderer(demoChartContainer, () => buildDemoOption(chartType.value))
const chartRenderer = useChartRenderer(chartContainer, buildEchartsOption)

// ── Demo Data ──────────────────────────────────────────

const demoKpiItems = [
  { name: '总收入', displayValue: '128.56K' },
  { name: '用户数', displayValue: '8.65K' },
  { name: '转化率', displayValue: '3.24%' }
]

const demoTableData = {
  columns: ['日期', '产品', '销量', '金额'],
  rows: [
    { '日期': '2026-01', '产品': '产品 A', '销量': 320, '金额': 12800 },
    { '日期': '2026-02', '产品': '产品 B', '销量': 480, '金额': 19200 },
    { '日期': '2026-03', '产品': '产品 C', '销量': 350, '金额': 14000 },
    { '日期': '2026-04', '产品': '产品 A', '销量': 520, '金额': 20800 },
    { '日期': '2026-05', '产品': '产品 B', '销量': 410, '金额': 16400 }
  ]
}

// ── Demo Chart Builders ────────────────────────────────

const DEMO_MONTHS = ['1月', '2月', '3月', '4月', '5月', '6月']

function buildDemoBarOption() {
  return applyDarkAxis({
    animation: true,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: DEMO_MONTHS },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: [320, 480, 350, 520, 410, 580],
      itemStyle: { borderRadius: BAR_BORDER_RADIUS }
    }],
    grid: GRID_DEFAULT
  })
}

function buildDemoLineOption() {
  return applyDarkAxis({
    animation: true,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: DEMO_MONTHS },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      data: [150, 230, 224, 318, 435, 510],
      smooth: true,
      areaStyle: { opacity: 0.08 }
    }],
    grid: GRID_DEFAULT
  })
}

function buildDemoPieOption() {
  return {
    animation: true,
    color: DARK_THEME.color,
    tooltip: { ...DARK_THEME.tooltip, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { ...DARK_THEME.legend, orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: PIE_RADIUS,
      data: [
        { name: '类目A', value: 335 },
        { name: '类目B', value: 210 },
        { name: '类目C', value: 174 },
        { name: '类目D', value: 135 },
        { name: '类目E', value: 96 }
      ],
      label: { color: 'rgba(255,255,255,0.6)', fontSize: 12 },
      itemStyle: { borderColor: 'rgba(12, 13, 18, 0.8)', borderWidth: 2 },
      emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(124,108,240,0.3)' } }
    }]
  }
}

function buildDemoGroupedBarOption() {
  return applyDarkAxis({
    animation: true,
    tooltip: { trigger: 'axis' },
    legend: { data: ['产品A', '产品B'] },
    xAxis: { type: 'category', data: ['Q1', 'Q2', 'Q3', 'Q4'] },
    yAxis: { type: 'value' },
    series: [
      { name: '产品A', type: 'bar', data: [120, 200, 150, 180], itemStyle: { borderRadius: BAR_BORDER_RADIUS } },
      { name: '产品B', type: 'bar', data: [180, 160, 230, 140], itemStyle: { borderRadius: BAR_BORDER_RADIUS } }
    ],
    grid: GRID_WITH_LEGEND
  })
}

const DEMO_OPTION_BUILDERS = {
  bar: buildDemoBarOption,
  line: buildDemoLineOption,
  pie: buildDemoPieOption,
  groupedBar: buildDemoGroupedBarOption
}

function buildDemoOption(type) {
  return DEMO_OPTION_BUILDERS[type]?.() ?? null
}

// ── Column Mapping ─────────────────────────────────────

/** @returns {{ xAxis: string, yAxis: string[], category: string }} */
const parsedMapping = computed(() => {
  const raw = props.card.datasourceConfig?.columnMapping
  if (!raw) return { xAxis: '', yAxis: [], category: '' }
  try {
    const m = typeof raw === 'string' ? JSON.parse(raw) : raw
    return {
      xAxis: m.xAxis || '',
      yAxis: Array.isArray(m.yAxis) ? m.yAxis : (m.yAxis ? [m.yAxis] : []),
      category: m.category || ''
    }
  } catch {
    return { xAxis: '', yAxis: [], category: '' }
  }
})

// ── KPI ────────────────────────────────────────────────

const kpiItems = computed(() => {
  if (!queryResult.value?.rows?.length) return []
  const rows = queryResult.value.rows
  const yAxes = parsedMapping.value.yAxis
  const cols = yAxes.length > 0 ? yAxes : queryResult.value.columns.map(c => c.name)

  return cols.map(col => {
    const values = rows.map(r => {
      const v = r[col]
      if (typeof v === 'number' && !Number.isNaN(v)) return v
      if (typeof v === 'string' && v.trim() !== '') {
        const n = Number(v.replace(/,/g, ''))
        return Number.isNaN(n) ? null : n
      }
      return null
    }).filter(v => v !== null)
    if (values.length === 0) return null
    const sum = values.reduce((a, b) => a + b, 0)
    return { name: col, displayValue: formatNumber(sum) }
  }).filter(Boolean)
})

function formatNumber(num) {
  if (num == null || Number.isNaN(num)) return '-'
  if (num >= FORMAT_THRESHOLDS.M) return (num / FORMAT_THRESHOLDS.M).toFixed(DECIMAL_PLACES) + 'M'
  if (num >= FORMAT_THRESHOLDS.K) return (num / FORMAT_THRESHOLDS.K).toFixed(DECIMAL_PLACES) + 'K'
  return typeof num === 'number' ? num.toFixed(DECIMAL_PLACES) : String(num)
}

// ── Data Fetching ──────────────────────────────────────

async function fetchData() {
  const dsConfig = props.card.datasourceConfig
  if (!dsConfig) {
    error.value = '数据源配置缺失'
    return
  }
  loading.value = true
  error.value = null
  try {
    let data
    if (dsConfig.id) {
      const res = await executeDatasourceCard(dsConfig.id)
      data = res.data || res
    } else {
      const res = await previewDatasourceQuery({
        datasourceId: dsConfig.datasourceId,
        queryType: dsConfig.queryType,
        sqlTemplate: dsConfig.sqlTemplate,
        apiUrl: dsConfig.apiUrl,
        apiMethod: dsConfig.apiMethod,
        apiHeaders: dsConfig.apiHeaders,
        apiBody: dsConfig.apiBody,
        responseDataPath: dsConfig.responseDataPath
      })
      data = res.data || res
    }
    queryResult.value = data?.rows?.length ? data : null
  } catch (e) {
    error.value = e?.message ?? (typeof e === 'string' ? e : '查询执行失败')
  } finally {
    loading.value = false
  }
}

// ── ECharts Option Builders ────────────────────────────

function buildEchartsOption() {
  const data = queryResult.value
  if (!data) return null
  const mapping = parsedMapping.value
  const type = chartType.value
  const rows = data.rows || []

  if (type === 'pie') return buildPieOption(rows, mapping)
  if (type === 'groupedBar') return buildGroupedBarOption(rows, mapping)
  return buildAxisOption(rows, mapping, type)
}

function applyDarkAxis(option) {
  if (option.xAxis) {
    option.xAxis.axisLine = DARK_THEME.categoryAxis.axisLine
    option.xAxis.axisTick = DARK_THEME.categoryAxis.axisTick
    option.xAxis.axisLabel = { ...option.xAxis.axisLabel, ...DARK_THEME.categoryAxis.axisLabel }
  }
  if (option.yAxis) {
    option.yAxis.axisLine = DARK_THEME.valueAxis.axisLine
    option.yAxis.axisTick = DARK_THEME.valueAxis.axisTick
    option.yAxis.axisLabel = { ...option.yAxis.axisLabel, ...DARK_THEME.valueAxis.axisLabel }
    option.yAxis.splitLine = DARK_THEME.valueAxis.splitLine
  }
  option.tooltip = { ...option.tooltip, ...DARK_THEME.tooltip }
  if (option.legend) option.legend = { ...option.legend, ...DARK_THEME.legend }
  option.color = DARK_THEME.color
  return option
}

function buildAxisOption(rows, mapping, type) {
  const xField = mapping.xAxis
  const yFields = mapping.yAxis
  if (!xField || yFields.length === 0) return null

  const xData = rows.map(r => r[xField])
  const series = yFields.map(field => ({
    name: field,
    type: type === 'bar' ? 'bar' : 'line',
    data: rows.map(r => r[field]),
    smooth: type === 'line',
    itemStyle: { borderRadius: type === 'bar' ? BAR_BORDER_RADIUS : undefined },
    areaStyle: type === 'line' ? { opacity: 0.08 } : undefined
  }))

  return applyDarkAxis({
    tooltip: { trigger: 'axis' },
    legend: yFields.length > 1 ? { data: yFields } : undefined,
    xAxis: { type: 'category', data: xData },
    yAxis: { type: 'value' },
    series,
    grid: yFields.length > 1 ? GRID_WITH_LEGEND : GRID_DEFAULT
  })
}

function buildPieOption(rows, mapping) {
  const nameField = mapping.xAxis
  const valueField = mapping.yAxis?.[0]
  if (!nameField || !valueField) return null

  return {
    color: DARK_THEME.color,
    tooltip: { ...DARK_THEME.tooltip, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { ...DARK_THEME.legend, orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: PIE_RADIUS,
      data: rows.map(r => ({ name: r[nameField], value: r[valueField] })),
      label: { color: 'rgba(255,255,255,0.6)', fontSize: 12 },
      itemStyle: { borderColor: 'rgba(12, 13, 18, 0.8)', borderWidth: 2 },
      emphasis: { itemStyle: { shadowBlur: 16, shadowColor: 'rgba(124,108,240,0.3)' } }
    }]
  }
}

function buildGroupedBarOption(rows, mapping) {
  const xField = mapping.xAxis
  const yField = mapping.yAxis?.[0]
  const categoryField = mapping.category
  if (!xField || !yField) return null

  if (!categoryField) {
    return buildAxisOption(rows, mapping, 'bar')
  }

  const xValues = [...new Set(rows.map(r => r[xField]))]
  const categories = [...new Set(rows.map(r => r[categoryField]))]

  const series = categories.map(cat => ({
    name: cat,
    type: 'bar',
    data: xValues.map(x => {
      const row = rows.find(r => r[xField] === x && r[categoryField] === cat)
      return row ? row[yField] : 0
    }),
    itemStyle: { borderRadius: BAR_BORDER_RADIUS }
  }))

  return applyDarkAxis({
    tooltip: { trigger: 'axis' },
    legend: { data: categories },
    xAxis: { type: 'category', data: xValues },
    yAxis: { type: 'value' },
    series,
    grid: GRID_WITH_LEGEND
  })
}

// ── Lifecycle & Watchers ───────────────────────────────

function syncDemoChart() {
  demoRenderer.dispose()
  if (needsEchartsRender.value) {
    nextTick(() => demoRenderer.render())
  }
}

function syncDataChart() {
  if (queryResult.value && needsEchartsRender.value) {
    nextTick(() => chartRenderer.render())
  }
}

onMounted(() => {
  if (isUnconfigured.value) {
    syncDemoChart()
  } else {
    fetchData()
  }
})

onBeforeUnmount(() => {
  chartRenderer.dispose()
  demoRenderer.dispose()
})

watch(queryResult, syncDataChart)

watch(chartType, () => {
  if (isUnconfigured.value) {
    syncDemoChart()
  } else {
    syncDataChart()
  }
})

watch(isUnconfigured, (val) => {
  if (val) {
    syncDemoChart()
  } else {
    demoRenderer.dispose()
    fetchData()
  }
})
</script>

<style scoped>
.ds-renderer {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  font-family: 'DM Sans', system-ui, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.ds-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 6px 8px;
  flex-shrink: 0;
}

.ds-refresh-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: none;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(255, 255, 255, 0.35);
  cursor: pointer;
  transition: all 0.2s;

  &:hover:not(:disabled) {
    background: rgba(255, 255, 255, 0.08);
    color: rgba(255, 255, 255, 0.7);
  }

  &:disabled {
    cursor: default;
  }

  svg.spinning {
    animation: ds-spin 0.8s linear infinite;
  }
}

@keyframes ds-spin {
  to { transform: rotate(360deg); }
}

/* ===== States ===== */
.ds-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.ds-state-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 0.02em;
}

.ds-pulse-ring {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(124, 108, 240, 0.3);
  border-top-color: rgba(124, 108, 240, 0.8);
  animation: ds-spin 0.7s linear infinite;
}

.ds-error {
  svg {
    color: #ff6b6b;
  }
  .ds-state-text {
    color: rgba(255, 107, 107, 0.8);
    font-size: 12px;
    max-width: 80%;
    text-align: center;
    line-height: 1.4;
  }
}

.ds-retry-btn {
  padding: 5px 16px;
  border-radius: 6px;
  border: 1px solid rgba(124, 108, 240, 0.3);
  background: rgba(124, 108, 240, 0.1);
  color: rgba(176, 168, 255, 0.9);
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(124, 108, 240, 0.2);
    border-color: rgba(124, 108, 240, 0.5);
  }
}

.ds-empty {
  svg {
    color: rgba(255, 255, 255, 0.15);
  }
}

/* ===== Chart Area ===== */
.ds-chart-area {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* ===== KPI ===== */
.ds-kpi-grid {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 12px;
  position: relative;
}

.ds-kpi-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 120px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 10px;
  transition: all 0.25s;

  &:hover {
    background: rgba(124, 108, 240, 0.06);
    border-color: rgba(124, 108, 240, 0.15);
  }
}

.ds-kpi-value {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #b0a8ff, #7c6cf0);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.1;
}

.ds-kpi-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-weight: 500;
}

/* ===== Table ===== */
.ds-table-wrap {
  flex: 1;
  overflow: auto;
  padding: 4px;

  &::-webkit-scrollbar {
    width: 4px;
    height: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(255, 255, 255, 0.1);
    border-radius: 2px;
  }
}

.ds-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 12px;

  th {
    position: sticky;
    top: 0;
    z-index: 1;
    padding: 8px 12px;
    background: rgba(255, 255, 255, 0.04);
    color: rgba(255, 255, 255, 0.55);
    font-weight: 600;
    text-align: left;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    font-size: 11px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    white-space: nowrap;
  }

  td {
    padding: 7px 12px;
    color: rgba(255, 255, 255, 0.75);
    border-bottom: 1px solid rgba(255, 255, 255, 0.03);
    white-space: nowrap;
    font-family: 'JetBrains Mono', monospace;
    font-size: 12px;
  }

  tbody tr {
    transition: background 0.15s;

    &:hover {
      background: rgba(124, 108, 240, 0.04);
    }
  }
}

/* ===== ECharts ===== */
.ds-echarts {
  flex: 1;
  min-height: 160px;
  width: 100%;
  padding: 4px;
}

/* ===== Demo Preview ===== */
.ds-demo-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}

.ds-demo-wrapper .ds-chart-area {
  opacity: 0.55;
  filter: saturate(0.7);
  transition: all 0.3s ease;
}

.ds-demo-wrapper:hover .ds-chart-area {
  opacity: 0.75;
  filter: saturate(0.9);
}

.ds-demo-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 12px;
  pointer-events: none;
  background: linear-gradient(to top, rgba(12, 13, 18, 0.6) 0%, transparent 40%);
}

.ds-demo-badge {
  font-size: 11px;
  color: rgba(176, 168, 255, 0.85);
  background: rgba(124, 108, 240, 0.12);
  border: 1px solid rgba(124, 108, 240, 0.2);
  padding: 3px 12px;
  border-radius: 10px;
  letter-spacing: 0.04em;
  font-weight: 500;
  backdrop-filter: blur(8px);
  transition: all 0.25s;
}

.ds-demo-wrapper:hover .ds-demo-badge {
  background: rgba(124, 108, 240, 0.2);
  border-color: rgba(124, 108, 240, 0.4);
  color: rgba(200, 192, 255, 0.95);
}
</style>

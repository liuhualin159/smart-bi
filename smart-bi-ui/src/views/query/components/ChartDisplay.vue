<template>
  <div class="chart-display">
    <!-- 图表工具栏 -->
    <div class="chart-toolbar">
      <div class="chart-type-selector">
        <el-tooltip
          v-if="!props.fixedChartType"
          :content="availableChartTypes.length < chartTypes.length ? '仅显示与当前数据兼容的展示类型' : '切换数据展示方式'"
          placement="top"
        >
          <el-select
            v-model="currentChartType"
            @change="handleChartTypeChange"
            size="default"
            style="width: 150px"
          >
            <el-option
              v-for="type in availableChartTypes"
              :key="type.code"
              :label="type.name"
              :value="type.code"
            />
          </el-select>
        </el-tooltip>
        <span v-else class="fixed-type-label">{{ (chartTypes.find(t => t.code === props.fixedChartType) || { name: props.fixedChartType }).name }}</span>
        <el-tooltip v-if="!props.fixedChartType" content="推荐置信度">
          <el-tag
            :type="chartConfidence >= 0.6 ? 'success' : 'warning'"
            size="small"
            class="confidence-badge"
          >
            {{ Math.round(chartConfidence * 100) }}%
          </el-tag>
        </el-tooltip>
        <span v-if="!props.fixedChartType && chartAlternatives.length > 0" class="alternatives-hint">
          备选：{{ chartAlternatives.map(c => (chartTypes.find(t => t.code === c) || { name: c }).name).join('、') }}
        </span>
        <el-tooltip v-if="props.qualityScores?.length > 0" :content="qualityScoresTooltip">
          <span class="quality-badges">
            <el-tag
              v-for="qs in props.qualityScores"
              :key="qs.tableName"
              :type="(qs.score || 0) >= 60 ? 'success' : 'warning'"
              size="small"
              class="quality-badge"
            >
              {{ qs.tableName }}:{{ qs.score ?? '–' }}分
            </el-tag>
          </span>
        </el-tooltip>
      </div>
      
      <div class="chart-actions">
        <el-button 
          type="info" 
          plain
          size="small"
          @click="handleFeedback"
        >
          <el-icon><ChatLineRound /></el-icon>
          反馈
        </el-button>
        <el-dropdown @command="handleExportCommand" trigger="click">
          <el-button type="primary" plain size="small">
            <el-icon><Download /></el-icon>
            导出
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="exportPng">导出为PNG</el-dropdown-item>
              <el-dropdown-item command="exportPdf">导出为PDF</el-dropdown-item>
              <el-dropdown-item command="exportExcel">导出为Excel</el-dropdown-item>
              <el-dropdown-item command="exportCsv">导出为CSV</el-dropdown-item>
              <el-dropdown-item command="exportJson">导出为JSON</el-dropdown-item>
              <el-dropdown-item command="exportParquet">导出为Parquet</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button 
          v-if="showSaveButton"
          type="primary" 
          size="small"
          @click="handleSaveCard"
        >
          保存为卡片
        </el-button>
        <el-button
          v-if="!props.fixedChartType && currentChartType !== 'table' && currentChartType !== 'kpi'"
          plain
          size="small"
          @click="showAnnotationDialog = true"
        >
          添加注释
        </el-button>
      </div>
    </div>

    <!-- 注释编辑对话框 -->
    <el-dialog v-model="showAnnotationDialog" :title="editingAnnotationIndex >= 0 ? '编辑注释' : '添加图表注释'" width="400px" @close="resetAnnotationForm">
      <el-form label-width="80px">
        <el-form-item label="类型">
          <el-radio-group v-model="annotationForm.type">
            <el-radio label="text">文本</el-radio>
            <el-radio label="markLine">标注线</el-radio>
            <el-radio label="markArea">标注区域</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'text'" label="内容">
          <el-input v-model="annotationForm.content" placeholder="注释文本" />
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'text'" label="位置 X%">
          <el-input-number v-model="annotationForm.x" :min="0" :max="100" :step="5" />
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'text'" label="位置 Y%">
          <el-input-number v-model="annotationForm.y" :min="0" :max="100" :step="5" />
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'markLine'" label="方向">
          <el-radio-group v-model="annotationForm.orient">
            <el-radio label="horizontal">水平线</el-radio>
            <el-radio label="vertical">垂直线</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'markLine'" label="值">
          <el-input v-model="annotationForm.value" placeholder="轴坐标值" />
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'markArea'" label="方向">
          <el-radio-group v-model="annotationForm.orient">
            <el-radio label="horizontal">水平带</el-radio>
            <el-radio label="vertical">垂直带</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'markArea'" label="起点值">
          <el-input v-model="annotationForm.areaMin" placeholder="最小值" />
        </el-form-item>
        <el-form-item v-if="annotationForm.type === 'markArea'" label="终点值">
          <el-input v-model="annotationForm.areaMax" placeholder="最大值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAnnotationDialog = false">取消</el-button>
        <el-button type="primary" @click="addAnnotation">{{ editingAnnotationIndex >= 0 ? '保存' : '添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 注释列表（可编辑/删除） -->
    <div v-if="annotations.length > 0" class="annotations-list">
      <el-tag
        v-for="(a, i) in annotations"
        :key="i"
        closable
        size="small"
        class="annotation-tag"
        @click="editAnnotation(i)"
        @close="removeAnnotation(i)"
      >
        {{ getAnnotationLabel(a) }}
      </el-tag>
    </div>
    
    <!-- 保存卡片对话框 -->
    <SaveCardDialog
      v-model="showSaveDialog"
      :card-data="cardDataForSave"
      @saved="handleCardSaved"
    />
    
    <!-- 反馈表单对话框 -->
    <FeedbackForm
      v-model="showFeedbackDialog"
      :query-id="props.queryId"
      @submitted="handleFeedbackSubmitted"
    />

    <!-- 溯源对话框 -->
    <TraceDialog
      v-model="showTraceDialog"
      :query-id="traceQueryId"
    />
    
    <!-- 图表容器 -->
    <div 
      ref="chartContainer" 
      class="chart-container"
      :style="{ minHeight: chartHeight + 'px' }"
    ></div>
    
    <!-- 错误提示 -->
    <el-alert
      v-if="chartError"
      :title="chartError"
      type="error"
      :closable="false"
      show-icon
      style="margin-top: 20px"
    />

    <!-- 结论折叠面板 -->
    <el-collapse v-if="displayData?.length > 0 && displayColumns?.length > 0" v-model="conclusionActive" class="conclusion-panel" @change="onConclusionChange">
      <el-collapse-item name="conclusion">
        <template #title>
          <span>结论</span>
        </template>
        <div v-if="summaryLoading" class="summary-loading"><el-icon class="is-loading"><Loading /></el-icon> 生成中...</div>
        <div v-else-if="summaryText" class="summary-text">{{ summaryText }}</div>
        <div v-else class="summary-placeholder">点击展开生成总结</div>
      </el-collapse-item>
    </el-collapse>

    <!-- 下钻面包屑 -->
    <div v-if="drillStack.length > 0" class="drill-breadcrumb">
      <el-tag
        v-for="(d, i) in drillStack"
        :key="i"
        closable
        @close="handleDrillPop(i)"
      >
        {{ d.dimension }}={{ d.value }}
      </el-tag>
    </div>
  </div>
</template>

<script setup name="ChartDisplay">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { recommendChartType, generateChartConfig, summarizeQuery, drillQuery } from '@/api/query'
import { exportChartPng, exportChartPdf, exportDataExcel, exportData } from '@/api/export'
import SaveCardDialog from './SaveCardDialog.vue'
import { Download, ArrowDown, ChatLineRound, Loading } from '@element-plus/icons-vue'
import FeedbackForm from './FeedbackForm.vue'
import TraceDialog from './TraceDialog.vue'
import { formatByDisplayFormat } from '@/utils/formatUtils'

const props = defineProps({
  columns: {
    type: Array,
    default: () => []
  },
  data: {
    type: Array,
    default: () => []
  },
  showSaveButton: {
    type: Boolean,
    default: true
  },
  queryId: {
    type: [Number, String],
    default: null
  },
  /** 用户问题，供后端 LLM 推荐图表时使用 */
  question: {
    type: String,
    default: ''
  },
  /** 生成的 SQL，供后端 LLM 推荐图表时使用 */
  sql: {
    type: String,
    default: ''
  },
  /** 固定图表类型（如 table），不参与推荐与切换 */
  fixedChartType: {
    type: String,
    default: null
  },
  /** 表级质量评分 [{ tableName, tableId, score }]，用于展示与低质量标注 */
  qualityScores: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['save-card', 'chart-type-change', 'feedback', 'drill', 'filter-by'])

const showSaveDialog = ref(false)
const showFeedbackDialog = ref(false)
const showTraceDialog = ref(false)
const traceQueryId = ref(null)
const summaryText = ref('')
const summaryLoading = ref(false)
const summaryLoaded = ref(false)
const drillStack = ref([])
const drillResultData = ref(null)
const conclusionActive = ref([])

const displayData = computed(() => drillStack.value.length > 0 && drillResultData.value ? drillResultData.value : props.data)
const qualityScoresTooltip = computed(() => {
  const list = props.qualityScores || []
  if (list.length === 0) return ''
  return '数据质量评分（低于60为低质量）: ' + list.map(q => `${q.tableName}: ${q.score ?? '–'}分`).join('、')
})
const displayColumns = computed(() => drillStack.value.length > 0 && drillResultData.value?.length ? Object.keys(drillResultData.value[0]) : props.columns)

const chartContainer = ref(null)
const chartInstance = ref(null)
const currentChartType = ref('table')
/** 上次渲染成功的图表类型，切换失败时用于回退 */
const lastSuccessfulChartType = ref('table')
/** 推荐接口返回的图表类型，用于限制下拉仅展示兼容类型 */
const recommendedChartType = ref(null)
const chartError = ref('')
const chartHeight = ref(400)
const chartConfidence = ref(1)
const chartAlternatives = ref([])
const showAnnotationDialog = ref(false)
const annotationForm = ref({ type: 'text', content: '', x: 50, y: 50, orient: 'horizontal', value: '', areaMin: '', areaMax: '' })
const annotations = ref([])
const editingAnnotationIndex = ref(-1)

// 图表类型选项
const chartTypes = [
  { code: 'bar', name: '柱状图' },
  { code: 'line', name: '折线图' },
  { code: 'pie', name: '饼图' },
  { code: 'groupedBar', name: '分组柱状图' },
  { code: 'heatmap', name: '热力图' },
  { code: 'funnel', name: '漏斗图' },
  { code: 'boxplot', name: '箱线图' },
  { code: 'scatter', name: '散点图' },
  { code: 'sankey', name: '桑基图' },
  { code: 'kpi', name: '指标卡' },
  { code: 'table', name: '表格' }
]

// 可选展示类型：有推荐结果时仅展示表格 + 推荐类型 + 备选，避免因数据格式导致切换报错
const availableChartTypes = computed(() => {
  if (props.fixedChartType) return chartTypes
  const recommended = recommendedChartType.value
  const alternatives = chartAlternatives.value || []
  if (recommended != null && recommended !== '') {
    const allowed = new Set(['table', recommended, ...alternatives])
    return chartTypes.filter(t => allowed.has(t.code))
  }
  return chartTypes
})

function onConclusionChange(activeNames) {
  if (Array.isArray(activeNames) && activeNames.includes('conclusion') && !summaryLoaded.value) {
    loadSummary()
  }
}

async function loadSummary() {
  if (summaryLoading.value) return
  summaryLoading.value = true
  try {
    const res = await summarizeQuery({
      queryId: props.queryId,
      chartType: currentChartType.value,
      columns: displayColumns.value,
      data: displayData.value
    })
    if (res.code === 200 && res.data?.summary) {
      summaryText.value = res.data.summary
      summaryLoaded.value = true
    }
  } catch (e) {
    console.warn('生成总结失败', e)
  } finally {
    summaryLoading.value = false
  }
}

async function handleDrill(dimension, value) {
  if (!props.queryId) return
  try {
    const res = await drillQuery({ queryId: props.queryId, drillDimension: dimension, drillValue: value })
    if (res.code === 200 && res.data?.data) {
      drillStack.value.push({ dimension, value })
      drillResultData.value = res.data.data
      emit('drill', res.data)
      await nextTick()
      recommendChart()
    }
  } catch (e) {
    console.error('下钻失败', e)
  }
}

function handleDrillPop(index) {
  drillStack.value.splice(index, drillStack.value.length - index)
  if (drillStack.value.length === 0) drillResultData.value = null
}

// 监听数据变化，自动推荐图表类型
watch(() => [displayColumns.value, displayData.value], ([columns, data]) => {
  if (columns && columns.length > 0 && data && data.length > 0) {
    recommendChart()
  }
}, { immediate: true, deep: true })

watch(() => [props.queryId, props.data], () => {
  drillStack.value = []
  drillResultData.value = null
  summaryText.value = ''
  summaryLoaded.value = false
  recommendedChartType.value = null
})

// 推荐图表类型（fixedChartType 时跳过推荐，直接使用固定类型）
async function recommendChart() {
  if (props.fixedChartType) {
    currentChartType.value = props.fixedChartType
    await renderChart()
    return
  }
  const cols = displayColumns.value
  const dat = displayData.value
  if (!cols || cols.length === 0 || !dat || dat.length === 0) {
    return
  }
  
  try {
    const payload = {
      columns: cols,
      data: dat
    }
    if (props.question) payload.question = props.question
    if (props.sql) payload.sql = props.sql
    const response = await recommendChartType(payload)
    
    if (response.code === 200) {
      const recommended = response.data.chartType || 'table'
      currentChartType.value = recommended
      recommendedChartType.value = recommended
      chartConfidence.value = response.data.confidence ?? 1
      chartAlternatives.value = response.data.alternatives ?? []
      await renderChart()
    }
  } catch (error) {
    console.warn('推荐图表类型失败:', error)
    recommendedChartType.value = null
    currentChartType.value = 'table'
    await renderChart()
  }
}

// 渲染图表
async function renderChart() {
  if (!chartContainer.value) {
    return
  }
  
  chartError.value = ''
  
  const cols = displayColumns.value
  const dat = displayData.value
  if (!Array.isArray(cols) || !Array.isArray(dat)) {
    chartError.value = '数据格式异常，无法渲染图表，请尝试重新查询'
    return
  }
  
  try {
    // 如果是表格类型，直接显示表格
    if (currentChartType.value === 'table') {
      renderTable()
      lastSuccessfulChartType.value = currentChartType.value
      return
    }
    
    // 如果是KPI类型，显示指标卡
    if (currentChartType.value === 'kpi') {
      renderKpi()
      lastSuccessfulChartType.value = currentChartType.value
      return
    }
    
    // 其他类型使用ECharts渲染
    const response = await generateChartConfig({
      chartType: currentChartType.value,
      columns: displayColumns.value,
      data: displayData.value
    })
    
    if (response.code === 200) {
      const config = response.data
      
      // 检查是否有错误
      if (config.error) {
        chartError.value = config.errorMessage || '图表渲染失败'
        return
      }
      
      lastSuccessfulChartType.value = currentChartType.value
      // 初始化或更新图表
      if (!chartInstance.value) {
        chartInstance.value = echarts.init(chartContainer.value)
        
        // 监听窗口大小变化
        window.addEventListener('resize', handleResize)
      }
      
      // 应用主题和样式
      const finalConfig = applyChartTheme(config)
      
      chartInstance.value.setOption(finalConfig, true)
      applyAnnotationsAndRender()
      if (props.queryId) {
        chartInstance.value.off('click')
        chartInstance.value.on('click', () => { traceQueryId.value = props.queryId; showTraceDialog.value = true })
      }
    } else {
      chartError.value = response.msg || '生成图表配置失败'
    }
  } catch (error) {
    console.error('渲染图表失败:', error)
    chartError.value = '图表渲染失败: ' + (error.message || '未知错误')
  }
}

// 应用图表主题（确保可访问性）
function applyChartTheme(config) {
  const theme = {
    textStyle: {
      fontSize: 14, // 确保文字大小≥12px
      color: '#dfe2ef'
    },
    color: [
      '#00f2ff', '#7000ff', '#74f5ff', '#d1bcff', '#ffe173',
      '#22d3ee', '#a855f7', '#67C23A', '#E6A23C', '#F56C6C'
    ],
    backgroundColor: 'transparent'
  }
  
  // 合并主题配置（legend 仅合并已有配置，避免覆盖导致 undefined series；单系列时由 ECharts 根据 series[].name 自动生成）
  const result = {
    ...config,
    ...theme,
    title: {
      top: 16,
      ...config.title,
      textStyle: {
        fontSize: 16,
        ...config.title?.textStyle
      }
    },
    tooltip: {
      ...config.tooltip,
      backgroundColor: 'rgba(15, 19, 28, 0.92)',
      borderColor: 'rgba(58, 73, 75, 0.5)',
      borderWidth: 1,
      textStyle: {
        fontSize: 12,
        color: '#dfe2ef'
      }
    }
  }
  if (config.legend && Object.keys(config.legend).length > 0) {
    result.legend = {
      top: 48,
      left: 'center',
      orient: 'horizontal',
      ...config.legend,
      textStyle: {
        fontSize: 12,
        ...config.legend?.textStyle
      }
    }
    // 标题与图例共存时，为绘图区域预留顶部空间，避免重叠
    result.grid = {
      ...config.grid,
      top: 85,
      left: 60,
      right: 40,
      bottom: 60,
      containLabel: true
    }
  }
  // 注入 axisLabel.formatter（后端返回 _formatterType 时）
  if (result.yAxis?.axisLabel?._formatterType) {
    const t = result.yAxis.axisLabel._formatterType
    delete result.yAxis.axisLabel._formatterType
    result.yAxis.axisLabel.formatter = formatterByType(t)
  }
  // 注入 series itemStyle.color 正负变色（后端返回 _colorBySign 时）
  if (result.series?.length) {
    result.series = result.series.map(s => {
      if (s?.itemStyle?._colorBySign) {
        const { _colorBySign, ...rest } = s.itemStyle
        return { ...s, itemStyle: { ...rest, color: params => (params.value != null && params.value < 0 ? '#F56C6C' : '#67C23A') } }
      }
      return s
    })
  }
  // 深色主题坐标与网格线
  const axisBase = {
    axisLine: { lineStyle: { color: 'rgba(132, 148, 149, 0.55)' } },
    axisLabel: { color: 'rgba(185, 202, 203, 0.85)', fontSize: 12 },
    splitLine: { lineStyle: { color: 'rgba(58, 73, 75, 0.28)' } }
  }
  if (result.xAxis) result.xAxis = Array.isArray(result.xAxis) ? result.xAxis.map(x => ({ ...axisBase, ...x })) : { ...axisBase, ...result.xAxis }
  if (result.yAxis) result.yAxis = Array.isArray(result.yAxis) ? result.yAxis.map(y => ({ ...axisBase, ...y })) : { ...axisBase, ...result.yAxis }
  return result
}

function formatterByType(t) {
  if (t === 'percent' || t === 'percentage') return v => (v != null ? (Number(v) * 100).toFixed(2) + '%' : '')
  if (t === 'currency' || t === 'money') return v => (v != null ? '¥' + Number(v).toLocaleString() : '')
  if (t === 'decimal') return v => (v != null ? Number(v).toLocaleString() : '')
  return null
}

// 渲染表格
function renderTable() {
  if (!chartContainer.value) {
    return
  }
  
  // 清空容器
  chartContainer.value.innerHTML = ''
  
  // 创建表格
  const table = document.createElement('table')
  table.className = 'chart-table'
  table.style.width = '100%'
  table.style.borderCollapse = 'collapse'
  
  // 表头
  const thead = document.createElement('thead')
  const headerRow = document.createElement('tr')
  const cols = displayColumns.value
  cols.forEach(column => {
    const th = document.createElement('th')
    th.textContent = column
    th.style.padding = '12px'
    th.style.border = '1px solid rgba(58, 73, 75, 0.38)'
    th.style.backgroundColor = 'rgba(255, 255, 255, 0.04)'
    th.style.color = '#b9cacb'
    th.style.fontSize = '14px'
    headerRow.appendChild(th)
  })
  thead.appendChild(headerRow)
  table.appendChild(thead)
  
  // 表体
  const tbody = document.createElement('tbody')
  const dat = displayData.value
  dat.forEach(row => {
    const tr = document.createElement('tr')
  cols.forEach(column => {
    const td = document.createElement('td')
    const val = row[column]
    const isNumeric = typeof val === 'number' || (typeof val === 'string' && val !== '' && !Number.isNaN(Number(val.replace(/,/g, ''))))
    td.textContent = val != null ? (isNumeric ? formatByDisplayFormat(val, 'decimal') : String(val)) : ''
    td.style.padding = '12px'
    td.style.border = '1px solid rgba(58, 73, 75, 0.28)'
    td.style.fontSize = '14px'
    td.style.color = '#dfe2ef'
    if (props.queryId && isNumeric) {
      td.style.cursor = 'pointer'
      td.title = '点击查看数据溯源'
      td.addEventListener('click', () => { traceQueryId.value = props.queryId; showTraceDialog.value = true })
    } else if (props.queryId && !isNumeric && typeof val === 'string') {
      td.style.cursor = 'pointer'
      td.title = '点击筛选联动'
      td.addEventListener('click', () => emit('filter-by', { dimension: column, value: val }))
    }
    tr.appendChild(td)
  })
    tbody.appendChild(tr)
  })
  table.appendChild(tbody)
  
  chartContainer.value.appendChild(table)
}

// 渲染KPI指标卡
function renderKpi() {
  if (!chartContainer.value) {
    return
  }
  
  chartContainer.value.innerHTML = ''
  
  const kpiContainer = document.createElement('div')
  kpiContainer.className = 'kpi-container'
  kpiContainer.style.display = 'flex'
  kpiContainer.style.flexWrap = 'wrap'
  kpiContainer.style.gap = '20px'
  
  // 找出数值列（支持 number 或可解析为数字的字符串）
  const measureColumns = props.columns.filter(col => {
    if (props.data.length === 0) return false
    const firstValue = props.data[0][col]
    if (typeof firstValue === 'number' && !Number.isNaN(firstValue)) return true
    if (typeof firstValue === 'string' && firstValue.trim() !== '') {
      const n = Number(firstValue.replace(/,/g, ''))
      return !Number.isNaN(n)
    }
    return false
  })
  
  if (measureColumns.length === 0) {
    kpiContainer.innerHTML = '<div style="padding: 20px; text-align: center; color: #909399;">无数值数据</div>'
    chartContainer.value.appendChild(kpiContainer)
    return
  }
  
  // 计算每个指标的值（求和或平均值）
  measureColumns.forEach(column => {
    const values = props.data.map(row => {
      const v = row[column]
      if (typeof v === 'number') return v
      if (typeof v === 'string') {
        const n = Number(v.replace(/,/g, ''))
        return Number.isNaN(n) ? null : n
      }
      return null
    }).filter(v => v != null)
    const sum = values.reduce((a, b) => a + b, 0)
    const avg = values.length > 0 ? sum / values.length : 0
    
    const kpiCard = document.createElement('div')
    kpiCard.className = 'kpi-card'
    kpiCard.style.cssText = `
      flex: 1;
      min-width: 200px;
      padding: 20px;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(58, 73, 75, 0.32);
      border-radius: 8px;
      text-align: center;
    `
    
    kpiCard.innerHTML = `
      <div style="font-size: 14px; color: #b9cacb; margin-bottom: 10px;">${column}</div>
      <div style="font-size: 32px; font-weight: bold; color: #00f2ff;">${formatNumber(sum)}</div>
      <div style="font-size: 12px; color: rgba(185,202,203,0.8); margin-top: 5px;">平均值: ${formatNumber(avg)}</div>
    `
    
    kpiContainer.appendChild(kpiCard)
  })
  
  chartContainer.value.appendChild(kpiContainer)
}

// 格式化数字
function formatNumber(num) {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(2) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(2) + 'K'
  }
  return num.toFixed(2)
}

// 图表类型切换：失败时回退到上次成功的类型并提示
async function handleChartTypeChange() {
  await renderChart()
  if (chartError.value) {
    const fallback = lastSuccessfulChartType.value || 'table'
    currentChartType.value = fallback
    chartError.value = ''
    await renderChart()
    ElMessage.warning('当前数据格式不支持该展示类型，已切换回「' + (chartTypes.find(t => t.code === fallback)?.name || fallback) + '」。请使用表格或上方推荐类型。')
  }
  emit('chart-type-change', currentChartType.value)
}

// 图表注释
function resetAnnotationForm() {
  annotationForm.value = { type: 'text', content: '', x: 50, y: 50, orient: 'horizontal', value: '', areaMin: '', areaMax: '' }
  editingAnnotationIndex.value = -1
}

function getAnnotationLabel(a) {
  if (a.type === 'text') return a.content
  if (a.type === 'markLine') return (a.orient === 'vertical' ? '垂直线' : '水平线') + '@' + a.value
  if (a.type === 'markArea') return (a.orient === 'vertical' ? '垂直区域' : '水平区域') + ' ' + (a.areaMin ?? '') + '~' + (a.areaMax ?? '')
  return ''
}

function editAnnotation(i) {
  const a = annotations.value[i]
  if (!a) return
  editingAnnotationIndex.value = i
  annotationForm.value = {
    type: a.type,
    content: a.content || '',
    x: a.x ?? 50,
    y: a.y ?? 50,
    orient: a.orient || 'horizontal',
    value: a.value ?? '',
    areaMin: a.areaMin ?? '',
    areaMax: a.areaMax ?? ''
  }
  showAnnotationDialog.value = true
}

function addAnnotation() {
  const f = annotationForm.value
  const idx = editingAnnotationIndex.value
  let item = null
  if (f.type === 'text' && f.content) {
    item = { type: 'text', content: f.content, x: f.x ?? 50, y: f.y ?? 50 }
  } else if (f.type === 'markLine' && f.value != null && f.value !== '') {
    item = { type: 'markLine', orient: f.orient || 'horizontal', value: f.value }
  } else if (f.type === 'markArea' && f.areaMin != null && f.areaMax != null && f.areaMin !== '' && f.areaMax !== '') {
    item = { type: 'markArea', orient: f.orient || 'horizontal', areaMin: f.areaMin, areaMax: f.areaMax }
  }
  if (item) {
    if (idx >= 0 && idx < annotations.value.length) {
      annotations.value[idx] = item
    } else {
      annotations.value.push(item)
    }
  }
  showAnnotationDialog.value = false
  resetAnnotationForm()
  if (chartInstance.value) applyAnnotationsAndRender()
}

function removeAnnotation(i) {
  annotations.value.splice(i, 1)
  if (chartInstance.value) applyAnnotationsAndRender()
}

function applyAnnotationsAndRender() {
  if (!chartInstance.value || props.fixedChartType || currentChartType.value === 'table' || currentChartType.value === 'kpi') return
  if (annotations.value.length === 0) return
  const opt = chartInstance.value.getOption()
  if (!opt || !opt.series) return
  const graphics = []
  const markLines = []
  const markAreas = []
  for (const a of annotations.value) {
    if (a.type === 'text') {
      graphics.push({ type: 'text', left: (a.x || 50) + '%', top: (a.y || 50) + '%', style: { text: a.content, fontSize: 12 } })
    } else if (a.type === 'markLine') {
      markLines.push({ [a.orient === 'vertical' ? 'xAxis' : 'yAxis']: a.value })
    } else if (a.type === 'markArea') {
      const axis = a.orient === 'vertical' ? 'xAxis' : 'yAxis'
      const minVal = a.areaMin != null ? (Number(a.areaMin) || a.areaMin) : ''
      const maxVal = a.areaMax != null ? (Number(a.areaMax) || a.areaMax) : ''
      if (minVal !== '' && maxVal !== '') {
        markAreas.push([{ [axis]: minVal }, { [axis]: maxVal }])
      }
    }
  }
  opt.graphic = graphics
  if (opt.series?.[0]) {
    opt.series[0].markLine = markLines.length > 0 ? { data: markLines, lineStyle: { type: 'dashed', color: '#999' } } : undefined
    opt.series[0].markArea = markAreas.length > 0 ? { data: markAreas, itemStyle: { color: 'rgba(64,158,255,0.2)' } } : undefined
  }
  chartInstance.value.setOption(opt, true)
}

// 保存卡片数据
const cardDataForSave = computed(() => ({
  chartType: currentChartType.value,
  columns: props.columns,
  data: props.data,
  name: `查询结果_${new Date().toLocaleString()}`
}))

// 保存卡片
function handleSaveCard() {
  showSaveDialog.value = true
}

// 卡片保存成功
function handleCardSaved(card) {
  emit('save-card', card)
}

// 反馈
function handleFeedback() {
  showFeedbackDialog.value = true
}

// 反馈提交成功
function handleFeedbackSubmitted() {
  emit('feedback')
}

// 导出命令处理
async function handleExportCommand(command) {
  const { proxy } = getCurrentInstance()
  
  try {
    if (command === 'exportPng') {
      // 前端导出PNG（使用ECharts的getDataURL）
      if (chartInstance.value && currentChartType.value !== 'table' && currentChartType.value !== 'kpi') {
        const dataUrl = chartInstance.value.getDataURL({
          type: 'png',
          pixelRatio: 2,
          backgroundColor: '#fff'
        })
        
        // 下载图片
        const link = document.createElement('a')
        link.download = `chart_${Date.now()}.png`
        link.href = dataUrl
        link.click()
        
        proxy.$modal.msgSuccess('图表导出成功')
      } else {
        proxy.$modal.msgWarning('当前图表类型不支持PNG导出')
      }
    } else if (command === 'exportPdf') {
      // PDF导出需要后端支持
      proxy.$modal.msgWarning('PDF导出功能需要后端支持，请使用卡片导出功能')
    } else if (command === 'exportExcel') {
      if (props.data && props.data.length > 0 && props.queryId) {
        await exportData({ queryId: props.queryId, format: 'excel', maxRows: 10000, applyDesensitization: true })
        proxy.$modal.msgSuccess('Excel 导出成功')
      } else {
        proxy.$modal.msgWarning('没有可导出的数据或缺少 queryId')
      }
    } else if (command === 'exportCsv') {
      if (props.data && props.data.length > 0 && props.queryId) {
        await exportData({ queryId: props.queryId, format: 'csv', maxRows: 10000, applyDesensitization: true })
        proxy.$modal.msgSuccess('CSV 导出成功')
      } else {
        proxy.$modal.msgWarning('没有可导出的数据或缺少 queryId')
      }
    } else if (command === 'exportJson') {
      if (props.data && props.data.length > 0 && props.queryId) {
        await exportData({ queryId: props.queryId, format: 'json', maxRows: 10000, applyDesensitization: true })
        proxy.$modal.msgSuccess('JSON 导出成功')
      } else {
        proxy.$modal.msgWarning('没有可导出的数据或缺少 queryId')
      }
    } else if (command === 'exportParquet') {
      if (props.data && props.data.length > 0 && props.queryId) {
        await exportData({ queryId: props.queryId, format: 'parquet', maxRows: 10000, applyDesensitization: true })
        proxy.$modal.msgSuccess('Parquet 导出成功')
      } else {
        proxy.$modal.msgWarning('没有可导出的数据或缺少 queryId')
      }
    }
  } catch (error) {
    proxy.$modal.msgError('导出失败: ' + (error.msg || error.message))
  }
}

// 窗口大小变化处理
function handleResize() {
  if (chartInstance.value) {
    chartInstance.value.resize()
  }
}

// 组件挂载
onMounted(() => {
  nextTick(() => {
    if (props.columns && props.columns.length > 0 && props.data && props.data.length > 0) {
      renderChart()
    }
  })
})

// 组件卸载
onBeforeUnmount(() => {
  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chart-display {
  width: 100%;
}

.chart-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(58, 73, 75, 0.32);
  border-radius: 8px;
}

.chart-type-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.confidence-badge {
  flex-shrink: 0;
}
.quality-badges {
  display: inline-flex;
  gap: 6px;
  margin-left: 8px;
}

.alternatives-hint {
  font-size: 12px;
  color: rgba(185, 202, 203, 0.75);
  margin-left: 4px;
}

.annotations-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.annotation-tag {
  cursor: pointer;
}

.chart-container {
  width: 100%;
  min-height: 400px;
  background: linear-gradient(180deg, rgba(15, 19, 28, 0.62) 0%, rgba(15, 19, 28, 0.35) 100%);
  border: 1px solid rgba(58, 73, 75, 0.28);
  border-radius: 8px;
  padding: 10px;
}

.chart-table {
  width: 100%;
  border-collapse: collapse;
}

.chart-table th,
.chart-table td {
  padding: 12px;
  border: 1px solid rgba(58, 73, 75, 0.28);
  text-align: left;
  font-size: 14px;
  color: #dfe2ef;
}

.chart-table th {
  background-color: rgba(255, 255, 255, 0.04);
  font-weight: 600;
  color: rgba(185, 202, 203, 0.9);
}

.kpi-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.kpi-card {
  flex: 1;
  min-width: 200px;
  padding: 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  text-align: center;
}

.conclusion-panel {
  margin-top: 16px;
}

.summary-loading,
.summary-text,
.summary-placeholder {
  padding: 8px 0;
  color: rgba(185, 202, 203, 0.88);
}

.drill-breadcrumb {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
</style>

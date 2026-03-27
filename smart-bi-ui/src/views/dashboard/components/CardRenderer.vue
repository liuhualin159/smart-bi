<template>
  <div class="card-renderer">
    <div class="card-header">
      <span class="card-title">{{ card.cardName || '未命名卡片' }}</span>
      <div class="card-actions">
        <el-button
          v-if="isChartCardWithSql"
          link
          type="primary"
          size="small"
          :loading="chartDataLoading"
          @click="fetchChartCardData(); $nextTick(() => renderEcharts())"
        >
          <el-icon><Refresh /></el-icon> 刷新
        </el-button>
        <el-dropdown @command="handleExportCommand" trigger="click">
          <el-button link type="primary" size="small">
            <el-icon><Download /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="exportPng">导出为PNG</el-dropdown-item>
              <el-dropdown-item command="exportPdf">导出为PDF</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <div class="card-content">
      <!-- 装饰组件渲染 -->
      <div v-if="props.card.componentType === 'decoration'" class="chart-wrapper decoration-wrapper">
        <component
          :is="decorationComponents[props.card.decorationType]"
          v-if="decorationComponents[props.card.decorationType]"
          :config="parsedStyleConfig"
        />
        <div v-else class="decoration-placeholder">
          <span>未知装饰类型: {{ props.card.decorationType }}</span>
        </div>
      </div>
      <!-- 卡片组合容器 -->
      <div v-else-if="props.card.componentType === 'group'" class="chart-wrapper group-wrapper">
        <div class="group-header">
          <span>{{ props.card.cardName || '卡片组合' }}</span>
        </div>
        <div class="group-children-container">
          <div
            v-for="child in (props.card.children || [])"
            :key="child.dashboardCardId || child.cardId"
            class="group-child-item"
            :style="{
              position: 'absolute',
              left: child.positionX + 'px',
              top: child.positionY + 'px',
              width: child.width + 'px',
              height: child.height + 'px'
            }"
          >
            <CardRenderer :card="child" />
          </div>
        </div>
      </div>
      <!-- 数据源卡片 -->
      <div v-else-if="props.card.componentType === 'datasource'" class="chart-wrapper datasource-wrapper">
        <DatasourceCardRenderer :card="props.card" />
      </div>
      <!-- KPI 指标卡：显示实际数值 -->
      <div v-else-if="chartConfig && chartType === 'kpi'" class="chart-wrapper kpi-wrapper">
        <div v-if="kpiItems.length > 0" class="kpi-container">
          <div
            v-for="item in kpiItems"
            :key="item.name"
            class="kpi-card"
          >
            <div class="kpi-label">{{ item.name }}</div>
            <div class="kpi-value">{{ item.displayValue }}</div>
            <div v-if="item.avg !== null" class="kpi-avg">平均值: {{ item.avg }}</div>
          </div>
        </div>
        <div v-else class="kpi-empty">无数值数据</div>
      </div>
      <!-- 表格：显示实际数据 -->
      <div v-else-if="chartConfig && chartType === 'table'" class="chart-wrapper table-wrapper">
        <table v-if="tableColumns.length > 0 && tableData.length > 0" class="chart-table">
          <thead>
            <tr>
              <th v-for="col in tableColumns" :key="col">{{ col }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in tableData" :key="idx">
              <td v-for="col in tableColumns" :key="col">{{ row[col] != null ? row[col] : '' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="table-empty">无表格数据</div>
      </div>
      <!-- 柱状图/折线图/饼图：使用 echarts 渲染 -->
      <div
        v-else-if="chartConfig && echartsTypes.includes(chartType)"
        ref="echartsContainer"
        class="chart-wrapper echarts-wrapper"
      />
      <!-- 无配置或未知类型：降级显示 -->
      <div v-else-if="chartConfig" class="chart-placeholder">
        <el-icon><DataAnalysis /></el-icon>
        <span>{{ card.cardName || getChartTypeName(card.chartType) }}</span>
      </div>
      <div v-else-if="chartDataLoading && isChartCardWithSql" class="card-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在执行 SQL 加载数据...</span>
      </div>
      <div v-else-if="chartDataError" class="card-error">
        <span>{{ chartDataError }}</span>
      </div>
      <div v-else class="card-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
    </div>
  </div>
</template>

<script setup name="CardRenderer">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { DataAnalysis, Loading, Download, Brush, Grid, Connection, Refresh } from '@element-plus/icons-vue'
import { exportChartPng, exportChartPdf } from '@/api/export'
import { getChartCardData } from '@/api/dashboard'
import { generateChartConfig } from '@/api/query'
import { decorationComponents } from './decorations/decorationRegistry'
import DatasourceCardRenderer from './datasource/DatasourceCardRenderer.vue'
import * as echarts from 'echarts'
import { getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()

const props = defineProps({
  card: {
    type: Object,
    required: true
  }
})

const chartConfig = ref(null)
const chartDataLoading = ref(false)
const chartDataError = ref(null)
const echartsContainer = ref(null)
let chartInstance = null

const chartType = computed(() => props.card.chartType || (chartConfig.value && chartConfig.value.type))
const isChartCardWithSql = computed(() => props.card.componentType === 'chart' && props.card.cardId)
const echartsTypes = ['bar', 'line', 'pie', 'groupedBar']

const parsedStyleConfig = computed(() => {
  if (props.card.styleConfig) {
    try {
      return typeof props.card.styleConfig === 'string'
        ? JSON.parse(props.card.styleConfig)
        : props.card.styleConfig
    } catch (e) {
      return {}
    }
  }
  return {}
})

// 解析图表配置（仅从 props 恢复，不请求接口）
function parseConfig() {
  if (!props.card.chartConfig) return
  try {
    chartConfig.value = typeof props.card.chartConfig === 'string'
      ? JSON.parse(props.card.chartConfig)
      : props.card.chartConfig
  } catch (e) {
    console.warn('解析图表配置失败:', e)
  }
}

// 通过执行 SQL 拉取图表数据（有 cardId 时优先用此方式，支持动态刷新）
async function fetchChartCardData() {
  const cardId = props.card.cardId
  if (!cardId) return
  chartDataLoading.value = true
  chartDataError.value = null
  try {
    const res = await getChartCardData(cardId)
    if (res.code === 200 && res.data) {
      chartConfig.value = {
        type: res.data.type || props.card.chartType,
        columns: res.data.columns || [],
        data: res.data.data || []
      }
    } else {
      parseConfig()
    }
  } catch (e) {
    chartDataError.value = e?.message || e?.msg || '加载失败'
    parseConfig()
  } finally {
    chartDataLoading.value = false
    if (chartConfig.value && echartsTypes.includes(props.card.chartType || chartConfig.value?.type)) {
      setTimeout(renderEcharts, 80)
    }
  }
}

// KPI 指标项（从 chartConfig.data 提取）
const kpiItems = computed(() => {
  const config = chartConfig.value
  if (!config || !config.data || config.data.length === 0) return []
  const cols = config.columns?.length ? config.columns : (config.data[0] ? Object.keys(config.data[0]) : [])
  const data = config.data || []
  return cols.map(col => {
    const values = data
      .map(row => {
        const v = row[col]
        if (typeof v === 'number' && !Number.isNaN(v)) return v
        if (typeof v === 'string' && v.trim() !== '') {
          const n = Number(v.replace(/,/g, ''))
          return Number.isNaN(n) ? null : n
        }
        return null
      })
      .filter(v => v != null)
    if (values.length === 0) return null
    const sum = values.reduce((a, b) => a + b, 0)
    const avg = sum / values.length
    return {
      name: col,
      value: sum,
      displayValue: formatNumber(sum),
      avg: formatNumber(avg)
    }
  }).filter(Boolean)
})

// 表格列与数据
const tableColumns = computed(() => chartConfig.value?.columns || [])
const tableData = computed(() => chartConfig.value?.data || [])

function formatNumber(num) {
  if (num == null || Number.isNaN(num)) return '-'
  if (num >= 1000000) return (num / 1000000).toFixed(2) + 'M'
  if (num >= 1000) return (num / 1000).toFixed(2) + 'K'
  return typeof num === 'number' ? num.toFixed(2) : String(num)
}

// 获取图表类型名称
function getChartTypeName(t) {
  const typeMap = { bar: '柱状图', line: '折线图', pie: '饼图', groupedBar: '分组柱状图', kpi: '指标卡', table: '表格' }
  return typeMap[t] || t || '图表'
}

// 渲染 echarts
async function renderEcharts() {
  const config = chartConfig.value
  if (!config || !echartsContainer.value) return
  const type = chartType.value
  if (!echartsTypes.includes(type)) return

  try {
    const res = await generateChartConfig({
      chartType: type,
      columns: config.columns || [],
      data: config.data || []
    })
    if (res.code !== 200 || !res.data) {
      console.warn('生成图表配置失败:', res.msg)
      return
    }
    const opt = res.data
    if (opt.error) return

    // 用卡片业务标题覆盖图表内 title，避免只显示「柱状图」等类型名
    const cardTitle = props.card.cardName?.trim()
    if (cardTitle) {
      if (!opt.title) opt.title = {}
      if (typeof opt.title === 'object' && !Array.isArray(opt.title)) {
        opt.title.text = cardTitle
      }
    }

    if (!chartInstance) {
      chartInstance = echarts.init(echartsContainer.value)
      window.addEventListener('resize', handleResize)
    }
    chartInstance.setOption(opt, true)
  } catch (e) {
    console.warn('渲染图表失败:', e)
  }
}

function handleResize() {
  chartInstance?.resize()
}

onMounted(() => {
  if (isChartCardWithSql.value) {
    fetchChartCardData()
  } else {
    parseConfig()
    if (chartConfig.value && echartsTypes.includes(chartType.value)) {
      setTimeout(renderEcharts, 50)
    }
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})

watch(
  () => [props.card.chartConfig, props.card.cardId],
  () => {
    if (isChartCardWithSql.value) {
      fetchChartCardData()
    } else {
      parseConfig()
      if (chartConfig.value && echartsTypes.includes(chartType.value)) {
        setTimeout(renderEcharts, 50)
      }
    }
  }
)

// 导出
async function handleExportCommand(command) {
  const cardId = props.card.cardId
  if (!cardId) return
  try {
    if (command === 'exportPng') {
      await exportChartPng(cardId)
      proxy.$modal.msgSuccess('图表导出成功')
    } else if (command === 'exportPdf') {
      await exportChartPdf(cardId)
      proxy.$modal.msgSuccess('图表导出成功')
    }
  } catch (error) {
    proxy.$modal.msgError('导出失败: ' + (error.msg || error.message))
  }
}
</script>

<style scoped>
.card-renderer {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.card-content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

.chart-wrapper {
  width: 100%;
  height: 100%;
  min-height: 80px;
}

.kpi-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.kpi-card {
  flex: 1;
  min-width: 150px;
  padding: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  text-align: center;
}

.kpi-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.kpi-value {
  font-size: 28px;
  font-weight: bold;
  color: #409eff;
}

.kpi-avg {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.kpi-empty,
.table-empty {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.table-wrapper {
  overflow: auto;
}

.chart-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.chart-table th,
.chart-table td {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
}

.chart-table th {
  background: #f5f7fa;
  font-weight: 500;
}

.echarts-wrapper {
  min-height: 200px;
}

.chart-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #909399;
}

.chart-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.card-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #909399;
}

.card-loading .el-icon {
  font-size: 32px;
  margin-bottom: 10px;
}

.card-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: #f56c6c;
  font-size: 12px;
}

.decoration-wrapper,
.datasource-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.group-wrapper {
  border: 2px dashed #409eff;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.group-header {
  padding: 4px 8px;
  background: rgba(64, 158, 255, 0.1);
  font-size: 12px;
  color: #409eff;
  border-bottom: 1px dashed #409eff;
}
.group-children-container {
  flex: 1;
  position: relative;
}
.group-child-item {
  position: absolute;
}

.decoration-placeholder,
.datasource-placeholder {
  text-align: center;
  color: #909399;
}
.decoration-placeholder .el-icon,
.datasource-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: 10px;
}
</style>

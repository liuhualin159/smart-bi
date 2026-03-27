<template>
  <el-dialog
    :model-value="props.visible"
    title="添加数据源卡片"
    width="900px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
    @closed="resetWizard"
  >
    <el-steps :active="currentStep" finish-status="success" align-center style="margin-bottom: 24px">
      <el-step title="选择数据源" />
      <el-step title="编写查询" />
      <el-step title="预览数据" />
      <el-step title="选择图表" />
    </el-steps>

    <div class="wizard-body">
      <!-- Step 1: 选择数据源 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-table
          :data="datasourceList"
          v-loading="datasourceLoading"
          highlight-current-row
          border
          size="small"
          @current-change="handleDatasourceSelect"
          style="width: 100%"
          max-height="360"
         class="aether-table">
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.subType || row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>

      <!-- Step 2: 编写查询 -->
      <div v-show="currentStep === 1" class="step-content">
        <el-form label-width="90px">
          <el-form-item label="查询类型">
            <el-radio-group v-model="config.queryType">
              <el-radio value="SQL">SQL 查询</el-radio>
              <el-radio value="API">API 调用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <div v-if="config.queryType === 'SQL'" class="query-editor">
          <SqlEditor v-model="config.sqlTemplate" height="220px" />
        </div>
        <div v-else class="query-editor">
          <ApiConfigForm v-model="apiForm" />
        </div>
      </div>

      <!-- Step 3: 预览数据 + 列映射 -->
      <div v-show="currentStep === 2" class="step-content">
        <div class="preview-toolbar">
          <el-button type="primary" :loading="previewLoading" @click="runPreview">
            <el-icon><CaretRight /></el-icon> 执行预览
          </el-button>
        </div>

        <DataPreview
          :columns="previewColumns"
          :rows="previewRows"
          :loading="previewLoading"
          :error="previewError"
        />

        <div v-if="previewColumns.length > 0" class="column-mapping">
          <el-divider>列映射配置</el-divider>
          <el-alert type="info" :closable="false" style="margin-bottom: 12px">
            <template #title>
              请将查询结果的列映射到图表的轴上。<strong>X 轴</strong>通常是分类/时间维度，<strong>Y 轴</strong>是数值度量。
            </template>
          </el-alert>
          <el-form label-width="100px" inline>
            <el-form-item label="X 轴(维度)">
              <el-select v-model="columnMapping.xAxis" placeholder="选择列" clearable style="width: 180px">
                <el-option
                  v-for="col in previewColumns"
                  :key="col.name"
                  :label="col.name"
                  :value="col.name"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="Y 轴(度量)">
              <el-select
                v-model="columnMapping.yAxis"
                placeholder="选择列"
                multiple
                clearable
                style="width: 240px"
              >
                <el-option
                  v-for="col in previewColumns"
                  :key="col.name"
                  :label="col.name"
                  :value="col.name"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="系列分组" tooltip="用于分组柱状图，按此列的值拆分为多个系列">
              <el-select v-model="columnMapping.category" placeholder="可选，用于分组图表" clearable style="width: 180px">
                <el-option
                  v-for="col in previewColumns"
                  :key="col.name"
                  :label="col.name"
                  :value="col.name"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- Step 4: 选择图表类型 -->
      <div v-show="currentStep === 3" class="step-content">
        <el-form label-width="90px">
          <el-form-item label="卡片名称">
            <el-input v-model="config.cardName" placeholder="请输入卡片名称" clearable style="width: 300px" />
          </el-form-item>
          <el-form-item label="图表类型">
            <el-radio-group v-model="config.chartType" class="chart-type-group">
              <el-radio-button v-for="ct in chartTypes" :key="ct.value" :value="ct.value">
                <el-icon><component :is="ct.icon" /></el-icon>
                <span>{{ ct.label }}</span>
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>

        <div class="summary-section">
          <el-divider>配置摘要</el-divider>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="数据源">{{ selectedDatasource?.name || '-' }}</el-descriptions-item>
            <el-descriptions-item label="查询类型">{{ config.queryType }}</el-descriptions-item>
            <el-descriptions-item label="图表类型">{{ chartTypeLabel }}</el-descriptions-item>
            <el-descriptions-item label="X 轴">{{ columnMapping.xAxis || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Y 轴">{{ columnMapping.yAxis?.join(', ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="系列分组">{{ columnMapping.category || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
      <el-button v-if="currentStep < 3" type="primary" :disabled="!canNext" @click="handleNext">
        下一步
      </el-button>
      <el-button v-if="currentStep === 3" type="primary" :disabled="!canSubmit" @click="handleSubmit">
        完成
      </el-button>
      <el-button @click="emit('update:visible', false)">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { CaretRight, Histogram, TrendCharts, PieChart, DataAnalysis, Grid, Odometer } from '@element-plus/icons-vue'
import { previewDatasourceQuery } from '@/api/dashboard'
import request from '@/utils/request'
import SqlEditor from './SqlEditor.vue'
import ApiConfigForm from './ApiConfigForm.vue'
import DataPreview from './DataPreview.vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  dashboardId: { type: [Number, String], required: true }
})

const emit = defineEmits(['update:visible', 'created'])

const currentStep = ref(0)

const datasourceList = ref([])
const datasourceLoading = ref(false)
const selectedDatasource = ref(null)

const config = reactive({
  queryType: 'SQL',
  sqlTemplate: '',
  chartType: 'bar',
  cardName: ''
})

const apiForm = ref({
  apiUrl: '',
  apiMethod: 'GET',
  apiHeaders: '',
  apiBody: '',
  responseDataPath: ''
})

const columnMapping = reactive({
  xAxis: '',
  yAxis: [],
  category: ''
})

const previewLoading = ref(false)
const previewError = ref('')
const previewColumns = ref([])
const previewRows = ref([])

const chartTypes = [
  { value: 'bar', label: '柱状图', icon: Histogram },
  { value: 'line', label: '折线图', icon: TrendCharts },
  { value: 'pie', label: '饼图', icon: PieChart },
  { value: 'groupedBar', label: '分组柱状图', icon: DataAnalysis },
  { value: 'kpi', label: '指标卡', icon: Odometer },
  { value: 'table', label: '表格', icon: Grid }
]

const chartTypeLabel = computed(() => {
  return chartTypes.find(ct => ct.value === config.chartType)?.label || config.chartType
})

const canNext = computed(() => {
  if (currentStep.value === 0) return !!selectedDatasource.value
  if (currentStep.value === 1) {
    if (config.queryType === 'SQL') return !!config.sqlTemplate.trim()
    return !!apiForm.value.apiUrl?.trim()
  }
  if (currentStep.value === 2) return previewColumns.value.length > 0
  return true
})

const canSubmit = computed(() => {
  return !!config.chartType && !!config.cardName?.trim()
})

async function loadDatasources() {
  datasourceLoading.value = true
  try {
    const res = await request({ url: '/api/datasource/list', method: 'get', params: { pageNum: 1, pageSize: 1000 } })
    datasourceList.value = res.rows || res.data || []
  } catch (e) {
    console.warn('加载数据源列表失败:', e)
  } finally {
    datasourceLoading.value = false
  }
}

function handleDatasourceSelect(row) {
  selectedDatasource.value = row
}

function handleNext() {
  if (currentStep.value === 1 && previewColumns.value.length === 0) {
    runPreview()
  }
  if (currentStep.value === 2 && !config.cardName?.trim()) {
    const dsName = selectedDatasource.value?.name || '数据源'
    const typeName = chartTypes.find(ct => ct.value === config.chartType)?.label || '图表'
    config.cardName = `${dsName} - ${typeName}`
  }
  currentStep.value++
}

async function runPreview() {
  if (!selectedDatasource.value) return
  previewLoading.value = true
  previewError.value = ''
  previewColumns.value = []
  previewRows.value = []

  const payload = {
    datasourceId: selectedDatasource.value.datasourceId || selectedDatasource.value.id,
    queryType: config.queryType,
    sqlTemplate: config.queryType === 'SQL' ? config.sqlTemplate : undefined,
    apiUrl: config.queryType === 'API' ? apiForm.value.apiUrl : undefined,
    apiMethod: config.queryType === 'API' ? apiForm.value.apiMethod : undefined,
    apiHeaders: config.queryType === 'API' ? apiForm.value.apiHeaders : undefined,
    apiBody: config.queryType === 'API' ? apiForm.value.apiBody : undefined,
    responseDataPath: config.queryType === 'API' ? apiForm.value.responseDataPath : undefined
  }

  try {
    const res = await previewDatasourceQuery(payload)
    if (res.code === 200 && res.data) {
      previewColumns.value = res.data.columns || []
      previewRows.value = res.data.rows || []
    } else {
      previewError.value = res.msg || '预览失败'
    }
  } catch (e) {
    previewError.value = e.message || '预览请求失败'
  } finally {
    previewLoading.value = false
  }
}

function handleSubmit() {
  const result = {
    datasourceId: selectedDatasource.value.datasourceId || selectedDatasource.value.id,
    queryType: config.queryType,
    sqlTemplate: config.queryType === 'SQL' ? config.sqlTemplate : undefined,
    apiUrl: config.queryType === 'API' ? apiForm.value.apiUrl : undefined,
    apiMethod: config.queryType === 'API' ? apiForm.value.apiMethod : undefined,
    apiHeaders: config.queryType === 'API' ? apiForm.value.apiHeaders : undefined,
    apiBody: config.queryType === 'API' ? apiForm.value.apiBody : undefined,
    responseDataPath: config.queryType === 'API' ? apiForm.value.responseDataPath : undefined,
    chartType: config.chartType,
    columnMapping: JSON.stringify(columnMapping),
    cardName: config.cardName
  }
  emit('created', result)
  emit('update:visible', false)
}

function resetWizard() {
  currentStep.value = 0
  selectedDatasource.value = null
  config.queryType = 'SQL'
  config.sqlTemplate = ''
  config.chartType = 'bar'
  config.cardName = ''
  apiForm.value = { apiUrl: '', apiMethod: 'GET', apiHeaders: '', apiBody: '', responseDataPath: '' }
  columnMapping.xAxis = ''
  columnMapping.yAxis = []
  columnMapping.category = ''
  previewColumns.value = []
  previewRows.value = []
  previewError.value = ''
}

watch(() => props.visible, (val) => {
  if (val) loadDatasources()
})
</script>

<style scoped>
.wizard-body {
  min-height: 360px;
}

.step-content {
  padding: 8px 0;
}

.query-editor {
  margin-top: 8px;
}

.preview-toolbar {
  margin-bottom: 12px;
}

.column-mapping {
  margin-top: 16px;
}

.chart-type-group .el-radio-button {
  margin-bottom: 8px;
}

.chart-type-group .el-radio-button :deep(.el-radio-button__inner) {
  display: flex;
  align-items: center;
  gap: 6px;
}

.summary-section {
  margin-top: 12px;
}
</style>

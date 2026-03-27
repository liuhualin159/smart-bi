<template>
  <div class="quality-report-panel">
    <el-card>
      <template #header>
        <div class="report-header">
          <span>质量报告</span>
          <div>
            <el-select v-model="selectedTableIds" multiple collapse-tags placeholder="选择表（空=全部）" style="width: 280px; margin-right: 12px">
              <el-option v-for="t in tableList" :key="t.id" :label="t.tableComment || t.tableName" :value="t.id" />
            </el-select>
            <el-button type="primary" icon="Document" :loading="loading" @click="generateReport">生成报告</el-button>
            <el-button type="success" icon="Download" :loading="exporting" :disabled="!reportData" @click="exportExcel">导出Excel</el-button>
          </div>
        </div>
      </template>
      <div v-if="reportData" class="report-content">
        <el-row :gutter="16" class="score-row">
          <el-col v-for="t in reportData.tables" :key="t.tableId" :span="6">
            <div class="score-card" :class="getScoreClass(t.score)">
              <div class="score-label">{{ t.tableName }}</div>
              <div class="score-value">{{ t.score != null ? t.score : '-' }}</div>
              <div class="score-meta">{{ t.calculatedAt ? formatTime(t.calculatedAt) : '未计算' }}</div>
            </div>
          </el-col>
        </el-row>
        <el-collapse v-for="t in reportData.tables" :key="t.tableId" class="table-detail">
          <el-collapse-item :title="`${t.tableName} - 规则结果`" :name="t.tableId">
            <el-table :data="t.ruleResults || []" border size="small" class="aether-table">
              <el-table-column label="规则类型" width="100" prop="ruleTypeLabel" />
              <el-table-column label="结果" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.passed ? 'success' : 'danger'" size="small">{{ row.passed ? '通过' : '未通过' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="总行数" prop="totalRows" width="90" />
              <el-table-column label="失败行数" prop="failedRows" width="100" />
              <el-table-column label="说明" prop="message" />
            </el-table>
            <div v-if="(t.rootCauses || []).length > 0" class="root-causes">
              <div class="root-title">根因（未通过规则）</div>
              <ul>
                <li v-for="(rc, i) in t.rootCauses" :key="i">{{ rc.ruleTypeLabel }}: {{ rc.message }}（失败率 {{ rc.failRate }}）</li>
              </ul>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
      <el-empty v-else-if="!loading" description="点击「生成报告」查看" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { generateQualityReport, exportQualityReport } from '@/api/quality'

const { proxy } = getCurrentInstance()
const props = defineProps({ tableList: { type: Array, default: () => [] } })

const selectedTableIds = ref([])
const loading = ref(false)
const exporting = ref(false)
const reportData = ref(null)

function getScoreClass(score) {
  if (score == null) return 'score-unknown'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-warning'
  return 'score-bad'
}

function formatTime(v) {
  if (!v) return ''
  const d = new Date(v)
  return d.toLocaleString('zh-CN')
}

async function generateReport() {
  loading.value = true
  reportData.value = null
  try {
    const res = await generateQualityReport({ tableIds: selectedTableIds.value })
    reportData.value = res.data || res
  } catch (e) {
    proxy.$modal.msgError(e.msg || '生成失败')
  } finally {
    loading.value = false
  }
}

async function exportExcel() {
  if (!reportData.value) return
  exporting.value = true
  try {
    const res = await exportQualityReport({ tableIds: selectedTableIds.value })
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = 'quality_report_' + Date.now() + '.xlsx'
    a.click()
    URL.revokeObjectURL(a.href)
    proxy.$modal.msgSuccess('导出成功')
  } catch (e) {
    proxy.$modal.msgError(e?.msg || e?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.report-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.score-row { margin-bottom: 20px; }
.score-card {
  padding: 16px; border-radius: 8px; text-align: center; background: #f5f7fa;
}
.score-card .score-label { font-size: 12px; color: #666; }
.score-card .score-value { font-size: 28px; font-weight: 600; margin: 8px 0; }
.score-card .score-meta { font-size: 11px; color: #999; }
.score-card.score-good .score-value { color: #67c23a; }
.score-card.score-warning .score-value { color: #e6a23c; }
.score-card.score-bad .score-value { color: #f56c6c; }
.table-detail { margin-top: 16px; }
.root-causes { margin-top: 12px; padding: 12px; background: #fef0f0; border-radius: 4px; }
.root-title { font-weight: 500; margin-bottom: 8px; }
</style>

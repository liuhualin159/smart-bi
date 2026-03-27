<template>
  <div class="quality-test-panel">
    <el-card>
      <template #header>
        <span>规则抽样测试</span>
      </template>
      <el-form :inline="true" class="test-form">
        <el-form-item label="选择表">
          <el-select v-model="testTableId" placeholder="请选择表" style="width: 240px">
            <el-option v-for="t in tableList" :key="t.id" :label="t.tableComment || t.tableName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="抽样行数">
          <el-input-number v-model="sampleSize" :min="100" :max="10000" :step="500" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="VideoPlay" :loading="testing" @click="runTest">运行测试</el-button>
          <el-button type="success" :loading="calculating" @click="calculateScore">计算评分</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="testResult" class="result-card">
      <template #header>
        <span>测试结果</span>
        <el-tag :type="(testResult.passed || 0) === (testResult.totalRules || 0) ? 'success' : 'warning'" size="small" style="margin-left: 8px">
          {{ testResult.passed }}/{{ testResult.totalRules }} 通过
        </el-tag>
      </template>
      <div v-if="testResult.error" class="error-msg">{{ testResult.error }}</div>
      <el-table v-else :data="testResult.results || []" border size="small" class="aether-table">
        <el-table-column label="规则类型" width="100">
          <template #default="{ row }">{{ ruleLabel(row.ruleType) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'" size="small">{{ row.passed ? '通过' : '未通过' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总行数" prop="totalRows" width="90" />
        <el-table-column label="失败行数" prop="failedRows" width="100" />
        <el-table-column label="说明" prop="message" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { testQualityRules, calculateQualityScore } from '@/api/quality'

const { proxy } = getCurrentInstance()
const props = defineProps({ tableList: { type: Array, default: () => [] } })

const testTableId = ref(null)
const sampleSize = ref(1000)
const testing = ref(false)
const calculating = ref(false)
const testResult = ref(null)

const RULE_LABELS = { COMPLETENESS: '完整性', ACCURACY: '准确性', CONSISTENCY: '一致性', UNIQUENESS: '唯一性', TIMELINESS: '及时性' }
function ruleLabel(t) { return RULE_LABELS[t] || t }

async function runTest() {
  if (!testTableId.value) {
    proxy.$modal.msgWarning('请选择表')
    return
  }
  testing.value = true
  testResult.value = null
  try {
    const res = await testQualityRules({ tableId: testTableId.value, sampleSize: sampleSize.value })
    testResult.value = res.data || res
  } catch (e) {
    proxy.$modal.msgError(e.msg || '测试失败')
  } finally {
    testing.value = false
  }
}

async function calculateScore() {
  if (!testTableId.value) {
    proxy.$modal.msgWarning('请选择表')
    return
  }
  calculating.value = true
  try {
    const res = await calculateQualityScore({ tableId: testTableId.value })
    proxy.$modal.msgSuccess('评分已计算：' + (res.data?.score ?? res?.score ?? '-'))
    runTest()
  } catch (e) {
    proxy.$modal.msgError(e.msg || '计算失败')
  } finally {
    calculating.value = false
  }
}
</script>

<style scoped>
.test-form { margin-bottom: 0; }
.result-card { margin-top: 16px; }
.error-msg { color: var(--el-color-danger); }
</style>

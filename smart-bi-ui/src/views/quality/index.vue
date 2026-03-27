<template>
  <div class="app-container quality-container">
    <el-tabs v-model="activeTab" class="quality-tabs">
      <el-tab-pane label="规则配置" name="rules">
        <QualityRulePanel
          :table-list="tableList"
          @refresh="loadTableList"
        />
      </el-tab-pane>
      <el-tab-pane label="规则测试" name="test">
        <QualityTestPanel
          :table-list="tableList"
        />
      </el-tab-pane>
      <el-tab-pane label="质量报告" name="report">
        <QualityReportPanel
          :table-list="tableList"
        />
      </el-tab-pane>
      <el-tab-pane label="告警" name="alert">
        <QualityAlertPanel :table-list="tableList" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="Quality">
import { ref, onMounted } from 'vue'
import { listTableMetadata } from '@/api/metadata'
import QualityRulePanel from './components/QualityRulePanel.vue'
import QualityTestPanel from './components/QualityTestPanel.vue'
import QualityReportPanel from './components/QualityReportPanel.vue'
import QualityAlertPanel from './components/QualityAlertPanel.vue'

const activeTab = ref('rules')
const tableList = ref([])

async function loadTableList() {
  try {
    const res = await listTableMetadata({ pageNum: 1, pageSize: 500 })
    tableList.value = res.rows || res.data || []
  } catch (e) {
    console.warn('加载表列表失败', e)
    tableList.value = []
  }
}

onMounted(loadTableList)
</script>

<style scoped>
.quality-container {
  padding: 16px 20px;
}
.quality-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.quality-tabs :deep(.el-tabs__item) {
  font-size: 15px;
}
.quality-tabs :deep(.el-tabs__content) {
  overflow: visible;
}
</style>

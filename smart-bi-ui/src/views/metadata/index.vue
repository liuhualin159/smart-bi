<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧：业务域树 -->
      <el-col :span="6">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>业务域</span>
              <el-button type="primary" size="small" icon="Plus" @click="handleAddDomain" v-hasPermi="['metadata:domain:add']">新增</el-button>
            </div>
          </template>
          <BusinessDomainTree 
            ref="domainTreeRef"
            @node-click="handleDomainClick"
            @refresh="loadTables"
          />
        </el-card>
      </el-col>

      <!-- 右侧：表管理和指标管理 -->
      <el-col :span="18">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <!-- 表管理（字段管理在表的弹窗中） -->
          <el-tab-pane label="表管理" name="table">
            <TableList 
              ref="tableListRef"
              :domain-id="selectedDomainId"
              :initial-table-name="route.query.tableName"
            />
          </el-tab-pane>

          <!-- 指标管理 -->
          <el-tab-pane label="指标管理" name="metric">
            <MetricList 
              ref="metricListRef"
              :domain-id="selectedDomainId"
            />
          </el-tab-pane>

          <!-- 表关系（推荐 join） -->
          <el-tab-pane label="表关系" name="relation">
            <RelationView ref="relationViewRef" />
          </el-tab-pane>

          <!-- 歧义优化 / 智能标注 -->
          <el-tab-pane label="歧义优化" name="ambiguity">
            <AmbiguityList ref="ambiguityListRef" :initial-table-name="route.query.tableName" />
          </el-tab-pane>

          <!-- Few-shot 示例（NL2SQL 参考示例） -->
          <el-tab-pane label="Few-shot 示例" name="fewshot">
            <FewshotExampleList ref="fewshotListRef" />
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Metadata">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import BusinessDomainTree from './components/BusinessDomainTree.vue'
import TableList from './components/TableList.vue'
import MetricList from './components/MetricList.vue'
import RelationView from './components/RelationView.vue'
import AmbiguityList from './components/AmbiguityList.vue'
import FewshotExampleList from './components/FewshotExampleList.vue'

const route = useRoute()
const domainTreeRef = ref(null)
const tableListRef = ref(null)
const metricListRef = ref(null)
const relationViewRef = ref(null)
const ambiguityListRef = ref(null)
const fewshotListRef = ref(null)

const activeTab = ref('table')
const selectedDomainId = ref(null)

function handleDomainClick(domainId) {
  selectedDomainId.value = domainId
  loadTables()
}

function handleTabChange(tabName) {
  if (tabName === 'table') {
    loadTables()
  } else if (tabName === 'metric') {
    loadMetrics()
  } else if (tabName === 'relation') {
    if (relationViewRef.value) relationViewRef.value.getList()
  } else if (tabName === 'ambiguity') {
    if (ambiguityListRef.value) ambiguityListRef.value.getList()
  } else if (tabName === 'fewshot') {
    if (fewshotListRef.value) fewshotListRef.value.getList()
  }
}

onMounted(() => {
  syncTabFromRoute()
})

function syncTabFromRoute() {
  const q = route.query || {}
  if (q.tab === 'ambiguity') activeTab.value = 'ambiguity'
  else if (q.tab === 'metric') activeTab.value = 'metric'
  else if (q.tab === 'relation') activeTab.value = 'relation'
  else if (q.tab === 'fewshot') activeTab.value = 'fewshot'
  else if (q.tab === 'table' || q.tableName || q.highlightTableId) activeTab.value = 'table'
  // 确保 tab 切换后对应列表能刷新（尤其是同路由仅 query 变化的场景）
  nextTick(() => handleTabChange(activeTab.value))
}

// 从歧义/字段冲突等场景通过 query 跳转到当前页时，需要同步切换 tab
watch(
  () => route.query,
  () => syncTabFromRoute(),
  { deep: true }
)

function loadTables() {
  if (tableListRef.value) {
    tableListRef.value.getList()
  }
}

function loadMetrics() {
  if (metricListRef.value) {
    metricListRef.value.getList()
  }
}

function handleAddDomain() {
  if (domainTreeRef.value) {
    domainTreeRef.value.handleAdd()
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

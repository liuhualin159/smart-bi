<template>
  <div class="relation-view">
    <div class="relation-view-toolbar">
      <el-radio-group v-model="viewMode" size="small" @change="onViewModeChange">
        <el-radio-button value="graph">
          <el-icon><Share /></el-icon>
          <span>血缘图</span>
        </el-radio-button>
        <el-radio-button value="list">
          <el-icon><List /></el-icon>
          <span>列表</span>
        </el-radio-button>
      </el-radio-group>
    </div>

    <div v-show="viewMode === 'graph'" class="relation-view-graph">
      <RelationGraph ref="graphRef" @refreshed="onGraphRefreshed" />
    </div>

    <div v-show="viewMode === 'list'" class="relation-view-list">
      <RelationList ref="listRef" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Share, List } from '@element-plus/icons-vue'
import RelationGraph from './RelationGraph.vue'
import RelationList from './RelationList.vue'

const viewMode = ref('graph')
const graphRef = ref(null)
const listRef = ref(null)

function onViewModeChange(mode) {
  if (mode === 'list' && listRef.value) {
    listRef.value.getList()
  } else if (mode === 'graph' && graphRef.value) {
    graphRef.value.load()
  }
}

function onGraphRefreshed() {
  if (viewMode.value === 'list' && listRef.value) {
    listRef.value.getList()
  }
}

function getList() {
  if (viewMode.value === 'graph' && graphRef.value) {
    graphRef.value.load()
  } else if (listRef.value) {
    listRef.value.getList()
  }
}

defineExpose({
  getList
})
</script>

<style scoped>
.relation-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 220px);
  min-height: 420px;
}

.relation-view-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.relation-view-toolbar :deep(.el-radio-button__inner) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.relation-view-graph {
  flex: 1;
  min-height: 400px;
  display: flex;
  flex-direction: column;
}

.relation-view-list {
  min-height: 400px;
}
</style>

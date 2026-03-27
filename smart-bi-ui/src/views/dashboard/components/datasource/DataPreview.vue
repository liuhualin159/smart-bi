<template>
  <div class="data-preview">
    <el-skeleton v-if="props.loading" :rows="6" animated />

    <el-alert
      v-else-if="props.error"
      :title="props.error"
      type="error"
      show-icon
      :closable="false"
    />

    <el-empty v-else-if="!props.rows || props.rows.length === 0" description="暂无数据" />

    <el-table
      v-else
      :data="displayRows"
      border
      stripe
      size="small"
      max-height="400"
      style="width: 100%"
     class="aether-table">
      <el-table-column
        v-for="col in props.columns"
        :key="col.name"
        :prop="col.name"
        :label="col.name"
        min-width="120"
        show-overflow-tooltip>
        <template #header>
          <div class="column-header">
            <span>{{ col.name }}</span>
            <el-tag size="small" type="info" class="column-type">{{ col.type }}</el-tag>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="props.rows && props.rows.length > maxRows" class="preview-tip">
      仅展示前 {{ maxRows }} 行，共 {{ props.rows.length }} 行
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const maxRows = 100

const props = defineProps({
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' }
})

const displayRows = computed(() => {
  if (!props.rows) return []
  return props.rows.slice(0, maxRows)
})
</script>

<style scoped>
.data-preview {
  width: 100%;
}

.column-header {
  display: flex;
  align-items: center;
  gap: 4px;
}

.column-type {
  font-size: 10px;
  transform: scale(0.85);
}

.preview-tip {
  margin-top: 8px;
  text-align: center;
  font-size: 12px;
  color: #909399;
}
</style>

<template>
  <div class="data-discovery">
    <div class="discovery-header">
      <span class="discovery-title">数据发现</span>
      <el-button link type="primary" size="small" @click="collapsed = !collapsed">
        {{ collapsed ? '展开' : '收起' }}
        <el-icon><ArrowDown v-if="collapsed" /><ArrowUp v-else /></el-icon>
      </el-button>
    </div>
    <el-collapse-transition>
      <div v-show="!collapsed" class="discovery-body">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索表名、注释"
          clearable
          size="small"
          class="search-input"
          :prefix-icon="Search"
        />
        <div v-loading="tableLoading" class="table-list">
          <div
            v-for="t in filteredTables"
            :key="t.id"
            class="table-item"
            :class="{ active: selectedTable?.id === t.id }"
            @click="selectTable(t)"
          >
            <el-icon class="table-icon"><Grid /></el-icon>
            <div class="table-info">
              <span class="table-name">{{ t.tableName }}</span>
              <span class="table-desc">{{ t.tableComment || t.businessDescription || '—' }}</span>
            </div>
          </div>
          <el-empty v-if="!tableLoading && filteredTables.length === 0" description="暂无数据表" />
        </div>

        <!-- 展开的表详情 -->
        <template v-if="selectedTable">
          <el-divider content-position="left">
            <span class="detail-title">{{ selectedTable.tableName }}</span>
          </el-divider>
          <el-descriptions :column="1" border size="small" class="table-meta">
            <el-descriptions-item label="表注释">{{ selectedTable.tableComment || '—' }}</el-descriptions-item>
            <el-descriptions-item label="业务描述">{{ selectedTable.businessDescription || '—' }}</el-descriptions-item>
            <el-descriptions-item label="粒度">{{ selectedTable.grainDesc || '—' }}</el-descriptions-item>
          </el-descriptions>
          <div class="field-section">
            <div class="section-label">字段</div>
            <div class="field-list">
              <div v-for="f in fields" :key="f.id" class="field-item">
                <span class="field-name">{{ f.fieldName }}</span>
                <el-tag size="small" type="info">{{ f.fieldType || '—' }}</el-tag>
                <span class="field-comment">{{ f.fieldComment || f.businessAlias || '' }}</span>
              </div>
              <el-empty v-if="!fieldLoading && fields.length === 0" description="无字段信息" />
            </div>
          </div>
          <div class="preview-section">
            <div class="section-label">数据预览</div>
            <el-button type="primary" size="small" :loading="previewLoading" @click="loadPreview">
              {{ previewData ? '刷新预览' : '加载预览' }}
            </el-button>
            <div v-if="previewData" class="preview-table-wrap">
              <el-table :data="previewData.data" border size="small" max-height="220" class="aether-table">
                <el-table-column
                  v-for="col in previewData.columns"
                  :key="col"
                  :prop="col"
                  :label="col"
                  show-overflow-tooltip
                  min-width="100"
                />
              </el-table>
            </div>
          </div>
          <div class="quick-actions">
            <el-button type="primary" size="small" @click="emitAskWithTable(false)">
              基于该表提问
            </el-button>
            <el-button size="small" :disabled="!previewData" @click="emitAskWithTable(true)">
              查看预览后提问
            </el-button>
          </div>
        </template>
      </div>
    </el-collapse-transition>
  </div>
</template>

<script setup name="DataDiscovery">
import { ref, computed, watch } from 'vue'
import { Search, Grid, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import { listTableMetadata, listFieldMetadataByTable } from '@/api/metadata'
import { previewTable } from '@/api/explore'

const emit = defineEmits(['ask-with-table'])

const collapsed = ref(true)
const searchKeyword = ref('')
const tableLoading = ref(false)
const tableList = ref([])
const selectedTable = ref(null)
const fields = ref([])
const fieldLoading = ref(false)
const previewLoading = ref(false)
const previewData = ref(null)

const filteredTables = computed(() => {
  const kw = (searchKeyword.value || '').trim().toLowerCase()
  if (!kw) return tableList.value
  return tableList.value.filter(
    t =>
      (t.tableName || '').toLowerCase().includes(kw) ||
      (t.tableComment || '').toLowerCase().includes(kw) ||
      (t.businessDescription || '').toLowerCase().includes(kw)
  )
})

function loadTables() {
  tableLoading.value = true
  listTableMetadata({
    pageNum: 1,
    pageSize: 500,
    nl2sqlVisibilityLevel: undefined
  })
    .then(res => {
      tableList.value = res.rows || []
    })
    .catch(() => {
      tableList.value = []
    })
    .finally(() => {
      tableLoading.value = false
    })
}

function selectTable(t) {
  if (selectedTable.value?.id === t.id) return
  selectedTable.value = t
  fields.value = []
  previewData.value = null
  fieldLoading.value = true
  listFieldMetadataByTable(t.id)
    .then(res => {
      fields.value = res.data || []
    })
    .catch(() => {
      fields.value = []
    })
    .finally(() => {
      fieldLoading.value = false
    })
}

function loadPreview() {
  if (!selectedTable.value) return
  previewLoading.value = true
  previewTable(selectedTable.value.id, 100)
    .then(res => {
      if (res.code === 200 && res.data) {
        previewData.value = {
          columns: res.data.columns || [],
          data: res.data.data || []
        }
      }
    })
    .catch(() => {})
    .finally(() => {
      previewLoading.value = false
    })
}

function emitAskWithTable(afterPreview) {
  if (!selectedTable.value) return
  const tableName = selectedTable.value.tableName
  const tableComment = selectedTable.value.tableComment || selectedTable.value.businessDescription || tableName
  const suggestedQuestion = `从${tableName}表中查询数据`
  emit('ask-with-table', {
    tableName,
    tableComment,
    suggestedQuestion,
    afterPreview
  })
}

watch(selectedTable, (v) => {
  if (!v) {
    fields.value = []
    previewData.value = null
  }
})

loadTables()
</script>

<style scoped>
.data-discovery {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.discovery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.discovery-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  letter-spacing: 0.02em;
}
.discovery-body {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}
.search-input {
  width: 220px;
  margin-bottom: 12px;
}
.table-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 16px;
}
.table-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
}
.table-item:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.08);
}
.table-item.active {
  border-color: var(--el-color-primary);
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f4ff 100%);
}
.table-icon {
  color: var(--el-color-primary);
  flex-shrink: 0;
}
.table-info {
  flex: 1;
  min-width: 0;
}
.table-name {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}
.table-desc {
  display: block;
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-title {
  font-size: 14px;
  font-weight: 600;
}
.table-meta {
  margin-bottom: 12px;
}
.field-section,
.preview-section {
  margin-bottom: 12px;
}
.section-label {
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  margin-bottom: 8px;
}
.field-list {
  max-height: 140px;
  overflow-y: auto;
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}
.field-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 13px;
}
.field-name {
  font-weight: 500;
  min-width: 120px;
}
.field-comment {
  color: #64748b;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.preview-table-wrap {
  margin-top: 8px;
}
.quick-actions {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  gap: 10px;
}
</style>

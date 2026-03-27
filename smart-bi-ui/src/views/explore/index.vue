<template>
  <div class="explore-page">
    <div class="explore-layout">
      <!-- 左侧：表列表（卡片网格） -->
      <div class="table-section">
        <div class="section-header">
          <h2 class="section-title">数据表</h2>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索表名、注释"
            clearable
            style="width: 200px"
            :prefix-icon="Search"
          />
        </div>
        <div v-loading="tableLoading" class="table-grid">
          <div
            v-for="t in filteredTables"
            :key="t.id"
            class="table-card"
            :class="{ active: selectedTable?.id === t.id }"
            @click="selectTable(t)"
          >
            <div class="card-icon">
              <el-icon :size="24"><Grid /></el-icon>
            </div>
            <div class="card-name">{{ t.tableName }}</div>
            <div class="card-desc">{{ t.tableComment || t.businessDescription || '—' }}</div>
          </div>
        </div>
      </div>

      <!-- 右侧：表详情抽屉 -->
      <el-drawer
        v-model="drawerVisible"
        :title="drawerTitle"
        size="520px"
        direction="rtl"
        destroy-on-close
        class="table-detail-drawer"
      >
        <template v-if="selectedTable">
          <div class="drawer-body">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="表名">{{ selectedTable.tableName }}</el-descriptions-item>
              <el-descriptions-item label="表注释">{{ selectedTable.tableComment || '—' }}</el-descriptions-item>
              <el-descriptions-item label="业务描述">{{ selectedTable.businessDescription || '—' }}</el-descriptions-item>
              <el-descriptions-item label="粒度">{{ selectedTable.grainDesc || '—' }}</el-descriptions-item>
            </el-descriptions>

            <el-divider content-position="left">字段</el-divider>
            <div class="field-list">
              <div v-for="f in fields" :key="f.id" class="field-item">
                <span class="field-name">{{ f.fieldName }}</span>
                <el-tag size="small" type="info">{{ f.fieldType || '—' }}</el-tag>
                <span class="field-comment">{{ f.fieldComment || f.businessAlias || '' }}</span>
              </div>
              <el-empty v-if="!fieldLoading && fields.length === 0" description="无字段信息" />
            </div>

            <el-divider content-position="left">数据预览</el-divider>
            <el-button type="primary" size="small" :loading="previewLoading" @click="loadPreview">
              {{ previewData ? '刷新预览' : '加载预览' }}
            </el-button>
            <div v-if="previewData" class="preview-table-wrap">
              <el-table :data="previewData.data" border size="small" max-height="280" class="aether-table">
                <el-table-column
                  v-for="col in previewData.columns"
                  :key="col"
                  :prop="col"
                  :label="col"
                  show-overflow-tooltip
                />
              </el-table>
            </div>

            <div class="drawer-footer">
              <el-button type="primary" @click="goToQuery">去智能问数</el-button>
            </div>
          </div>
        </template>
      </el-drawer>
    </div>

    <!-- 分析模板库 -->
    <div class="template-section">
      <h2 class="section-title">分析模板</h2>
      <div class="template-grid">
        <div
          v-for="(tpl, idx) in templates"
          :key="idx"
          class="template-card"
          @click="applyTemplate(tpl)"
        >
          <div class="tpl-icon">{{ tpl.icon }}</div>
          <div class="tpl-name">{{ tpl.name }}</div>
          <div class="tpl-desc">{{ tpl.description }}</div>
          <span class="tpl-hint">点击试用</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="Explore">
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Grid } from '@element-plus/icons-vue'
import { listTableMetadata, listFieldMetadataByTable } from '@/api/metadata'
import { previewTable } from '@/api/explore'

const router = useRouter()
const searchKeyword = ref('')
const tableLoading = ref(false)
const tableList = ref([])
const selectedTable = ref(null)
const drawerVisible = ref(false)
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

const drawerTitle = computed(() =>
  selectedTable.value ? `表详情 - ${selectedTable.value.tableName}` : '表详情'
)

const templates = ref([
  { icon: '📊', name: '销售额汇总', description: '按区域/产品汇总销售额', question: '上月各区域销售额是多少？' },
  { icon: '📈', name: '同比环比', description: '同比环比增长分析', question: '销售额同比增长 Top 5 的产品' },
  { icon: '👥', name: '用户对比', description: '渠道用户数量对比', question: '各渠道用户数量对比' },
  { icon: '💰', name: '预算执行', description: '各部门预算执行率', question: '各部门预算执行率' },
  { icon: '📉', name: '成本占比', description: '各成本项占比分析', question: '各成本项占比' }
])

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
  selectedTable.value = t
  drawerVisible.value = true
  fields.value = []
  previewData.value = null
  fieldLoading.value = true
  listFieldMetadataByTable(t.id).then(res => {
    fields.value = res.data || []
  }).catch(() => {
    fields.value = []
  }).finally(() => {
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

function goToQuery() {
  router.push({ path: '/bi/query' })
}

function applyTemplate(tpl) {
  router.push({
    path: '/bi/query',
    query: { q: tpl.question }
  })
}

loadTables()
</script>

<style scoped>
.explore-page {
  padding: 20px;
  background: linear-gradient(180deg, #fafbfc 0%, #f0f2f5 100%);
  min-height: calc(100vh - 84px);
}
.explore-layout {
  display: flex;
  gap: 20px;
}
.table-section {
  flex: 1;
  min-width: 0;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: 0.02em;
}
.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.table-card {
  padding: 16px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8eaed;
  cursor: pointer;
  transition: all 0.22s ease;
}
.table-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 4px 14px rgba(64, 158, 255, 0.12);
}
.table-card.active {
  border-color: var(--el-color-primary);
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f4ff 100%);
}
.card-icon {
  color: var(--el-color-primary);
  margin-bottom: 10px;
}
.card-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
}
.card-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.table-detail-drawer :deep(.el-drawer__header) {
  margin-bottom: 16px;
}
.drawer-body {
  padding-right: 8px;
}
.field-list {
  max-height: 200px;
  overflow-y: auto;
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
  margin-top: 12px;
}
.drawer-footer {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}
.template-section {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e8eaed;
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}
.template-card {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  cursor: pointer;
  transition: all 0.22s ease;
}
.template-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 6px 18px rgba(64, 158, 255, 0.14);
  transform: translateY(-2px);
}
.tpl-icon {
  font-size: 28px;
  margin-bottom: 12px;
}
.tpl-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 6px;
}
.tpl-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
  margin-bottom: 8px;
}
.tpl-hint {
  font-size: 11px;
  color: var(--el-color-primary);
}
</style>

<template>
  <el-dialog
    v-model="visible"
    title="数据溯源"
    width="700px"
    destroy-on-close
    @close="handleClose"
  >
    <div v-if="record" class="trace-content">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="生成SQL">
          <el-input
            :model-value="record.generatedSql"
            type="textarea"
            :rows="4"
            readonly
          />
        </el-descriptions-item>
        <el-descriptions-item label="执行SQL">
          <el-input
            :model-value="record.executedSql"
            type="textarea"
            :rows="4"
            readonly
          />
        </el-descriptions-item>
        <el-descriptions-item label="涉及表" v-if="record.involvedTables">
          <span>{{ formatInvolvedTables(record.involvedTables) }}</span>
        </el-descriptions-item>
      </el-descriptions>
      <div class="trace-actions">
        <el-button type="primary" plain @click="copySql(record.generatedSql)">
          复制生成SQL
        </el-button>
        <el-button type="primary" plain @click="copySql(record.executedSql)">
          复制执行SQL
        </el-button>
      </div>
    </div>
    <el-skeleton v-else :rows="5" animated />
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="TraceDialog">
import { ref, watch } from 'vue'
import { getQueryRecord } from '@/api/query'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  queryId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const record = ref(null)

watch(() => props.modelValue, val => {
  visible.value = val
  if (val && props.queryId) {
    loadRecord()
  }
}, { immediate: true })

watch(visible, val => {
  emit('update:modelValue', val)
})

function loadRecord() {
  record.value = null
  if (!props.queryId) return
  getQueryRecord(props.queryId).then(res => {
    if (res.code === 200) record.value = res.data
  }).catch(() => {
    ElMessage.error('获取溯源信息失败')
  })
}

function formatInvolvedTables(involvedTables) {
  if (!involvedTables) return '-'
  try {
    const arr = typeof involvedTables === 'string' ? JSON.parse(involvedTables) : involvedTables
    return Array.isArray(arr) ? arr.join(', ') : String(involvedTables)
  } catch {
    return involvedTables
  }
}

function copySql(sql) {
  if (!sql) return
  navigator.clipboard?.writeText(sql).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function handleClose() {
  record.value = null
}
</script>

<style scoped>
.trace-content {
  padding: 0 8px;
}
.trace-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>

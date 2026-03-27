<template>
  <el-form label-width="100px" class="api-config-form">
    <el-form-item label="API URL">
      <el-input v-model="form.apiUrl" placeholder="请输入 API 地址" clearable />
    </el-form-item>

    <el-form-item label="请求方法">
      <el-radio-group v-model="form.apiMethod">
        <el-radio value="GET">GET</el-radio>
        <el-radio value="POST">POST</el-radio>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="请求头">
      <div class="headers-list">
        <div v-for="(header, index) in headers" :key="index" class="header-row">
          <el-input v-model="header.key" placeholder="Header Key" style="width: 40%" />
          <el-input v-model="header.value" placeholder="Header Value" style="width: 40%; margin-left: 8px" />
          <el-button link type="danger" @click="removeHeader(index)" style="margin-left: 8px">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button type="primary" link @click="addHeader">
          <el-icon><Plus /></el-icon> 添加 Header
        </el-button>
      </div>
    </el-form-item>

    <el-form-item v-if="form.apiMethod === 'POST'" label="请求体">
      <el-input v-model="form.apiBody" type="textarea" :rows="4" placeholder="请输入请求体 (JSON)" />
    </el-form-item>

    <el-form-item label="数据路径">
      <el-input v-model="form.responseDataPath" placeholder="如 data.records" clearable />
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Delete, Plus } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({
      apiUrl: '',
      apiMethod: 'GET',
      apiHeaders: '',
      apiBody: '',
      responseDataPath: ''
    })
  }
})

const emit = defineEmits(['update:modelValue'])

const form = ref({ ...props.modelValue })
const headers = ref(parseHeaders(props.modelValue.apiHeaders))

function parseHeaders(headersStr) {
  if (!headersStr) return []
  try {
    const obj = typeof headersStr === 'string' ? JSON.parse(headersStr) : headersStr
    return Object.entries(obj).map(([key, value]) => ({ key, value }))
  } catch {
    return []
  }
}

function serializeHeaders() {
  const obj = {}
  headers.value.forEach(h => {
    if (h.key && h.key.trim()) {
      obj[h.key.trim()] = h.value || ''
    }
  })
  return Object.keys(obj).length > 0 ? JSON.stringify(obj) : ''
}

function addHeader() {
  headers.value.push({ key: '', value: '' })
}

function removeHeader(index) {
  headers.value.splice(index, 1)
  emitUpdate()
}

function emitUpdate() {
  emit('update:modelValue', {
    ...form.value,
    apiHeaders: serializeHeaders()
  })
}

watch(form, () => emitUpdate(), { deep: true })
watch(headers, () => emitUpdate(), { deep: true })

watch(() => props.modelValue, (val) => {
  form.value = { ...val }
  headers.value = parseHeaders(val.apiHeaders)
}, { deep: true })
</script>

<style scoped>
.api-config-form {
  padding: 10px 0;
}
.headers-list {
  width: 100%;
}
.header-row {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
</style>

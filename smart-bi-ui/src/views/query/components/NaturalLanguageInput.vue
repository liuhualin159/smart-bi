<template>
  <div class="natural-language-input">
    <div class="input-wrapper">
      <el-input
        v-model="question"
        type="textarea"
        :rows="4"
        placeholder="请输入您的问题，例如：查询最近一个月的销售额"
        :maxlength="500"
        show-word-limit
        @keyup.ctrl.enter="handleSubmit"
      />
    </div>
    <div class="input-actions">
      <el-button type="primary" icon="Search" @click="handleSubmit" :loading="isLoading">查询</el-button>
      <el-button icon="Delete" @click="handleClear" :disabled="isLoading">清空</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { getCurrentInstance } from 'vue'

const props = defineProps({
  initialQuestion: { type: String, default: '' },
  /** 由父组件传入，表示查询请求进行中（优先于本地 loading） */
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['query', 'clear'])

const { proxy } = getCurrentInstance()
const question = ref(props.initialQuestion || '')

const isLoading = computed(() => props.loading)

watch(() => props.initialQuestion, (v) => {
  if (v) question.value = v
})

function setQuestion(v) {
  question.value = v || ''
}

defineExpose({ setQuestion })

function handleSubmit() {
  const q = question.value.trim()
  
  // 输入验证
  if (!q) {
    proxy.$modal.msgWarning('请输入问题')
    return
  }
  
  if (q.length > 500) {
    proxy.$modal.msgWarning('问题长度不能超过500字符')
    return
  }
  
  // 特殊字符检查
  const dangerousChars = /[<>\"'%;()&+]/
  if (dangerousChars.test(q)) {
    proxy.$modal.msgWarning('问题包含特殊字符，请重新输入')
    return
  }
  
  emit('query', q)
}

function handleClear() {
  question.value = ''
  emit('clear')
}
</script>

<style scoped>
.natural-language-input {
  margin-bottom: 20px;
}

.input-wrapper {
  position: relative;
}

.input-actions {
  margin-top: 10px;
  text-align: right;
}
</style>

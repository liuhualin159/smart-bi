<template>
  <div class="chat-input-wrap">
    <div class="input-inner">
      <el-input
        v-model="question"
        type="textarea"
        :rows="2"
        :maxrows="6"
        placeholder="输入您的问题，例如：上个月销售环比、各区域销售额 Top 5…"
        :maxlength="500"
        show-word-limit
        resize="none"
        class="chat-textarea"
        @keydown.enter.ctrl="handleSubmit"
      />
      <div class="input-actions">
        <el-button type="primary" :icon="Search" @click="handleSubmit" :loading="loading">发送</el-button>
        <el-button :icon="Delete" @click="handleClear" :disabled="loading">清空</el-button>
      </div>
    </div>
  </div>
</template>

<script setup name="QueryChatInput">
import { ref, watch } from 'vue'
import { getCurrentInstance } from 'vue'
import { Search, Delete } from '@element-plus/icons-vue'

const props = defineProps({
  initialQuestion: { type: String, default: '' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['query', 'clear'])

const { proxy } = getCurrentInstance()
const question = ref(props.initialQuestion || '')

watch(() => props.initialQuestion, (v) => {
  if (v) question.value = v
})

function setQuestion(v) {
  question.value = v || ''
}
defineExpose({ setQuestion })

function handleSubmit() {
  const q = question.value.trim()
  if (!q) {
    proxy.$modal.msgWarning('请输入问题')
    return
  }
  if (q.length > 500) {
    proxy.$modal.msgWarning('问题长度不能超过500字符')
    return
  }
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
.chat-input-wrap {
  padding: 12px 0 0;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  background: transparent;
}

.input-inner {
  background: var(--chat-input-bg);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 12px 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
  position: relative;
}
.input-inner:focus-within {
  border-color: rgba(0, 242, 255, 0.58);
  box-shadow: 0 0 0 2px rgba(0, 242, 255, 0.18), 0 0 24px rgba(0, 242, 255, 0.14);
}

.chat-textarea :deep(.el-textarea__inner) {
  border: none;
  background: transparent;
  box-shadow: none;
  font-size: 14px;
  line-height: 1.5;
  padding: 0;
  min-height: 48px;
  color: #dfe2ef;
}

.chat-textarea :deep(.el-input__count) {
  color: rgba(185, 202, 203, 0.65);
  background: transparent;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}
</style>

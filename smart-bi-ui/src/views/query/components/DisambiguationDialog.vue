<template>
  <el-dialog
    v-model="visible"
    title="请澄清问题"
    width="500px"
    destroy-on-close
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <p class="disambig-hint">当前理解不够确定，请选择或补充说明：</p>
    <el-radio-group v-model="selectedChoice" class="disambig-choices">
      <el-radio
        v-for="(q, idx) in questions"
        :key="idx"
        :label="q"
      >
        {{ q }}
      </el-radio>
    </el-radio-group>
    <el-input
      v-model="customInput"
      placeholder="或输入您的澄清（可选）"
      class="disambig-custom"
      clearable
    />
    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :disabled="!canConfirm" @click="handleConfirm">确认并继续</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="DisambiguationDialog">
import { ref, watch, computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  questions: { type: Array, default: () => [] },
  originalQuestion: { type: String, default: '' },
  suggestedSql: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const visible = ref(false)
const selectedChoice = ref('')
const customInput = ref('')

watch(() => props.modelValue, val => {
  visible.value = val
  if (val) {
    selectedChoice.value = props.questions?.[0] || ''
    customInput.value = ''
  }
}, { immediate: true })

watch(visible, val => {
  emit('update:modelValue', val)
})

const finalQuestion = computed(() => {
  const original = (props.originalQuestion || '').trim()
  const answer = (customInput.value || '').trim()
  if (!answer) return original
  return `${original}。用户补充：${answer}`.trim()
})

const canConfirm = computed(() => {
  return (customInput.value || '').trim().length > 0
})

function handleConfirm() {
  emit('confirm', {
    question: finalQuestion.value,
    selectedChoice: selectedChoice.value,
    answer: (customInput.value || '').trim()
  })
  visible.value = false
}

function handleCancel() {
  emit('cancel')
  visible.value = false
}

function handleClose() {
  emit('cancel')
}
</script>

<style scoped>
.disambig-hint {
  margin-bottom: 16px;
  color: #606266;
}
.disambig-choices {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}
.disambig-custom {
  width: 100%;
}
</style>

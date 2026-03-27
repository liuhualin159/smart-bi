<template>
  <div class="sql-editor">
    <Codemirror
      v-model="code"
      :style="{ height: props.height || '200px' }"
      :extensions="extensions"
      :autofocus="true"
      placeholder="请输入 SELECT 查询语句..."
      @change="handleChange"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Codemirror } from 'vue-codemirror'
import { sql } from '@codemirror/lang-sql'
import { oneDark } from '@codemirror/theme-one-dark'

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: String, default: '200px' },
})

const emit = defineEmits(['update:modelValue'])

const code = ref(props.modelValue)
const extensions = [sql(), oneDark]

watch(() => props.modelValue, (val) => {
  if (val !== code.value) code.value = val
})

function handleChange(value) {
  emit('update:modelValue', value)
}
</script>

<style scoped>
.sql-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}
</style>

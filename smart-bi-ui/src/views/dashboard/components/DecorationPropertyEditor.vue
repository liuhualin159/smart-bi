<template>
  <el-form label-width="80px" size="small">
    <!-- title-bar -->
    <template v-if="decorationType === 'title-bar'">
      <el-form-item label="标题文字">
        <el-input v-model="form.title" @input="emitUpdate" />
      </el-form-item>
      <el-form-item label="字号">
        <el-input-number v-model="form.fontSize" :min="12" :max="72" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="字体颜色">
        <el-color-picker v-model="form.fontColor" show-alpha @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="背景">
        <el-input v-model="form.backgroundColor" placeholder="CSS颜色或渐变" @input="emitUpdate" />
      </el-form-item>
      <el-form-item label="装饰色">
        <el-color-picker v-model="form.borderColor" show-alpha @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- border-box -->
    <template v-if="decorationType === 'border-box'">
      <el-form-item label="边框风格">
        <el-select v-model="form.borderStyle" @change="emitUpdate">
          <el-option label="标准科技风" value="tech-border-1" />
          <el-option label="圆角科技风" value="tech-border-2" />
          <el-option label="极简" value="tech-border-3" />
        </el-select>
      </el-form-item>
      <el-form-item label="边框颜色">
        <el-color-picker v-model="form.borderColor" show-alpha @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="边框粗细">
        <el-input-number v-model="form.borderWidth" :min="1" :max="10" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="流光动画">
        <el-switch v-model="form.animationEnabled" @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- divider-line -->
    <template v-if="decorationType === 'divider-line'">
      <el-form-item label="线条样式">
        <el-select v-model="form.lineStyle" @change="emitUpdate">
          <el-option label="实线" value="solid" />
          <el-option label="虚线" value="dashed" />
          <el-option label="点线" value="dotted" />
          <el-option label="渐变" value="gradient" />
        </el-select>
      </el-form-item>
      <el-form-item label="线条颜色">
        <el-color-picker v-model="form.lineColor" show-alpha @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="线条粗细">
        <el-input-number v-model="form.lineWidth" :min="1" :max="20" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="方向">
        <el-radio-group v-model="form.orientation" @change="emitUpdate">
          <el-radio-button value="horizontal">水平</el-radio-button>
          <el-radio-button value="vertical">垂直</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  decorationType: {
    type: String,
    required: true
  },
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue'])

const form = reactive({ ...props.modelValue })

watch(() => props.modelValue, (val) => {
  Object.assign(form, val)
}, { deep: true })

function emitUpdate() {
  emit('update:modelValue', { ...form })
}
</script>

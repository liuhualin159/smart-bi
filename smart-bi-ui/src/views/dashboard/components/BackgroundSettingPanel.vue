<template>
  <div class="background-setting-panel">
    <!-- 背景类型切换 -->
    <div class="setting-section">
      <div class="section-label">背景类型</div>
      <el-radio-group v-model="config.type" @change="emitUpdate">
        <el-radio-button value="solid">纯色</el-radio-button>
        <el-radio-button value="gradient">渐变</el-radio-button>
        <el-radio-button value="image">图片</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 纯色模式 -->
    <div v-if="config.type === 'solid'" class="setting-section">
      <div class="section-label">背景颜色</div>
      <el-color-picker v-model="config.color" show-alpha @change="emitUpdate" />
    </div>

    <!-- 渐变模式 -->
    <div v-if="config.type === 'gradient'" class="setting-section">
      <div class="section-label">渐变方向</div>
      <el-select v-model="config.direction" @change="emitUpdate" style="width: 100%">
        <el-option label="从上到下" value="to bottom" />
        <el-option label="从左到右" value="to right" />
        <el-option label="左上到右下" value="to bottom right" />
        <el-option label="左下到右上" value="to top right" />
      </el-select>
      <div class="color-row">
        <div class="color-item">
          <span class="color-label">起始色</span>
          <el-color-picker v-model="config.colors[0]" show-alpha @change="emitUpdate" />
        </div>
        <div class="color-item">
          <span class="color-label">结束色</span>
          <el-color-picker v-model="config.colors[1]" show-alpha @change="emitUpdate" />
        </div>
      </div>
    </div>

    <!-- 图片模式 -->
    <div v-if="config.type === 'image'" class="setting-section">
      <div class="section-label">上传图片</div>
      <el-upload
        class="bg-uploader"
        :action="uploadUrl"
        :headers="uploadHeaders"
        :show-file-list="false"
        :on-success="handleUploadSuccess"
        :before-upload="handleBeforeUpload"
        accept="image/*"
      >
        <el-button size="small" type="primary">点击上传</el-button>
      </el-upload>

      <div class="section-label" style="margin-top: 12px">图片地址</div>
      <el-input v-model="config.url" placeholder="图片URL" @input="emitUpdate" />

      <div class="section-label" style="margin-top: 12px">填充模式</div>
      <el-select v-model="config.fit" @change="emitUpdate" style="width: 100%">
        <el-option label="覆盖 (cover)" value="cover" />
        <el-option label="包含 (contain)" value="contain" />
        <el-option label="拉伸 (stretch)" value="stretch" />
      </el-select>

      <div class="section-label" style="margin-top: 12px">
        透明度: {{ config.opacity }}
      </div>
      <el-slider
        v-model="config.opacity"
        :min="0"
        :max="1"
        :step="0.05"
        @change="emitUpdate"
      />
    </div>

    <!-- 背景预览 -->
    <div class="setting-section">
      <div class="section-label">预览</div>
      <div class="preview-box" :style="previewStyle"></div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import { getCurrentInstance } from 'vue'
import { getToken } from '@/utils/auth'
import request from '@/utils/request'

const { proxy } = getCurrentInstance()

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue'])

const baseApi = request.defaults.baseURL
const uploadUrl = baseApi + '/common/upload'
const uploadHeaders = { Authorization: 'Bearer ' + getToken() }

// 深色预设：与看板设计器画布风格一致
const defaultConfig = {
  type: 'gradient',
  color: '#0e0f14',
  direction: 'to bottom right',
  colors: ['#0e0f14', '#1a1b26'],
  url: '',
  fit: 'cover',
  opacity: 1.0
}

const config = reactive({ ...defaultConfig })

watch(
  () => props.modelValue,
  (val) => {
    if (val && typeof val === 'object' && val.type) {
      Object.assign(config, { ...defaultConfig, ...val })
      if (val.colors) {
        config.colors = [...val.colors]
      }
    }
  },
  { immediate: true, deep: true }
)

function emitUpdate() {
  const result = { type: config.type }
  if (config.type === 'solid') {
    result.color = config.color
  } else if (config.type === 'gradient') {
    result.direction = config.direction
    result.colors = [...config.colors]
  } else if (config.type === 'image') {
    result.url = config.url
    result.fit = config.fit
    result.opacity = config.opacity
  }
  emit('update:modelValue', result)
}

function handleBeforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    proxy.$modal.msgError('只能上传图片文件')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    proxy.$modal.msgError('图片大小不能超过 5MB')
    return false
  }
  return true
}

function handleUploadSuccess(res) {
  if (res.code === 200) {
    config.url = res.fileName
    emitUpdate()
    proxy.$modal.msgSuccess('图片上传成功')
  } else {
    proxy.$modal.msgError(res.msg || '上传失败')
  }
}

const previewStyle = computed(() => {
  if (config.type === 'solid') {
    return { backgroundColor: config.color }
  }
  if (config.type === 'gradient') {
    return {
      background: `linear-gradient(${config.direction}, ${config.colors[0]}, ${config.colors[1]})`
    }
  }
  if (config.type === 'image' && config.url) {
    const fullUrl = config.url.startsWith('http') ? config.url : baseApi + config.url
    const fitValue = config.fit === 'stretch' ? '100% 100%' : config.fit
    return {
      backgroundImage: `url(${fullUrl})`,
      backgroundSize: fitValue,
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      opacity: config.opacity
    }
  }
  return {}
})
</script>

<style scoped>
.background-setting-panel {
  padding: 10px 0;
}

.setting-section {
  margin-bottom: 18px;
}

.section-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
  font-weight: 500;
}

.color-row {
  display: flex;
  gap: 20px;
  margin-top: 12px;
}

.color-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.color-label {
  font-size: 12px;
  color: #909399;
}

.preview-box {
  width: 200px;
  height: 120px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fafafa;
}

.bg-uploader {
  margin-bottom: 8px;
}
</style>

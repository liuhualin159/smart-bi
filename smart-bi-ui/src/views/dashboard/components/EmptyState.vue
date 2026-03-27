<template>
  <div class="empty-state">
    <el-empty 
      :image-size="200"
      :description="description"
    >
      <template #image>
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg" width="200" height="200">
          <path d="M512 128a384 384 0 1 0 0 768 384 384 0 0 0 0-768z m0 64a320 320 0 1 1 0 640 320 320 0 0 1 0-640z" fill="#C0C4CC"/>
          <path d="M512 320a32 32 0 0 1 32 32v128a32 32 0 0 1-64 0V352a32 32 0 0 1 32-32z" fill="#C0C4CC"/>
          <path d="M512 640a32 32 0 1 1 0 64 32 32 0 0 1 0-64z" fill="#C0C4CC"/>
        </svg>
      </template>
      <el-button v-if="showAction" type="primary" @click="$emit('action')">
        {{ actionText }}
      </el-button>
    </el-empty>
  </div>
</template>

<script setup name="EmptyState">
defineProps({
  description: {
    type: String,
    default: '暂无卡片，请添加卡片到看板'
  },
  showAction: {
    type: Boolean,
    default: true
  },
  actionText: {
    type: String,
    default: '添加卡片'
  }
})

defineEmits(['action'])
</script>

<style scoped>
.empty-state {
  padding: 60px 20px;
  text-align: center;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

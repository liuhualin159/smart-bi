<template>
  <div class="empty-result">
    <el-empty 
      :image-size="200"
      description="暂无查询结果"
    >
      <template #image>
        <svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg">
          <path d="M512 128a384 384 0 1 0 0 768 384 384 0 0 0 0-768z m0 64a320 320 0 1 1 0 640 320 320 0 0 1 0-640z" fill="#909399"/>
          <path d="M512 320a32 32 0 0 1 32 32v128a32 32 0 0 1-64 0V352a32 32 0 0 1 32-32z" fill="#909399"/>
          <path d="M512 640a32 32 0 1 1 0 64 32 32 0 0 1 0-64z" fill="#909399"/>
        </svg>
      </template>
      <el-button type="primary" @click="$emit('retry')">重新查询</el-button>
    </el-empty>
  </div>
</template>

<script setup name="EmptyResult">
defineEmits(['retry'])
</script>

<style scoped>
.empty-result {
  padding: 60px 20px;
  text-align: center;
}
</style>

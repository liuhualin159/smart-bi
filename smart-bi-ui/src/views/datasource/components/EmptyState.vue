<template>
  <div class="empty-state">
    <el-empty description="暂无数据源">
      <el-button type="primary" @click="handleAdd">创建数据源</el-button>
    </el-empty>
  </div>
</template>

<script setup>
const emit = defineEmits(['add'])

function handleAdd() {
  emit('add')
}
</script>

<style scoped>
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
</style>

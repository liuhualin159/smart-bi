<template>
  <div class="case-library">
    <div class="case-header">
      <span class="case-title">最佳实践案例</span>
      <el-button link type="primary" size="small" @click="collapsed = !collapsed">
        {{ collapsed ? '展开' : '收起' }}
        <el-icon><ArrowDown v-if="collapsed" /><ArrowUp v-else /></el-icon>
      </el-button>
    </div>
    <div v-show="!collapsed" class="case-grid">
      <div
        v-for="(item, idx) in cases"
        :key="idx"
        class="case-card"
        @click="emit('try', item.question)"
      >
        <span class="case-category">{{ item.category }}</span>
        <p class="case-question">{{ item.question }}</p>
        <span class="case-hint">点击试用</span>
      </div>
    </div>
  </div>
</template>

<script setup name="CaseLibrary">
import { ref } from 'vue'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'

defineEmits(['try'])

const collapsed = ref(false)

const cases = ref([
  { category: '销售分析', question: '上月各区域销售额是多少？' },
  { category: '销售分析', question: '销售额同比增长 Top 5 的产品' },
  { category: '用户分析', question: '各渠道用户数量对比' },
  { category: '用户分析', question: '本月新增用户环比变化' },
  { category: '财务分析', question: '各部门预算执行率' },
  { category: '财务分析', question: '各成本项占比' },
  { category: '趋势分析', question: '近12个月销售趋势' },
  { category: '排名分析', question: '销售额排名前10的客户' }
])
</script>

<style scoped>
.case-library {
  margin-bottom: 20px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}
.case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.case-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  letter-spacing: 0.02em;
}
.case-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}
.case-card {
  padding: 14px 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
}
.case-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
}
.case-category {
  display: inline-block;
  font-size: 11px;
  color: var(--el-color-primary);
  background: rgba(64, 158, 255, 0.08);
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}
.case-question {
  margin: 0 0 8px;
  font-size: 14px;
  color: #1e293b;
  line-height: 1.45;
}
.case-hint {
  font-size: 12px;
  color: #94a3b8;
}
</style>

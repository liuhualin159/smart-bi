<template>
  <el-dialog
    v-model="visible"
    title="欢迎使用智能问数"
    width="560px"
    :close-on-click-modal="false"
    class="onboarding-guide"
    @closed="handleClosed"
  >
    <el-steps :active="currentStep" finish-status="success" align-center class="onboarding-steps">
      <el-step title="输入问题" />
      <el-step title="查看结果" />
      <el-step title="溯源与下钻" />
    </el-steps>

    <div class="step-content">
      <div v-show="currentStep === 0" class="step-panel">
        <p class="step-desc">在输入框中用自然语言描述您想查询的数据，例如：</p>
        <ul class="example-list">
          <li>「上月销售额是多少？」</li>
          <li>「各区域用户数量对比」</li>
          <li>「同比环比增长 Top 5」</li>
        </ul>
        <p class="step-tip">系统将自动把您的问题转换为 SQL 并执行，支持纠错、指代消解和同比环比。</p>
      </div>
      <div v-show="currentStep === 1" class="step-panel">
        <p class="step-desc">查询结果以表格或图表展示，您可以：</p>
        <ul class="example-list">
          <li>切换图表类型（柱状图、折线图、饼图等）</li>
          <li>点击数字查看 SQL 溯源</li>
          <li>导出为 PNG、PDF 或 Excel</li>
        </ul>
      </div>
      <div v-show="currentStep === 2" class="step-panel">
        <p class="step-desc">深入探索数据：</p>
        <ul class="example-list">
          <li><strong>溯源</strong>：点击单元格查看生成与执行的 SQL</li>
          <li><strong>下钻</strong>：点击维度值下钻查看明细，面包屑可回退</li>
          <li><strong>结论</strong>：展开结论面板获取 AI 总结</li>
        </ul>
      </div>
    </div>

    <template #footer>
      <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
      <el-button v-if="currentStep < 2" type="primary" @click="currentStep++">下一步</el-button>
      <el-button v-else type="primary" @click="finish">开始使用</el-button>
      <el-button link @click="skip">跳过引导</el-button>
    </template>
  </el-dialog>
</template>

<script setup name="OnboardingGuide">
import { ref, computed, onMounted } from 'vue'

const STORAGE_KEY = 'smart-bi-onboarding-v1'

const visible = ref(false)
const currentStep = ref(0)

const shouldShow = () => {
  try {
    return !localStorage.getItem(STORAGE_KEY)
  } catch {
    return false
  }
}

function finish() {
  try { localStorage.setItem(STORAGE_KEY, 'done') } catch {}
  visible.value = false
}

function skip() {
  finish()
}

function handleClosed() {
  currentStep.value = 0
}

onMounted(() => {
  if (shouldShow()) {
    visible.value = true
  }
})

defineExpose({
  show: () => { visible.value = true },
  reset: () => {
    try { localStorage.removeItem(STORAGE_KEY) } catch {}
    visible.value = true
    currentStep.value = 0
  }
})
</script>

<style scoped>
.onboarding-guide :deep(.el-dialog__body) {
  padding: 24px 28px 20px;
}
.onboarding-steps {
  margin-bottom: 28px;
}
.step-panel {
  min-height: 160px;
}
.step-desc {
  color: #606266;
  margin-bottom: 16px;
  font-size: 15px;
  line-height: 1.6;
}
.example-list {
  margin: 0 0 16px 20px;
  padding: 0;
  list-style: none;
}
.example-list li {
  position: relative;
  padding-left: 12px;
  margin-bottom: 10px;
  color: #303133;
  font-size: 14px;
  line-height: 1.5;
}
.example-list li::before {
  content: '•';
  position: absolute;
  left: 0;
  color: var(--el-color-primary);
}
.step-tip {
  font-size: 13px;
  color: #909399;
  margin-top: 12px;
  padding: 10px 14px;
  background: #f4f4f5;
  border-radius: 6px;
}
</style>

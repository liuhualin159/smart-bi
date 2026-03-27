<template>
  <div class="query-page chat-layout">
    <div class="chat-container">
      <header class="chat-header">
        <div class="header-left">
          <span class="header-icon">
            <el-icon :size="22"><Monitor /></el-icon>
          </span>
          <span class="header-title">智能问数</span>
        </div>
        <el-button link type="info" size="small" @click="handleShowOnboarding">重新查看引导</el-button>
      </header>

      <!-- 对话消息区域 -->
      <main class="chat-messages" ref="messagesRef">
        <template v-for="(msg, index) in messages" :key="msg.id || index">
          <QueryChatMessage
            :message="msg"
            :async-task-id="index === pendingAssistantIndex ? asyncTaskId : null"
            :async-query-id="index === pendingAssistantIndex ? queryResult?.queryId : null"
            @try="handleCaseTry"
            @save-card="handleSaveCard"
            @chart-type-change="handleChartTypeChange"
            @feedback="handleFeedback"
            @filter-by="handleChartFilter"
            @retry="handleRetryFromMessage"
            @async-completed="handleAsyncTaskCompleted"
            @async-failed="handleAsyncTaskFailed"
          />
        </template>
      </main>

      <!-- 筛选器：有推荐筛选时显示 -->
      <div v-if="recommendedFilters.length > 0" class="chat-filters">
        <FilterPanel
          :filters="recommendedFilters"
          v-model="filterValues"
          @change="handleFilterChange"
        />
      </div>

      <!-- 底部输入区 -->
      <footer class="chat-footer">
        <QueryChatInput
          ref="chatInputRef"
          :initial-question="initialQuestion"
          :loading="queryLoading"
          @query="handleQuery"
          @clear="handleClear"
        />
      </footer>
    </div>

    <DisambiguationDialog
      v-model="showDisambiguation"
      :questions="disambiguationData?.questions || []"
      :original-question="disambiguationData?.originalQuestion || ''"
      :suggested-sql="disambiguationData?.suggestedSql"
      @confirm="handleDisambiguationConfirm"
    />
    <OnboardingGuide ref="onboardingRef" />
  </div>
</template>

<script setup name="Query">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getCurrentInstance } from 'vue'
import { Monitor } from '@element-plus/icons-vue'
import { executeQuery, recommendFilters, summarizeQuery } from '@/api/query'
import QueryChatMessage from './components/QueryChatMessage.vue'
import QueryChatInput from './components/QueryChatInput.vue'
import FilterPanel from './components/FilterPanel.vue'
import DisambiguationDialog from './components/DisambiguationDialog.vue'
import OnboardingGuide from './components/OnboardingGuide.vue'

const { proxy } = getCurrentInstance()
const route = useRoute()

const initialQuestion = computed(() => (route.query.q || '').toString())

const messagesRef = ref(null)
const chatInputRef = ref(null)
const onboardingRef = ref(null)

// 消息列表：欢迎 + 多轮 user/assistant
const messages = ref([])
const pendingAssistantIndex = ref(-1) // 当前正在填充的助手消息下标
/** 对话内思考态提示（不再使用全页遮罩） */
const thinkingMessageText = '我正在理解您的问题、生成 SQL 并执行查询，请稍候…'

const queryResult = ref(null)
const generatedSql = ref('')
const recommendedFilters = ref([])
const filterValues = ref({})
const currentQuestion = ref('')
const sessionKey = ref('')
const sessionId = ref(null)
const sessionTimeoutTimer = ref(null)
const asyncTaskId = ref(null)
const showDisambiguation = ref(false)
const disambiguationData = ref(null)
const queryLoading = ref(false)

// 欢迎语 + 最佳实践案例（首次提示词）
const welcomeCases = [
  { category: '销售分析', question: '上月各区域销售额是多少？' },
  { category: '销售分析', question: '上个月销售环比' },
  { category: '用户分析', question: '各渠道用户数量对比' },
  { category: '趋势分析', question: '近12个月销售趋势' },
  { category: '排名分析', question: '销售额排名前10的客户' }
]

function initWelcome() {
  if (messages.value.length === 0) {
    messages.value.push({
      id: 'welcome',
      role: 'system',
      content: '您好，我是智能数据助手。您可以直接用自然语言提问，例如：「上个月销售环比」「各区域销售额 Top 5」。我会为您生成 SQL、执行查询并以数据卡片和解读的形式展示结果。',
      cases: welcomeCases
    })
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function handleCaseTry(question) {
  chatInputRef.value?.setQuestion?.(question)
  handleQuery(question)
}

function handleShowOnboarding() {
  onboardingRef.value?.reset?.()
}

function handleQuery(question, isFilterChange = false) {
  currentQuestion.value = question
  const finalQuestion = buildQueryWithFilters(question, filterValues.value)

  if (!isFilterChange) {
    messages.value.push({ id: 'u-' + Date.now(), role: 'user', content: question })
    messages.value.push({
      id: 'a-' + Date.now(),
      role: 'assistant',
      status: 'thinking',
      statusText: thinkingMessageText
    })
    pendingAssistantIndex.value = messages.value.length - 1
  } else {
    if (pendingAssistantIndex.value >= 0) {
      const m = messages.value[pendingAssistantIndex.value]
      m.status = 'thinking'
      m.statusText = '我正在根据筛选条件重新查询，请稍候…'
      m.sql = undefined
      m.rowCount = undefined
      m.queryResult = undefined
      m.analysis = undefined
      m.error = undefined
    }
  }

  recommendFilters({ question: finalQuestion }).then(response => {
    if (response.code === 200) recommendedFilters.value = response.data || []
  }).catch(() => { recommendedFilters.value = [] })

  const requestParams = {
    question: finalQuestion,
    sessionKey: sessionKey.value,
    sessionId: sessionId.value
  }

  queryLoading.value = true

  executeQuery(requestParams).then(response => {
    if (response.code !== 200) {
      setAssistantError(response.msg || '查询失败')
      return
    }
    const data = response.data
    const record = data.queryRecord || data

    if (data.needDisambiguation) {
      disambiguationData.value = {
        questions: data.disambiguationQuestions || [],
        originalQuestion: finalQuestion,
        suggestedSql: data.suggestedSql,
        sessionId: data.sessionId,
        sessionKey: data.sessionKey
      }
      showDisambiguation.value = true
      return
    }

    generatedSql.value = record.generatedSql || ''

    if (data.isAsync && data.asyncTaskId) {
      asyncTaskId.value = data.asyncTaskId
      proxy.$modal.msgInfo(data.message || '查询已切换为异步任务模式')
      return
    }

    if (data.sessionId) sessionId.value = data.sessionId
    if (data.sessionKey) sessionKey.value = data.sessionKey
    resetSessionTimeout()

    let columns = []
    let resultData = []
    if (data.data) {
      resultData = data.data
      columns = resultData.length > 0 ? Object.keys(resultData[0]) : []
    } else if (record.result) {
      try {
        const result = JSON.parse(record.result)
        resultData = result.data || result
        columns = result.columns || (resultData.length > 0 ? Object.keys(resultData[0]) : [])
      } catch (e) {
        setAssistantError('解析查询结果失败')
        return
      }
    }

    const rowCount = data.rowCount !== undefined ? data.rowCount : resultData.length
    queryResult.value = {
      queryId: record.id,
      columns,
      data: resultData,
      qualityScores: data.qualityScores || []
    }

    updateAssistantMessage({
      question: finalQuestion,
      sql: record.generatedSql || record.executedSql || '',
      rowCount,
      queryResult: { ...queryResult.value },
      analysis: resultData.length === 0 ? '未查询到符合条件的数据，请尝试调整问题或筛选条件。' : ''
    })

    if (resultData.length > 0 && columns.length > 0) {
      loadAnalysisForCurrentMessage(record.id, 'table', columns, resultData)
    }
  }).catch(error => {
    setAssistantError('查询失败: ' + (error.msg || error.message || '未知错误'))
  }).finally(() => {
    queryLoading.value = false
    scrollToBottom()
  })
}

function updateAssistantMessage(payload) {
  if (pendingAssistantIndex.value < 0) return
  const m = messages.value[pendingAssistantIndex.value]
  if (!m || m.role !== 'assistant') return
  delete m.status
  delete m.statusText
  Object.assign(m, payload)
}

function setAssistantError(err) {
  if (pendingAssistantIndex.value >= 0) {
    const m = messages.value[pendingAssistantIndex.value]
    if (m?.role === 'assistant') {
      m.status = undefined
      m.statusText = undefined
      m.error = err
    }
  }
}

async function loadAnalysisForCurrentMessage(queryId, chartType, columns, data) {
  if (pendingAssistantIndex.value < 0) return
  try {
    const res = await summarizeQuery({
      queryId,
      chartType: chartType || 'table',
      columns,
      data
    })
    const summary = res.code === 200 && res.data?.summary ? res.data.summary : ''
    if (pendingAssistantIndex.value >= 0 && messages.value[pendingAssistantIndex.value]) {
      messages.value[pendingAssistantIndex.value].analysis = summary || '暂无自动解读，请查看上方数据。'
    }
  } catch (e) {
    if (pendingAssistantIndex.value >= 0 && messages.value[pendingAssistantIndex.value]) {
      messages.value[pendingAssistantIndex.value].analysis = '数据解读暂时不可用，请查看上方数据。'
    }
  }
  scrollToBottom()
}

function buildQueryWithFilters(question, filters) {
  if (!filters || Object.keys(filters).length === 0) return question
  const filterDescriptions = []
  for (const [fieldName, value] of Object.entries(filters)) {
    if (value !== null && value !== undefined && value !== '') {
      if (Array.isArray(value) && value.length > 0) {
        filterDescriptions.push(`${fieldName} 在 [${value.join(', ')}] 中`)
      } else {
        filterDescriptions.push(`${fieldName} = ${value}`)
      }
    }
  }
  if (filterDescriptions.length > 0) {
    return question + '，筛选条件：' + filterDescriptions.join('，')
  }
  return question
}

function handleChartFilter({ dimension, value }) {
  if (dimension && value != null) {
    filterValues.value = { ...filterValues.value, [dimension]: value }
    if (currentQuestion.value) handleQuery(currentQuestion.value, true)
  }
}

function getBaseQuestion(question) {
  const q = (question || '').trim()
  if (!q) return ''
  // 避免多轮澄清时把历史“澄清/答案”不断叠加到下一轮请求里
  return q.replace(/。?澄清：[\s\S]*$/, '').replace(/。?用户补充：[\s\S]*$/, '').trim()
}

function handleDisambiguationConfirm({ question: clarifiedQuestion, answer }) {
  showDisambiguation.value = false
  const snapshot = disambiguationData.value
  if (disambiguationData.value?.sessionId) sessionId.value = disambiguationData.value.sessionId
  if (disambiguationData.value?.sessionKey) sessionKey.value = disambiguationData.value.sessionKey
  disambiguationData.value = null

  const safeAnswer = (answer || '').trim()
  const baseQuestion = getBaseQuestion(snapshot?.originalQuestion || currentQuestion.value || clarifiedQuestion)
  if (!safeAnswer || !baseQuestion) return
  handleQuery(`${baseQuestion}。用户补充：${safeAnswer}`)
}

function handleFilterChange() {
  if (currentQuestion.value) handleQuery(currentQuestion.value, true)
}

function handleClear() {
  queryResult.value = null
  generatedSql.value = ''
  recommendedFilters.value = []
  filterValues.value = {}
  currentQuestion.value = ''
}

function handleRetryFromMessage() {
  if (currentQuestion.value) handleQuery(currentQuestion.value, true)
}

function resetSessionTimeout() {
  clearSessionTimeout()
  sessionTimeoutTimer.value = setTimeout(() => {
    proxy.$modal.msgWarning('会话已超时，对话历史已清空')
    messages.value = []
    initWelcome()
    pendingAssistantIndex.value = -1
    sessionKey.value = ''
    sessionId.value = null
  }, 30 * 60 * 1000)
}

function clearSessionTimeout() {
  if (sessionTimeoutTimer.value) {
    clearTimeout(sessionTimeoutTimer.value)
    sessionTimeoutTimer.value = null
  }
}

onMounted(() => {
  initWelcome()
  const q = initialQuestion.value.trim()
  if (q) handleQuery(q)
  scrollToBottom()
})

import { onBeforeUnmount } from 'vue'
onBeforeUnmount(() => {
  clearSessionTimeout()
})

function handleSaveCard(card) {
  proxy.$modal.msgSuccess('卡片已保存')
  console.log('保存的卡片:', card)
}

function handleChartTypeChange(chartType) {
  console.log('图表类型切换:', chartType)
}

function handleFeedback() {
  proxy.$modal.msgSuccess('反馈已提交')
}

function handleAsyncTaskCompleted(task) {
  if (task?.queryId && task?.data) {
    const columns = task.data.length > 0 ? Object.keys(task.data[0]) : []
    queryResult.value = {
      columns,
      data: task.data,
      queryId: task.queryId
    }
    updateAssistantMessage({
      question: currentQuestion.value,
      sql: generatedSql.value || '',
      rowCount: task.data.length,
      queryResult: { ...queryResult.value },
      analysis: ''
    })
    loadAnalysisForCurrentMessage(task.queryId, 'table', columns, task.data)
  }
  asyncTaskId.value = null
  proxy.$modal.msgSuccess('查询任务已完成')
  scrollToBottom()
}

function handleAsyncTaskFailed(task) {
  asyncTaskId.value = null
  setAssistantError('查询任务执行失败: ' + (task?.errorMessage || '未知错误'))
  proxy.$modal.msgError('查询任务执行失败')
}
</script>

<style scoped>
.query-page {
  --chat-primary: #00f2ff;
  --chat-secondary: #7000ff;
  --chat-text: #dfe2ef;
  --chat-text-muted: #b9cacb;
  --chat-border: rgba(58, 73, 75, 0.34);
  --chat-user-bg: linear-gradient(135deg, rgba(0, 242, 255, 0.2) 0%, rgba(0, 219, 231, 0.18) 100%);
  --chat-user-text: #e1fdff;
  --chat-assistant-bg: rgba(49, 53, 63, 0.42);
  --chat-chip-bg: rgba(112, 0, 255, 0.14);
  --chat-chip-hover: rgba(112, 0, 255, 0.22);
  --chat-code-bg: #0a0e17;
  --chat-input-bg: rgba(24, 27, 37, 0.84);
  --chat-glow: rgba(0, 242, 255, 0.2);
  --chat-grid: rgba(58, 73, 75, 0.2);
  color: var(--chat-text);
}

.chat-layout {
  min-height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle at 82% 8%, rgba(112, 0, 255, 0.12), transparent 33%),
    linear-gradient(180deg, #0f131c 0%, #0a0e17 100%);
  position: relative;
}
.chat-layout::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, var(--chat-grid) 1px, transparent 1px);
  background-size: 28px 28px;
  pointer-events: none;
  opacity: 0.16;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 1120px;
  margin: 0 auto;
  width: 100%;
  padding: 0 20px 20px;
  background: rgba(28, 31, 41, 0.48);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 14px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 0 0 1px rgba(58, 73, 75, 0.2);
  border: 1px solid var(--chat-border);
  position: relative;
  z-index: 1;
  animation: containerIn 0.4s ease-out;
}
@keyframes containerIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  background: linear-gradient(180deg, rgba(15, 19, 28, 0.85) 0%, rgba(15, 19, 28, 0.3) 100%);
  border-radius: 14px 14px 0 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 42px;
  height: 42px;
  border-radius: 4px;
  background: linear-gradient(135deg, rgba(112, 0, 255, 0.28) 0%, rgba(112, 0, 255, 0.18) 100%);
  color: #d1bcff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px var(--chat-glow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.header-icon:hover {
  transform: scale(1.02);
  box-shadow: 0 6px 16px var(--chat-glow);
}

.header-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--chat-primary);
  letter-spacing: 0.02em;
  font-family: 'Space Grotesk', 'Inter', sans-serif;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  min-height: 420px;
  scroll-behavior: smooth;
}

.chat-filters {
  padding: 0 20px 12px;
  flex-shrink: 0;
}

.chat-footer {
  flex-shrink: 0;
  padding: 0 20px 18px;
}
</style>

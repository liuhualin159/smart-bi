<template>
  <!-- 系统欢迎消息 -->
  <div v-if="message.role === 'system'" class="chat-message system-message">
    <div class="message-avatar system-avatar">
      <el-icon :size="20"><Monitor /></el-icon>
    </div>
    <div class="message-body system-body">
      <div class="welcome-title">智能数据助手</div>
      <p class="welcome-text">{{ message.content }}</p>
      <div v-if="message.cases?.length" class="quick-cases">
        <span class="quick-label">试试这些提问：</span>
        <div class="case-chips">
          <button
            v-for="(c, idx) in message.cases"
            :key="idx"
            class="case-chip"
            @click="$emit('try', c.question)"
          >
            {{ c.question }}
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 用户消息 -->
  <div v-else-if="message.role === 'user'" class="chat-message user-message">
    <div class="message-body user-body">
      <span class="user-text">{{ message.content }}</span>
    </div>
    <div class="message-avatar user-avatar">
      <el-icon :size="18"><User /></el-icon>
    </div>
  </div>

  <!-- 助手消息：加载中（含异步任务状态） -->
  <div v-else-if="message.role === 'assistant' && message.status === 'thinking'" class="chat-message assistant-message">
    <div class="message-avatar assistant-avatar">
      <el-icon :size="20"><Cpu /></el-icon>
    </div>
    <div class="message-body assistant-body thinking">
      <template v-if="asyncTaskId">
        <QueryStatus
          :task-id="asyncTaskId"
          :query-id="asyncQueryId"
          @completed="$emit('async-completed', $event)"
          @failed="$emit('async-failed', $event)"
        />
      </template>
      <template v-else>
        <div class="thinking-dots">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
        <span class="thinking-text">{{ message.statusText || '正在处理…' }}</span>
      </template>
    </div>
  </div>

  <!-- 助手消息：完整回复（SQL + 数据卡 + 分析） -->
  <div v-else-if="message.role === 'assistant'" class="chat-message assistant-message">
    <div class="message-avatar assistant-avatar">
      <el-icon :size="20"><Cpu /></el-icon>
    </div>
    <div class="message-body assistant-body">
      <!-- 第一块：原始 SQL + 返回条数 -->
      <div v-if="message.sql" class="reply-block block-sql">
        <div class="block-label">
          <el-icon><Document /></el-icon>
          <span>查询语句与结果量</span>
        </div>
        <pre class="sql-code">{{ message.sql }}</pre>
        <div v-if="message.rowCount != null" class="row-count">
          共返回 <strong>{{ message.rowCount }}</strong> 条数据
        </div>
      </div>

      <!-- 第二块：数据展示卡（表格/图表切换保留） -->
      <div v-if="message.queryResult" class="reply-block block-data">
        <div class="block-label">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据展示</span>
          <el-checkbox v-model="showSecondaryChart" class="linkage-toggle-inline">对比视图（表格+图表）</el-checkbox>
        </div>
        <div v-if="message.queryResult.data && message.queryResult.data.length > 0" class="chart-grid" :class="{ 'has-secondary': showSecondaryChart }">
          <div class="chart-panel">
            <ChartDisplay
              :columns="message.queryResult.columns"
              :data="message.queryResult.data"
              :query-id="message.queryResult.queryId"
              :question="message.question"
              :sql="message.sql"
              :quality-scores="message.queryResult.qualityScores || []"
              :fixed-chart-type="showSecondaryChart ? 'table' : null"
              @save-card="$emit('save-card', $event)"
              @chart-type-change="$emit('chart-type-change', $event)"
              @feedback="$emit('feedback')"
              @filter-by="$emit('filter-by', $event)"
            />
          </div>
          <div v-if="showSecondaryChart" class="chart-panel">
            <ChartDisplay
              :columns="message.queryResult.columns"
              :data="message.queryResult.data"
              :query-id="message.queryResult.queryId"
              :question="message.question"
              :sql="message.sql"
              :quality-scores="message.queryResult.qualityScores || []"
              :show-save-button="false"
              @filter-by="$emit('filter-by', $event)"
            />
          </div>
        </div>
        <EmptyResult v-else @retry="$emit('retry', message)" />
      </div>

      <!-- 第三块：基于数据的分析 -->
      <div v-if="message.analysis" class="reply-block block-analysis">
        <div class="block-label">
          <el-icon><Comment /></el-icon>
          <span>数据解读</span>
        </div>
        <div class="analysis-text">{{ message.analysis }}</div>
      </div>

      <!-- 错误态 -->
      <div v-if="message.error" class="reply-block block-error">
        <el-alert type="error" :title="message.error" show-icon :closable="false" />
      </div>
    </div>
  </div>
</template>

<script setup name="QueryChatMessage">
import { ref } from 'vue'
import { User, Cpu, Monitor, Document, DataAnalysis, Comment } from '@element-plus/icons-vue'
import ChartDisplay from './ChartDisplay.vue'
import EmptyResult from './EmptyResult.vue'
import QueryStatus from './QueryStatus.vue'

defineProps({
  message: { type: Object, required: true },
  asyncTaskId: { type: [String, Number], default: null },
  asyncQueryId: { type: [String, Number], default: null }
})

defineEmits(['try', 'save-card', 'chart-type-change', 'feedback', 'filter-by', 'retry', 'async-completed', 'async-failed'])

const showSecondaryChart = ref(false)
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: messageIn 0.35s ease-out;
}

@keyframes messageIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.system-avatar {
  background: linear-gradient(135deg, rgba(112, 0, 255, 0.25) 0%, rgba(112, 0, 255, 0.15) 100%);
  color: #d1bcff;
}

.user-avatar {
  background: rgba(0, 242, 255, 0.16);
  color: #00f2ff;
}

.assistant-avatar {
  background: linear-gradient(135deg, rgba(112, 0, 255, 0.24) 0%, rgba(0, 242, 255, 0.18) 100%);
  color: #d1bcff;
  border: 1px solid rgba(209, 188, 255, 0.28);
}

.message-body {
  flex: 1;
  min-width: 0;
}

.system-message .message-body { max-width: 100%; }
.user-message { flex-direction: row-reverse; }
.user-body { text-align: right; }

.user-body .user-text {
  display: inline-block;
  padding: 10px 16px;
  background: var(--chat-user-bg);
  color: var(--chat-user-text);
  border-radius: 10px;
  border-left: 2px solid rgba(0, 242, 255, 0.45);
  font-size: 14px;
  line-height: 1.5;
  max-width: 85%;
}

.assistant-body {
  padding: 12px 0;
}

.assistant-body.thinking {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  background: var(--chat-assistant-bg);
  border-radius: 10px;
  border: 1px solid var(--chat-border);
}

.thinking-dots {
  display: flex;
  gap: 4px;
}
.thinking-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--chat-primary);
  animation: dotPulse 1.2s ease-in-out infinite;
}
.thinking-dots .dot:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotPulse {
  0%, 100% { opacity: 0.4; transform: scale(0.9); }
  50% { opacity: 1; transform: scale(1); }
}

.thinking-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.welcome-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--chat-text);
  font-family: 'Space Grotesk', 'Inter', sans-serif;
  margin-bottom: 8px;
  letter-spacing: 0.02em;
}

.welcome-text {
  font-size: 14px;
  color: var(--chat-text-muted);
  line-height: 1.6;
  margin: 0 0 14px 0;
}

.quick-cases { margin-top: 12px; }
.quick-label {
  font-size: 12px;
  color: rgba(185, 202, 203, 0.75);
  display: block;
  margin-bottom: 8px;
}

.case-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.case-chip {
  padding: 6px 12px;
  font-size: 12px;
  color: #d1bcff;
  background: var(--chat-chip-bg);
  border: 1px solid var(--chat-border);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.case-chip:hover {
  background: var(--chat-chip-hover);
  border-color: rgba(112, 0, 255, 0.65);
  color: #e9ddff;
}

.reply-block {
  margin-top: 16px;
  padding: 14px 16px;
  background: var(--chat-assistant-bg);
  border-radius: 12px;
  border: 1px solid var(--chat-border);
}

.reply-block:first-child { margin-top: 0; }

.block-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--chat-text);
  margin-bottom: 10px;
}
.block-label .el-icon {
  color: var(--chat-primary);
  opacity: 0.9;
}

.linkage-toggle-inline {
  margin-left: auto;
  font-weight: normal;
  font-size: 12px;
}

.sql-code {
  margin: 0;
  padding: 14px 16px;
  background: var(--chat-code-bg);
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.55;
  overflow-x: auto;
  color: #e2e8f0;
  border: 1px solid rgba(255,255,255,0.06);
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
}

.row-count {
  margin-top: 8px;
  font-size: 13px;
  color: var(--chat-text-muted);
}

.chart-grid { display: block; }
.chart-grid.has-secondary { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-panel { min-width: 0; }

.analysis-text {
  font-size: 14px;
  line-height: 1.65;
  color: var(--chat-text-muted);
  white-space: pre-wrap;
}

.block-error { padding: 10px; }
</style>

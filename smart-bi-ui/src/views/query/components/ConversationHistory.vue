<template>
  <div class="conversation-history">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>对话历史</span>
          <el-button 
            v-if="conversationList.length > 0"
            type="text" 
            size="small"
            @click="handleClearHistory"
          >
            清空历史
          </el-button>
        </div>
      </template>
      
      <div class="history-list" ref="historyListRef">
        <div 
          v-for="(item, index) in conversationList" 
          :key="index"
          class="conversation-item"
        >
          <div class="question-item">
            <el-icon class="question-icon"><User /></el-icon>
            <div class="question-content">{{ item.question }}</div>
          </div>
          <div class="answer-item">
            <el-icon class="answer-icon"><ChatDotRound /></el-icon>
            <div class="answer-content">
              <div v-if="item.sql" class="sql-preview">
                <el-tag size="small" type="info">SQL</el-tag>
                <code>{{ item.sql }}</code>
              </div>
              <div v-if="item.resultSummary" class="result-summary">
                {{ item.resultSummary }}
              </div>
            </div>
          </div>
        </div>
        
        <el-empty 
          v-if="conversationList.length === 0"
          description="暂无对话历史"
          :image-size="100"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup name="ConversationHistory">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { User, ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  conversationList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['clear'])

const historyListRef = ref(null)

// 监听对话列表变化，自动滚动到底部
watch(() => props.conversationList, () => {
  nextTick(() => {
    scrollToBottom()
  })
}, { deep: true })

// 滚动到底部
function scrollToBottom() {
  if (historyListRef.value) {
    historyListRef.value.scrollTop = historyListRef.value.scrollHeight
  }
}

// 清空历史
function handleClearHistory() {
  emit('clear')
}

// 组件挂载
onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.conversation-history {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-list {
  max-height: 400px;
  overflow-y: auto;
}

.conversation-item {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.conversation-item:last-child {
  border-bottom: none;
}

.question-item,
.answer-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
}

.question-icon {
  color: #409EFF;
  margin-right: 10px;
  margin-top: 4px;
}

.answer-icon {
  color: #67C23A;
  margin-right: 10px;
  margin-top: 4px;
}

.question-content,
.answer-content {
  flex: 1;
  word-break: break-word;
}

.sql-preview {
  margin-bottom: 8px;
}

.sql-preview code {
  display: block;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  margin-top: 4px;
  overflow-x: auto;
}

.result-summary {
  color: #606266;
  font-size: 14px;
}
</style>

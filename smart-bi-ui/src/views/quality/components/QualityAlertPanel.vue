<template>
  <div class="quality-alert-panel">
    <el-card>
      <template #header>
        <span>质量告警</span>
      </template>
      <el-form :inline="true">
        <el-form-item label="表">
          <el-select v-model="alertTableId" placeholder="全部表" clearable style="width: 200px">
            <el-option label="全部表" :value="null" />
            <el-option v-for="t in tableList" :key="t.id" :label="t.tableComment || t.tableName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分阈值">
          <el-input-number v-model="scoreThreshold" :min="0" :max="100" />
          <span class="hint">低于此分值将触发告警</span>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" icon="Warning" :loading="checking" @click="runCheck">执行告警检查</el-button>
        </el-form-item>
      </el-form>
      <el-alert v-if="lastResult !== null" :title="`本次检查发送 ${lastResult} 条告警`" :type="lastResult > 0 ? 'warning' : 'success'" show-icon style="margin-top: 12px" />
      <p class="alert-desc">告警服务将检查各表最新评分，低于阈值时记录并通知。多通道（邮件/钉钉/企业微信）需在系统参数中配置。</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { checkQualityAlert } from '@/api/quality'

const { proxy } = getCurrentInstance()
defineProps({ tableList: { type: Array, default: () => [] } })

const alertTableId = ref(null)
const scoreThreshold = ref(60)
const checking = ref(false)
const lastResult = ref(null)

async function runCheck() {
  checking.value = true
  lastResult.value = null
  try {
    const res = await checkQualityAlert({ tableId: alertTableId.value, scoreThreshold: scoreThreshold.value })
    lastResult.value = res.data?.alertsSent ?? res?.alertsSent ?? 0
    proxy.$modal.msgSuccess('检查完成')
  } catch (e) {
    proxy.$modal.msgError(e.msg || '检查失败')
  } finally {
    checking.value = false
  }
}
</script>

<style scoped>
.hint { margin-left: 8px; color: #909399; font-size: 12px; }
.alert-desc { margin-top: 16px; color: #606266; font-size: 13px; line-height: 1.6; }
</style>

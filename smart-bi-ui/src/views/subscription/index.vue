<template>
  <div class="app-container subscription-container">
    <el-form :inline="true" class="search-form">
      <el-form-item label="类型">
        <el-select v-model="queryParams.subscribeType" placeholder="全部" clearable style="width: 140px" @change="loadList">
          <el-option label="看板" value="DASHBOARD" />
          <el-option label="卡片" value="CARD" />
          <el-option label="查询" value="QUERY" />
          <el-option label="质量报告" value="QUALITY_REPORT" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 110px" @change="loadList">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">查询</el-button>
        <el-button icon="Plus" @click="openDrawer()" v-hasPermi="['bi:subscription:add']">新增订阅</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="16">
      <el-col :span="selectedId ? 14 : 24">
        <el-table v-loading="loading" :data="list" border highlight-current-row @current-change="onRowSelect" class="aether-table">
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag size="small">{{ subscribeTypeLabel(row.subscribeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="目标ID" prop="targetId" width="90" />
          <el-table-column label="Cron" prop="scheduleCron" min-width="140" show-overflow-tooltip />
          <el-table-column label="渠道" prop="receiveChannels" width="140" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openDrawer(row)" v-hasPermi="['bi:subscription:edit']">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)" v-hasPermi="['bi:subscription:remove']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="loadList" />
      </el-col>
      <el-col v-if="selectedId" :span="10">
        <el-card shadow="never" class="push-history-card">
          <template #header>
            <span>推送历史</span>
            <el-button link type="primary" size="small" @click="loadPushRecords(selectedId)">刷新</el-button>
          </template>
          <el-table v-loading="recordsLoading" :data="pushRecords" size="small" max-height="360" class="aether-table">
            <el-table-column label="推送时间" prop="pushAt" width="160" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ row.status === 'SUCCESS' ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="重试" prop="retryCount" width="60" />
            <el-table-column label="备注" prop="remark" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="!recordsLoading && pushRecords.length === 0" description="暂无推送记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="drawerVisible" :title="editing?.id ? '编辑订阅' : '新增订阅'" size="420px" destroy-on-close @close="drawerClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="订阅类型" prop="subscribeType">
          <el-select v-model="form.subscribeType" placeholder="请选择" style="width: 100%" :disabled="!!editing?.id">
            <el-option label="看板" value="DASHBOARD" />
            <el-option label="卡片" value="CARD" />
            <el-option label="查询" value="QUERY" />
            <el-option label="质量报告" value="QUALITY_REPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标ID" prop="targetId">
          <el-input-number v-model="form.targetId" :min="1" placeholder="看板/卡片/查询/报告ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Cron" prop="scheduleCron">
          <el-input v-model="form.scheduleCron" placeholder="如 0 0 9 * * ? 表示每天9点" />
        </el-form-item>
        <el-form-item label="推送渠道" prop="receiveChannels">
          <el-input v-model="form.receiveChannels" placeholder="邮件,钉钉,企业微信 逗号分隔" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="Subscription">
import { ref, reactive, onMounted } from 'vue'
import { listSubscription, getSubscription, addSubscription, updateSubscription, delSubscription, getPushRecords } from '@/api/subscription'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const recordsLoading = ref(false)
const list = ref([])
const total = ref(0)
const pushRecords = ref([])
const selectedId = ref(null)
const queryParams = reactive({ pageNum: 1, pageSize: 10, subscribeType: null, status: null })
const drawerVisible = ref(false)
const editing = ref(null)
const formRef = ref(null)
const form = reactive({
  subscribeType: 'DASHBOARD',
  targetId: null,
  scheduleCron: '0 0 9 * * ?',
  receiveChannels: '邮件',
  status: 'ENABLED',
  remark: ''
})
const rules = {
  subscribeType: [{ required: true, message: '请选择订阅类型', trigger: 'change' }],
  targetId: [{ required: true, message: '请输入目标ID', trigger: 'blur' }],
  scheduleCron: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }],
  receiveChannels: [{ required: true, message: '请输入推送渠道', trigger: 'blur' }]
}

const SUBSCRIBE_LABELS = { DASHBOARD: '看板', CARD: '卡片', QUERY: '查询', QUALITY_REPORT: '质量报告' }
function subscribeTypeLabel(t) { return SUBSCRIBE_LABELS[t] || t }

async function loadList() {
  loading.value = true
  try {
    const res = await listSubscription(queryParams)
    list.value = res.rows || res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

function onRowSelect(row) {
  selectedId.value = row?.id ?? null
  if (selectedId.value) loadPushRecords(selectedId.value)
}

async function loadPushRecords(id) {
  recordsLoading.value = true
  try {
    const res = await getPushRecords(id)
    pushRecords.value = res.data || res || []
  } finally {
    recordsLoading.value = false
  }
}

function openDrawer(row) {
  editing.value = row || null
  if (row) {
    Object.assign(form, { ...row, status: row.status || 'ENABLED' })
  } else {
    Object.assign(form, {
      subscribeType: 'DASHBOARD',
      targetId: null,
      scheduleCron: '0 0 9 * * ?',
      receiveChannels: '邮件',
      status: 'ENABLED',
      remark: ''
    })
  }
  drawerVisible.value = true
}

function drawerClose() { editing.value = null }

async function submitForm() {
  await formRef.value?.validate()
  try {
    if (editing.value?.id) {
      await updateSubscription(form)
      ElMessage.success('修改成功')
    } else {
      await addSubscription(form)
      ElMessage.success('新增成功')
    }
    drawerVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm('确认删除该订阅？', '提示', { type: 'warning' })
  try {
    await delSubscription([row.id])
    ElMessage.success('删除成功')
    if (selectedId.value === row.id) selectedId.value = null
    loadList()
  } catch (e) {
    ElMessage.error(e?.message || '删除失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.subscription-container { padding: 16px 20px; }
.search-form { margin-bottom: 16px; }
.push-history-card { margin-top: 0; }
.push-history-card :deep(.el-card__header) { display: flex; justify-content: space-between; align-items: center; }
</style>

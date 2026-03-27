<template>
  <div class="app-container aether-list-page">
    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="任务列表" name="list">
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px" class="aether-filter-card">
      <el-form-item label="任务名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入任务名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="数据源" prop="datasourceId">
        <el-select v-model="queryParams.datasourceId" placeholder="请选择数据源" clearable style="width: 240px">
          <el-option
            v-for="ds in datasourceList"
            :key="ds.id"
            :label="ds.name"
            :value="ds.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 240px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
          <el-option label="暂停" value="PAUSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['etl:task:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['etl:task:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['etl:task:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="VideoPlay" :disabled="single" @click="handleTrigger" v-hasPermi="['etl:task:trigger']">执行</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- ETL任务列表 -->
    <el-table v-loading="loading" :data="etlTaskList" @selection-change="handleSelectionChange" v-if="etlTaskList.length > 0" class="aether-table">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="任务ID" align="center" prop="id" />
      <el-table-column label="任务名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="数据源ID" align="center" prop="datasourceId" />
      <el-table-column label="源类型" align="center" prop="sourceType">
        <template #default="scope">
          <el-tag v-if="scope.row.sourceType === 'TABLE'" type="primary">表</el-tag>
          <el-tag v-else-if="scope.row.sourceType === 'SQL'" type="success">SQL</el-tag>
          <el-tag v-else-if="scope.row.sourceType === 'API'" type="warning">API</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="抽取方式" align="center" prop="extractMode">
        <template #default="scope">
          <el-tag v-if="scope.row.extractMode === 'FULL'" type="info">全量</el-tag>
          <el-tag v-else-if="scope.row.extractMode === 'INCREMENTAL'" type="warning">增量</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="调度类型" align="center" prop="scheduleType">
        <template #default="scope">
          <el-tag v-if="scope.row.scheduleType === 'CRON'" type="success">定时</el-tag>
          <el-tag v-else-if="scope.row.scheduleType === 'MANUAL'" type="info">手动</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'ACTIVE'" type="success">启用</el-tag>
          <el-tag v-else-if="scope.row.status === 'INACTIVE'" type="danger">停用</el-tag>
          <el-tag v-else-if="scope.row.status === 'PAUSED'" type="warning">暂停</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后运行时间" align="center" prop="lastRunTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.lastRunTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['etl:task:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['etl:task:remove']">删除</el-button>
          <el-button link type="success" icon="VideoPlay" @click="handleTrigger(scope.row)" v-hasPermi="['etl:task:trigger']">执行</el-button>
          <el-button v-if="scope.row.status === 'ACTIVE'" link type="warning" icon="VideoPause" @click="handlePause(scope.row)" v-hasPermi="['etl:task:edit']">暂停</el-button>
          <el-button v-if="scope.row.status === 'PAUSED'" link type="success" icon="VideoPlay" @click="handleResume(scope.row)" v-hasPermi="['etl:task:edit']">恢复</el-button>
          <el-button link type="info" icon="View" @click="handleViewExecution(scope.row)" v-hasPermi="['etl:task:list']">执行记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 零状态提示 -->
    <el-empty v-else-if="!loading" description="暂无ETL任务">
      <el-button type="primary" @click="handleAdd">创建ETL任务</el-button>
    </el-empty>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>
      
      <el-tab-pane label="监控看板" name="monitor">
        <EtlMonitorDashboard ref="monitorDashboardRef" @task-selected="handleTaskSelected" />
      </el-tab-pane>
      
      <el-tab-pane label="执行记录" name="execution">
        <ExecutionList />
      </el-tab-pane>
    </el-tabs>

    <!-- 添加或修改ETL任务对话框 -->
    <el-dialog :title="title" v-model="open" width="900px" append-to-body @close="cancel">
      <EtlTaskWizard
        ref="etlTaskWizardRef"
        :form-data="form"
        @update:form-data="form = $event"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 执行记录对话框 -->
    <el-dialog title="执行记录" v-model="executionDialogVisible" width="1000px" append-to-body @close="closeExecutionDialog">
      <el-table v-loading="executionLoading" :data="executionList" class="aether-table">
        <el-table-column label="执行ID" align="center" prop="id" />
        <el-table-column label="状态" align="center" prop="status">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 'RUNNING'" type="warning">运行中</el-tag>
            <el-tag v-else-if="scope.row.status === 'SUCCESS'" type="success">成功</el-tag>
            <el-tag v-else-if="scope.row.status === 'FAILED'" type="danger">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" align="center" prop="startTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.startTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="结束时间" align="center" prop="endTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时(ms)" align="center" prop="duration" />
        <el-table-column label="数据量" align="center" prop="dataCount" />
        <el-table-column label="错误信息" align="center" prop="errorMessage" :show-overflow-tooltip="true" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="EtlTask">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { listEtlTask, getEtlTask, delEtlTask, addEtlTask, updateEtlTask, triggerEtlTask, pauseEtlTask, resumeEtlTask, listEtlTaskExecution } from "@/api/etl"
import { listDataSource } from "@/api/datasource"
import { parseTime } from '@/utils/ruoyi'
import EtlTaskWizard from './components/EtlTaskWizard.vue'
import EtlMonitorDashboard from './components/EtlMonitorDashboard.vue'
import ExecutionList from './components/ExecutionList.vue'

const etlTaskWizardRef = ref(null)
const monitorDashboardRef = ref(null)

const { proxy } = getCurrentInstance()

const activeTab = ref('list')

const etlTaskList = ref([])
const datasourceList = ref([])
const executionList = ref([])
const open = ref(false)
const executionDialogVisible = ref(false)
const loading = ref(true)
const executionLoading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const title = ref("")

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined,
    datasourceId: undefined,
    status: undefined
  }
})

const { queryParams, form } = toRefs(data)

/** 查询数据源列表 */
function getDataSourceList() {
  listDataSource({ pageNum: 1, pageSize: 9999 }).then(response => {
    datasourceList.value = response.rows || []
  }).catch(() => {
    datasourceList.value = []
  })
}

/** 查询ETL任务列表 */
function getList() {
  loading.value = true
  listEtlTask(queryParams.value).then(response => {
    etlTaskList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  proxy.resetForm("queryRef")
  handleQuery()
}

/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
  single.value = selection.length != 1
  multiple.value = !selection.length
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  open.value = true
  title.value = "添加ETL任务"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  getEtlTask(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改ETL任务"
  })
}

/** 提交按钮 */
function submitForm() {
  if (etlTaskWizardRef.value) {
    etlTaskWizardRef.value.validate(valid => {
      if (valid) {
        if (form.value.id != undefined) {
          updateEtlTask(form.value).then(() => {
            proxy.$modal.msgSuccess("修改成功")
            open.value = false
            getList()
          }).catch(() => {
            // 错误已在API层处理
          })
        } else {
          addEtlTask(form.value).then(() => {
            proxy.$modal.msgSuccess("新增成功")
            open.value = false
            getList()
          }).catch(() => {
            // 错误已在API层处理
          })
        }
      }
    })
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  const taskIds = row?.id ? [row.id] : ids.value
  proxy.$modal.confirm('是否确认删除ETL任务编号为"' + taskIds + '"的数据项？').then(function() {
    return delEtlTask(taskIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 触发执行按钮操作 */
function handleTrigger(row) {
  const taskId = row?.id || ids.value[0]
  proxy.$modal.confirm('是否确认立即执行ETL任务？').then(function() {
    return triggerEtlTask(taskId)
  }).then(() => {
    proxy.$modal.msgSuccess("任务已触发执行")
    getList()
  }).catch(() => {})
}

/** 暂停按钮操作 */
function handlePause(row) {
  const taskId = row?.id || ids.value[0]
  proxy.$modal.confirm('是否确认暂停ETL任务？').then(function() {
    return pauseEtlTask(taskId)
  }).then(() => {
    proxy.$modal.msgSuccess("任务已暂停")
    getList()
  }).catch(() => {})
}

/** 恢复按钮操作 */
function handleResume(row) {
  const taskId = row?.id || ids.value[0]
  proxy.$modal.confirm('是否确认恢复ETL任务？').then(function() {
    return resumeEtlTask(taskId)
  }).then(() => {
    proxy.$modal.msgSuccess("任务已恢复")
    getList()
  }).catch(() => {})
}

/** 查看执行记录 */
function handleViewExecution(row) {
  const taskId = row.id
  executionDialogVisible.value = true
  executionLoading.value = true
  listEtlTaskExecution(taskId).then(response => {
    executionList.value = response.rows
    executionLoading.value = false
  }).catch(() => {
    executionLoading.value = false
  })
}

/** 关闭执行记录对话框 */
function closeExecutionDialog() {
  executionDialogVisible.value = false
  executionList.value = []
}

/** 标签页切换 */
function handleTabChange(tabName) {
  if (tabName === 'monitor' && monitorDashboardRef.value) {
    monitorDashboardRef.value.refreshOverview()
  }
}

/** 任务选择处理 */
function handleTaskSelected(taskId) {
  // 可以在这里处理任务选择逻辑
  console.log('任务已选择:', taskId)
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 表单重置 */
function reset() {
  form.value = {
    id: undefined,
    name: undefined,
    datasourceId: undefined,
    sourceType: undefined,
    sourceConfig: undefined,
    targetTable: undefined,
    extractMode: undefined,
    incrementField: undefined,
    incrementType: undefined,
    scheduleType: undefined,
    cronExpression: undefined,
    retryCount: 3,
    retryInterval: "[1,5,15]",
    status: "ACTIVE",
    remark: undefined
  }
  if (etlTaskWizardRef.value) {
    proxy.resetForm("etlTaskWizardRef")
  }
}

onMounted(() => {
  getList()
  getDataSourceList()
})
</script>

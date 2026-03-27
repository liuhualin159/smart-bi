<template>
  <el-form ref="etlTaskFormRef" :model="localFormData" :rules="rules" label-width="120px">
    <!-- 步骤1: 基本信息 -->
    <el-form-item label="任务名称" prop="name">
      <el-input v-model="localFormData.name" placeholder="请输入任务名称" />
    </el-form-item>
    
    <!-- 源配置（被抽取方） -->
    <el-divider content-position="left">源配置（被抽取方）</el-divider>
    <el-form-item label="源数据源" prop="datasourceId">
      <el-select v-model="localFormData.datasourceId" placeholder="请选择源数据源（被抽取的数据源）" style="width: 100%" @change="handleSourceDataSourceChange">
        <el-option
          v-for="ds in datasourceList"
          :key="ds.id"
          :label="ds.name"
          :value="ds.id"
        />
      </el-select>
      <div style="margin-top: 5px; color: #909399; font-size: 12px;">
        选择要抽取数据的外部数据源
      </div>
    </el-form-item>

    <!-- 目标配置（存放抽取数据的地方） -->
    <el-divider content-position="left">目标配置（存放抽取数据的地方）</el-divider>
    <el-form-item label="目标表名" prop="targetTable">
      <el-autocomplete
        v-model="localFormData.targetTable"
        :fetch-suggestions="queryTargetTable"
        placeholder="输入新表名（自动建表）或选择已有表"
        style="width: 100%"
        :loading="targetTableListLoading"
        @select="handleTargetTableSelect"
        clearable
      >
        <template #default="{ item }">
          <div>
            <span>{{ item.tableName }}</span>
            <span v-if="item.tableComment" style="color: #909399; margin-left: 10px;">{{ item.tableComment }}</span>
            <el-tag v-if="item.isNew" type="success" size="small" style="margin-left: 10px;">新建</el-tag>
          </div>
        </template>
      </el-autocomplete>
      <div style="margin-top: 5px; color: #909399; font-size: 12px;">
        数据将抽取到本地数据库（smart_bi），输入新表名将自动创建表结构
      </div>
    </el-form-item>
    
    <el-form-item label="备注" prop="remark">
      <el-input v-model="localFormData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
    </el-form-item>

    <!-- 源数据配置 -->
    <el-divider content-position="left">源数据配置</el-divider>
    <el-form-item label="源类型" prop="sourceType">
      <el-radio-group v-model="localFormData.sourceType" @change="handleSourceTypeChange">
        <el-radio label="TABLE">表</el-radio>
        <el-radio label="SQL">SQL查询</el-radio>
        <el-radio label="API">API接口</el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- 表类型配置 -->
    <template v-if="localFormData.sourceType === 'TABLE'">
      <el-form-item label="源表名" prop="sourceConfig">
        <el-select 
          v-model="tableConfig.tableName" 
          placeholder="请选择源表名（从源数据源中选择）" 
          style="width: 100%"
          filterable
          :loading="sourceTableListLoading"
          :disabled="!localFormData.datasourceId"
          @visible-change="handleSourceTableSelectVisible"
        >
          <el-option
            v-for="table in sourceTableList"
            :key="table.tableName"
            :label="table.tableName + (table.tableComment ? ' - ' + table.tableComment : '')"
            :value="table.tableName"
          />
        </el-select>
        <div style="margin-top: 5px; color: #909399; font-size: 12px;">
          从源数据源中选择要抽取的表
        </div>
      </el-form-item>
    </template>

    <!-- SQL类型配置 -->
    <template v-if="localFormData.sourceType === 'SQL'">
      <el-form-item label="SQL查询" prop="sourceConfig">
        <el-input v-model="sqlConfig.sql" type="textarea" :rows="5" placeholder="请输入SQL查询语句" />
      </el-form-item>
    </template>

    <!-- API类型配置 -->
    <template v-if="localFormData.sourceType === 'API'">
      <el-form-item label="API URL" prop="sourceConfig">
        <el-input v-model="apiConfig.url" placeholder="请输入API URL" />
      </el-form-item>
      <el-form-item label="请求方法">
        <el-select v-model="apiConfig.method" placeholder="请选择请求方法" style="width: 100%">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
        </el-select>
      </el-form-item>
    </template>

    <!-- 步骤3: 抽取配置 -->
    <el-divider content-position="left">抽取配置</el-divider>
    <el-form-item label="抽取方式" prop="extractMode">
      <el-radio-group v-model="localFormData.extractMode" @change="handleExtractModeChange">
        <el-radio label="FULL">全量</el-radio>
        <el-radio label="INCREMENTAL">增量</el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- 增量抽取配置 -->
    <template v-if="localFormData.extractMode === 'INCREMENTAL'">
      <el-form-item label="增量字段" prop="incrementField">
        <el-input v-model="localFormData.incrementField" placeholder="请输入增量字段名" />
      </el-form-item>
      <el-form-item label="增量类型" prop="incrementType">
        <el-select v-model="localFormData.incrementType" placeholder="请选择增量类型" style="width: 100%">
          <el-option label="时间戳" value="TIMESTAMP" />
          <el-option label="自增ID" value="AUTO_INCREMENT" />
          <el-option label="CDC" value="CDC" />
        </el-select>
      </el-form-item>
    </template>

    <!-- 步骤4: 调度配置 -->
    <el-divider content-position="left">调度配置</el-divider>
    <el-form-item label="调度类型" prop="scheduleType">
      <el-radio-group v-model="localFormData.scheduleType" @change="handleScheduleTypeChange">
        <el-radio label="CRON">定时</el-radio>
        <el-radio label="MANUAL">手动</el-radio>
      </el-radio-group>
    </el-form-item>

    <!-- Cron调度配置 -->
    <template v-if="localFormData.scheduleType === 'CRON'">
      <el-form-item label="Cron表达式" prop="cronExpression">
        <el-input v-model="localFormData.cronExpression" placeholder="请输入Cron表达式，如：0 0 2 * * ?" />
        <div style="margin-top: 5px; color: #909399; font-size: 12px;">
          示例：0 0 2 * * ? (每天凌晨2点执行)
        </div>
      </el-form-item>
    </template>

    <!-- 步骤5: 高级配置 -->
    <el-divider content-position="left">高级配置</el-divider>
    <el-form-item label="重试次数" prop="retryCount">
      <el-input-number v-model="localFormData.retryCount" :min="0" :max="10" style="width: 100%" />
    </el-form-item>
    <el-form-item label="重试间隔(分钟)" prop="retryInterval">
      <el-input v-model="localFormData.retryInterval" placeholder="JSON数组，如：[1,5,15]" />
      <div style="margin-top: 5px; color: #909399; font-size: 12px;">
        格式：JSON数组，表示第1次、第2次、第3次重试的间隔时间（分钟）
      </div>
    </el-form-item>
    <el-form-item label="状态" prop="status">
      <el-radio-group v-model="localFormData.status">
        <el-radio label="ACTIVE">启用</el-radio>
        <el-radio label="INACTIVE">停用</el-radio>
      </el-radio-group>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { listDataSource, getTableList, getLocalTableList, createTargetTable } from "@/api/datasource"
// Note: JSON parsing will be handled by backend

const props = defineProps({
  formData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:formData'])

const { proxy } = getCurrentInstance()
const etlTaskFormRef = ref(null)

// 数据源列表
const datasourceList = ref([])

// 表列表
const targetTableList = ref([])
const sourceTableList = ref([])
const targetTableListLoading = ref(false)
const sourceTableListLoading = ref(false)

// 创建本地表单数据副本
const localFormData = ref({ ...props.formData })

// 监听props变化
watch(() => props.formData, (newVal) => {
  if (newVal) {
    localFormData.value = { ...newVal }
    parseSourceConfig()
  }
}, { deep: true, immediate: true })

// 监听本地数据变化，同步到父组件
watch(localFormData, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    updateSourceConfig()
    emit('update:formData', { ...newVal })
  }
}, { deep: true })

// 源配置对象
const tableConfig = ref({ tableName: '' })
const sqlConfig = ref({ sql: '' })
const apiConfig = ref({ url: '', method: 'GET' })

// 解析源配置
function parseSourceConfig() {
  if (!localFormData.value.sourceConfig) {
    return
  }
  
  try {
    const config = typeof localFormData.value.sourceConfig === 'string' 
      ? JSON.parse(localFormData.value.sourceConfig)
      : localFormData.value.sourceConfig
    if (localFormData.value.sourceType === 'TABLE') {
      tableConfig.value = config
    } else if (localFormData.value.sourceType === 'SQL') {
      sqlConfig.value = config
    } else if (localFormData.value.sourceType === 'API') {
      apiConfig.value = config
    }
  } catch (e) {
    console.warn('解析源配置失败', e)
  }
}

// 更新源配置
function updateSourceConfig() {
  let config = {}
  if (localFormData.value.sourceType === 'TABLE') {
    config = tableConfig.value
  } else if (localFormData.value.sourceType === 'SQL') {
    config = sqlConfig.value
  } else if (localFormData.value.sourceType === 'API') {
    config = apiConfig.value
  }
  localFormData.value.sourceConfig = typeof config === 'string' ? config : JSON.stringify(config)
}

// 源类型改变
function handleSourceTypeChange() {
  localFormData.value.sourceConfig = ''
  if (localFormData.value.sourceType === 'TABLE') {
    tableConfig.value = { tableName: '' }
  } else if (localFormData.value.sourceType === 'SQL') {
    sqlConfig.value = { sql: '' }
  } else if (localFormData.value.sourceType === 'API') {
    apiConfig.value = { url: '', method: 'GET' }
  }
}

// 抽取方式改变
function handleExtractModeChange() {
  if (localFormData.value.extractMode === 'FULL') {
    localFormData.value.incrementField = undefined
    localFormData.value.incrementType = undefined
  }
}

// 调度类型改变
function handleScheduleTypeChange() {
  if (localFormData.value.scheduleType === 'MANUAL') {
    localFormData.value.cronExpression = undefined
  }
}

// 验证规则
const rules = computed(() => {
  const rules = {
    name: [{ required: true, message: "任务名称不能为空", trigger: "blur" }],
    datasourceId: [{ required: true, message: "数据源不能为空", trigger: "change" }],
    targetTable: [{ required: true, message: "目标表名不能为空", trigger: "blur" }],
    sourceType: [{ required: true, message: "源类型不能为空", trigger: "change" }],
    extractMode: [{ required: true, message: "抽取方式不能为空", trigger: "change" }],
    scheduleType: [{ required: true, message: "调度类型不能为空", trigger: "change" }]
  }
  
  if (localFormData.value.scheduleType === 'CRON') {
    rules.cronExpression = [{ required: true, message: "Cron表达式不能为空", trigger: "blur" }]
  }
  
  if (localFormData.value.extractMode === 'INCREMENTAL') {
    rules.incrementField = [{ required: true, message: "增量字段不能为空", trigger: "blur" }]
    rules.incrementType = [{ required: true, message: "增量类型不能为空", trigger: "change" }]
  }
  
  return rules
})

// 加载数据源列表
function getDataSourceList() {
  listDataSource({ pageNum: 1, pageSize: 1000, status: 'ACTIVE' }).then(response => {
    datasourceList.value = response.rows || []
  }).catch(() => {
    datasourceList.value = []
  })
}

// 加载本地数据库表列表（目标表）
function loadLocalTableList() {
  if (targetTableList.value.length > 0) {
    return // 已加载过
  }
  
  targetTableListLoading.value = true
  getLocalTableList().then(response => {
    targetTableList.value = (response.data || []).map(table => ({
      ...table,
      isNew: false
    }))
  }).catch(() => {
    targetTableList.value = []
  }).finally(() => {
    targetTableListLoading.value = false
  })
}

// 加载源数据源表列表
function loadSourceTableList(dataSourceId) {
  if (!dataSourceId) {
    sourceTableList.value = []
    return
  }

  sourceTableListLoading.value = true
  getTableList(dataSourceId).then(response => {
    sourceTableList.value = response.data || []
  }).catch(() => {
    sourceTableList.value = []
  }).finally(() => {
    sourceTableListLoading.value = false
  })
}

// 目标表自动完成查询
function queryTargetTable(queryString, cb) {
  const results = []
  
  // 如果输入了表名，检查是否是新表
  if (queryString && queryString.trim()) {
    const inputTableName = queryString.trim()
    // 检查是否已存在
    const exists = targetTableList.value.some(t => t.tableName === inputTableName)
    if (!exists) {
      results.push({
        tableName: inputTableName,
        tableComment: '',
        isNew: true
      })
    }
  }
  
  // 添加已有表（匹配输入）
  const filtered = targetTableList.value.filter(table => 
    !queryString || table.tableName.toLowerCase().includes(queryString.toLowerCase())
  )
  results.push(...filtered)
  
  cb(results)
}

// 目标表选择事件
function handleTargetTableSelect(item) {
  // 如果是新表，需要自动建表
  if (item.isNew) {
    // 检查是否有源表信息
    if (localFormData.value.sourceType === 'TABLE' && tableConfig.value.tableName && localFormData.value.datasourceId) {
      proxy.$modal.confirm('将根据源表"' + tableConfig.value.tableName + '"的结构自动创建目标表"' + item.tableName + '"，是否继续？').then(() => {
        createTargetTable({
          sourceDataSourceId: localFormData.value.datasourceId,
          sourceTableName: tableConfig.value.tableName,
          targetTableName: item.tableName
        }).then(() => {
          proxy.$modal.msgSuccess('目标表创建成功')
          // 重新加载本地表列表
          targetTableList.value = []
          loadLocalTableList()
        }).catch(error => {
          proxy.$modal.msgError('创建目标表失败: ' + (error.msg || error.message || '未知错误'))
        })
      }).catch(() => {
        // 取消选择
        localFormData.value.targetTable = ''
      })
    } else {
      proxy.$modal.msgWarning('请先配置源数据源和源表，以便自动创建目标表结构')
      localFormData.value.targetTable = ''
    }
  }
}

// 源数据源变化事件
function handleSourceDataSourceChange() {
  // 清空源表列表
  sourceTableList.value = []
  tableConfig.value.tableName = ''
}

// 源表下拉框显示/隐藏事件
function handleSourceTableSelectVisible(visible) {
  if (visible && localFormData.value.datasourceId && sourceTableList.value.length === 0) {
    loadSourceTableList(localFormData.value.datasourceId)
  }
}

// 监听数据源变化
watch(() => localFormData.value.datasourceId, (newVal, oldVal) => {
  if (newVal && newVal !== oldVal) {
    sourceTableList.value = []
    tableConfig.value.tableName = ''
  }
})

// 组件挂载时加载数据源列表和本地表列表
onMounted(() => {
  getDataSourceList()
  loadLocalTableList()
})

// 暴露方法给父组件
defineExpose({
  validate: (callback) => {
    etlTaskFormRef.value.validate(callback)
  },
  resetFields: () => {
    if (etlTaskFormRef.value) {
      etlTaskFormRef.value.resetFields()
    }
  }
})
</script>

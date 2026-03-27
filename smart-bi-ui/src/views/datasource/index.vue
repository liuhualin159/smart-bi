<template>
  <div class="app-container aether-list-page">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px" class="aether-filter-card">
      <el-form-item label="数据源名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入数据源名称" clearable style="width: 240px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="数据源类型" prop="type">
        <el-select v-model="queryParams.type" placeholder="数据源类型" clearable style="width: 240px">
          <el-option label="数据库" value="DATABASE" />
          <el-option label="API接口" value="API" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="状态" clearable style="width: 240px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['datasource:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['datasource:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['datasource:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="Connection" :disabled="single" @click="handleTestConnection" v-hasPermi="['datasource:test']">测试连接</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据源列表 -->
    <el-table v-loading="loading" :data="dataSourceList" @selection-change="handleSelectionChange" v-if="dataSourceList.length > 0" class="aether-table">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="数据源ID" align="center" prop="id" />
      <el-table-column label="数据源名称" align="center" prop="name" :show-overflow-tooltip="true" />
      <el-table-column label="数据源类型" align="center" prop="type">
        <template #default="scope">
          <el-tag v-if="scope.row.type === 'DATABASE'" type="primary">数据库</el-tag>
          <el-tag v-else-if="scope.row.type === 'API'" type="success">API接口</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="子类型" align="center" prop="subType" />
      <el-table-column label="主机地址" align="center" prop="host" :show-overflow-tooltip="true" />
      <el-table-column label="端口" align="center" prop="port" />
      <el-table-column label="状态" align="center" prop="status">
        <template #default="scope">
          <el-tag v-if="scope.row.status === 'ACTIVE'" type="success">启用</el-tag>
          <el-tag v-else type="danger">停用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['datasource:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['datasource:remove']">删除</el-button>
          <el-button link type="info" icon="Connection" @click="handleTestConnection(scope.row)" v-hasPermi="['datasource:test']">测试</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 零状态提示 -->
    <EmptyState v-else-if="!loading" @add="handleAdd" />

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改数据源对话框 -->
    <el-dialog :title="title" v-model="open" width="800px" append-to-body @close="cancel">
      <DataSourceForm
        ref="dataSourceFormRef"
        :form-data="form"
        @update:form-data="form = $event"
      />
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
          <el-button type="info" @click="handleTestConnectionFromDialog" v-if="form.type">测试连接</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DataSource">
import { ref, reactive, toRefs, getCurrentInstance } from 'vue'
import { listDataSource, getDataSource, delDataSource, addDataSource, updateDataSource, testDataSourceConnection } from "@/api/datasource"
import { parseTime } from '@/utils/ruoyi'
import DataSourceForm from './components/DataSourceForm.vue'
import EmptyState from './components/EmptyState.vue'

const dataSourceFormRef = ref(null)

const { proxy } = getCurrentInstance()

const dataSourceList = ref([])
const open = ref(false)
const loading = ref(true)
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
    type: undefined,
    status: undefined
  },
  rules: {
    name: [{ required: true, message: "数据源名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "数据源类型不能为空", trigger: "change" }],
    subType: [{ required: true, message: "子类型不能为空", trigger: "change" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询数据源列表 */
function getList() {
  loading.value = true
  listDataSource(queryParams.value).then(response => {
    dataSourceList.value = response.rows
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
  title.value = "添加数据源"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  getDataSource(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改数据源"
  })
}

/** 提交按钮 */
function submitForm() {
  if (dataSourceFormRef.value) {
    dataSourceFormRef.value.validate(valid => {
      if (valid) {
        if (form.value.id != undefined) {
          updateDataSource(form.value).then(() => {
            proxy.$modal.msgSuccess("修改成功")
            open.value = false
            getList()
          })
        } else {
          addDataSource(form.value).then(() => {
            proxy.$modal.msgSuccess("新增成功")
            open.value = false
            getList()
          })
        }
      }
    })
  }
}

/** 删除按钮操作 */
function handleDelete(row) {
  const dataSourceIds = row?.id ? [row.id] : ids.value
  proxy.$modal.confirm('是否确认删除数据源编号为"' + dataSourceIds + '"的数据项？').then(function() {
    return delDataSource(dataSourceIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 测试连接按钮操作（从对话框调用） */
function handleTestConnectionFromDialog() {
  // 从表单组件获取最新数据
  let testData = {}
  if (dataSourceFormRef.value) {
    // 尝试从表单组件获取数据
    const formComponent = dataSourceFormRef.value
    // 如果表单组件有获取数据的方法，使用它；否则使用 form.value
    if (formComponent.getFormData) {
      testData = formComponent.getFormData()
    } else {
      testData = { ...form.value }
    }
  } else {
    testData = { ...form.value }
  }
  
  // 验证数据
  if (!testData || Object.keys(testData).length === 0) {
    proxy.$modal.msgWarning("请先填写数据源信息")
    return
  }
  
  if (!testData.type) {
    proxy.$modal.msgWarning("请先填写数据源基本信息")
    return
  }
  
  // 验证必填字段
  if (testData.type === 'DATABASE') {
    if (!testData.host || !testData.port || !testData.databaseName) {
      proxy.$modal.msgWarning("请填写完整的数据库连接信息（主机、端口、数据库名）")
      return
    }
    if (!testData.username) {
      proxy.$modal.msgWarning("请填写数据库用户名")
      return
    }
    if (!testData.password) {
      proxy.$modal.msgWarning("请填写数据库密码")
      return
    }
  }
  
  if (testData.type === 'API' && !testData.url) {
    proxy.$modal.msgWarning("请填写API连接URL")
    return
  }
  
  // 确保传递的数据不为空
  if (!testData || Object.keys(testData).length === 0) {
    proxy.$modal.msgError("数据源信息为空，无法测试连接")
    return
  }
  
  proxy.$modal.loading("正在测试连接...")
  testDataSourceConnection(testData).then(response => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgSuccess("连接测试成功")
  }).catch(error => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError("连接测试失败: " + (error.msg || error.message || "未知错误"))
  })
}

/** 测试连接按钮操作（从列表调用） */
function handleTestConnection(row) {
  // 如果传递了行数据，使用行数据；否则从选中项中获取
  const dataSource = row || dataSourceList.value.find(item => item.id === ids.value[0])
  if (!dataSource) {
    proxy.$modal.msgWarning("请选择要测试的数据源")
    return
  }
  
  if (!dataSource.id) {
    proxy.$modal.msgWarning("数据源ID不存在，无法测试")
    return
  }
  
  // 通过ID测试（后端会从数据库获取完整信息，包括密码）
  proxy.$modal.loading("正在测试连接...")
  testDataSourceConnection({ id: dataSource.id }).then(response => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgSuccess("连接测试成功")
  }).catch(error => {
    proxy.$modal.closeLoading()
    proxy.$modal.msgError("连接测试失败: " + (error.msg || error.message || "未知错误"))
  })
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
    type: undefined,
    subType: undefined,
    host: undefined,
    port: undefined,
    databaseName: undefined,
    url: undefined,
    username: undefined,
    password: undefined,
    authType: undefined,
    authConfig: undefined,
    status: "ACTIVE",
    remark: undefined
  }
  proxy.resetForm("dataSourceFormRef")
}

getList()
</script>

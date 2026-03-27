<template>
  <div class="app-container aether-list-page">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="100px" class="aether-filter-card">
      <el-form-item label="权限类型" prop="permissionType">
        <el-select v-model="queryParams.permissionType" placeholder="请选择权限类型" clearable style="width: 200px">
          <el-option label="表级权限" value="TABLE" />
          <el-option label="字段级权限" value="FIELD" />
          <el-option label="行级权限" value="ROW" />
        </el-select>
      </el-form-item>
      <el-form-item label="表名" prop="tableName">
        <el-input v-model="queryParams.tableName" placeholder="请输入表名" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作" prop="operation">
        <el-select v-model="queryParams.operation" placeholder="请选择操作" clearable style="width: 200px">
          <el-option label="允许" value="ALLOW" />
          <el-option label="禁止" value="DENY" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['system:dataPermission:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['system:dataPermission:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:dataPermission:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="dataPermissionList" @selection-change="handleSelectionChange" class="aether-table">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="权限ID" align="center" prop="id" width="100" />
      <el-table-column label="用户ID" align="center" prop="userId" width="100">
        <template #default="scope">
          <span v-if="scope.row.userId">{{ scope.row.userId }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="角色ID" align="center" prop="roleId" width="100">
        <template #default="scope">
          <span v-if="scope.row.roleId">{{ scope.row.roleId }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="权限类型" align="center" prop="permissionType" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.permissionType === 'TABLE'" type="primary">表级</el-tag>
          <el-tag v-else-if="scope.row.permissionType === 'FIELD'" type="success">字段级</el-tag>
          <el-tag v-else-if="scope.row.permissionType === 'ROW'" type="warning">行级</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="表名" align="center" prop="tableName" :show-overflow-tooltip="true" />
      <el-table-column label="字段名" align="center" prop="fieldName" :show-overflow-tooltip="true">
        <template #default="scope">
          <span v-if="scope.row.fieldName">{{ scope.row.fieldName }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" prop="operation" width="100">
        <template #default="scope">
          <el-tag v-if="scope.row.operation === 'ALLOW'" type="success">允许</el-tag>
          <el-tag v-else-if="scope.row.operation === 'DENY'" type="danger">禁止</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="行级过滤" align="center" prop="rowFilter" :show-overflow-tooltip="true" min-width="200">
        <template #default="scope">
          <span v-if="scope.row.rowFilter">{{ scope.row.rowFilter }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:dataPermission:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['system:dataPermission:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改数据权限对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body @close="cancel">
      <el-form ref="dataPermissionRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="用户ID" prop="userId">
          <el-input-number v-model="form.userId" :min="1" placeholder="留空表示角色权限" style="width: 100%" />
          <div class="form-tip">留空表示角色权限，与角色ID二选一</div>
        </el-form-item>
        <el-form-item label="角色ID" prop="roleId">
          <el-input-number v-model="form.roleId" :min="1" placeholder="留空表示用户权限" style="width: 100%" />
          <div class="form-tip">留空表示用户权限，与用户ID二选一</div>
        </el-form-item>
        <el-form-item label="权限类型" prop="permissionType">
          <el-select v-model="form.permissionType" placeholder="请选择权限类型" style="width: 100%">
            <el-option label="表级权限" value="TABLE" />
            <el-option label="字段级权限" value="FIELD" />
            <el-option label="行级权限" value="ROW" />
          </el-select>
        </el-form-item>
        <el-form-item label="表名" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名" />
        </el-form-item>
        <el-form-item label="字段名" prop="fieldName" v-if="form.permissionType === 'FIELD'">
          <el-input v-model="form.fieldName" placeholder="请输入字段名（字段级权限必填）" />
        </el-form-item>
        <el-form-item label="操作" prop="operation">
          <el-select v-model="form.operation" placeholder="请选择操作" style="width: 100%">
            <el-option label="允许" value="ALLOW" />
            <el-option label="禁止" value="DENY" />
          </el-select>
        </el-form-item>
        <el-form-item label="行级过滤" prop="rowFilter" v-if="form.permissionType === 'ROW'">
          <el-input v-model="form.rowFilter" type="textarea" :rows="3" placeholder="请输入SQL WHERE子句（行级权限必填），例如：dept_id = 1" />
          <div class="form-tip">SQL WHERE子句，例如：dept_id = 1 或 user_id = #{userId}</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DataPermission">
import { ref, reactive, toRefs, getCurrentInstance, onMounted } from 'vue'
import { listDataPermission, getDataPermission, delDataPermission, addDataPermission, updateDataPermission } from "@/api/system/dataPermission"

const { proxy } = getCurrentInstance()

const dataPermissionList = ref([])
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
    permissionType: undefined,
    tableName: undefined,
    operation: undefined
  },
  rules: {
    permissionType: [
      { required: true, message: "权限类型不能为空", trigger: "change" }
    ],
    tableName: [
      { required: true, message: "表名不能为空", trigger: "blur" }
    ],
    operation: [
      { required: true, message: "操作不能为空", trigger: "change" }
    ],
    fieldName: [
      { 
        validator: (rule, value, callback) => {
          if (data.form.permissionType === 'FIELD' && !value) {
            callback(new Error('字段级权限时字段名不能为空'))
          } else {
            callback()
          }
        }, 
        trigger: "blur" 
      }
    ],
    rowFilter: [
      { 
        validator: (rule, value, callback) => {
          if (data.form.permissionType === 'ROW' && !value) {
            callback(new Error('行级权限时行级过滤条件不能为空'))
          } else {
            callback()
          }
        }, 
        trigger: "blur" 
      }
    ]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询数据权限列表 */
function getList() {
  loading.value = true
  listDataPermission(queryParams.value).then(response => {
    dataPermissionList.value = response.rows
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
  title.value = "添加数据权限"
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const id = row?.id || ids.value[0]
  getDataPermission(id).then(response => {
    form.value = response.data
    open.value = true
    title.value = "修改数据权限"
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["dataPermissionRef"].validate(valid => {
    if (valid) {
      if (form.value.id != undefined) {
        updateDataPermission(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
        })
      } else {
        addDataPermission(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

/** 删除按钮操作 */
function handleDelete(row) {
  const permissionIds = row?.id ? [row.id] : ids.value
  proxy.$modal.confirm('是否确认删除数据权限编号为"' + permissionIds + '"的数据项？').then(function() {
    return delDataPermission(permissionIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
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
    userId: undefined,
    roleId: undefined,
    permissionType: undefined,
    tableName: undefined,
    fieldName: undefined,
    operation: undefined,
    rowFilter: undefined,
    remark: undefined
  }
  proxy.resetForm("dataPermissionRef")
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>

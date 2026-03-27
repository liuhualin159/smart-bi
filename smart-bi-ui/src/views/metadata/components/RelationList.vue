<template>
  <div>
    <el-form ref="queryRef" :inline="true" v-show="showSearch" class="mb2 aether-filter-card">
      <el-form-item label="左表">
        <el-input v-model="queryParams.leftTable" placeholder="左表名" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="右表">
        <el-input v-model="queryParams.rightTable" placeholder="右表名" clearable style="width:140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['metadata:relation:add']">新增关系</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="list" border class="aether-table">
      <el-table-column label="左表" prop="leftTable" min-width="120" show-overflow-tooltip />
      <el-table-column label="左表字段" prop="leftField" min-width="120" show-overflow-tooltip />
      <el-table-column label="右表" prop="rightTable" min-width="120" show-overflow-tooltip />
      <el-table-column label="右表字段" prop="rightField" min-width="120" show-overflow-tooltip />
      <el-table-column label="关系类型" prop="relationType" width="100" show-overflow-tooltip />
      <el-table-column label="优先级" prop="priority" width="80" align="center" sortable />
      <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['metadata:relation:edit']">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['metadata:relation:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" @close="cancel">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="左表名" prop="leftTable">
          <el-input v-model="form.leftTable" placeholder="物理表名，如 order_info" />
        </el-form-item>
        <el-form-item label="左表字段" prop="leftField">
          <el-input v-model="form.leftField" placeholder="关联字段，如 user_id" />
        </el-form-item>
        <el-form-item label="右表名" prop="rightTable">
          <el-input v-model="form.rightTable" placeholder="物理表名，如 user_info" />
        </el-form-item>
        <el-form-item label="右表字段" prop="rightField">
          <el-input v-model="form.rightField" placeholder="关联字段，如 id" />
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-select v-model="form.relationType" placeholder="可选" clearable style="width:100%">
            <el-option label="INNER JOIN" value="INNER_JOIN" />
            <el-option label="LEFT JOIN" value="LEFT_JOIN" />
            <el-option label="RIGHT JOIN" value="RIGHT_JOIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="100" />
          <span class="form-tip">数值越大在 NL2SQL 中越优先推荐</span>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { listTableRelation, getTableRelation, addTableRelation, updateTableRelation, delTableRelation } from '@/api/metadata'

const { proxy } = getCurrentInstance()
const formRef = ref(null)

const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const list = ref([])
const total = ref(0)

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  leftTable: undefined,
  rightTable: undefined
})

const form = reactive({
  id: undefined,
  leftTable: '',
  leftField: '',
  rightTable: '',
  rightField: '',
  relationType: undefined,
  priority: 0,
  remark: ''
})

const rules = {
  leftTable: [{ required: true, message: '请输入左表名', trigger: 'blur' }],
  leftField: [{ required: true, message: '请输入左表字段', trigger: 'blur' }],
  rightTable: [{ required: true, message: '请输入右表名', trigger: 'blur' }],
  rightField: [{ required: true, message: '请输入右表字段', trigger: 'blur' }]
}

onMounted(() => {
  getList()
})

function getList() {
  loading.value = true
  listTableRelation(queryParams).then(response => {
    list.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.leftTable = undefined
  queryParams.rightTable = undefined
  queryParams.pageNum = 1
  handleQuery()
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增表关系（推荐 join）'
}

function handleUpdate(row) {
  reset()
  getTableRelation(row.id).then(response => {
    Object.assign(form, response.data)
    open.value = true
    title.value = '编辑表关系'
  })
}

function submitForm() {
  formRef.value.validate(valid => {
    if (valid) {
      if (form.id) {
        updateTableRelation(form.id, form).then(() => {
          proxy.$modal.msgSuccess('修改成功，将影响 NL2SQL 的推荐 join')
          open.value = false
          getList()
        }).catch(() => {})
      } else {
        addTableRelation(form).then(() => {
          proxy.$modal.msgSuccess('新增成功，将影响 NL2SQL 的推荐 join')
          open.value = false
          getList()
        }).catch(() => {})
      }
    }
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除该表关系？').then(() => {
    return delTableRelation([row.id])
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

function cancel() {
  open.value = false
  reset()
}

function reset() {
  form.id = undefined
  form.leftTable = ''
  form.leftField = ''
  form.rightTable = ''
  form.rightField = ''
  form.relationType = undefined
  form.priority = 0
  form.remark = ''
  proxy.resetForm('formRef')
}

defineExpose({
  getList
})
</script>

<style scoped>
.form-tip {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>

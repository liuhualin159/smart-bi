<template>
  <div>
    <el-tree
      ref="treeRef"
      :data="domainList"
      :props="{ children: 'children', label: 'name' }"
      node-key="id"
      default-expand-all
      @node-click="handleNodeClick"
    >
      <template #default="{ node, data }">
        <span class="custom-tree-node">
          <span>{{ node.label }}</span>
          <span>
            <el-button link type="primary" size="small" @click.stop="handleEdit(data)" v-hasPermi="['metadata:domain:edit']">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(data)" v-hasPermi="['metadata:domain:remove']">删除</el-button>
          </span>
        </span>
      </template>
    </el-tree>

    <!-- 业务域表单对话框 -->
    <el-dialog :title="domainFormTitle" v-model="domainFormVisible" width="600px">
      <el-form ref="domainFormRef" :model="domainForm" :rules="domainRules" label-width="100px">
        <el-form-item label="业务域名称" prop="name">
          <el-input v-model="domainForm.name" placeholder="请输入业务域名称" />
        </el-form-item>
        <el-form-item label="业务域编码" prop="code">
          <el-input v-model="domainForm.code" placeholder="请输入业务域编码" />
        </el-form-item>
        <el-form-item label="业务域描述" prop="description">
          <el-input v-model="domainForm.description" type="textarea" :rows="4" placeholder="请输入业务域描述（最多1000字）" />
        </el-form-item>
        <el-form-item label="父业务域" prop="parentId">
          <el-tree-select
            v-model="domainForm.parentId"
            :data="domainList"
            :props="{ children: 'children', label: 'name', value: 'id' }"
            placeholder="请选择父业务域"
            clearable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="domainFormVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitDomainForm">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { listBusinessDomain, addBusinessDomain, updateBusinessDomain, delBusinessDomain } from '@/api/metadata'

const emit = defineEmits(['node-click', 'refresh'])

const { proxy } = getCurrentInstance()
const treeRef = ref(null)
const domainFormRef = ref(null)

const domainList = ref([])
const domainFormVisible = ref(false)
const domainFormTitle = ref('')
const domainForm = ref({
  id: undefined,
  name: '',
  code: '',
  description: '',
  parentId: null
})

const domainRules = {
  name: [{ required: true, message: '业务域名称不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '业务域编码不能为空', trigger: 'blur' }]
}

onMounted(() => {
  getList()
})

function getList() {
  listBusinessDomain({}).then(response => {
    domainList.value = buildTree(response.rows || [])
  })
}

function buildTree(data) {
  const map = {}
  const tree = []
  data.forEach(item => {
    map[item.id] = { ...item, children: [] }
  })
  data.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(map[item.id])
    } else {
      tree.push(map[item.id])
    }
  })
  return tree
}

function handleNodeClick(data) {
  emit('node-click', data.id)
}

function handleAdd() {
  resetDomainForm()
  domainFormTitle.value = '新增业务域'
  domainFormVisible.value = true
}

function handleEdit(data) {
  resetDomainForm()
  domainForm.value = { ...data }
  domainFormTitle.value = '修改业务域'
  domainFormVisible.value = true
}

function handleDelete(data) {
  proxy.$modal.confirm('是否确认删除业务域"' + data.name + '"？').then(() => {
    return delBusinessDomain([data.id])
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
    emit('refresh')
  })
}

function submitDomainForm() {
  domainFormRef.value.validate(valid => {
    if (valid) {
      if (domainForm.value.id) {
        updateBusinessDomain(domainForm.value).then(() => {
          proxy.$modal.msgSuccess('修改成功')
          domainFormVisible.value = false
          getList()
          emit('refresh')
        })
      } else {
        addBusinessDomain(domainForm.value).then(() => {
          proxy.$modal.msgSuccess('新增成功')
          domainFormVisible.value = false
          getList()
          emit('refresh')
        })
      }
    }
  })
}

function resetDomainForm() {
  domainForm.value = {
    id: undefined,
    name: '',
    code: '',
    description: '',
    parentId: null
  }
}

defineExpose({
  handleAdd
})
</script>

<style scoped>
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
</style>

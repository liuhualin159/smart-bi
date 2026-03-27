<template>
  <div class="app-container aether-list-page">
    <el-form :inline="true" v-show="showSearch" class="aether-filter-card">
      <el-form-item>
        <el-input v-model="queryParams.name" placeholder="看板名称" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8 aether-action-bar">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['dashboard:add']">新增</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="dashboardList" class="aether-table">
      <el-table-column label="看板ID" align="center" prop="id" />
      <el-table-column label="看板名称" align="center" prop="name" />
      <el-table-column label="刷新频率" align="center" prop="refreshInterval">
        <template #default="scope">
          <span v-if="scope.row.refreshInterval">{{ scope.row.refreshInterval }}分钟</span>
          <span v-else>不自动刷新</span>
        </template>
      </el-table-column>
      <el-table-column label="是否公开" align="center" prop="isPublic">
        <template #default="scope">
          <el-tag v-if="scope.row.isPublic" type="success">是</el-tag>
          <el-tag v-else type="info">否</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template #default="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['dashboard:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['dashboard:remove']">删除</el-button>
          <el-button link type="success" icon="View" @click="handleView(scope.row)" v-hasPermi="['dashboard:list']">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" class="aether-pagination" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="Dashboard">
import { ref, reactive, toRefs, getCurrentInstance, computed } from 'vue'
import { useRoute } from 'vue-router'
import { listDashboard, getDashboard, delDashboard, addDashboard, updateDashboard } from '@/api/dashboard'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()
const route = useRoute()
// 路由由数据库菜单配置，使用当前列表页路径动态拼接（如 /bi/dashboard -> /bi/dashboard/edit）
const listBasePath = computed(() => (route.path || '').replace(/\/$/, ''))

const dashboardList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    name: undefined
  }
})

const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listDashboard(queryParams.value).then(response => {
    dashboardList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleAdd() {
  proxy.$router.push(listBasePath.value + '/edit')
}

function handleUpdate(row) {
  const route = proxy.$router.resolve({ path: '/dashboard/designer', query: { id: row.id } })
  window.open(route.href, '_blank')
}

function handleView(row) {
  const resolved = proxy.$router.resolve({ path: '/dashboard/designer', query: { id: row.id, mode: 'preview' } })
  window.open(resolved.href, '_blank')
}

function handleDelete(row) {
  proxy.$modal.confirm('是否确认删除看板"' + row.name + '"？').then(() => {
    return delDashboard([row.id])
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  })
}

getList()
</script>

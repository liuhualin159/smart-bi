<template>
  <el-form ref="dataSourceFormRef" :model="localFormData" :rules="dynamicRules" label-width="120px">
      <el-form-item label="数据源名称" prop="name">
        <el-input v-model="localFormData.name" placeholder="请输入数据源名称" />
      </el-form-item>
      <el-form-item label="数据源类型" prop="type">
        <el-radio-group v-model="localFormData.type" @change="handleTypeChange">
          <el-radio label="DATABASE">数据库</el-radio>
          <el-radio label="API">API接口</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <!-- 数据库类型配置 -->
      <template v-if="localFormData.type === 'DATABASE'">
        <el-form-item label="数据库类型" prop="subType">
          <el-select v-model="localFormData.subType" placeholder="请选择数据库类型" style="width: 100%">
            <el-option label="MySQL" value="MySQL" />
            <el-option label="PostgreSQL" value="PostgreSQL" />
            <el-option label="SQL Server" value="SQLServer" />
            <el-option label="Oracle" value="Oracle" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机地址" prop="host">
          <el-input v-model="localFormData.host" placeholder="请输入主机地址" />
        </el-form-item>
        <el-form-item label="端口号" prop="port">
          <el-input-number v-model="localFormData.port" :min="1" :max="65535" style="width: 100%" />
        </el-form-item>
        <el-form-item label="数据库名" prop="databaseName">
          <el-input v-model="localFormData.databaseName" placeholder="请输入数据库名" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="localFormData.username" placeholder="请输入数据库用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="localFormData.password" type="password" :placeholder="localFormData.id ? '不修改请留空（保留原密码）' : '请输入数据库密码'" show-password clearable />
        </el-form-item>
      </template>
      
      <!-- API类型配置 -->
      <template v-if="localFormData.type === 'API'">
        <el-form-item label="API类型" prop="subType">
          <el-select v-model="localFormData.subType" placeholder="请选择API类型" style="width: 100%">
            <el-option label="REST" value="REST" />
          </el-select>
        </el-form-item>
        <el-form-item label="连接URL" prop="url">
          <el-input v-model="localFormData.url" placeholder="请输入API连接URL" />
        </el-form-item>
        <el-form-item label="认证类型" prop="authType">
          <el-select v-model="localFormData.authType" placeholder="请选择认证类型" style="width: 100%">
            <el-option label="用户名密码" value="USERNAME_PASSWORD" />
            <el-option label="API Key" value="API_KEY" />
            <el-option label="Basic Auth" value="BASIC_AUTH" />
            <el-option label="OAuth2" value="OAUTH2" />
          </el-select>
        </el-form-item>
        <el-form-item label="认证配置" prop="authConfig" v-if="localFormData.authType">
          <el-input v-model="localFormData.authConfig" type="textarea" :rows="3" placeholder="请输入认证配置（JSON格式）" />
        </el-form-item>
      </template>
      
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="localFormData.status">
          <el-radio label="ACTIVE">启用</el-radio>
          <el-radio label="INACTIVE">停用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="localFormData.remark" type="textarea" :rows="3" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { getCurrentInstance } from 'vue'
import { testDataSourceConnection } from '@/api/datasource'

const props = defineProps({
  formData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:formData'])

const { proxy } = getCurrentInstance()
const dataSourceFormRef = ref(null)

// 创建本地表单数据副本（使用ref以便双向绑定）
const localFormData = ref({ ...props.formData })

// 监听props变化，同步到本地
watch(() => props.formData, (newVal) => {
  if (newVal) {
    localFormData.value = { ...newVal }
  }
}, { deep: true, immediate: true })

// 监听本地数据变化，同步到父组件
watch(localFormData, (newVal) => {
  if (newVal && Object.keys(newVal).length > 0) {
    emit('update:formData', { ...newVal })
  }
}, { deep: true })

// 动态验证规则
const dynamicRules = computed(() => {
  const rules = {
    name: [{ required: true, message: "数据源名称不能为空", trigger: "blur" }],
    type: [{ required: true, message: "数据源类型不能为空", trigger: "change" }],
    subType: [{ required: true, message: "子类型不能为空", trigger: "change" }]
  }
  
  if (localFormData.value.type === 'DATABASE') {
    rules.host = [{ required: true, message: "主机地址不能为空", trigger: "blur" }]
    rules.port = [{ required: true, message: "端口号不能为空", trigger: "blur" }]
    rules.databaseName = [{ required: true, message: "数据库名不能为空", trigger: "blur" }]
    rules.username = [{ required: true, message: "数据库用户名不能为空", trigger: "blur" }]
    // 新增时密码必填，修改时可不填（保留原密码）
    if (!localFormData.value.id) {
      rules.password = [{ required: true, message: "数据库密码不能为空", trigger: "blur" }]
    }
  } else if (localFormData.value.type === 'API') {
    rules.url = [{ required: true, message: "连接URL不能为空", trigger: "blur" }]
  }
  
  return rules
})

/** 数据源类型改变 */
function handleTypeChange() {
  // 清空相关字段
  if (localFormData.value.type === 'DATABASE') {
    localFormData.value.url = undefined
    localFormData.value.authType = undefined
    localFormData.value.authConfig = undefined
  } else {
    localFormData.value.host = undefined
    localFormData.value.port = undefined
    localFormData.value.databaseName = undefined
    localFormData.value.username = undefined
    localFormData.value.password = undefined
  }
}

// 暴露validate和getFormData方法给父组件
defineExpose({
  validate: (callback) => {
    dataSourceFormRef.value.validate(callback)
  },
  getFormData: () => {
    return { ...localFormData.value }
  }
})
</script>

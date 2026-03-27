<template>
  <el-dialog
    v-model="visible"
    title="保存为卡片"
    width="500px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="卡片名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入卡片名称"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item label="图表类型">
        <el-input
          :value="cardData.chartType"
          disabled
        />
      </el-form-item>
      
      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入卡片描述（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="SaveCardDialog">
import { ref, reactive, watch } from 'vue'
import { addChartCard } from '@/api/dashboard'
import { getCurrentInstance } from 'vue'

const { proxy } = getCurrentInstance()

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  cardData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const visible = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '',
  description: ''
})

const rules = {
  name: [
    { required: true, message: '请输入卡片名称', trigger: 'blur' },
    { min: 1, max: 50, message: '卡片名称长度在 1 到 50 个字符', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    // 初始化表单
    form.name = props.cardData.name || `查询结果_${new Date().toLocaleString()}`
    form.description = props.cardData.description || ''
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function handleClose() {
  visible.value = false
  formRef.value?.resetFields()
}

async function handleSave() {
  if (!formRef.value) {
    return
  }
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      
      try {
        // 构建卡片数据
        const cardData = {
          name: form.name,
          chartType: props.cardData.chartType || 'table',
          chartConfig: JSON.stringify({
            type: props.cardData.chartType,
            columns: props.cardData.columns || [],
            data: props.cardData.data || []
          }),
          sql: props.cardData.sql || '',
          remark: form.description
        }
        
        const response = await addChartCard(cardData)
        
        if (response.code === 200) {
          proxy.$modal.msgSuccess('卡片保存成功')
          emit('saved', response.data)
          handleClose()
        } else {
          proxy.$modal.msgError(response.msg || '保存失败')
        }
      } catch (error) {
        console.error('保存卡片失败:', error)
        proxy.$modal.msgError('保存卡片失败: ' + (error.msg || error.message || '未知错误'))
      } finally {
        saving.value = false
      }
    }
  })
}
</script>

<style scoped>
.dialog-footer {
  text-align: right;
}
</style>

<template>
  <el-dialog
    v-model="visible"
    title="提交反馈"
    width="600px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="反馈类型" prop="feedbackType">
        <el-radio-group v-model="form.feedbackType">
          <el-radio label="CORRECT">查询结果正确</el-radio>
          <el-radio label="INCORRECT">查询结果错误</el-radio>
          <el-radio label="SUGGESTION">改进建议</el-radio>
        </el-radio-group>
      </el-form-item>
      
      <el-form-item label="反馈内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="4"
          placeholder="请描述您的反馈内容"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
      
      <el-form-item 
        v-if="form.feedbackType === 'INCORRECT'"
        label="建议SQL"
        prop="suggestedSql"
      >
        <el-input
          v-model="form.suggestedSql"
          type="textarea"
          :rows="5"
          placeholder="如果您知道正确的SQL，请在此输入（可选）"
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          提交
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="FeedbackForm">
import { ref, reactive, watch } from 'vue'
import { getCurrentInstance } from 'vue'
import { submitFeedback } from '@/api/query'

const { proxy } = getCurrentInstance()

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  queryId: {
    type: [Number, String],
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'submitted'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  feedbackType: 'CORRECT',
  content: '',
  suggestedSql: ''
})

const rules = {
  feedbackType: [
    { required: true, message: '请选择反馈类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    { min: 5, max: 500, message: '反馈内容长度在 5 到 500 个字符', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    // 重置表单
    form.feedbackType = 'CORRECT'
    form.content = ''
    form.suggestedSql = ''
  }
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

function handleClose() {
  visible.value = false
  formRef.value?.resetFields()
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!props.queryId) {
        proxy.$modal.msgError('查询记录ID不能为空')
        return
      }
      
      submitting.value = true
      
      try {
        const response = await submitFeedback({
          queryId: props.queryId,
          feedbackType: form.feedbackType,
          content: form.content,
          suggestedSql: form.suggestedSql || null
        })
        
        if (response.code === 200) {
          proxy.$modal.msgSuccess('反馈提交成功，感谢您的反馈！')
          emit('submitted')
          handleClose()
        } else {
          proxy.$modal.msgError(response.msg || '提交失败')
        }
      } catch (error) {
        proxy.$modal.msgError('提交反馈失败: ' + (error.msg || error.message || '未知错误'))
      } finally {
        submitting.value = false
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

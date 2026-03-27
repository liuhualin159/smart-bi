<template>
  <div class="filter-panel">
    <el-card v-if="filters.length > 0" shadow="never">
      <template #header>
        <div class="card-header">
          <span>筛选条件</span>
          <el-button 
            v-if="hasActiveFilters" 
            type="text" 
            size="small" 
            @click="clearFilters"
          >
            清除全部
          </el-button>
        </div>
      </template>
      
      <el-form :model="filterValues" label-width="100px" size="default">
        <el-row :gutter="20">
          <el-col 
            v-for="filter in filters" 
            :key="filter.fieldName"
            :xs="24" 
            :sm="12" 
            :md="8" 
            :lg="6"
          >
            <el-form-item :label="filter.fieldLabel || filter.fieldName">
              <!-- 文本输入框 -->
              <el-input
                v-if="filter.filterType === 'text'"
                v-model="filterValues[filter.fieldName]"
                :placeholder="filter.placeholder || '请输入' + filter.fieldLabel"
                clearable
                @change="handleFilterChange"
              />
              
              <!-- 数字输入框 -->
              <el-input-number
                v-else-if="filter.filterType === 'number'"
                v-model="filterValues[filter.fieldName]"
                :min="filter.minValue"
                :max="filter.maxValue"
                :placeholder="filter.placeholder || '请输入' + filter.fieldLabel"
                style="width: 100%"
                @change="handleFilterChange"
              />
              
              <!-- 日期选择器 -->
              <el-date-picker
                v-else-if="filter.filterType === 'date'"
                v-model="filterValues[filter.fieldName]"
                type="date"
                :placeholder="filter.placeholder || '请选择' + filter.fieldLabel"
                style="width: 100%"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="handleFilterChange"
              />
              
              <!-- 单选下拉框 -->
              <el-select
                v-else-if="filter.filterType === 'select'"
                v-model="filterValues[filter.fieldName]"
                :placeholder="filter.placeholder || '请选择' + filter.fieldLabel"
                clearable
                style="width: 100%"
                @change="handleFilterChange"
              >
                <el-option
                  v-for="(option, index) in filter.options || []"
                  :key="index"
                  :label="String(option)"
                  :value="option"
                />
              </el-select>
              
              <!-- 多选下拉框 -->
              <el-select
                v-else-if="filter.filterType === 'multiSelect'"
                v-model="filterValues[filter.fieldName]"
                :placeholder="filter.placeholder || '请选择' + filter.fieldLabel"
                multiple
                clearable
                style="width: 100%"
                @change="handleFilterChange"
              >
                <el-option
                  v-for="(option, index) in filter.options || []"
                  :key="index"
                  :label="String(option)"
                  :value="option"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>

<script setup name="FilterPanel">
import { ref, computed, watch } from 'vue'

const props = defineProps({
  filters: {
    type: Array,
    default: () => []
  },
  modelValue: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const filterValues = ref({})

// 计算是否有激活的筛选器
const hasActiveFilters = computed(() => {
  return Object.values(filterValues.value).some(value => {
    if (Array.isArray(value)) {
      return value.length > 0
    }
    return value !== null && value !== undefined && value !== ''
  })
})

// 监听 props.filters 变化，初始化筛选值
watch(() => props.filters, (newFilters) => {
  if (newFilters && newFilters.length > 0) {
    newFilters.forEach(filter => {
      if (filterValues.value[filter.fieldName] === undefined) {
        if (filter.defaultValue !== undefined) {
          filterValues.value[filter.fieldName] = filter.defaultValue
        } else if (filter.filterType === 'multiSelect') {
          filterValues.value[filter.fieldName] = []
        } else {
          filterValues.value[filter.fieldName] = null
        }
      }
    })
  }
}, { immediate: true })

// 监听 props.modelValue 变化，同步到 filterValues
watch(() => props.modelValue, (newValue) => {
  if (newValue) {
    filterValues.value = { ...newValue }
  }
}, { deep: true, immediate: true })

// 筛选器变化处理
function handleFilterChange() {
  emit('update:modelValue', { ...filterValues.value })
  emit('change', { ...filterValues.value })
}

// 清除所有筛选器
function clearFilters() {
  filterValues.value = {}
  handleFilterChange()
}
</script>

<style scoped>
.filter-panel {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

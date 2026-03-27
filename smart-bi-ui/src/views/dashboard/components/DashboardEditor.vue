<template>
  <div class="dashboard-editor">
    <!-- 工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <el-dropdown @command="handleAddComponent">
          <el-button type="primary" size="small">
            <el-icon><Plus /></el-icon>
            添加组件 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="chart">
                <el-icon><DataAnalysis /></el-icon> 图表卡片
              </el-dropdown-item>
              <el-dropdown-item command="decoration" divided>
                <el-icon><Brush /></el-icon> 装饰组件
              </el-dropdown-item>
              <el-dropdown-item command="datasource" divided>
                <el-icon><Connection /></el-icon> 数据源卡片
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button size="small" @click="handleClearLayout">
          <el-icon><Delete /></el-icon>
          清空布局
        </el-button>
        <el-button size="small" @click="showBackgroundDrawer = true">
          <el-icon><Picture /></el-icon>
          背景设置
        </el-button>
        <el-button v-if="selectedCardIds.size >= 2" type="success" size="small" @click="handleCreateGroup">
          <el-icon><FolderAdd /></el-icon>
          合并为组合
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="success" size="small" @click="handleSaveLayout" :loading="saving">
          <el-icon><Check /></el-icon>
          保存布局
        </el-button>
      </div>
    </div>
    
    <!-- 拖拽画布 -->
    <div class="editor-canvas" ref="canvasRef" :style="canvasBackgroundStyle">
      <div 
        v-for="(card, index) in cards" 
        :key="card.dashboardCardId || card.cardId || index"
        class="card-item"
        :class="{ 'card-selected': selectedCardIds.has(card.dashboardCardId || card.cardId) }"
        :style="{
          position: 'absolute',
          left: card.positionX + 'px',
          top: card.positionY + 'px',
          width: card.width + 'px',
          height: card.height + 'px',
          zIndex: card.sortOrder || index
        }"
        @click.stop="handleCardClick(card, $event)"
        @contextmenu.prevent="handleCardContextMenu(card, $event)"
        @mousedown="handleCardMouseDown(card, $event)"
        @mousemove="handleCardMouseMove(card, $event)"
        @mouseup="handleCardMouseUp"
      >
        <div class="card-select-overlay" v-if="selectedCardIds.has(card.dashboardCardId || card.cardId)"></div>
        <div class="card-header">
          <span class="card-title">{{ card.cardName || '未命名卡片' }}</span>
          <div class="card-actions">
            <el-button 
              link 
              type="danger" 
              size="small"
              @click.stop="handleRemoveCard(card)"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="card-content">
          <div class="card-placeholder">
            <el-icon><DataAnalysis /></el-icon>
            <span>{{ card.chartType || '图表' }}</span>
          </div>
        </div>
        <!-- 调整大小手柄 -->
        <div 
          class="resize-handle"
          @mousedown.stop="handleResizeStart(card, $event)"
        ></div>
      </div>

      <!-- 右键菜单 -->
      <teleport to="body">
        <div
          v-if="contextMenu.visible"
          class="context-menu"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        >
          <div
            v-if="contextMenu.card && contextMenu.card.componentType === 'group'"
            class="context-menu-item"
            @click="handleUngroup(contextMenu.card.dashboardCardId)"
          >
            解除组合
          </div>
          <div class="context-menu-item" @click="handleRemoveCard(contextMenu.card); contextMenu.visible = false">
            删除卡片
          </div>
        </div>
      </teleport>
      
      <!-- 空状态提示 -->
      <EmptyState 
        v-if="cards.length === 0"
        description="拖拽或点击添加卡片到看板"
        action-text="添加卡片"
        @action="handleAddCard"
      />
    </div>
    
    <!-- 添加卡片对话框 -->
    <el-dialog
      v-model="showCardDialog"
      title="添加卡片到看板"
      width="800px"
    >
      <el-table
        :data="availableCards"
        v-loading="loadingCards"
        @selection-change="handleCardSelectionChange"
       class="aether-table">
        <el-table-column type="selection" width="55" />
        <el-table-column label="卡片名称" prop="name" />
        <el-table-column label="图表类型" prop="chartType">
          <template #default="scope">
            <el-tag>{{ getChartTypeName(scope.row.chartType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180">
          <template #default="scope">
            <span>{{ parseTime(scope.row.createTime) }}</span>
          </template>
        </el-table-column>
      </el-table>
      
      <template #footer>
        <el-button @click="showCardDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAddCards" :disabled="selectedCards.length === 0">
          添加选中卡片
        </el-button>
      </template>
    </el-dialog>

    <!-- 装饰组件选择对话框 -->
    <el-dialog v-model="showDecorationDialog" title="添加装饰组件" width="500px">
      <div class="decoration-type-list">
        <div
          v-for="item in decorationOptions"
          :key="item.type"
          class="decoration-type-item"
          @click="handleAddDecoration(item.type)"
        >
          <el-icon :size="32"><component :is="getDecorationIcon(item.icon)" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 装饰属性编辑抽屉 -->
    <el-drawer
      v-model="showDecorationEditor"
      title="装饰属性编辑"
      size="380px"
    >
      <DecorationPropertyEditor
        v-if="editingDecorationCard"
        :decoration-type="editingDecorationCard.decorationType"
        v-model="editingDecorationCard.parsedStyleConfig"
        @update:modelValue="handleDecorationStyleUpdate"
      />
    </el-drawer>

    <!-- 数据源卡片向导 -->
    <DatasourceCardWizard
      :visible="showDatasourceWizard"
      :dashboard-id="props.dashboardId"
      @update:visible="showDatasourceWizard = $event"
      @created="handleDatasourceCardCreated"
    />

    <!-- 背景设置抽屉 -->
    <el-drawer
      v-model="showBackgroundDrawer"
      title="背景设置"
      size="380px"
    >
      <BackgroundSettingPanel v-model="backgroundConfig" />
      <template #footer>
        <el-button @click="showBackgroundDrawer = false">取消</el-button>
        <el-button type="primary" :loading="savingBackground" @click="handleSaveBackground">
          保存
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="DashboardEditor">
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getCurrentInstance } from 'vue'
import { listChartCard, saveDashboardLayout, getBackground, updateBackground, getDashboardCards, createGroup, ungroupCards } from '@/api/dashboard'
import { parseTime } from '@/utils/ruoyi'
import request from '@/utils/request'
import EmptyState from './EmptyState.vue'
import BackgroundSettingPanel from './BackgroundSettingPanel.vue'
import DecorationPropertyEditor from './DecorationPropertyEditor.vue'
import DatasourceCardWizard from './datasource/DatasourceCardWizard.vue'
import { decorationOptions, decorationDefaults } from './decorations/decorationRegistry'
import { Plus, Delete, Check, Close, DataAnalysis, ArrowDown, Brush, Connection, Picture, FolderAdd, EditPen, FullScreen, SemiSelect } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()

const props = defineProps({
  dashboardId: {
    type: [Number, String],
    required: true
  },
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'save'])

const canvasRef = ref(null)
const cards = ref([])
const saving = ref(false)
const showCardDialog = ref(false)
const availableCards = ref([])
const loadingCards = ref(false)
const selectedCards = ref([])

// 多选相关 (T029)
const selectedCardIds = ref(new Set())
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  card: null
})

// 背景设置相关
const showBackgroundDrawer = ref(false)
const backgroundConfig = ref({})
const savingBackground = ref(false)
const showDecorationDialog = ref(false)
const showDatasourceWizard = ref(false)
const editingDecorationCard = ref(null)
const showDecorationEditor = ref(false)

// 加载背景配置
async function loadBackgroundConfig() {
  if (!props.dashboardId) return
  try {
    const response = await getBackground(props.dashboardId)
    if (response.code === 200 && response.data) {
      backgroundConfig.value = typeof response.data === 'string' ? JSON.parse(response.data) : response.data
    }
  } catch (error) {
    console.warn('加载背景配置失败:', error)
  }
}

// 保存背景配置
async function handleSaveBackground() {
  if (!props.dashboardId) return
  savingBackground.value = true
  try {
    const response = await updateBackground(props.dashboardId, backgroundConfig.value)
    if (response.code === 200) {
      proxy.$modal.msgSuccess('背景配置保存成功')
      showBackgroundDrawer.value = false
    } else {
      proxy.$modal.msgError(response.msg || '保存失败')
    }
  } catch (error) {
    proxy.$modal.msgError('保存背景配置失败: ' + (error.msg || error.message))
  } finally {
    savingBackground.value = false
  }
}

// 画布背景样式 (T019)
const canvasBackgroundStyle = computed(() => {
  const cfg = backgroundConfig.value
  if (!cfg || !cfg.type) return {}
  if (cfg.type === 'solid') {
    return { backgroundColor: cfg.color }
  }
  if (cfg.type === 'gradient' && cfg.colors && cfg.colors.length >= 2) {
    return {
      background: `linear-gradient(${cfg.direction || 'to bottom'}, ${cfg.colors.join(', ')})`
    }
  }
  if (cfg.type === 'image' && cfg.url) {
    const baseApi = import.meta.env.VITE_APP_BASE_API
    const fullUrl = cfg.url.startsWith('http') ? cfg.url : baseApi + cfg.url
    const fitValue = cfg.fit === 'stretch' ? '100% 100%' : (cfg.fit || 'cover')
    return {
      backgroundImage: `url(${fullUrl})`,
      backgroundSize: fitValue,
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      opacity: cfg.opacity ?? 1
    }
  }
  return {}
})

// 拖拽相关
const dragging = ref(false)
const draggingCard = ref(null)
const dragStartX = ref(0)
const dragStartY = ref(0)
const cardStartX = ref(0)
const cardStartY = ref(0)

// 调整大小相关
const resizing = ref(false)
const resizingCard = ref(null)
const resizeStartX = ref(0)
const resizeStartY = ref(0)
const resizeStartWidth = ref(0)
const resizeStartHeight = ref(0)

// 卡片尺寸限制
const MIN_CARD_WIDTH = 200
const MIN_CARD_HEIGHT = 150
const MAX_CARD_WIDTH = window.innerWidth - 100
const MAX_CARD_HEIGHT = window.innerHeight - 200

// 初始化卡片数据
watch(() => props.modelValue, (newValue) => {
  if (newValue && newValue.length > 0) {
    cards.value = newValue.map(card => ({
      ...card,
      positionX: card.positionX || 0,
      positionY: card.positionY || 0,
      width: card.width || 400,
      height: card.height || 300,
      sortOrder: card.sortOrder || 0
    }))
  } else {
    cards.value = []
  }
}, { immediate: true, deep: true })

// 添加卡片
function handleAddCard() {
  loadAvailableCards()
  showCardDialog.value = true
}

function handleAddComponent(command) {
  if (command === 'chart') {
    handleAddCard()
  } else if (command === 'decoration') {
    showDecorationDialog.value = true
  } else if (command === 'datasource') {
    showDatasourceWizard.value = true
  }
}

function getDecorationIcon(iconName) {
  const iconMap = { EditPen, FullScreen, SemiSelect }
  return iconMap[iconName] || EditPen
}

function handleAddDecoration(decorationType) {
  const defaultStyle = decorationDefaults[decorationType] || {}
  const newCard = {
    dashboardCardId: null,
    cardId: null,
    componentType: 'decoration',
    decorationType: decorationType,
    cardName: decorationOptions.find(o => o.type === decorationType)?.label || '装饰组件',
    styleConfig: JSON.stringify(defaultStyle),
    positionX: 20,
    positionY: cards.value.length * 50 + 20,
    width: decorationType === 'divider-line' ? 600 : 400,
    height: decorationType === 'divider-line' ? 40 : (decorationType === 'title-bar' ? 60 : 300),
    sortOrder: cards.value.length
  }
  cards.value.push(newCard)
  emit('update:modelValue', cards.value)
  showDecorationDialog.value = false
  proxy.$modal.msgSuccess('装饰组件已添加')
}

// 加载可用卡片列表
async function loadAvailableCards() {
  loadingCards.value = true
  try {
    const response = await listChartCard({})
    if (response.code === 200) {
      // 过滤掉已经在看板中的卡片
      const existingCardIds = cards.value.map(c => c.cardId)
      availableCards.value = response.rows.filter(card => !existingCardIds.includes(card.id))
    }
  } catch (error) {
    proxy.$modal.msgError('加载卡片列表失败: ' + (error.msg || error.message))
  } finally {
    loadingCards.value = false
  }
}

// 卡片选择变化
function handleCardSelectionChange(selection) {
  selectedCards.value = selection
}

// 确认添加卡片
function handleConfirmAddCards() {
  if (selectedCards.value.length === 0) {
    return
  }
  
  // 计算新卡片的位置（避免重叠）
  let nextX = 20
  let nextY = 20
  const cardSpacing = 20
  
  selectedCards.value.forEach((card, index) => {
    const newCard = {
      dashboardCardId: null,
      cardId: card.id,
      componentType: 'chart',
      cardName: card.name,
      chartType: card.chartType,
      chartConfig: card.chartConfig,
      positionX: nextX,
      positionY: nextY,
      width: 400,
      height: 300,
      sortOrder: cards.value.length + index
    }
    
    cards.value.push(newCard)
    
    // 计算下一个卡片位置
    nextX += 420
    if (nextX + 400 > MAX_CARD_WIDTH) {
      nextX = 20
      nextY += 320
    }
  })
  
  emit('update:modelValue', cards.value)
  showCardDialog.value = false
  selectedCards.value = []
  proxy.$modal.msgSuccess('卡片添加成功')
}

// 移除卡片
function handleRemoveCard(card) {
  proxy.$modal.confirm('是否确认从看板中移除该卡片？').then(() => {
    const index = cards.value.findIndex(c => c.cardId === card.cardId)
    if (index > -1) {
      cards.value.splice(index, 1)
      emit('update:modelValue', cards.value)
      proxy.$modal.msgSuccess('卡片已移除')
    }
  })
}

// 清空布局
function handleClearLayout() {
  proxy.$modal.confirm('是否确认清空看板布局？此操作不可恢复。').then(() => {
    cards.value = []
    emit('update:modelValue', cards.value)
    proxy.$modal.msgSuccess('布局已清空')
  })
}

// 保存布局
async function handleSaveLayout() {
  if (!props.dashboardId) {
    proxy.$modal.msgError('看板ID不能为空')
    return
  }
  
  saving.value = true
  try {
    const layoutData = {
      cards: cards.value.map((card, index) => ({
        dashboardCardId: card.dashboardCardId || null,
        cardId: card.cardId || null,
        componentType: card.componentType || 'chart',
        positionX: card.positionX,
        positionY: card.positionY,
        width: Math.max(MIN_CARD_WIDTH, Math.min(card.width, MAX_CARD_WIDTH)),
        height: Math.max(MIN_CARD_HEIGHT, Math.min(card.height, MAX_CARD_HEIGHT)),
        sortOrder: index,
        styleConfig: card.styleConfig || null,
        parentId: card.parentId || null,
        decorationType: card.decorationType || null,
        cardName: card.cardName || null
      }))
    }
    
    const response = await saveDashboardLayout(props.dashboardId, layoutData)
    if (response.code === 200) {
      proxy.$modal.msgSuccess('布局保存成功')
      emit('save', layoutData)
    } else {
      proxy.$modal.msgError(response.msg || '保存失败')
    }
  } catch (error) {
    proxy.$modal.msgError('保存布局失败: ' + (error.msg || error.message))
  } finally {
    saving.value = false
  }
}

// 卡片拖拽开始
function handleCardMouseDown(card, event) {
  if (event.button !== 0) return // 只处理左键
  
  dragging.value = true
  draggingCard.value = card
  dragStartX.value = event.clientX
  dragStartY.value = event.clientY
  cardStartX.value = card.positionX
  cardStartY.value = card.positionY
  
  event.preventDefault()
  document.addEventListener('mousemove', handleDocumentMouseMove)
  document.addEventListener('mouseup', handleDocumentMouseUp)
}

// 文档鼠标移动（拖拽）
function handleDocumentMouseMove(event) {
  if (!dragging.value || !draggingCard.value) return
  
  const deltaX = event.clientX - dragStartX.value
  const deltaY = event.clientY - dragStartY.value
  
  let newX = cardStartX.value + deltaX
  let newY = cardStartY.value + deltaY
  
  // 限制在画布范围内
  newX = Math.max(0, Math.min(newX, MAX_CARD_WIDTH - draggingCard.value.width))
  newY = Math.max(0, Math.min(newY, MAX_CARD_HEIGHT - draggingCard.value.height))
  
  draggingCard.value.positionX = newX
  draggingCard.value.positionY = newY
}

// 文档鼠标抬起（结束拖拽）
function handleDocumentMouseUp() {
  if (dragging.value) {
    dragging.value = false
    draggingCard.value = null
    emit('update:modelValue', cards.value)
  }
  
  document.removeEventListener('mousemove', handleDocumentMouseMove)
  document.removeEventListener('mouseup', handleDocumentMouseUp)
}

// 注意：卡片拖拽功能由vuedraggable库处理，使用文档级别事件
// 这些函数已由vuedraggable库内部实现，无需手动处理

// 调整大小开始
function handleResizeStart(card, event) {
  if (event.button !== 0) return
  
  resizing.value = true
  resizingCard.value = card
  resizeStartX.value = event.clientX
  resizeStartY.value = event.clientY
  resizeStartWidth.value = card.width
  resizeStartHeight.value = card.height
  
  event.preventDefault()
  event.stopPropagation()
  document.addEventListener('mousemove', handleDocumentResizeMove)
  document.addEventListener('mouseup', handleDocumentResizeUp)
}

// 文档调整大小移动
function handleDocumentResizeMove(event) {
  if (!resizing.value || !resizingCard.value) return
  
  const deltaX = event.clientX - resizeStartX.value
  const deltaY = event.clientY - resizeStartY.value
  
  let newWidth = resizeStartWidth.value + deltaX
  let newHeight = resizeStartHeight.value + deltaY
  
  // 限制尺寸
  newWidth = Math.max(MIN_CARD_WIDTH, Math.min(newWidth, MAX_CARD_WIDTH - resizingCard.value.positionX))
  newHeight = Math.max(MIN_CARD_HEIGHT, Math.min(newHeight, MAX_CARD_HEIGHT - resizingCard.value.positionY))
  
  resizingCard.value.width = newWidth
  resizingCard.value.height = newHeight
}

// 文档调整大小结束
function handleDocumentResizeUp() {
  if (resizing.value) {
    resizing.value = false
    resizingCard.value = null
    emit('update:modelValue', cards.value)
  }
  
  document.removeEventListener('mousemove', handleDocumentResizeMove)
  document.removeEventListener('mouseup', handleDocumentResizeUp)
}

// 获取图表类型名称
function getChartTypeName(chartType) {
  const typeMap = {
    'bar': '柱状图',
    'line': '折线图',
    'pie': '饼图',
    'groupedBar': '分组柱状图',
    'kpi': '指标卡',
    'table': '表格'
  }
  return typeMap[chartType] || chartType
}

// 卡片多选点击 (T029)
function handleCardClick(card, event) {
  const cardKey = card.dashboardCardId || card.cardId
  if (!cardKey) return
  if (event.shiftKey) {
    const newSet = new Set(selectedCardIds.value)
    if (newSet.has(cardKey)) {
      newSet.delete(cardKey)
    } else {
      newSet.add(cardKey)
    }
    selectedCardIds.value = newSet
  } else {
    if (selectedCardIds.value.has(cardKey) && selectedCardIds.value.size === 1) {
      selectedCardIds.value = new Set()
    } else {
      selectedCardIds.value = new Set([cardKey])
    }
  }
  if (card.componentType === 'decoration' && !event.shiftKey) {
    editingDecorationCard.value = {
      ...card,
      parsedStyleConfig: card.styleConfig ? (typeof card.styleConfig === 'string' ? JSON.parse(card.styleConfig) : card.styleConfig) : {}
    }
    showDecorationEditor.value = true
  }
}

function handleDecorationStyleUpdate(newStyle) {
  if (!editingDecorationCard.value) return
  const idx = cards.value.findIndex(c =>
    (c.dashboardCardId && c.dashboardCardId === editingDecorationCard.value.dashboardCardId) ||
    (c === editingDecorationCard.value)
  )
  if (idx !== -1) {
    cards.value[idx].styleConfig = JSON.stringify(newStyle)
    emit('update:modelValue', cards.value)
  }
}

function handleDatasourceCardCreated(config) {
  const newCard = {
    dashboardCardId: null,
    cardId: null,
    componentType: 'datasource',
    cardName: config.cardName || '数据源卡片',
    styleConfig: null,
    positionX: 20,
    positionY: cards.value.length * 50 + 20,
    width: 500,
    height: 350,
    sortOrder: cards.value.length,
    datasourceConfig: config
  }
  cards.value.push(newCard)
  emit('update:modelValue', cards.value)
  showDatasourceWizard.value = false
  proxy.$modal.msgSuccess('数据源卡片已添加')
}

// 右键菜单 (T031)
function handleCardContextMenu(card, event) {
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.card = card
}

function closeContextMenu() {
  contextMenu.visible = false
  contextMenu.card = null
}

// 刷新卡片列表
async function loadCards() {
  if (!props.dashboardId) return
  try {
    const response = await getDashboardCards(props.dashboardId)
    if (response.code === 200) {
      const data = response.data || []
      cards.value = data.map(card => ({
        ...card,
        positionX: card.positionX || 0,
        positionY: card.positionY || 0,
        width: card.width || 400,
        height: card.height || 300,
        sortOrder: card.sortOrder || 0
      }))
      emit('update:modelValue', cards.value)
    }
  } catch (error) {
    console.warn('刷新卡片列表失败:', error)
  }
}

// 创建组合 (T030)
async function handleCreateGroup() {
  if (selectedCardIds.value.size < 2) return
  try {
    const cardIds = Array.from(selectedCardIds.value)
    await createGroup(props.dashboardId, {
      cardIds: cardIds,
      groupName: '卡片组合'
    })
    await loadCards()
    selectedCardIds.value = new Set()
    proxy.$modal.msgSuccess('组合创建成功')
  } catch (error) {
    proxy.$modal.msgError('组合创建失败: ' + (error.msg || error.message))
  }
}

// 解除组合 (T031)
async function handleUngroup(groupId) {
  try {
    await ungroupCards(props.dashboardId, groupId)
    contextMenu.visible = false
    contextMenu.card = null
    await loadCards()
    proxy.$modal.msgSuccess('解除组合成功')
  } catch (error) {
    proxy.$modal.msgError('解除组合失败: ' + (error.msg || error.message))
  }
}

// 组件加载时获取背景配置
onMounted(() => {
  loadBackgroundConfig()
  document.addEventListener('click', closeContextMenu)
})

// 组件卸载时清理事件监听
onBeforeUnmount(() => {
  document.removeEventListener('mousemove', handleDocumentMouseMove)
  document.removeEventListener('mouseup', handleDocumentMouseUp)
  document.removeEventListener('mousemove', handleDocumentResizeMove)
  document.removeEventListener('mouseup', handleDocumentResizeUp)
})
</script>

<style scoped>
.dashboard-editor {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.editor-canvas {
  flex: 1;
  position: relative;
  overflow: auto;
  background: #fafafa;
  min-height: 600px;
}

.card-item {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  cursor: move;
  user-select: none;
}

.card-item:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
  cursor: move;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.card-content {
  padding: 20px;
  height: calc(100% - 40px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-placeholder {
  text-align: center;
  color: #909399;
}

.card-placeholder .el-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
  background: linear-gradient(135deg, transparent 0%, transparent 40%, #409EFF 40%, #409EFF 60%, transparent 60%);
}

.resize-handle:hover {
  background: linear-gradient(135deg, transparent 0%, transparent 40%, #66b1ff 40%, #66b1ff 60%, transparent 60%);
}

.card-selected {
  outline: 2px solid #409eff;
  outline-offset: 2px;
}
.card-select-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(64, 158, 255, 0.08);
  pointer-events: none;
  z-index: 10;
}
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0,0,0,.12);
  padding: 4px 0;
  min-width: 120px;
}
.context-menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
}
.context-menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}
.decoration-type-list {
  display: flex;
  gap: 20px;
  justify-content: center;
  padding: 20px 0;
}
.decoration-type-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 100px;
}
.decoration-type-item:hover {
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 2px 8px rgba(64,158,255,0.2);
}
</style>

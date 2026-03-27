<template>
  <div class="designer-root" :class="{ 'preview-mode': isPreview }" @click="handleRootClick" @keydown="handleKeyDown" tabindex="0">
    <!-- 顶部工具栏 -->
    <header class="designer-header">
      <div class="header-left">
        <button class="back-btn" @click="handleBack" title="返回">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        </button>
        <div class="header-divider"></div>
        <div class="dashboard-name-wrap">
          <input
            v-if="editingName"
            ref="nameInputRef"
            v-model="dashboard.name"
            class="name-input"
            @blur="finishEditName"
            @keydown.enter="finishEditName"
            maxlength="50"
          />
          <h1 v-else class="dashboard-name" @dblclick="startEditName">
            {{ dashboard.name || '未命名看板' }}
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="edit-icon"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
          </h1>
        </div>
      </div>
      <div class="header-center">
        <div class="zoom-controls">
          <button class="tool-btn" @click="zoomOut" title="缩小">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </button>
          <span class="zoom-label">{{ Math.round(zoom * 100) }}%</span>
          <button class="tool-btn" @click="zoomIn" title="放大">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </button>
          <button class="tool-btn" @click="zoomReset" title="重置缩放">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7"/></svg>
          </button>
        </div>
      </div>
      <div class="header-right">
        <button class="tool-btn" :class="{ active: showGrid }" @click="showGrid = !showGrid" title="网格">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        </button>
        <button class="tool-btn" :class="{ active: snapToGrid }" @click="snapToGrid = !snapToGrid" title="吸附网格">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1"/><path d="M12 2v4m0 12v4M2 12h4m12 0h4"/></svg>
        </button>
        <div class="header-divider"></div>
        <button class="action-btn secondary" @click="handlePreview" :disabled="cards.length === 0">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          预览
        </button>
        <button class="action-btn primary" @click="handleSave" :disabled="saving">
          <svg v-if="!saving" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
          <span v-if="saving" class="spinner"></span>
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </header>

    <div class="designer-body">
      <!-- 左侧组件面板 -->
      <aside class="panel-left" :class="{ collapsed: !showLeftPanel }">
        <div class="panel-toggle" @click="showLeftPanel = !showLeftPanel">
          <svg :class="{ flipped: !showLeftPanel }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
        </div>
        <div v-show="showLeftPanel" class="panel-content">
          <div class="panel-section">
            <h3 class="section-title">组件</h3>
            <div class="component-grid">
              <div
                v-for="comp in componentList"
                :key="comp.type"
                class="component-card"
                draggable="true"
                @dragstart="handleComponentDragStart($event, comp)"
                @click="handleAddComponent(comp)"
              >
                <div class="comp-icon" v-html="comp.icon"></div>
                <span class="comp-label">{{ comp.label }}</span>
              </div>
            </div>
          </div>
          <div class="panel-section">
            <h3 class="section-title">图表</h3>
            <div class="component-grid">
              <div
                v-for="ct in chartTypeList"
                :key="ct.chartType"
                class="component-card chart-type-card"
                draggable="true"
                @dragstart="handleChartTypeDragStart($event, ct)"
              >
                <div class="comp-icon" v-html="ct.icon"></div>
                <span class="comp-label">{{ ct.label }}</span>
              </div>
            </div>
          </div>
          <div class="panel-section">
            <h3 class="section-title">装饰</h3>
            <div class="component-grid">
              <div
                v-for="deco in decorationList"
                :key="deco.type"
                class="component-card decoration"
                @click="handleAddDecoration(deco.type)"
              >
                <div class="comp-icon" v-html="deco.icon"></div>
                <span class="comp-label">{{ deco.label }}</span>
              </div>
            </div>
          </div>
          <div class="panel-section">
            <h3 class="section-title">
              已有卡片
              <button class="refresh-btn" @click="loadAvailableCards" title="刷新">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
              </button>
            </h3>
            <div v-if="loadingAvailableCards" class="loading-indicator">
              <span class="spinner small"></span> 加载中...
            </div>
            <div v-else-if="availableCards.length === 0" class="empty-hint">暂无可用卡片</div>
            <div v-else class="card-list">
              <div
                v-for="card in availableCards"
                :key="card.id"
                class="available-card-item"
                @click="handleAddExistingCard(card)"
              >
                <div class="card-type-badge">{{ getChartTypeName(card.chartType) }}</div>
                <span class="card-item-name">{{ card.name }}</span>
              </div>
            </div>
          </div>
          <div class="panel-section">
            <button type="button" class="report-open-drawer-btn" @click="showReportDrawer = true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/></svg>
              <span>生成报表</span>
            </button>
          </div>
        </div>
      </aside>

      <!-- 画布工作区：深色基底 + 紫/青/粉光晕点缀（内联保证必现） -->
      <main
        class="canvas-area"
        ref="canvasAreaRef"
        :style="canvasAreaStyle"
        @drop="handleCanvasDrop"
        @dragover.prevent
        @mousedown="handleCanvasMouseDown"
      >
        <div
          class="canvas-viewport"
          ref="canvasViewportRef"
          :style="{
            transform: `scale(${zoom}) translate(${panX}px, ${panY}px)`,
            transformOrigin: '0 0'
          }"
        >
          <div
            class="canvas-surface"
            ref="canvasSurfaceRef"
            :style="[canvasBackgroundStyle, { width: canvasWidth + 'px', height: canvasHeight + 'px' }]"
          >
            <!-- 网格 -->
            <svg v-if="showGrid" class="grid-overlay" :width="canvasWidth" :height="canvasHeight">
              <defs>
                <pattern id="smallGrid" :width="gridSize" :height="gridSize" patternUnits="userSpaceOnUse">
                  <path :d="`M ${gridSize} 0 L 0 0 0 ${gridSize}`" fill="none" stroke="rgba(124,108,240,0.05)" stroke-width="0.5"/>
                </pattern>
                <pattern id="grid" :width="gridSize * 5" :height="gridSize * 5" patternUnits="userSpaceOnUse">
                  <rect :width="gridSize * 5" :height="gridSize * 5" fill="url(#smallGrid)"/>
                  <path :d="`M ${gridSize * 5} 0 L 0 0 0 ${gridSize * 5}`" fill="none" stroke="rgba(34,211,238,0.08)" stroke-width="1"/>
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#grid)" />
            </svg>

            <!-- 卡片 -->
            <div
              v-for="(card, index) in cards"
              :key="card._uid"
              class="design-card"
              :class="{
                selected: selectedCardId === card._uid,
                dragging: draggingCardId === card._uid,
                resizing: resizingCardId === card._uid
              }"
              :style="{
                left: card.positionX + 'px',
                top: card.positionY + 'px',
                width: card.width + 'px',
                height: card.height + 'px',
                zIndex: card.sortOrder
              }"
              @mousedown.stop="handleCardMouseDown(card, $event)"
              @click.stop="selectCard(card._uid)"
              @dblclick.stop="handleCardDblClick(card)"
            >
              <div class="design-card-header">
                <span class="design-card-title">{{ card.cardName || '未命名' }}</span>
                <div class="design-card-type">{{ getCardTypeLabel(card) }}</div>
              </div>
              <div class="design-card-body">
                <CardRenderer :key="card._uid + '_' + (card._refreshKey || 0)" :card="card" />
              </div>

              <!-- 选中态控制点 -->
              <template v-if="!isPreview && selectedCardId === card._uid">
                <div class="resize-handle nw" @mousedown.stop="handleResizeStart(card, 'nw', $event)"></div>
                <div class="resize-handle ne" @mousedown.stop="handleResizeStart(card, 'ne', $event)"></div>
                <div class="resize-handle sw" @mousedown.stop="handleResizeStart(card, 'sw', $event)"></div>
                <div class="resize-handle se" @mousedown.stop="handleResizeStart(card, 'se', $event)"></div>
                <div class="resize-handle n" @mousedown.stop="handleResizeStart(card, 'n', $event)"></div>
                <div class="resize-handle s" @mousedown.stop="handleResizeStart(card, 's', $event)"></div>
                <div class="resize-handle w" @mousedown.stop="handleResizeStart(card, 'w', $event)"></div>
                <div class="resize-handle e" @mousedown.stop="handleResizeStart(card, 'e', $event)"></div>
              </template>
            </div>

            <!-- 空状态 -->
            <div v-if="cards.length === 0" class="canvas-empty">
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" opacity="0.3">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <line x1="12" y1="8" x2="12" y2="16"/>
                <line x1="8" y1="12" x2="16" y2="12"/>
              </svg>
              <p>从左侧拖入组件，或点击添加</p>
            </div>
          </div>
        </div>
      </main>

      <!-- 右侧属性面板 -->
      <aside class="panel-right" :class="{ visible: selectedCard !== null }">
        <template v-if="selectedCard">
          <div class="panel-header">
            <h3>属性</h3>
            <button class="close-panel-btn" @click="selectedCardId = null">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <div class="props-content">
            <!-- 基本信息 -->
            <div class="prop-group">
              <label class="prop-label">名称</label>
              <input class="prop-input" v-model="selectedCard.cardName" @change="emitUpdate" />
            </div>
            <div class="prop-group">
              <label class="prop-label">类型</label>
              <div class="prop-readonly">{{ getCardTypeLabel(selectedCard) }}</div>
            </div>

            <!-- 位置和尺寸 -->
            <div class="prop-section-title">布局</div>
            <div class="prop-row">
              <div class="prop-group half">
                <label class="prop-label">X</label>
                <input class="prop-input" type="number" v-model.number="selectedCard.positionX" @change="emitUpdate" />
              </div>
              <div class="prop-group half">
                <label class="prop-label">Y</label>
                <input class="prop-input" type="number" v-model.number="selectedCard.positionY" @change="emitUpdate" />
              </div>
            </div>
            <div class="prop-row">
              <div class="prop-group half">
                <label class="prop-label">宽度</label>
                <input class="prop-input" type="number" v-model.number="selectedCard.width" :min="MIN_CARD_WIDTH" @change="emitUpdate" />
              </div>
              <div class="prop-group half">
                <label class="prop-label">高度</label>
                <input class="prop-input" type="number" v-model.number="selectedCard.height" :min="MIN_CARD_HEIGHT" @change="emitUpdate" />
              </div>
            </div>
            <div class="prop-group">
              <label class="prop-label">层级</label>
              <input class="prop-input" type="number" v-model.number="selectedCard.sortOrder" :min="0" @change="emitUpdate" />
            </div>

            <!-- 装饰属性编辑 -->
            <template v-if="selectedCard.componentType === 'decoration'">
              <div class="prop-section-title">样式配置</div>
              <DecorationPropertyEditor
                :decoration-type="selectedCard.decorationType"
                v-model="selectedCardParsedStyle"
                @update:modelValue="handleDecorationStyleUpdate"
              />
            </template>

            <!-- 数据源属性编辑 -->
            <template v-if="selectedCard.componentType === 'datasource'">
              <DatasourcePropertyEditor
                v-model="selectedCard.datasourceConfig"
                show-refresh-preview
                @refresh="handleDatasourceConfigRefresh"
              />
            </template>

            <!-- 操作 -->
            <div class="prop-section-title">操作</div>
            <div class="prop-actions">
              <button class="danger-btn" @click="handleRemoveCard(selectedCard)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                删除卡片
              </button>
            </div>
          </div>
        </template>
      </aside>
    </div>

    <!-- 底部状态栏 -->
    <footer class="designer-footer">
      <span class="status-item">{{ cards.length }} 个组件</span>
      <span class="status-item" v-if="selectedCard">
        已选: {{ selectedCard.cardName || '未命名' }} ({{ selectedCard.width }}×{{ selectedCard.height }})
      </span>
      <span class="status-spacer"></span>
      <span class="status-item hint">双击卡片名编辑 · Shift+滚轮缩放 · Delete 删除</span>
    </footer>

    <!-- 添加卡片对话框 -->
    <el-dialog v-model="showCardDialog" title="添加卡片到看板" width="800px" append-to-body>
      <el-table :data="availableCards" v-loading="loadingAvailableCards" @selection-change="handleCardSelectionChange" class="aether-table">
        <el-table-column type="selection" width="55" />
        <el-table-column label="卡片名称" prop="name" />
        <el-table-column label="图表类型" prop="chartType">
          <template #default="scope">
            <el-tag>{{ getChartTypeName(scope.row.chartType) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showCardDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAddCards" :disabled="selectedTableCards.length === 0">添加选中卡片</el-button>
      </template>
    </el-dialog>

    <!-- 背景设置抽屉 -->
    <el-drawer v-model="showBackgroundDrawer" title="画布背景" size="360px" append-to-body>
      <BackgroundSettingPanel v-model="backgroundConfig" />
      <template #footer>
        <el-button @click="showBackgroundDrawer = false">取消</el-button>
        <el-button type="primary" :loading="savingBackground" @click="handleSaveBackground">保存</el-button>
      </template>
    </el-drawer>

    <!-- 生成报表：右侧抽屉 + 对话列表 + SSE 进度 -->
    <el-drawer
      v-model="showReportDrawer"
      title="生成报表"
      direction="rtl"
      size="420px"
      append-to-body
      class="report-generate-drawer"
    >
      <div class="report-drawer-body">
        <div class="report-messages" ref="reportMessagesRef">
          <div
            v-for="(msg, idx) in reportMessages"
            :key="idx"
            class="report-msg"
            :class="msg.role"
          >
            <span class="report-msg-content">{{ msg.content }}</span>
          </div>
          <div v-if="reportGenerating && reportMessages.length > 0" class="report-msg system">
            <span class="spinner small"></span>
            <span class="report-msg-content">处理中…</span>
          </div>
        </div>
        <div class="report-drawer-footer">
          <textarea
            v-model="reportPrompt"
            class="report-prompt-input"
            placeholder="例如：我想要创建一个商品销售大屏，包含销售趋势、区域对比和 Top 商品"
            rows="3"
            :disabled="reportGenerating"
          />
          <button
            type="button"
            class="report-generate-btn"
            :disabled="reportGenerating || !reportPrompt.trim()"
            @click="handleGenerateReportStream"
          >
            <span v-if="reportGenerating" class="spinner small"></span>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/></svg>
            {{ reportGenerating ? '生成中…' : '生成' }}
          </button>
        </div>
      </div>
    </el-drawer>

    <!-- 预览模式：浮动退出按钮 -->
    <transition name="fade">
      <button v-if="isPreview" class="preview-exit-btn" @click="exitPreview" title="退出预览 (ESC)">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        退出预览
      </button>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentInstance } from 'vue'
import {
  getDashboard, updateDashboard, getDashboardCards, saveDashboardLayout,
  listChartCard, getBackground, updateBackground, generateReportStream
} from '@/api/dashboard'
import CardRenderer from './components/CardRenderer.vue'
import DecorationPropertyEditor from './components/DecorationPropertyEditor.vue'
import DatasourcePropertyEditor from './components/datasource/DatasourcePropertyEditor.vue'
import BackgroundSettingPanel from './components/BackgroundSettingPanel.vue'
import { decorationOptions, decorationDefaults } from './components/decorations/decorationRegistry'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const dashboardId = computed(() => route.query.id)
const isPreview = computed(() => route.query.mode === 'preview')
const dashboard = ref({ name: '', refreshInterval: 0, isPublic: false, remark: '' })
const cards = ref([])
const saving = ref(false)
const editingName = ref(false)
const nameInputRef = ref(null)

const MIN_CARD_WIDTH = 160
const MIN_CARD_HEIGHT = 120
const canvasWidth = ref(2400)
const canvasHeight = ref(1600)

let uidCounter = 0
function genUid() { return `_card_${++uidCounter}_${Date.now()}` }

// 缩放与平移
const zoom = ref(1)
const panX = ref(0)
const panY = ref(0)
const showGrid = ref(true)
const snapToGrid = ref(true)
const gridSize = ref(20)

function zoomIn() { zoom.value = Math.min(3, zoom.value + 0.1) }
function zoomOut() { zoom.value = Math.max(0.2, zoom.value - 0.1) }
function zoomReset() { zoom.value = 1; panX.value = 0; panY.value = 0 }

// 面板
const showLeftPanel = ref(true)
const selectedCardId = ref(null)
const selectedCard = computed(() => cards.value.find(c => c._uid === selectedCardId.value) || null)
const selectedCardParsedStyle = ref({})

watch(selectedCard, (card) => {
  if (card && card.styleConfig) {
    try {
      selectedCardParsedStyle.value = typeof card.styleConfig === 'string' ? JSON.parse(card.styleConfig) : { ...card.styleConfig }
    } catch { selectedCardParsedStyle.value = {} }
  } else {
    selectedCardParsedStyle.value = {}
  }
}, { immediate: true })

watch(selectedCard, (card) => {
  if (!card || card.componentType !== 'datasource') return
  if (!card.datasourceConfig || typeof card.datasourceConfig !== 'object') {
    card.datasourceConfig = {
      datasourceId: null,
      queryType: 'SQL',
      sqlTemplate: '',
      apiUrl: '',
      apiMethod: 'GET',
      apiHeaders: '',
      apiBody: '',
      responseDataPath: '',
      chartType: 'bar',
      columnMapping: JSON.stringify({ xAxis: '', yAxis: [], category: '' })
    }
  }
  if (card.cardName && card.datasourceConfig.cardName !== card.cardName) {
    card.datasourceConfig.cardName = card.cardName
  }
}, { immediate: true })

// 对话框
const showCardDialog = ref(false)
const showBackgroundDrawer = ref(false)
const availableCards = ref([])
const loadingAvailableCards = ref(false)
const selectedTableCards = ref([])
const reportPrompt = ref('')
const reportGenerating = ref(false)
const showReportDrawer = ref(false)
const reportMessages = ref([])
const reportMessagesRef = ref(null)
// 画布深色默认（与 BackgroundSettingPanel 一致），无配置或旧浅色默认时使用
const DEFAULT_BACKGROUND_CONFIG = {
  type: 'gradient',
  direction: 'to bottom right',
  colors: ['#0e0f14', '#1a1b26']
}
const backgroundConfig = ref({ ...DEFAULT_BACKGROUND_CONFIG })
const savingBackground = ref(false)

// 拖拽状态
const draggingCardId = ref(null)
const dragOffsetX = ref(0)
const dragOffsetY = ref(0)
const resizingCardId = ref(null)
const resizeDirection = ref(null)
const resizeStartRect = ref({ x: 0, y: 0, w: 0, h: 0 })
const resizeStartMouse = ref({ x: 0, y: 0 })

const canvasAreaRef = ref(null)
const canvasViewportRef = ref(null)
const canvasSurfaceRef = ref(null)

const componentList = [
  {
    type: 'chart',
    label: '图表卡片',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 20V10M12 20V4M6 20v-6"/></svg>'
  },
  {
    type: 'background',
    label: '画布背景',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>'
  }
]

const chartTypeList = [
  {
    chartType: 'bar',
    label: '柱状图',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M18 20V10M12 20V4M6 20v-6"/></svg>'
  },
  {
    chartType: 'line',
    label: '折线图',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="22 12 18 8 13 13 9 9 2 16"/></svg>'
  },
  {
    chartType: 'pie',
    label: '饼图',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21.21 15.89A10 10 0 1 1 8 2.83"/><path d="M22 12A10 10 0 0 0 12 2v10z"/></svg>'
  },
  {
    chartType: 'groupedBar',
    label: '分组柱状图',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="10" width="4" height="10"/><rect x="7" y="6" width="4" height="14"/><rect x="13" y="10" width="4" height="10"/><rect x="18" y="3" width="4" height="17"/></svg>'
  },
  {
    chartType: 'kpi',
    label: '指标卡',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 14l2 2 4-5"/></svg>'
  },
  {
    chartType: 'table',
    label: '表格',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="21" y2="15"/><line x1="9" y1="3" x2="9" y2="21"/></svg>'
  }
]

const decorationList = [
  {
    type: 'title-bar',
    label: '标题栏',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M4 7h16M4 12h10"/></svg>'
  },
  {
    type: 'divider-line',
    label: '分隔线',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><line x1="3" y1="12" x2="21" y2="12"/></svg>'
  },
  {
    type: 'border-box',
    label: '边框盒',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2" ry="2" stroke-dasharray="4 2"/></svg>'
  }
]

// 画布外围样式（深色 + 光晕）：内联保证在 /dashboard/designer 下一定生效
const canvasAreaStyle = {
  background: '#08090e',
  backgroundImage: [
    'radial-gradient(ellipse 80% 50% at 15% 20%, rgba(124, 108, 240, 0.1) 0%, transparent 55%)',
    'radial-gradient(ellipse 60% 40% at 85% 75%, rgba(34, 211, 238, 0.08) 0%, transparent 50%)',
    'radial-gradient(ellipse 50% 35% at 75% 15%, rgba(236, 72, 153, 0.06) 0%, transparent 45%)',
    'radial-gradient(circle at 50% 50%, rgba(124, 108, 240, 0.03) 0%, transparent 70%)'
  ].join(', ')
}

// 画布背景样式（表面：纯色/渐变/图片，无配置时用深色渐变）
const canvasBackgroundStyle = computed(() => {
  const cfg = backgroundConfig.value
  if (!cfg || !cfg.type) {
    return { background: 'linear-gradient(to bottom right, #0e0f14, #1a1b26)' }
  }
  if (cfg.type === 'solid') return { backgroundColor: cfg.color }
  if (cfg.type === 'gradient' && cfg.colors?.length >= 2) {
    return { background: `linear-gradient(${cfg.direction || 'to bottom'}, ${cfg.colors.join(', ')})` }
  }
  if (cfg.type === 'image' && cfg.url) {
    const baseApi = import.meta.env.VITE_APP_BASE_API
    const fullUrl = cfg.url.startsWith('http') ? cfg.url : baseApi + cfg.url
    return {
      backgroundImage: `url(${fullUrl})`,
      backgroundSize: cfg.fit === 'stretch' ? '100% 100%' : (cfg.fit || 'cover'),
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }
  }
  return { background: 'linear-gradient(to bottom right, #0e0f14, #1a1b26)' }
})

function snapValue(val) {
  if (!snapToGrid.value) return val
  return Math.round(val / gridSize.value) * gridSize.value
}

// ==================== 数据加载 ====================
async function loadDashboard() {
  const id = dashboardId.value
  if (!id) return
  try {
    const res = await getDashboard(id)
    const data = res.data?.dashboard || res.data
    dashboard.value = { ...data }
    // 始终使用 getDashboardCards 获取完整卡片数据（含关联的 chartConfig/chartType/cardName）
    // getInfo 返回的 cards 是原始 DashboardCard 实体，缺少 ChartCard 关联字段
    const cardsRes = await getDashboardCards(id)
    setCards(cardsRes.data || [])
  } catch (e) {
    proxy.$modal.msgError('加载看板失败: ' + (e.msg || e.message))
  }
}

function setCards(list) {
  cards.value = list.map(c => ({
    ...c,
    _uid: genUid(),
    positionX: c.positionX || 0,
    positionY: c.positionY || 0,
    width: c.width || 400,
    height: c.height || 300,
    sortOrder: c.sortOrder || 1
  }))
  autoExpandCanvas()
}

function autoExpandCanvas() {
  let maxX = 2400, maxY = 1600
  cards.value.forEach(c => {
    maxX = Math.max(maxX, c.positionX + c.width + 200)
    maxY = Math.max(maxY, c.positionY + c.height + 200)
  })
  canvasWidth.value = maxX
  canvasHeight.value = maxY
}

async function loadBackgroundConfig() {
  if (!dashboardId.value) return
  try {
    const res = await getBackground(dashboardId.value)
    let cfg = res.data != null && res.data !== '' ? (typeof res.data === 'string' ? JSON.parse(res.data) : res.data) : null
    if (!cfg || !cfg.type) {
      backgroundConfig.value = { ...DEFAULT_BACKGROUND_CONFIG }
      return
    }
    // 旧版默认浅色视为未设置，改用深色预设
    if (cfg.type === 'solid' && (cfg.color === '#fafafa' || cfg.color === '#f5f5f5')) {
      backgroundConfig.value = { ...DEFAULT_BACKGROUND_CONFIG }
      return
    }
    backgroundConfig.value = cfg.colors ? { ...cfg, colors: [...cfg.colors] } : { ...cfg }
  } catch (e) {
    console.warn('加载背景配置失败:', e)
    backgroundConfig.value = { ...DEFAULT_BACKGROUND_CONFIG }
  }
}

async function loadAvailableCards() {
  loadingAvailableCards.value = true
  try {
    const res = await listChartCard({})
    const rows = res.rows || res.data || []
    const existing = new Set(cards.value.map(c => c.cardId))
    availableCards.value = rows.filter(c => !existing.has(c.id))
  } catch (e) {
    console.warn('加载可用卡片失败:', e)
  } finally {
    loadingAvailableCards.value = false
  }
}

// ==================== 保存 ====================
async function handleSave() {
  if (!dashboardId.value) return
  saving.value = true

  const unconfiguredCount = cards.value.filter(c => {
    if (c.componentType !== 'datasource') return false
    const cfg = c.datasourceConfig
    if (!cfg) return true
    const hasQuery = cfg.queryType === 'SQL' ? !!(cfg.sqlTemplate || '').trim() : !!(cfg.apiUrl || '').trim()
    return !cfg.datasourceId && !hasQuery
  }).length
  if (unconfiguredCount > 0) {
    proxy.$modal.msgWarning(`${unconfiguredCount} 个图表卡片尚未配置数据源，已按草稿保存`)
  }

  try {
    await updateDashboard({
      id: dashboard.value.id,
      name: dashboard.value.name,
      refreshInterval: dashboard.value.refreshInterval,
      isPublic: dashboard.value.isPublic,
      remark: dashboard.value.remark
    })
    const layoutData = {
      cards: cards.value.map((card, idx) => ({
        dashboardCardId: card.dashboardCardId || null,
        cardId: card.cardId || null,
        componentType: card.componentType || 'chart',
        positionX: card.positionX,
        positionY: card.positionY,
        width: Math.max(MIN_CARD_WIDTH, card.width),
        height: Math.max(MIN_CARD_HEIGHT, card.height),
        sortOrder: idx,
        styleConfig: card.styleConfig || null,
        parentId: card.parentId || null,
        decorationType: card.decorationType || null,
        cardName: card.cardName || null,
        datasourceConfig: card.componentType === 'datasource' ? (card.datasourceConfig || null) : undefined
      }))
    }
    const res = await saveDashboardLayout(dashboardId.value, layoutData)
    const savedCards = res.data?.cards || res.cards || []
    if (savedCards.length > 0) {
      cards.value.forEach((card, idx) => {
        const saved = savedCards[idx]
        if (saved) {
          card.dashboardCardId = saved.dashboardCardId || card.dashboardCardId
          if (saved.datasourceConfig && card.datasourceConfig) {
            card.datasourceConfig.id = saved.datasourceConfig.id
            card.datasourceConfig.dashboardCardId = saved.datasourceConfig.dashboardCardId
          }
        }
      })
    }
    // 保存后从后端重新拉取一次，避免“已删除卡片”仍留在本地/预览读到旧布局
    const cardsRes = await getDashboardCards(dashboardId.value)
    setCards(cardsRes.data || [])
    await loadAvailableCards()
    proxy.$modal.msgSuccess('保存成功')
  } catch (e) {
    proxy.$modal.msgError('保存失败: ' + (e.msg || e.message))
  } finally {
    saving.value = false
  }
}

async function handleSaveBackground() {
  if (!dashboardId.value) return
  savingBackground.value = true
  try {
    await updateBackground(dashboardId.value, backgroundConfig.value)
    proxy.$modal.msgSuccess('背景已保存')
    showBackgroundDrawer.value = false
  } catch (e) {
    proxy.$modal.msgError('保存背景失败')
  } finally {
    savingBackground.value = false
  }
}

// ==================== 添加组件 ====================
function handleAddComponent(comp) {
  if (comp.type === 'chart') {
    loadAvailableCards()
    showCardDialog.value = true
  } else if (comp.type === 'background') {
    showBackgroundDrawer.value = true
  }
}

function handleAddDecoration(decorationType) {
  const defaults = decorationDefaults[decorationType] || {}
  const newCard = {
    _uid: genUid(),
    dashboardCardId: null,
    cardId: null,
    componentType: 'decoration',
    decorationType,
    cardName: decorationOptions.find(o => o.type === decorationType)?.label || '装饰组件',
    styleConfig: JSON.stringify(defaults),
    positionX: snapValue(40),
    positionY: snapValue(cards.value.length * 60 + 40),
    width: decorationType === 'divider-line' ? 600 : 400,
    height: decorationType === 'divider-line' ? 40 : (decorationType === 'title-bar' ? 60 : 300),
    sortOrder: cards.value.length + 1
  }
  cards.value.push(newCard)
  selectedCardId.value = newCard._uid
  autoExpandCanvas()
}

function handleAddExistingCard(card) {
  const pos = findOpenPosition()
  const newCard = {
    _uid: genUid(),
    dashboardCardId: null,
    cardId: card.id,
    componentType: 'chart',
    cardName: card.name,
    chartType: card.chartType,
    chartConfig: card.chartConfig,
    positionX: pos.x,
    positionY: pos.y,
    width: 400,
    height: 300,
    sortOrder: cards.value.length + 1
  }
  cards.value.push(newCard)
  selectedCardId.value = newCard._uid
  const idx = availableCards.value.findIndex(c => c.id === card.id)
  if (idx > -1) availableCards.value.splice(idx, 1)
  autoExpandCanvas()
}

function findOpenPosition() {
  let x = 40, y = 40
  const spacing = 20
  while (cards.value.some(c => Math.abs(c.positionX - x) < 50 && Math.abs(c.positionY - y) < 50)) {
    x += 420 + spacing
    if (x + 400 > canvasWidth.value - 100) { x = 40; y += 320 + spacing }
  }
  return { x: snapValue(x), y: snapValue(y) }
}

function handleCardSelectionChange(selection) { selectedTableCards.value = selection }

function handleConfirmAddCards() {
  selectedTableCards.value.forEach(card => handleAddExistingCard(card))
  showCardDialog.value = false
  selectedTableCards.value = []
}

function appendReportMessage(content, role = 'system') {
  reportMessages.value.push({ role, content })
  nextTick(() => {
    const el = reportMessagesRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function handleGenerateReportStream() {
  const prompt = reportPrompt.value?.trim()
  if (!prompt) {
    proxy.$modal.msgWarning('请输入大屏描述')
    return
  }
  const id = dashboardId.value
  if (!id) {
    proxy.$modal.msgWarning('请先保存看板后再使用生成报表')
    return
  }
  reportGenerating.value = true
  reportMessages.value.push({ role: 'user', content: prompt })
  appendReportMessage('正在连接并开始生成…')
  try {
    await generateReportStream(
      { prompt, dashboardId: id },
      {
        onMessage(msg) {
          appendReportMessage(msg)
        },
        onCard(payload) {
          const raw = payload.card || {}
          const layoutItem = payload.layoutItem || {}
          const baseOrder = cards.value.length
          const newCard = {
            _uid: genUid(),
            dashboardCardId: raw.dashboardCardId ?? null,
            cardId: raw.cardId ?? null,
            componentType: raw.componentType || 'chart',
            cardName: raw.cardName || '报表',
            chartType: raw.chartType || 'table',
            chartConfig: raw.chartConfig ?? null,
            positionX: raw.positionX ?? layoutItem.x ?? 40,
            positionY: raw.positionY ?? layoutItem.y ?? 40,
            width: raw.width ?? layoutItem.w ?? 480,
            height: raw.height ?? layoutItem.h ?? 320,
            sortOrder: baseOrder + 1,
            datasourceConfig: raw.componentType === 'datasource' ? (raw.datasourceConfig || null) : undefined
          }
          cards.value.push(newCard)
          autoExpandCanvas()
        },
        onDone() {
          appendReportMessage('全部生成完成，可拖拽调整或保存看板。')
          proxy.$modal.msgSuccess('报表生成完成')
        },
        onError(e) {
          const msg = e?.message || e?.msg || '报表生成失败，请重试或简化描述'
          appendReportMessage('错误：' + msg)
          proxy.$modal.msgError(msg)
        }
      }
    )
  } catch (e) {
    const msg = e?.message || e?.msg || '连接失败或超时'
    appendReportMessage('错误：' + msg)
    proxy.$modal.msgError(msg)
  } finally {
    reportGenerating.value = false
  }
}

// ==================== 拖拽移动 ====================
function handleCardMouseDown(card, event) {
  if (isPreview.value || event.button !== 0) return
  draggingCardId.value = card._uid
  selectedCardId.value = card._uid
  const rect = event.currentTarget.getBoundingClientRect()
  dragOffsetX.value = (event.clientX - rect.left) / zoom.value
  dragOffsetY.value = (event.clientY - rect.top) / zoom.value
  document.addEventListener('mousemove', handleDragMove)
  document.addEventListener('mouseup', handleDragEnd)
  event.preventDefault()
}

function handleDragMove(event) {
  if (!draggingCardId.value) return
  const card = cards.value.find(c => c._uid === draggingCardId.value)
  if (!card) return
  const canvasRect = canvasSurfaceRef.value?.getBoundingClientRect()
  if (!canvasRect) return
  let x = (event.clientX - canvasRect.left) / zoom.value - dragOffsetX.value
  let y = (event.clientY - canvasRect.top) / zoom.value - dragOffsetY.value
  x = Math.max(0, Math.min(x, canvasWidth.value - card.width))
  y = Math.max(0, Math.min(y, canvasHeight.value - card.height))
  card.positionX = snapValue(x)
  card.positionY = snapValue(y)
}

function handleDragEnd() {
  draggingCardId.value = null
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  autoExpandCanvas()
}

// ==================== 调整大小 ====================
function handleResizeStart(card, direction, event) {
  if (isPreview.value) return
  resizingCardId.value = card._uid
  resizeDirection.value = direction
  resizeStartRect.value = { x: card.positionX, y: card.positionY, w: card.width, h: card.height }
  resizeStartMouse.value = { x: event.clientX, y: event.clientY }
  document.addEventListener('mousemove', handleResizeMove)
  document.addEventListener('mouseup', handleResizeEnd)
  event.preventDefault()
}

function handleResizeMove(event) {
  if (!resizingCardId.value) return
  const card = cards.value.find(c => c._uid === resizingCardId.value)
  if (!card) return
  const dx = (event.clientX - resizeStartMouse.value.x) / zoom.value
  const dy = (event.clientY - resizeStartMouse.value.y) / zoom.value
  const r = resizeStartRect.value
  const dir = resizeDirection.value

  let newX = r.x, newY = r.y, newW = r.w, newH = r.h

  if (dir.includes('e')) newW = Math.max(MIN_CARD_WIDTH, r.w + dx)
  if (dir.includes('w')) { newW = Math.max(MIN_CARD_WIDTH, r.w - dx); newX = r.x + r.w - newW }
  if (dir.includes('s')) newH = Math.max(MIN_CARD_HEIGHT, r.h + dy)
  if (dir.includes('n')) { newH = Math.max(MIN_CARD_HEIGHT, r.h - dy); newY = r.y + r.h - newH }

  card.positionX = snapValue(Math.max(0, newX))
  card.positionY = snapValue(Math.max(0, newY))
  card.width = snapValue(newW)
  card.height = snapValue(newH)
}

function handleResizeEnd() {
  resizingCardId.value = null
  resizeDirection.value = null
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
  autoExpandCanvas()
}

// ==================== 其他交互 ====================
function selectCard(uid) { if (!isPreview.value) selectedCardId.value = uid }

function handleRootClick(e) {
  if (e.target.closest('.design-card') || e.target.closest('.panel-right') || e.target.closest('.panel-left')) return
  selectedCardId.value = null
}

function handleCanvasMouseDown(event) {
  if (event.target.closest('.design-card')) return
  selectedCardId.value = null
}

function handleKeyDown(event) {
  if (event.key === 'Delete' || event.key === 'Backspace') {
    if (selectedCard.value && !event.target.closest('input') && !event.target.closest('textarea')) {
      handleRemoveCard(selectedCard.value)
    }
  }
  if (event.key === 'Escape') {
    if (isPreview.value) {
      exitPreview()
      return
    }
    selectedCardId.value = null
  }
}

function handleRemoveCard(card) {
  const idx = cards.value.findIndex(c => c._uid === card._uid)
  if (idx > -1) {
    cards.value.splice(idx, 1)
    if (selectedCardId.value === card._uid) selectedCardId.value = null
  }
}

function handleCardDblClick(card) {
  if (card.componentType === 'decoration') {
    // 选中并展开属性面板
    selectedCardId.value = card._uid
  }
}

function handleComponentDragStart(event, comp) {
  event.dataTransfer.setData('text/plain', comp.type)
  event.dataTransfer.effectAllowed = 'copy'
}

function handleChartTypeDragStart(event, ct) {
  event.dataTransfer.setData('text/plain', 'chartType:' + ct.chartType)
  event.dataTransfer.effectAllowed = 'copy'
}

function handleCanvasDrop(event) {
  const type = event.dataTransfer.getData('text/plain')

  if (type.startsWith('chartType:')) {
    const chartType = type.replace('chartType:', '')
    const canvasRect = canvasSurfaceRef.value?.getBoundingClientRect()
    let posX = 40, posY = 40
    if (canvasRect) {
      posX = (event.clientX - canvasRect.left) / zoom.value - 80
      posY = (event.clientY - canvasRect.top) / zoom.value - 60
    }
    const defaultW = 4 * gridSize.value * 5
    const defaultH = 3 * gridSize.value * 5
    const newCard = {
      _uid: genUid(),
      dashboardCardId: null,
      cardId: null,
      componentType: 'datasource',
      cardName: getChartTypeName(chartType),
      positionX: snapValue(Math.max(0, posX)),
      positionY: snapValue(Math.max(0, posY)),
      width: defaultW,
      height: defaultH,
      sortOrder: cards.value.length + 1,
      datasourceConfig: {
        datasourceId: null,
        queryType: 'SQL',
        sqlTemplate: '',
        apiUrl: '',
        apiMethod: 'GET',
        apiHeaders: '',
        apiBody: '',
        responseDataPath: '',
        chartType,
        columnMapping: JSON.stringify({ xAxis: '', yAxis: [], category: '' })
      }
    }
    cards.value.push(newCard)
    selectedCardId.value = newCard._uid
    autoExpandCanvas()
    return
  }

  if (type === 'chart') {
    loadAvailableCards()
    showCardDialog.value = true
  } else if (type === 'background') {
    showBackgroundDrawer.value = true
  }
}

function handleBack() {
  if (window.opener) {
    window.close()
  } else {
    router.back()
  }
}

function exitPreview() {
  if (window.opener) {
    window.close()
  } else {
    router.back()
  }
}

function handlePreview() {
  const id = dashboardId.value
  if (!id) return
  const resolved = router.resolve({ path: '/dashboard/designer', query: { id, mode: 'preview' } })
  window.open(resolved.href, '_blank')
}

function startEditName() {
  editingName.value = true
  nextTick(() => nameInputRef.value?.focus())
}
function finishEditName() { editingName.value = false }

function handleDecorationStyleUpdate(newStyle) {
  if (!selectedCard.value) return
  selectedCard.value.styleConfig = JSON.stringify(newStyle)
}

function handleDatasourceConfigRefresh() {
  if (!selectedCard.value || selectedCard.value.componentType !== 'datasource') return
  selectedCard.value._refreshKey = (selectedCard.value._refreshKey || 0) + 1
}

function emitUpdate() { /* triggers reactivity, all state is local until save */ }

function getChartTypeName(t) {
  const map = { bar: '柱状图', line: '折线图', pie: '饼图', groupedBar: '分组柱状图', kpi: '指标卡', table: '表格' }
  return map[t] || t || '图表'
}

function getCardTypeLabel(card) {
  if (card.componentType === 'decoration') return '装饰'
  if (card.componentType === 'datasource') return '数据源'
  if (card.componentType === 'group') return '组合'
  return getChartTypeName(card.chartType)
}

// 滚轮缩放
function handleWheel(event) {
  if (event.shiftKey) {
    event.preventDefault()
    const delta = event.deltaY > 0 ? -0.05 : 0.05
    zoom.value = Math.max(0.2, Math.min(3, zoom.value + delta))
  }
}

onMounted(async () => {
  if (isPreview.value) {
    zoom.value = 1
    panX.value = 0
    panY.value = 0
    showGrid.value = false
  }
  await loadDashboard()
  await loadBackgroundConfig()
  if (!isPreview.value) {
    await loadAvailableCards()
    canvasAreaRef.value?.addEventListener('wheel', handleWheel, { passive: false })
  }
})

onBeforeUnmount(() => {
  canvasAreaRef.value?.removeEventListener('wheel', handleWheel)
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
})
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,300;0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700;1,9..40,400&family=JetBrains+Mono:wght@300;400;500&display=swap');
</style>

<style scoped lang="scss">
@use 'sass:color';
/* ========== 设计变量 ========== */
$bg-darkest: #0c0d12;
$bg-dark: #12131a;
$bg-medium: #181922;
$bg-light: #1f202c;
$bg-lighter: #282938;
$border-color: rgba(255, 255, 255, 0.07);
$border-active: rgba(255, 255, 255, 0.14);
$text-primary: #f0f1f5;
$text-secondary: #a8aabe;
$text-muted: #6e7088;
$accent: #7c6cf0;
$accent-glow: rgba(124, 108, 240, 0.35);
$accent-light: #b0a8ff;
$accent-cyan: #22d3ee;
$accent-cyan-glow: rgba(34, 211, 238, 0.25);
$accent-pink: #ec4899;
$accent-pink-glow: rgba(236, 72, 153, 0.2);
$success: #34d9a8;
$danger: #ff6b6b;
$danger-bg: rgba(255, 107, 107, 0.1);

$font-display: 'DM Sans', system-ui, -apple-system, sans-serif;
$font-mono: 'JetBrains Mono', 'Fira Code', monospace;

$header-height: 54px;
$footer-height: 30px;
$panel-width: 272px;
$right-panel-width: 292px;

$radius-sm: 6px;
$radius-md: 8px;
$radius-lg: 12px;

/* ========== 全局 ========== */
* {
  box-sizing: border-box;
}

.designer-root {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: $bg-darkest;
  color: $text-primary;
  font-family: $font-display;
  font-size: 14px;
  line-height: 1.5;
  overflow: hidden;
  outline: none;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;

  ::selection {
    background: $accent;
    color: white;
  }
}

/* ========== 顶部工具栏 ========== */
.designer-header {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: $bg-dark;
  border-bottom: 1px solid $border-color;
  flex-shrink: 0;
  gap: 16px;
  z-index: 100;
}

.header-left, .header-center, .header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-left {
  flex: 1;
  min-width: 0;
}
.header-center {
  flex-shrink: 0;
}
.header-right {
  flex: 1;
  justify-content: flex-end;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: $radius-sm;
  border: none;
  background: transparent;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: $bg-lighter;
    color: $text-primary;
  }
}

.header-divider {
  width: 1px;
  height: 24px;
  background: $border-color;
}

.dashboard-name-wrap {
  min-width: 0;
}

.dashboard-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  letter-spacing: -0.01em;

  .edit-icon {
    opacity: 0;
    transition: opacity 0.2s;
    color: $text-muted;
  }
  &:hover .edit-icon {
    opacity: 1;
  }
}

.name-input {
  background: $bg-lighter;
  border: 1px solid $accent;
  border-radius: $radius-sm;
  color: $text-primary;
  font-family: $font-display;
  font-size: 16px;
  font-weight: 600;
  padding: 5px 10px;
  outline: none;
  width: 260px;
}

.zoom-controls {
  display: flex;
  align-items: center;
  gap: 4px;
  background: $bg-medium;
  border-radius: $radius-md;
  padding: 2px 4px;
}

.zoom-label {
  font-family: $font-mono;
  font-size: 12px;
  color: $text-secondary;
  min-width: 44px;
  text-align: center;
  user-select: none;
}

.tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: $radius-sm;
  border: none;
  background: transparent;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: $bg-lighter;
    color: $text-primary;
  }
  &.active {
    background: rgba($accent, 0.15);
    color: $accent-light;
  }
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 16px;
  height: 34px;
  border-radius: $radius-sm;
  border: none;
  font-family: $font-display;
  font-size: 13.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &.primary {
    background: $accent;
    color: white;
    box-shadow: 0 2px 12px $accent-glow;

    &:hover:not(:disabled) {
      background: color.adjust($accent, $lightness: 5%);
      box-shadow: 0 4px 20px $accent-glow;
    }
  }

  &.secondary {
    background: $bg-lighter;
    color: $text-primary;
    border: 1px solid $border-active;

    &:hover:not(:disabled) {
      background: color.adjust($bg-lighter, $lightness: 5%);
    }
  }

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;

  &.small {
    width: 12px;
    height: 12px;
    border-width: 1.5px;
    border-color: $text-muted;
    border-top-color: $text-secondary;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ========== 主体布局 ========== */
.designer-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* ========== 左侧面板 ========== */
.panel-left {
  width: $panel-width;
  background: $bg-dark;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  position: relative;
  flex-shrink: 0;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  z-index: 50;

  &.collapsed {
    width: 0;
    border-right: none;
  }
}

.panel-toggle {
  position: absolute;
  right: -28px;
  top: 50%;
  transform: translateY(-50%);
  width: 28px;
  height: 48px;
  background: $bg-dark;
  border: 1px solid $border-color;
  border-left: none;
  border-radius: 0 $radius-sm $radius-sm 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: $text-muted;
  z-index: 60;
  transition: color 0.2s;

  &:hover {
    color: $text-primary;
  }

  svg.flipped {
    transform: rotate(180deg);
  }
}

.panel-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: $bg-lighter;
    border-radius: 2px;
  }
}

.panel-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-title {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: $text-secondary;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.refresh-btn {
  background: none;
  border: none;
  color: $text-muted;
  cursor: pointer;
  padding: 2px;
  border-radius: 4px;
  display: flex;
  transition: all 0.2s;

  &:hover {
    color: $accent-light;
    background: rgba($accent, 0.1);
  }
}

.report-open-drawer-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  font-size: 12px;
  font-weight: 500;
  color: $accent-light;
  background: rgba($accent, 0.12);
  border: 1px solid rgba($accent, 0.35);
  border-radius: $radius-sm;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;

  &:hover {
    background: rgba($accent, 0.2);
    border-color: $accent;
  }
}

.report-generate-drawer {
  .report-drawer-body {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 320px;
  }

  .report-messages {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
    background: $bg-medium;
    border-radius: $radius-sm;
    margin-bottom: 12px;
    min-height: 200px;
  }

  .report-msg {
    margin-bottom: 8px;
    font-size: 12px;
    line-height: 1.5;

    &.user {
      color: $accent-light;
    }
    &.system {
      color: $text-secondary;
    }
  }

  .report-msg-content {
    word-break: break-word;
  }

  .report-drawer-footer {
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .report-prompt-input {
    width: 100%;
    padding: 8px 10px;
    font-size: 12px;
    line-height: 1.4;
    color: $text-primary;
    background: $bg-medium;
    border: 1px solid $border-color;
    border-radius: $radius-sm;
    resize: vertical;
    min-height: 56px;
    transition: border-color 0.2s, box-shadow 0.2s;

    &::placeholder {
      color: $text-muted;
    }
    &:focus {
      outline: none;
      border-color: $accent;
      box-shadow: 0 0 0 2px rgba($accent, 0.15);
    }
    &:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  }

  .report-generate-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 8px 14px;
    font-size: 12px;
    font-weight: 500;
    color: #fff;
    background: $accent;
    border: none;
    border-radius: $radius-sm;
    cursor: pointer;
    transition: background-color 0.2s, opacity 0.2s;

    &:hover:not(:disabled) {
      background: $accent-light;
    }
    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

.component-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.component-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 14px 8px;
  background: $bg-medium;
  border: 1px solid $border-color;
  border-radius: $radius-md;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;

  &:hover {
    border-color: $accent;
    background: rgba($accent, 0.06);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  }

  &:active {
    transform: translateY(0);
  }
}

.comp-icon {
  color: $text-secondary;
  display: flex;
  transition: color 0.2s;

  .component-card:hover & {
    color: $accent-light;
  }
}

.comp-label {
  font-size: 12px;
  color: $text-secondary;
  text-align: center;
  line-height: 1.3;
  font-weight: 500;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 300px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 3px;
  }
  &::-webkit-scrollbar-thumb {
    background: $bg-lighter;
    border-radius: 2px;
  }
}

.available-card-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: $bg-medium;
  border: 1px solid $border-color;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: $accent;
    background: rgba($accent, 0.06);
  }
}

.card-type-badge {
  font-family: $font-mono;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
  background: rgba($accent, 0.15);
  color: $accent-light;
  white-space: nowrap;
  flex-shrink: 0;
}

.card-item-name {
  font-size: 13px;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  color: $text-secondary;
  font-size: 13px;
}

.empty-hint {
  padding: 14px;
  color: $text-secondary;
  font-size: 13px;
  text-align: center;
}

/* ========== 画布工作区（深色基底 + 创意点缀） ========== */
.canvas-area {
  flex: 1;
  overflow: auto;
  background: #08090e;
  position: relative;
  cursor: default;

  /* 多层渐变光晕：紫、青、粉点缀，营造醒目创意感 */
  background-image:
    radial-gradient(ellipse 80% 50% at 15% 20%, rgba($accent, 0.1) 0%, transparent 55%),
    radial-gradient(ellipse 60% 40% at 85% 75%, rgba($accent-cyan, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse 50% 35% at 75% 15%, rgba($accent-pink, 0.06) 0%, transparent 45%),
    radial-gradient(circle at 50% 50%, rgba($accent, 0.03) 0%, transparent 70%);

  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  &::-webkit-scrollbar-thumb {
    background: $bg-lighter;
    border-radius: 4px;
  }
  &::-webkit-scrollbar-corner {
    background: #08090e;
  }
}

.canvas-viewport {
  position: relative;
  will-change: transform;
}

.canvas-surface {
  position: relative;
  background: linear-gradient(165deg, #0e0f14 0%, #13141c 45%, #111218 100%);
  border-radius: 8px;
  margin: 40px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.04),
    0 0 0 1px rgba($accent, 0.08) inset,
    0 20px 60px rgba(0, 0, 0, 0.5),
    0 0 80px rgba($accent, 0.04),
    0 0 140px rgba($accent-cyan, 0.02);
  overflow: hidden;

  /* 边角创意点缀：左上紫、右下青，醒目光斑 */
  &::before,
  &::after {
    content: '';
    position: absolute;
    width: 380px;
    height: 380px;
    border-radius: 50%;
    pointer-events: none;
    z-index: 0;
    filter: blur(72px);
    opacity: 0.7;
  }
  &::before {
    top: -140px;
    left: -140px;
    background: radial-gradient(circle, rgba($accent, 0.18) 0%, transparent 65%);
  }
  &::after {
    bottom: -140px;
    right: -140px;
    background: radial-gradient(circle, rgba($accent-cyan, 0.15) 0%, transparent 65%);
  }
}

.grid-overlay {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 0;
}

/* ========== 设计卡片 ========== */
.design-card {
  position: absolute;
  background: transparent;
  border: 1px solid transparent;
  border-radius: $radius-md;
  cursor: move;
  user-select: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: box-shadow 0.25s, border-color 0.25s, background 0.25s;

  &:hover:not(.selected) {
    border-color: rgba(255, 255, 255, 0.08);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);
  }

  &.selected {
    background: #1e1f2e;
    border-color: $accent;
    box-shadow:
      0 0 0 1px $accent,
      0 0 20px $accent-glow,
      0 8px 32px rgba(0, 0, 0, 0.4);
  }

  &.dragging {
    opacity: 0.85;
    cursor: grabbing;
    z-index: 1000 !important;
  }
}

.design-card-header {
  display: none;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
  flex-shrink: 0;

  .design-card.selected & {
    display: flex;
  }
}

.design-card-title {
  font-size: 13px;
  font-weight: 500;
  color: $text-primary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.design-card-type {
  font-family: $font-mono;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.08);
  color: $text-secondary;
  flex-shrink: 0;
}

.design-card-body {
  flex: 1;
  overflow: hidden;
  min-height: 0;
  position: relative;

  :deep(.card-renderer) {
    background: transparent;
    position: absolute;
    inset: 0;
  }
  :deep(.card-header) {
    display: none;
  }
  :deep(.card-content) {
    padding: 10px;
    height: 100%;
  }
  :deep(.echarts-wrapper) {
    min-height: 100px;
  }
}

/* ========== 调整大小控制点 ========== */
.resize-handle {
  position: absolute;
  z-index: 10;

  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    background: $accent;
    box-shadow: 0 0 6px $accent-glow;
    transition: transform 0.15s;
  }

  &:hover::after {
    transform: scale(1.3);
  }

  &.nw, &.ne, &.sw, &.se {
    width: 16px;
    height: 16px;

    &::after {
      width: 8px;
      height: 8px;
      top: 4px;
      left: 4px;
    }
  }

  &.n, &.s {
    left: 16px;
    right: 16px;
    height: 10px;
    cursor: ns-resize;

    &::after {
      width: 24px;
      height: 4px;
      border-radius: 2px;
      left: 50%;
      top: 3px;
      transform: translateX(-50%);
    }
  }

  &.w, &.e {
    top: 16px;
    bottom: 16px;
    width: 10px;
    cursor: ew-resize;

    &::after {
      height: 24px;
      width: 4px;
      border-radius: 2px;
      top: 50%;
      left: 3px;
      transform: translateY(-50%);
    }
  }

  &.nw { top: -4px; left: -4px; cursor: nwse-resize; }
  &.ne { top: -4px; right: -4px; cursor: nesw-resize; }
  &.sw { bottom: -4px; left: -4px; cursor: nesw-resize; }
  &.se { bottom: -4px; right: -4px; cursor: nwse-resize; }
  &.n { top: -5px; }
  &.s { bottom: -5px; }
  &.w { left: -5px; }
  &.e { right: -5px; }
}

/* ========== 画布空状态 ========== */
.canvas-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  color: $text-secondary;
  pointer-events: none;
  z-index: 1;

  svg {
    color: $accent;
    opacity: 0.4;
  }
  p {
    margin: 0;
    font-size: 15px;
    letter-spacing: 0.02em;
  }
}

/* ========== 右侧属性面板 ========== */
.panel-right {
  width: 0;
  background: $bg-dark;
  border-left: 1px solid $border-color;
  flex-shrink: 0;
  overflow: hidden;
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  z-index: 50;

  &.visible {
    width: $right-panel-width;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid $border-color;

  h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: -0.01em;
  }
}

.close-panel-btn {
  background: none;
  border: none;
  color: $text-muted;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  transition: all 0.2s;

  &:hover {
    color: $text-primary;
    background: $bg-lighter;
  }
}

.props-content {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: $bg-lighter;
    border-radius: 2px;
  }
}

.prop-group {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &.half {
    flex: 1;
    min-width: 0;
  }
}

.prop-row {
  display: flex;
  gap: 8px;
}

.prop-label {
  font-size: 12px;
  font-weight: 500;
  color: $text-secondary;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.prop-input {
  background: $bg-medium;
  border: 1px solid $border-color;
  border-radius: $radius-sm;
  color: $text-primary;
  font-family: $font-mono;
  font-size: 13px;
  padding: 7px 10px;
  outline: none;
  width: 100%;
  transition: border-color 0.2s;

  &:focus {
    border-color: $accent;
    box-shadow: 0 0 0 2px $accent-glow;
  }

  &[type="number"] {
    -moz-appearance: textfield;
    &::-webkit-inner-spin-button {
      opacity: 0.5;
    }
  }
}

.prop-readonly {
  font-family: $font-mono;
  font-size: 13px;
  color: $text-secondary;
  padding: 6px 0;
}

.prop-section-title {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: $text-secondary;
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid $border-color;
}

.prop-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.danger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  background: $danger-bg;
  border: 1px solid rgba($danger, 0.2);
  border-radius: $radius-sm;
  color: $danger;
  font-family: $font-display;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba($danger, 0.2);
    border-color: rgba($danger, 0.4);
  }
}

/* ========== 底部状态栏 ========== */
.designer-footer {
  height: $footer-height;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: $bg-dark;
  border-top: 1px solid $border-color;
  font-size: 12px;
  color: $text-secondary;
  gap: 16px;
  flex-shrink: 0;
  z-index: 100;
}

.status-spacer {
  flex: 1;
}

.status-item.hint {
  font-size: 11px;
  opacity: 0.7;
}

/* ========== 动画 ========== */
.design-card {
  animation: cardAppear 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* ========== 暗色主题下 Element Plus 覆盖 ========== */
:deep(.el-dialog) {
  --el-dialog-bg-color: #{$bg-medium};
  --el-dialog-title-font-size: 16px;
  --el-dialog-border-radius: 12px;
  --el-text-color-primary: #{$text-primary};
  --el-text-color-regular: #{$text-secondary};

  .el-dialog__header {
    border-bottom: 1px solid #{$border-color};
    padding: 18px 22px;
  }
  .el-dialog__title {
    color: #{$text-primary};
    font-family: #{$font-display};
    font-weight: 600;
  }
  .el-dialog__body {
    padding: 22px;
    color: #{$text-primary};
    font-size: 14px;
  }
  .el-dialog__footer {
    border-top: 1px solid #{$border-color};
    padding: 14px 22px;
  }
}

:deep(.el-drawer) {
  --el-drawer-bg-color: #{$bg-dark};
  --el-text-color-primary: #{$text-primary};
  --el-text-color-regular: #{$text-secondary};
}

:deep(.el-table) {
  --el-table-bg-color: #{$bg-medium};
  --el-table-tr-bg-color: #{$bg-medium};
  --el-table-header-bg-color: #{$bg-light};
  --el-table-row-hover-bg-color: #{$bg-lighter};
  --el-table-border-color: #{$border-color};
  --el-table-text-color: #{$text-primary};
  --el-table-header-text-color: #{$text-secondary};
  font-size: 13px;
}

:deep(.el-form-item__label) {
  color: #{$text-secondary};
  font-size: 13px;
}

:deep(.el-input__inner) {
  color: #{$text-primary};
}

:deep(.el-radio__label),
:deep(.el-checkbox__label) {
  color: #{$text-primary};
  font-size: 13px;
}

:deep(.el-steps) {
  .el-step__title {
    font-size: 13px;
    color: #{$text-secondary};

    &.is-finish,
    &.is-process {
      color: #{$text-primary};
    }
  }
}

:deep(.el-descriptions) {
  --el-text-color-primary: #{$text-primary};
  --el-text-color-regular: #{$text-secondary};
}

:deep(.el-button) {
  font-size: 13px;
}

:deep(.el-tag) {
  font-size: 12px;
}

:deep(.el-select-dropdown__item) {
  font-size: 13px;
}

/* ========== 暗色主题下 CardRenderer 覆盖 ========== */
.design-card-body {
  :deep(.card-renderer) {
    background: transparent;
    color: #{$text-primary};
  }
  :deep(.card-content) {
    background: transparent;
  }
  :deep(.chart-placeholder),
  :deep(.card-loading) {
    color: #{$text-muted};
  }
  :deep(.kpi-card) {
    background: rgba(255, 255, 255, 0.04);
    border-color: rgba(255, 255, 255, 0.06);
  }
  :deep(.kpi-label) {
    color: #{$text-secondary};
  }
  :deep(.kpi-value) {
    color: #{$accent-light};
  }
  :deep(.chart-table th) {
    background: rgba(255, 255, 255, 0.04);
    color: #{$text-secondary};
    border-color: rgba(255, 255, 255, 0.06);
  }
  :deep(.chart-table td) {
    color: #{$text-primary};
    border-color: rgba(255, 255, 255, 0.04);
  }
}

/* ========== 微交互动效 ========== */
.panel-left,
.panel-right {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1), border-color 0.2s;
}

.component-card {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);

  &:active {
    transform: scale(0.97);
  }
}

.available-card-item {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 选中卡片闪光效果 */
.design-card.selected::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  background: linear-gradient(135deg, rgba($accent, 0.1), transparent 60%);
  pointer-events: none;
  z-index: -1;
}

/* 工具按钮涟漪效果 */
.tool-btn,
.action-btn {
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at var(--x, 50%) var(--y, 50%), rgba(255,255,255,0.1) 0%, transparent 60%);
    opacity: 0;
    transition: opacity 0.3s;
  }

  &:hover::after {
    opacity: 1;
  }
}

/* ========== 预览模式 ========== */
.designer-root.preview-mode {
  .designer-header {
    display: none;
  }

  .panel-left,
  .panel-left .panel-toggle {
    display: none;
  }

  .panel-right {
    display: none;
  }

  .designer-footer {
    display: none;
  }

  .canvas-area {
    background: $bg-darkest;
  }

  .canvas-surface {
    margin: 0;
    border-radius: 0;
    box-shadow: none;
    min-height: 100vh;
  }

  .grid-overlay {
    display: none;
  }

  .design-card {
    cursor: default;
    border-color: transparent;

    &:hover {
      box-shadow: none;
      border-color: transparent;
    }
  }

  .design-card-header {
    display: none;
  }

  .design-card-body {
    :deep(.card-renderer) {
      top: 0;
    }
  }
}

/* 退出预览按钮 */
.preview-exit-btn {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: $radius-md;
  background: rgba($bg-dark, 0.7);
  backdrop-filter: blur(12px);
  color: $text-secondary;
  font-family: $font-display;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  opacity: 0.4;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    opacity: 1;
    background: rgba($bg-dark, 0.9);
    color: $text-primary;
    border-color: rgba(255, 255, 255, 0.25);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

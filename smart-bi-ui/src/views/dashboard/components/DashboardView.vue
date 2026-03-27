<template>
  <div ref="dashboardRoot" class="dashboard-view" :class="{ 'export-mode': exportCapturing }">
    <!-- 看板头部 -->
    <div class="dashboard-header">
      <div class="header-left">
        <h2>{{ dashboard.name }}</h2>
        <div class="dashboard-meta">
          <el-tag v-if="dashboard.refreshInterval > 0" type="info" size="small">
            每{{ dashboard.refreshInterval }}分钟刷新
          </el-tag>
          <el-tag v-if="dashboard.isPublic" type="success" size="small">
            公开
          </el-tag>
        </div>
      </div>
      <div class="header-right">
        <el-button 
          type="success" 
          size="small"
          @click="handleShare"
        >
          <el-icon><Share /></el-icon>
          分享
        </el-button>
        <el-dropdown @command="handleExportCommand">
          <el-button type="primary" size="small">
            <el-icon><Download /></el-icon>
            导出
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="exportDashboardPdf">导出看板为PDF</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button 
          v-if="showRefresh"
          type="primary" 
          size="small"
          @click="handleRefresh"
          :loading="refreshing"
        >
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button 
          v-if="showEdit"
          type="primary" 
          plain
          size="small"
          @click="handleEdit"
        >
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
      </div>
    </div>
    
    <!-- 卡片容器 -->
    <div 
      ref="cardsContainer"
      class="cards-container"
      :style="[dashboardBackgroundStyle, exportCapturing ? { minHeight: exportContainerHeight + 'px', height: 'auto' } : {}]"
      v-loading="loading"
    >
      <!-- 虚拟滚动或分页加载（导出时渲染全部以便截图） -->
      <div 
        v-if="useVirtualScroll && !exportCapturing"
        class="virtual-scroll-container"
        :style="{ height: containerHeight + 'px' }"
        @scroll="handleScroll"
      >
        <div 
          class="virtual-scroll-content"
          :style="{ height: totalHeight + 'px' }"
        >
          <div
            v-for="(card, index) in visibleCards"
            :key="card.dashboardCardId || card.cardId || index"
            class="card-item"
            :style="{
              position: 'absolute',
              left: card.positionX + 'px',
              top: card.positionY + 'px',
              width: card.width + 'px',
              height: card.height + 'px'
            }"
          >
            <CardRenderer :card="card" />
          </div>
        </div>
      </div>
      
      <!-- 普通渲染（卡片数量较少时，或导出模式） -->
      <div 
        v-else
        class="normal-container"
        :style="exportCapturing ? { minHeight: exportContainerHeight + 'px' } : {}"
      >
        <div
          v-for="(card, index) in cards"
          :key="card.dashboardCardId || card.cardId || index"
          class="card-item"
          :style="{
            position: 'absolute',
            left: card.positionX + 'px',
            top: card.positionY + 'px',
            width: card.width + 'px',
            height: card.height + 'px'
          }"
        >
          <CardRenderer :card="card" />
        </div>
      </div>
      
      <!-- 空状态 -->
      <EmptyState 
        v-if="!loading && cards.length === 0"
        description="看板暂无卡片"
        :show-action="false"
      />
      
      <!-- 加载更多提示 -->
      <div 
        v-if="useVirtualScroll && hasMore"
        class="load-more"
        v-loading="loadingMore"
      >
        <el-button 
          type="text"
          @click="loadMoreCards"
        >
          加载更多卡片
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup name="DashboardView">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCurrentInstance } from 'vue'
import { getDashboard, getDashboardCards, refreshDashboard } from '@/api/dashboard'
import EmptyState from './EmptyState.vue'
import CardRenderer from './CardRenderer.vue'
import { Refresh, Edit, Download, ArrowDown, Share } from '@element-plus/icons-vue'
import { generateShareLink } from '@/api/share'
import request from '@/utils/request'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()

const props = defineProps({
  dashboardId: {
    type: [Number, String],
    default: null
  },
  showRefresh: {
    type: Boolean,
    default: true
  },
  showEdit: {
    type: Boolean,
    default: true
  }
})

const dashboard = ref({})
const cards = ref([])
const loading = ref(false)
const refreshing = ref(false)
const loadingMore = ref(false)

const dashboardRoot = ref(null)
const cardsContainer = ref(null)
const containerHeight = ref(800)
const scrollTop = ref(0)
const exportCapturing = ref(false)

// 大数据量处理：超过50个卡片时使用虚拟滚动或分页
const CARD_THRESHOLD = 50
const useVirtualScroll = computed(() => cards.value.length > CARD_THRESHOLD)

// 虚拟滚动相关
const visibleRange = ref({ start: 0, end: 20 })
const cardHeight = ref(300) // 平均卡片高度
const cardSpacing = ref(20)

const visibleCards = computed(() => {
  if (!useVirtualScroll.value) {
    return cards.value
  }
  
  // 计算可见区域的卡片
  const viewportTop = scrollTop.value
  const viewportBottom = scrollTop.value + containerHeight.value
  
  return cards.value.filter(card => {
    const cardTop = card.positionY
    const cardBottom = card.positionY + card.height
    return cardBottom >= viewportTop && cardTop <= viewportBottom
  })
})

const totalHeight = computed(() => {
  if (cards.value.length === 0) return 0
  const maxY = Math.max(...cards.value.map(c => c.positionY + c.height))
  return maxY + cardSpacing.value
})

// 导出模式下容器高度（容纳所有卡片）
const exportContainerHeight = computed(() => Math.max(totalHeight.value, 600))

// 看板背景样式：从 backgroundConfig 解析
const dashboardBackgroundStyle = computed(() => {
  const cfg = dashboard.value?.backgroundConfig
  if (!cfg) return {}
  let config
  try {
    config = typeof cfg === 'string' ? JSON.parse(cfg) : cfg
  } catch {
    return {}
  }
  if (!config || !config.type) return {}
  const type = config.type
  if (type === 'solid') {
    return { backgroundColor: config.color || '#fafafa' }
  }
  if (type === 'gradient') {
    const dir = config.direction || 'to bottom'
    const colors = config.colors || []
    if (colors.length === 0) return {}
    const colorStops = colors.join(', ')
    return { background: `linear-gradient(${dir}, ${colorStops})` }
  }
  if (type === 'image') {
    let url = config.url
    if (!url) return {}
    if (!url.startsWith('http')) {
      const base = request.defaults?.baseURL || ''
      url = base.replace(/\/$/, '') + (url.startsWith('/') ? url : '/' + url)
    }
    const size = config.fit === 'contain' ? 'contain' : (config.fit === 'cover' ? 'cover' : 'cover')
    return {
      backgroundImage: `url(${url})`,
      backgroundSize: size,
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }
  }
  return {}
})

const hasMore = computed(() => {
  return visibleCards.value.length < cards.value.length
})

// 加载看板数据
async function loadDashboard() {
  const id = props.dashboardId || route.query.id
  if (!id) {
    proxy.$modal.msgError('看板ID不能为空')
    return
  }
  
  loading.value = true
  try {
    const response = await getDashboard(id)
    if (response.code === 200) {
      const data = response.data.dashboard || response.data
      dashboard.value = data
      
      // 加载卡片列表
      await loadCards(id)
    } else {
      proxy.$modal.msgError(response.msg || '加载看板数据失败')
    }
  } catch (error) {
    proxy.$modal.msgError('加载看板数据失败: ' + (error.msg || error.message))
  } finally {
    loading.value = false
  }
}

// 加载卡片列表
async function loadCards(dashboardId) {
  try {
    const response = await getDashboardCards(dashboardId)
    if (response.code === 200) {
      cards.value = response.data || []
      
      // 如果卡片数量很多，初始化虚拟滚动
      if (useVirtualScroll.value) {
        await nextTick()
        updateContainerHeight()
      }
    }
  } catch (error) {
    proxy.$modal.msgError('加载卡片列表失败: ' + (error.msg || error.message))
  }
}

// 更新容器高度
function updateContainerHeight() {
  if (cardsContainer.value) {
    containerHeight.value = cardsContainer.value.clientHeight || 800
  }
}

// 滚动处理（虚拟滚动）
function handleScroll(event) {
  scrollTop.value = event.target.scrollTop
}

// 加载更多卡片（分页模式）
async function loadMoreCards() {
  if (loadingMore.value) return
  
  loadingMore.value = true
  try {
    // 这里可以实现分页加载逻辑
    // 当前实现是一次性加载所有卡片，使用虚拟滚动优化渲染
    await new Promise(resolve => setTimeout(resolve, 500))
  } finally {
    loadingMore.value = false
  }
}

// 刷新看板
async function handleRefresh() {
  const id = props.dashboardId || route.query.id
  if (!id) {
    return
  }
  
  refreshing.value = true
  try {
    const response = await refreshDashboard(id)
    if (response.code === 200) {
      const data = response.data
      if (data.successCount !== undefined) {
        proxy.$modal.msgSuccess(data.message || '看板刷新成功')
      } else {
        proxy.$modal.msgSuccess('看板刷新成功')
      }
      // 重新加载卡片数据
      await loadCards(id)
    } else {
      proxy.$modal.msgError(response.msg || '刷新失败')
    }
  } catch (error) {
    proxy.$modal.msgError('刷新失败: ' + (error.msg || error.message))
  } finally {
    refreshing.value = false
  }
}

// 自动刷新逻辑
const autoRefreshTimer = ref(null)

// 启动自动刷新
function startAutoRefresh() {
  const id = props.dashboardId || route.query.id
  if (!id || !dashboard.value || !dashboard.value.refreshInterval || dashboard.value.refreshInterval <= 0) {
    return
  }
  
  // 清除已有定时器
  stopAutoRefresh()
  
  // 设置新的定时器（转换为毫秒）
  const intervalMs = dashboard.value.refreshInterval * 60 * 1000
  autoRefreshTimer.value = setInterval(() => {
    handleRefresh()
  }, intervalMs)
  
  console.log(`看板自动刷新已启动，刷新间隔: ${dashboard.value.refreshInterval}分钟`)
}

// 停止自动刷新
function stopAutoRefresh() {
  if (autoRefreshTimer.value) {
    clearInterval(autoRefreshTimer.value)
    autoRefreshTimer.value = null
  }
}

function handleEdit() {
  const id = props.dashboardId || route.query.id
  const resolved = router.resolve({ path: '/dashboard/designer', query: { id } })
  window.open(resolved.href, '_blank')
}

// 分享看板
async function handleShare() {
  const id = props.dashboardId || route.query.id
  if (!id) {
    return
  }
  
  try {
    // 显示分享对话框
    proxy.$prompt('请输入分享密码（可选）和过期天数（默认7天）', '生成分享链接', {
      confirmButtonText: '生成',
      cancelButtonText: '取消',
      inputPlaceholder: '密码（可选）',
      inputPattern: /^.{0,20}$/,
      inputErrorMessage: '密码长度不能超过20个字符'
    }).then(async ({ value: password }) => {
      const response = await generateShareLink({
        resourceType: 'DASHBOARD',
        resourceId: id,
        password: password || null,
        expireDays: 7
      })
      
      if (response.code === 200) {
        const shareUrl = response.data.shareUrl
        // 复制到剪贴板
        await navigator.clipboard.writeText(shareUrl)
        proxy.$modal.msgSuccess('分享链接已生成并复制到剪贴板: ' + shareUrl)
      } else {
        proxy.$modal.msgError(response.msg || '生成分享链接失败')
      }
    }).catch(() => {
      // 用户取消
    })
  } catch (error) {
    proxy.$modal.msgError('生成分享链接失败: ' + (error.msg || error.message))
  }
}

// 导出命令处理（前端 html2canvas + jspdf，与页面展示效果一致）
async function handleExportCommand(command) {
  if (command !== 'exportDashboardPdf') return
  const id = props.dashboardId || route.query.id
  if (!id || !dashboardRoot.value) return

  const loading = proxy.$loading({ text: '正在生成PDF...', background: 'rgba(0,0,0,0.6)' })
  try {
    exportCapturing.value = true
    await nextTick()
    await nextTick() // 等待 DOM 更新
    await new Promise(r => setTimeout(r, 300)) // 等待 ECharts 等异步图表渲染完成

    const el = dashboardRoot.value
    const canvas = await html2canvas(el, {
      useCORS: true,
      allowTaint: true,
      scale: 2,
      logging: false,
      backgroundColor: '#fafafa'
    })

    const imgData = canvas.toDataURL('image/png', 1.0)
    const pdf = new jsPDF({
      orientation: canvas.width > canvas.height ? 'landscape' : 'portrait',
      unit: 'pt',
      format: 'a4'
    })
    const a4W = pdf.internal.pageSize.getWidth()
    const a4H = pdf.internal.pageSize.getHeight()
    const scale = Math.min(a4W / canvas.width, a4H / canvas.height)
    const w = canvas.width * scale
    const h = canvas.height * scale
    pdf.addImage(imgData, 'PNG', 0, 0, w, h)
    pdf.save(`看板_${dashboard.value?.name || id}_${new Date().toISOString().slice(0, 10)}.pdf`)

    proxy.$modal.msgSuccess('看板导出成功')
  } catch (error) {
    console.error('导出PDF失败:', error)
    proxy.$modal.msgError('导出失败: ' + (error.message || '未知错误'))
  } finally {
    exportCapturing.value = false
    loading.close()
  }
}

// 窗口大小变化处理
function handleResize() {
  updateContainerHeight()
}

// 监听看板数据变化，启动自动刷新
watch(() => dashboard.value, (newDashboard) => {
  if (newDashboard && newDashboard.id) {
    stopAutoRefresh()
    startAutoRefresh()
  }
}, { deep: true })

// 组件挂载
onMounted(() => {
  loadDashboard().then(() => {
    // 加载完成后启动自动刷新
    if (dashboard.value && dashboard.value.refreshInterval > 0) {
      startAutoRefresh()
    }
  })
  window.addEventListener('resize', handleResize)
})

// 组件卸载
onBeforeUnmount(() => {
  stopAutoRefresh()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-view {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.header-left h2 {
  margin: 0 0 10px 0;
  font-size: 20px;
  font-weight: 500;
}

.dashboard-meta {
  display: flex;
  gap: 10px;
}

.header-right {
  display: flex;
  gap: 10px;
}

.cards-container {
  flex: 1;
  position: relative;
  overflow: auto;
  background: #fafafa;
  min-height: 600px;
}

/* 导出模式：显示全部内容以便截图 */
.dashboard-view.export-mode .cards-container {
  overflow: visible;
}

.virtual-scroll-container {
  position: relative;
  overflow: auto;
}

.virtual-scroll-content {
  position: relative;
}

.normal-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.card-item {
  position: absolute;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.load-more {
  text-align: center;
  padding: 20px;
}
</style>

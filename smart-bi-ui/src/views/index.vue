<template>
  <div class="home-page">
    <div class="home-page__grid" aria-hidden="true" />
    <div class="home-page__inner">
      <!-- Hero：對齊設計稿 Central Search -->
      <section class="home-hero">
        <div class="home-hero__glow" aria-hidden="true" />
        <h2 class="home-hero__title">
          欢迎回来，<span class="home-hero__accent">{{ displayName }}</span>
        </h2>
        <p class="home-hero__lead">
          基于第五代智能问数引擎，将复杂数据模式转化为可执行洞察。
        </p>
        <div class="home-hero__glass glass-panel">
          <div class="home-hero__search-row">
            <el-icon class="home-hero__spark" :size="26"><MagicStick /></el-icon>
            <el-input
              v-model="questionInput"
              class="home-hero__input"
              placeholder="向 AI 提问：例如上月各区域销售额是多少？"
              clearable
              @keyup.enter="goToQuery"
            />
            <el-button type="primary" class="home-hero__cta" @click="goToQuery">
              <span class="home-hero__cta-text">生成</span>
              <el-icon :size="16"><Lightning /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="home-hero__tags">
          <button
            v-for="(item, i) in quickQuestions.slice(0, 3)"
            :key="item.question"
            type="button"
            class="home-hero__tag"
            :class="`home-hero__tag--${i}`"
            @click="goToQuery(item.question)"
          >
            <span class="home-hero__tag-dot" />
            <span class="home-hero__tag-text">{{ item.question }}</span>
          </button>
        </div>
      </section>

      <!-- 数据概览：對齊 My Dashboard 三張玻璃卡（使用真實統計） -->
      <section class="home-section">
        <div class="home-section__head">
          <div>
            <h3 class="home-section__title">数据概览</h3>
            <p class="home-section__sub">实时指标快照</p>
          </div>
        </div>
        <div class="home-metrics">
          <div class="home-metric glass-panel">
            <div class="home-metric__icon">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <p class="home-metric__label">数据源</p>
            <h4 class="home-metric__value home-metric__value--cyan">
              {{ stats.datasourceCount }}
            </h4>
            <div class="home-metric__bar">
              <div
                class="home-metric__bar-fill home-metric__bar-fill--cyan"
                :style="{ width: overviewBarWidth(stats.datasourceCount) }"
              />
            </div>
            <p class="home-metric__foot">
              <span class="home-metric__up">已接入</span> 数据源实例
            </p>
          </div>
          <div class="home-metric glass-panel">
            <div class="home-metric__icon home-metric__icon--purple">
              <el-icon :size="28"><Histogram /></el-icon>
            </div>
            <p class="home-metric__label">元数据表</p>
            <h4 class="home-metric__value">{{ stats.tableCount }}</h4>
            <div class="home-metric__spark">
              <span v-for="n in 7" :key="n" class="home-metric__spark-bar" :style="sparkStyle(n)" />
            </div>
            <p class="home-metric__foot">资产目录中的表数量</p>
          </div>
          <div class="home-metric glass-panel">
            <div class="home-metric__icon home-metric__icon--amber">
              <el-icon :size="28"><CircleCheck /></el-icon>
            </div>
            <p class="home-metric__label">质量规则</p>
            <h4 class="home-metric__value">{{ qualityHeadline }}</h4>
            <div class="home-metric__status">
              <el-icon class="home-metric__ok" :size="18"><CircleCheck /></el-icon>
              <span>规则引擎就绪</span>
            </div>
            <p class="home-metric__foot home-metric__foot--muted">持续守护数据质量</p>
          </div>
        </div>
      </section>

      <!-- 我的看板 -->
      <section class="home-section">
        <div class="home-section__head">
          <div>
            <h3 class="home-section__title">我的看板</h3>
            <p class="home-section__sub">实时分析与可视化</p>
          </div>
          <router-link
            v-if="dashboardList.length > 0"
            to="/bi/dashboard"
            class="home-section__manage"
          >
            管理看板
            <el-icon :size="16"><ArrowRight /></el-icon>
          </router-link>
        </div>
        <div v-loading="dashboardLoading" class="home-dashboards">
          <template v-if="dashboardList.length > 0">
            <div
              v-for="d in dashboardList"
              :key="d.id"
              class="home-dash-card glass-panel"
              @click="openDashboard(d, 'preview')"
            >
              <div class="home-dash-card__icon">
                <el-icon :size="22"><DataBoard /></el-icon>
              </div>
              <p class="home-dash-card__label">Dashboard</p>
              <h4 class="home-dash-card__name">{{ d.name }}</h4>
              <p class="home-dash-card__meta">
                {{ d.refreshInterval ? `${d.refreshInterval} 分钟刷新` : '不自动刷新' }}
              </p>
              <div class="home-dash-card__foot">
                <span class="home-dash-card__tier">VIEW</span>
                <el-icon class="home-dash-card__chev"><ArrowRight /></el-icon>
              </div>
              <div class="home-dash-card__actions" @click.stop>
                <el-button link type="primary" size="small" @click="openDashboard(d, 'preview')">
                  查看
                </el-button>
                <el-button link size="small" @click="openDashboard(d, 'edit')">设计</el-button>
              </div>
            </div>
          </template>
          <div v-else-if="!dashboardLoading" class="home-empty glass-panel">
            <p>暂无看板</p>
            <router-link to="/bi/dashboard">去创建看板</router-link>
          </div>
        </div>
      </section>

      <!-- 数据资产：四格入口 -->
      <section class="home-section">
        <div class="home-section__head">
          <div>
            <h3 class="home-section__title">数据资产</h3>
            <p class="home-section__sub">主要信息仓库与入口</p>
          </div>
          <div class="home-section__tools">
            <button type="button" class="home-tool-btn" aria-label="筛选">
              <el-icon :size="18"><Filter /></el-icon>
            </button>
            <button type="button" class="home-tool-btn" aria-label="排序">
              <el-icon :size="18"><Sort /></el-icon>
            </button>
          </div>
        </div>
        <div class="home-assets">
          <div
            v-for="asset in assetEntries"
            :key="asset.title"
            class="home-asset"
            :class="asset.mod"
            @click="goTo(asset.path)"
          >
            <div class="home-asset__icon">
              <el-icon :size="28"><component :is="asset.icon" /></el-icon>
            </div>
            <h5 class="home-asset__title">{{ asset.title }}</h5>
            <p class="home-asset__desc">{{ asset.desc }}</p>
            <div class="home-asset__foot">
              <span class="home-asset__tag">{{ asset.tag }}</span>
              <el-icon class="home-asset__chev"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </section>

      <!-- Footer / Status -->
      <footer class="home-footer">
        <div class="home-footer__stats">
          <div class="home-footer__stat">
            <p class="home-footer__stat-label">系统可用</p>
            <p class="home-footer__stat-value">99.9%</p>
          </div>
          <div class="home-footer__stat">
            <p class="home-footer__stat-label">服务延迟</p>
            <p class="home-footer__stat-value">&lt;50ms</p>
          </div>
          <div class="home-footer__stat">
            <p class="home-footer__stat-label">智能问数</p>
            <p class="home-footer__stat-value">就绪</p>
          </div>
        </div>
        <p class="home-footer__copy">{{ footerContent }}</p>
      </footer>
    </div>
  </div>
</template>

<script setup name="Index">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Grid,
  DocumentChecked,
  MagicStick,
  Lightning,
  TrendCharts,
  Histogram,
  CircleCheck,
  ArrowRight,
  DataBoard,
  Filter,
  Sort,
  Coin,
  Cpu
} from '@element-plus/icons-vue'
import { listDashboard } from '@/api/dashboard'
import { listDataSource } from '@/api/datasource'
import { listTableMetadata } from '@/api/metadata'
import { listQualityRule } from '@/api/quality'
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const router = useRouter()
const userStore = useUserStore()
const footerContent = defaultSettings.footerContent

const displayName = computed(() => {
  const n = userStore.nickName || userStore.name
  return n || '用户'
})

const questionInput = ref('')
const quickQuestions = [
  { question: '上月各区域销售额是多少？' },
  { question: '近12个月销售趋势' },
  { question: '销售额排名前10的客户' },
  { question: '各渠道用户数量对比' }
]

function goToQuery(question) {
  const q = (question || questionInput.value || '').trim()
  if (q) {
    router.push({ path: '/bi/query', query: { q } })
  } else {
    router.push('/bi/query')
  }
}

const dashboardList = ref([])
const dashboardLoading = ref(true)

function loadDashboards() {
  dashboardLoading.value = true
  listDashboard({ pageNum: 1, pageSize: 12 })
    .then((res) => {
      dashboardList.value = res.rows || []
    })
    .finally(() => {
      dashboardLoading.value = false
    })
}

function openDashboard(d, mode) {
  const route = router.resolve({
    path: '/dashboard/designer',
    query: { id: d.id, ...(mode === 'preview' ? { mode: 'preview' } : {}) }
  })
  window.open(route.href, '_blank')
}

const stats = ref({
  datasourceCount: 0,
  tableCount: 0,
  qualityRuleCount: 0
})

const qualityHeadline = computed(() =>
  stats.value.qualityRuleCount > 0 ? String(stats.value.qualityRuleCount) : '—'
)

function overviewBarWidth(n) {
  const pct = Math.min(100, 8 + n * 12)
  return `${pct}%`
}

function sparkStyle(n) {
  const heights = [12, 20, 32, 24, 16, 28, 14]
  const h = heights[(n - 1) % heights.length]
  return { height: `${h}px` }
}

function loadStats() {
  Promise.all([
    listDataSource({ pageNum: 1, pageSize: 1 }).then((res) => res.total ?? 0),
    listTableMetadata({ pageNum: 1, pageSize: 1 }).then((res) => res.total ?? 0),
    listQualityRule({ pageNum: 1, pageSize: 1 }).then((res) => res.total ?? 0)
  ])
    .then(([ds, tbl, rule]) => {
      stats.value = {
        datasourceCount: ds,
        tableCount: tbl,
        qualityRuleCount: rule
      }
    })
    .catch(() => {})
}

function goTo(path) {
  router.push(path)
}

const assetEntries = [
  {
    title: '数据源',
    desc: '连接与配置',
    tag: '核心',
    path: '/bi/datasource',
    icon: Coin,
    mod: 'home-asset--cyan'
  },
  {
    title: '元数据',
    desc: '表与字段目录',
    tag: '目录',
    path: '/bi/metadata',
    icon: Grid,
    mod: 'home-asset--purple'
  },
  {
    title: '质量规则',
    desc: '校验与报告',
    tag: '治理',
    path: '/quality/rule',
    icon: DocumentChecked,
    mod: 'home-asset--cyan'
  },
  {
    title: '智能问数',
    desc: '自然语言查询',
    tag: '入口',
    path: '/bi/query',
    icon: Cpu,
    mod: 'home-asset--amber'
  }
]

onMounted(() => {
  loadDashboards()
  loadStats()
})
</script>

<style scoped lang="scss">
@import url("https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@600;700&family=Inter:wght@400;500;600&display=swap");

$lowest: #0a0e17;
$surface: #0f131c;
$on-surface: #dfe2ef;
$on-variant: #b9cacb;
$outline: #849495;
$primary-container: #00f2ff;
$on-primary-container: #006a71;
$secondary-container: #7000ff;
$tertiary-container: #fed83a;
$surface-container: #1c1f29;
$surface-container-low: #181b25;

.home-page {
  position: relative;
  min-height: 100%;
  background: $lowest;
  color: $on-surface;
  font-family: "Inter", system-ui, sans-serif;
  padding: 0;
}

.home-page__grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: radial-gradient(circle, #3a494b 1px, transparent 1px);
  background-size: 30px 30px;
  opacity: 0.05;
}

.home-page__inner {
  position: relative;
  z-index: 1;
  max-width: 80rem;
  margin: 0 auto;
  width: 100%;
  padding: 2.5rem;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.glass-panel {
  background: rgba(49, 53, 63, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

/* Hero */
.home-hero {
  position: relative;
  padding: 3rem 0 1rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.home-hero__glow {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at center,
    rgba(112, 0, 255, 0.08) 0%,
    transparent 70%
  );
  pointer-events: none;
}

.home-hero__title {
  position: relative;
  margin: 0 0 1rem;
  font-family: "Space Grotesk", "Inter", sans-serif;
  font-size: 3rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: $on-surface;
}

@media (max-width: 768px) {
  .home-hero__title {
    font-size: 2rem;
  }
}

.home-hero__accent {
  color: $primary-container;
}

.home-hero__lead {
  position: relative;
  margin: 0 0 2.5rem;
  max-width: 42rem;
  font-size: 1.125rem;
  line-height: 1.6;
  color: $on-variant;
}

.home-hero__glass {
  position: relative;
  width: 100%;
  max-width: 48rem;
  padding: 0.5rem;
  border-radius: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.home-hero__search-row {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.75rem;
  background: rgba($surface-container-low, 0.5);
  border-radius: 0.5rem;
  transition: box-shadow 0.2s;

  &:focus-within {
    box-shadow: 0 0 0 2px rgba($primary-container, 0.2);
  }
}

.home-hero__spark {
  flex-shrink: 0;
  color: $secondary-container;
  padding: 0 0.5rem;
}

.home-hero__input {
  flex: 1;
  min-width: 0;

  :deep(.el-input__wrapper) {
    background: transparent;
    border: none;
    box-shadow: none;
    padding: 0 0.5rem;
  }

  :deep(.el-input__inner) {
    font-size: 1.125rem;
    color: $on-surface;

    &::placeholder {
      color: rgba($on-variant, 0.45);
    }
  }
}

.home-hero__cta {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1.5rem !important;
  border-radius: 0.125rem !important;
  font-weight: 700;
  letter-spacing: 0.06em;
  background: $secondary-container !important;
  border: none !important;

  &:hover {
    filter: brightness(1.1);
  }
}

.home-hero__cta-text {
  font-size: 0.75rem;
}

.home-hero__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 1.5rem;
}

.home-hero__tag {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  background: transparent;
  color: inherit;
  font-family: inherit;
  transition: background 0.2s, border-color 0.2s;
  max-width: min(100%, 20rem);
}

.home-hero__tag-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-hero__tag--0 {
  border-color: rgba($secondary-container, 0.2);
  background: rgba($secondary-container, 0.1);
  color: #c4b5fd;

  &:hover {
    background: rgba($secondary-container, 0.2);
  }
}

.home-hero__tag--1 {
  border-color: rgba($primary-container, 0.2);
  background: rgba($primary-container, 0.1);
  color: $primary-container;

  &:hover {
    background: rgba($primary-container, 0.2);
  }
}

.home-hero__tag--2 {
  border-color: rgba($tertiary-container, 0.25);
  background: rgba($tertiary-container, 0.1);
  color: #fde68a;

  &:hover {
    background: rgba($tertiary-container, 0.2);
  }
}

.home-hero__tag-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.home-hero__tag--0 .home-hero__tag-dot {
  animation: home-pulse 2s ease-in-out infinite;
}

@keyframes home-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
}

/* Section */
.home-section__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 2rem;
  gap: 1rem;
  flex-wrap: wrap;
}

.home-section__title {
  margin: 0;
  font-family: "Space Grotesk", "Inter", sans-serif;
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: $on-surface;
}

.home-section__sub {
  margin: 0.25rem 0 0;
  font-size: 0.875rem;
  color: $on-variant;
}

.home-section__manage {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.875rem;
  font-weight: 700;
  color: $primary-container;
  text-decoration: none;
  transition: gap 0.2s;

  &:hover {
    gap: 0.5rem;
  }
}

.home-section__tools {
  display: flex;
  gap: 0.5rem;
}

.home-tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  border-radius: 0.125rem;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(49, 53, 63, 0.4);
  color: $on-surface;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
  }
}

/* Metrics */
.home-metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
}

@media (max-width: 900px) {
  .home-metrics {
    grid-template-columns: 1fr;
  }
}

.home-metric {
  position: relative;
  padding: 1.5rem;
  border-radius: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

.home-metric__icon {
  position: absolute;
  top: 0;
  right: 0;
  padding: 1rem;
  opacity: 0.2;
  color: $primary-container;
  transition: opacity 0.2s;
}

.home-metric:hover .home-metric__icon {
  opacity: 1;
}

.home-metric__icon--purple {
  color: $secondary-container;
}

.home-metric__icon--amber {
  color: $tertiary-container;
}

.home-metric__label {
  margin: 0 0 0.5rem;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: $on-variant;
}

.home-metric__value {
  margin: 0 0 1rem;
  font-family: "Space Grotesk", "Inter", sans-serif;
  font-size: 2.25rem;
  font-weight: 700;
  color: $on-surface;
}

.home-metric__value--cyan {
  color: $primary-container;
}

.home-metric__bar {
  height: 8px;
  width: 100%;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.05);
  overflow: hidden;
}

.home-metric__bar-fill {
  height: 100%;
  border-radius: 9999px;
}

.home-metric__bar-fill--cyan {
  background: $primary-container;
}

.home-metric__foot {
  margin: 1rem 0 0;
  font-size: 0.75rem;
  color: $on-variant;
}

.home-metric__up {
  color: #4ade80;
  font-weight: 700;
}

.home-metric__spark {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 2rem;
}

.home-metric__spark-bar {
  width: 4px;
  border-radius: 1px;
  background: rgba($secondary-container, 0.45);
}

.home-metric__spark-bar:nth-child(3) {
  background: $secondary-container;
  opacity: 1;
}

.home-metric__status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: $on-variant;
}

.home-metric__ok {
  color: #4ade80;
}

.home-metric__foot--muted {
  font-style: italic;
}

/* Dashboards */
.home-dashboards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  min-height: 120px;
}

@media (max-width: 1024px) {
  .home-dashboards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .home-dashboards {
    grid-template-columns: 1fr;
  }
}

.home-dash-card {
  position: relative;
  padding: 1.5rem;
  border-radius: 0.5rem;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: rgba($primary-container, 0.35);
    box-shadow: 0 0 24px rgba(0, 242, 255, 0.08);

    .home-dash-card__chev {
      transform: translateX(4px);
    }
  }
}

.home-dash-card__icon {
  position: absolute;
  top: 0;
  right: 0;
  padding: 1rem;
  opacity: 0.25;
  color: $primary-container;
}

.home-dash-card__label {
  margin: 0 0 0.25rem;
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: $on-variant;
}

.home-dash-card__name {
  margin: 0 0 0.75rem;
  font-family: "Space Grotesk", "Inter", sans-serif;
  font-size: 1.25rem;
  font-weight: 700;
  color: $on-surface;
  line-height: 1.3;
  word-break: break-word;
}

.home-dash-card__meta {
  margin: 0 0 1rem;
  font-size: 0.75rem;
  color: $on-variant;
}

.home-dash-card__foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.home-dash-card__tier {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  padding: 0.125rem 0.5rem;
  border-radius: 0.125rem;
  background: rgba(255, 255, 255, 0.05);
  color: $on-variant;
}

.home-dash-card__chev {
  color: $on-variant;
  transition: transform 0.2s;
}

.home-dash-card__actions {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.home-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: 3rem 1.5rem;
  border-radius: 0.5rem;

  p {
    margin: 0 0 0.75rem;
    color: $on-variant;
  }

  a {
    color: $primary-container;
    font-weight: 600;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

/* Assets */
.home-assets {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1rem;
}

@media (max-width: 1200px) {
  .home-assets {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 560px) {
  .home-assets {
    grid-template-columns: 1fr;
  }
}

.home-asset {
  padding: 1.25rem;
  border-radius: 0.125rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: $surface-container-low;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;

  &:hover {
    border-color: rgba(0, 242, 255, 0.35);

    .home-asset__chev {
      transform: translateX(4px);
    }
  }
}

.home-asset--purple:hover {
  border-color: rgba(167, 139, 250, 0.4);
}

.home-asset--amber:hover {
  border-color: rgba($tertiary-container, 0.35);
}

.home-asset__icon {
  width: 3rem;
  height: 3rem;
  border-radius: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
  background: $surface-container;
  color: #22d3ee;
  transition: background 0.2s;
}

.home-asset--purple .home-asset__icon {
  color: #c4b5fd;
}

.home-asset--amber .home-asset__icon {
  color: $tertiary-container;
}

.home-asset:hover .home-asset__icon {
  background: rgba(0, 242, 255, 0.1);
}

.home-asset--purple:hover .home-asset__icon {
  background: rgba(167, 139, 250, 0.12);
}

.home-asset--amber:hover .home-asset__icon {
  background: rgba($tertiary-container, 0.12);
}

.home-asset__title {
  margin: 0 0 0.25rem;
  font-size: 1rem;
  font-weight: 700;
  color: $on-surface;
}

.home-asset__desc {
  margin: 0 0 1rem;
  font-size: 0.75rem;
  color: $on-variant;
}

.home-asset__foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.home-asset__tag {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  padding: 0.125rem 0.5rem;
  border-radius: 0.125rem;
  background: rgba(255, 255, 255, 0.05);
  color: $on-variant;
}

.home-asset__chev {
  font-size: 0.875rem;
  color: $on-variant;
  transition: transform 0.2s;
}

/* Footer */
.home-footer {
  padding: 4rem 0 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.home-footer__stats {
  display: flex;
  gap: 2.5rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
  justify-content: center;
}

.home-footer__stat {
  text-align: center;
}

.home-footer__stat-label {
  margin: 0 0 0.25rem;
  font-size: 10px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: $on-variant;
}

.home-footer__stat-value {
  margin: 0;
  font-family: "Space Grotesk", "Inter", sans-serif;
  font-size: 1.125rem;
  font-weight: 700;
  color: $on-surface;
}

.home-footer__copy {
  margin: 0;
  font-size: 0.75rem;
  color: rgba($on-variant, 0.45);
}
</style>

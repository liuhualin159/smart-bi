<template>
  <div class="navbar" :class="[navbarClass, 'nav' + settingsStore.navType]">
    <!-- Aether 顶栏：设计稿 — 品牌 + 全局搜索 + 快捷链 + 工具 -->
    <template v-if="settingsStore.navType === 1">
      <hamburger
        id="hamburger-container"
        :is-active="appStore.sidebar.opened"
        class="hamburger-container"
        @toggleClick="toggleSideBar"
      />
      <div class="navbar-aether-left">
        <span class="navbar-brand-title">{{ appTitle }}</span>
        <div class="navbar-search-slot" @click="openMenuSearch">
          <el-input
            class="navbar-search-input"
            readonly
            placeholder="Query the neural network..."
            :prefix-icon="Search"
          />
        </div>
      </div>
      <header-search ref="headerSearchRef" class="navbar-search-trigger" />

      <div class="navbar-quick-links">
        <router-link to="/index" class="navbar-quick-links__item">工作台</router-link>
        <router-link to="/bi/query" class="navbar-quick-links__item navbar-quick-links__item--muted">
          智能问数
        </router-link>
      </div>
      <div class="navbar-divider" />

      <div class="right-menu">
        <template v-if="appStore.device !== 'mobile'">
          <el-tooltip content="源码地址" effect="dark" placement="bottom">
            <ruo-yi-git id="ruoyi-git" class="right-menu-item hover-effect" />
          </el-tooltip>
          <el-tooltip content="文档地址" effect="dark" placement="bottom">
            <ruo-yi-doc id="ruoyi-doc" class="right-menu-item hover-effect" />
          </el-tooltip>
          <el-tooltip content="通知" effect="dark" placement="bottom">
            <div class="right-menu-item hover-effect" @click="onNavIconClick('notice')">
              <el-icon :size="20"><Bell /></el-icon>
            </div>
          </el-tooltip>
          <el-tooltip content="应用" effect="dark" placement="bottom">
            <div class="right-menu-item hover-effect" @click="onNavIconClick('grid')">
              <el-icon :size="20"><Grid /></el-icon>
            </div>
          </el-tooltip>
          <screenfull id="screenfull" class="right-menu-item hover-effect" />
          <el-tooltip content="布局大小" effect="dark" placement="bottom">
            <size-select id="size-select" class="right-menu-item hover-effect" />
          </el-tooltip>
        </template>

        <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="hover" @command="handleCommand">
          <div class="avatar-wrapper">
            <img :src="userStore.avatar" class="user-avatar" alt="" />
            <span v-if="appStore.device !== 'mobile'" class="user-nickname">{{ userStore.nickName }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <router-link to="/user/profile">
                <el-dropdown-item>个人中心</el-dropdown-item>
              </router-link>
              <el-dropdown-item v-if="settingsStore.showSettings" command="setLayout">
                <span>布局设置</span>
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>

    <!-- 其它导航模式（混合 / 纯顶栏） -->
    <template v-else>
      <hamburger
        id="hamburger-container"
        :is-active="appStore.sidebar.opened"
        class="hamburger-container"
        @toggleClick="toggleSideBar"
      />
      <top-nav v-if="settingsStore.navType == 2" id="topmenu-container" class="topmenu-container" />
      <template v-if="settingsStore.navType == 3">
        <logo v-show="settingsStore.sidebarLogo" :collapse="false" />
        <top-bar id="topbar-container" class="topbar-container" />
      </template>

      <div class="right-menu">
        <template v-if="appStore.device !== 'mobile'">
          <header-search id="header-search" class="right-menu-item" />
          <el-tooltip content="源码地址" effect="dark" placement="bottom">
            <ruo-yi-git id="ruoyi-git" class="right-menu-item hover-effect" />
          </el-tooltip>
          <el-tooltip content="文档地址" effect="dark" placement="bottom">
            <ruo-yi-doc id="ruoyi-doc" class="right-menu-item hover-effect" />
          </el-tooltip>
          <el-tooltip content="通知" effect="dark" placement="bottom">
            <div class="right-menu-item hover-effect" @click="onNavIconClick('notice')">
              <el-icon :size="20"><Bell /></el-icon>
            </div>
          </el-tooltip>
          <el-tooltip content="应用" effect="dark" placement="bottom">
            <div class="right-menu-item hover-effect" @click="onNavIconClick('grid')">
              <el-icon :size="20"><Grid /></el-icon>
            </div>
          </el-tooltip>
          <screenfull id="screenfull" class="right-menu-item hover-effect" />
          <el-tooltip content="布局大小" effect="dark" placement="bottom">
            <size-select id="size-select" class="right-menu-item hover-effect" />
          </el-tooltip>
        </template>

        <el-dropdown class="avatar-container right-menu-item hover-effect" trigger="hover" @command="handleCommand">
          <div class="avatar-wrapper">
            <img :src="userStore.avatar" class="user-avatar" alt="" />
            <span v-if="appStore.device !== 'mobile'" class="user-nickname">{{ userStore.nickName }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <router-link to="/user/profile">
                <el-dropdown-item>个人中心</el-dropdown-item>
              </router-link>
              <el-dropdown-item v-if="settingsStore.showSettings" command="setLayout">
                <span>布局设置</span>
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ElMessageBox, ElMessage } from 'element-plus'
import { Search, Bell, Grid } from '@element-plus/icons-vue'
import TopNav from '@/components/TopNav'
import TopBar from './TopBar'
import Logo from './Sidebar/Logo'
import Hamburger from '@/components/Hamburger'
import Screenfull from '@/components/Screenfull'
import SizeSelect from '@/components/SizeSelect'
import HeaderSearch from '@/components/HeaderSearch'
import RuoYiGit from '@/components/RuoYi/Git'
import RuoYiDoc from '@/components/RuoYi/Doc'
import useAppStore from '@/store/modules/app'
import useUserStore from '@/store/modules/user'
import useSettingsStore from '@/store/modules/settings'

const appStore = useAppStore()
const userStore = useUserStore()
const settingsStore = useSettingsStore()

const appTitle = import.meta.env.VITE_APP_TITLE
const headerSearchRef = ref(null)

const navbarClass = computed(() => (settingsStore.navType === 1 ? 'navbar--aether' : ''))

function openMenuSearch() {
  headerSearchRef.value?.open?.()
}

function toggleSideBar() {
  appStore.toggleSideBar()
}

function handleCommand(command) {
  switch (command) {
    case 'setLayout':
      setLayout()
      break
    case 'logout':
      logout()
      break
    default:
      break
  }
}

function logout() {
  ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logOut().then(() => {
      location.href = '/index'
    })
  }).catch(() => {})
}

const emits = defineEmits(['setLayout'])
function setLayout() {
  emits('setLayout')
}

function onNavIconClick(kind) {
  if (kind === 'notice') {
    ElMessage.info('通知中心开发中')
  } else {
    ElMessage.info('应用中心开发中')
  }
}
</script>

<style lang="scss" scoped>
.navbar.nav3 {
  .hamburger-container {
    display: none !important;
  }
}

.navbar {
  height: var(--navbar-height, 50px);
  overflow: hidden;
  position: relative;
  background: var(--navbar-bg);
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  box-sizing: border-box;

  .hamburger-container {
    line-height: 1;
    height: 100%;
    cursor: pointer;
    transition: background 0.3s;
    -webkit-tap-highlight-color: transparent;
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-right: 8px;
    padding-left: 8px;

    &:hover {
      background: rgba(0, 0, 0, 0.025);
    }
  }

  .breadcrumb-container {
    flex-shrink: 0;
  }

  .topmenu-container {
    position: absolute;
    left: 50px;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 8px;
  }

  .right-menu {
    height: 100%;
    line-height: var(--navbar-height, 50px);
    display: flex;
    align-items: center;
    margin-left: auto;
    flex-shrink: 0;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #5a5e66;
      vertical-align: middle;

      &.hover-effect {
        cursor: pointer;
        transition: background 0.3s;

        &:hover {
          background: rgba(0, 0, 0, 0.025);
        }
      }

    }

    .avatar-container {
      margin-right: 0;
      padding-right: 8px;

      .avatar-wrapper {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;

        .user-avatar {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          border: 1px solid rgba(34, 211, 238, 0.35);
        }

        .user-nickname {
          font-size: 13px;
          font-weight: 600;
          color: var(--navbar-text, #303133);
          max-width: 120px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
  }
}

/* Aether 顶栏：设计稿 home_screen_modernized — header px-6 h-16，品牌与搜索 gap-8，搜索 max-w-md py-1.5 pl-10 pr-4 */
.navbar.navbar--aether {
  padding: 0 1.5rem;
  gap: 0.75rem;

  .hamburger-container {
    margin-right: 0;
    padding-left: 0;
  }

  .hamburger-container:hover {
    background: rgba(255, 255, 255, 0.06);
  }

  .right-menu .right-menu-item {
    color: var(--aether-cyan-muted, #94a3b8);

    &:hover {
      color: rgb(103, 232, 249);
      background: rgba(255, 255, 255, 0.08);
    }
  }
}

.navbar-aether-left {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 2rem; /* gap-8 与稿一致 */
}

.navbar-brand-title {
  flex-shrink: 0;
  font-family: "Space Grotesk", "Inter", system-ui, sans-serif;
  font-size: 1.125rem; /* text-lg */
  font-weight: 900;
  letter-spacing: -0.05em; /* tracking-tighter */
  color: var(--aether-cyan, #22d3ee);
}

/* 稿：max-w-md w-full */
.navbar-search-slot {
  flex: 1 1 auto;
  min-width: 0;
  width: 100%;
  max-width: 28rem; /* max-w-md = 448px */
  cursor: text;
}

.navbar-search-input {
  width: 100%;

  :deep(.el-input__wrapper) {
    --el-input-inner-height: 32px;
    min-height: 32px;
    padding: 0 1rem 0 2.5rem; /* pr-4 pl-10 */
    background: rgba(255, 255, 255, 0.05) !important;
    border: none;
    border-radius: 2px; /* rounded-sm */
    box-shadow: none !important;
    color: var(--navbar-text, #dfe2ef);
  }

  :deep(.el-input__inner) {
    cursor: pointer;
    height: 32px;
    line-height: 32px;
    min-height: 32px;
    font-size: 0.875rem; /* text-sm */
    font-weight: 500;
    color: var(--navbar-text, #dfe2ef);

    &::placeholder {
      color: rgba(148, 163, 184, 0.5); /* placeholder:text-on-surface-variant/50 */
    }
  }

  :deep(.el-input__prefix) {
    color: var(--aether-cyan-muted, #94a3b8);
  }

  :deep(.el-input__prefix .el-icon) {
    font-size: 1.125rem; /* text-lg 与稿搜索图标 */
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px rgba(6, 182, 212, 0.35) !important; /* ring-cyan-500/30 */
  }
}

.navbar-search-trigger {
  position: absolute;
  width: 0;
  height: 0;
  overflow: hidden;
  opacity: 0;
  pointer-events: none;
}

.navbar-quick-links {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  flex-shrink: 0;
}

.navbar-quick-links__item {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--aether-cyan, #22d3ee);
  text-decoration: none;
  &:hover {
    filter: brightness(1.1);
  }
}

.navbar-quick-links__item--muted {
  font-weight: 500;
  color: var(--aether-cyan-muted, #94a3b8);
  &:hover {
    color: rgb(103, 232, 249);
  }
}

.navbar-divider {
  display: block;
  width: 1px;
  height: 1rem;
  background: rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

html.dark .navbar.navbar--aether .navbar-quick-links__item.router-link-active {
  color: var(--aether-cyan);
}
</style>

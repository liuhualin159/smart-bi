<template>
  <div :class="{ 'has-logo': showLogo }" class="sidebar-container aether-sidebar">
    <!-- 与稿一致：Logo + 按钮同处 px-6 容器内，按钮 w-full 不贴侧栏左右边 -->
    <div v-if="showLogo && !isCollapse" class="aether-sidebar-head">
      <logo :collapse="isCollapse" />
      <router-link class="aether-cta" to="/bi/query">
        <el-icon :size="16"><Plus /></el-icon>
        <span>新建分析</span>
      </router-link>
    </div>
    <logo v-else-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="getMenuBackground"
        :text-color="getMenuTextColor"
        :unique-opened="true"
        :active-text-color="activeTextColor"
        :collapse-transition="false"
        mode="vertical"
        :class="sideTheme"
      >
        <sidebar-item
          v-for="(route, index) in sidebarRouters"
          :key="route.path + index"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
    <div v-if="showLogo && !isCollapse" class="aether-sidebar-foot">
      <router-link class="aether-sidebar-foot__link" to="/system/user">
        <el-icon :size="20"><Setting /></el-icon>
        <span>用户管理</span>
      </router-link>
      <router-link class="aether-user" to="/user/profile">
        <img :src="userStore.avatar" class="aether-user__avatar" alt="" />
        <div class="aether-user__meta">
          <p class="aether-user__name">{{ userStore.nickName || userStore.name }}</p>
          <p class="aether-user__role">{{ userRoleLabel }}</p>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import Logo from './Logo'
import SidebarItem from './SidebarItem'
import variables from '@/assets/styles/variables.module.scss'
import useAppStore from '@/store/modules/app'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'
import useUserStore from '@/store/modules/user'
import { Plus, Setting } from '@element-plus/icons-vue'

const route = useRoute()
const appStore = useAppStore()
const settingsStore = useSettingsStore()
const permissionStore = usePermissionStore()
const userStore = useUserStore()

const sidebarRouters = computed(() => permissionStore.sidebarRouters)
const showLogo = computed(() => settingsStore.sidebarLogo)
const sideTheme = computed(() => settingsStore.sideTheme)
const theme = computed(() => settingsStore.theme)
const isCollapse = computed(() => !appStore.sidebar.opened)

const activeTextColor = computed(() =>
  settingsStore.isDark ? '#22d3ee' : theme.value
)

const userRoleLabel = computed(() => {
  const roles = userStore.roles
  if (roles && roles.length > 0) {
    return String(roles[0]).replace(/^ROLE_/, '')
  }
  return '用户'
})

const getMenuBackground = computed(() => {
  if (settingsStore.isDark) {
    return 'transparent'
  }
  return sideTheme.value === 'theme-dark' ? variables.menuBg : variables.menuLightBg
})

const getMenuTextColor = computed(() => {
  if (settingsStore.isDark) {
    return 'var(--sidebar-text)'
  }
  return sideTheme.value === 'theme-dark' ? variables.menuText : variables.menuLightText
})

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})
</script>

<style lang="scss" scoped>
/* 稿：aside 内第一层 div 为 px-6 mb-10，内含品牌区 mb-8 与 w-full 按钮 */
.aether-sidebar-head {
  box-sizing: border-box;
  width: 100%;
  padding: 0 1.5rem; /* px-6 */
  margin-bottom: 2.5rem; /* mb-10：与下方菜单区分 */
}

.aether-cta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  box-sizing: border-box;
  width: 100%;
  margin: 0;
  padding: 0.75rem 1rem; /* py-3 px-4 */
  border-radius: 2px;
  font-size: 0.875rem;
  font-weight: 700;
  text-decoration: none;
  color: var(--on-primary-container, #006a71) !important;
  background: var(--aether-primary, #00f2ff);
  box-shadow: 0 0 0 1px rgba(0, 242, 255, 0.2);
  transition: box-shadow 0.3s, filter 0.2s;

  &:hover {
    box-shadow: 0 0 15px rgba(0, 242, 255, 0.4);
    filter: brightness(1.05);
  }
}

.aether-sidebar-foot {
  flex-shrink: 0;
  margin-top: auto;
  padding: 1.5rem 1rem 0; /* pt-6 px-4 */
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.aether-sidebar-foot__link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-radius: 2px;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--sidebar-text);
  text-decoration: none;
  font-family: "Space Grotesk", "Inter", system-ui, sans-serif;
  letter-spacing: -0.01em;
  transition: background 0.2s, color 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.05);
    color: #e2e8f0;
  }
}

.aether-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0;
  padding: 1.5rem 1rem; /* py-6 px-4 与稿底部用户区一致 */
  text-decoration: none;
  color: inherit;
  border-radius: 4px;
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }
}

.aether-user__avatar {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(34, 211, 238, 0.35);
  flex-shrink: 0;
}

.aether-user__meta {
  min-width: 0;
  overflow: hidden;
}

.aether-user__name {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--navbar-text, #dfe2ef);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.aether-user__role {
  margin: 0.15rem 0 0;
  font-size: 10px;
  color: var(--aether-cyan-muted, #94a3b8);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-container {
  background-color: v-bind(getMenuBackground);

  /* 稿：nav 区为 px-4，菜单不贴侧栏左右边 */
  :deep(.scrollbar-wrapper) {
    padding: 0 1rem;
    box-sizing: border-box;
    background-color: transparent;
  }

  .el-menu {
    border: none;
    height: 100%;
    width: 100% !important;

    .el-menu-item,
    .el-sub-menu__title {
      &:hover {
        background-color: var(--menu-hover, rgba(0, 0, 0, 0.06)) !important;
      }
    }

    .el-menu-item {
      color: v-bind(getMenuTextColor);

      /* 激活态背景由 theme-aether 提供（稿：bg-cyan-400/5），勿强制透明 */
      &.is-active {
        color: var(--menu-active-text, #409eff);
      }
    }

    .el-sub-menu__title {
      color: v-bind(getMenuTextColor);
    }
  }
}
</style>

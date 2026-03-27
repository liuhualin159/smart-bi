<template>
  <div class="aether-brand" :class="{ collapse }">
    <router-link class="aether-brand__link" to="/">
      <div class="aether-brand__icon-wrap">
        <img class="aether-brand__icon-image" src="/site-icon.png" alt="site-icon" />
      </div>
      <div v-if="!collapse" class="aether-brand__text">
        <h1 class="aether-brand__title">{{ title }}</h1>
        <p class="aether-brand__sub">智能问数 · 数据平台</p>
      </div>
    </router-link>
  </div>
</template>

<script setup>
defineProps({
  collapse: {
    type: Boolean,
    required: true
  }
})

const title = import.meta.env.VITE_APP_TITLE
</script>

<style lang="scss" scoped>
/* 展开时由父级 .aether-sidebar-head 提供 px-6，此处仅控制品牌行与按钮间距 mb-8 */
.aether-brand {
  padding: 0;
  margin-bottom: 2rem; /* mb-8：品牌行与「新建分析」之间 */

  &.collapse {
    margin-bottom: 0;
    /* 侧栏 54px 宽：勿加左右 padding，否则 40px 图标区被裁切 */
    padding: 0 0 0.75rem;

    .aether-brand__link {
      justify-content: center;
    }
  }
}

.aether-brand__link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
}

.aether-brand__icon-wrap {
  flex-shrink: 0;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #22d3ee 0%, #9333ea 100%);
  box-shadow: 0 4px 14px rgba(34, 211, 238, 0.25);
}

.aether-brand__icon-image {
  width: 1.5rem;
  height: 1.5rem;
  object-fit: contain;
}

.aether-brand__text {
  min-width: 0;
}

.aether-brand__title {
  margin: 0;
  font-family: "Space Grotesk", "Inter", system-ui, sans-serif;
  font-size: 1.25rem; /* text-xl 与稿 Neural Cartographer 标题比例 */
  font-weight: 700;
  letter-spacing: -0.025em; /* tracking-tight */
  line-height: 1.2;
  background: linear-gradient(90deg, #22d3ee, #a855f7);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.aether-brand__sub {
  margin: 0.2rem 0 0;
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--aether-cyan-muted, #94a3b8);
}
</style>

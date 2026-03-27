<template>
  <div class="title-bar" :style="barStyle">
    <div class="title-bar__accent" :style="{ backgroundColor: mergedConfig.borderColor }"></div>
    <div class="title-bar__content">
      <span class="title-bar__text" :style="textStyle">{{ mergedConfig.title }}</span>
    </div>
    <div class="title-bar__bottom-line" :style="{ backgroundColor: mergedConfig.borderColor }"></div>
    <div class="title-bar__glow" :style="{ backgroundColor: mergedConfig.borderColor }"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  config: {
    type: Object,
    default: () => ({})
  }
})

const defaults = {
  title: '标题',
  fontSize: 18,
  fontColor: '#ffffff',
  backgroundColor: 'linear-gradient(90deg, #568aea 0%, transparent 100%)',
  borderColor: '#568aea'
}

const mergedConfig = computed(() => ({ ...defaults, ...props.config }))

const barStyle = computed(() => {
  const bg = mergedConfig.value.backgroundColor
  const isGradient = bg && bg.includes('gradient')
  return {
    background: isGradient ? bg : undefined,
    backgroundColor: isGradient ? undefined : bg
  }
})

const textStyle = computed(() => ({
  fontSize: mergedConfig.value.fontSize + 'px',
  color: mergedConfig.value.fontColor
}))
</script>

<style scoped>
.title-bar {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  min-height: 40px;
  padding: 0 16px;
  box-sizing: border-box;
  overflow: hidden;
}

.title-bar__accent {
  position: absolute;
  left: 0;
  top: 10%;
  width: 4px;
  height: 80%;
  border-radius: 0 2px 2px 0;
  animation: accent-breathe 2.5s ease-in-out infinite;
}

.title-bar__content {
  flex: 1;
  padding-left: 12px;
  z-index: 1;
}

.title-bar__text {
  font-weight: 600;
  letter-spacing: 2px;
  text-shadow: 0 0 8px rgba(86, 138, 234, 0.5);
}

.title-bar__bottom-line {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 1px;
  opacity: 0.6;
}

.title-bar__glow {
  position: absolute;
  bottom: 0;
  left: -100%;
  width: 60%;
  height: 2px;
  opacity: 0.8;
  filter: blur(1px);
  animation: glow-sweep 3s ease-in-out infinite;
}

@keyframes accent-breathe {
  0%, 100% { opacity: 0.7; }
  50% { opacity: 1; }
}

@keyframes glow-sweep {
  0% { left: -60%; }
  100% { left: 100%; }
}
</style>

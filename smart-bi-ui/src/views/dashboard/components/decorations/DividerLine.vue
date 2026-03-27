<template>
  <div class="divider-line" :class="orientationClass">
    <div class="divider-line__line" :style="lineStyle"></div>
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
  lineStyle: 'solid',
  lineColor: '#568aea',
  lineWidth: 1,
  orientation: 'horizontal'
}

const mergedConfig = computed(() => ({ ...defaults, ...props.config }))

const orientationClass = computed(() =>
  mergedConfig.value.orientation === 'vertical' ? 'divider-line--vertical' : 'divider-line--horizontal'
)

const lineStyle = computed(() => {
  const cfg = mergedConfig.value
  const isHorizontal = cfg.orientation !== 'vertical'

  if (cfg.lineStyle === 'gradient') {
    const gradientDir = isHorizontal ? 'to right' : 'to bottom'
    return {
      background: `linear-gradient(${gradientDir}, transparent, ${cfg.lineColor}, transparent)`,
      [isHorizontal ? 'height' : 'width']: cfg.lineWidth + 'px'
    }
  }

  if (isHorizontal) {
    return {
      borderTopStyle: cfg.lineStyle,
      borderTopWidth: cfg.lineWidth + 'px',
      borderTopColor: cfg.lineColor
    }
  }
  return {
    borderLeftStyle: cfg.lineStyle,
    borderLeftWidth: cfg.lineWidth + 'px',
    borderLeftColor: cfg.lineColor
  }
})
</script>

<style scoped>
.divider-line {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.divider-line--horizontal .divider-line__line {
  width: 100%;
}

.divider-line--vertical .divider-line__line {
  height: 100%;
}
</style>

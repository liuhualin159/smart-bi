<template>
  <div class="border-box" :class="boxClass">
    <!-- Corner decorations -->
    <div class="border-box__corner border-box__corner--tl" :style="cornerStyle"></div>
    <div class="border-box__corner border-box__corner--tr" :style="cornerStyle"></div>
    <div class="border-box__corner border-box__corner--bl" :style="cornerStyle"></div>
    <div class="border-box__corner border-box__corner--br" :style="cornerStyle"></div>

    <!-- Border edges -->
    <div class="border-box__edge border-box__edge--top" :style="edgeHStyle"></div>
    <div class="border-box__edge border-box__edge--bottom" :style="edgeHStyle"></div>
    <div class="border-box__edge border-box__edge--left" :style="edgeVStyle"></div>
    <div class="border-box__edge border-box__edge--right" :style="edgeVStyle"></div>

    <!-- Sweep animation -->
    <div
      v-if="mergedConfig.animationEnabled"
      class="border-box__sweep"
      :style="{ backgroundColor: mergedConfig.borderColor }"
    ></div>

    <div class="border-box__content">
      <slot></slot>
    </div>
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
  borderStyle: 'tech-border-1',
  borderColor: '#568aea',
  borderWidth: 2,
  animationEnabled: true
}

const mergedConfig = computed(() => ({ ...defaults, ...props.config }))

const boxClass = computed(() => mergedConfig.value.borderStyle)

const cornerStyle = computed(() => ({
  borderColor: mergedConfig.value.borderColor,
  borderWidth: mergedConfig.value.borderWidth + 'px'
}))

const edgeHStyle = computed(() => ({
  borderColor: mergedConfig.value.borderColor,
  borderWidth: mergedConfig.value.borderWidth + 'px'
}))

const edgeVStyle = computed(() => ({
  borderColor: mergedConfig.value.borderColor,
  borderWidth: mergedConfig.value.borderWidth + 'px'
}))
</script>

<style scoped>
.border-box {
  position: relative;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

/* Corner L-shaped decorations */
.border-box__corner {
  position: absolute;
  width: 16px;
  height: 16px;
  border-style: solid;
  z-index: 2;
}

.border-box__corner--tl {
  top: 0; left: 0;
  border-right: none !important;
  border-bottom: none !important;
}
.border-box__corner--tr {
  top: 0; right: 0;
  border-left: none !important;
  border-bottom: none !important;
}
.border-box__corner--bl {
  bottom: 0; left: 0;
  border-right: none !important;
  border-top: none !important;
}
.border-box__corner--br {
  bottom: 0; right: 0;
  border-left: none !important;
  border-top: none !important;
}

/* Edge borders (dashed for tech feel) */
.border-box__edge {
  position: absolute;
  z-index: 1;
}
.border-box__edge--top {
  top: 0; left: 16px; right: 16px;
  height: 0;
  border-top-style: dashed;
  border-bottom: none !important;
  border-left: none !important;
  border-right: none !important;
}
.border-box__edge--bottom {
  bottom: 0; left: 16px; right: 16px;
  height: 0;
  border-bottom-style: dashed;
  border-top: none !important;
  border-left: none !important;
  border-right: none !important;
}
.border-box__edge--left {
  top: 16px; bottom: 16px; left: 0;
  width: 0;
  border-left-style: dashed;
  border-top: none !important;
  border-bottom: none !important;
  border-right: none !important;
}
.border-box__edge--right {
  top: 16px; bottom: 16px; right: 0;
  width: 0;
  border-right-style: dashed;
  border-top: none !important;
  border-bottom: none !important;
  border-left: none !important;
}

/* Sweep animation light */
.border-box__sweep {
  position: absolute;
  top: 0;
  left: -100%;
  width: 40%;
  height: 100%;
  opacity: 0.06;
  filter: blur(20px);
  animation: border-sweep 4s linear infinite;
  z-index: 0;
}

@keyframes border-sweep {
  0% { left: -40%; }
  100% { left: 100%; }
}

.border-box__content {
  position: relative;
  width: 100%;
  height: 100%;
  padding: 12px;
  box-sizing: border-box;
  z-index: 1;
}

/* tech-border-1: standard tech style */
.tech-border-1 .border-box__corner { width: 16px; height: 16px; }

/* tech-border-2: rounded tech style */
.tech-border-2 {
  border-radius: 8px;
}
.tech-border-2 .border-box__corner { width: 12px; height: 12px; border-radius: 4px 0 0 0; }
.tech-border-2 .border-box__corner--tr { border-radius: 0 4px 0 0; }
.tech-border-2 .border-box__corner--bl { border-radius: 0 0 0 4px; }
.tech-border-2 .border-box__corner--br { border-radius: 0 0 4px 0; }
.tech-border-2 .border-box__edge--top,
.tech-border-2 .border-box__edge--bottom { left: 12px; right: 12px; }
.tech-border-2 .border-box__edge--left,
.tech-border-2 .border-box__edge--right { top: 12px; bottom: 12px; }

/* tech-border-3: minimal style */
.tech-border-3 .border-box__corner { width: 8px; height: 8px; }
.tech-border-3 .border-box__edge--top,
.tech-border-3 .border-box__edge--bottom {
  left: 8px; right: 8px;
  border-top-style: solid;
  border-bottom-style: solid;
  opacity: 0.4;
}
.tech-border-3 .border-box__edge--left,
.tech-border-3 .border-box__edge--right {
  top: 8px; bottom: 8px;
  border-left-style: solid;
  border-right-style: solid;
  opacity: 0.4;
}
</style>

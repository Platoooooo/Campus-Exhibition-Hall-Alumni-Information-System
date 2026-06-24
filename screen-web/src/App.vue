<script setup>
import { useScale } from '@/composables/useScale'
import ParticleBg from '@/components/ParticleBg.vue'

const { scale } = useScale()
</script>

<template>
  <div class="screen-root" :style="{ transform: `scale(${scale})` }">
    <ParticleBg />
    <router-view />
  </div>
</template>

<style>
/* ---- 字体定义 ---- */
@font-face {
  font-family: 'HarmonyOS_SansSC_Bold';
  src: url('@/assets/fonts/HarmonyOS_SansSC_Bold.ttf') format('truetype');
  font-display: swap;
}
@font-face {
  font-family: 'HarmonyOS_SansSC_Regular';
  src: url('@/assets/fonts/HarmonyOS_SansSC_Regular.ttf') format('truetype');
  font-display: swap;
}
@font-face {
  font-family: 'din-bold-2';
  src: url('@/assets/fonts/din-bold-2.ttf') format('truetype');
  font-display: swap;
}
@font-face {
  font-family: 'Rajdhani-SemiBold';
  src: url('@/assets/fonts/Rajdhani-SemiBold-5.ttf') format('truetype');
  font-display: swap;
}

html, body, #app {
  margin: 0; padding: 0;
  width: 100vw; height: 100vh;
  overflow: hidden;
  font-family: 'HarmonyOS_SansSC_Regular', 'Microsoft YaHei', sans-serif;
  background: #0A1326;
  color: #EAF2FF;
}

/* ---- 等比缩放根容器 ---- */
.screen-root {
  position: relative;
  width: 1920px;
  height: 1080px;
  transform-origin: top left;
  overflow: hidden;
  background: linear-gradient(135deg, #0A1326 0%, #0E1B3A 40%, #0C1832 100%);
}

/* ---- 全局玻璃卡片 ---- */
.glass-card {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(120, 170, 255, 0.25);
  border-radius: 16px;
  backdrop-filter: blur(12px);
  box-shadow: 0 0 20px rgba(30, 139, 255, 0.08);
}

/* ---- 发光描边 ---- */
.glow-border {
  box-shadow:
    0 0 1px rgba(120, 170, 255, 0.4),
    0 0 8px rgba(30, 139, 255, 0.15);
}

/* ---- 科技角标 ---- */
.corner-decor {
  position: fixed;
  width: 40px; height: 40px;
  border-color: rgba(120, 170, 255, 0.35);
  border-style: solid;
  pointer-events: none;
  z-index: 0;
}

.corner-decor.tl { top: 20px; left: 20px; border-width: 2px 0 0 2px; }
.corner-decor.tr { top: 20px; right: 20px; border-width: 2px 2px 0 0; }
.corner-decor.bl { bottom: 20px; left: 20px; border-width: 0 0 2px 2px; }
.corner-decor.br { bottom: 20px; right: 20px; border-width: 0 2px 2px 0; }
</style>

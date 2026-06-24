<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
let animationId = null

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  canvas.width = 1920
  canvas.height = 1080

  // 粒子池
  const particles = Array.from({ length: 60 }, () => ({
    x: Math.random() * 1920,
    y: Math.random() * 1080,
    r: Math.random() * 2 + 0.5,
    vx: (Math.random() - 0.5) * 0.3,
    vy: (Math.random() - 0.5) * 0.3 - 0.1,
    alpha: Math.random() * 0.4 + 0.1
  }))

  function draw() {
    ctx.clearRect(0, 0, 1920, 1080)
    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy

      // 边界回弹
      if (p.x < 0 || p.x > 1920) p.vx *= -1
      if (p.y < 0 || p.y > 1080) p.vy *= -1

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(30,139,255,${p.alpha})`
      ctx.fill()

      // 发光光晕
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r * 3, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(30,139,255,${p.alpha * 0.15})`
      ctx.fill()
    }
    animationId = requestAnimationFrame(draw)
  }

  draw()
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<template>
  <canvas ref="canvasRef" class="particle-canvas" />
</template>

<style scoped>
.particle-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 1920px;
  height: 1080px;
  pointer-events: none;
  z-index: 0;
}
</style>

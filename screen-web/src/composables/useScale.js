import { ref, onMounted, onUnmounted } from 'vue'

/**
 * 基于 1920x1080 基准的等比自适应缩放
 * 返回 scale 值，应用到根容器 transform: scale(scale)
 */
export function useScale(baselineW = 1920, baselineH = 1080) {
  const scale = ref(1)

  function calc() {
    const w = window.innerWidth
    const h = window.innerHeight
    scale.value = Math.min(w / baselineW, h / baselineH)
  }

  let timer = null
  function onResize() {
    clearTimeout(timer)
    timer = setTimeout(calc, 200)
  }

  onMounted(() => {
    calc()
    window.addEventListener('resize', onResize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', onResize)
    clearTimeout(timer)
  })

  return { scale }
}

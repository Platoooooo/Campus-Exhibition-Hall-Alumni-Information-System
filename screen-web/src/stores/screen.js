import { defineStore } from 'pinia'
import { ref } from 'vue'

const CACHE_KEY = 'screen_cache_carousel'

export const useScreenStore = defineStore('screen', () => {
  // ---- 状态 ----
  const currentAlumni = ref(null)
  const isFaceMode = ref(false)
  const carouselData = ref(null)
  const isOffline = ref(false)

  // ---- 人脸识别状态机 ----
  // 'idle' | 'scanning' | 'hit' | 'miss' | 'degraded'
  const faceState = ref('idle')
  const hitAlumni = ref(null)       // FaceRecognizeVO.alumni
  const hitTimeline = ref([])       // FaceRecognizeVO.timeline
  const hitVersion = ref(0)         // 递增版本号：支持连续命中时平滑切换（carousel 监听此值）

  // ---- 轮播数据 + 缓存 ----
  function setCarouselData(data) {
    if (data) {
      carouselData.value = data
      try {
        localStorage.setItem(CACHE_KEY, JSON.stringify(data))
      } catch { /* quota exceeded */ }
      isOffline.value = false
    }
  }

  function loadFromCache() {
    try {
      const cached = localStorage.getItem(CACHE_KEY)
      if (cached) {
        carouselData.value = JSON.parse(cached)
        isOffline.value = true
        return true
      }
    } catch { /* ignore */ }
    return false
  }

  // ---- 人脸模式 ----
  function setAlumni(alumni) {
    currentAlumni.value = alumni
    isFaceMode.value = !!alumni
  }

  function resetToDefault() {
    currentAlumni.value = null
    isFaceMode.value = false
  }

  /** 人脸命中：设置 alumni + timeline，切换到 hit 状态 */
  function onFaceHit(alumni, timeline) {
    hitAlumni.value = alumni
    hitTimeline.value = timeline || []
    hitVersion.value++            // 递增版本号（carousel 监听到变化即重播）
    faceState.value = 'hit'
    setAlumni(alumni)
  }

  /** 未命中/降级 */
  function onFaceMiss(status) {
    faceState.value = status // 'miss' | 'degraded'
  }

  /** 从人脸模式恢复到默认 */
  function resetFace() {
    faceState.value = 'idle'
    hitAlumni.value = null
    hitTimeline.value = []
    resetToDefault()
  }

  function setFaceState(state) {
    faceState.value = state
  }

  return {
    currentAlumni, isFaceMode,
    carouselData, isOffline,
    faceState, hitAlumni, hitTimeline, hitVersion,
    setCarouselData, loadFromCache,
    setAlumni, resetToDefault,
    onFaceHit, onFaceMiss, resetFace, setFaceState
  }
})

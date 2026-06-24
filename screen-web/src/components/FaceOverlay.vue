<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useScreenStore } from '@/stores/screen'
import { recognize } from '@/api/face'

const store = useScreenStore()

// ---- 配置 ----
const CAPTURE_INTERVAL = 3000   // 抓帧间隔 ms
const HIT_INTRO_MS = 2500       // 命中卡片展示时长（不遮挡轮播）
const HIT_MAX_MS = 20000        // 命中后最长展示时间（超时强制恢复默认）

// 摄像头配置（环境变量可覆盖）
const CAMERA_FACING_MODE = import.meta.env.VITE_CAMERA_FACING_MODE || 'environment'
const CAMERA_DEVICE_ID = import.meta.env.VITE_CAMERA_DEVICE_ID || null

// ---- 摄像头 ----
const videoRef = ref(null)
const canvasRef = ref(null)
const cameraOk = ref(false)
const cameraError = ref('')
const cameraLabel = ref('')    // 当前使用的摄像头名称

let stream = null
let captureTimer = null
let hitTimer = null
let isRecognizing = false       // 防止并发 API 调用
let recognitionPaused = false   // 命中后暂停识别，等 timeline 播完再恢复

// ---- 扫描动画文本 ----
const scanDots = ref('')

// ---- 启动摄像头 ----
async function startCamera() {
  try {
    // 构建约束：优先 exact deviceId，其次 facingMode，最后任意
    const constraints = { video: { width: 640, height: 480 }, audio: false }

    if (CAMERA_DEVICE_ID) {
      constraints.video.deviceId = { exact: CAMERA_DEVICE_ID }
    } else if (CAMERA_FACING_MODE !== 'auto') {
      constraints.video.facingMode = CAMERA_FACING_MODE
    }

    stream = await navigator.mediaDevices.getUserMedia(constraints)

    // 获取实际使用的摄像头名称
    const videoTrack = stream.getVideoTracks()[0]
    if (videoTrack) {
      cameraLabel.value = videoTrack.label || '摄像头'
    }

    if (videoRef.value) {
      videoRef.value.srcObject = stream
      await videoRef.value.play()
    }
    cameraOk.value = true
    cameraError.value = ''
    // 启动定时抓帧
    scheduleCapture()
  } catch (err) {
    cameraOk.value = false
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      cameraError.value = '摄像头未授权'
    } else if (err.name === 'NotFoundError' || err.name === 'OverconstrainedError') {
      // 指定设备不可用时，回退到任意摄像头
      if (!CAMERA_DEVICE_ID && CAMERA_FACING_MODE !== 'auto') {
        try {
          stream = await navigator.mediaDevices.getUserMedia({
            video: { width: 640, height: 480 },
            audio: false
          })
          const vt = stream.getVideoTracks()[0]
          if (vt) cameraLabel.value = vt.label || '默认摄像头'
          if (videoRef.value) {
            videoRef.value.srcObject = stream
            await videoRef.value.play()
          }
          cameraOk.value = true
          cameraError.value = ''
          scheduleCapture()
          return
        } catch (_) { /* 回退也失败，继续报错 */ }
      }
      cameraError.value = '未检测到摄像头'
    } else {
      cameraError.value = '摄像头不可用'
    }
  }
}

function stopCamera() {
  clearInterval(captureTimer)
  clearTimeout(hitTimer)
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
    stream = null
  }
}

// ---- 抓帧 ----
function captureFrame() {
  if (!canvasRef.value || !videoRef.value || !cameraOk.value) return null
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  canvas.width = 640
  canvas.height = 480
  ctx.drawImage(videoRef.value, 0, 0, 640, 480)
  // 获取 base64（去掉 data:image/...;base64, 前缀）
  const dataUrl = canvas.toDataURL('image/jpeg', 0.85)
  return dataUrl.substring(dataUrl.indexOf(',') + 1)
}

function scheduleCapture() {
  captureTimer = setInterval(async () => {
    if (isRecognizing || recognitionPaused) return
    const frame = captureFrame()
    if (!frame) return

    isRecognizing = true
    store.setFaceState('scanning')
    scanDots.value = ''

    try {
      const result = await recognize(frame, 'screen-01')

      if (result.status === 'HIT') {
        // 命中：展示卡片，暂停后续识别，等 timeline 播完再恢复
        store.onFaceHit(result.alumni, result.timeline)
        recognitionPaused = true
        resetHitTimer()
      }
      // MISS / DEGRADED / 其他：静默回到 idle，不弹任何负向通知
    } catch {
      // 网络异常：静默忽略
    } finally {
      isRecognizing = false
      if (store.faceState === 'scanning') store.resetFace()
    }
  }, CAPTURE_INTERVAL)
}

// ---- 扫描时动画点 ----
let dotsTimer = null
watch(() => store.faceState, (state) => {
  if (state === 'scanning') {
    let count = 0
    dotsTimer = setInterval(() => {
      count = (count + 1) % 4
      scanDots.value = '.'.repeat(count)
    }, 500)
  } else {
    clearInterval(dotsTimer)
    scanDots.value = ''
  }
})

// ---- HIT 过场倒计时（可重置：新人识别后重新计时，最多 HIT_MAX_MS） ----
function resetHitTimer() {
  clearTimeout(hitTimer)
  const cycleTime = (store.hitTimeline.length || 1) * (store.carouselData?.intervalSec || 8) * 1000
  const capped = Math.min(HIT_INTRO_MS + cycleTime, HIT_MAX_MS)
  hitTimer = setTimeout(() => {
    store.resetFace()
  }, capped)
}

// 命中时启动计时
watch(() => store.faceState, (state) => {
  if (state === 'hit') {
    resetHitTimer()
  }
})

// 摄像头断开时强制恢复默认
watch(cameraOk, (ok) => {
  if (!ok) {
    recognitionPaused = false
    clearTimeout(hitTimer)
    if (store.faceState !== 'idle') store.resetFace()
  } else {
    recognitionPaused = false
  }
})

// timeline 播放完毕 → 恢复识别
watch(() => store.faceState, (state) => {
  if (state === 'idle') {
    recognitionPaused = false
  }
})

onMounted(() => {
  startCamera()
})

onUnmounted(() => {
  stopCamera()
})
</script>

<template>
  <div class="face-overlay">
    <!-- 隐藏的视频和 canvas -->
    <video ref="videoRef" style="display:none" playsinline />
    <canvas ref="canvasRef" style="display:none" />

    <!-- ════ 待机态：摄像头就绪指示（自动检测，无需点击） ════ -->
    <Transition name="face-fade">
      <div v-if="store.faceState === 'idle'" class="face-idle">
        <div class="idle-indicator">
          <span v-if="cameraOk" class="camera-live-dot" />
          <span v-else class="camera-off-dot" />
          <span class="idle-label" v-if="cameraOk">人脸识别就绪</span>
          <span class="idle-label" v-else>{{ cameraError || '摄像头未就绪' }}</span>
        </div>
        <div class="idle-camera-name" v-if="cameraLabel">{{ cameraLabel }}</div>
      </div>
    </Transition>

    <!-- ════ 扫描态：右下角小指示器，不影响大屏播放 ════ -->
    <Transition name="face-fade">
      <div v-if="store.faceState === 'scanning'" class="face-scanning">
        <span class="scan-dot" />
        <span class="scan-label">识别中{{ scanDots }}</span>
      </div>
    </Transition>

    <!-- ════ 命中卡片：右侧滑入，不遮挡轮播 ════ -->
    <Transition name="hit-slide">
      <div v-if="store.faceState === 'hit'" class="face-hit-card">
        <div class="hit-card-avatar" v-if="store.hitAlumni?.avatar">
          <img :src="store.hitAlumni.avatar" :alt="store.hitAlumni.name" />
        </div>
        <div class="hit-card-avatar hit-card-placeholder" v-else>
          {{ (store.hitAlumni?.name || '?')[0] }}
        </div>
        <div class="hit-card-info">
          <span class="hit-card-name">{{ store.hitAlumni?.name || '' }}</span>
          <span class="hit-card-meta">{{ store.hitAlumni?.collegeName }} · {{ store.hitAlumni?.gradYear }}届</span>
        </div>
        <div class="hit-card-badge">已识别</div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.face-overlay {
  position: fixed; inset: 0; pointer-events: none; z-index: 100;
}

.face-overlay > * { pointer-events: auto; }

/* ---- 过渡 ---- */
.face-fade-enter-active { transition: all 0.5s ease-out; }
.face-fade-leave-active { transition: all 0.3s ease-in; }
.face-fade-enter-from, .face-fade-leave-to { opacity: 0; transform: scale(0.95); }

/* ════ 待机态：摄像头就绪指示 ════ */
.face-idle {
  position: absolute; bottom: 30px; right: 30px;
  display: flex; flex-direction: column; align-items: flex-end; gap: 4px;
}

.idle-indicator {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  background: rgba(10, 19, 38, 0.75);
  border: 1px solid rgba(120, 170, 255, 0.15);
  border-radius: 20px;
  backdrop-filter: blur(8px);
}

.camera-live-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: #36C5A6;
  box-shadow: 0 0 10px rgba(54, 197, 166, 0.5);
  animation: live-pulse 2s infinite;
}

@keyframes live-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.85); }
}

.camera-off-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: #E5484D;
  box-shadow: 0 0 8px rgba(229, 72, 77, 0.4);
}

.idle-label {
  font-size: 12px; color: rgba(234, 242, 255, 0.7);
  user-select: none;
}

.idle-camera-name {
  font-size: 10px; color: rgba(134, 144, 156, 0.6);
  padding-right: 4px;
  user-select: none;
}

/* ════ 扫描态：右下角小指示器 ════ */
.face-scanning {
  position: absolute; bottom: 30px; right: 30px;
  display: flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  background: rgba(10, 19, 38, 0.75);
  border: 1px solid rgba(30, 139, 255, 0.2);
  border-radius: 20px;
  backdrop-filter: blur(8px);
}

.scan-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: #1E8BFF;
  box-shadow: 0 0 10px rgba(30, 139, 255, 0.5);
  animation: scan-blink 0.8s ease-in-out infinite;
}

@keyframes scan-blink {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(0.7); }
}

.scan-label {
  font-size: 12px; color: rgba(234, 242, 255, 0.7);
  user-select: none;
}

/* ════ 命中卡片：右侧滑入，不遮挡轮播 ════ */
.face-hit-card {
  position: absolute; top: 80px; right: 30px;
  display: flex; align-items: center; gap: 14px;
  padding: 16px 20px;
  background: rgba(10, 19, 38, 0.9);
  border: 1px solid rgba(230, 180, 80, 0.4);
  border-radius: 16px;
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5), 0 0 20px rgba(230, 180, 80, 0.15);
  max-width: 340px;
  pointer-events: none;
}

.hit-card-avatar {
  width: 56px; height: 56px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #E6B450;
  flex-shrink: 0;
}

.hit-card-avatar img { width: 100%; height: 100%; object-fit: cover; }

.hit-card-placeholder {
  display: flex; align-items: center; justify-content: center;
  background: rgba(230, 180, 80, 0.15);
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 24px; color: #E6B450;
  border-color: rgba(230, 180, 80, 0.4);
}

.hit-card-info {
  display: flex; flex-direction: column; gap: 2px; min-width: 0;
}

.hit-card-name {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 18px; color: #EAF2FF;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.hit-card-meta {
  font-size: 12px; color: #93A7C9;
}

.hit-card-badge {
  font-size: 10px; color: #E6B450;
  border: 1px solid rgba(230, 180, 80, 0.3);
  border-radius: 10px;
  padding: 2px 8px;
  flex-shrink: 0;
}

/* 卡片滑入/滑出动画 */
.hit-slide-enter-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.hit-slide-leave-active { transition: all 0.3s ease-in; }
.hit-slide-enter-from { opacity: 0; transform: translateX(60px); }
.hit-slide-leave-to { opacity: 0; transform: translateX(60px); }
</style>

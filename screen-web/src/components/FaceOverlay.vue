<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useScreenStore } from '@/stores/screen'
import { recognize } from '@/api/face'

const store = useScreenStore()

// ---- 配置 ----
const CAPTURE_INTERVAL = 3000   // 抓帧间隔 ms（缩短以提升响应速度）
const MISS_DISPLAY_MS = 3500    // 未命中提示时长
const HIT_INTRO_MS = 4000       // "欢迎"过场时长

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
let missTimer = null
let hitTimer = null
let isRecognizing = false       // 防止并发 API 调用

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
  clearTimeout(missTimer)
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
    if (isRecognizing) return  // 上一次识别未完成，跳过本轮
    const frame = captureFrame()
    if (!frame) return

    isRecognizing = true
    const prevState = store.faceState
    store.setFaceState('scanning')
    scanDots.value = ''

    try {
      const result = await recognize(frame, 'screen-01')

      if (result.status === 'HIT') {
        // 命中：更新数据。若已在 hit 态则平滑切换（递增 hitVersion 触发 carousel 重播）
        store.onFaceHit(result.alumni, result.timeline)
        // 重置自动恢复计时
        resetHitTimer()
      } else if (result.status === 'DEGRADED') {
        // HIT 态下忽略降级，保持当前展示
        if (prevState !== 'hit') {
          store.onFaceMiss('degraded')
          missTimer = setTimeout(() => {
            if (store.faceState === 'degraded') store.resetFace()
          }, MISS_DISPLAY_MS)
        } else {
          store.setFaceState('hit')
        }
      } else {
        // NO_MATCH：HIT 态下忽略，保持当前展示
        if (prevState !== 'hit') {
          store.onFaceMiss('miss')
          missTimer = setTimeout(() => {
            if (store.faceState === 'miss') store.resetFace()
          }, MISS_DISPLAY_MS)
        } else {
          store.setFaceState('hit')
        }
      }
    } catch {
      // 网络异常：HIT 态下忽略
      if (prevState !== 'hit') {
        store.onFaceMiss('degraded')
        missTimer = setTimeout(() => {
          if (store.faceState === 'degraded') store.resetFace()
        }, MISS_DISPLAY_MS)
      } else {
        store.setFaceState('hit')
      }
    } finally {
      isRecognizing = false
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

// ---- HIT 过场倒计时（可重置：新人识别后重新计时） ----
function resetHitTimer() {
  clearTimeout(hitTimer)
  hitTimer = setTimeout(() => {
    store.resetFace()
  }, HIT_INTRO_MS + (store.hitTimeline.length || 3) * (store.carouselData?.intervalSec || 8) * 1000)
}

// 命中时启动计时
watch(() => store.faceState, (state) => {
  if (state === 'hit') {
    resetHitTimer()
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

    <!-- ════ 扫描态：中央扫描环 ════ -->
    <Transition name="face-fade">
      <div v-if="store.faceState === 'scanning'" class="face-scanning">
        <div class="scan-ring">
          <div class="scan-ring-inner" />
          <div class="scan-arc" />
        </div>
        <p class="scan-text">正在识别{{ scanDots }}</p>
      </div>
    </Transition>

    <!-- ════ 未命中/降级提示 ════ -->
    <Transition name="face-fade">
      <div v-if="store.faceState === 'miss' || store.faceState === 'degraded'" class="face-miss">
        <span v-if="store.faceState === 'miss'" class="miss-icon">🙂</span>
        <span v-else class="miss-icon">🔌</span>
        <p class="miss-text" v-if="store.faceState === 'miss'">
          未识别到您，继续浏览吧
        </p>
        <p class="miss-text" v-else>
          服务暂不可用
        </p>
      </div>
    </Transition>

    <!-- ════ 命中过场 ════ -->
    <Transition name="face-fade">
      <div v-if="store.faceState === 'hit'" class="face-hit">
        <div class="hit-particles">
          <div v-for="i in 8" :key="i" class="hit-particle" :style="{
            animationDelay: `${i * 0.15}s`,
            left: `${40 + Math.cos(i * Math.PI / 4) * 160}px`,
            top: `${40 + Math.sin(i * Math.PI / 4) * 160}px`
          }" />
        </div>
        <div class="hit-avatar" v-if="store.hitAlumni?.avatar">
          <img :src="store.hitAlumni.avatar" :alt="store.hitAlumni.name" />
        </div>
        <div class="hit-avatar-placeholder" v-else>
          {{ (store.hitAlumni?.name || '?')[0] }}
        </div>
        <h2 class="hit-welcome">欢迎，{{ store.hitAlumni?.name || '' }}！</h2>
        <p class="hit-sub">{{ store.hitAlumni?.collegeName }} · {{ store.hitAlumni?.gradYear }}届</p>
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

/* ════ 扫描态 ════ */
.face-scanning {
  position: absolute; inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: rgba(10, 19, 38, 0.7);
}

.scan-ring {
  position: relative;
  width: 200px; height: 200px;
}

.scan-ring-inner {
  position: absolute;
  inset: 20px;
  border-radius: 50%;
  border: 3px solid rgba(30, 139, 255, 0.3);
  animation: ring-pulse 1.5s ease-in-out infinite;
}

@keyframes ring-pulse {
  0%, 100% { transform: scale(0.8); opacity: 0.3; }
  50% { transform: scale(1); opacity: 1; }
}

.scan-arc {
  position: absolute; inset: 0;
  border-radius: 50%;
  border: 3px solid transparent;
  border-top-color: #1E8BFF;
  animation: arc-spin 2s linear infinite;
  filter: drop-shadow(0 0 8px rgba(30, 139, 255, 0.6));
}

@keyframes arc-spin { to { transform: rotate(360deg); } }

.scan-text {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px; color: #EAF2FF;
  margin-top: 32px; letter-spacing: 4px;
}

/* ════ 未命中 ════ */
.face-miss {
  position: absolute; bottom: 30px; right: 30px;
  display: flex; align-items: center; gap: 10px;
  padding: 14px 22px;
  background: rgba(10, 19, 38, 0.9);
  border: 1px solid rgba(254, 215, 102, 0.3);
  border-radius: 16px;
  backdrop-filter: blur(8px);
}

.miss-icon { font-size: 24px; }

.miss-text {
  font-size: 13px; color: #93A7C9; margin: 0;
}

/* ════ 命中过场 ════ */
.face-hit {
  position: absolute; inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  background: rgba(10, 19, 38, 0.9);
}

.hit-particles {
  position: relative;
  width: 360px; height: 360px;
  margin-bottom: -280px;
}

.hit-particle {
  position: absolute;
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #E6B450;
  box-shadow: 0 0 12px rgba(230, 180, 80, 0.8);
  animation: particle-converge 1.2s ease-out forwards;
}

@keyframes particle-converge {
  0% { transform: scale(1); }
  50% { transform: scale(2.5); opacity: 1; }
  100% { transform: scale(0); opacity: 0; }
}

.hit-avatar {
  width: 120px; height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #E6B450;
  box-shadow: 0 0 40px rgba(230, 180, 80, 0.5);
  margin-bottom: 24px;
  animation: hit-bounce 0.6s ease-out;
}

@keyframes hit-bounce {
  0% { transform: scale(0.3); opacity: 0; }
  60% { transform: scale(1.1); }
  100% { transform: scale(1); opacity: 1; }
}

.hit-avatar img { width: 100%; height: 100%; object-fit: cover; }

.hit-avatar-placeholder {
  width: 120px; height: 120px;
  border-radius: 50%;
  background: rgba(30, 139, 255, 0.2);
  display: flex; align-items: center; justify-content: center;
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 48px; color: #1E8BFF;
  border: 3px solid rgba(30, 139, 255, 0.4);
  margin-bottom: 24px;
  animation: hit-bounce 0.6s ease-out;
}

.hit-welcome {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 42px; color: #EAF2FF;
  margin: 0 0 8px; letter-spacing: 4px;
  animation: hit-bounce 0.6s ease-out 0.1s both;
}

.hit-sub {
  font-size: 18px; color: #93A7C9;
  margin: 0;
  animation: hit-bounce 0.6s ease-out 0.2s both;
}
</style>

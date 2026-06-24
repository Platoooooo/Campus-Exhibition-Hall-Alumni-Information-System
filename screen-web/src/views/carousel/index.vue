<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useScreenStore } from '@/stores/screen'
import { getCarousel } from '@/api/screen'
import FaceOverlay from '@/components/FaceOverlay.vue'

const store = useScreenStore()

// ---- 轮播数据 ----
const loading = ref(true)
const dataError = ref(false)

// ---- 当前展示索引 ----
const activeIndex = ref(0)
const isTransitioning = ref(false)

// ---- 人脸命中后的 timeline 轮播索引 ----
const hitTimelineIndex = ref(0)
let hitAdvanceTimer = null

// ---- 实时时钟 ----
const now = ref(new Date())
let clockTimer = null

const timeStr = computed(() => {
  const h = String(now.value.getHours()).padStart(2, '0')
  const m = String(now.value.getMinutes()).padStart(2, '0')
  const s = String(now.value.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
})

const dateStr = computed(() => {
  const d = now.value
  const weekMap = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekMap[d.getDay()]}`
})

// ---- 轮播项列表 ----
const items = computed(() => store.carouselData?.items || [])
const totalItems = computed(() => items.value.length)

// 当前展示项
const currentItem = computed(() => {
  if (totalItems.value === 0) return null
  return items.value[activeIndex.value]
})

// 当前媒体的第一张图片
const currentMedia = computed(() => {
  if (!currentItem.value?.mediaList?.length) return null
  return currentItem.value.mediaList.find(m => m.type === 1) || currentItem.value.mediaList[0]
})

// ---- 轮播控制 ----
let autoTimer = null
let autoPlayPausedByHit = false   // 标记：是否被人脸命中暂停（防止识别循环重置定时器）
const intervalMs = computed(() => (store.carouselData?.intervalSec || 8) * 1000)

function nextSlide() {
  if (totalItems.value <= 1) return
  isTransitioning.value = true
  setTimeout(() => {
    activeIndex.value = (activeIndex.value + 1) % totalItems.value
    setTimeout(() => {
      isTransitioning.value = false
    }, 50)
  }, 400)
}

function startAutoPlay() {
  stopAutoPlay()
  if (totalItems.value <= 1) return
  autoTimer = setInterval(nextSlide, intervalMs.value)
}

function stopAutoPlay() {
  if (autoTimer) {
    clearInterval(autoTimer)
    autoTimer = null
  }
}

// ---- 人脸命中 timeline 当前展示项 ----
const hitTimelineLen = computed(() => store.hitTimeline?.length || 0)
const currentTimelineItem = computed(() => {
  if (hitTimelineLen.value === 0) return null
  return store.hitTimeline[hitTimelineIndex.value]
})
const currentTimelineMedia = computed(() => {
  if (!currentTimelineItem.value?.media?.length) return null
  return currentTimelineItem.value.media.find(m => m.type === 1) || currentTimelineItem.value.media[0]
})

// 监听 item 总数变化，确保 activeIndex 不越界
watch(totalItems, (n) => {
  if (n > 0 && activeIndex.value >= n) {
    activeIndex.value = 0
  }
  startAutoPlay()
})

// 监听配置的 interval
watch(intervalMs, () => {
  startAutoPlay()
})

// ---- 人脸命中 → 播放专属 timeline（最多 2 轮） ----
let hitCycleCount = 0
const MAX_HIT_CYCLES = 2

function startHitTimeline() {
  stopHitTimeline()
  hitTimelineIndex.value = 0
  hitCycleCount = 0
  if (hitTimelineLen.value <= 1) return
  const ival = (store.carouselData?.intervalSec || 8) * 1000
  hitAdvanceTimer = setInterval(() => {
    hitTimelineIndex.value = (hitTimelineIndex.value + 1) % hitTimelineLen.value
    // 回到索引 0 表示完成一轮
    if (hitTimelineIndex.value === 0) {
      hitCycleCount++
      if (hitCycleCount >= MAX_HIT_CYCLES) {
        stopHitTimeline()
        store.resetFace()
      }
    }
  }, ival)
}

function stopHitTimeline() {
  if (hitAdvanceTimer) {
    clearInterval(hitAdvanceTimer)
    hitAdvanceTimer = null
  }
  hitCycleCount = 0
}

watch([() => store.faceState, () => store.hitVersion], ([state]) => {
  if (state === 'hit') {
    stopAutoPlay()
    stopHitTimeline()
    autoPlayPausedByHit = true
    // 短暂延迟等命中卡片展示后开始 timeline（与 HIT_INTRO_MS 对齐）
    setTimeout(() => startHitTimeline(), 2000)
  } else if (state === 'idle' && autoPlayPausedByHit) {
    // 仅当之前是被人脸命中暂停的，才恢复自动轮播
    // 避免人脸识别循环（scanning → idle 每 3s 一次）反复重置定时器
    stopHitTimeline()
    autoPlayPausedByHit = false
    startAutoPlay()
  }
})

// ---- 加载数据 ----
async function loadData() {
  loading.value = true
  dataError.value = false
  try {
    const data = await getCarousel()
    store.setCarouselData(data)
  } catch {
    // 降级：尝试从缓存加载
    const hit = store.loadFromCache()
    if (!hit) {
      dataError.value = true
    }
  } finally {
    loading.value = false
    startAutoPlay()
  }
}

// ---- 生命周期 ----
onMounted(() => {
  clockTimer = setInterval(() => { now.value = new Date() }, 1000)
  loadData()
})

onUnmounted(() => {
  clearInterval(clockTimer)
  stopAutoPlay()
  stopHitTimeline()
})

// ---- 头像滚动带去重 ----
const uniqueAlumni = computed(() => {
  const seen = new Set()
  return items.value.filter(item => {
    const key = item.alumniId
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
})
</script>

<template>
  <div class="carousel-page">
    <!-- 四角科技装饰 -->
    <div class="corner-decor tl" />
    <div class="corner-decor tr" />
    <div class="corner-decor bl" />
    <div class="corner-decor br" />

    <!-- 离线提示 -->
    <div v-if="store.isOffline" class="offline-badge">
      <span class="offline-dot" />
      离线数据 · 已缓存
    </div>

    <!-- 加载/错误状态 -->
    <template v-if="loading">
      <div class="center-message">
        <div class="loading-ring" />
        <p>正在加载…</p>
      </div>
    </template>

    <template v-else-if="dataError">
      <div class="center-message">
        <p style="font-size:24px;color:#93A7C9">数据加载失败</p>
        <p style="font-size:14px;color:rgba(147,167,201,0.6)">请检查网络连接</p>
      </div>
    </template>

    <template v-else-if="totalItems === 0">
      <div class="center-message">
        <p style="font-size:24px;color:#93A7C9">暂无轮播内容</p>
        <p style="font-size:14px;color:rgba(147,167,201,0.6)">请在管理后台配置轮播方案</p>
      </div>
    </template>

    <template v-else>
      <!-- ════ 顶部：校徽 + 标题 + 时钟 ════ -->
      <header class="top-bar">
        <div class="top-left">
          <span class="school-badge">🏛</span>
          <div class="title-group">
            <h1 class="main-title">校园荣誉展览馆</h1>
            <p class="sub-title">CAMPUS HALL OF HONOR</p>
          </div>
        </div>
        <div class="top-right">
          <div class="clock-time">{{ timeStr }}</div>
          <div class="clock-date">{{ dateStr }}</div>
        </div>
      </header>

      <!-- ════ 中央：大卡轮播 / 人脸命中 Timeline ════ -->
      <main class="carousel-stage">
        <!-- 默认轮播 -->
        <template v-if="store.faceState !== 'hit'">
          <Transition name="carousel-fade" mode="out-in">
            <div class="hero-card glass-card glow-border" :key="'c'+activeIndex" v-if="currentItem">
            <!-- 媒体背景图（半透明叠底） -->
            <div class="hero-media-bg" v-if="currentMedia">
              <img
                :src="currentMedia.thumbnail || currentMedia.url"
                :alt="currentItem.title"
              />
            </div>

            <div class="hero-content">
              <!-- 左侧头像/信息 -->
              <div class="hero-left">
                <div class="hero-avatar">
                  <img
                    v-if="currentItem.alumniAvatar"
                    :src="currentItem.alumniAvatar"
                    :alt="currentItem.alumniName"
                  />
                  <span v-else class="avatar-placeholder">
                    {{ (currentItem.alumniName || '?')[0] }}
                  </span>
                </div>
              </div>

              <!-- 右侧荣誉信息 -->
              <div class="hero-right">
                <div class="hero-badge">{{ store.carouselData?.name || '荣誉档案' }}</div>
                <h2 class="hero-title">{{ currentItem.title }}</h2>
                <div class="hero-meta">
                  <span class="hero-name">{{ currentItem.alumniName }}</span>
                  <span class="hero-divider">|</span>
                  <span>{{ currentItem.collegeName }}</span>
                  <span class="hero-divider">|</span>
                  <span>{{ currentItem.gradYear }} 届</span>
                </div>
                <p class="hero-desc" v-if="currentItem.content">
                  {{ currentItem.content.length > 120
                    ? currentItem.content.slice(0, 120) + '…'
                    : currentItem.content }}
                </p>

                <!-- 进度指示点 -->
                <div class="hero-dots">
                  <span
                    v-for="(_, i) in totalItems"
                    :key="i"
                    class="dot"
                    :class="{ active: i === activeIndex }"
                    @click="activeIndex = i; startAutoPlay()"
                  />
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </template>

      <!-- 人脸命中专属 Timeline 轮播 -->
      <template v-else>
        <Transition name="carousel-fade" mode="out-in">
          <div class="hero-card glass-card glow-border" :key="'t'+hitTimelineIndex" v-if="currentTimelineItem">
            <div class="hero-media-bg" v-if="currentTimelineMedia">
              <img
                :src="currentTimelineMedia.thumbnail || currentTimelineMedia.url"
                :alt="currentTimelineItem.title"
              />
            </div>

            <div class="hero-content">
              <div class="hero-left">
                <div class="hero-avatar">
                  <img
                    v-if="store.hitAlumni?.avatar"
                    :src="store.hitAlumni.avatar"
                    :alt="store.hitAlumni.name"
                  />
                  <span v-else class="avatar-placeholder">
                    {{ (store.hitAlumni?.name || '?')[0] }}
                  </span>
                </div>
              </div>

              <div class="hero-right">
                <div class="hero-badge">
                  {{ currentTimelineItem.categoryName || '荣誉档案' }}
                </div>
                <h2 class="hero-title">{{ currentTimelineItem.title }}</h2>
                <div class="hero-meta">
                  <span class="hero-name">{{ store.hitAlumni?.name }}</span>
                  <span class="hero-divider">|</span>
                  <span>{{ store.hitAlumni?.collegeName }}</span>
                  <span class="hero-divider">|</span>
                  <span>{{ currentTimelineItem.eventDate }}</span>
                </div>
                <p class="hero-desc" v-if="currentTimelineItem.content">
                  {{ currentTimelineItem.content.length > 120
                    ? currentTimelineItem.content.slice(0, 120) + '…'
                    : currentTimelineItem.content }}
                </p>

                <div class="hero-dots">
                  <span
                    v-for="(_, i) in hitTimelineLen"
                    :key="i"
                    class="dot"
                    :class="{ active: i === hitTimelineIndex }"
                  />
                </div>
              </div>
            </div>
          </div>
        </Transition>
      </template>
    </main>

    <!-- ════ 底部：头像滚动带 ════ -->
      <footer class="bottom-strip" v-if="uniqueAlumni.length">
        <div class="strip-scroll">
          <!-- 双份以实现无缝滚动 -->
          <div class="strip-track" v-for="dup in [0, 1]" :key="dup">
            <div
              v-for="item in uniqueAlumni"
              :key="`${dup}-${item.alumniId}`"
              class="strip-avatar"
              :class="{ 'is-active': item.alumniId === currentItem?.alumniId }"
            >
              <img v-if="item.alumniAvatar" :src="item.alumniAvatar" :alt="item.alumniName" />
              <span v-else class="strip-placeholder">{{ (item.alumniName || '?')[0] }}</span>
              <span class="strip-name">{{ item.alumniName }}</span>
            </div>
          </div>
        </div>
      </footer>
    </template>

    <!-- ════ 人脸识别浮层 ════ -->
    <FaceOverlay />
  </div>
</template>

<style scoped>
.carousel-page {
  position: relative;
  width: 1920px; height: 1080px;
  overflow: hidden;
  z-index: 1;
}

/* ---- 离线角标 ---- */
.offline-badge {
  position: absolute; top: 70px; right: 24px; z-index: 10;
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px;
  background: rgba(229, 72, 77, 0.2);
  border: 1px solid rgba(229, 72, 77, 0.4);
  border-radius: 20px;
  font-size: 12px; color: #E5484D;
}

.offline-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: #E5484D;
  animation: blink 1.5s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ---- 加载/空态 ---- */
.center-message {
  position: absolute; inset: 0;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 8px; color: #93A7C9;
}

.loading-ring {
  width: 48px; height: 48px;
  border: 3px solid rgba(30, 139, 255, 0.2);
  border-top-color: #1E8BFF;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ---- 顶栏 ---- */
.top-bar {
  position: absolute; top: 0; left: 0; right: 0;
  height: 100px;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 60px;
  background: linear-gradient(to bottom, rgba(10,19,38,0.9) 0%, transparent 100%);
  z-index: 5;
}

.top-left { display: flex; align-items: center; gap: 20px; }

.school-badge { font-size: 42px; }

.title-group { display: flex; flex-direction: column; }

.main-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 30px; color: #EAF2FF;
  margin: 0; letter-spacing: 6px;
}

.sub-title {
  font-family: 'Rajdhani-SemiBold', sans-serif;
  font-size: 16px; color: #E6B450;
  margin: 2px 0 0; letter-spacing: 4px;
}

.top-right { text-align: right; }

.clock-time {
  font-family: 'din-bold-2', sans-serif;
  font-size: 52px; color: #1E8BFF;
  line-height: 1; letter-spacing: 4px;
}

.clock-date {
  font-size: 14px; color: #93A7C9;
  margin-top: 4px; letter-spacing: 2px;
}

/* ---- 中央轮播 ---- */
.carousel-stage {
  position: absolute; inset: 100px 60px 130px;
  display: flex; align-items: center; justify-content: center;
}

.hero-card {
  position: relative;
  width: 100%; height: 100%;
  display: flex;
  overflow: hidden;
}

.hero-media-bg {
  position: absolute; inset: 0;
}

.hero-media-bg img {
  width: 100%; height: 100%;
  object-fit: cover;
  opacity: 0.25;
  filter: blur(2px);
}

.hero-content {
  position: relative; z-index: 1;
  display: flex; align-items: center;
  width: 100%; height: 100%;
  padding: 60px 80px; gap: 60px;
}

.hero-left { flex-shrink: 0; }

.hero-avatar {
  width: 240px; height: 240px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid rgba(120, 170, 255, 0.5);
  box-shadow: 0 0 40px rgba(30, 139, 255, 0.3);
  display: flex; align-items: center; justify-content: center;
  background: rgba(30, 139, 255, 0.1);
}

.hero-avatar img {
  width: 100%; height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 80px; color: #1E8BFF;
}

.hero-right { flex: 1; min-width: 0; }

.hero-badge {
  display: inline-block;
  padding: 4px 16px;
  background: rgba(230, 180, 80, 0.2);
  border: 1px solid rgba(230, 180, 80, 0.4);
  border-radius: 20px;
  font-size: 13px; color: #E6B450;
  margin-bottom: 16px;
  font-family: 'Rajdhani-SemiBold', sans-serif;
  letter-spacing: 2px;
}

.hero-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 42px; color: #fff;
  margin: 0 0 16px; line-height: 1.2;
}

.hero-meta {
  display: flex; align-items: center; gap: 12px;
  font-size: 18px; color: #93A7C9;
  margin-bottom: 20px;
}

.hero-name {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  color: #EAF2FF;
}

.hero-divider { color: rgba(120, 170, 255, 0.3); }

.hero-desc {
  font-size: 16px; color: #93A7C9;
  line-height: 1.8; max-width: 700px;
}

.hero-dots {
  display: flex; gap: 10px; margin-top: 32px;
}

.dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  cursor: pointer; transition: all 0.3s;
}

.dot.active {
  background: #1E8BFF;
  box-shadow: 0 0 8px rgba(30, 139, 255, 0.6);
}

/* ---- 轮播过渡动画 ---- */
.carousel-fade-enter-active {
  transition: all 0.6s ease-out;
}

.carousel-fade-leave-active {
  transition: all 0.4s ease-in;
}

.carousel-fade-enter-from {
  opacity: 0;
  transform: scale(0.96);
}

.carousel-fade-leave-to {
  opacity: 0;
  transform: scale(1.02);
}

/* ---- 底部头像滚动带 ---- */
.bottom-strip {
  position: absolute; bottom: 0; left: 0; right: 0;
  height: 110px;
  background: rgba(0, 0, 0, 0.25);
  border-top: 1px solid rgba(120, 170, 255, 0.15);
  overflow: hidden;
  z-index: 5;
}

.strip-scroll {
  display: flex;
  width: 100%; height: 100%;
  align-items: center;
}

.strip-track {
  display: flex; gap: 24px;
  padding: 0 24px;
  animation: scroll-strip 30s linear infinite;
  flex-shrink: 0;
}

@keyframes scroll-strip {
  from { transform: translateX(0); }
  to { transform: translateX(-100%); }
}

/* 鼠标悬停时暂停滚动 */
.bottom-strip:hover .strip-track {
  animation-play-state: paused;
}

.strip-avatar {
  display: flex; flex-direction: column;
  align-items: center; gap: 6px;
  transition: transform 0.3s;
}

.strip-avatar:hover { transform: scale(1.15); }

.strip-avatar.is-active {
  transform: scale(1.25);
}

.strip-avatar img {
  width: 54px; height: 54px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid transparent;
  transition: border-color 0.3s, box-shadow 0.3s;
}

.strip-avatar.is-active img {
  border-color: #E6B450;
  box-shadow: 0 0 16px rgba(230, 180, 80, 0.5);
}

.strip-placeholder {
  width: 54px; height: 54px;
  border-radius: 50%;
  background: rgba(30, 139, 255, 0.15);
  display: flex; align-items: center; justify-content: center;
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px; color: #1E8BFF;
}

.strip-name {
  font-size: 11px; color: #93A7C9;
  max-width: 72px; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap;
}
</style>

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

// ---- 底部导航可见数量 ----
const maxNavVisible = 9
const navStartIndex = computed(() => {
  if (uniqueAlumni.value.length <= maxNavVisible) return 0
  const half = Math.floor(maxNavVisible / 2)
  return Math.max(0, Math.min(activeIndex.value - half, uniqueAlumni.value.length - maxNavVisible))
})
const visibleNavItems = computed(() => {
  return uniqueAlumni.value.slice(navStartIndex.value, navStartIndex.value + maxNavVisible)
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
      <!-- ════ 顶部 Header ════ -->
      <header class="top-bar">
        <div class="top-left">
          <div class="school-badge-wrap">
            <img src="@/assets/images/school_badge.png" class="school-badge" alt="福软校徽" />
          </div>
          <div class="title-group">
            <h1 class="main-title">福软荣誉展览馆</h1>
            <p class="sub-title">FUROAN HALL OF HONOR</p>
          </div>
        </div>
        <div class="top-right">
          <div class="clock-time">{{ timeStr }}</div>
          <div class="clock-date">{{ dateStr }}</div>
        </div>
      </header>

      <!-- ════ 中央 MainPanel ════ -->
      <main class="main-stage">
        <!-- 默认轮播 -->
        <template v-if="store.faceState !== 'hit'">
          <Transition name="panel-fade" mode="out-in">
            <div class="main-panel" :key="'c'+activeIndex" v-if="currentItem">

              <!-- ══ 扫描线 ══ -->
              <div class="scan-line" />

              <!-- ══ 左栏：头像 + 标签 + 导航点 ══ -->
              <div class="col col-left">
                <div class="avatar-wrap">
                  <div class="avatar-ring" />
                  <div class="avatar-inner">
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

                <div class="left-tags">
                  <span class="tag tag-gold">校园荣誉人物</span>
                  <span class="tag tag-blue">{{ currentItem.gradYear }}届</span>
                  <span class="tag tag-line">{{ currentItem.collegeName }}</span>
                </div>

                <!-- 导航指示点 -->
                <div class="nav-dots" v-if="totalItems > 1">
                  <span
                    v-for="(_, i) in totalItems"
                    :key="i"
                    class="dot"
                    :class="{ active: i === activeIndex }"
                    @click="activeIndex = i; startAutoPlay()"
                  />
                </div>
              </div>

              <!-- ══ 中栏：姓名 + 事迹标题 + 正文 + 成就卡片 ══ -->
              <div class="col col-center">
                <h2 class="person-name">{{ currentItem.alumniName }}</h2>
                <div class="meta-row">
                  <span class="meta-badge">{{ store.carouselData?.name || '荣誉档案' }}</span>
                  <span class="meta-divider" />
                  <span>{{ currentItem.collegeName }}</span>
                  <span class="meta-divider" />
                  <span>{{ currentItem.gradYear }} 届</span>
                </div>

                <div class="section-header">
                  <span class="section-line" />
                  <span class="section-label">主 要 事 迹</span>
                  <span class="section-line" />
                </div>

                <h3 class="story-title">{{ currentItem.title }}</h3>
                <p class="story-text" v-if="currentItem.content">
                  {{ currentItem.content.length > 400
                    ? currentItem.content.slice(0, 400) + '…'
                    : currentItem.content }}
                </p>

                <!-- 关键成就卡片 -->
                <div class="achieve-cards">
                  <div class="achieve-card">
                    <div class="achieve-icon">🏆</div>
                    <div class="achieve-label">荣誉奖项</div>
                    <div class="achieve-value">{{ currentItem.title }}</div>
                  </div>
                  <div class="achieve-card">
                    <div class="achieve-icon">🎓</div>
                    <div class="achieve-label">毕业信息</div>
                    <div class="achieve-value">{{ currentItem.gradYear }}届 · {{ currentItem.collegeName }}</div>
                  </div>
                  <div class="achieve-card">
                    <div class="achieve-icon">📅</div>
                    <div class="achieve-label">获奖时间</div>
                    <div class="achieve-value">{{ currentItem.eventDate || '荣誉档案' }}</div>
                  </div>
                </div>
              </div>

              <!-- ══ 右栏：人物照片 ══ -->
              <div class="col col-right">
                <div class="photo-card" v-if="currentMedia">
                  <div class="photo-glow" />
                  <img
                    :src="currentMedia.thumbnail || currentMedia.url"
                    :alt="currentItem.title"
                  />
                  <div class="photo-label">
                    <span>{{ currentItem.gradYear }}届</span>
                  </div>
                </div>
                <div class="photo-card photo-empty" v-else>
                  <div class="photo-glow" />
                  <div class="empty-icon"><img src="@/assets/images/school_badge.png" alt="福软校徽" /></div>
                  <div class="photo-label">
                    <span>{{ currentItem.gradYear }}届</span>
                  </div>
                </div>
              </div>

            </div>
          </Transition>
        </template>

        <!-- 人脸命中专属 Timeline 轮播 -->
        <template v-else>
          <Transition name="panel-fade" mode="out-in">
            <div class="main-panel" :key="'t'+hitTimelineIndex" v-if="currentTimelineItem">

              <div class="scan-line" />

              <div class="col col-left">
                <div class="avatar-wrap">
                  <div class="avatar-ring" />
                  <div class="avatar-inner">
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

                <div class="left-tags">
                  <span class="tag tag-gold">已识别校友</span>
                  <span class="tag tag-blue">{{ store.hitAlumni?.gradYear }}届</span>
                  <span class="tag tag-line">{{ store.hitAlumni?.collegeName }}</span>
                </div>

                <div class="nav-dots" v-if="hitTimelineLen > 1">
                  <span
                    v-for="(_, i) in hitTimelineLen"
                    :key="i"
                    class="dot"
                    :class="{ active: i === hitTimelineIndex }"
                  />
                </div>
              </div>

              <div class="col col-center">
                <h2 class="person-name">{{ store.hitAlumni?.name }}</h2>
                <div class="meta-row">
                  <span class="meta-badge">{{ currentTimelineItem.categoryName || '荣誉档案' }}</span>
                  <span class="meta-divider" />
                  <span>{{ store.hitAlumni?.collegeName }}</span>
                  <span class="meta-divider" />
                  <span>{{ currentTimelineItem.eventDate }}</span>
                </div>

                <div class="section-header">
                  <span class="section-line" />
                  <span class="section-label">主 要 事 迹</span>
                  <span class="section-line" />
                </div>

                <h3 class="story-title">{{ currentTimelineItem.title }}</h3>
                <p class="story-text" v-if="currentTimelineItem.content">
                  {{ currentTimelineItem.content.length > 300
                    ? currentTimelineItem.content.slice(0, 300) + '…'
                    : currentTimelineItem.content }}
                </p>

                <div class="achieve-cards">
                  <div class="achieve-card">
                    <div class="achieve-icon">🏆</div>
                    <div class="achieve-label">荣誉奖项</div>
                    <div class="achieve-value">{{ currentTimelineItem.title }}</div>
                  </div>
                  <div class="achieve-card">
                    <div class="achieve-icon">🎓</div>
                    <div class="achieve-label">毕业信息</div>
                    <div class="achieve-value">{{ store.hitAlumni?.gradYear }}届 · {{ store.hitAlumni?.collegeName }}</div>
                  </div>
                  <div class="achieve-card">
                    <div class="achieve-icon">📅</div>
                    <div class="achieve-label">获奖时间</div>
                    <div class="achieve-value">{{ currentTimelineItem.eventDate || '荣誉档案' }}</div>
                  </div>
                </div>
              </div>

              <div class="col col-right">
                <div class="photo-card" v-if="currentTimelineMedia">
                  <div class="photo-glow" />
                  <img
                    :src="currentTimelineMedia.thumbnail || currentTimelineMedia.url"
                    :alt="currentTimelineItem.title"
                  />
                  <div class="photo-label">
                    <span>{{ store.hitAlumni?.gradYear }}届</span>
                  </div>
                </div>
                <div class="photo-card photo-empty" v-else>
                  <div class="photo-glow" />
                  <div class="empty-icon"><img src="@/assets/images/school_badge.png" alt="福软校徽" /></div>
                  <div class="photo-label">
                    <span>{{ store.hitAlumni?.gradYear }}届</span>
                  </div>
                </div>
              </div>

            </div>
          </Transition>
        </template>
      </main>

      <!-- ════ 底部版权 ════ -->
      <div class="copyright-bar">
        <span>Copyright © 2026 福州软件职业技术学院 陈灿</span>
      </div>

      <!-- ════ 底部 FooterNav：人物导航 ════ -->
      <footer class="footer-nav" v-if="uniqueAlumni.length > 1">
        <div class="nav-inner">
          <div
            v-for="item in visibleNavItems"
            :key="item.alumniId"
            class="nav-item"
            :class="{ active: item.alumniId === currentItem?.alumniId }"
            @click="activeIndex = items.findIndex(i => i.alumniId === item.alumniId); startAutoPlay()"
          >
            <div class="nav-avatar-ring">
              <img v-if="item.alumniAvatar" :src="item.alumniAvatar" :alt="item.alumniName" />
              <span v-else class="nav-placeholder">{{ (item.alumniName || '?')[0] }}</span>
            </div>
            <span class="nav-name">{{ item.alumniName }}</span>
          </div>
        </div>
      </footer>
    </template>

    <!-- ════ 人脸识别浮层 ════ -->
    <FaceOverlay />
  </div>
</template>

<style scoped>
/* ================================================================
   ROOT
   ================================================================ */
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

/* ================================================================
   TOP BAR (keep existing)
   ================================================================ */
.top-bar {
  position: absolute; top: 0; left: 0; right: 0;
  height: 90px;
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 60px;
  background: linear-gradient(to bottom, rgba(10,19,38,0.92) 0%, transparent 100%);
  z-index: 5;
}

.top-left { display: flex; align-items: center; gap: 20px; }
.school-badge-wrap {
  width: 64px; height: 64px;
  border-radius: 50%;
  background: radial-gradient(circle,
    rgba(255, 255, 255, 0.2) 0%,
    rgba(255, 255, 255, 0.08) 50%,
    transparent 70%
  );
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}

.school-badge {
  width: 48px; height: 48px;
  object-fit: contain;
}
.title-group { display: flex; flex-direction: column; }

.main-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 32px; color: #EAF2FF;
  margin: 0; letter-spacing: 6px;
}

.sub-title {
  font-family: 'Rajdhani-SemiBold', sans-serif;
  font-size: 17px; color: #E6B450;
  margin: 2px 0 0; letter-spacing: 4px;
}

.top-right { text-align: right; }

.clock-time {
  font-family: 'din-bold-2', sans-serif;
  font-size: 52px; color: #1E8BFF;
  line-height: 1; letter-spacing: 4px;
}

.clock-date {
  font-size: 16px; color: #93A7C9;
  margin-top: 4px; letter-spacing: 2px;
}

/* ================================================================
   MAIN STAGE
   ================================================================ */
.main-stage {
  position: absolute;
  inset: 108px 52px 128px;
  display: flex; align-items: center; justify-content: center;
}

/* ================================================================
   MAIN PANEL — 三栏布局
   ================================================================ */
.main-panel {
  position: relative;
  width: 100%; height: 100%;
  display: flex;
  align-items: center;
  gap: 34px;
  padding: 40px 52px;
  background: rgba(8, 25, 55, 0.72);
  border: 1px solid rgba(120, 170, 255, 0.28);
  border-radius: 20px;
  overflow: hidden;
  /* 发光边框 + 内阴影 */
  box-shadow:
    0 0 1px rgba(120, 170, 255, 0.45),
    0 0 12px rgba(30, 139, 255, 0.12),
    inset 0 1px 0 rgba(255,255,255,0.04);
  backdrop-filter: blur(8px);
}

/* 四角装饰短线 */
.main-panel::before,
.main-panel::after {
  content: '';
  position: absolute;
  width: 28px; height: 28px;
  pointer-events: none;
  z-index: 0;
}
.main-panel::before {
  top: 14px; left: 14px;
  border-top: 2px solid rgba(30, 139, 255, 0.35);
  border-left: 2px solid rgba(30, 139, 255, 0.35);
}
.main-panel::after {
  bottom: 14px; right: 14px;
  border-bottom: 2px solid rgba(30, 139, 255, 0.35);
  border-right: 2px solid rgba(30, 139, 255, 0.35);
}

/* ---- 扫描线 ---- */
.scan-line {
  position: absolute; top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(30, 139, 255, 0.15) 20%,
    rgba(30, 139, 255, 0.5) 50%,
    rgba(30, 139, 255, 0.15) 80%,
    transparent 100%
  );
  animation: scan-shift 4s ease-in-out infinite;
  z-index: 0; pointer-events: none;
}

@keyframes scan-shift {
  0%, 100% { top: 0; opacity: 0.4; }
  50% { top: calc(100% - 1px); opacity: 0.8; }
}

/* ================================================================
   THREE COLUMNS
   ================================================================ */
.col {
  position: relative;
  z-index: 1;
  display: flex; flex-direction: column;
}

/* 左栏 20% */
.col-left {
  width: 20%;
  align-items: center;
  justify-content: center;
  gap: 26px;
}

/* 中栏 55% */
.col-center {
  width: 55%;
  justify-content: center;
  gap: 0;
}

/* 右栏 25% */
.col-right {
  width: 25%;
  align-items: center;
  justify-content: center;
}

/* ================================================================
   LEFT COLUMN — 头像 + 标签 + 导航点
   ================================================================ */
.col-left {
  position: relative;
}
/* 左侧信息区微弱光晕 */
.col-left::before {
  content: '';
  position: absolute; top: 10%; left: 10%;
  width: 80%; height: 80%;
  background: radial-gradient(ellipse at 50% 40%,
    rgba(30, 139, 255, 0.07) 0%,
    rgba(30, 139, 255, 0.02) 50%,
    transparent 70%
  );
  pointer-events: none; z-index: 0;
}

.avatar-wrap {
  position: relative;
  width: 200px; height: 200px;
  display: flex; align-items: center; justify-content: center;
  z-index: 1;
}

/* 旋转光环 */
.avatar-ring {
  position: absolute; inset: -10px;
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: rgba(30, 139, 255, 0.55);
  border-right-color: rgba(30, 139, 255, 0.3);
  animation: ring-spin 8s linear infinite;
}

@keyframes ring-spin {
  to { transform: rotate(360deg); }
}

.avatar-inner {
  width: 176px; height: 176px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid rgba(120, 170, 255, 0.6);
  box-shadow: 0 0 36px rgba(30, 139, 255, 0.3), 0 0 60px rgba(30, 139, 255, 0.1);
  display: flex; align-items: center; justify-content: center;
  background: rgba(30, 139, 255, 0.08);
}

.avatar-inner img {
  width: 100%; height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 62px; color: #1E8BFF;
}

/* 标签 */
.left-tags {
  display: flex; flex-direction: column;
  align-items: center; gap: 10px;
}

.tag {
  display: inline-block;
  padding: 4px 16px;
  border-radius: 14px;
  font-size: 15px;
  letter-spacing: 1px;
  white-space: nowrap;
}

.tag-gold {
  background: rgba(230, 180, 80, 0.2);
  border: 1px solid rgba(230, 180, 80, 0.4);
  color: #E6B450;
  font-family: 'Rajdhani-SemiBold', sans-serif;
  font-size: 16px;
  letter-spacing: 2px;
}

.tag-blue {
  background: rgba(30, 139, 255, 0.18);
  border: 1px solid rgba(30, 139, 255, 0.35);
  color: #89BAFF;
  font-size: 14px;
}

.tag-line {
  background: rgba(147, 167, 201, 0.12);
  border: 1px solid rgba(147, 167, 201, 0.22);
  color: #93A7C9;
  font-size: 13px;
}

/* 导航点 */
.nav-dots {
  display: flex; gap: 10px;
  margin-top: 6px;
}

.dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  background: rgba(255,255,255,0.18);
  cursor: pointer;
  transition: all 0.3s ease;
}

.dot:hover { background: rgba(30, 139, 255, 0.4); }

.dot.active {
  background: #1E8BFF;
  box-shadow: 0 0 12px rgba(30, 139, 255, 0.7);
  transform: scale(1.4);
}

/* ================================================================
   CENTER COLUMN — 姓名 + 事迹
   ================================================================ */
.person-name {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 44px; font-weight: 700; color: #FFFFFF;
  margin: 0 0 12px;
  letter-spacing: 4px;
  text-shadow: 0 0 24px rgba(30, 139, 255, 0.35);
}

.meta-row {
  display: flex; align-items: center; gap: 10px;
  font-size: 17px; color: #93A7C9;
  margin-bottom: 24px;
}

.meta-badge {
  padding: 3px 14px;
  background: rgba(230, 180, 80, 0.18);
  border: 1px solid rgba(230, 180, 80, 0.35);
  border-radius: 12px;
  font-size: 15px; color: #E6B450;
  font-family: 'Rajdhani-SemiBold', sans-serif;
  letter-spacing: 2px;
}

.meta-divider {
  width: 1px; height: 14px;
  background: rgba(120, 170, 255, 0.3);
}

/* 事迹模块标题 */
.section-header {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 18px;
}

.section-line {
  flex: 1; height: 1px;
  background: linear-gradient(90deg,
    rgba(30, 139, 255, 0.55),
    rgba(30, 139, 255, 0.05)
  );
}
.section-line:last-child {
  background: linear-gradient(90deg,
    rgba(30, 139, 255, 0.05),
    rgba(30, 139, 255, 0.55)
  );
}

.section-label {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px; font-weight: 600; color: #78AAFF;
  letter-spacing: 8px;
  white-space: nowrap;
}

/* 事迹标题 */
.story-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 24px; font-weight: 600; color: #EAF2FF;
  margin: 0 0 14px;
  line-height: 1.4;
}

/* 事迹正文 */
.story-text {
  font-size: 17px; color: #93A7C9;
  line-height: 1.9;
  margin: 0 0 22px;
  text-align: justify;
  max-width: 680px;
}

/* ---- 成就卡片 ---- */
.achieve-cards {
  display: flex; gap: 18px;
  margin-top: 6px;
}

.achieve-card {
  flex: 1;
  min-height: 82px;
  padding: 18px 16px;
  background: rgba(30, 139, 255, 0.06);
  border: 1px solid rgba(120, 170, 255, 0.15);
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: default;
  display: flex; flex-direction: column;
}

.achieve-card:hover {
  background: rgba(30, 139, 255, 0.12);
  border-color: rgba(120, 170, 255, 0.4);
  box-shadow: 0 0 20px rgba(30, 139, 255, 0.18);
  transform: translateY(-2px);
}

.achieve-icon {
  font-size: 26px; margin-bottom: 6px;
}

.achieve-label {
  font-size: 13px; color: #78AAFF;
  letter-spacing: 2px;
  margin-bottom: 4px;
  font-family: 'Rajdhani-SemiBold', sans-serif;
}

.achieve-value {
  font-size: 15px; color: #BCCFE0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ================================================================
   RIGHT COLUMN — 人物照片
   ================================================================ */
.photo-card {
  position: relative;
  width: 100%;
  max-width: 370px;
  aspect-ratio: 3 / 4;
  border-radius: 16px;
  overflow: hidden;
  border: 3px solid rgba(30, 139, 255, 0.5);
  box-shadow:
    0 0 32px rgba(30, 139, 255, 0.25),
    0 0 60px rgba(30, 139, 255, 0.08),
    0 8px 48px rgba(0, 0, 0, 0.55);
  background: rgba(10, 19, 38, 0.6);
  display: flex; align-items: center; justify-content: center;
}

/* 照片后光晕 */
.photo-glow {
  position: absolute; inset: -50px;
  background: radial-gradient(ellipse at center,
    rgba(30, 139, 255, 0.2) 0%,
    rgba(30, 139, 255, 0.06) 40%,
    transparent 70%
  );
  pointer-events: none;
  animation: glow-pulse 3s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.photo-card img {
  position: relative; z-index: 1;
  width: 100%; height: 100%;
  object-fit: cover;
}

.photo-empty {
  display: flex; align-items: center; justify-content: center;
}

.empty-icon {
  font-size: 80px;
  opacity: 0.4;
}

.empty-icon img {
  width: 80px; height: 80px;
  object-fit: contain;
}

.photo-label {
  position: absolute; bottom: 14px; left: 50%;
  transform: translateX(-50%);
  z-index: 2;
  padding: 5px 18px;
  background: rgba(10, 19, 38, 0.85);
  border: 1px solid rgba(230, 180, 80, 0.4);
  border-radius: 12px;
  font-size: 15px; color: #E6B450;
  font-family: 'Rajdhani-SemiBold', sans-serif;
  letter-spacing: 2px;
  white-space: nowrap;
}

/* ================================================================
   PANEL TRANSITION
   ================================================================ */
.panel-fade-enter-active {
  transition: all 0.45s cubic-bezier(0.16, 1, 0.3, 1);
}
.panel-fade-leave-active {
  transition: all 0.3s ease-in;
}
.panel-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ================================================================
   FOOTER NAV — 人物导航
   ================================================================ */
.footer-nav {
  position: absolute; bottom: 0; left: 0; right: 0;
  height: 126px;
  display: flex; align-items: center; justify-content: center;
  padding: 0 60px;
  z-index: 5;
  background: linear-gradient(to top, rgba(8, 18, 38, 0.92) 0%, transparent 100%);
}

.nav-inner {
  display: flex; align-items: flex-end; gap: 24px;
  padding: 0 20px;
}

.nav-item {
  display: flex; flex-direction: column;
  align-items: center; gap: 7px;
  cursor: pointer;
  transition: all 0.3s ease;
  opacity: 0.5;
}

.nav-item:hover {
  opacity: 0.8;
  transform: translateY(-3px);
}

.nav-item.active {
  opacity: 1;
  transform: translateY(-8px);
}

.nav-avatar-ring {
  width: 64px; height: 64px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(120, 170, 255, 0.25);
  transition: all 0.35s ease;
  display: flex; align-items: center; justify-content: center;
  background: rgba(30, 139, 255, 0.08);
}

.nav-item.active .nav-avatar-ring {
  width: 74px; height: 74px;
  border-color: #1E8BFF;
  box-shadow:
    0 0 20px rgba(30, 139, 255, 0.55),
    0 0 40px rgba(30, 139, 255, 0.25),
    0 0 60px rgba(30, 139, 255, 0.1);
}

.nav-avatar-ring img {
  width: 100%; height: 100%;
  object-fit: cover;
}

.nav-placeholder {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 24px; color: #1E8BFF;
}

.nav-name {
  font-size: 13px; color: #93A7C9;
  max-width: 80px; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap;
  transition: color 0.3s;
}

.nav-item.active .nav-name {
  color: #EAF2FF;
}

/* ================================================================
   COPYRIGHT
   ================================================================ */
.copyright-bar {
  position: absolute; bottom: 134px; left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  font-size: 13px; color: rgba(147, 167, 201, 0.4);
  letter-spacing: 1px;
  white-space: nowrap;
  pointer-events: none;
}
</style>

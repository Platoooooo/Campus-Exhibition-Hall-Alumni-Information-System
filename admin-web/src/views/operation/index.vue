<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArchivePage } from '@/api/archive'
import { listAllCategories } from '@/api/category'
import {
  publishArchive, unpublishArchive, setTop, setDisplaySort,
  listCarousels, getCarousel, createCarousel, updateCarousel, deleteCarousel,
  addCarouselItem, removeCarouselItem, sortCarouselItems
} from '@/api/operation'

// ---- Tab ----
const activeTab = ref('publish')

// ============================
//  Section 1: 上架管理
// ============================
const gridLoading = ref(false)
const approvedList = ref([])  // 所有 approved/published/unpublished 档案

const statusLabels = {
  approved: '已入库', published: '已上架', unpublished: '已下架'
}

async function loadApprovedArchives() {
  gridLoading.value = true
  try {
    // 分两次查：已入库 + 已上架 + 已下架
    const [r1] = await Promise.all([
      getArchivePage({ status: 'approved', pageNum: 1, pageSize: 200 }),
    ])
    approvedList.value = (r1.records || []).filter(
      a => ['approved', 'published', 'unpublished'].includes(a.status)
    )
    // 按 displaySort 排序
    approvedList.value.sort((a, b) => (a.displaySort ?? 0) - (b.displaySort ?? 0))
  } finally {
    gridLoading.value = false
  }
}

/** 上下架开关 */
async function togglePublish(row) {
  try {
    if (row.status === 'published') {
      await unpublishArchive(row.id)
      row.status = 'unpublished'
      ElMessage.success('已下架')
    } else {
      await publishArchive(row.id)
      row.status = 'published'
      ElMessage.success('已上架')
    }
  } catch { /* ignore */ }
}

/** 置顶切换 */
async function toggleTop(row) {
  const newVal = row.isTop ? 0 : 1
  await setTop(row.id, newVal)
  row.isTop = newVal
  ElMessage.success(newVal ? '已置顶' : '已取消置顶')
}

// ---- 拖拽排序 ----
const dragSrcIdx = ref(-1)

function onGridDragStart(index) { dragSrcIdx.value = index }
function onGridDragOver(e) { e.preventDefault() }

async function onGridDrop(targetIdx) {
  if (dragSrcIdx.value < 0 || dragSrcIdx.value === targetIdx) return
  const items = [...approvedList.value]
  const [moved] = items.splice(dragSrcIdx.value, 1)
  items.splice(targetIdx, 0, moved)

  // 更新本地排序
  items.forEach((item, i) => { item.displaySort = i + 1 })
  approvedList.value = items

  // 持久化排序
  try {
    for (const item of items) {
      await setDisplaySort(item.id, item.displaySort)
    }
  } catch { /* ignore */ }
  dragSrcIdx.value = -1
}

// ============================
//  Section 2: 轮播配置
// ============================
const carouselLoading = ref(false)
const carousels = ref([])
const selectedCarouselId = ref(null)
const selectedCarousel = ref(null)
const carouselFormVisible = ref(false)
const carouselFormRef = ref(null)
const carouselSaving = ref(false)

const carouselForm = reactive({
  name: '', intervalSec: 8, effect: 'fade', orderType: 'sort', isDefault: 0
})

// Published archives for adding to carousel pool
const publishedArchives = ref([])

async function loadCarousels() {
  carouselLoading.value = true
  try {
    carousels.value = await listCarousels()
    if (carousels.value.length && !selectedCarouselId.value) {
      selectedCarouselId.value = carousels.value[0].id
      await selectCarousel(carousels.value[0].id)
    }
  } finally {
    carouselLoading.value = false
  }
}

async function selectCarousel(id) {
  selectedCarouselId.value = id
  selectedCarousel.value = await getCarousel(id)
}

async function loadPublishedPool() {
  try {
    const res = await getArchivePage({ status: 'published', pageNum: 1, pageSize: 200 })
    publishedArchives.value = res.records || []
  } catch { /* ignore */ }
}

// 新建/编辑轮播方案
function openCarouselForm(carousel) {
  if (carousel) {
    Object.assign(carouselForm, {
      name: carousel.name, intervalSec: carousel.intervalSec,
      effect: carousel.effect || 'fade',
      orderType: carousel.orderType || 'sort',
      isDefault: carousel.isDefault ?? 0
    })
    carouselForm.editId = carousel.id
  } else {
    Object.assign(carouselForm, {
      name: '', intervalSec: 8, effect: 'fade', orderType: 'sort', isDefault: 0
    })
    carouselForm.editId = null
  }
  carouselFormVisible.value = true
}

async function saveCarousel() {
  carouselSaving.value = true
  try {
    const payload = {
      name: carouselForm.name,
      intervalSec: carouselForm.intervalSec,
      effect: carouselForm.effect,
      orderType: carouselForm.orderType,
      isDefault: carouselForm.isDefault
    }
    if (carouselForm.editId) {
      await updateCarousel(carouselForm.editId, payload)
      ElMessage.success('方案已更新')
    } else {
      await createCarousel(payload)
      ElMessage.success('方案已创建')
    }
    carouselFormVisible.value = false
    await loadCarousels()
  } finally {
    carouselSaving.value = false
  }
}

async function handleDeleteCarousel(id) {
  try {
    await ElMessageBox.confirm('确定删除该轮播方案吗？', '确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteCarousel(id)
    if (selectedCarouselId.value === id) {
      selectedCarouselId.value = null
      selectedCarousel.value = null
    }
    ElMessage.success('已删除')
    await loadCarousels()
  } catch { /* cancel */ }
}

// 添加档案到轮播池
async function addToCarousel(archive) {
  if (!selectedCarouselId.value) { ElMessage.warning('请先选择轮播方案'); return }
  // 检查是否已在列表中
  if (selectedCarousel.value?.items?.some(i => i.archiveId === archive.id)) {
    ElMessage.warning('该档案已在轮播池中')
    return
  }
  try {
    await addCarouselItem(selectedCarouselId.value, {
      archiveId: archive.id,
      alumniId: archive.alumniId
    })
    await selectCarousel(selectedCarouselId.value)
    ElMessage.success('已添加到轮播')
  } catch { /* ignore */ }
}

// 从轮播池移除
async function removeFromCarousel(itemId) {
  try {
    await removeCarouselItem(selectedCarouselId.value, itemId)
    await selectCarousel(selectedCarouselId.value)
    ElMessage.success('已移除')
  } catch { /* ignore */ }
}

// 轮播项拖拽排序
const carouselDragIdx = ref(-1)

function onCarouselItemDragStart(index) { carouselDragIdx.value = index }
function onCarouselItemDragOver(e) { e.preventDefault() }

async function onCarouselItemDrop(targetIdx) {
  if (carouselDragIdx.value < 0 || carouselDragIdx.value === targetIdx) return
  const items = [...selectedCarousel.value.items]
  const [moved] = items.splice(carouselDragIdx.value, 1)
  items.splice(targetIdx, 0, moved)
  selectedCarousel.value.items = items
  try {
    await sortCarouselItems(selectedCarouselId.value, items.map(i => i.id))
  } catch { /* ignore */ }
  carouselDragIdx.value = -1
}

// 动效/顺序选项
const effectOptions = [
  { value: 'fade', label: '淡入淡出' },
  { value: 'slide', label: '滑动' },
  { value: 'zoom', label: '缩放' }
]
const orderOptions = [
  { value: 'sort', label: '按排序' },
  { value: 'random', label: '随机' },
  { value: 'time', label: '按时间' }
]

onMounted(() => {
  loadApprovedArchives()
  loadCarousels()
  loadPublishedPool()
})
</script>

<template>
  <div class="operation-page">
    <h2 class="page-title">资料库运营</h2>

    <el-tabs v-model="activeTab" @tab-change="activeTab === 'carousel' ? (loadCarousels(), loadPublishedPool()) : loadApprovedArchives()">
      <el-tab-pane label="上架管理" name="publish" />
      <el-tab-pane label="轮播配置" name="carousel" />
    </el-tabs>

    <!-- ==============================
         Section 1: 上架管理
         ============================== -->
    <div v-if="activeTab === 'publish'" v-loading="gridLoading">
      <div class="grid-container">
        <div
          v-for="(row, idx) in approvedList"
          :key="row.id"
          class="pub-card"
          :class="{ 'is-top': row.isTop, 'is-published': row.status === 'published' }"
          draggable="true"
          @dragstart="onGridDragStart(idx)"
          @dragover="onGridDragOver"
          @drop="onGridDrop(idx)"
        >
          <!-- 置顶角标 -->
          <div v-if="row.isTop" class="pin-badge">📌 置顶</div>

          <div class="card-body">
            <h4 class="card-title">{{ row.title }}</h4>
            <div class="card-meta">
              <el-tag size="small">{{ row.categoryName }}</el-tag>
              <span>{{ row.collegeName }}</span>
            </div>
            <div class="card-status">
              <el-tag size="small" :type="row.status === 'published' ? 'success' : row.status === 'unpublished' ? 'info' : ''">
                {{ statusLabels[row.status] || row.status }}
              </el-tag>
            </div>
          </div>

          <div class="card-actions">
            <el-switch
              :model-value="row.status === 'published'"
              :active-text="row.status === 'published' ? '已上架' : row.status === 'unpublished' ? '已下架' : ''"
              active-color="#36C5A6"
              @change="togglePublish(row)"
            />
            <el-button
              link size="small"
              :type="row.isTop ? 'warning' : ''"
              @click="toggleTop(row)"
            >
              {{ row.isTop ? '取消置顶' : '置顶' }}
            </el-button>
          </div>
        </div>
      </div>

      <el-empty v-if="!approvedList.length" description="暂无已入库的档案" />
    </div>

    <!-- ==============================
         Section 2: 轮播配置
         ============================== -->
    <div v-else class="carousel-section" v-loading="carouselLoading">
      <el-row :gutter="16">
        <!-- 左：轮播方案列表 + 已发布池 -->
        <el-col :span="10">
          <!-- 轮播方案列表 -->
          <div class="panel-card">
            <div class="panel-header">
              <h3>轮播方案</h3>
              <el-button size="small" type="primary" @click="openCarouselForm(null)">新建方案</el-button>
            </div>
            <el-radio-group
              v-model="selectedCarouselId"
              style="width:100%;display:flex;flex-direction:column;gap:8px"
              @change="selectCarousel"
            >
              <div v-for="c in carousels" :key="c.id" class="carousel-radio-item">
                <el-radio :value="c.id" border style="width:100%">
                  <div class="radio-label">
                    <span>{{ c.name }}</span>
                    <el-tag v-if="c.isDefault" size="small" type="success">默认</el-tag>
                    <span class="radio-meta">{{ c.intervalSec }}s · {{ c.effect }}</span>
                  </div>
                </el-radio>
                <el-button link type="primary" size="small" class="radio-edit" @click="openCarouselForm(c)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDeleteCarousel(c.id)">删除</el-button>
              </div>
            </el-radio-group>
          </div>

          <!-- 已发布档案池 -->
          <div class="panel-card" style="margin-top:16px">
            <h3 style="margin:0 0 12px">已上架档案池</h3>
            <div class="pool-list">
              <div
                v-for="a in publishedArchives"
                :key="'pool-'+a.id"
                class="pool-item"
                @click="addToCarousel(a)"
              >
                <span class="pool-title">{{ a.title }}</span>
                <el-tag size="small">{{ a.collegeName }}</el-tag>
                <el-icon class="pool-add"><CirclePlus /></el-icon>
              </div>
            </div>
            <el-empty v-if="!publishedArchives.length" description="暂无已上架档案" :image-size="40" />
          </div>
        </el-col>

        <!-- 右：选中方案的池内容 + 参数 -->
        <el-col :span="14">
          <template v-if="selectedCarousel">
            <!-- 方案参数预览 -->
            <div class="panel-card">
              <h3 style="margin:0 0 8px">{{ selectedCarousel.name }}</h3>
              <div class="param-tags">
                <el-tag>停留 {{ selectedCarousel.intervalSec }}s</el-tag>
                <el-tag :type="selectedCarousel.effect === 'zoom' ? 'success' : 'warning'">
                  {{ effectOptions.find(e => e.value === selectedCarousel.effect)?.label || selectedCarousel.effect }}
                </el-tag>
                <el-tag type="info">
                  {{ orderOptions.find(o => o.value === selectedCarousel.orderType)?.label || selectedCarousel.orderType }}
                </el-tag>
                <el-tag v-if="selectedCarousel.isDefault" type="success">默认方案</el-tag>
              </div>
            </div>

            <!-- 轮播池内容（可拖拽排序） -->
            <div class="panel-card" style="margin-top:16px">
              <h3 style="margin:0 0 12px">
                轮播内容
                <span style="font-weight:normal;font-size:12px;color:#86909C">
                  ({{ selectedCarousel.items?.length || 0 }} 项，可拖拽排序)
                </span>
              </h3>

              <div class="carousel-items" v-if="selectedCarousel.items?.length">
                <div
                  v-for="(item, idx) in selectedCarousel.items"
                  :key="item.id"
                  class="carousel-item-row"
                  :class="{ 'is-dragging': carouselDragIdx === idx }"
                  draggable="true"
                  @dragstart="onCarouselItemDragStart(idx)"
                  @dragover="onCarouselItemDragOver"
                  @drop="onCarouselItemDrop(idx)"
                >
                  <el-icon class="drag-handle"><Rank /></el-icon>
                  <span class="item-order">{{ idx + 1 }}</span>
                  <span class="item-title">{{ item.archiveTitle || '未知档案' }}</span>
                  <span class="item-alumni">{{ item.alumniName || '' }}</span>
                  <el-button link type="danger" size="small" @click="removeFromCarousel(item.id)">移除</el-button>
                </div>
              </div>
              <el-empty v-else description="暂无轮播内容，从左侧档案池点击添加" :image-size="60" />
            </div>
          </template>
          <el-empty v-else description="请选择或新建轮播方案" />
        </el-col>
      </el-row>

      <!-- 轮播方案表单弹窗 -->
      <el-dialog
        v-model="carouselFormVisible"
        :title="carouselForm.editId ? '编辑轮播方案' : '新建轮播方案'"
        width="460px"
      >
        <el-form :model="carouselForm" label-width="80px">
          <el-form-item label="方案名称" required>
            <el-input v-model="carouselForm.name" placeholder="如「默认轮播方案」" />
          </el-form-item>
          <el-form-item label="停留时长(s)">
            <el-input-number v-model="carouselForm.intervalSec" :min="3" :max="60" style="width:100%" />
          </el-form-item>
          <el-form-item label="动效">
            <el-select v-model="carouselForm.effect" style="width:100%">
              <el-option v-for="e in effectOptions" :key="e.value" :label="e.label" :value="e.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="顺序">
            <el-select v-model="carouselForm.orderType" style="width:100%">
              <el-option v-for="o in orderOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认方案">
            <el-switch v-model="carouselForm.isDefault" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="carouselFormVisible = false">取消</el-button>
          <el-button type="primary" @click="saveCarousel" :loading="carouselSaving">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<style scoped>
.operation-page { padding: 0; }
.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif; font-size: 20px; color: #1D2129; margin: 0 0 16px;
}

/* ---- 上架管理网格 ---- */
.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.pub-card {
  background: #fff; border-radius: 8px; padding: 16px;
  border: 1px solid #E5E6EB; box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  cursor: grab; transition: box-shadow 0.2s, border-color 0.2s;
  position: relative;
}

.pub-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); border-color: #2B5AED; }
.pub-card.is-top { border-color: #F5A623; background: #FFFCF0; }
.pub-card.is-published { border-left: 3px solid #36C5A6; }

.pin-badge {
  position: absolute; top: -1px; right: 12px;
  background: #F5A623; color: #fff; font-size: 11px;
  padding: 2px 10px; border-radius: 0 0 6px 6px;
}

.card-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif; font-size: 15px;
  color: #1D2129; margin: 0 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.card-meta { display: flex; gap: 8px; align-items: center; color: #86909C; font-size: 12px; margin-bottom: 8px; }
.card-status { margin-bottom: 10px; }
.card-actions { display: flex; align-items: center; justify-content: space-between; }

/* ---- 轮播配置区 ---- */
.panel-card {
  background: #fff; border-radius: 8px; padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.panel-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.panel-header h3 { margin: 0; font-size: 15px; }

.carousel-radio-item {
  display: flex; align-items: center; gap: 4px;
}
.radio-label { display: flex; gap: 8px; align-items: center; flex: 1; font-size: 13px; }
.radio-meta { color: #86909C; font-size: 11px; margin-left: auto; }
.radio-edit { font-size: 12px; }

/* 已发布档案池 */
.pool-list { max-height: 320px; overflow-y: auto; }
.pool-item {
  display: flex; align-items: center; gap: 8px; padding: 8px 10px;
  border-radius: 6px; cursor: pointer; transition: background 0.15s;
  border: 1px solid transparent;
}

.pool-item:hover { background: #EDF1FD; border-color: #2B5AED; }
.pool-title { flex: 1; font-size: 13px; color: #1D2129; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pool-add { color: #2B5AED; font-size: 16px; opacity: 0; transition: opacity 0.15s; }
.pool-item:hover .pool-add { opacity: 1; }

/* 轮播内容 */
.carousel-items { display: flex; flex-direction: column; gap: 6px; }
.carousel-item-row {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
  border: 1px solid #E5E6EB; border-radius: 6px; background: #FAFBFC;
  cursor: grab; transition: box-shadow 0.15s;
}

.carousel-item-row:hover { box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.carousel-item-row.is-dragging { opacity: 0.5; }
.drag-handle { color: #86909C; cursor: grab; font-size: 18px; }
.item-order { color: #86909C; font-size: 12px; width: 22px; text-align: center; }
.item-title { flex: 1; font-size: 13px; color: #1D2129; }
.item-alumni { color: #86909C; font-size: 12px; }

.param-tags { display: flex; gap: 8px; flex-wrap: wrap; }
</style>

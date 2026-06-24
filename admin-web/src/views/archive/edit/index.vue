<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArchiveById, createArchive, updateArchive, addMedia, removeMedia, sortMedia, submitForReview } from '@/api/archive'
import { listAllCategories } from '@/api/category'
import { listAllColleges } from '@/api/college'
import { getAlumniPage } from '@/api/alumni'
import { uploadFile } from '@/api/upload'

const router = useRouter()
const route = useRoute()
const editId = computed(() => route.params.id ? Number(route.params.id) : null)
const isEdit = computed(() => !!editId.value)
const isDraft = ref(true)    // 当前状态是否可编辑
const currentStatus = ref('') // 当前档案状态字符串

// ---- 表单 ----
const formRef = ref(null)
const formLoading = ref(false)
const saving = ref(false)

const form = reactive({
  title: '',
  categoryId: null,
  eventDate: '',
  content: '',
  alumniId: null,
  collegeId: null
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  alumniId: [{ required: true, message: '请选择校友', trigger: 'change' }],
  collegeId: [{ required: true, message: '请选择学院', trigger: 'change' }]
}

// ---- 下拉选项 ----
const categoryOptions = ref([])
const collegeOptions = ref([])

// ---- 校友远程搜索（防抖） ----
const alumniSearchLoading = ref(false)
const alumniOptions = ref([])
let alumniSearchTimer = null

async function searchAlumni(query) {
  clearTimeout(alumniSearchTimer)
  if (!query || query.length < 1) {
    alumniOptions.value = []
    alumniSearchLoading.value = false
    return
  }
  alumniSearchTimer = setTimeout(async () => {
    alumniSearchLoading.value = true
    try {
      const res = await getAlumniPage({ name: query, pageNum: 1, pageSize: 20 })
      alumniOptions.value = (res.records || []).map(a => ({
        value: a.id,
        label: `${a.name} · ${a.studentNo} · ${a.collegeName || ''}`,
        collegeId: a.collegeId
      }))
    } finally {
      alumniSearchLoading.value = false
    }
  }, 300)
}

// 选校友后自动填学院
watch(() => form.alumniId, (val) => {
  if (!val) return
  const found = alumniOptions.value.find(a => a.value === val)
  if (found && found.collegeId) {
    form.collegeId = found.collegeId
  }
})

// ---- 媒体列表 ----
const mediaList = ref([])
const mediaUploading = ref(false)

// ---- 拖拽排序状态 ----
const dragIndex = ref(-1)

function onDragStart(index) {
  dragIndex.value = index
}

function onDragOver(e) {
  e.preventDefault()
}

async function onDrop(targetIndex) {
  if (dragIndex.value < 0 || dragIndex.value === targetIndex) return
  const items = [...mediaList.value]
  const [moved] = items.splice(dragIndex.value, 1)
  items.splice(targetIndex, 0, moved)
  mediaList.value = items

  // 持久化排序
  try {
    await sortMedia(editId.value || 0, items.map(m => m.id))
  } catch { /* 忽略排序错误 */ }
  dragIndex.value = -1
}

// ---- 初始化 ----
async function loadDictionaries() {
  categoryOptions.value = await listAllCategories()
  collegeOptions.value = await listAllColleges()
}

async function loadArchive() {
  if (!editId.value) return
  formLoading.value = true
  try {
    const data = await getArchiveById(editId.value)
    Object.assign(form, {
      title: data.title || '',
      categoryId: data.categoryId ?? null,
      eventDate: data.eventDate || '',
      content: data.content || '',
      alumniId: data.alumniId ?? null,
      collegeId: data.collegeId ?? null
    })
    mediaList.value = data.mediaList || []
    currentStatus.value = data.status || ''
    isDraft.value = data.status === 'draft' || data.status === 'rejected'

    // 预填校友选择器
    if (data.alumniName) {
      alumniOptions.value = [{
        value: data.alumniId,
        label: `${data.alumniName} · ${data.collegeName || ''}`,
        collegeId: data.collegeId
      }]
    }
  } finally {
    formLoading.value = false
  }
}

onMounted(() => {
  loadDictionaries()
  loadArchive()
})

// ---- 保存 ----
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      title: form.title,
      categoryId: form.categoryId,
      eventDate: form.eventDate || null,
      content: form.content || '',
      alumniId: form.alumniId,
      collegeId: form.collegeId
    }

    if (isEdit.value) {
      await updateArchive(editId.value, payload)
      ElMessage.success('保存成功')
    } else {
      const created = await createArchive(payload)
      ElMessage.success('创建成功，请添加媒体素材')
      // 创建后跳转到编辑页
      router.replace(`/archive/${created.id}/edit`)
    }
  } finally {
    saving.value = false
  }
}

// ---- 提交审核 ----
async function handleSubmit() {
  try {
    await ElMessageBox.confirm(
      '确认提交审核吗？提交后将进入学院审核环节。',
      '确认提交',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    // 用户取消
    return
  }
  try {
    await submitForReview(editId.value)
    isDraft.value = false
    currentStatus.value = 'pending_college'
    ElMessage.success('已提交审核')
  } catch {
    ElMessage.error('提交失败，请重试')
  }
}

// ---- 媒体操作 ----
const uploadInputRef = ref(null)

/** 点击上传 */
function triggerUpload() {
  uploadInputRef.value?.click()
}

/** 处理文件 input change 事件 */
function onFileInputChange(e) {
  const file = e.target?.files?.[0]
  if (file) uploadMediaFile(file)
  // 重置 input 以支持重复上传同一文件
  if (uploadInputRef.value) uploadInputRef.value.value = ''
}

/** 拖拽放下上传 */
function onFileDrop(e) {
  e.preventDefault()
  const file = e.dataTransfer?.files?.[0]
  if (file) uploadMediaFile(file)
}

/** 上传媒体文件 */
async function uploadMediaFile(file) {
  if (!editId.value) {
    ElMessage.warning('请先保存档案再上传媒体')
    return
  }
  mediaUploading.value = true
  try {
    const updated = await addMedia(editId.value, file)
    mediaList.value = updated.mediaList || []
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('上传失败')
  } finally {
    mediaUploading.value = false
  }
}

/** 删除媒体 */
async function handleRemoveMedia(mediaId) {
  if (!editId.value) return
  try {
    await ElMessageBox.confirm('确定删除该媒体吗？', '确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await removeMedia(editId.value, mediaId)
    mediaList.value = mediaList.value.filter(m => m.id !== mediaId)
    ElMessage.success('已删除')
  } catch { /* 取消 */ }
}

/** 格式化文件大小 */
function formatSize(bytes) {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

/** 媒体类型标签 */
function mediaLabel(type) {
  return type === 1 ? '图片' : type === 2 ? '视频' : '文档'
}

function mediaTagType(type) {
  return type === 1 ? 'success' : type === 2 ? 'warning' : 'info'
}

// ---- 状态标签（存档查看时） ----
const statusLabels = {
  draft: '草稿',
  pending_college: '待学院审核',
  pending_academic: '待教务处审核',
  approved: '已入库',
  rejected: '已驳回',
  published: '已上架',
  unpublished: '已下架'
}
const statusTagTypes = {
  draft: 'info',
  pending_college: 'warning',
  pending_academic: 'warning',
  approved: 'success',
  rejected: 'danger',
  published: '',
  unpublished: 'info'
}

// ---- 返回 ----
function goBack() {
  router.push('/archive')
}
</script>

<template>
  <div class="edit-page">
    <div class="page-topbar">
      <el-button @click="goBack" text>
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <h2 class="page-title">{{ isEdit ? '编辑档案' : '新建档案' }}</h2>
      <div class="topbar-actions">
        <template v-if="isEdit">
          <el-tag
            v-if="currentStatus"
            size="large"
            :type="statusTagTypes[currentStatus] || 'info'"
          >
            {{ statusLabels[currentStatus] || currentStatus }}
          </el-tag>
        </template>
        <el-button @click="handleSave" :loading="saving" type="primary">
          {{ saving ? '保存中…' : '保存' }}
        </el-button>
        <el-button
          v-if="isEdit && isDraft"
          type="success"
          @click="handleSubmit"
        >
          提交审核
        </el-button>
      </div>
    </div>

    <!-- 主内容区：左表单 + 右媒体 -->
    <div class="edit-body" v-loading="formLoading">
      <!-- 左侧表单 -->
      <div class="form-panel">
        <div class="panel-card">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="档案标题" prop="title">
              <el-input v-model="form.title" placeholder="如「全国大学生数学建模竞赛一等奖」" />
            </el-form-item>

            <el-form-item label="资料分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
                <el-option
                  v-for="c in categoryOptions"
                  :key="c.id"
                  :label="c.name"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="事件时间">
              <el-date-picker
                v-model="form.eventDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width:100%"
              />
            </el-form-item>

            <el-form-item label="关联校友" prop="alumniId">
              <el-select
                v-model="form.alumniId"
                filterable
                remote
                reserve-keyword
                :remote-method="searchAlumni"
                :loading="alumniSearchLoading"
                placeholder="搜索姓名/学号"
                style="width:100%"
                :disabled="isEdit && form.status && form.status !== 'draft' && form.status !== 'rejected'"
              >
                <el-option
                  v-for="a in alumniOptions"
                  :key="a.value"
                  :label="a.label"
                  :value="a.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="所属学院" prop="collegeId">
              <el-select v-model="form.collegeId" placeholder="请选择学院" style="width:100%">
                <el-option
                  v-for="c in collegeOptions"
                  :key="c.id"
                  :label="c.name"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="正文内容">
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="8"
                placeholder="描述该项荣誉/作品/成绩的详细内容…"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 右侧媒体上传区 -->
      <div class="media-panel">
        <div class="panel-card">
          <h3 class="panel-title">
            媒体素材
            <span class="panel-sub">（图片/视频）</span>
          </h3>

          <!-- 上传区 -->
          <div
            class="upload-zone"
            :class="{ 'is-uploading': mediaUploading }"
            @click="triggerUpload"
            @dragover.prevent
            @drop="onFileDrop"
          >
            <template v-if="!mediaUploading">
              <el-icon :size="36" color="#86909C"><Plus /></el-icon>
              <p>点击或拖拽文件至此上传</p>
              <span class="upload-hint">支持 JPG / PNG / WEBP / MP4，图片最大 10MB，视频最大 100MB</span>
            </template>
            <el-icon v-else :size="36" class="is-loading"><Loading /></el-icon>
            <input
              ref="uploadInputRef"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/webm"
              style="display:none"
              @change="onFileInputChange"
            />
          </div>

          <!-- 缩略图网格 -->
          <div class="media-grid" v-if="mediaList.length">
            <div
              v-for="(media, index) in mediaList"
              :key="media.id"
              class="media-item"
              :class="{ 'is-dragging': dragIndex === index }"
              :draggable="isDraft"
              @dragstart="onDragStart(index)"
              @dragover="onDragOver"
              @drop="onDrop(index)"
            >
              <!-- 缩略图 -->
              <div class="media-thumb">
                <img
                  v-if="media.type === 1 && media.thumbnail"
                  :src="media.thumbnail"
                  :alt="media.fileName"
                />
                <img
                  v-else-if="media.type === 1 && media.url"
                  :src="media.url"
                  :alt="media.fileName"
                />
                <div v-else class="media-placeholder">
                  <el-icon :size="32"><VideoPlay /></el-icon>
                </div>

                <!-- 操作浮层 -->
                <div class="media-overlay" v-if="isDraft">
                  <el-icon
                    :size="18"
                    class="media-delete"
                    @click.stop="handleRemoveMedia(media.id)"
                  >
                    <Delete />
                  </el-icon>
                </div>
              </div>

              <!-- 信息条 -->
              <div class="media-info">
                <el-tag size="small" :type="mediaTagType(media.type)">
                  {{ mediaLabel(media.type) }}
                </el-tag>
                <span class="media-name" :title="media.fileName">{{ media.fileName || '未知文件' }}</span>
                <span class="media-size">{{ formatSize(media.fileSize) }}</span>
              </div>
            </div>
          </div>

          <el-empty
            v-else
            description="暂无媒体素材，上传图片或视频"
            :image-size="80"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.edit-page {
  padding: 0;
  min-height: 100%;
}

/* ---- 顶部栏 ---- */
.page-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px;
  color: #1D2129;
  margin: 0;
  flex: 1;
}

.topbar-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* ---- 主内容 ---- */
.edit-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.form-panel {
  flex: 1;
  min-width: 0;
}

.media-panel {
  width: 420px;
  flex-shrink: 0;
}

.panel-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

.panel-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 15px;
  color: #1D2129;
  margin: 0 0 16px;
}

.panel-sub {
  font-family: 'HarmonyOS_SansSC_Regular', sans-serif;
  font-size: 12px;
  color: #86909C;
  font-weight: normal;
}

/* ---- 上传区 ---- */
.upload-zone {
  border: 2px dashed #D0D5DD;
  border-radius: 8px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #F9FAFB;
}

.upload-zone:hover,
.upload-zone.is-uploading {
  border-color: #2B5AED;
  background: #EDF1FD;
}

.upload-zone p {
  margin: 8px 0 4px;
  font-size: 14px;
  color: #4E5969;
}

.upload-hint {
  font-size: 11px;
  color: #86909C;
}

/* ---- 媒体网格 ---- */
.media-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.media-item {
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #E5E6EB;
  background: #fff;
  transition: box-shadow 0.2s;
  cursor: grab;
}

.media-item:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.media-item.is-dragging {
  opacity: 0.5;
}

.media-thumb {
  position: relative;
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F7FB;
  overflow: hidden;
}

.media-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-placeholder {
  color: #86909C;
}

.media-overlay {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 6px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.2s;
  background: rgba(0,0,0,0.25);
}

.media-item:hover .media-overlay {
  opacity: 1;
}

.media-overlay > * {
  pointer-events: auto;
}

.media-delete {
  color: #fff;
  background: rgba(229,72,77,0.85);
  border-radius: 4px;
  padding: 4px;
  cursor: pointer;
}

.media-delete:hover {
  background: #E5484D;
}

.media-info {
  padding: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.media-name {
  font-size: 11px;
  color: #4E5969;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-size {
  font-size: 10px;
  color: #86909C;
  width: 100%;
}
</style>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getTodoList, getAuditLogs, collegeApprove, collegeReject, academicApprove, academicReject } from '@/api/audit'
import { getArchiveById } from '@/api/archive'

const userStore = useUserStore()

// ---- Tab ----
const activeTab = ref('pending')

// ---- 待审列表 ----
const loading = ref(false)
const todoList = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

// ---- 已通过/已驳回列表 ----
const historyList = ref([])
const historyTotal = ref(0)
const historyPage = ref(1)
const historyLoading = ref(false)

// ---- 审核弹窗 ----
const dialogVisible = ref(false)
const dialogTitle = ref('')
const auditArchive = ref(null)      // 当前审核的档案详情
const auditLogs = ref([])           // 该档案的审核记录
const dialogLoading = ref(false)
const auditOpinion = ref('')
const auditSubmitting = ref(false)
const showRejectInput = ref(false)  // 是否显示驳回意见输入

// ---- 状态映射 ----
const statusLabels = {
  draft: '草稿', pending_college: '待学院审核', pending_academic: '待教务处审核',
  approved: '已入库', rejected: '已驳回', published: '已上架', unpublished: '已下架'
}
const statusTagTypes = {
  draft: 'info', pending_college: 'warning', pending_academic: 'warning',
  approved: 'success', rejected: 'danger', published: '', unpublished: 'info'
}

// ---- 当前角色可做的审核操作 ----
const canAudit = computed(() => {
  if (!auditArchive.value) return false
  const status = auditArchive.value.status
  if (userStore.role === 'college') return status === 'pending_college'
  if (userStore.role === 'academic') return status === 'pending_academic'
  // admin can do both
  return status === 'pending_college' || status === 'pending_academic'
})

// ---- 加载待审列表 ----
async function loadTodo() {
  loading.value = true
  try {
    if (userStore.role === 'admin') {
      // admin 需要合并两个环节的待办
      const [collegeRes, academicRes] = await Promise.all([
        getTodoList({ role: 'college', pageNum: 1, pageSize: 100 }),
        getTodoList({ role: 'academic', pageNum: 1, pageSize: 100 })
      ])
      const merged = [...(collegeRes.records || []), ...(academicRes.records || [])]
      // 按 submitTime 降序，去重
      const seen = new Set()
      todoList.value = merged.filter(item => {
        if (seen.has(item.id)) return false
        seen.add(item.id)
        return true
      }).sort((a, b) => (b.submitTime || '').localeCompare(a.submitTime || ''))
      total.value = todoList.value.length
    } else {
      const res = await getTodoList({
        role: userStore.role,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
      todoList.value = res.records || []
      total.value = res.total || 0
    }
  } finally {
    loading.value = false
  }
}

// ---- 加载已通过/已驳回列表 ----
async function loadHistory() {
  historyLoading.value = true
  try {
    const { getArchivePage } = await import('@/api/archive')
    if (activeTab.value === 'rejected') {
      // 已驳回：只查 rejected
      const res = await getArchivePage({
        status: 'rejected',
        pageNum: historyPage.value,
        pageSize: pageSize.value
      })
      historyList.value = res.records || []
      historyTotal.value = res.total || 0
    } else {
      // 已通过：查 approved + published + unpublished（审核通过后的所有状态）
      const res = await getArchivePage({
        pageNum: 1,
        pageSize: 500
      })
      const filtered = (res.records || []).filter(
        a => ['approved', 'published', 'unpublished'].includes(a.status)
      )
      historyList.value = filtered
      historyTotal.value = filtered.length
    }
  } finally {
    historyLoading.value = false
  }
}

function handleTabChange(tab) {
  if (tab === 'pending') loadTodo()
  else { historyPage.value = 1; loadHistory() }
}

// ---- 打开审核弹窗 ----
async function openAuditDialog(row) {
  dialogTitle.value = `审核详情 — ${row.title || '档案'}`
  showRejectInput.value = false
  auditOpinion.value = ''
  dialogVisible.value = true
  dialogLoading.value = true
  try {
    // 并行加载档案详情和审核记录
    const [archive, logs] = await Promise.all([
      getArchiveById(row.id),
      getAuditLogs(row.id)
    ])
    auditArchive.value = archive
    auditLogs.value = logs || []
  } finally {
    dialogLoading.value = false
  }
}

// ---- 审核操作 ----
async function handleApprove() {
  if (!auditArchive.value) return
  auditSubmitting.value = true
  try {
    const archiveId = auditArchive.value.id
    if (userStore.role === 'college') {
      await collegeApprove(archiveId, auditOpinion.value || null)
    } else if (userStore.role === 'academic') {
      await academicApprove(archiveId, auditOpinion.value || null)
    } else {
      // admin: determine by current status
      if (auditArchive.value.status === 'pending_college') {
        await collegeApprove(archiveId, auditOpinion.value || null)
      } else {
        await academicApprove(archiveId, auditOpinion.value || null)
      }
    }
    ElMessage.success('审核通过')
    dialogVisible.value = false
    loadTodo()
  } finally {
    auditSubmitting.value = false
  }
}

async function handleReject() {
  if (!auditArchive.value) return
  if (!auditOpinion.value?.trim()) {
    ElMessage.warning('驳回必须填写审核意见')
    return
  }
  auditSubmitting.value = true
  try {
    const archiveId = auditArchive.value.id
    if (userStore.role === 'college') {
      await collegeReject(archiveId, auditOpinion.value)
    } else if (userStore.role === 'academic') {
      await academicReject(archiveId, auditOpinion.value)
    } else {
      if (auditArchive.value.status === 'pending_college') {
        await collegeReject(archiveId, auditOpinion.value)
      } else {
        await academicReject(archiveId, auditOpinion.value)
      }
    }
    ElMessage.success('已驳回')
    dialogVisible.value = false
    loadTodo()
  } finally {
    auditSubmitting.value = false
  }
}

// ---- 分页 ----
function handlePageChange(p) { pageNum.value = p; loadTodo() }
function handleSizeChange(s) { pageSize.value = s; pageNum.value = 1; loadTodo() }
function handleHistoryPageChange(p) { historyPage.value = p; loadHistory() }

// 节点/动作中文
const nodeLabels = { college: '学院审核', academic: '教务处审核' }
const actionLabels = { approve: '通过', reject: '驳回' }

onMounted(() => loadTodo())
</script>

<template>
  <div class="audit-page">
    <h2 class="page-title">审核中心</h2>

    <!-- Tab -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="audit-tabs">
      <el-tab-pane label="待审核" name="pending" />
      <el-tab-pane label="已通过" name="approved" />
      <el-tab-pane label="已驳回" name="rejected" />
    </el-tabs>

    <!-- 待审表格 -->
    <template v-if="activeTab === 'pending'">
      <el-table :data="todoList" v-loading="loading" stripe row-key="id">
        <el-table-column prop="title" label="档案标题" min-width="180" show-overflow-tooltip sortable />
        <el-table-column prop="alumniName" label="提交人" width="110" sortable />
        <el-table-column prop="collegeName" label="学院" width="140" show-overflow-tooltip sortable />
        <el-table-column prop="categoryName" label="分类" width="110" sortable />
        <el-table-column prop="status" label="状态" width="120" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagTypes[row.status] || 'info'">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170" sortable />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openAuditDialog(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum" v-model:page-size="pageSize"
          :total="total" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange" @size-change="handleSizeChange"
        />
      </div>
    </template>

    <!-- 已通过/已驳回表格 -->
    <template v-else>
      <el-table :data="historyList" v-loading="historyLoading" stripe row-key="id">
        <el-table-column prop="title" label="档案标题" min-width="180" show-overflow-tooltip sortable />
        <el-table-column prop="alumniName" label="提交人" width="110" sortable />
        <el-table-column prop="collegeName" label="学院" width="140" show-overflow-tooltip sortable />
        <el-table-column prop="categoryName" label="分类" width="110" sortable />
        <el-table-column prop="status" label="状态" width="120" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="statusTagTypes[row.status] || 'info'">
              {{ statusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" sortable />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="historyPage" v-model:page-size="pageSize"
          :total="historyTotal" :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="handleHistoryPageChange"
        />
      </div>
    </template>

    <!-- 审核详情弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="880px"
      :close-on-click-modal="false"
      top="5vh"
    >
      <template v-if="auditArchive" v-loading="dialogLoading">
        <div class="dialog-body">
          <!-- 档案信息 -->
          <div class="dialog-left">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="标题">{{ auditArchive.title }}</el-descriptions-item>
              <el-descriptions-item label="分类">{{ auditArchive.categoryName }}</el-descriptions-item>
              <el-descriptions-item label="校友">{{ auditArchive.alumniName }}</el-descriptions-item>
              <el-descriptions-item label="学院">{{ auditArchive.collegeName }}</el-descriptions-item>
              <el-descriptions-item label="事件时间">{{ auditArchive.eventDate || '—' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag size="small" :type="statusTagTypes[auditArchive.status] || 'info'">
                  {{ statusLabels[auditArchive.status] || auditArchive.status }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="正文">
                <div style="max-height:120px;overflow-y:auto;white-space:pre-wrap;font-size:13px;color:#4E5969">
                  {{ auditArchive.content || '（无内容）' }}
                </div>
              </el-descriptions-item>
            </el-descriptions>

            <!-- 审核记录 -->
            <h4 class="section-title">审核记录</h4>
            <el-timeline v-if="auditLogs.length">
              <el-timeline-item
                v-for="log in auditLogs"
                :key="log.id"
                :timestamp="log.createTime"
                :type="log.action === 'approve' ? 'success' : 'danger'"
                placement="top"
              >
                <div>{{ nodeLabels[log.node] || log.node }} · {{ actionLabels[log.action] || log.action }}</div>
                <div v-if="log.opinion" style="color:#4E5969;font-size:12px">意见：{{ log.opinion }}</div>
                <div style="color:#86909C;font-size:11px">{{ log.auditorName }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无审核记录" :image-size="40" />
          </div>

          <!-- 媒体预览 -->
          <div class="dialog-right">
            <h4 class="section-title">媒体素材</h4>
            <div class="media-preview-list" v-if="auditArchive.mediaList?.length">
              <div v-for="m in auditArchive.mediaList" :key="m.id" class="media-preview-item">
                <img v-if="m.type === 1" :src="m.thumbnail || m.url" :alt="m.fileName" />
                <div v-else class="video-placeholder">
                  <el-icon :size="40"><VideoPlay /></el-icon>
                  <span>{{ m.fileName }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="无媒体" :image-size="60" />
          </div>
        </div>

        <!-- 审核操作区 -->
        <div v-if="canAudit" class="audit-actions">
          <template v-if="showRejectInput">
            <el-input
              v-model="auditOpinion"
              type="textarea"
              :rows="2"
              placeholder="请输入驳回意见（必填）"
              style="flex:1"
            />
            <el-button type="danger" @click="handleReject" :loading="auditSubmitting">确认驳回</el-button>
            <el-button @click="showRejectInput = false">取消</el-button>
          </template>
          <template v-else>
            <el-button type="success" @click="handleApprove" :loading="auditSubmitting">审核通过</el-button>
            <el-button type="danger" @click="showRejectInput = true">驳回</el-button>
          </template>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.audit-page { padding: 0; }
.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif; font-size: 20px; color: #1D2129; margin: 0 0 16px;
}

.audit-tabs {
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex; justify-content: flex-end; margin-top: 16px; padding: 12px 0;
}

/* ---- 审核弹窗 ---- */
.dialog-body {
  display: flex; gap: 20px;
}

.dialog-left { flex: 1; min-width: 0; }
.dialog-right { width: 280px; flex-shrink: 0; }

.section-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 14px; color: #1D2129; margin: 16px 0 10px;
}

.media-preview-list {
  display: flex; flex-direction: column; gap: 8px; max-height: 320px; overflow-y: auto;
}

.media-preview-item {
  border-radius: 6px; overflow: hidden; border: 1px solid #E5E6EB;
}

.media-preview-item img {
  width: 100%; max-height: 180px; object-fit: cover; display: block;
}

.video-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  height: 120px; background: #F5F7FB; color: #86909C; font-size: 12px; gap: 8px;
}

.audit-actions {
  margin-top: 20px; padding-top: 16px; border-top: 1px solid #E5E6EB;
  display: flex; gap: 10px; align-items: center; justify-content: flex-end;
}
</style>

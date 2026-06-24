<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArchivePage, deleteArchive, submitForReview } from '@/api/archive'
import { listAllCategories } from '@/api/category'
import { listAllColleges } from '@/api/college'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// ---- 查询 ----
const query = reactive({
  title: '',
  categoryId: null,
  status: null,
  collegeId: null
})

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

const categoryOptions = ref([])
const collegeOptions = ref([])

// ---- 状态映射 ----
const statusConfig = {
  draft:            { label: '草稿', type: 'info' },
  pending_college:  { label: '待学院审核', type: 'warning' },
  pending_academic: { label: '待教务处审核', type: 'warning' },
  approved:         { label: '已入库', type: 'success' },
  rejected:         { label: '已驳回', type: 'danger' },
  published:        { label: '已上架', type: '' },
  unpublished:       { label: '已下架', type: 'info' }
}

// ---- 加载 ----
async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (query.title) params.title = query.title
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.status) params.status = query.status
    if (query.collegeId) params.collegeId = query.collegeId
    const res = await getArchivePage(params)
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadDictionaries() {
  categoryOptions.value = await listAllCategories()
  collegeOptions.value = await listAllColleges()
}

onMounted(() => {
  loadDictionaries()
  loadData()
})

// ---- 查询（实时搜索 + 回车） ----
let searchTimer = null

watch(query, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    handleSearch()
  }, 300)
}, { deep: true })

function handleSearch() {
  clearTimeout(searchTimer)
  pageNum.value = 1
  loadData()
}

function handleReset() {
  clearTimeout(searchTimer)
  Object.assign(query, { title: '', categoryId: null, status: null, collegeId: null })
  pageNum.value = 1
  loadData()
}

function handlePageChange(p) {
  pageNum.value = p
  loadData()
}

function handleSizeChange(s) {
  pageSize.value = s
  pageNum.value = 1
  loadData()
}

// ---- 操作 ----
function openCreate() {
  router.push('/archive/create')
}

function openEdit(row) {
  router.push(`/archive/${row.id}/edit`)
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除档案「${row.title}」吗？`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteArchive(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* 取消 */ }
}

async function handleSubmit(row) {
  try {
    await ElMessageBox.confirm(
      `确认提交审核「${row.title}」吗？提交后将进入学院审核环节。`,
      '确认提交',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'info' }
    )
    await submitForReview(row.id)
    ElMessage.success('已提交审核')
    loadData()
  } catch { /* 取消 */ }
}

/** 媒体类型图标 */
function mediaTypeIcon(type) {
  return type === 1 ? 'Picture' : type === 2 ? 'VideoPlay' : 'Document'
}
</script>

<template>
  <div class="archive-page">
    <div class="page-header">
      <h2 class="page-title">资料档案</h2>
      <el-button type="primary" @click="openCreate">新建档案</el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="query.title" placeholder="标题关键词" clearable style="width:180px" @keyup.enter="handleSearch" />
      <el-select v-model="query.categoryId" placeholder="分类" clearable style="width:140px">
        <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:140px">
        <el-option v-for="(cfg, key) in statusConfig" :key="key" :label="cfg.label" :value="key" />
      </el-select>
      <el-select v-model="query.collegeId" placeholder="学院" clearable style="width:160px">
        <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe style="width:100%" row-key="id">
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip sortable />
      <el-table-column prop="categoryName" label="分类" width="110" sortable />
      <el-table-column prop="alumniName" label="关联校友" width="110" sortable />
      <el-table-column prop="collegeName" label="学院" width="140" show-overflow-tooltip sortable />
      <el-table-column prop="status" label="状态" width="130" sortable>
        <template #default="{ row }">
          <el-tag size="small" :type="statusConfig[row.status]?.type || 'info'">
            {{ statusConfig[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="媒体" width="80">
        <template #default="{ row }">
          <span style="color:#86909C;font-size:12px">
            {{ row.mediaList?.length || 0 }}
            <el-icon :size="12" v-if="row.mediaList?.length"><Picture /></el-icon>
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="eventDate" label="事件时间" width="120" sortable />
      <el-table-column prop="createTime" label="创建时间" width="170" sortable />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'draft'"
            link
            type="success"
            size="small"
            @click="handleSubmit(row)"
          >
            提交审核
          </el-button>
          <el-button
            v-if="row.status === 'draft' || row.status === 'rejected'"
            link
            type="danger"
            size="small"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.archive-page { padding: 0; }

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px;
  color: #1D2129;
  margin: 0;
}

.search-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 12px 0;
}
</style>

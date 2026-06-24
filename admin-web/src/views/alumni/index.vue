<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAlumniPage, getAlumniById, createAlumni, updateAlumni,
  deleteAlumni, downloadTemplate, importExcel
} from '@/api/alumni'
import { listAllColleges } from '@/api/college'
import { listMajorsByCollege } from '@/api/major'
import { uploadFile } from '@/api/upload'
import { enrollFace } from '@/api/face'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ---- 查询参数 ----
const query = reactive({
  name: '',
  studentNo: '',
  collegeId: null,
  gradYear: null,
  identity: null,
  status: null
})

// ---- 列表数据 ----
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)

// ---- 下拉选项 ----
const collegeOptions = ref([])
const majorOptions = ref([])

// ---- 抽屉表单 ----
const drawerVisible = ref(false)
const drawerTitle = ref('新增校友')
const formLoading = ref(false)
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref(null)

const form = reactive({
  studentNo: '',
  name: '',
  gender: null,
  collegeId: null,
  majorId: null,
  enrollYear: null,
  gradYear: null,
  identity: 1,
  avatar: '',
  summary: ''
})

const rules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  collegeId: [{ required: true, message: '请选择学院', trigger: 'change' }]
}

// ---- Excel 导入 ----
const importLoading = ref(false)
const importRef = ref(null)

// ---- 人脸录入对话框 ----
const faceDialogVisible = ref(false)
const faceLoading = ref(false)
const faceAlumni = ref(null)        // 当前操作校友
const faceFile = ref(null)          // 选中的图片文件
const facePreviewUrl = ref('')      // 本地预览 URL
const faceResult = ref(null)        // 录入结果 { quality, modelVer }
const faceUploadRef = ref(null)     // el-upload 引用

// 按学院级联加载专业
watch(() => form.collegeId, async (val) => {
  form.majorId = null
  majorOptions.value = []
  if (val) {
    majorOptions.value = await listMajorsByCollege(val)
  }
}, { immediate: false })

// ---- 数据加载 ----
async function loadData() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...query
    }
    // 清除空值
    Object.keys(params).forEach(k => {
      if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
    })
    const res = await getAlumniPage(params)
    tableData.value = res.records || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadColleges() {
  collegeOptions.value = await listAllColleges()
}

onMounted(() => {
  loadColleges()
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
  Object.assign(query, { name: '', studentNo: '', collegeId: null, gradYear: null, identity: null, status: null })
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

// ---- 新增/编辑 ----
function openCreate() {
  isEdit.value = false
  editId.value = null
  drawerTitle.value = '新增校友'
  resetForm()
  drawerVisible.value = true
}

async function openEdit(row) {
  isEdit.value = true
  editId.value = row.id
  drawerTitle.value = '编辑校友'
  resetForm()

  // 加载详情
  formLoading.value = true
  try {
    const data = await getAlumniById(row.id)
    Object.assign(form, {
      studentNo: data.studentNo || '',
      name: data.name || '',
      gender: data.gender ?? null,
      collegeId: data.collegeId ?? null,
      majorId: data.majorId ?? null,
      enrollYear: data.enrollYear ?? null,
      gradYear: data.gradYear ?? null,
      identity: data.identity ?? 1,
      avatar: data.avatar || '',
      summary: data.summary || ''
    })
    // 触发专业级联
    if (data.collegeId) {
      majorOptions.value = await listMajorsByCollege(data.collegeId)
    }
  } finally {
    formLoading.value = false
  }
  drawerVisible.value = true
}

function resetForm() {
  Object.assign(form, {
    studentNo: '', name: '', gender: null, collegeId: null,
    majorId: null, enrollYear: null, gradYear: null,
    identity: 1, avatar: '', summary: ''
  })
  majorOptions.value = []
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  formLoading.value = true
  try {
    if (isEdit.value) {
      await updateAlumni(editId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createAlumni({ ...form })
      ElMessage.success('新增成功')
    }
    drawerVisible.value = false
    loadData()
  } finally {
    formLoading.value = false
  }
}

// ---- 删除 ----
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除校友「${row.name}」吗？此操作不可恢复。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteAlumni(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* 取消 */ }
}

// ---- 头像上传 ----
async function handleAvatarUpload(file) {
  try {
    const result = await uploadFile(file.raw)
    form.avatar = result.url
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('上传失败')
  }
}

// ---- Excel 导入 ----
async function handleDownloadTemplate() {
  try {
    const blob = await downloadTemplate()
    const url = URL.createObjectURL(blob instanceof Blob ? blob : new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = '校友导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

function handleImportClick() {
  importRef.value?.click()
}

async function handleImportChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  importLoading.value = true
  try {
    const result = await importExcel(file)
    const msg = `导入完成：共 ${result.total} 条，成功 ${result.success} 条，失败 ${result.fail} 条`
    if (result.fail > 0) {
      ElMessage.warning(msg + '\n错误详情：' + (result.errors || []).join('；'))
    } else {
      ElMessage.success(msg)
    }
    loadData()
  } catch {
    ElMessage.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
    // 重置 input 以支持重复上传同一文件
    e.target.value = ''
  }
}

// ---- 人脸录入 ----
function openFaceDialog(row) {
  faceAlumni.value = row
  faceFile.value = null
  facePreviewUrl.value = ''
  faceResult.value = null
  faceDialogVisible.value = true
}

function handleFaceFileChange(file) {
  faceFile.value = file.raw
  // 生成本地预览 URL
  if (facePreviewUrl.value) {
    URL.revokeObjectURL(facePreviewUrl.value)
  }
  facePreviewUrl.value = URL.createObjectURL(file.raw)
  faceResult.value = null
}

async function submitFaceEnroll() {
  if (!faceFile.value) {
    ElMessage.warning('请先选择人脸照片')
    return
  }
  faceLoading.value = true
  faceResult.value = null
  try {
    const result = await enrollFace(faceAlumni.value.id, faceFile.value)
    faceResult.value = result
    faceAlumni.value.faceStatus = 1
    ElMessage.success('人脸录入成功')
  } catch {
    // 错误已在拦截器中提示
  } finally {
    faceLoading.value = false
  }
}

function handleFaceDialogClose() {
  if (facePreviewUrl.value) {
    URL.revokeObjectURL(facePreviewUrl.value)
    facePreviewUrl.value = ''
  }
  faceFile.value = null
  faceResult.value = null
}

// ---- 生成年份选项（最近 40 年） ----
const yearOptions = Array.from({ length: 40 }, (_, i) => new Date().getFullYear() - i)
</script>

<template>
  <div class="alumni-page">
    <h2 class="page-title">校友管理</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input v-model="query.name" placeholder="姓名" clearable style="width:140px" @keyup.enter="handleSearch" />
      <el-input v-model="query.studentNo" placeholder="学号" clearable style="width:140px" @keyup.enter="handleSearch" />
      <el-select v-model="query.collegeId" placeholder="学院" clearable style="width:150px">
        <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.gradYear" placeholder="毕业年份" clearable style="width:120px">
        <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
      </el-select>
      <el-select v-model="query.identity" placeholder="身份" clearable style="width:110px">
        <el-option label="在校生" :value="1" />
        <el-option label="校友" :value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:100px">
        <el-option label="正常" :value="1" />
        <el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="openCreate">新增校友</el-button>
      <el-button @click="handleDownloadTemplate">下载模板</el-button>
      <el-button :loading="importLoading" @click="handleImportClick">
        {{ importLoading ? '导入中…' : 'Excel 导入' }}
      </el-button>
      <input
        ref="importRef"
        type="file"
        accept=".xlsx,.xls"
        style="display:none"
        @change="handleImportChange"
      />
    </div>

    <!-- 表格 -->
    <el-table
      :data="tableData"
      v-loading="loading"
      stripe
      style="width:100%"
      row-key="id"
    >
      <el-table-column label="头像" width="60">
        <template #default="{ row }">
          <el-avatar :size="36" :src="row.avatar">
            <el-icon><User /></el-icon>
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="姓名" width="110" sortable />
      <el-table-column prop="studentNo" label="学号" width="140" sortable />
      <el-table-column prop="collegeName" label="学院" width="150" show-overflow-tooltip sortable />
      <el-table-column prop="gradYear" label="届别" width="80" sortable />
      <el-table-column prop="identity" label="身份" width="90" sortable>
        <template #default="{ row }">
          <el-tag size="small" :type="row.identity === 1 ? '' : 'success'">
            {{ row.identity === 1 ? '在校生' : '校友' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" sortable>
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '正常' : '已停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="faceStatus" label="人脸" width="80" sortable>
        <template #default="{ row }">
          <el-tag size="small" :type="row.faceStatus === 1 ? 'success' : 'info'">
            {{ row.faceStatus === 1 ? '已录' : '未录' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="summary" label="简介" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="success" size="small" @click="openFaceDialog(row)">人脸</el-button>
          <el-button
            v-if="userStore.role === 'admin'"
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

    <!-- 分页 -->
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

    <!-- 抽屉表单 -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="480px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="80px"
        v-loading="formLoading"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="请输入学号" />
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="学院" prop="collegeId">
          <el-select v-model="form.collegeId" placeholder="请选择学院" style="width:100%">
            <el-option v-for="c in collegeOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="专业">
          <el-select
            v-model="form.majorId"
            placeholder="请先选学院"
            clearable
            :disabled="!form.collegeId"
            style="width:100%"
          >
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="入学年份">
          <el-select v-model="form.enrollYear" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
        </el-form-item>

        <el-form-item label="毕业年份">
          <el-select v-model="form.gradYear" placeholder="请选择" clearable style="width:100%">
            <el-option v-for="y in yearOptions" :key="y" :label="y" :value="y" />
          </el-select>
        </el-form-item>

        <el-form-item label="身份">
          <el-radio-group v-model="form.identity">
            <el-radio :value="1">在校生</el-radio>
            <el-radio :value="2">校友</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="头像">
          <div class="avatar-upload">
            <el-avatar :size="80" :src="form.avatar" v-if="form.avatar">
              <el-icon :size="36"><User /></el-icon>
            </el-avatar>
            <el-upload
              :show-file-list="false"
              :http-request="handleAvatarUpload"
              accept="image/jpeg,image/png,image/webp"
            >
              <el-button size="small">上传头像</el-button>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="简介">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="一句话简介，如「ACM 全国金牌」"
          />
        </el-form-item>

        <el-form-item>
          <div style="width:100%;display:flex;gap:12px;justify-content:flex-end">
            <el-button @click="drawerVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSubmit" :loading="formLoading">
              {{ isEdit ? '保存修改' : '确认新增' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-drawer>

    <!-- 人脸录入弹窗 -->
    <el-dialog
      v-model="faceDialogVisible"
      title="录入人脸"
      width="460px"
      :close-on-click-modal="false"
      @close="handleFaceDialogClose"
    >
      <div class="face-dialog-body">
        <!-- 校友信息 -->
        <div class="face-alumni-info">
          <el-avatar :size="48" :src="faceAlumni?.avatar">
            <el-icon :size="24"><User /></el-icon>
          </el-avatar>
          <div class="face-alumni-text">
            <div class="face-alumni-name">{{ faceAlumni?.name }}</div>
            <div class="face-alumni-meta">{{ faceAlumni?.studentNo }} · {{ faceAlumni?.collegeName }}</div>
          </div>
        </div>

        <!-- 上传区域 -->
        <div class="face-upload-area" v-if="!faceResult">
          <div class="face-preview" v-if="facePreviewUrl">
            <img :src="facePreviewUrl" alt="人脸预览" />
          </div>
          <el-upload
            ref="faceUploadRef"
            :show-file-list="false"
            :auto-upload="false"
            accept="image/jpeg,image/png"
            :on-change="handleFaceFileChange"
            drag
          >
            <el-icon :size="40"><Upload /></el-icon>
            <div class="el-upload__text">
              拖拽或<em>点击上传</em>人脸照片
            </div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持 JPG/PNG，单人正面照，脸部清晰无遮挡
              </div>
            </template>
          </el-upload>
        </div>

        <!-- 录入结果 -->
        <div class="face-result" v-if="faceResult">
          <el-result
            icon="success"
            title="录入成功"
            :sub-title="`人脸特征已保存（模型版本：${faceResult.modelVer || '-'}）`"
          >
            <template #extra>
              <el-button type="primary" @click="faceDialogVisible = false">完成</el-button>
              <el-button @click="faceResult = null; faceFile = null; facePreviewUrl = ''">继续录入</el-button>
            </template>
          </el-result>
        </div>
      </div>

      <template #footer v-if="!faceResult">
        <el-button @click="faceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="faceLoading" :disabled="!faceFile" @click="submitFaceEnroll">
          确认录入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.alumni-page {
  padding: 0;
}

.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px;
  color: #1D2129;
  margin: 0 0 20px;
}

/* ---- 搜索栏 ---- */
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

/* ---- 操作栏 ---- */
.action-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 12px 0;
}

/* ---- 头像上传 ---- */
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ---- 人脸录入弹窗 ---- */
.face-dialog-body {
  padding: 0;
}

.face-alumni-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f7f8fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.face-alumni-text {
  flex: 1;
}

.face-alumni-name {
  font-size: 15px;
  font-weight: 600;
  color: #1D2129;
}

.face-alumni-meta {
  font-size: 12px;
  color: #86909C;
  margin-top: 2px;
}

.face-upload-area {
  min-height: 180px;
}

.face-preview {
  text-align: center;
  margin-bottom: 12px;
}

.face-preview img {
  max-width: 100%;
  max-height: 240px;
  border-radius: 8px;
  border: 1px solid #e5e6eb;
}

.face-result {
  padding: 20px 0;
}
</style>

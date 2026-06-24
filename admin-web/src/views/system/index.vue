<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// API
import { listAllColleges } from '@/api/college'
import { listMajorsByCollege } from '@/api/major'
import { getUserPage, createUser, updateUser, deleteUser, resetPassword, toggleUserStatus } from '@/api/user'
import { getOperLogs, getLoginLogs } from '@/api/log'
import request from '@/api/request'  // for raw college/major CRUD
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ---- Tab ----
const activeTab = ref('college')

// ============================
//  Section 1: 学院管理
// ============================
const collegeLoading = ref(false)
const collegeList = ref([])
const collegeDialogVisible = ref(false)
const collegeDialogTitle = ref('')
const collegeForm = reactive({ id: null, name: '', code: '', sort: 0, status: 1 })
const collegeSaving = ref(false)

async function loadColleges() {
  collegeLoading.value = true
  try { collegeList.value = await listAllColleges() } finally { collegeLoading.value = false }
}

function openCollegeForm(row) {
  if (row) {
    Object.assign(collegeForm, { id: row.id, name: row.name, code: row.code, sort: row.sort ?? 0, status: row.status ?? 1 })
    collegeDialogTitle.value = '编辑学院'
  } else {
    Object.assign(collegeForm, { id: null, name: '', code: '', sort: 0, status: 1 })
    collegeDialogTitle.value = '新增学院'
  }
  collegeDialogVisible.value = true
}

async function saveCollege() {
  collegeSaving.value = true
  try {
    const payload = { name: collegeForm.name, code: collegeForm.code, sort: collegeForm.sort, status: collegeForm.status }
    if (collegeForm.id) {
      await request.put(`/college/${collegeForm.id}`, payload)
    } else {
      await request.post('/college', payload)
    }
    ElMessage.success(collegeForm.id ? '已更新' : '已创建')
    collegeDialogVisible.value = false
    loadColleges()
  } finally { collegeSaving.value = false }
}

async function toggleCollege(row) {
  await request.put(`/college/${row.id}/toggle`)
  ElMessage.success('状态已切换')
  loadColleges()
}

// ============================
//  Section 2: 专业管理
// ============================
const majorLoading = ref(false)
const majorList = ref([])
const majorCollegeFilter = ref(null)
const majorDialogVisible = ref(false)
const majorDialogTitle = ref('')
const majorForm = reactive({ id: null, collegeId: null, name: '', code: '', sort: 0, status: 1 })
const majorSaving = ref(false)

async function loadMajors() {
  majorLoading.value = true
  try {
    majorList.value = await listMajorsByCollege(majorCollegeFilter.value)
  } finally { majorLoading.value = false }
}

watch(activeTab, (tab) => { if (tab === 'major') loadMajors() })

function openMajorForm(row) {
  if (row) {
    Object.assign(majorForm, {
      id: row.id, collegeId: row.collegeId, name: row.name,
      code: row.code, sort: row.sort ?? 0, status: row.status ?? 1
    })
    majorDialogTitle.value = '编辑专业'
  } else {
    Object.assign(majorForm, { id: null, collegeId: null, name: '', code: '', sort: 0, status: 1 })
    majorDialogTitle.value = '新增专业'
  }
  majorDialogVisible.value = true
}

async function saveMajor() {
  majorSaving.value = true
  try {
    const payload = { collegeId: majorForm.collegeId, name: majorForm.name, code: majorForm.code, sort: majorForm.sort, status: majorForm.status }
    if (majorForm.id) {
      await request.put(`/major/${majorForm.id}`, payload)
    } else {
      await request.post('/major', payload)
    }
    ElMessage.success(majorForm.id ? '已更新' : '已创建')
    majorDialogVisible.value = false
    loadMajors()
  } finally { majorSaving.value = false }
}

async function toggleMajor(row) {
  await request.put(`/major/${row.id}/toggle`)
  loadMajors()
}

// ============================
//  Section 3: 账号管理
// ============================
const userLoading = ref(false)
const userList = ref([])
const userTotal = ref(0)
const userPage = ref(1)
const userQuery = reactive({ username: '', realName: '', role: '', collegeId: null })
const userDialogVisible = ref(false)
const userDialogTitle = ref('')
const userForm = reactive({ id: null, username: '', password: '', realName: '', role: '', collegeId: null, phone: '', status: 1 })
const userSaving = ref(false)
const passwordDialogVisible = ref(false)
const resetPwdUserId = ref(null)
const newPassword = ref('')

const roleOptions = [
  { value: 'college', label: '学院管理员' },
  { value: 'academic', label: '教务处管理员' },
  { value: 'admin', label: '校级管理员' }
]
const roleLabelMap = { college: '学院管理员', academic: '教务处', admin: '校级管理员' }

// ---- 用户搜索（实时搜索 + 回车） ----
let userSearchTimer = null

watch(userQuery, () => {
  clearTimeout(userSearchTimer)
  userSearchTimer = setTimeout(() => {
    handleUserSearch()
  }, 300)
}, { deep: true })

function handleUserSearch() {
  clearTimeout(userSearchTimer)
  userPage.value = 1
  loadUsers()
}

async function loadUsers() {
  clearTimeout(userSearchTimer)
  userLoading.value = true
  try {
    const params = { pageNum: userPage.value, pageSize: 20 }
    if (userQuery.username) params.username = userQuery.username
    if (userQuery.realName) params.realName = userQuery.realName
    if (userQuery.role) params.role = userQuery.role
    if (userQuery.collegeId) params.collegeId = userQuery.collegeId
    const res = await getUserPage(params)
    userList.value = res.records || []
    userTotal.value = res.total || 0
  } finally { userLoading.value = false }
}

function openUserForm(row) {
  if (row) {
    Object.assign(userForm, {
      id: row.id, username: row.username, password: '',
      realName: row.realName, role: row.role,
      collegeId: row.collegeId ?? null, phone: row.phone || '', status: row.status
    })
    userDialogTitle.value = '编辑用户'
  } else {
    Object.assign(userForm, {
      id: null, username: '', password: '', realName: '',
      role: '', collegeId: null, phone: '', status: 1
    })
    userDialogTitle.value = '新增用户'
  }
  userDialogVisible.value = true
}

async function saveUser() {
  userSaving.value = true
  try {
    const payload = {
      username: userForm.username,
      realName: userForm.realName,
      role: userForm.role,
      collegeId: userForm.collegeId,
      phone: userForm.phone,
      status: userForm.status
    }
    if (userForm.password) payload.password = userForm.password
    if (userForm.id) {
      await updateUser(userForm.id, payload)
    } else {
      await createUser(payload)
    }
    ElMessage.success(userForm.id ? '已更新' : '已创建')
    userDialogVisible.value = false
    loadUsers()
  } finally { userSaving.value = false }
}

async function handleDeleteUser(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteUser(row.id)
    ElMessage.success('已删除')
    loadUsers()
  } catch { /* cancel */ }
}

function openPasswordDialog(row) {
  resetPwdUserId.value = row.id
  newPassword.value = ''
  passwordDialogVisible.value = true
}

async function handleResetPassword() {
  if (!newPassword.value) { ElMessage.warning('请输入新密码'); return }
  await resetPassword(resetPwdUserId.value, newPassword.value)
  ElMessage.success('密码已重置')
  passwordDialogVisible.value = false
}

async function handleToggleUser(row) {
  await toggleUserStatus(row.id)
  loadUsers()
}

// ============================
//  Section 4: 操作日志
// ============================
const operLogLoading = ref(false)
const operLogList = ref([])
const operLogTotal = ref(0)
const operLogPage = ref(1)

async function loadOperLogs() {
  operLogLoading.value = true
  try {
    const res = await getOperLogs({ pageNum: operLogPage.value, pageSize: 20 })
    operLogList.value = res.records || []
    operLogTotal.value = res.total || 0
  } finally { operLogLoading.value = false }
}

// ============================
//  Section 5: 登录日志
// ============================
const loginLogLoading = ref(false)
const loginLogList = ref([])
const loginLogTotal = ref(0)
const loginLogPage = ref(1)

async function loadLoginLogs() {
  loginLogLoading.value = true
  try {
    const res = await getLoginLogs({ pageNum: loginLogPage.value, pageSize: 20 })
    loginLogList.value = res.records || []
    loginLogTotal.value = res.total || 0
  } finally { loginLogLoading.value = false }
}

// ---- 通用：切 Tab 加载 ----
watch(activeTab, (tab) => {
  if (tab === 'college') loadColleges()
  else if (tab === 'major') loadMajors()
  else if (tab === 'user') loadUsers()
  else if (tab === 'operLog') loadOperLogs()
  else if (tab === 'loginLog') loadLoginLogs()
})

onMounted(() => loadColleges())
</script>

<template>
  <div class="system-page">
    <h2 class="page-title">系统管理</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="学院管理" name="college" />
      <el-tab-pane label="专业管理" name="major" />
      <el-tab-pane label="账号管理" name="user" />
      <el-tab-pane label="操作日志" name="operLog" />
      <el-tab-pane label="登录日志" name="loginLog" />
    </el-tabs>

    <!-- ════ 学院管理 ════ -->
    <template v-if="activeTab === 'college'">
      <el-button type="primary" size="small" @click="openCollegeForm(null)" style="margin-bottom:12px">新增学院</el-button>
      <el-table :data="collegeList" v-loading="collegeLoading" stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" sortable />
        <el-table-column prop="name" label="学院名称" width="160" sortable />
        <el-table-column prop="code" label="编码" width="110" sortable />
        <el-table-column prop="sort" label="排序" width="80" sortable />
        <el-table-column prop="status" label="状态" width="90" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCollegeForm(row)">编辑</el-button>
            <el-button link size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleCollege(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 学院弹窗 -->
      <el-dialog v-model="collegeDialogVisible" :title="collegeDialogTitle" width="400px">
        <el-form :model="collegeForm" label-width="80px">
          <el-form-item label="名称" required><el-input v-model="collegeForm.name" /></el-form-item>
          <el-form-item label="编码" required><el-input v-model="collegeForm.code" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="collegeForm.sort" :min="0" /></el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="collegeForm.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer><el-button @click="collegeDialogVisible = false">取消</el-button><el-button type="primary" @click="saveCollege" :loading="collegeSaving">保存</el-button></template>
      </el-dialog>
    </template>

    <!-- ════ 专业管理 ════ -->
    <template v-if="activeTab === 'major'">
      <div style="margin-bottom:12px;display:flex;gap:10px;align-items:center">
        <el-select v-model="majorCollegeFilter" placeholder="按学院筛选" clearable @change="loadMajors" style="width:200px">
          <el-option v-for="c in collegeList" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" size="small" @click="openMajorForm(null)">新增专业</el-button>
      </div>
      <el-table :data="majorList" v-loading="majorLoading" stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" sortable />
        <el-table-column prop="collegeName" label="所属学院" width="140" sortable />
        <el-table-column prop="name" label="专业名称" width="160" sortable />
        <el-table-column prop="code" label="编码" width="110" sortable />
        <el-table-column prop="sort" label="排序" width="80" sortable />
        <el-table-column prop="status" label="状态" width="90" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openMajorForm(row)">编辑</el-button>
            <el-button link size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleMajor(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 专业弹窗 -->
      <el-dialog v-model="majorDialogVisible" :title="majorDialogTitle" width="420px">
        <el-form :model="majorForm" label-width="80px">
          <el-form-item label="所属学院" required>
            <el-select v-model="majorForm.collegeId" style="width:100%">
              <el-option v-for="c in collegeList" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="名称" required><el-input v-model="majorForm.name" /></el-form-item>
          <el-form-item label="编码" required><el-input v-model="majorForm.code" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="majorForm.sort" :min="0" /></el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="majorForm.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer><el-button @click="majorDialogVisible = false">取消</el-button><el-button type="primary" @click="saveMajor" :loading="majorSaving">保存</el-button></template>
      </el-dialog>
    </template>

    <!-- ════ 账号管理 ════ -->
    <template v-if="activeTab === 'user'">
      <div style="margin-bottom:12px;display:flex;gap:10px;flex-wrap:wrap;align-items:center">
        <el-input v-model="userQuery.username" placeholder="用户名" clearable style="width:140px" @keyup.enter="handleUserSearch" />
        <el-input v-model="userQuery.realName" placeholder="姓名" clearable style="width:120px" @keyup.enter="handleUserSearch" />
        <el-select v-model="userQuery.role" placeholder="角色" clearable style="width:130px">
          <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
        </el-select>
        <el-button type="primary" size="small" @click="handleUserSearch">查询</el-button>
        <el-button type="success" size="small" @click="openUserForm(null)">新增用户</el-button>
      </div>
      <el-table :data="userList" v-loading="userLoading" stripe row-key="id">
        <el-table-column prop="username" label="用户名" width="120" sortable />
        <el-table-column prop="realName" label="姓名" width="110" sortable />
        <el-table-column prop="role" label="角色" width="120" sortable>
          <template #default="{ row }">
            <el-tag size="small">{{ roleLabelMap[row.role] || row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="120" sortable />
        <el-table-column prop="status" label="状态" width="70" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openUserForm(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="openPasswordDialog(row)">重置密码</el-button>
            <el-button link size="small" :type="row.status === 1 ? '' : 'success'" @click="handleToggleUser(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDeleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="userPage" :total="userTotal" :page-size="20"
          layout="total, prev, pager, next" @current-change="loadUsers" />
      </div>
      <!-- 用户弹窗 -->
      <el-dialog v-model="userDialogVisible" :title="userDialogTitle" width="440px">
        <el-form :model="userForm" label-width="80px">
          <el-form-item label="用户名" required><el-input v-model="userForm.username" /></el-form-item>
          <el-form-item label="密码" :required="!userForm.id">
            <el-input v-model="userForm.password" :placeholder="userForm.id ? '留空不修改' : '请输入密码'" show-password />
          </el-form-item>
          <el-form-item label="姓名" required><el-input v-model="userForm.realName" /></el-form-item>
          <el-form-item label="角色" required>
            <el-select v-model="userForm.role" style="width:100%">
              <el-option v-for="r in roleOptions" :key="r.value" :label="r.label" :value="r.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="所属学院">
            <el-select v-model="userForm.collegeId" clearable style="width:100%">
              <el-option v-for="c in collegeList" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="手机号"><el-input v-model="userForm.phone" /></el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>
        <template #footer><el-button @click="userDialogVisible = false">取消</el-button><el-button type="primary" @click="saveUser" :loading="userSaving">保存</el-button></template>
      </el-dialog>
      <!-- 重置密码弹窗 -->
      <el-dialog v-model="passwordDialogVisible" title="重置密码" width="360px">
        <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
        <template #footer><el-button @click="passwordDialogVisible = false">取消</el-button><el-button type="primary" @click="handleResetPassword">确认重置</el-button></template>
      </el-dialog>
    </template>

    <!-- ════ 操作日志 ════ -->
    <template v-if="activeTab === 'operLog'">
      <el-table :data="operLogList" v-loading="operLogLoading" stripe row-key="id">
        <el-table-column prop="description" label="操作" min-width="140" sortable />
        <el-table-column prop="username" label="操作人" width="110" sortable />
        <el-table-column label="请求" width="150">
          <template #default="{ row }">
            <span style="font-size:12px;color:#86909C">{{ row.method }} {{ row.uri }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="status" label="状态" width="80" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '成功' : '异常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="80" sortable>
          <template #default="{ row }">{{ row.costMs }}ms</template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" sortable />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="operLogPage" :total="operLogTotal" :page-size="20"
          layout="total, prev, pager, next" @current-change="loadOperLogs" />
      </div>
    </template>

    <!-- ════ 登录日志 ════ -->
    <template v-if="activeTab === 'loginLog'">
      <el-table :data="loginLogList" v-loading="loginLogLoading" stripe row-key="id">
        <el-table-column prop="username" label="用户名" width="120" sortable />
        <el-table-column prop="status" label="结果" width="90" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip sortable />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="createTime" label="时间" width="170" sortable />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="loginLogPage" :total="loginLogTotal" :page-size="20"
          layout="total, prev, pager, next" @current-change="loadLoginLogs" />
      </div>
    </template>
  </div>
</template>

<style scoped>
.system-page { padding: 0; }
.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif; font-size: 20px; color: #1D2129; margin: 0 0 16px;
}
.pagination-wrap {
  display: flex; justify-content: flex-end; margin-top: 16px; padding: 12px 0;
}
</style>

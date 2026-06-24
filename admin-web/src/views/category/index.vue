<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAllCategories, createCategory, updateCategory, deleteCategory, toggleCategory } from '@/api/category'

// ---- 数据 ----
const loading = ref(false)
const categories = ref([])

// 分类树（按 parentId 分组显示）
const parentCategories = computed(() => categories.value.filter(c => c.parentId === 0))
const childMap = computed(() => {
  const map = {}
  categories.value.forEach(c => {
    if (c.parentId !== 0) {
      if (!map[c.parentId]) map[c.parentId] = []
      map[c.parentId].push(c)
    }
  })
  return map
})

// ---- 弹窗 ----
const dialogVisible = ref(false)
const dialogTitle = ref('')
const saving = ref(false)
const form = reactive({
  id: null, parentId: null, name: '', icon: '', sort: 0, status: 1
})

async function loadData() {
  loading.value = true
  try {
    categories.value = await listAllCategories()
  } finally {
    loading.value = false
  }
}

function openCreate(parentId = null) {
  Object.assign(form, { id: null, parentId, name: '', icon: '', sort: 0, status: 1 })
  dialogTitle.value = parentId ? '新增子分类' : '新增分类'
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id, parentId: row.parentId || 0,
    name: row.name, icon: row.icon || '',
    sort: row.sort ?? 0, status: row.status ?? 1
  })
  dialogTitle.value = '编辑分类'
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  saving.value = true
  try {
    const payload = {
      parentId: form.parentId || 0,
      name: form.name,
      icon: form.icon || null,
      sort: form.sort || 0,
      status: form.status
    }
    if (form.id) {
      await updateCategory(form.id, payload)
      ElMessage.success('已更新')
    } else {
      await createCategory(payload)
      ElMessage.success('已创建')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  // 检查是否有子分类
  const children = childMap.value[row.id]
  if (children?.length) {
    ElMessage.warning(`请先删除「${row.name}」下的 ${children.length} 个子分类`)
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteCategory(row.id)
    ElMessage.success('已删除')
    loadData()
  } catch { /* cancel */ }
}

async function handleToggle(row) {
  await toggleCategory(row.id)
  ElMessage.success(row.status === 1 ? '已停用' : '已启用')
  loadData()
}

function getChildren(parentId) {
  return childMap.value[parentId] || []
}

onMounted(loadData)
</script>

<template>
  <div class="category-page">
    <div class="page-header">
      <h2 class="page-title">分类管理</h2>
      <el-button type="primary" @click="openCreate(null)">新增分类</el-button>
    </div>

    <el-table :data="parentCategories" v-loading="loading" row-key="id" stripe>
      <el-table-column prop="name" label="分类名称" width="140" sortable>
        <template #default="{ row }">
          <span style="font-weight:600">{{ row.icon ? row.icon + ' ' : '' }}{{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" sortable />
      <el-table-column prop="status" label="状态" width="90" sortable>
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openCreate(row.id)">添加子分类</el-button>
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggle(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>

      <!-- 展开子分类 -->
      <el-table-column type="expand">
        <template #default="{ row }">
          <div v-if="getChildren(row.id).length" class="child-table-wrap">
            <table class="child-table">
              <thead>
                <tr>
                  <th style="width:140px">名称</th>
                  <th style="width:60px">排序</th>
                  <th style="width:80px">状态</th>
                  <th style="width:220px">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="child in getChildren(row.id)" :key="child.id">
                  <td>
                    <span style="padding-left:20px;color:#4E5969">
                      {{ child.icon ? child.icon + ' ' : '' }}{{ child.name }}
                    </span>
                  </td>
                  <td>{{ child.sort ?? 0 }}</td>
                  <td>
                    <el-tag size="small" :type="child.status === 1 ? 'success' : 'danger'">
                      {{ child.status === 1 ? '启用' : '停用' }}
                    </el-tag>
                  </td>
                  <td>
                    <el-button link type="primary" size="small" @click="openEdit(child)">编辑</el-button>
                    <el-button link size="small" :type="child.status === 1 ? 'warning' : 'success'" @click="handleToggle(child)">
                      {{ child.status === 1 ? '停用' : '启用' }}
                    </el-button>
                    <el-button link type="danger" size="small" @click="handleDelete(child)">删除</el-button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <el-empty v-else description="暂无子分类" :image-size="40" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" clearable placeholder="根分类（留空）" style="width:100%">
            <el-option :value="0" label="根分类" />
            <el-option
              v-for="c in parentCategories"
              :key="c.id"
              :label="c.name"
              :value="c.id"
              :disabled="c.id === form.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如「荣誉类」" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="emoji 如 🏆" style="width:120px" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.category-page { padding: 0; }

.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-family: 'HarmonyOS_SansSC_Bold', sans-serif;
  font-size: 20px; color: #1D2129; margin: 0;
}

.child-table-wrap {
  padding: 8px 0;
}

.child-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.child-table th,
.child-table td {
  padding: 8px 12px;
  text-align: left;
  border-bottom: 1px solid #F2F3F5;
}

.child-table th {
  color: #86909C;
  font-weight: normal;
  font-size: 12px;
}
</style>

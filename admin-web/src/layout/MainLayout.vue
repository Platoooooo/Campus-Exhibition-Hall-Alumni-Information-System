<script setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { menuRoutes } from '@/router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)

// 角色权限的菜单项
const visibleMenus = computed(() =>
  menuRoutes.filter(item => {
    const roles = item.meta.roles
    if (!roles) return true
    return roles.includes(userStore.role)
  })
)

// 角色中文名映射
const roleLabelMap = { admin: '校级管理员', academic: '教务处', college: '学院管理员' }
const roleLabel = computed(() => roleLabelMap[userStore.role] || userStore.role)

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <el-container style="height:100vh">
    <!-- 侧边栏 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo-area" :class="{ collapsed }">
        <span class="logo-icon">🏛</span>
        <span v-show="!collapsed" class="logo-text">校园展览馆</span>
      </div>

      <el-menu
        :default-active="route.path"
        :collapse="collapsed"
        background-color="#1D2129"
        text-color="#86909C"
        active-text-color="#FFFFFF"
        router
      >
        <el-menu-item
          v-for="item in visibleMenus"
          :key="item.path"
          :index="'/' + item.path"
        >
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <template #title>{{ item.meta.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="top-header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            @click="collapsed = !collapsed"
          >
            <component :is="collapsed ? 'Expand' : 'Fold'" />
          </el-icon>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-trigger">
              <el-icon><UserFilled /></el-icon>
              <span class="user-name">{{ userStore.realName }}</span>
              <el-tag size="small" type="info">{{ roleLabel }}</el-tag>
              <el-icon style="margin-left:4px"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  {{ userStore.realName }} · {{ roleLabel }}
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
/* ---- 侧边栏 ---- */
.sidebar {
  background: #1D2129;
  transition: width 0.3s;
  overflow: hidden;
}

.logo-area {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  gap: 10px;
  transition: all 0.3s;
}

.logo-area.collapsed {
  justify-content: center;
  gap: 0;
}

.logo-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.logo-text {
  font-family: 'HarmonyOS_SansSC_Bold', 'Microsoft YaHei', sans-serif;
  font-size: 16px;
  color: #fff;
  white-space: nowrap;
}

/* 菜单项选中态背景 */
:deep(.el-menu-item.is-active) {
  background-color: #2B5AED !important;
}

/* ---- 顶栏 ---- */
.top-header {
  background: #fff;
  border-bottom: 1px solid #E5E6EB;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  font-size: 20px;
  color: #4E5969;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.collapse-btn:hover {
  background: #F2F3F5;
  color: #2B5AED;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: background 0.2s;
  color: #4E5969;
  font-size: 14px;
}

.user-trigger:hover {
  background: #F2F3F5;
}

.user-name {
  font-family: 'HarmonyOS_SansSC_Bold', 'Microsoft YaHei', sans-serif;
  color: #1D2129;
}

/* ---- 内容区 ---- */
.main-content {
  background: #F5F7FB;
  padding: 24px;
  overflow-y: auto;
}
</style>

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 菜单路由配置
 * roles: 允许访问的角色列表，省略表示所有角色均可访问
 */
export const menuRoutes = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '工作台', icon: 'Monitor' }
  },
  {
    path: 'alumni',
    name: 'Alumni',
    component: () => import('@/views/alumni/index.vue'),
    meta: { title: '校友管理', icon: 'User' }
  },
  {
    path: 'archive',
    name: 'Archive',
    component: () => import('@/views/archive/index.vue'),
    meta: { title: '资料档案', icon: 'Document' }
  },
  {
    path: 'audit',
    name: 'Audit',
    component: () => import('@/views/audit/index.vue'),
    meta: { title: '审核中心', icon: 'Checked' }
  },
  {
    path: 'category',
    name: 'Category',
    component: () => import('@/views/category/index.vue'),
    meta: { title: '分类管理', icon: 'Grid' }
  },
  {
    path: 'operation',
    name: 'Operation',
    component: () => import('@/views/operation/index.vue'),
    meta: { title: '资料库运营', icon: 'Setting' }
  },
  {
    path: 'system',
    name: 'System',
    component: () => import('@/views/system/index.vue'),
    meta: { title: '系统管理', icon: 'Tools', roles: ['admin'] }
  }
]

// 非菜单路由（编辑页等细节页面，不出现在侧边栏）
export const hiddenRoutes = [
  {
    path: 'archive/create',
    name: 'ArchiveCreate',
    component: () => import('@/views/archive/edit/index.vue'),
    meta: { title: '新建档案' }
  },
  {
    path: 'archive/:id/edit',
    name: 'ArchiveEdit',
    component: () => import('@/views/archive/edit/index.vue'),
    meta: { title: '编辑档案' }
  }
]

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', anonymous: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    children: [...menuRoutes, ...hiddenRoutes]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// ---- 全局路由守卫 ----
router.beforeEach((to, from, next) => {
  // 页面标题
  document.title = to.meta.title ? `${to.meta.title} · 福软校友展览馆` : '福软校友展览馆 · 管理后台'

  const userStore = useUserStore()

  // 匿名页面（登录页）直接放行
  if (to.meta.anonymous) {
    if (userStore.isLoggedIn) {
      return next('/dashboard')
    }
    return next()
  }

  // 未登录 → 跳转登录页
  if (!userStore.isLoggedIn) {
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  // 角色权限校验
  const allowedRoles = to.meta.roles
  if (allowedRoles && !allowedRoles.includes(userStore.role)) {
    return next('/dashboard')
  }

  next()
})

export default router

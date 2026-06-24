import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/carousel'
  },
  {
    path: '/carousel',
    name: 'Carousel',
    component: () => import('@/views/carousel/index.vue'),
    meta: { title: '默认轮播' }
  },
  {
    path: '/wall',
    name: 'Wall',
    component: () => import('@/views/wall/index.vue'),
    meta: { title: '校友墙' }
  },
  {
    path: '/detail/:id',
    name: 'Detail',
    component: () => import('@/views/detail/index.vue'),
    meta: { title: '校友详情' }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/search/index.vue'),
    meta: { title: '查询' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

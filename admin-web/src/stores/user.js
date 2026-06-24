import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // ---- 状态 ----
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(loadUserInfo())

  function loadUserInfo() {
    try {
      const raw = localStorage.getItem('userInfo')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  // ---- 计算属性 ----
  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const realName = computed(() => userInfo.value?.realName || userInfo.value?.username || '')

  /** 是否为校级/教务处管理员，可访问全校数据 */
  const isCampusWide = computed(() => ['admin', 'academic'].includes(role.value))

  // ---- 操作 ----
  function setToken(val) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }

  /**
   * 登录
   * @param {string} username
   * @param {string} password
   */
  async function loginAction(username, password) {
    const data = await loginApi(username, password)
    setToken(data.token)
    setUserInfo(data.user)
    ElMessage.success(`欢迎，${data.user.realName || data.user.username}`)
    return data
  }

  /** 退出登录 */
  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    role,
    realName,
    isCampusWide,
    setToken,
    setUserInfo,
    loginAction,
    logout
  }
})

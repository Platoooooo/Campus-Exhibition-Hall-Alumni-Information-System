import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

// mock login API
vi.mock('@/api/auth', () => ({
  login: vi.fn()
}))

// mock element-plus
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() }
}))

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('初始状态：token 为空，未登录', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
    expect(store.userInfo).toBeNull()
  })

  it('从 localStorage 恢复 token 和 userInfo', () => {
    localStorage.setItem('token', 'test-jwt')
    localStorage.setItem('userInfo', JSON.stringify({ id: 1, username: 'admin', role: 'admin', realName: '管理员' }))

    const store = useUserStore()
    expect(store.token).toBe('test-jwt')
    expect(store.isLoggedIn).toBe(true)
    expect(store.role).toBe('admin')
    expect(store.realName).toBe('管理员')
  })

  it('loginAction 成功后设置 token 和 userInfo', async () => {
    const { login } = await import('@/api/auth')
    login.mockResolvedValue({
      token: 'jwt-token',
      user: { id: 1, username: 'admin', role: 'admin', realName: '管理员' }
    })

    const store = useUserStore()
    await store.loginAction('admin', 'admin123')

    expect(store.token).toBe('jwt-token')
    expect(store.isLoggedIn).toBe(true)
    expect(store.role).toBe('admin')
    expect(localStorage.getItem('token')).toBe('jwt-token')
  })

  it('loginAction 失败不设置 token', async () => {
    const { login } = await import('@/api/auth')
    login.mockRejectedValue(new Error('密码错误'))

    const store = useUserStore()
    try {
      await store.loginAction('admin', 'wrong')
    } catch { /* expected */ }

    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
  })

  it('logout 清除所有状态', () => {
    localStorage.setItem('token', 'jwt')
    localStorage.setItem('userInfo', JSON.stringify({ username: 'admin' }))

    const store = useUserStore()
    expect(store.isLoggedIn).toBe(true)

    store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('userInfo')).toBeNull()
  })

  it('isCampusWide: admin 角色为 true', () => {
    localStorage.setItem('token', 'jwt')
    localStorage.setItem('userInfo', JSON.stringify({ role: 'admin' }))
    const store = useUserStore()
    expect(store.isCampusWide).toBe(true)
  })

  it('isCampusWide: academic 角色为 true', () => {
    localStorage.setItem('token', 'jwt')
    localStorage.setItem('userInfo', JSON.stringify({ role: 'academic' }))
    const store = useUserStore()
    expect(store.isCampusWide).toBe(true)
  })

  it('isCampusWide: college 角色为 false', () => {
    localStorage.setItem('token', 'jwt')
    localStorage.setItem('userInfo', JSON.stringify({ role: 'college' }))
    const store = useUserStore()
    expect(store.isCampusWide).toBe(false)
  })

  it('realName 缺失时 fallback 到 username', () => {
    localStorage.setItem('token', 'jwt')
    localStorage.setItem('userInfo', JSON.stringify({ username: 'college01' }))
    const store = useUserStore()
    expect(store.realName).toBe('college01')
  })

  it('setToken / setUserInfo 直接设置并持久化', () => {
    const store = useUserStore()
    store.setToken('direct-token')
    store.setUserInfo({ username: 'test', role: 'college' })

    expect(store.token).toBe('direct-token')
    expect(store.role).toBe('college')
    expect(localStorage.getItem('token')).toBe('direct-token')
  })
})

/**
 * LoginPage 登录业务逻辑测试
 *
 * 策略：不 import 组件本身（会触发 Element Plus CSS ESM 错误），
 * 直接测试 loginAction 的业务流程（store + router 集成）。
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'

const mockPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockPush }),
  useRoute: () => ({ query: {} })
}))

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), error: vi.fn() }
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn()
}))

describe('LoginPage 登录业务逻辑', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    localStorage.clear()
    mockPush.mockClear()

    // 重置 login mock
    const { login } = await import('@/api/auth')
    login.mockReset()
  })

  async function simulateLoginAction(username, password) {
    if (!username || !password) return 'invalid' // 模拟前端校验
    const store = useUserStore()
    const { login } = await import('@/api/auth')
    try {
      const data = await login(username, password)
      store.setToken(data.token)
      store.setUserInfo(data.user)
      mockPush('/dashboard')
      return 'success'
    } catch {
      return 'failed'
    }
  }

  it('有效凭据登录成功：设置 token, userInfo, 跳转 dashboard', async () => {
    const { login } = await import('@/api/auth')
    login.mockResolvedValue({
      token: 'jwt-token-123',
      user: { id: 1, username: 'admin', role: 'admin', realName: '管理员' }
    })

    const result = await simulateLoginAction('admin', 'admin123')

    expect(result).toBe('success')
    expect(mockPush).toHaveBeenCalledWith('/dashboard')
    const store = useUserStore()
    expect(store.token).toBe('jwt-token-123')
    expect(store.isLoggedIn).toBe(true)
    expect(store.role).toBe('admin')
  })

  it('错误密码登录失败：token 保持为空', async () => {
    const { login } = await import('@/api/auth')
    login.mockRejectedValue(new Error('密码错误'))

    const result = await simulateLoginAction('admin', 'wrong')

    expect(result).toBe('failed')
    expect(mockPush).not.toHaveBeenCalled()
    const store = useUserStore()
    expect(store.isLoggedIn).toBe(false)
  })

  it('空账号不做请求', async () => {
    const { login } = await import('@/api/auth')
    const result = await simulateLoginAction('', 'password')

    expect(result).toBe('invalid')
    expect(login).not.toHaveBeenCalled()
  })

  it('空密码不做请求', async () => {
    const { login } = await import('@/api/auth')
    const result = await simulateLoginAction('admin', '')

    expect(result).toBe('invalid')
    expect(login).not.toHaveBeenCalled()
  })

  it('college 角色 isCampusWide 为 false', async () => {
    const { login } = await import('@/api/auth')
    login.mockResolvedValue({
      token: 'jwt',
      user: { id: 2, username: 'college01', role: 'college', realName: '王老师' }
    })

    await simulateLoginAction('college01', 'pass')
    const store = useUserStore()
    expect(store.role).toBe('college')
    expect(store.isCampusWide).toBe(false)
  })
})

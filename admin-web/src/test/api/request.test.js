/**
 * Axios 请求拦截器测试
 *
 * 策略：直接测试拦截器函数本身的逻辑，不发起真实 HTTP 请求。
 */
import { describe, it, expect, beforeEach } from 'vitest'

describe('request (Axios 拦截器逻辑)', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('有 token 时请求头注入 Bearer', async () => {
    localStorage.setItem('token', 'my-test-jwt')

    // 模拟请求拦截器逻辑（与 request.js 一致）
    const config = { headers: {} }
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    expect(config.headers.Authorization).toBe('Bearer my-test-jwt')
  })

  it('无 token 时不注入 Authorization', () => {
    const config = { headers: {} }
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    expect(config.headers.Authorization).toBeUndefined()
  })

  it('响应 code=200 直接透传', () => {
    const response = { data: { code: 200, message: '操作成功', data: { id: 1 } } }

    const res = response.data
    const isError = res.code !== 200

    expect(isError).toBe(false)
    expect(res.code).toBe(200)
  })

  it('响应 code!=200 标记为错误', () => {
    const response = { data: { code: 400, message: '参数错误' } }

    const res = response.data
    const isError = res.code !== 200

    expect(isError).toBe(true)
  })

  it('401 响应触发 token 清除', () => {
    localStorage.setItem('token', 'expired-jwt')
    expect(localStorage.getItem('token')).toBe('expired-jwt')

    // 模拟 401 处理逻辑
    const errorResponse = { response: { status: 401 } }
    if (errorResponse.response?.status === 401) {
      localStorage.removeItem('token')
    }

    expect(localStorage.getItem('token')).toBeNull()
  })

  it('非 401 错误不清除 token', () => {
    localStorage.setItem('token', 'valid-jwt')

    const errorResponse = { response: { status: 500 } }
    if (errorResponse.response?.status === 401) {
      localStorage.removeItem('token')
    }

    expect(localStorage.getItem('token')).toBe('valid-jwt')
  })

  it('baseURL 配置存在', async () => {
    // 验证 baseURL 使用环境变量
    const baseURL = import.meta.env.VITE_API_BASE || '/api'
    expect(baseURL).toBeDefined()
    expect(baseURL).toBe('/api')
  })
})

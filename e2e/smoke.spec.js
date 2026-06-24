/**
 * 端到端冒烟测试
 *
 * 流程：登录 → 创建校友 → 创建档案 → 提交审核 → 验证状态
 *
 * 运行方式：
 *   1. 启动后端: cd server && mvn spring-boot:run
 *   2. 运行测试: cd e2e && npm test
 */

import { test, expect } from '@playwright/test'

const BASE = process.env.BASE_URL || 'http://localhost:8080'

let token
let alumniId
let archiveId

test.describe('E2E 冒烟链路', () => {

  test('1. 登录获取 JWT token', async ({ request }) => {
    const res = await request.post(`${BASE}/api/auth/login`, {
      data: { username: 'admin', password: 'admin123' }
    })
    expect(res.status()).toBe(200)

    const body = await res.json()
    expect(body.code).toBe(200)
    expect(body.data.token).toBeTruthy()
    expect(body.data.user.username).toBe('admin')

    token = body.data.token
  })

  test('2. 创建校友', async ({ request }) => {
    expect(token).toBeTruthy()

    const res = await request.post(`${BASE}/api/alumni`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        studentNo: 'e2e-test-001',
        name: 'E2E测试校友',
        gender: 1,
        collegeId: 1,
        identity: 1
      }
    })
    expect(res.status()).toBe(200)

    const body = await res.json()
    // 学号可能已存在（重跑场景）
    if (body.code === 200) {
      alumniId = body.data.id
    } else {
      // 学号已存在，查询已有记录
      const listRes = await request.get(`${BASE}/api/alumni?name=E2E测试校友`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      const listBody = await listRes.json()
      if (listBody.data?.records?.length > 0) {
        alumniId = listBody.data.records[0].id
      }
    }
    expect(alumniId).toBeTruthy()
  })

  test('3. 创建资料档案', async ({ request }) => {
    expect(token).toBeTruthy()
    expect(alumniId).toBeTruthy()

    const res = await request.post(`${BASE}/api/archive`, {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        alumniId,
        categoryId: 1,
        collegeId: 1,
        title: 'E2E冒烟测试档案',
        content: '由自动化测试创建的档案',
        eventDate: '2026-01-01'
      }
    })
    expect(res.status()).toBe(200)

    const body = await res.json()
    expect(body.code).toBe(200)
    expect(body.data.status).toBe('draft')
    archiveId = body.data.id
  })

  test('4. 提交审核 draft → pending_college', async ({ request }) => {
    expect(token).toBeTruthy()
    expect(archiveId).toBeTruthy()

    const res = await request.put(`${BASE}/api/audit/${archiveId}/submit`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(res.status()).toBe(200)

    const body = await res.json()
    expect(body.code).toBe(200)
    // 验证状态已变为待学院审核
    const detailRes = await request.get(`${BASE}/api/archive/${archiveId}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const detail = await detailRes.json()
    expect(detail.data.status).toBe('pending_college')
  })

  test('5. 大屏公开接口可访问', async ({ request }) => {
    const res = await request.get(`${BASE}/api/screen/carousel`)
    expect(res.status()).toBe(200)

    const body = await res.json()
    expect(body.code).toBe(200)
  })

  test('6. 健康检查正常', async ({ request }) => {
    const res = await request.get(`${BASE}/api/ping`)
    expect(res.status()).toBe(200)
  })

  test('7. 未认证请求被拒绝', async ({ request }) => {
    const res = await request.put(`${BASE}/api/audit/1/submit`)
    expect(res.status()).toBe(200)

    const body = await res.json()
    // 401 UNAUTHORIZED in R body
    expect(body.code).toBe(401)
  })
})

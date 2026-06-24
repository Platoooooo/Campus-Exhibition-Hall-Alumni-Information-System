import request from './request'

/** 获取工作台聚合统计 */
export function getDashboardStats() {
  return request.get('/dashboard/stats').then(res => res.data)
}

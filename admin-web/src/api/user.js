import request from './request'

/** 用户分页列表 */
export function getUserPage(params) {
  return request.get('/user', { params }).then(res => res.data)
}

/** 创建用户 */
export function createUser(data) {
  return request.post('/user', data).then(res => res.data)
}

/** 更新用户 */
export function updateUser(id, data) {
  return request.put(`/user/${id}`, data).then(res => res.data)
}

/** 删除用户 */
export function deleteUser(id) {
  return request.delete(`/user/${id}`)
}

/** 重置密码 */
export function resetPassword(id, newPassword) {
  return request.put(`/user/${id}/reset-password`, { newPassword })
}

/** 启停用户 */
export function toggleUserStatus(id) {
  return request.put(`/user/${id}/toggle`)
}

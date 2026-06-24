import request from './request'

/** 获取当前角色的待办列表 */
export function getTodoList(params) {
  const { role, pageNum, pageSize } = params
  const path = role === 'academic' ? '/audit/academic/todo' : '/audit/college/todo'
  return request.get(path, { params: { pageNum, pageSize } }).then(res => res.data)
}

/** 获取档案的审核记录 */
export function getAuditLogs(archiveId) {
  return request.get(`/audit/${archiveId}/logs`).then(res => res.data)
}

/** 学院审核：通过 */
export function collegeApprove(archiveId, opinion) {
  return request.put(`/audit/${archiveId}/college/approve`, opinion ? { opinion } : null).then(res => res.data)
}

/** 学院审核：驳回 */
export function collegeReject(archiveId, opinion) {
  return request.put(`/audit/${archiveId}/college/reject`, { opinion }).then(res => res.data)
}

/** 教务处审核：通过 */
export function academicApprove(archiveId, opinion) {
  return request.put(`/audit/${archiveId}/academic/approve`, opinion ? { opinion } : null).then(res => res.data)
}

/** 教务处审核：驳回 */
export function academicReject(archiveId, opinion) {
  return request.put(`/audit/${archiveId}/academic/reject`, { opinion }).then(res => res.data)
}

import request from './request'

/** 分页查询档案列表 */
export function getArchivePage(params) {
  return request.get('/archive', { params }).then(res => res.data)
}

/** 获取档案详情（含媒体列表） */
export function getArchiveById(id) {
  return request.get(`/archive/${id}`).then(res => res.data)
}

/** 新增档案 */
export function createArchive(data) {
  return request.post('/archive', data).then(res => res.data)
}

/** 更新档案 */
export function updateArchive(id, data) {
  return request.put(`/archive/${id}`, data).then(res => res.data)
}

/** 删除档案 */
export function deleteArchive(id) {
  return request.delete(`/archive/${id}`).then(res => res.data)
}

/** 为档案上传媒体 */
export function addMedia(archiveId, file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/archive/${archiveId}/media`, formData).then(res => res.data)
}

/** 删除档案媒体 */
export function removeMedia(archiveId, mediaId) {
  return request.delete(`/archive/${archiveId}/media/${mediaId}`)
}

/** 媒体排序 */
export function sortMedia(archiveId, mediaIds) {
  return request.put(`/archive/${archiveId}/media/sort`, mediaIds)
}

/** 提交审核 */
export function submitForReview(archiveId) {
  return request.put(`/audit/${archiveId}/submit`).then(res => res.data)
}

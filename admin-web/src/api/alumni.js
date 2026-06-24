import request from './request'

/** 分页查询校友列表 */
export function getAlumniPage(params) {
  return request.get('/alumni', { params }).then(res => res.data)
}

/** 获取校友详情 */
export function getAlumniById(id) {
  return request.get(`/alumni/${id}`).then(res => res.data)
}

/** 新增校友 */
export function createAlumni(data) {
  return request.post('/alumni', data).then(res => res.data)
}

/** 更新校友 */
export function updateAlumni(id, data) {
  return request.put(`/alumni/${id}`, data).then(res => res.data)
}

/** 删除校友 */
export function deleteAlumni(id) {
  return request.delete(`/alumni/${id}`).then(res => res.data)
}

/** 下载 Excel 导入模板（返回 blob） */
export function downloadTemplate() {
  return request.get('/alumni/template', { responseType: 'blob' })
}

/** 上传 Excel 导入校友 */
export function importExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/alumni/import', formData).then(res => res.data)
}

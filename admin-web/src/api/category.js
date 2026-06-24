import request from './request'

/** 获取所有分类（含停用） */
export function listAllCategories() {
  return request.get('/category/all').then(res => res.data)
}

/** 新增分类 */
export function createCategory(data) {
  return request.post('/category', data).then(res => res.data)
}

/** 更新分类 */
export function updateCategory(id, data) {
  return request.put(`/category/${id}`, data).then(res => res.data)
}

/** 删除分类 */
export function deleteCategory(id) {
  return request.delete(`/category/${id}`)
}

/** 启停分类 */
export function toggleCategory(id) {
  return request.put(`/category/${id}/toggle`)
}

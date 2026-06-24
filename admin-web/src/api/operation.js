import request from './request'

// ---- 上架/下架 ----
export function publishArchive(id) {
  return request.put(`/operation/archive/${id}/publish`).then(res => res.data)
}

export function unpublishArchive(id) {
  return request.put(`/operation/archive/${id}/unpublish`).then(res => res.data)
}

// ---- 运营属性 ----
export function setTop(id, value) {
  return request.put(`/operation/archive/${id}/top`, null, { params: { value } }).then(res => res.data)
}

export function setRecommend(id, value) {
  return request.put(`/operation/archive/${id}/recommend`, null, { params: { value } }).then(res => res.data)
}

export function setDisplaySort(id, value) {
  return request.put(`/operation/archive/${id}/sort`, null, { params: { value } }).then(res => res.data)
}

// ---- 轮播方案 CRUD ----
export function listCarousels() {
  return request.get('/operation/carousel').then(res => res.data)
}

export function getCarousel(id) {
  return request.get(`/operation/carousel/${id}`).then(res => res.data)
}

export function createCarousel(data) {
  return request.post('/operation/carousel', data).then(res => res.data)
}

export function updateCarousel(id, data) {
  return request.put(`/operation/carousel/${id}`, data).then(res => res.data)
}

export function deleteCarousel(id) {
  return request.delete(`/operation/carousel/${id}`)
}

// ---- 轮播池内容 ----
export function addCarouselItem(carouselId, data) {
  return request.post(`/operation/carousel/${carouselId}/item`, data).then(res => res.data)
}

export function removeCarouselItem(carouselId, itemId) {
  return request.delete(`/operation/carousel/${carouselId}/item/${itemId}`)
}

export function sortCarouselItems(carouselId, itemIds) {
  return request.put(`/operation/carousel/${carouselId}/items/sort`, itemIds)
}

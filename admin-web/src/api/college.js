import request from './request'

/** 获取所有启用的学院列表（下拉框用） */
export function listAllColleges() {
  return request.get('/college/all').then(res => res.data)
}

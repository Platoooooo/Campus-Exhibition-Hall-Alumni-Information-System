import request from './request'

/** 按学院查专业列表（级联下拉框用） */
export function listMajorsByCollege(collegeId) {
  return request.get('/major/by-college', { params: { collegeId } }).then(res => res.data)
}

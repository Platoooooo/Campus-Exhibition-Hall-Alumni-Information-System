import request from './request'

/** 操作日志分页 */
export function getOperLogs(params) {
  return request.get('/log/oper', { params }).then(res => res.data)
}

/** 登录日志分页 */
export function getLoginLogs(params) {
  return request.get('/log/login', { params }).then(res => res.data)
}

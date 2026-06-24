import request from './request'

/**
 * 登录
 * @param {string} username
 * @param {string} password
 * @returns {Promise<{token: string, user: object}>}
 */
export function login(username, password) {
  return request.post('/auth/login', { username, password }).then(res => res.data)
}

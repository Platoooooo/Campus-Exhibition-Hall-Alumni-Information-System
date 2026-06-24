import request from './request'

/** 获取默认轮播方案 */
export function getCarousel() {
  return request.get('/screen/carousel').then(res => res.data)
}

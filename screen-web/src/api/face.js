import request from './request'

/**
 * 大屏人脸识别
 * @param {string} imageBase64 摄像头抓帧 base64（不含 data: 前缀）
 * @param {string} device 设备标识
 * @returns {Promise<{status, alumniId, score, alumni, timeline}>}
 */
export function recognize(imageBase64, device = 'screen-01') {
  return request.post('/face/recognize', {
    image: imageBase64,
    device
  }).then(res => res.data)
}

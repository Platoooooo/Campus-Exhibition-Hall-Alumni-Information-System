import request from './request'

/** 上传文件（图片/视频），返回 { url, thumbnail, ... } */
export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/media/upload', formData).then(res => res.data)
}

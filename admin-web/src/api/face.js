import request from './request'

/** 录入校友人脸 */
export function enrollFace(alumniId, file) {
  const formData = new FormData()
  formData.append('alumniId', alumniId)
  formData.append('file', file)
  return request.post('/face/enroll', formData).then(res => res.data)
}

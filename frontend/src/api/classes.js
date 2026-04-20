import { apiDelete, apiGet, apiPost } from '@/api/http'

export function fetchClasses(viewerUserId, keyword = '') {
  const query = new URLSearchParams({ viewerUserId: String(viewerUserId) })
  if (keyword?.trim()) query.set('keyword', keyword.trim())
  return apiGet(`/classes?${query.toString()}`)
}

export function createClass(payload) {
  return apiPost('/classes', payload)
}

export function deleteClass(id, operatorUserId) {
  return apiDelete(`/classes/${id}?operatorUserId=${operatorUserId}`)
}

export function batchInviteStudents(classId, payload) {
  return apiPost(`/classes/${classId}/invite`, payload)
}

export function fetchClassStudentRecords(classId, viewerUserId) {
  return apiGet(`/classes/${classId}/students/records?viewerUserId=${viewerUserId}`)
}

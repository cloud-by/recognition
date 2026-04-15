import { apiDelete, apiGet, apiPost } from '@/api/http'

export function fetchTeacherClasses(teacherId) {
  return apiGet(`/classes?teacherId=${teacherId}`)
}

export function createClass(payload) {
  return apiPost('/classes', payload)
}

export function deleteClass(id, teacherId) {
  return apiDelete(`/classes/${id}?teacherId=${teacherId}`)
}

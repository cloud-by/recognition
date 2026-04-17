import { apiDelete, apiGet, apiPost } from '@/api/http'
import { getAuthUser } from '@/utils/auth'

export async function getProblemTags(keyword = '') {
  const params = new URLSearchParams()
  if (keyword?.trim()) params.set('keyword', keyword.trim())
  const query = params.toString() ? `?${params.toString()}` : ''
  const resp = await apiGet(`/problem-tags${query}`)
  return (resp.data || []).map((item) => ({
    id: item.id,
    name: item.name,
  }))
}

export async function addProblemTag(tagName) {
  const user = getAuthUser()
  const resp = await apiPost('/problem-tags', {
    operatorUserId: user?.id,
    name: String(tagName || '').trim(),
  })
  if (!resp?.success) throw new Error(resp?.message || '新增标签失败')
  return resp.data
}

export async function removeProblemTag(tagId) {
  const user = getAuthUser()
  const resp = await apiDelete(`/problem-tags/${tagId}?operatorUserId=${user?.id}`)
  if (!resp?.success) throw new Error(resp?.message || '删除标签失败')
  return (resp.data || []).map((item) => ({ id: item.id, name: item.name }))
}

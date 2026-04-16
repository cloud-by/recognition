import { apiGet, apiPost, apiPut } from '@/api/http'

export function fetchContestList(viewerUserId) {
  const query = viewerUserId ? `?viewerUserId=${viewerUserId}` : ''
  return apiGet(`/contests${query}`)
}

export function fetchContestDetail(id) {
  return apiGet(`/contests/${id}`)
}

export function fetchContestProblemOptions() {
  return apiGet('/contests/problems/options')
}

export function createContest(payload) {
  return apiPost('/contests', payload)
}

export function updateContest(contestId, payload) {
  return apiPut(`/contests/${contestId}`, payload)
}

export function registerContest(contestId, userId) {
  return apiPost(`/contests/${contestId}/register`, { userId })
}

export function enterContest(contestId, userId) {
  return apiPost(`/contests/${contestId}/enter`, { userId })
}

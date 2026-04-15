const AUTH_KEY = 'oj_auth_user'
const AUTH_EVENT = 'oj-auth-changed'

function notifyAuthChange() {
  window.dispatchEvent(new CustomEvent(AUTH_EVENT))
}

export function getAuthUser() {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function setAuthUser(user) {
  if (!user) {
    localStorage.removeItem(AUTH_KEY)
    notifyAuthChange()
    return
  }
  localStorage.setItem(AUTH_KEY, JSON.stringify(user))
  notifyAuthChange()
}

export function getAuthChangeEventName() {
  return AUTH_EVENT
}

export function isLoggedIn() {
  const user = getAuthUser()
  return Boolean(user?.id)
}

export function isAdminUser() {
  const user = getAuthUser()
  return user?.role === 'ADMIN'
}

export function isTeacherUser() {
  const user = getAuthUser()
  return user?.role === 'TEACHER'
}

export function isManagerUser() {
  const user = getAuthUser()
  return user?.role === 'ADMIN' || user?.role === 'TEACHER'
}

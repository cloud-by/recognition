const AUTH_KEY = 'oj_auth_user'

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
    return
  }
  localStorage.setItem(AUTH_KEY, JSON.stringify(user))
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

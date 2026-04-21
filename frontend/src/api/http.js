const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const TOKEN_KEY = 'oj_token'  // token 存储的 key

function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export async function apiGet(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'token': getToken()
    }
  })
  if (!response.ok) {
    throw new Error(`GET ${path} failed: ${response.status}`)
  }
  return response.json()
}

export async function apiPost(path, body) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'token': getToken()
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    throw new Error(`POST ${path} failed: ${response.status}`)
  }

  return response.json()
}

export async function apiPut(path, body) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'PUT',
    headers: { 
      'Content-Type': 'application/json',
      'token': getToken()
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    throw new Error(`PUT ${path} failed: ${response.status}`)
  }

  return response.json()
}

export async function apiDelete(path) {
  const response = await fetch(`${API_BASE_URL}${path}`, { 
    method: 'DELETE',
    headers: {
      'token': getToken()
    }
  })
  if (!response.ok) throw new Error(`DELETE ${path} failed: ${response.status}`)
  return response.json()
}

export async function apiPatch(path, body) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'PATCH',
    headers: { 
      'Content-Type': 'application/json',
      'token': getToken()
    },
    body: JSON.stringify(body),
  })

  if (!response.ok) {
    throw new Error(`PATCH ${path} failed: ${response.status}`)
  }

  return response.json()
}
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/sign/').replace(/\/$/, '')
const adminApiBaseUrl = (import.meta.env.VITE_ADMIN_API_BASE_URL || '/maxkey-mgt-api/').replace(/\/$/, '')

export interface ApiResponse<T = unknown> {
  code?: number
  msg?: string
  message?: string
  data?: T
}

export class ApiError extends Error {
  constructor(message: string, public readonly code?: number, public readonly data?: unknown) {
    super(message)
    this.name = 'ApiError'
  }
}

let refreshPromise: Promise<boolean> | null = null

function buildUrl(path: string, params?: Record<string, unknown>, baseUrl = apiBaseUrl): string {
  const url = new URL(`${baseUrl}/${path.replace(/^\//, '')}`, window.location.origin)
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, String(value))
    }
  })
  return url.toString()
}

async function refreshToken(): Promise<boolean> {
  if (refreshPromise) return refreshPromise
  refreshPromise = (async () => {
    try {
      const saved = JSON.parse(localStorage.getItem('_token') || '{}') as { refresh_token?: string }
      if (!saved.refresh_token) return false
      const response = await fetch(buildUrl('/auth/token/refresh', { refresh_token: saved.refresh_token }, apiBaseUrl), {
        method: 'POST',
        credentials: 'include',
        headers: { Accept: 'application/json', AuthServer: 'MaxKey', hostname: window.location.hostname },
      })
      const body = await response.json().catch(() => ({})) as ApiResponse<Record<string, unknown>>
      if (!response.ok || body.code !== undefined && body.code !== 0 || !body.data?.token) return false
      localStorage.setItem('_token', JSON.stringify({ ...saved, ...body.data }))
      window.dispatchEvent(new CustomEvent('maxkey:token-refreshed', { detail: body.data }))
      return true
    } catch {
      return false
    } finally {
      refreshPromise = null
    }
  })()
  return refreshPromise
}

export async function request<T>(path: string, options: RequestInit = {}, params?: Record<string, unknown>, baseUrl = apiBaseUrl, allowRefresh = true, acceptedCodes = [0]): Promise<T> {
  const token = localStorage.getItem('_token')
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  headers.set('AuthServer', 'MaxKey')
  headers.set('hostname', window.location.hostname)
  if (token) {
    try {
      const tokenData = JSON.parse(token)
      if (tokenData.token) headers.set('Authorization', `Bearer ${tokenData.token}`)
    } catch {
      localStorage.removeItem('_token')
    }
  }

  const response = await fetch(buildUrl(path, params, baseUrl), {
    credentials: 'include',
    ...options,
    headers,
  })

  if (response.status === 401 && allowRefresh && await refreshToken()) {
    return request<T>(path, options, params, baseUrl, false, acceptedCodes)
  }
  if (response.status === 401) {
    localStorage.removeItem('_token')
    window.dispatchEvent(new CustomEvent('maxkey:unauthorized'))
  }
  const body = (await response.json().catch(() => ({}))) as ApiResponse<T>
  if (!response.ok) {
    throw new ApiError(body.message || body.msg || `请求失败（${response.status}）`, body.code, body.data)
  }
  if (typeof body.code === 'number' && !acceptedCodes.includes(body.code)) {
    throw new ApiError(body.message || body.msg || '服务端返回错误', body.code, body.data)
  }
  return body.data as T
}

export function get<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>(path, {}, params)
}

export function adminGet<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>(path, {}, params, adminApiBaseUrl)
}

// The legacy access-list controller incorrectly marks successful page data as FAIL (code 2).
export function adminGetLegacyPage<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>(path, {}, params, adminApiBaseUrl, true, [0, 2])
}

export function post<T>(path: string, data: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export function postForm<T>(path: string, data: Record<string, unknown>): Promise<T> {
  const body = new URLSearchParams()
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined && value !== null) body.set(key, String(value))
  })
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
    body,
  })
}

export function put<T>(path: string, data: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  })
}

export function del<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>(path, { method: 'DELETE' }, params)
}

export function adminPost<T>(path: string, data: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  }, undefined, adminApiBaseUrl)
}

export function adminPostFormData<T>(path: string, data: FormData): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    body: data,
  }, undefined, adminApiBaseUrl)
}

export function adminPut<T>(path: string, data: unknown): Promise<T> {
  return request<T>(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  }, undefined, adminApiBaseUrl)
}

export function adminDelete<T>(path: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>(path, { method: 'DELETE' }, params, adminApiBaseUrl)
}

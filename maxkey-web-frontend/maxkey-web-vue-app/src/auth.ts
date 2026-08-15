import { computed, reactive } from 'vue'
import { get, post } from './api'

export interface MaxKeyToken {
  token?: string
  ticket?: string
  refresh_token?: string
  expired?: number
  twoFactor?: string | number
  remeberMe?: string
  id?: string
  username?: string
  displayName?: string
  email?: string
  avatar?: string
  passwordSetType?: number
  authorities?: string[]
  [key: string]: unknown
}

const TOKEN_KEY = '_token'
const savedToken = localStorage.getItem(TOKEN_KEY)

function readToken(): MaxKeyToken | null {
  if (!savedToken) return null
  try {
    return JSON.parse(savedToken) as MaxKeyToken
  } catch {
    localStorage.removeItem(TOKEN_KEY)
    return null
  }
}

const state = reactive<{ token: MaxKeyToken | null }>({ token: readToken() })

function cookieDomain() {
  const hostname = window.location.hostname
  if (hostname === 'localhost' || hostname.includes(':') || /^\d{1,3}(\.\d{1,3}){3}$/.test(hostname)) return ''
  const parts = hostname.split('.')
  return parts.length >= 2 ? parts.slice(-2).join('.') : ''
}

function setCookie(name: string, value: string, domain = '') {
  const domainPart = domain ? `; domain=${domain}` : ''
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; SameSite=Lax${domainPart}`
}

function removeCookie(name: string, domain = '') {
  const domainPart = domain ? `; domain=${domain}` : ''
  document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax${domainPart}`
}

function persistToken(token: MaxKeyToken) {
  state.token = token
  localStorage.setItem(TOKEN_KEY, JSON.stringify(token))
  if (token.token) setCookie('congress', token.token)
  if (token.ticket) setCookie('online_ticket', token.ticket, cookieDomain())
  if (token.remeberMe) localStorage.setItem('remember_me', token.remeberMe)
}

window.addEventListener('maxkey:token-refreshed', event => {
  const token = (event as CustomEvent<MaxKeyToken>).detail
  if (token) persistToken({ ...(state.token || {}), ...token })
})

window.addEventListener('maxkey:unauthorized', () => {
  state.token = null
  localStorage.removeItem(TOKEN_KEY)
  removeCookie('congress')
  removeCookie('online_ticket', cookieDomain())
})

function normalizeAuthorities(token: MaxKeyToken | null): string[] {
  return Array.isArray(token?.authorities) ? token.authorities.map(String).map(item => item.toUpperCase()) : []
}

export const auth = {
  token: computed(() => state.token),
  isAuthenticated: computed(() => Boolean(state.token?.token)),
  isAdmin: computed(() => {
    const authorities = normalizeAuthorities(state.token)
    return authorities.some(item => ['ROLE_ADMINISTRATORS', 'ROLE_ADMIN', 'ADMINISTRATORS', 'ADMIN'].includes(item))
  }),
  authorities: computed(() => normalizeAuthorities(state.token)),
  signIn(token: MaxKeyToken) {
    persistToken(token)
  },
  clearToken() {
    state.token = null
    localStorage.removeItem(TOKEN_KEY)
  },
  signOut() {
    state.token = null
    localStorage.removeItem(TOKEN_KEY)
    removeCookie('congress')
    removeCookie('online_ticket', cookieDomain())
  },
  async login(payload: Record<string, unknown>) {
    const token = await post<MaxKeyToken>('/login/signin?_allow_anonymous=true', payload)
    if (String(token?.twoFactor || '0') !== '0') {
      localStorage.setItem('two_factor_data', JSON.stringify({ ...token, remeberMeRequested: Boolean(payload.remeberMe) }))
      const error = new Error('当前登录需要二次认证') as Error & { twoFactor?: boolean }
      error.twoFactor = true
      throw error
    }
    this.signIn(token)
    return token
  },
  async loadLoginConfig(useRemember = true) {
    return get<Record<string, unknown>>('/login/get?_allow_anonymous=true', {
      remember_me: useRemember ? localStorage.getItem('remember_me') || '' : '',
    })
  },
  async logout() {
    try {
      await get('/logout')
    } finally {
      this.signOut()
      localStorage.removeItem('remember_me')
    }
  },
}

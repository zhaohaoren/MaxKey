import { computed, reactive } from 'vue'
import { get, post } from './api'

export interface MaxKeyToken {
  token?: string
  refresh_token?: string
  username?: string
  displayName?: string
  email?: string
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

window.addEventListener('maxkey:token-refreshed', event => {
  const token = (event as CustomEvent<MaxKeyToken>).detail
  if (token) state.token = { ...(state.token || {}), ...token }
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
    state.token = token
    localStorage.setItem(TOKEN_KEY, JSON.stringify(token))
  },
  signOut() {
    state.token = null
    localStorage.removeItem(TOKEN_KEY)
  },
  async login(payload: Record<string, unknown>) {
    const token = await post<MaxKeyToken>('/login/signin?_allow_anonymous=true', payload)
    if (token?.twoFactor && token.twoFactor !== '0') {
      localStorage.setItem('two_factor_data', JSON.stringify(token))
      const error = new Error('当前登录需要二次认证') as Error & { twoFactor?: boolean }
      error.twoFactor = true
      throw error
    }
    this.signIn(token)
    return token
  },
  async loadLoginConfig() {
    return get<Record<string, unknown>>('/login/get?_allow_anonymous=true', {
      remember_me: localStorage.getItem('remember_me') || '',
    })
  },
  async logout() {
    try {
      await get('/logout')
    } finally {
      this.signOut()
    }
  },
}

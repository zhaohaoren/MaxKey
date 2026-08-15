import type { Router } from 'vue-router'

const EXTERNAL_REDIRECT_KEY = 'redirect_uri'
const INTERNAL_REDIRECT_KEY = 'maxkey_login_redirect'

function decodeBase64Url(value: string) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  const bytes = Uint8Array.from(window.atob(padded), character => character.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

export function rememberLoginRedirect(encodedExternal?: string, internal?: string) {
  if (encodedExternal) {
    try {
      const decoded = decodeBase64Url(encodedExternal)
      if (decoded) localStorage.setItem(EXTERNAL_REDIRECT_KEY, decoded)
    } catch {
      localStorage.removeItem(EXTERNAL_REDIRECT_KEY)
    }
  }
  if (internal?.startsWith('/')) localStorage.setItem(INTERNAL_REDIRECT_KEY, internal)
}

export async function navigateAfterLogin(router: Router, defaultPath = '/dashboard/home') {
  const external = localStorage.getItem(EXTERNAL_REDIRECT_KEY) || ''
  const internal = localStorage.getItem(INTERNAL_REDIRECT_KEY) || ''
  localStorage.removeItem(EXTERNAL_REDIRECT_KEY)
  localStorage.removeItem(INTERNAL_REDIRECT_KEY)
  if (external) {
    window.location.assign(external)
    return
  }
  await router.replace(internal || defaultPath)
}

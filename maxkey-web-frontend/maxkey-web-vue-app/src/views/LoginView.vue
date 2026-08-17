<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, type MaxKeyToken } from '../auth'
import { ApiError, get, post } from '../api'
import { navigateAfterLogin, rememberLoginRedirect } from '../authRedirect'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const passkeyLoading = ref(false)
const passkeyEnabled = ref(false)
const mobileLoginEnabled = ref(false)
const mobileLoginVisible = false
const error = ref('')
const captchaImage = ref('')
const captchaType = ref('NONE')
const state = ref('')
const loginType = ref<'normal' | 'mobile' | 'qrscan'>('normal')
const passwordVisible = ref(false)
const form = ref({ username: '', password: '', captcha: '', mobile: '', otpCaptcha: '', remember: false })
const enterpriseEmailSuffix = '@snowx.com'
const showEnterpriseEmailSuffix = computed(() => {
  const username = form.value.username.trim()
  return !/^admin$/i.test(username) && !username.includes('@')
})
const otpCountdown = ref(0)
interface SocialProvider {
  provider: string
  providerName: string
  icon?: string
  clientId?: string
  agentId?: string
  redirectUri?: string
  state?: string
}

interface PasskeyAuthenticationOptions {
  challenge: string
  challengeId: string
  timeout?: number
  rpId?: string
  userVerification?: UserVerificationRequirement
}

interface FeishuQrLogin {
  matchOrigin(origin: string): boolean
  matchData(data: unknown): boolean
}

interface SocialSdkWindow extends Window {
  WwLogin?: new (options: Record<string, unknown>) => unknown
  DDLogin?: (options: Record<string, unknown>) => unknown
  QRLogin?: (options: Record<string, unknown>) => FeishuQrLogin
}

const socials = ref<SocialProvider[]>([])
const feishuSocial = computed(() => socials.value.find(item => item.provider.toLowerCase() === 'feishu'))
const socialQrProvider = ref('')
const socialLoadingProvider = ref('')
const socialQrLoading = ref(false)
const socialQrError = ref('')
const socialQrContainer = ref<HTMLElement>()
const qrImage = ref('')
const qrTicket = ref('')
const qrExpired = ref(false)
let qrTimer: number | undefined
let otpTimer: number | undefined
let qrPolling = false
let feishuQrLogin: FeishuQrLogin | undefined
let feishuRedirectUri = ''
let dingTalkRedirectUri = ''
const scriptPromises = new Map<string, Promise<void>>()

async function loadConfig() {
  try {
    const data = await auth.loadLoginConfig()
    if (data.token) {
      auth.signIn(data as MaxKeyToken)
      await navigateAfterLogin(router)
      return
    }
    captchaType.value = String(data.captcha || 'NONE')
    state.value = String(data.state || '')
    captchaImage.value = ''
    const socialConfig = data.socials as { providers?: SocialProvider[]; qrScan?: string } | SocialProvider[] | undefined
    socials.value = Array.isArray(socialConfig) ? socialConfig : Array.isArray(socialConfig?.providers) ? socialConfig.providers : []
    socialQrProvider.value = Array.isArray(socialConfig) ? '' : String(socialConfig?.qrScan || '')
    passkeyEnabled.value = canUsePasskey(data.passkeyEnabled, data.passkeyAllowedOrigins)
    mobileLoginEnabled.value = Boolean(data.otpType)
    if (!mobileLoginEnabled.value && loginType.value === 'mobile') loginType.value = 'normal'
    if (captchaType.value.toUpperCase() !== 'NONE') await loadCaptcha()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '无法加载登录配置'
  }
}

async function initializeLogin() {
  const encodedRedirect = typeof route.query.redirect_uri === 'string' ? route.query.redirect_uri : ''
  const internalRedirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  rememberLoginRedirect(encodedRedirect, internalRedirect)

  // The legacy login entry clears the current access token before starting a new flow.
  auth.clearToken()
  const congress = typeof route.query.congress === 'string' ? route.query.congress : ''
  if (congress) {
    loading.value = true
    try {
      const token = await post<MaxKeyToken>('/login/congress?_allow_anonymous=true', { congress })
      auth.signIn(token)
      await navigateAfterLogin(router)
      return
    } catch (err) {
      error.value = err instanceof Error ? err.message : '票据登录失败'
    } finally {
      loading.value = false
    }
  }
  await loadConfig()
}

function canUsePasskey(enabled: unknown, configuredOrigins: unknown) {
  if (enabled !== true || !window.PublicKeyCredential) return false
  if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') return true
  if (window.location.protocol !== 'https:') return false

  const allowedOrigins = Array.isArray(configuredOrigins) ? configuredOrigins.map(String) : []
  return allowedOrigins.some(origin => {
    try {
      return new URL(origin).origin === window.location.origin
    } catch {
      return window.location.href.includes(origin)
    }
  })
}

function base64UrlToArrayBuffer(value: string) {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
  const binary = window.atob(padded)
  return Uint8Array.from(binary, character => character.charCodeAt(0)).buffer
}

function arrayBufferToBase64Url(value: ArrayBuffer) {
  const bytes = new Uint8Array(value)
  let binary = ''
  bytes.forEach(byte => { binary += String.fromCharCode(byte) })
  return window.btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function passkeyErrorMessage(err: unknown) {
  const message = err instanceof Error ? err.message : String(err || '')
  if (/PASSKEY_NOT_REGISTERED|没有.*Passkey|No Passkeys registered|No credentials available|credential not found|用户未注册/i.test(message)) {
    return '还未注册 Passkey，请先注册 Passkey'
  }
  if (err instanceof DOMException) {
    if (err.name === 'NotAllowedError') return 'Passkey 登录已取消或已超时'
    if (err.name === 'SecurityError') return '安全错误：请确认当前域名已配置并使用 HTTPS'
    if (err.name === 'NotSupportedError') return '您的设备不支持 Passkey 功能'
    if (err.name === 'InvalidStateError') return '认证器状态异常，请重试'
    if (err.name === 'ConstraintError') return '认证器约束错误，请重试'
  }
  return message ? `Passkey 登录失败：${message}` : 'Passkey 登录失败，请重试或使用其他登录方式'
}

async function passkeyLogin() {
  error.value = ''
  if (!window.PublicKeyCredential) {
    error.value = '您的浏览器不支持 WebAuthn/Passkey 功能'
    return
  }

  passkeyLoading.value = true
  try {
    const options = await post<PasskeyAuthenticationOptions>('/passkey/authentication/begin?_allow_anonymous=true', {})
    if (!options?.challenge || !options.challengeId) throw new Error('服务器返回的认证选项无效')

    const credential = await navigator.credentials.get({
      publicKey: {
        challenge: base64UrlToArrayBuffer(options.challenge),
        timeout: options.timeout || 60000,
        rpId: options.rpId,
        userVerification: options.userVerification || 'preferred',
      },
    }) as PublicKeyCredential | null
    if (!credential) throw new Error('浏览器未返回认证凭据')

    const response = credential.response as AuthenticatorAssertionResponse
    const token = await post<MaxKeyToken>('/passkey/authentication/finish?_allow_anonymous=true', {
      challengeId: options.challengeId,
      credentialId: credential.id,
      authenticatorData: arrayBufferToBase64Url(response.authenticatorData),
      clientDataJSON: arrayBufferToBase64Url(response.clientDataJSON),
      signature: arrayBufferToBase64Url(response.signature),
      userHandle: response.userHandle ? arrayBufferToBase64Url(response.userHandle) : null,
    })
    if (!token?.token) throw new Error('认证成功但服务端未返回登录令牌')
    auth.signIn(token)
    await navigateAfterLogin(router)
  } catch (err) {
    error.value = passkeyErrorMessage(err)
  } finally {
    passkeyLoading.value = false
  }
}

async function loadCaptcha() {
  try {
    const data = await get<{ image?: string; state?: string }>('/captcha?_allow_anonymous=true', {
      state: state.value,
      captcha: captchaType.value,
    })
    captchaImage.value = String(data.image || '')
    if (data.state) state.value = data.state
  } catch (err) {
    captchaImage.value = ''
    error.value = err instanceof Error ? err.message : '验证码加载失败'
  }
}

async function submit() {
  error.value = ''
  if (loginType.value === 'normal' && (!form.value.username || !form.value.password)) {
    error.value = '请输入飞书企业邮箱和密码'
    return
  }
  if (loginType.value === 'mobile' && (!/^1\d{10}$/.test(form.value.mobile) || !form.value.otpCaptcha)) {
    error.value = '请输入正确的手机号和短信验证码'
    return
  }

  const usernameInput = form.value.username.trim()
  let loginUsername = usernameInput
  if (loginType.value === 'normal') {
    if (/^admin$/i.test(usernameInput)) {
      loginUsername = 'admin'
    } else if (/^[^@\s]+$/i.test(usernameInput)) {
      loginUsername = `${usernameInput}${enterpriseEmailSuffix}`
    } else if (!/^[^@\s]+@snowx\.com$/i.test(usernameInput)) {
      error.value = '请使用 @snowx.com 飞书企业邮箱登录'
      return
    }
  }

  loading.value = true
  try {
    if (!form.value.remember) localStorage.removeItem('remember_me')
    await auth.login({
      authType: loginType.value,
      state: state.value,
      username: loginUsername,
      password: form.value.password,
      captcha: form.value.captcha,
      mobile: form.value.mobile,
      otpCaptcha: form.value.otpCaptcha,
      remeberMe: form.value.remember,
    })
    await navigateAfterLogin(router)
  } catch (err) {
    if (err && typeof err === 'object' && 'twoFactor' in err) {
      await router.replace('/passport/tfa')
      return
    }
    error.value = err instanceof Error ? err.message : '登录失败'
    if (loginType.value === 'normal' && captchaType.value.toUpperCase() !== 'NONE') await loadCaptcha()
  } finally {
    loading.value = false
  }
}

async function sendMobileCode() {
  error.value = ''
  if (!/^1\d{10}$/.test(form.value.mobile)) {
    error.value = '请输入正确的手机号'
    return
  }
  try {
    await get(`/login/sendotp/${encodeURIComponent(form.value.mobile)}?_allow_anonymous=true`)
    otpCountdown.value = 59
    if (otpTimer) window.clearInterval(otpTimer)
    otpTimer = window.setInterval(() => {
      otpCountdown.value -= 1
      if (otpCountdown.value <= 0 && otpTimer) window.clearInterval(otpTimer)
    }, 1000)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '短信验证码发送失败'
  }
}

function socialIcon(icon?: string) {
  if (!icon) return ''
  if (/^(data:|https?:|\/)/i.test(icon)) return icon
  return `${import.meta.env.BASE_URL}${icon.replace(/^\.\//, '')}`
}

async function socialLogin(provider: string) {
  socialLoadingProvider.value = provider
  error.value = ''
  try {
    const authorizationUrl = await get<string>(`/logon/oauth20/authorize/${encodeURIComponent(provider)}?_allow_anonymous=true`)
    if (!authorizationUrl) throw new Error('服务端未返回第三方授权地址')
    window.location.assign(authorizationUrl)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '第三方登录初始化失败'
    socialLoadingProvider.value = ''
  }
}

function loadScript(src: string) {
  const existing = scriptPromises.get(src)
  if (existing) return existing
  const promise = new Promise<void>((resolve, reject) => {
    const loaded = Array.from(document.scripts).find(item => item.src === src)
    if (loaded) { resolve(); return }
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('第三方二维码组件加载失败'))
    document.head.appendChild(script)
  })
  scriptPromises.set(src, promise)
  return promise
}

async function renderSocialQrCode(provider: string, data: SocialProvider) {
  await nextTick()
  const container = socialQrContainer.value
  if (!container) return
  container.innerHTML = ''
  const sdkWindow = window as unknown as SocialSdkWindow

  if (provider === 'workweixin') {
    await loadScript('https://wwcdn.weixin.qq.com/node/wework/wwopen/js/wwLogin-1.2.7.js')
    if (!sdkWindow.WwLogin) throw new Error('企业微信二维码组件不可用')
    new sdkWindow.WwLogin({
      id: 'social-qrcode', appid: data.clientId, agentid: data.agentId,
      redirect_uri: encodeURIComponent(String(data.redirectUri || '')), state: data.state,
      href: 'data:text/css;base64,LmltcG93ZXJCb3ggLnFyY29kZSB7d2lkdGg6IDI1MHB4O30=',
    })
    return
  }

  if (provider === 'dingtalk') {
    await loadScript('https://g.alicdn.com/dingding/dinglogin/0.0.5/ddLogin.js')
    if (!sdkWindow.DDLogin) throw new Error('钉钉二维码组件不可用')
    const authorizationUrl = new URL('https://oapi.dingtalk.com/connect/oauth2/sns_authorize')
    authorizationUrl.searchParams.set('appid', String(data.clientId || ''))
    authorizationUrl.searchParams.set('response_type', 'code')
    authorizationUrl.searchParams.set('scope', 'snsapi_login')
    authorizationUrl.searchParams.set('state', String(data.state || ''))
    authorizationUrl.searchParams.set('redirect_uri', String(data.redirectUri || ''))
    dingTalkRedirectUri = authorizationUrl.toString()
    sdkWindow.DDLogin({ id: 'social-qrcode', goto: encodeURIComponent(dingTalkRedirectUri), style: 'border:none;background-color:#fff;', width: '360', height: '400' })
    return
  }

  if (provider === 'feishu') {
    await loadScript('https://lf-package-cn.feishucdn.com/obj/feishu-static/lark/passport/qrcode/LarkSSOSDKWebQRCode-1.0.3.js')
    if (!sdkWindow.QRLogin) throw new Error('飞书二维码组件不可用')
    // Feishu's QR SDK only supports the legacy authorization entry; the code
    // it returns can still be exchanged through the current authen/v1 APIs.
    const authorizationUrl = new URL('https://passport.feishu.cn/suite/passport/oauth/authorize')
    authorizationUrl.searchParams.set('client_id', String(data.clientId || ''))
    authorizationUrl.searchParams.set('redirect_uri', String(data.redirectUri || ''))
    authorizationUrl.searchParams.set('response_type', 'code')
    authorizationUrl.searchParams.set('state', String(data.state || ''))
    feishuRedirectUri = authorizationUrl.toString()
    feishuQrLogin = sdkWindow.QRLogin({ id: 'social-qrcode', goto: feishuRedirectUri, width: '300', height: '300', style: 'border:0;' })
  }
}

function handleSocialQrMessage(event: MessageEvent) {
  if (dingTalkRedirectUri && typeof event.data === 'string' && event.origin.includes('//login.dingtalk.com')) {
    const target = new URL(dingTalkRedirectUri)
    target.searchParams.set('loginTmpCode', event.data)
    window.location.assign(target.toString())
    return
  }
  const iframe = socialQrContainer.value?.querySelector('iframe')
  if (feishuQrLogin && iframe && event.source === iframe.contentWindow && feishuQrLogin.matchOrigin(event.origin) && feishuQrLogin.matchData(event.data)) {
    const target = new URL(feishuRedirectUri)
    const data = event.data as { tmp_code?: string }
    if (data.tmp_code) target.searchParams.set('tmp_code', data.tmp_code)
    window.location.assign(target.toString())
  }
}

async function loadLoginQrCode() {
  socialQrError.value = ''
  qrImage.value = ''
  if (socialQrProvider.value && ['workweixin', 'dingtalk', 'feishu'].includes(socialQrProvider.value.toLowerCase())) {
    socialQrLoading.value = true
    try {
      const provider = socialQrProvider.value.toLowerCase()
      const data = await get<SocialProvider>(`/logon/oauth20/scanqrcode/${encodeURIComponent(provider)}?_allow_anonymous=true`)
      await renderSocialQrCode(provider, data)
    } catch (err) {
      socialQrError.value = err instanceof Error ? err.message : '第三方二维码加载失败'
    } finally {
      socialQrLoading.value = false
    }
    return
  }
  await loadQrCode()
}

async function loadQrCode() {
  try {
    qrExpired.value = false
    const data = await get<{ rqCode?: string; ticket?: string }>('/login/genScanCode?_allow_anonymous=true')
    qrImage.value = String(data.rqCode || '')
    qrTicket.value = String(data.ticket || '')
    if (qrTimer) window.clearInterval(qrTimer)
    qrTimer = window.setInterval(pollQrLogin, 5000)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '二维码加载失败'
  }
}

async function pollQrLogin() {
  if (!qrTicket.value || qrExpired.value || qrPolling) return
  qrPolling = true
  try {
    const token = await post<MaxKeyToken>('/login/sign/qrcode?_allow_anonymous=true', { authType: 'scancode', code: qrTicket.value, state: state.value })
    auth.signIn(token)
    qrExpired.value = true
    if (qrTimer) window.clearInterval(qrTimer)
    await navigateAfterLogin(router)
  } catch (err) {
    if (err instanceof ApiError && err.code === 20004) {
      qrExpired.value = true
      if (qrTimer) window.clearInterval(qrTimer)
    } else if (err instanceof ApiError && err.code === 20005) {
      await loadConfig()
    }
    // code 2 means the QR code has not been confirmed yet; keep polling.
  } finally {
    qrPolling = false
  }
}

watch(loginType, value => {
  if (value === 'qrscan') loadLoginQrCode()
})

onMounted(() => { window.addEventListener('message', handleSocialQrMessage); initializeLogin() })
onBeforeUnmount(() => {
  window.removeEventListener('message', handleSocialQrMessage)
  if (qrTimer) window.clearInterval(qrTimer)
  if (otpTimer) window.clearInterval(otpTimer)
})
</script>

<template>
  <main class="snowx-login-page">
    <section class="snowx-login-brand">
      <div class="snowx-login-brand__header">
        <img class="snowx-login-logo" src="/assets/brand/snowx-logo-white.png" alt="SnowX" />
        <h1>统一身份认证平台</h1>
      </div>
      <div class="snowx-word-visual" aria-hidden="true">
        <div class="snowx-brand-welcome">
          <span>WELCOME</span>
        </div>
        <div class="snowx-orbit-backdrop">
          <span class="snowx-orbit-aura" />
          <span class="snowx-orbit-sweep" />
          <span class="snowx-orbit-ring snowx-orbit-ring--outer" />
          <span class="snowx-orbit-ring snowx-orbit-ring--middle" />
          <span class="snowx-orbit-ring snowx-orbit-ring--inner" />
          <span class="snowx-orbit-core" />
          <span class="snowx-orbit-node snowx-orbit-node--top" />
          <span class="snowx-orbit-node snowx-orbit-node--right" />
          <span class="snowx-orbit-node snowx-orbit-node--bottom" />
          <span class="snowx-orbit-node snowx-orbit-node--left" />
        </div>
        <div class="snowx-print-stage">
          <span class="snowx-printer-rail"><i class="snowx-printer-head"><b class="snowx-laser-focus" /></i></span>
          <span class="snowx-print-word snowx-print-word--snowx">SNOWX</span>
          <span class="snowx-print-word snowx-print-word--iam">IAM</span>
          <span class="snowx-print-layer" />
          <span class="snowx-print-bed" />
        </div>
        <div class="snowx-word-line snowx-word-line--top" />
        <div class="snowx-word-line snowx-word-line--bottom" />
        <span class="snowx-sso-trigger">
          <span class="snowx-sso-pulse" />
          <svg viewBox="0 0 32 32" role="presentation">
            <path class="snowx-sso-cursor" d="M8 5.5v20.2l5.4-5.3 3.8 7.6 3.8-1.9-3.6-7.2h7.2z" />
            <path class="snowx-sso-rays" d="M8 1v3M2.5 6.5h3M3.8 2.6 6 4.8M14 2.2l-1.5 2.6" />
          </svg>
        </span>
        <div class="snowx-app-marquee">
          <div class="snowx-app-track">
            <div v-for="copy in 2" :key="copy" class="snowx-app-group">
              <span class="snowx-app-logo"><img src="/assets/social/gitlab.png" alt="" /></span>
              <span class="snowx-app-logo"><img src="/assets/social/feishu.png" alt="" /></span>
              <span class="snowx-app-logo snowx-app-logo--jira">
                <svg viewBox="0 0 40 40" role="presentation"><path d="M20 3 36 19 20 35 4 19zm0 9.2L13.2 19l6.8 6.8 6.8-6.8z" /><path d="m20 12.2 6.8 6.8-6.8 6.8-6.8-6.8z" opacity=".48" /></svg>
              </span>
              <span class="snowx-app-logo snowx-app-logo--iot">
                <svg viewBox="0 0 40 40" role="presentation"><rect x="5" y="7" width="30" height="23" rx="4" /><path d="m10 23 6-6 5 4 8-9" /><path d="M15 35h10M20 30v5" /></svg>
              </span>
              <span class="snowx-app-logo snowx-app-logo--cms">
                <svg viewBox="0 0 40 40" role="presentation"><rect x="5" y="5" width="13" height="13" rx="3" /><rect x="22" y="5" width="13" height="13" rx="3" /><rect x="5" y="22" width="13" height="13" rx="3" /><rect x="22" y="22" width="13" height="13" rx="3" /></svg>
              </span>
            </div>
          </div>
        </div>
      </div>
      <footer>
        <span>Copyright © {{ new Date().getFullYear() }} SnowX</span>
        <span>SnowX Identity and Access Management System</span>
      </footer>
    </section>

    <section class="snowx-login-form-pane">
      <div class="login-panel">
        <header class="snowx-login-welcome">
          <img class="snowx-login-mobile-logo" src="/assets/brand/snowx-logo-black.png" alt="SnowX" />
          <h1>欢迎使用 <span class="snowx-iam-title">SNOWX IAM</span></h1>
        </header>
          <a-radio-group v-model:value="loginType" button-style="solid" size="large" class="login-tabs">
            <a-radio-button value="normal">账号登录</a-radio-button>
            <a-radio-button v-if="mobileLoginVisible && mobileLoginEnabled" value="mobile">手机登录</a-radio-button>
            <a-radio-button value="qrscan">扫码登录</a-radio-button>
          </a-radio-group>
          <a-alert v-if="error" type="error" show-icon :message="error" class="login-alert" />
          <template v-if="loginType !== 'qrscan'">
            <a-form layout="vertical" @submit.prevent="submit">
              <a-form-item v-if="loginType === 'normal'" label="企业邮箱">
                <a-input
                  v-model:value="form.username"
                  size="large"
                  placeholder="飞书企业邮箱"
                  autocomplete="username"
                >
                  <template v-if="showEnterpriseEmailSuffix" #suffix><span class="enterprise-email-suffix">{{ enterpriseEmailSuffix }}</span></template>
                </a-input>
              </a-form-item>
              <a-form-item v-if="loginType === 'normal'" label="密码">
                <a-input-password v-model:value="form.password" size="large" placeholder="请输入密码" autocomplete="current-password" :visibility-toggle="{ visible: passwordVisible, onVisibleChange: (value: boolean) => (passwordVisible = value) }" />
              </a-form-item>
              <a-form-item v-if="loginType === 'normal' && captchaType.toUpperCase() !== 'NONE'" label="验证码">
                <div class="login-field-action-row captcha-composite">
                  <a-input v-model:value="form.captcha" size="large" placeholder="请输入验证码" />
                  <button type="button" class="captcha-control" title="点击刷新验证码" @click="loadCaptcha">
                    <img v-if="captchaImage" class="captcha" :src="captchaImage" alt="验证码" />
                    <span v-else>获取验证码</span>
                  </button>
                </div>
              </a-form-item>
              <a-form-item v-if="loginType === 'mobile'" label="手机号">
                <a-input v-model:value="form.mobile" size="large" placeholder="手机号" autocomplete="tel" />
              </a-form-item>
              <a-form-item v-if="loginType === 'mobile'" label="短信验证码">
                <div class="login-field-action-row">
                  <a-input v-model:value="form.otpCaptcha" size="large" placeholder="短信验证码" />
                  <a-button class="login-field-action" :disabled="otpCountdown > 0" @click="sendMobileCode">{{ otpCountdown > 0 ? `${otpCountdown}s` : '发送验证码' }}</a-button>
                </div>
              </a-form-item>
              <a-form-item>
                <div class="login-options"><a-checkbox v-model:checked="form.remember">记住我</a-checkbox><RouterLink to="/passport/forgot">忘记密码</RouterLink></div>
              </a-form-item>
              <a-form-item><a-button html-type="submit" type="primary" size="large" block :loading="loading" :disabled="passkeyLoading">登录</a-button></a-form-item>
            </a-form>
          </template>
          <template v-else>
            <div class="qr-login-content">
              <div class="qrcode-placeholder">
                <a-spin v-if="socialQrLoading" tip="二维码加载中…" />
                <div v-show="socialQrProvider && !socialQrLoading && !socialQrError" id="social-qrcode" ref="socialQrContainer" class="social-qrcode" />
                <img v-if="!socialQrProvider && qrImage" :src="qrImage" alt="登录二维码" />
                <span v-else-if="!socialQrProvider && !qrImage">二维码加载中…</span>
                <a-typography-text v-if="socialQrError" type="danger">{{ socialQrError }}</a-typography-text>
                <a-typography-text v-if="qrExpired" type="warning">二维码已失效，请重新获取</a-typography-text>
              </div>
              <p class="qr-login-hint">请使用飞书扫描二维码登录</p>
            </div>
            <div class="qrcode-actions">
              <a-button v-if="socialQrProvider && socialQrError" block @click="socialLogin(socialQrProvider)">改用{{ socials.find(item => item.provider === socialQrProvider)?.providerName || socialQrProvider }}授权登录</a-button>
              <a-button v-if="qrExpired" block @click="loadLoginQrCode">重新获取二维码</a-button>
              <a-button block @click="loginType = 'normal'">返回账号登录</a-button>
            </div>
          </template>
          <div v-if="feishuSocial && loginType === 'normal'" class="login-other">
            <button type="button" class="social-login-button" :disabled="socialLoadingProvider === feishuSocial.provider" @click="socialLogin(feishuSocial.provider)">
              <img v-if="feishuSocial.icon" class="social-icon" :src="socialIcon(feishuSocial.icon)" :alt="feishuSocial.providerName" />
              <span v-else class="social-icon-fallback">飞</span>
              <span>使用飞书登录</span>
            </button>
          </div>
      </div>
      <footer class="snowx-login-mobile-footer">Copyright © {{ new Date().getFullYear() }} SnowX</footer>
    </section>
  </main>
</template>

<style scoped>
.snowx-login-page {
  display: grid;
  height: 100vh;
  overflow: hidden;
  grid-template-columns: minmax(420px, 1fr) minmax(520px, 1fr);
  background: #f8faff;
}

.snowx-login-brand {
  position: relative;
  display: flex;
  box-sizing: border-box;
  height: 100vh;
  overflow: hidden;
  flex-direction: column;
  padding: 40px 44px 34px;
  color: #fff;
  background: #3f3ff3;
}

.snowx-login-brand__header { position: relative; z-index: 1; display: flex; align-items: center; gap: 18px; }
.snowx-login-logo { width: 126px; height: auto; }
.snowx-login-brand__header h1 { margin: 0; padding-left: 18px; border-left: 1px solid rgba(255, 255, 255, .38); color: #fff; font-size: 19px; font-weight: 600; line-height: 1; letter-spacing: -.02em; }
.snowx-brand-welcome { position: absolute; z-index: 2; top: calc(50% - 132px); right: 32px; left: 32px; display: flex; align-items: center; justify-content: center; color: rgba(255, 255, 255, .58); pointer-events: none; }
.snowx-brand-welcome::before, .snowx-brand-welcome::after { width: min(11%, 54px); height: 1px; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .35)); content: ""; }
.snowx-brand-welcome::after { background: linear-gradient(90deg, rgba(255, 255, 255, .35), transparent); }
.snowx-brand-welcome span { margin: 0 18px; font-family: "Avenir Next", "Helvetica Neue", sans-serif; font-size: clamp(25px, 3vw, 40px); font-weight: 650; letter-spacing: .2em; line-height: 1; text-indent: .2em; text-shadow: 0 8px 26px rgba(20, 18, 130, .18); }
.snowx-word-visual { position: absolute; z-index: 0; inset: 70px 0 118px; display: flex; overflow: hidden; align-items: center; justify-content: center; pointer-events: none; }
.snowx-orbit-backdrop { position: absolute; top: 50%; left: 50%; width: min(84%, 520px); aspect-ratio: 1; transform: translate(-50%, -50%); }
.snowx-orbit-aura { position: absolute; inset: 5%; border-radius: 50%; background: radial-gradient(circle, rgba(255, 255, 255, .115) 0, rgba(255, 255, 255, .04) 34%, rgba(255, 255, 255, .012) 58%, transparent 72%); animation: snowx-orbit-aura 5.4s ease-in-out infinite; }
.snowx-orbit-sweep { position: absolute; inset: 7%; border-radius: 50%; background: conic-gradient(from 30deg, transparent 0 72%, rgba(255, 255, 255, .015) 82%, rgba(255, 255, 255, .14) 98%, transparent 100%); mask-image: radial-gradient(circle, transparent 0 25%, #000 26% 100%); -webkit-mask-image: radial-gradient(circle, transparent 0 25%, #000 26% 100%); animation: snowx-orbit-spin 9s linear infinite; }
.snowx-orbit-ring { position: absolute; box-sizing: border-box; border-radius: 50%; transform-origin: center; }
.snowx-orbit-ring::before, .snowx-orbit-ring::after { position: absolute; border-radius: 50%; content: ""; }
.snowx-orbit-ring--outer { inset: 1%; border: 1px dashed rgba(255, 255, 255, .16); animation: snowx-orbit-spin 34s linear infinite; }
.snowx-orbit-ring--outer::before { inset: -2px; border: 2px solid transparent; border-top-color: rgba(255, 255, 255, .46); border-right-color: rgba(255, 255, 255, .08); filter: drop-shadow(0 0 6px rgba(255, 255, 255, .32)); }
.snowx-orbit-ring--outer::after { top: 48%; right: -5px; width: 9px; height: 9px; background: rgba(255, 255, 255, .65); box-shadow: 0 0 12px rgba(255, 255, 255, .65); }
.snowx-orbit-ring--middle { inset: 13%; border: 1px solid rgba(255, 255, 255, .12); outline: 1px dotted rgba(255, 255, 255, .1); outline-offset: 9px; animation: snowx-orbit-spin 25s linear infinite reverse; }
.snowx-orbit-ring--middle::before { inset: -2px; border: 2px solid transparent; border-bottom-color: rgba(255, 255, 255, .38); border-left-color: rgba(255, 255, 255, .06); }
.snowx-orbit-ring--middle::after { bottom: 12%; left: 10%; width: 7px; height: 7px; background: rgba(117, 230, 255, .7); box-shadow: 0 0 12px rgba(117, 230, 255, .55); }
.snowx-orbit-ring--inner { inset: 27%; border: 1px dashed rgba(255, 255, 255, .18); animation: snowx-orbit-spin 17s linear infinite; }
.snowx-orbit-ring--inner::before { inset: 8px; border: 1px solid rgba(255, 255, 255, .09); }
.snowx-orbit-core { position: absolute; inset: 36%; border: 1px solid rgba(255, 255, 255, .18); border-radius: 50%; background: radial-gradient(circle, rgba(255, 255, 255, .13), rgba(255, 255, 255, .025) 55%, transparent 58%); box-shadow: 0 0 36px rgba(255, 255, 255, .07), inset 0 0 22px rgba(255, 255, 255, .06); animation: snowx-orbit-core 4.2s ease-in-out infinite; }
.snowx-orbit-node { position: absolute; width: 8px; height: 8px; border: 2px solid rgba(255, 255, 255, .34); border-radius: 50%; background: #7373ff; box-shadow: 0 0 0 5px rgba(255, 255, 255, .045), 0 0 14px rgba(255, 255, 255, .4); animation: snowx-orbit-node 3.4s ease-in-out infinite; }
.snowx-orbit-node--top { top: 8%; left: 49%; }
.snowx-orbit-node--right { top: 48%; right: 8%; animation-delay: -.8s; }
.snowx-orbit-node--bottom { bottom: 8%; left: 49%; animation-delay: -1.6s; }
.snowx-orbit-node--left { top: 48%; left: 8%; animation-delay: -2.4s; }
.snowx-print-stage { position: relative; z-index: 1; width: min(88%, 560px); height: 154px; overflow: hidden; perspective: 600px; mask-image: linear-gradient(90deg, transparent, #000 10%, #000 90%, transparent); -webkit-mask-image: linear-gradient(90deg, transparent, #000 10%, #000 90%, transparent); }
.snowx-printer-rail { position: absolute; z-index: 4; top: 12px; right: 8%; left: 8%; height: 2px; border-radius: 999px; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .25) 8%, rgba(255, 255, 255, .25) 92%, transparent); }
.snowx-printer-head { position: absolute; top: -6px; left: 0; width: 46px; height: 13px; border: 1px solid rgba(225, 235, 246, .48); border-radius: 4px 4px 7px 7px; background: linear-gradient(180deg, #dce5ee 0, #8491a2 34%, #3c4657 68%, #b4c0ce 100%); box-shadow: 0 3px 12px rgba(18, 22, 41, .32), inset 0 1px rgba(255, 255, 255, .55); animation: snowx-printer-head 1.35s ease-in-out infinite alternate; }
.snowx-printer-head::before { position: absolute; top: 12px; left: 17px; width: 10px; height: 10px; background: linear-gradient(180deg, #cdd6e1, #556174); clip-path: polygon(12% 0, 88% 0, 68% 100%, 32% 100%); content: ""; }
.snowx-printer-head::after { position: absolute; top: 20px; left: 21px; width: 2px; height: 104px; background: linear-gradient(rgba(157, 229, 255, .12), rgba(205, 246, 255, .72) 72%, #fff 100%); box-shadow: 0 0 7px rgba(137, 226, 255, .42); content: ""; }
.snowx-laser-focus { position: absolute; z-index: 2; top: 119px; left: 18px; width: 8px; height: 8px; border-radius: 50%; background: #fff; box-shadow: 0 0 6px #fff, 0 0 13px rgba(119, 229, 255, .95), 0 0 22px rgba(255, 174, 76, .78); animation: snowx-laser-focus 1.35s ease-in-out infinite alternate; }
.snowx-laser-focus::before, .snowx-laser-focus::after { position: absolute; width: 2px; height: 11px; border-radius: 999px; background: linear-gradient(#fff5c2, #ff9f43, transparent); content: ""; transform-origin: center bottom; }
.snowx-laser-focus::before { top: -8px; left: -4px; transform: rotate(-42deg); }
.snowx-laser-focus::after { top: -10px; right: -5px; transform: rotate(48deg); }
.snowx-print-word { position: absolute; z-index: 2; bottom: 23px; left: 50%; opacity: 0; color: transparent; background: repeating-linear-gradient(to bottom, rgba(255, 255, 255, .32) 0 2px, rgba(71, 82, 101, .2) 2px 4px), linear-gradient(108deg, #697688 0%, #dbe3ec 14%, #8794a5 28%, #f7fbff 44%, #8c99a9 59%, #dce5ee 76%, #6d798a 100%); background-clip: text; -webkit-background-clip: text; background-blend-mode: soft-light, normal; font-family: "Avenir Next", "Helvetica Neue", sans-serif; font-size: clamp(100px, 9vw, 138px); font-weight: 800; line-height: .88; letter-spacing: -.025em; white-space: nowrap; transform: translateX(-50%) rotateX(-2deg); clip-path: inset(100% 0 0 0); filter: drop-shadow(1px 2px 0 rgba(255, 255, 255, .18)) drop-shadow(4px 5px 0 rgba(49, 59, 78, .2)) drop-shadow(7px 9px 0 rgba(21, 24, 55, .12)) drop-shadow(0 13px 22px rgba(16, 18, 63, .18)); animation: snowx-word-print 12s linear infinite both; }
.snowx-print-word--iam { animation-delay: 6s; }
.snowx-print-layer { position: absolute; z-index: 3; right: 7%; bottom: 23px; left: 7%; height: 2px; background: linear-gradient(90deg, transparent, rgba(141, 217, 239, .28) 16%, rgba(222, 247, 255, .7) 50%, rgba(141, 217, 239, .28) 84%, transparent); box-shadow: 0 0 8px rgba(164, 240, 255, .36); animation: snowx-print-layer 6s linear infinite; }
.snowx-print-layer::after { position: absolute; top: -2px; left: 0; width: 18px; height: 6px; border-radius: 50%; background: radial-gradient(ellipse, #fff 0 18%, #9beaff 32%, #ffb45d 52%, transparent 72%); box-shadow: 0 0 10px rgba(147, 234, 255, .8), 0 0 16px rgba(255, 169, 76, .45); content: ""; animation: snowx-laser-track 1.35s ease-in-out infinite alternate; }
.snowx-print-bed { position: absolute; z-index: 4; right: 6%; bottom: 16px; left: 6%; height: 7px; border-top: 1px solid rgba(230, 238, 246, .48); border-bottom: 1px solid rgba(39, 48, 65, .36); background: linear-gradient(180deg, rgba(222, 230, 238, .38), rgba(89, 102, 120, .24)), repeating-linear-gradient(90deg, rgba(255, 255, 255, .16) 0 3px, transparent 3px 7px); box-shadow: 0 5px 14px rgba(16, 20, 58, .24), inset 0 1px rgba(255, 255, 255, .24); }
.snowx-word-line { position: absolute; left: 50%; width: min(68%, 430px); height: 1px; background: linear-gradient(90deg, transparent, rgba(255, 255, 255, .5), transparent); transform: translateX(-50%); }
.snowx-word-line::after { position: absolute; top: -2px; left: 0; width: 56px; height: 5px; border-radius: 999px; background: rgba(255, 255, 255, .48); box-shadow: 0 0 12px rgba(255, 255, 255, .38); content: ""; animation: snowx-line-scan 4.8s ease-in-out infinite alternate; }
.snowx-word-line--top { top: calc(50% - 92px); }
.snowx-word-line--bottom { top: calc(50% + 91px); }
.snowx-word-line--bottom::after { animation-delay: -2.4s; }
.snowx-sso-trigger { position: absolute; z-index: 3; top: calc(50% + 91px); left: 50%; display: grid; box-sizing: border-box; width: 42px; height: 42px; border: 1px solid rgba(255, 255, 255, .32); border-radius: 50%; background: rgba(91, 91, 250, .78); box-shadow: 0 0 0 6px rgba(255, 255, 255, .05), 0 8px 24px rgba(23, 22, 132, .22), inset 0 1px rgba(255, 255, 255, .2); transform: translate(-50%, -50%); backdrop-filter: blur(8px); place-items: center; animation: snowx-sso-click 5.2s ease-in-out infinite; }
.snowx-sso-trigger::after { position: absolute; top: 100%; left: 50%; width: 1px; height: 21px; background: linear-gradient(rgba(255, 255, 255, .5), transparent); content: ""; transform: translateX(-50%); }
.snowx-sso-trigger svg { position: relative; z-index: 2; width: 23px; height: 23px; overflow: visible; }
.snowx-sso-cursor { fill: rgba(255, 255, 255, .84); stroke: rgba(63, 63, 243, .55); stroke-width: 1; stroke-linejoin: round; }
.snowx-sso-rays { fill: none; stroke: rgba(255, 255, 255, .82); stroke-width: 2; stroke-linecap: round; animation: snowx-sso-rays 5.2s ease-in-out infinite; }
.snowx-sso-pulse, .snowx-sso-pulse::before { position: absolute; inset: -1px; border: 1px solid rgba(255, 255, 255, .42); border-radius: 50%; content: ""; animation: snowx-sso-wave 5.2s ease-out infinite; }
.snowx-sso-pulse::before { animation-delay: .55s; }
.snowx-app-marquee { position: absolute; top: calc(50% + 124px); left: 50%; width: min(82%, 500px); overflow: hidden; padding: 5px 0; transform: translateX(-50%); mask-image: linear-gradient(90deg, transparent, #000 8%, #000 92%, transparent); -webkit-mask-image: linear-gradient(90deg, transparent, #000 8%, #000 92%, transparent); }
.snowx-app-track { display: flex; width: max-content; align-items: center; will-change: transform; animation: snowx-app-scroll 18s linear infinite reverse; }
.snowx-app-group { display: flex; flex-shrink: 0; align-items: center; gap: 18px; padding-right: 18px; }
.snowx-app-logo { display: grid; box-sizing: border-box; width: 54px; height: 54px; flex: 0 0 54px; border: 1px solid rgba(255, 255, 255, .17); border-radius: 15px; background: rgba(255, 255, 255, .1); box-shadow: 0 8px 24px rgba(25, 24, 127, .13), inset 0 1px rgba(255, 255, 255, .12); backdrop-filter: blur(8px); place-items: center; animation: snowx-app-authorized 5.2s ease-in-out infinite; }
.snowx-app-logo:nth-child(2) { animation-delay: .18s; }
.snowx-app-logo:nth-child(3) { animation-delay: .36s; }
.snowx-app-logo:nth-child(4) { animation-delay: .54s; }
.snowx-app-logo:nth-child(5) { animation-delay: .72s; }
.snowx-app-logo img { display: block; width: 32px; height: 32px; object-fit: contain; opacity: .88; }
.snowx-app-logo svg { width: 31px; height: 31px; overflow: visible; }
.snowx-app-logo--jira svg { fill: #8dbdff; filter: drop-shadow(0 3px 5px rgba(16, 92, 190, .22)); }
.snowx-app-logo--iot svg { fill: none; stroke: #63e6df; stroke-width: 2.2; stroke-linecap: round; stroke-linejoin: round; filter: drop-shadow(0 3px 5px rgba(32, 200, 193, .18)); }
.snowx-app-logo--cms svg { fill: #ffca72; filter: drop-shadow(0 3px 5px rgba(231, 153, 28, .18)); }

@keyframes snowx-app-scroll { to { transform: translateX(-50%); } }
@keyframes snowx-printer-head { to { left: calc(100% - 46px); } }
@keyframes snowx-laser-track { to { left: calc(100% - 18px); } }
@keyframes snowx-laser-focus {
  from { opacity: .72; transform: scale(.72); }
  to { opacity: 1; transform: scale(1.15); }
}
@keyframes snowx-print-layer {
  0% { opacity: 0; transform: translateY(0); }
  4% { opacity: 1; }
  72% { opacity: .9; transform: translateY(-101px); }
  76%, 100% { opacity: 0; transform: translateY(-101px); }
}
@keyframes snowx-word-print {
  0% { opacity: .12; clip-path: inset(100% 0 0 0); }
  4% { opacity: .62; }
  35% { opacity: .72; clip-path: inset(0 0 0 0); }
  44% { opacity: .72; clip-path: inset(0 0 0 0); }
  49%, 100% { opacity: 0; clip-path: inset(0 0 0 0); }
}
@keyframes snowx-orbit-spin { to { transform: rotate(360deg); } }
@keyframes snowx-orbit-aura {
  0%, 100% { opacity: .7; transform: scale(.94); }
  50% { opacity: 1; transform: scale(1.04); }
}
@keyframes snowx-orbit-core {
  0%, 100% { opacity: .55; transform: scale(.88); }
  50% { opacity: 1; transform: scale(1.08); }
}
@keyframes snowx-orbit-node {
  0%, 100% { opacity: .48; transform: scale(.82); }
  50% { opacity: 1; transform: scale(1.18); }
}
@keyframes snowx-sso-click {
  0%, 9%, 100% { transform: translate(-50%, -50%) scale(1); }
  3% { transform: translate(-50%, -50%) scale(.88); }
  6% { transform: translate(-50%, -50%) scale(1.08); }
}
@keyframes snowx-sso-rays {
  0%, 10%, 100% { opacity: .18; }
  3%, 7% { opacity: 1; }
}
@keyframes snowx-sso-wave {
  0%, 2% { opacity: 0; transform: scale(.75); }
  4% { opacity: .85; }
  18%, 100% { opacity: 0; transform: scale(2.2); }
}
@keyframes snowx-app-authorized {
  0%, 11%, 100% { border-color: rgba(255, 255, 255, .17); background: rgba(255, 255, 255, .1); box-shadow: 0 8px 24px rgba(25, 24, 127, .13), inset 0 1px rgba(255, 255, 255, .12); transform: translateY(0); }
  15%, 23% { border-color: rgba(255, 255, 255, .46); background: rgba(255, 255, 255, .19); box-shadow: 0 9px 28px rgba(25, 24, 127, .18), 0 0 18px rgba(255, 255, 255, .18), inset 0 1px rgba(255, 255, 255, .24); transform: translateY(-3px); }
  30% { transform: translateY(0); }
}
@keyframes snowx-line-scan { to { left: calc(100% - 56px); } }

@media (prefers-reduced-motion: reduce) {
  .snowx-print-word, .snowx-printer-head, .snowx-laser-focus, .snowx-print-layer, .snowx-print-layer::after, .snowx-app-track, .snowx-orbit-aura, .snowx-orbit-sweep, .snowx-orbit-ring, .snowx-orbit-core, .snowx-orbit-node, .snowx-sso-trigger, .snowx-sso-rays, .snowx-sso-pulse, .snowx-sso-pulse::before, .snowx-app-logo, .snowx-word-line::after { animation: none; }
  .snowx-print-word { opacity: .68; clip-path: none; }
  .snowx-print-word--iam { display: none; }
  .snowx-print-layer { display: none; }
  .snowx-iam-title { animation: none !important; }
}
.snowx-login-brand footer { position: relative; z-index: 1; display: flex; align-items: center; justify-content: space-between; margin-top: auto; color: rgba(255, 255, 255, .5); font-size: 12px; }

.snowx-login-form-pane { display: flex; box-sizing: border-box; height: 100vh; overflow-y: auto; align-items: center; justify-content: center; padding: 24px 72px; background: #fff; }
.login-panel { width: min(100%, 430px); margin: 0; }
.snowx-login-welcome { margin-bottom: 20px; text-align: center; }
.snowx-login-welcome h1 { margin: 0 0 8px; color: #17243a; font-size: 30px; font-weight: 600; letter-spacing: -.03em; }
.snowx-iam-title { position: relative; display: inline-block; color: transparent; background: linear-gradient(100deg, #3f3ff3 0%, #1677ff 34%, #00a6d6 52%, #3f3ff3 100%); background-size: 220% 100%; background-clip: text; -webkit-background-clip: text; font-weight: 700; letter-spacing: .02em; white-space: nowrap; animation: snowx-title-flow 4s ease-in-out infinite; filter: drop-shadow(0 4px 10px rgba(63, 63, 243, .16)); }
@keyframes snowx-title-flow {
  from { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  to { background-position: 0% 50%; }
}
.snowx-login-welcome p { margin: 0; color: #7a879a; font-size: 14px; }
.snowx-login-mobile-logo { display: none; width: 112px; height: auto; margin: 0 auto 36px; }

.login-tabs { display: flex; width: 100%; margin-bottom: 18px; padding: 4px; border-radius: 10px; background: #f0f3f8; }
.login-tabs :deep(.ant-radio-button-wrapper) { height: 38px; flex: 1; border: 0; border-radius: 8px; color: #758197; background: transparent; line-height: 38px; text-align: center; box-shadow: none; }
.login-tabs :deep(.ant-radio-button-wrapper::before) { display: none; }
.login-tabs :deep(.ant-radio-button-wrapper-checked) { color: #17243a; background: #fff; box-shadow: 0 3px 10px rgba(23, 45, 82, .09); }

.login-panel :deep(.ant-form-item) { margin-bottom: 15px; }
.login-panel :deep(.ant-form-item-label) { padding-bottom: 6px; }
.login-panel :deep(.ant-form-item-label > label) { height: auto; color: #33435b; font-size: 13px; font-weight: 600; }
.login-panel :deep(input.ant-input) { height: 44px; border-color: #dce3ee; border-radius: 9px; }
.login-panel :deep(.ant-input-affix-wrapper) { height: 44px; padding-top: 0; padding-bottom: 0; border-color: #dce3ee; border-radius: 9px; }
.login-panel :deep(.ant-input-affix-wrapper > input.ant-input) { height: auto; min-height: 0; border: 0; border-radius: 0; }
.login-panel :deep(.ant-btn-lg) { height: 44px; border-radius: 9px; font-size: 14px; }
.login-panel :deep(.ant-btn-primary) { background: #3f3ff3; }
.enterprise-email-suffix { display: inline-flex; height: 42px; align-items: center; margin-left: 8px; padding-left: 12px; border-left: 1px solid #dce3ee; color: #53647c; font-size: 13px; }
.login-options { color: #68768b; font-size: 13px; }
.login-options a { color: #3f3ff3; }
.login-alert { margin-bottom: 16px; }
.login-field-action-row { display: grid; grid-template-columns: minmax(0, 1fr) 116px; gap: 10px; }
.login-field-action { width: 116px; height: 44px; border-radius: 9px; }
.captcha-control { display: grid; width: 116px; height: 44px; overflow: hidden; padding: 0; border: 1px solid #dce3ee; border-radius: 9px; color: #3f3ff3; background: #f8faff; cursor: pointer; font-size: 12px; place-items: center; }
.captcha-control:hover { border-color: #8f8ff8; }
.captcha-control .captcha { display: block; width: 100%; max-width: none; height: 54px; object-fit: fill; transform: translateY(-6px); }
.captcha-composite { box-sizing: border-box; height: 44px; overflow: hidden; grid-template-columns: minmax(0, 1fr) 116px; gap: 0; border: 1px solid #dce3ee; border-radius: 9px; background: #fff; transition: border-color .2s, box-shadow .2s; }
.captcha-composite:focus-within { border-color: #3f3ff3; box-shadow: 0 0 0 2px rgba(63, 63, 243, .1); }
.captcha-composite :deep(input.ant-input) { height: 100%; border: 0; border-radius: 0; box-shadow: none; }
.captcha-composite .captcha-control { box-sizing: border-box; height: 100%; align-self: stretch; border: 0; border-left: 1px solid #e1e6ee; border-radius: 0; background: #f7f8fb; }
.captcha-composite .captcha-control:hover { background: #f1f3f8; }

.qr-login-content { padding-top: 2px; text-align: center; }
.qrcode-placeholder { min-height: 300px; margin: 0; border: 0; border-radius: 0; background: #fff; }
.social-qrcode { min-width: 300px; min-height: 300px; }
.qr-login-hint { margin: 2px 0 14px; color: #7d899b; font-size: 12px; }
.qrcode-actions { display: grid; gap: 8px; }
.qrcode-actions :deep(.ant-btn) { height: 40px; border-radius: 9px; }
.login-other { display: block; margin-top: 18px; color: #8490a2; }
.social-login-button { display: flex; width: 100%; height: 44px; align-items: center; justify-content: center; gap: 8px; padding: 0 12px; border: 1px solid #dce3ee; border-radius: 9px; color: #42526a; background: #fff; font-size: 13px; font-weight: 600; }
.social-login-button:hover { border-color: #9f9ffa; color: #3f3ff3; background: #f8f8ff; }
.social-icon { width: 22px; height: 22px; }
.social-icon-fallback { width: 22px; height: 22px; font-size: 11px; }
.snowx-login-mobile-footer { display: none; }

@media (max-width: 960px) {
  .snowx-login-page { grid-template-columns: minmax(330px, .8fr) minmax(460px, 1.2fr); }
  .snowx-login-brand { padding: 32px; }
  .snowx-login-brand footer { align-items: flex-start; flex-direction: column; gap: 5px; }
  .snowx-login-form-pane { padding: 24px 40px; }
}

@media (max-width: 720px) {
  .snowx-login-page { display: block; background: #fff; }
  .snowx-login-brand { display: none; }
  .snowx-login-form-pane { min-height: 100vh; flex-direction: column; padding: 28px 24px 20px; }
  .snowx-login-mobile-logo { display: block; }
  .snowx-login-mobile-footer { display: block; margin-top: auto; padding-top: 36px; color: #9aa5b5; font-size: 11px; }
}

@media (max-width: 420px) {
  .snowx-login-form-pane { padding-right: 18px; padding-left: 18px; }
}
</style>

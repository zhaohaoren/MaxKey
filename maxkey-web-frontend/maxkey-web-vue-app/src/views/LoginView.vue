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
  <main class="passport-page">
    <div class="passport-container">
      <header class="passport-header">
        <img class="logo" src="/assets/logo.png" alt="MaxKey" />
        <div class="title"><b>Max</b><strong>Key</strong> 统一认证</div>
      </header>
      <section class="passport-wrap">
        <div class="passport-top"><b>MaxKey 单点登录认证系统</b></div>
        <div class="login-panel">
          <a-radio-group v-model:value="loginType" button-style="solid" size="large" class="login-tabs">
            <a-radio-button value="normal">账号登录</a-radio-button>
            <a-radio-button v-if="mobileLoginVisible && mobileLoginEnabled" value="mobile">手机登录</a-radio-button>
            <a-radio-button value="qrscan">扫码登录</a-radio-button>
          </a-radio-group>
          <a-alert v-if="error" type="error" show-icon :message="error" class="login-alert" />
          <template v-if="loginType !== 'qrscan'">
            <a-form layout="vertical" @submit.prevent="submit">
              <a-form-item v-if="loginType === 'normal'">
                <a-input
                  v-model:value="form.username"
                  size="large"
                  placeholder="飞书企业邮箱"
                  autocomplete="username"
                  :addon-after="showEnterpriseEmailSuffix ? enterpriseEmailSuffix : undefined"
                />
              </a-form-item>
              <a-form-item v-if="loginType === 'normal'">
                <a-input-password v-model:value="form.password" size="large" placeholder="密码" autocomplete="current-password" :visibility-toggle="{ visible: passwordVisible, onVisibleChange: (value: boolean) => (passwordVisible = value) }" />
              </a-form-item>
              <a-form-item v-if="loginType === 'normal' && captchaType.toUpperCase() !== 'NONE'">
                <a-input v-model:value="form.captcha" size="large" placeholder="验证码">
                  <template #addonAfter><img v-if="captchaImage" class="captcha" :src="captchaImage" alt="验证码" @click="loadCaptcha" /><a-button v-else type="link" @click="loadCaptcha">获取验证码</a-button></template>
                </a-input>
              </a-form-item>
              <a-form-item v-if="loginType === 'mobile'">
                <a-input v-model:value="form.mobile" size="large" placeholder="手机号" autocomplete="tel" />
              </a-form-item>
              <a-form-item v-if="loginType === 'mobile'">
                <a-input v-model:value="form.otpCaptcha" size="large" placeholder="短信验证码"><template #addonAfter><a-button type="link" :disabled="otpCountdown > 0" @click="sendMobileCode">{{ otpCountdown > 0 ? `${otpCountdown}s` : '发送验证码' }}</a-button></template></a-input>
              </a-form-item>
              <a-form-item>
                <div class="login-options"><a-checkbox v-model:checked="form.remember">记住我</a-checkbox><RouterLink to="/passport/forgot">忘记密码</RouterLink></div>
              </a-form-item>
              <a-form-item><a-button html-type="submit" type="primary" size="large" block :loading="loading" :disabled="passkeyLoading">登录</a-button></a-form-item>
              <a-form-item v-if="passkeyEnabled"><a-button type="default" size="large" block :loading="passkeyLoading" :disabled="loading" @click="passkeyLogin">使用 Passkey 登录</a-button></a-form-item>
            </a-form>
          </template>
          <template v-else>
            <div class="qrcode-placeholder">
              <a-spin v-if="socialQrLoading" tip="二维码加载中…" />
              <div v-show="socialQrProvider && !socialQrLoading && !socialQrError" id="social-qrcode" ref="socialQrContainer" class="social-qrcode" />
              <img v-if="!socialQrProvider && qrImage" :src="qrImage" alt="登录二维码" />
              <span v-else-if="!socialQrProvider && !qrImage">二维码加载中…</span>
              <a-typography-text v-if="socialQrError" type="danger">{{ socialQrError }}</a-typography-text>
              <a-typography-text v-if="qrExpired" type="warning">二维码已失效，请重新获取</a-typography-text>
            </div>
            <a-button v-if="socialQrProvider && socialQrError" block @click="socialLogin(socialQrProvider)">改用{{ socials.find(item => item.provider === socialQrProvider)?.providerName || socialQrProvider }}授权登录</a-button>
            <a-button v-if="qrExpired" block @click="loadLoginQrCode">重新获取二维码</a-button>
            <a-button block @click="loginType = 'normal'">返回账号登录</a-button>
          </template>
          <div v-if="socials.length && loginType === 'normal'" class="login-other">
            <span>其他方式登录</span>
            <a-tooltip v-for="item in socials" :key="item.provider" :title="item.providerName">
              <button type="button" class="social-login-button" :aria-label="item.providerName" :disabled="socialLoadingProvider === item.provider" @click="socialLogin(item.provider)">
                <img v-if="item.icon" class="social-icon" :src="socialIcon(item.icon)" :alt="item.providerName" />
                <span v-else class="social-icon-fallback">{{ item.providerName.slice(0, 1) }}</span>
              </button>
            </a-tooltip>
          </div>
        </div>
      </section>
      <footer class="passport-footer">MaxKey v4.2.0<br />Copyright {{ new Date().getFullYear() }} <a href="//www.maxkey.top" target="_blank">http://www.maxkey.top</a><br />Licensed under the Apache License, Version 2.0</footer>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiError, adminGet, get, post } from '../api'
import { auth, type MaxKeyToken } from '../auth'
import { navigateAfterLogin } from '../authRedirect'

const route = useRoute()
const router = useRouter()
const mode = computed(() => String(route.meta.callbackMode || 'social'))
const loading = ref(true)
const error = ref('')
const bindRequired = ref(false)
const socialUserId = ref('')
const countdown = ref(0)
const bindForm = reactive({ mobile: '', code: '' })
let timer: number | undefined

function queryParams() {
  const params: Record<string, unknown> = {}
  Object.entries(route.query).forEach(([key, value]) => {
    if (Array.isArray(value)) params[key] = value[0] || ''
    else if (value !== null) params[key] = value
  })
  return params
}

async function finishLogin(token: MaxKeyToken, defaultPath = '/dashboard/home') {
  auth.signIn(token)
  await navigateAfterLogin(router, defaultPath)
}

async function processSocial() {
  const provider = String(route.params.provider || '')
  if (!provider) throw new Error('缺少社会化登录提供方')
  try {
    await finishLogin(await get<MaxKeyToken>(`/logon/oauth20/callback/${encodeURIComponent(provider)}?_allow_anonymous=true`, queryParams()))
  } catch (err) {
    if (err instanceof ApiError && err.code === 102) {
      if (provider.toLowerCase() === 'feishu') {
        throw new Error('飞书账号未完成授权，请使用企业邮箱登录并联系管理员授权')
      }
      socialUserId.value = String(err.data || err.message || '')
      bindRequired.value = true
      return
    }
    throw err
  }
}

async function processTokenLogin() {
  const key = mode.value === 'trust' ? 'ticket' : 'jwt'
  const tokenValue = String(route.query[key] || '')
  if (!tokenValue) throw new Error(`缺少 ${key} 参数`)
  if (mode.value === 'trust') {
    await finishLogin(await adminGet<MaxKeyToken>('/login/trust?_allow_anonymous=true', { ticket: tokenValue }), '/admin')
  } else {
    await finishLogin(await get<MaxKeyToken>('/login/jwt?_allow_anonymous=true', { jwt: tokenValue }))
  }
}

async function processLogout() {
  const redirect = String(route.query.redirect_uri || '')
  try {
    await auth.logout()
  } finally {
    if (redirect.startsWith('http://') || redirect.startsWith('https://')) window.location.replace(redirect)
    else await router.replace(redirect || '/passport/login')
  }
}

async function process() {
  try {
    if (mode.value === 'logout') await processLogout()
    else if (mode.value === 'social') await processSocial()
    else await processTokenLogin()
  } catch (err) {
    if (mode.value === 'jwt' || mode.value === 'trust') {
      await router.replace('/passport/login')
      return
    }
    error.value = err instanceof Error ? err.message : '认证处理失败'
  } finally {
    loading.value = false
  }
}

async function sendCode() {
  if (!bindForm.mobile) { message.warning('请输入手机号'); return }
  try {
    await get(`/login/sendotp/${encodeURIComponent(bindForm.mobile)}?_allow_anonymous=true`)
    message.success('验证码已发送')
    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) window.clearInterval(timer)
    }, 1000)
  } catch (err) {
    message.error(err instanceof Error ? err.message : '验证码发送失败')
  }
}

async function bindUser() {
  if (!bindForm.mobile || !bindForm.code) { error.value = '请输入手机号和验证码'; return }
  loading.value = true
  try {
    const token = await post<MaxKeyToken>('/login/signin/bindusersocials?_allow_anonymous=true', {
      username: socialUserId.value,
      mobile: bindForm.mobile,
      code: bindForm.code,
      authType: String(route.params.provider || ''),
    })
    await finishLogin(token)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '账号绑定失败'
  } finally {
    loading.value = false
  }
}

onMounted(process)
onBeforeUnmount(() => { if (timer) window.clearInterval(timer) })
</script>

<template>
  <main class="passport-page"><div class="passport-container"><header class="passport-header"><img class="logo" src="/assets/logo.png" alt="MaxKey" /><div class="title"><b>Max</b><strong>Key</strong> 认证处理中</div></header><section class="passport-wrap"><div class="login-panel">
    <a-spin v-if="loading" tip="正在完成认证，请稍候…" />
    <template v-else-if="bindRequired">
      <h2>绑定已有账号</h2><p>首次使用该社会化账号登录，请验证已有 MaxKey 账号。</p>
      <a-alert v-if="error" type="error" show-icon :message="error" class="login-alert" />
      <a-form layout="vertical" @submit.prevent="bindUser">
        <a-form-item label="手机号"><a-input v-model:value="bindForm.mobile" /></a-form-item>
        <a-form-item label="验证码"><a-input v-model:value="bindForm.code"><template #addonAfter><a-button type="link" :disabled="countdown > 0" @click="sendCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</a-button></template></a-input></a-form-item>
        <a-button type="primary" html-type="submit" block>绑定并登录</a-button>
      </a-form>
    </template>
    <a-result v-else status="error" title="认证未完成" :sub-title="error || '请返回登录页重试'"><template #extra><a-button type="primary" @click="router.replace('/passport/login')">返回登录</a-button></template></a-result>
  </div></section></div></main>
</template>

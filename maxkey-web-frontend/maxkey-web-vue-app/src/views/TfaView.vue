<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { auth, type MaxKeyToken } from '../auth'
import { post } from '../api'
import { navigateAfterLogin } from '../authRedirect'

const router = useRouter()
const data = ref<MaxKeyToken & { mobile?: string; remeberMeRequested?: boolean }>({})
const state = ref('')
const loading = ref(false)
const error = ref('')
const form = reactive({ otpCaptcha: '', remember: false })
const countdown = ref(0)
let timer: number | undefined

async function load() {
  try {
    data.value = JSON.parse(localStorage.getItem('two_factor_data') || '{}')
    if (!data.value.token || String(data.value.twoFactor || '0') === '0') throw new Error('二次认证信息不存在')
    form.remember = Boolean(data.value.remeberMeRequested)
    const config = await auth.loadLoginConfig(false)
    state.value = String(config.state || '')
    if (!state.value) throw new Error('登录状态已失效')
  } catch {
    error.value = '二次认证信息已失效，请重新登录'
  }
}

async function sendCode() {
  try {
    await post('/login/sendTwoFactorCode?_allow_anonymous=true', { jwtToken: data.value.token })
    countdown.value = 59
    if (timer) window.clearInterval(timer)
    timer = window.setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) window.clearInterval(timer)
    }, 1000)
    message.success('验证码已发送')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '验证码发送失败')
  }
}

async function submit() {
  if (!form.otpCaptcha) { error.value = '请输入验证码'; return }
  loading.value = true
  try {
    const token = await post<MaxKeyToken>('/login/signin?_allow_anonymous=true', { authType: 'twoFactor', state: state.value, jwtToken: data.value.token, otpCaptcha: form.otpCaptcha, remeberMe: form.remember })
    auth.signIn(token)
    localStorage.removeItem('two_factor_data')
    await navigateAfterLogin(router)
  } catch (err) { error.value = err instanceof Error ? err.message : '二次认证失败' } finally { loading.value = false }
}

onMounted(load)
onBeforeUnmount(() => { if (timer) window.clearInterval(timer) })
</script>

<template><main class="passport-page"><div class="passport-container"><header class="passport-header"><img class="logo" src="/assets/logo.png" alt="MaxKey" /><div class="title"><b>Max</b><strong>Key</strong> 二次认证</div></header><section class="passport-wrap"><div class="login-panel"><a-alert v-if="error" type="error" :message="error" show-icon /><a-form layout="vertical" @submit.prevent="submit">
  <a-form-item v-if="String(data.twoFactor) === '2'" label="邮箱"><a-input :value="String(data.email || '')" disabled /></a-form-item>
  <a-form-item v-if="String(data.twoFactor) === '3'" label="手机号"><a-input :value="String(data.mobile || '')" disabled /></a-form-item>
  <a-form-item :label="String(data.twoFactor) === '1' ? '动态口令' : '验证码'"><a-input v-model:value="form.otpCaptcha"><template v-if="['2', '3'].includes(String(data.twoFactor))" #addonAfter><a-button type="link" :disabled="countdown > 0" @click="sendCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</a-button></template></a-input></a-form-item>
  <a-form-item><a-checkbox v-model:checked="form.remember">记住我</a-checkbox><RouterLink style="float: right" to="/passport/login">返回登录</RouterLink></a-form-item>
  <a-button type="primary" html-type="submit" block :loading="loading" :disabled="!data.token">确认</a-button>
</a-form></div></section></div></main></template>

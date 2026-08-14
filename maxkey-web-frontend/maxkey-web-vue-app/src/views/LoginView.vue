<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, type MaxKeyToken } from '../auth'
import { get, post } from '../api'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const error = ref('')
const captchaImage = ref('')
const captchaType = ref('NONE')
const state = ref('')
const loginType = ref<'normal' | 'qrscan'>('normal')
const passwordVisible = ref(false)
const form = ref({ username: '', password: '', captcha: '', remember: false })
const socials = ref<Array<{ provider: string; providerName: string; icon?: string }>>([])
const qrImage = ref('')
const qrTicket = ref('')
const qrExpired = ref(false)
let qrTimer: number | undefined

async function loadConfig() {
  try {
    const data = await auth.loadLoginConfig()
    captchaType.value = String(data.captcha || 'NONE')
    state.value = String(data.state || '')
    captchaImage.value = ''
    socials.value = (data.socials as Array<{ provider: string; providerName: string; icon?: string }>) || []
    if (captchaType.value.toUpperCase() !== 'NONE') await loadCaptcha()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '无法加载登录配置'
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
  if (!form.value.username || !form.value.password) {
    error.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    localStorage.setItem('remember_me', form.value.remember ? 'true' : '')
    await auth.login({
      authType: 'normal',
      state: state.value,
      username: form.value.username,
      password: form.value.password,
      captcha: form.value.captcha,
      remeberMe: form.value.remember,
    })
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/portal')
  } catch (err) {
    if (err && typeof err === 'object' && 'twoFactor' in err) {
      await router.replace('/passport/tfa')
      return
    }
    error.value = err instanceof Error ? err.message : '登录失败'
    await loadConfig()
  } finally {
    loading.value = false
  }
}

function socialLogin(provider: string) {
  window.location.href = `/sign/socialsignon/${provider}`
}

async function loadQrCode() {
  try {
    qrExpired.value = false
    const data = await get<{ rqCode?: string; ticket?: string }>('/login/genScanCode')
    qrImage.value = String(data.rqCode || '')
    qrTicket.value = String(data.ticket || '')
    if (qrTimer) window.clearInterval(qrTimer)
    qrTimer = window.setInterval(pollQrLogin, 5000)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '二维码加载失败'
  }
}

async function pollQrLogin() {
  if (!qrTicket.value || qrExpired.value) return
  try {
    const token = await post<MaxKeyToken>('/login/sign/qrcode?_allow_anonymous=true', { authType: 'scancode', code: qrTicket.value, state: state.value })
    auth.signIn(token)
    qrExpired.value = true
    if (qrTimer) window.clearInterval(qrTimer)
    await router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/portal')
  } catch {
    // 二维码未扫码或尚未确认时，服务端会返回业务错误，继续轮询即可。
  }
}

watch(loginType, value => {
  if (value === 'qrscan') loadQrCode()
})

onMounted(loadConfig)
onBeforeUnmount(() => { if (qrTimer) window.clearInterval(qrTimer) })
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
            <a-radio-button value="qrscan">扫码登录</a-radio-button>
          </a-radio-group>
          <a-alert v-if="error" type="error" show-icon :message="error" class="login-alert" />
          <template v-if="loginType === 'normal'">
            <a-form layout="vertical" @submit.prevent="submit">
              <a-form-item>
                <a-input v-model:value="form.username" size="large" placeholder="用户名" autocomplete="username" />
              </a-form-item>
              <a-form-item>
                <a-input-password v-model:value="form.password" size="large" placeholder="密码" autocomplete="current-password" :visibility-toggle="{ visible: passwordVisible, onVisibleChange: (value: boolean) => (passwordVisible = value) }" />
              </a-form-item>
              <a-form-item v-if="captchaType.toUpperCase() !== 'NONE'">
                <a-input v-model:value="form.captcha" size="large" placeholder="验证码">
                  <template #addonAfter><img v-if="captchaImage" class="captcha" :src="captchaImage" alt="验证码" @click="loadCaptcha" /><a-button v-else type="link" @click="loadCaptcha">获取验证码</a-button></template>
                </a-input>
              </a-form-item>
              <a-form-item>
                <div class="login-options"><a-checkbox v-model:checked="form.remember">记住我</a-checkbox><RouterLink to="/passport/forgot">忘记密码</RouterLink></div>
              </a-form-item>
              <a-form-item><a-button html-type="submit" type="primary" size="large" block :loading="loading">登录</a-button></a-form-item>
            </a-form>
          </template>
          <template v-else>
            <div class="qrcode-placeholder"><img v-if="qrImage" :src="qrImage" alt="登录二维码" /><span v-else>二维码加载中…</span><a-typography-text v-if="qrExpired" type="warning">二维码已失效，请重新获取</a-typography-text></div>
            <a-button v-if="qrExpired" block @click="loadQrCode">重新获取二维码</a-button>
            <a-button block @click="loginType = 'normal'">返回账号登录</a-button>
          </template>
          <div v-if="socials.length && loginType === 'normal'" class="login-other">其他方式登录 <img v-for="item in socials" :key="item.provider" class="social-icon" :src="item.icon" :alt="item.providerName" @click="socialLogin(item.provider)" /></div>
        </div>
      </section>
      <footer class="passport-footer">MaxKey v4.2.0<br />Copyright {{ new Date().getFullYear() }} <a href="//www.maxkey.top" target="_blank">http://www.maxkey.top</a><br />Licensed under the Apache License, Version 2.0</footer>
    </div>
  </main>
</template>

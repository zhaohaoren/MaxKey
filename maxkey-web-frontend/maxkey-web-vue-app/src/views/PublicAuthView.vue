<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { get } from '../api'

const route = useRoute()
const router = useRouter()
const mode = computed(() => route.path.includes('register') ? 'register' : 'forgot')
const title = computed(() => mode.value === 'register' ? '用户注册' : '忘记密码')
const loading = ref(false)
const sent = ref(false)
const error = ref('')
const countdown = ref(0)
const form = reactive({ username: '', displayName: '', email: '', mobile: '', password: '', confirmPassword: '', captcha: '' })
let timer: number | undefined

async function sendCode() {
  const endpoint = mode.value === 'register' ? '/signup/produceOtp?_allow_anonymous=true' : form.email ? '/forgotpassword/produceEmailOtp?_allow_anonymous=true' : '/forgotpassword/produceOtp?_allow_anonymous=true'
  const params = mode.value === 'register' ? { mobile: form.mobile } : form.email ? { email: form.email } : { mobile: form.mobile }
  try {
    const result = await get<Record<string, unknown>>(endpoint, params)
    if (mode.value === 'forgot' && result) Object.assign(form, { username: result.username || form.username })
    sent.value = true
    countdown.value = 60
    timer = window.setInterval(() => { countdown.value -= 1; if (countdown.value <= 0 && timer) window.clearInterval(timer) }, 1000)
    message.success('验证码已发送')
  } catch (err) { error.value = err instanceof Error ? err.message : '验证码发送失败' }
}

async function submit() {
  error.value = ''
  if (!form.password || form.password !== form.confirmPassword) { error.value = '两次密码输入不一致'; return }
  loading.value = true
  try {
    if (mode.value === 'register') {
      await get('/signup/register?_allow_anonymous=true', form)
      message.success('注册成功')
    } else {
      await get('/forgotpassword/setpassword?_allow_anonymous=true', form)
      message.success('密码重置成功')
    }
    await router.replace('/passport/login')
  } catch (err) { error.value = err instanceof Error ? err.message : '提交失败' } finally { loading.value = false }
}
</script>

<template>
  <main class="passport-page"><div class="passport-container"><header class="passport-header"><img class="logo" src="/assets/logo.png" alt="MaxKey" /><div class="title"><b>Max</b><strong>Key</strong> {{ title }}</div></header><section class="passport-wrap"><div class="login-panel"><a-alert v-if="error" type="error" show-icon :message="error" /><a-form layout="vertical" @submit.prevent="submit"><a-form-item v-if="mode === 'register'" label="用户名"><a-input v-model:value="form.username" /></a-form-item><a-form-item v-if="mode === 'register'" label="显示名称"><a-input v-model:value="form.displayName" /></a-form-item><a-form-item label="邮箱"><a-input v-model:value="form.email" /></a-form-item><a-form-item label="手机号"><a-input v-model:value="form.mobile" /></a-form-item><a-form-item v-if="mode === 'forgot' && sent" label="用户名"><a-input v-model:value="form.username" /></a-form-item><a-form-item label="短信/邮件验证码"><a-input v-model:value="form.captcha"><template #addonAfter><a-button type="link" :disabled="countdown > 0" @click="sendCode">{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</a-button></template></a-input></a-form-item><a-form-item label="新密码"><a-input-password v-model:value="form.password" /></a-form-item><a-form-item label="确认密码"><a-input-password v-model:value="form.confirmPassword" /></a-form-item><a-button type="primary" html-type="submit" block :loading="loading">提交</a-button><a-button type="link" block @click="router.replace('/passport/login')">返回登录</a-button></a-form></div></section></div></main>
</template>

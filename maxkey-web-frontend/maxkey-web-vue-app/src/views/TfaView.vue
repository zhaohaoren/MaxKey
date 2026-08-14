<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { auth, type MaxKeyToken } from '../auth'
import { get, post } from '../api'

const router = useRouter()
const data = ref<Record<string, any>>({})
const state = ref('')
const loading = ref(false)
const error = ref('')
const form = reactive({ otpCaptcha: '', remember: false })

async function load() {
  try { data.value = JSON.parse(localStorage.getItem('two_factor_data') || '{}'); const config = await auth.loadLoginConfig(); state.value = String(config.state || '') } catch { error.value = '二次认证信息已失效，请重新登录' }
}

async function sendCode() {
  try { await post('/login/sendTwoFactorCode?_allow_anonymous=true', { jwtToken: data.value.token }); message.success('验证码已发送') } catch (err) { message.error(err instanceof Error ? err.message : '验证码发送失败') }
}

async function submit() {
  if (!form.otpCaptcha) { error.value = '请输入验证码'; return }
  loading.value = true
  try {
    const token = await post<MaxKeyToken>('/login/signin?_allow_anonymous=true', { authType: 'twoFactor', state: state.value, jwtToken: data.value.token, otpCaptcha: form.otpCaptcha, remeberMe: form.remember })
    auth.signIn(token)
    localStorage.removeItem('two_factor_data')
    await router.replace('/portal')
  } catch (err) { error.value = err instanceof Error ? err.message : '二次认证失败' } finally { loading.value = false }
}

onMounted(load)
</script>

<template><main class="passport-page"><div class="passport-container"><header class="passport-header"><img class="logo" src="/assets/logo.png" alt="MaxKey" /><div class="title"><b>Max</b><strong>Key</strong> 二次认证</div></header><section class="passport-wrap"><div class="login-panel"><a-alert v-if="error" type="error" :message="error" show-icon /><a-form layout="vertical" @submit.prevent="submit"><a-form-item label="验证码"><a-input v-model:value="form.otpCaptcha"><template #addonAfter><a-button type="link" @click="sendCode">发送验证码</a-button></template></a-input></a-form-item><a-form-item><a-checkbox v-model:checked="form.remember">记住我</a-checkbox></a-form-item><a-button type="primary" html-type="submit" block :loading="loading">确认</a-button></a-form></div></section></div></main></template>

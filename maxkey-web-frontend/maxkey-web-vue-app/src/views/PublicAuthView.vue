<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { get, postForm } from '../api'

interface PasswordPolicy {
  minLength?: number
  maxLength?: number
  lowerCase?: number
  upperCase?: number
  digits?: number
  specialChar?: number
  policMessageList?: string[]
}

const route = useRoute()
const router = useRouter()
const mode = computed(() => route.path.endsWith('register-result') ? 'result' : route.path.endsWith('register') ? 'register' : 'forgot')
const title = computed(() => mode.value === 'result' ? '注册完成' : mode.value === 'register' ? '用户注册' : '忘记密码')
const loading = ref(false)
const sending = ref(false)
const error = ref('')
const countdown = ref(0)
const forgotStep = ref(0)
const forgotType = ref<'mobile' | 'email'>('mobile')
const captchaImage = ref('')
const captchaState = ref('')
const resetUserId = ref('')
const resetUsername = ref('')
const policy = ref<PasswordPolicy>({})
const registerForm = reactive({ username: '', displayName: '', email: '', mobilePrefix: '+86', mobile: '', password: '', confirm: '', captcha: '' })
const forgotForm = reactive({ email: '', mobile: '', captcha: '', otpCaptcha: '', password: '', confirmPassword: '' })
let timer: number | undefined

function startCountdown() {
  countdown.value = 59
  if (timer) window.clearInterval(timer)
  timer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0 && timer) window.clearInterval(timer)
  }, 1000)
}

async function loadCaptcha() {
  try {
    const result = await get<{ image?: string; state?: string }>('/captcha?_allow_anonymous=true')
    captchaImage.value = String(result.image || '')
    captchaState.value = String(result.state || '')
    forgotForm.captcha = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '图片验证码加载失败'
  }
}

async function initializeForgot() {
  forgotStep.value = 0
  resetUserId.value = ''
  resetUsername.value = ''
  await loadCaptcha()
  try {
    policy.value = await get<PasswordPolicy>('/forgotpassword/passwordpolicy?_allow_anonymous=true')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '密码策略加载失败'
  }
}

async function sendRegisterCode() {
  error.value = ''
  if (!/^1\d{10}$/.test(registerForm.mobile)) {
    error.value = '请输入正确的手机号'
    return
  }
  sending.value = true
  try {
    await get('/signup/produceOtp?_allow_anonymous=true', { mobile: registerForm.mobile })
    startCountdown()
    message.success('短信验证码已发送')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '短信验证码发送失败'
  } finally {
    sending.value = false
  }
}

async function sendForgotCode() {
  error.value = ''
  const account = forgotType.value === 'mobile' ? forgotForm.mobile : forgotForm.email
  if (!account || !forgotForm.captcha) {
    error.value = '请输入账号信息和图片验证码'
    return
  }
  sending.value = true
  try {
    const endpoint = forgotType.value === 'mobile' ? '/forgotpassword/produceOtp?_allow_anonymous=true' : '/forgotpassword/produceEmailOtp?_allow_anonymous=true'
    const params = forgotType.value === 'mobile'
      ? { mobile: forgotForm.mobile, state: captchaState.value, captcha: forgotForm.captcha }
      : { email: forgotForm.email, state: captchaState.value, captcha: forgotForm.captcha }
    const result = await get<{ userId?: string; username?: string }>(endpoint, params)
    resetUserId.value = String(result.userId || '')
    resetUsername.value = String(result.username || '')
    if (!resetUserId.value) throw new Error('服务端未返回待重置用户')
    startCountdown()
    message.success('验证码已发送')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '验证码发送失败'
    await loadCaptcha()
  } finally {
    sending.value = false
  }
}

async function nextForgotStep() {
  error.value = ''
  if (!resetUserId.value || !forgotForm.otpCaptcha) {
    error.value = '请先获取并输入短信或邮件验证码'
    return
  }
  loading.value = true
  try {
    await get('/forgotpassword/validateCaptcha?_allow_anonymous=true', {
      forgotType: forgotType.value,
      userId: resetUserId.value,
      state: captchaState.value,
      captcha: forgotForm.captcha,
      otpCaptcha: forgotForm.otpCaptcha,
    })
    forgotStep.value = 1
  } catch (err) {
    error.value = err instanceof Error ? err.message : '验证码校验失败'
  } finally {
    loading.value = false
  }
}

function passwordPolicyError(value: string) {
  const current = policy.value
  if (current.minLength && value.length < current.minLength) return '密码不符合密码强度策略'
  if (current.maxLength && value.length > current.maxLength) return '密码不符合密码强度策略'
  if (current.lowerCase && (value.match(/[a-z]/g) || []).length < current.lowerCase) return '密码不符合密码强度策略'
  if (current.upperCase && (value.match(/[A-Z]/g) || []).length < current.upperCase) return '密码不符合密码强度策略'
  if (current.digits && (value.match(/[0-9]/g) || []).length < current.digits) return '密码不符合密码强度策略'
  if (current.specialChar && (value.match(/[^a-zA-Z0-9]/g) || []).length < current.specialChar) return '密码不符合密码强度策略'
  return ''
}

async function submitForgot() {
  error.value = passwordPolicyError(forgotForm.password)
  if (!forgotForm.password || forgotForm.password !== forgotForm.confirmPassword) error.value = '两次密码输入不一致'
  if (error.value) return
  loading.value = true
  try {
    await get('/forgotpassword/setpassword?_allow_anonymous=true', {
      forgotType: forgotType.value,
      userId: resetUserId.value,
      username: resetUsername.value,
      password: forgotForm.password,
      confirmPassword: forgotForm.confirmPassword,
      otpCaptcha: forgotForm.otpCaptcha,
      state: captchaState.value,
    })
    message.success('密码重置成功')
    await router.replace('/passport/login')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '密码重置失败'
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  error.value = ''
  if (!registerForm.username || !registerForm.displayName || !registerForm.email || !/^1\d{10}$/.test(registerForm.mobile)) error.value = '请完整填写注册信息'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(registerForm.email)) error.value = '请输入正确的邮箱地址'
  else if (registerForm.password.length < 6 || registerForm.password !== registerForm.confirm) error.value = '两次密码输入不一致或密码长度不足'
  else if (!registerForm.captcha) error.value = '请输入短信验证码'
  if (error.value) return
  loading.value = true
  try {
    await postForm('/signup/register?_allow_anonymous=true', registerForm)
    message.success('注册成功')
    await router.replace({ path: '/passport/register-result', query: { email: registerForm.email } })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '注册失败'
  } finally {
    loading.value = false
  }
}

watch(mode, value => {
  error.value = ''
  if (value === 'forgot') initializeForgot()
}, { immediate: true })

onBeforeUnmount(() => { if (timer) window.clearInterval(timer) })
</script>

<template>
  <main class="passport-page"><div class="passport-container"><header class="passport-header"><img class="logo" src="/assets/logo.png" alt="MaxKey" /><div class="title"><b>Max</b><strong>Key</strong> {{ title }}</div></header><section class="passport-wrap"><div class="login-panel">
    <a-result v-if="mode === 'result'" status="success" title="注册申请已提交" :sub-title="`账号激活信息将发送至 ${String(route.query.email || '您的邮箱')}`"><template #extra><a-button type="primary" @click="router.replace('/passport/login')">返回登录</a-button></template></a-result>

    <template v-else-if="mode === 'register'">
      <a-alert v-if="error" type="error" show-icon :message="error" />
      <a-form layout="vertical" @submit.prevent="submitRegister">
        <a-form-item label="用户名"><a-input v-model:value="registerForm.username" /></a-form-item>
        <a-form-item label="显示名称"><a-input v-model:value="registerForm.displayName" /></a-form-item>
        <a-form-item label="邮箱"><a-input v-model:value="registerForm.email" /></a-form-item>
        <a-form-item label="密码"><a-input-password v-model:value="registerForm.password" /></a-form-item>
        <a-form-item label="确认密码"><a-input-password v-model:value="registerForm.confirm" /></a-form-item>
        <a-form-item label="手机号"><a-input v-model:value="registerForm.mobile"><template #addonBefore><a-select v-model:value="registerForm.mobilePrefix" style="width: 84px"><a-select-option value="+86">+86</a-select-option><a-select-option value="+87">+87</a-select-option></a-select></template></a-input></a-form-item>
        <a-form-item label="短信验证码"><a-input v-model:value="registerForm.captcha"><template #addonAfter><a-button type="link" :loading="sending" :disabled="countdown > 0" @click="sendRegisterCode">{{ countdown > 0 ? `${countdown}s` : '获取验证码' }}</a-button></template></a-input></a-form-item>
        <a-button type="primary" html-type="submit" block :loading="loading">注册</a-button><a-button type="link" block @click="router.replace('/passport/login')">返回登录</a-button>
      </a-form>
    </template>

    <template v-else>
      <a-steps :current="forgotStep" size="small" class="feature-actions"><a-step title="验证身份" /><a-step title="重置密码" /></a-steps>
      <a-alert v-if="error" type="error" show-icon :message="error" />
      <a-form v-if="forgotStep === 0" layout="vertical" @submit.prevent="nextForgotStep">
        <a-radio-group v-model:value="forgotType" button-style="solid" class="login-tabs"><a-radio-button value="mobile">手机号找回</a-radio-button><a-radio-button value="email">邮箱找回</a-radio-button></a-radio-group>
        <a-form-item v-if="forgotType === 'mobile'" label="手机号"><a-input v-model:value="forgotForm.mobile" /></a-form-item>
        <a-form-item v-else label="邮箱"><a-input v-model:value="forgotForm.email" /></a-form-item>
        <a-form-item label="图片验证码"><a-input v-model:value="forgotForm.captcha"><template #addonAfter><img v-if="captchaImage" class="captcha" :src="captchaImage" alt="验证码" @click="loadCaptcha" /><a-button v-else type="link" @click="loadCaptcha">刷新</a-button></template></a-input></a-form-item>
        <a-form-item label="短信/邮件验证码"><a-input v-model:value="forgotForm.otpCaptcha"><template #addonAfter><a-button type="link" :loading="sending" :disabled="countdown > 0" @click="sendForgotCode">{{ countdown > 0 ? `${countdown}s` : '发送验证码' }}</a-button></template></a-input></a-form-item>
        <a-button type="primary" html-type="submit" block :loading="loading">下一步</a-button><a-button type="link" block @click="router.replace('/passport/login')">返回登录</a-button>
      </a-form>
      <a-form v-else layout="vertical" @submit.prevent="submitForgot">
        <a-alert v-if="policy.policMessageList?.length" type="info" show-icon><template #message><div>密码强度要求</div><div v-for="item in policy.policMessageList" :key="item">{{ item }}</div></template></a-alert>
        <a-form-item label="新密码"><a-input-password v-model:value="forgotForm.password" /></a-form-item>
        <a-form-item label="确认密码"><a-input-password v-model:value="forgotForm.confirmPassword" /></a-form-item>
        <a-button type="primary" html-type="submit" block :loading="loading">提交</a-button><a-button type="link" block @click="router.replace('/passport/login')">返回登录</a-button>
      </a-form>
    </template>
  </div></section></div></main>
</template>

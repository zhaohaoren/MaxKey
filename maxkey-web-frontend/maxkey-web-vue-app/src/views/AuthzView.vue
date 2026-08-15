<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { get, post, put } from '../api'

const route = useRoute()
const router = useRouter()
const feature = computed(() => String(route.meta.authz || 'credential'))
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const credential = reactive<Record<string, unknown>>({})
const approval = reactive<Record<string, unknown>>({ user_oauth_approval: true, approval_prompt: 'force' })

async function loadCredential() {
  const appId = String(route.query.appId || '')
  if (!appId) throw new Error('缺少应用标识 appId')
  Object.assign(credential, await get<Record<string, unknown>>(`/authz/credential/get/${encodeURIComponent(appId)}`))
}

async function loadApproval() {
  const approvalId = String(route.query.oauth_approval || '')
  if (!approvalId) throw new Error('缺少授权请求标识 oauth_approval')
  Object.assign(approval, await get<Record<string, unknown>>(`/authz/oauth/v20/approval_confirm/get/${encodeURIComponent(approvalId)}`))
  approval.user_oauth_approval = true
  if (approval.approval_prompt === 'auto') await submitApproval()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (feature.value === 'mgt') {
      window.location.replace('/sign/authz/maxkey_mgt')
      return
    }
    if (feature.value === 'oauth') await loadApproval()
    else await loadCredential()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '授权信息加载失败'
  } finally {
    loading.value = false
  }
}

async function saveCredential() {
  if (!credential.relatedUsername || !credential.relatedPassword) {
    error.value = '请输入应用账号和密码'
    return
  }
  if (credential.relatedPassword !== credential.confirmPassword) {
    error.value = '两次密码输入不一致'
    return
  }
  saving.value = true
  try {
    await put('/authz/credential/update', credential)
    message.success('应用凭证保存成功')
    const redirect = String(route.query.redirect_uri || '')
    if (redirect) window.location.href = redirect.startsWith('http') ? redirect : `/sign/${redirect.replace(/^\//, '')}`
    else await router.replace('/dashboard/home')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用凭证保存失败'
  } finally {
    saving.value = false
  }
}

async function submitApproval() {
  saving.value = true
  try {
    const redirect = await post<string>(`/authz/oauth/v20/authorize/approval?user_oauth_approval=${approval.user_oauth_approval ? 'true' : 'false'}`, approval)
    if (approval.approval_prompt !== 'auto') message.success('授权已提交')
    if (redirect) window.location.href = redirect
  } catch (err) {
    error.value = err instanceof Error ? err.message : '授权提交失败'
  } finally {
    saving.value = false
  }
}

function deny() {
  approval.user_oauth_approval = false
  submitApproval()
}

onMounted(load)
</script>

<template>
  <main class="passport-page">
    <div class="passport-container authz-container">
      <header class="passport-header"><img class="logo" src="/assets/logo.png" alt="MaxKey" /><div class="title"><b>Max</b><strong>Key</strong> 应用授权</div></header>
      <section class="passport-wrap">
        <div class="login-panel authz-panel">
          <a-spin :spinning="loading">
            <a-alert v-if="error" type="error" show-icon :message="error" class="login-alert" />
            <template v-if="feature === 'oauth' && !loading">
              <a-result title="应用访问授权" :sub-title="`${approval.appName || '第三方应用'} 正在申请访问您的 MaxKey 账号`">
                <template #icon><a-avatar :src="String(approval.iconBase64 || '')" :size="88">{{ String(approval.appName || 'A').slice(0, 1) }}</a-avatar></template>
                <template #extra><a-space><a-button type="primary" :loading="saving" @click="submitApproval">同意授权</a-button><a-button :disabled="saving" @click="deny">拒绝</a-button></a-space></template>
              </a-result>
            </template>
            <a-form v-else-if="feature === 'credential' && !loading" layout="vertical" @submit.prevent="saveCredential">
              <a-form-item label="用户"><a-input :value="String(credential.displayName || credential.username || '')" disabled /></a-form-item>
              <a-form-item label="应用账号" required><a-input v-model:value="credential.relatedUsername" /></a-form-item>
              <a-form-item label="应用密码" required><a-input-password v-model:value="credential.relatedPassword" /></a-form-item>
              <a-form-item label="确认应用密码" required><a-input-password v-model:value="credential.confirmPassword" /></a-form-item>
              <a-button type="primary" html-type="submit" block :loading="saving">保存并继续</a-button>
            </a-form>
          </a-spin>
        </div>
      </section>
    </div>
  </main>
</template>

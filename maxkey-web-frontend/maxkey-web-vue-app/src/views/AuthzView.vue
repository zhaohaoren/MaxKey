<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { get, post, put } from '../api'

const route = useRoute()
const { t } = useI18n({ useScope: 'global' })
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
    message.success(t('portal.mxk.alert.operate.success'))
    const redirect = String(route.query.redirect_uri || '')
    if (redirect) window.location.href = redirect.startsWith('http') ? redirect : `/sign/${redirect.replace(/^\//, '')}`
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('portal.mxk.alert.operate.error')
  } finally {
    saving.value = false
  }
}

async function submitApproval() {
  saving.value = true
  try {
    const redirect = await post<string>(`/authz/oauth/v20/authorize/approval?user_oauth_approval=${approval.user_oauth_approval ? 'true' : 'false'}`, approval)
    if (approval.approval_prompt !== 'auto') message.success(t('portal.mxk.alert.operate.success'))
    if (redirect) window.location.href = redirect
  } catch (err) {
    error.value = err instanceof Error ? err.message : '授权提交失败'
  } finally {
    saving.value = false
  }
}

function deny() {
  window.close()
}

onMounted(load)
</script>

<template>
  <main class="authz-blank">
    <a-spin :spinning="loading">
      <a-alert v-if="error" type="error" show-icon :message="error" />
      <a-card v-if="feature === 'oauth' && !loading" class="authz-card">
        <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
          <div class="authz-title"><h2>{{ t('portal.mxk.apps.oauth.approval.title') }}</h2></div>
          <div class="authz-approval">
            <img :src="String(approval.iconBase64 || '')" alt="" />
            <div>
              <p class="authz-app-name">{{ approval.appName }}</p>
              <p>{{ t('portal.mxk.apps.oauth.approval.info') }}</p>
              <a-checkbox v-model:checked="approval.user_oauth_approval">{{ t('portal.mxk.apps.oauth.approval.context') }}</a-checkbox>
            </div>
          </div>
          <div class="authz-actions"><a-space><a-button type="primary" :loading="saving" @click="submitApproval">{{ t('portal.mxk.apps.oauth.approval.authorize') }}</a-button><a-button :disabled="saving" @click="deny">{{ t('portal.mxk.apps.oauth.approval.deny') }}</a-button></a-space></div>
        </a-form>
      </a-card>
      <a-card v-else-if="feature === 'credential' && !loading" class="authz-card">
        <a-form :label-col="{ span: 7 }" :wrapper-col="{ span: 14 }" @submit.prevent="saveCredential">
          <a-form-item :label="t('portal.mxk.password.displayName')"><a-input v-model:value="credential.displayName" disabled /></a-form-item>
          <a-form-item :label="t('portal.mxk.password.username')"><a-input v-model:value="credential.username" disabled /></a-form-item>
          <a-form-item required :label="t('portal.mxk.accounts.relatedUsername')"><a-input v-model:value="credential.relatedUsername" /></a-form-item>
          <a-form-item required :label="t('portal.mxk.accounts.relatedPassword')"><a-input-password v-model:value="credential.relatedPassword" /></a-form-item>
          <a-form-item required :label="t('portal.mxk.password.confirmPassword')"><a-input-password v-model:value="credential.confirmPassword" /></a-form-item>
          <a-form-item :wrapper-col="{ offset: 10, span: 12 }"><a-button type="primary" html-type="submit" :loading="saving">{{ t('portal.mxk.text.save') }}</a-button></a-form-item>
        </a-form>
      </a-card>
    </a-spin>
  </main>
</template>

<style scoped>
.authz-blank { min-height: 100vh; padding: 48px 24px; background: #f5f7fa; }
.authz-card { max-width: 760px; margin: 0 auto; }
.authz-title { text-align: center; }
.authz-approval { display: grid; grid-template-columns: 160px 1fr; gap: 24px; align-items: center; margin: 36px 0; }
.authz-approval img { width: 120px; max-height: 120px; margin: 0 auto; object-fit: contain; }
.authz-app-name { font-size: 16px; font-weight: 600; }
.authz-actions { text-align: center; }
@media (max-width: 600px) { .authz-approval { grid-template-columns: 1fr; text-align: center; } }
</style>

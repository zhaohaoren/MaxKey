<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { auth } from '../auth'
import { del, get, post, put } from '../api'

interface RecordData { id?: string; credentialId?: string; [key: string]: unknown }

const route = useRoute()
const feature = computed(() => {
  const value = String(route.path)
  if (value.includes('sessions')) return 'sessions'
  if (value.includes('profile')) return 'profile'
  if (value.includes('password')) return 'password'
  if (value.includes('mfa')) return 'mfa'
  if (value.includes('passkey')) return 'passkey'
  return 'audit'
})
const title = computed(() => ({ sessions: '会话管理', profile: '我的资料', password: '密码修改', mfa: '二次认证', passkey: 'Passkey 注册', audit: '登录审计' }[feature.value]))
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const rows = ref<RecordData[]>([])
const model = reactive<RecordData>({})
const password = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const mfa = reactive<RecordData>({})
const otpCode = ref('')

function userId() {
  const token = auth.token.value || {}
  return String(token.userId || token.id || token.username || '')
}

function normalizeRows(data: unknown): RecordData[] {
  if (Array.isArray(data)) return data as RecordData[]
  const value = (data || {}) as Record<string, unknown>
  return Array.isArray(value.rows) ? value.rows as RecordData[] : Array.isArray(value.records) ? value.records as RecordData[] : []
}

function valueOf(item: RecordData, key: string) {
  const value = item[key]
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (feature.value === 'sessions') {
      rows.value = normalizeRows(await get('/access/session/fetch', { pageNumber: 1, pageSize: 50 }))
    } else if (feature.value === 'profile') {
      Object.assign(model, await get<RecordData>('/users/profile/get'))
    } else if (feature.value === 'mfa') {
      Object.assign(mfa, await get<RecordData>('/config/timebased/view'))
    } else if (feature.value === 'passkey') {
      rows.value = normalizeRows(await get(`/passkey/registration/list/${userId()}`))
    } else if (feature.value === 'audit') {
      rows.value = normalizeRows(await get('/historys/fetch', { pageNumber: 1, pageSize: 50 }))
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '数据加载失败'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try { await put('/users/profile/update', model); message.success('资料保存成功') } catch (err) { message.error(err instanceof Error ? err.message : '保存失败') } finally { saving.value = false }
}

async function changePassword() {
  if (!password.newPassword || password.newPassword !== password.confirmPassword) { message.warning('两次输入的新密码不一致'); return }
  saving.value = true
  try { await put('/users/changePassword', password); message.success('密码修改成功'); password.oldPassword = ''; password.newPassword = ''; password.confirmPassword = '' } catch (err) { message.error(err instanceof Error ? err.message : '密码修改失败') } finally { saving.value = false }
}

async function generateMfa() {
  try { Object.assign(mfa, await get('/config/timebased/generate')); message.success('新的认证密钥已生成') } catch (err) { message.error(err instanceof Error ? err.message : '生成失败') }
}

async function saveMfa() {
  try { await put('/config/timebased/update', mfa); message.success('二次认证配置已保存') } catch (err) { message.error(err instanceof Error ? err.message : '保存失败') }
}

async function verifyMfa() {
  try { await get('/config/timebased/verify', { otpCode: otpCode.value }); message.success('验证码正确') } catch (err) { message.error(err instanceof Error ? err.message : '验证码校验失败') }
}

async function terminate(ids: string[]) {
  Modal.confirm({ title: '确认终止会话？', async onOk() { try { await del('/access/session/terminate', { ids: ids.join(',') }); message.success('会话已终止'); await load() } catch (err) { message.error(err instanceof Error ? err.message : '操作失败') } } })
}

async function deletePasskey(item: RecordData) {
  Modal.confirm({ title: '确认删除 Passkey？', async onOk() { try { await del(`/passkey/registration/delete/${userId()}/${item.credentialId}`); message.success('Passkey 已删除'); await load() } catch (err) { message.error(err instanceof Error ? err.message : '删除失败') } } })
}

function toBuffer(value: string) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const binary = atob(normalized + '='.repeat((4 - normalized.length % 4) % 4))
  return Uint8Array.from(binary, char => char.charCodeAt(0)).buffer
}

function toBase64(value: ArrayBuffer) {
  return btoa(String.fromCharCode(...new Uint8Array(value)))
}

async function registerPasskey() {
  if (!window.PublicKeyCredential || !navigator.credentials) { message.error('当前浏览器不支持 Passkey'); return }
  try {
    saving.value = true
    const options = await post<Record<string, any>>('/passkey/registration/begin', { userId: userId(), username: String(auth.token.value?.username || 'user'), displayName: String(auth.token.value?.displayName || '用户') })
    const publicKey = { ...options, challenge: toBuffer(options.challenge), user: { ...options.user, id: toBuffer(options.user.id) }, excludeCredentials: (options.excludeCredentials || []).map((item: Record<string, any>) => ({ ...item, id: toBuffer(item.id) })) }
    const credential = await navigator.credentials.create({ publicKey: publicKey as PublicKeyCredentialCreationOptions }) as PublicKeyCredential
    const response = credential.response as AuthenticatorAttestationResponse
    await post('/passkey/registration/finish', { userId: userId(), challengeId: options.challengeId, credentialId: credential.id, attestationObject: toBase64(response.attestationObject), clientDataJSON: toBase64(response.clientDataJSON) })
    message.success('Passkey 注册成功')
    await load()
  } catch (err) { message.error(err instanceof Error ? err.message : 'Passkey 注册失败') } finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>{{ title }}</h1><a-button @click="load">刷新</a-button></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />

    <a-card v-if="feature === 'profile'" title="个人资料">
      <a-form layout="vertical" @submit.prevent="saveProfile"><a-row :gutter="16"><a-col :span="12" v-for="key in ['username', 'displayName', 'email', 'mobile', 'employeeNumber']" :key="key"><a-form-item :label="key"><a-input v-model:value="model[key]" /></a-form-item></a-col></a-row><a-button type="primary" :loading="saving" @click="saveProfile">保存</a-button></a-form>
    </a-card>

    <a-card v-else-if="feature === 'password'" title="修改密码"><a-form layout="vertical" @submit.prevent="changePassword"><a-form-item label="当前密码"><a-input-password v-model:value="password.oldPassword" /></a-form-item><a-form-item label="新密码"><a-input-password v-model:value="password.newPassword" /></a-form-item><a-form-item label="确认新密码"><a-input-password v-model:value="password.confirmPassword" /></a-form-item><a-button type="primary" :loading="saving" @click="changePassword">提交</a-button></a-form></a-card>

    <a-card v-else-if="feature === 'mfa'" title="二次认证"><a-descriptions bordered :column="1"><a-descriptions-item label="密钥">{{ mfa.secret || mfa.secretKey || '未生成' }}</a-descriptions-item><a-descriptions-item label="状态">{{ mfa.status || '未启用' }}</a-descriptions-item></a-descriptions><a-space class="feature-actions"><a-button @click="generateMfa">生成密钥</a-button><a-button type="primary" @click="saveMfa">保存配置</a-button></a-space><a-divider /><a-input v-model:value="otpCode" placeholder="输入验证码" style="max-width: 280px" /><a-button @click="verifyMfa">校验</a-button></a-card>

    <a-card v-else-if="feature === 'passkey'" title="Passkey"><a-space class="feature-actions"><a-button type="primary" :loading="saving" @click="registerPasskey">注册 Passkey</a-button></a-space><a-table :data-source="rows" :loading="loading" row-key="id" :pagination="false"><a-table-column title="名称" data-index="displayName" /><a-table-column title="设备" data-index="deviceType" /><a-table-column title="创建时间" data-index="createdDate" /><a-table-column title="操作"><template #default="{ record }"><a-button danger type="link" @click="deletePasskey(record)">删除</a-button></template></a-table-column></a-table></a-card>

    <a-card v-else :title="title"><a-table :data-source="rows" :loading="loading" row-key="id" :pagination="false"><a-table-column v-for="key in (feature === 'sessions' ? ['username', 'host', 'loginTime', 'id'] : ['username', 'action', 'createTime', 'result'])" :key="key" :title="key" :data-index="key" /><a-table-column v-if="feature === 'sessions'" title="操作"><template #default="{ record }"><a-button danger type="link" @click="terminate([String(record.id)])">终止</a-button></template></a-table-column></a-table></a-card>
  </DefaultLayout>
</template>

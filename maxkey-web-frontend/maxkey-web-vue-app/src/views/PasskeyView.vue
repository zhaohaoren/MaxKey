<script setup lang="ts">
import { PlusCircleOutlined } from '@ant-design/icons-vue'
import { onMounted, ref } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { auth } from '../auth'
import { del, get, post } from '../api'
import type { IdentityRow } from '../identity'

const { t } = useI18n({ useScope: 'global' })
const loading = ref(false)
const error = ref('')
const rows = ref<IdentityRow[]>([])

function currentUserId() {
  const token = auth.token.value || {}
  return String(token.userId || token.id || '')
}

function base64UrlToBuffer(value: string) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const binary = atob(normalized + '='.repeat((4 - normalized.length % 4) % 4))
  return Uint8Array.from(binary, char => char.charCodeAt(0)).buffer
}

function bufferToBase64Url(value: ArrayBuffer) {
  return btoa(String.fromCharCode(...new Uint8Array(value))).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
}

function formatDate(value: unknown) {
  if (!value) return '—'
  const date = new Date(String(value))
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (part: number) => String(part).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function load() {
  const userId = currentUserId()
  if (!userId) {
    error.value = t('ui.currentUserMissing')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await get<IdentityRow[]>(`/passkey/registration/list/${encodeURIComponent(userId)}`)
    rows.value = Array.isArray(data) ? data : []
  } catch (err) {
    rows.value = []
    error.value = err instanceof Error ? err.message : t('ui.passkeyLoadError')
  } finally {
    loading.value = false
  }
}

async function register() {
  const userId = currentUserId()
  if (!userId) return void message.error(t('ui.currentUserMissing'))
  if (!window.PublicKeyCredential || !navigator.credentials?.create) return void message.error(t('ui.passkeyUnsupported'))
  loading.value = true
  try {
    const token = auth.token.value || {}
    const options = await post<Record<string, any>>('/passkey/registration/begin', {
      userId,
      username: String(token.username || 'unknown_user'),
      displayName: String(token.displayName || token.username || 'user'),
    })
    const publicKey = {
      ...options,
      challenge: base64UrlToBuffer(String(options.challenge)),
      user: { ...options.user, id: base64UrlToBuffer(String(options.user.id)) },
      excludeCredentials: (options.excludeCredentials || []).map((item: Record<string, any>) => ({ ...item, id: base64UrlToBuffer(String(item.id)) })),
    } as PublicKeyCredentialCreationOptions
    const credential = await navigator.credentials.create({ publicKey }) as PublicKeyCredential | null
    if (!credential) throw new Error(t('ui.passkeyCreateError'))
    const response = credential.response as AuthenticatorAttestationResponse
    await post('/passkey/registration/finish', {
      userId,
      challengeId: options.challengeId,
      credentialId: credential.id,
      attestationObject: bufferToBase64Url(response.attestationObject),
      clientDataJSON: bufferToBase64Url(response.clientDataJSON),
    })
    message.success(t('ui.passkeyRegisterSuccess'))
    await load()
  } catch (err) {
    const error = err as Error & { name?: string }
    if (error.name === 'NotAllowedError') message.error(t('ui.passkeyCancelled'))
    else if (error.name === 'SecurityError') message.error(t('ui.passkeySecurityError'))
    else message.error(error.message || t('ui.passkeyRegisterError'))
  } finally {
    loading.value = false
  }
}

function remove(row: IdentityRow) {
  const credentialId = String(row.credentialId || row.id || '')
  if (!credentialId) return
  Modal.confirm({
    title: t('ui.passkeyDeleteTitle'),
    content: t('ui.passkeyDeleteContent'),
    okType: 'danger',
    async onOk() {
      try {
        await del(`/passkey/registration/delete/${encodeURIComponent(currentUserId())}/${encodeURIComponent(credentialId)}`)
        message.success(t('ui.passkeyDeleteSuccess'))
        await load()
      } catch (err) {
        message.error(err instanceof Error ? err.message : t('ui.passkeyDeleteError'))
      }
    },
  })
}

onMounted(() => { void load() })
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>{{ t('portal.mxk.menu.config.passkey') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :title="t('ui.passkeyManagement')">
      <p class="passkey-description">{{ t('ui.passkeyDescription') }}</p>
      <a-button type="primary" size="large" :loading="loading" @click="register"><PlusCircleOutlined />{{ t('ui.passkeyRegister') }}</a-button>
      <a-divider orientation="left">{{ t('ui.passkeyRegistered') }}</a-divider>
      <a-table :data-source="rows" :loading="loading" row-key="id" :pagination="false" :scroll="{ x: 900 }">
        <a-table-column :title="t('ui.passkeyCredential')"><template #default="{ record }"><div class="passkey-credential">{{ record.credentialId || record.id }}</div><small>{{ record.deviceType === 'platform' ? t('ui.platformAuthenticator') : t('ui.crossPlatformAuthenticator') }}</small></template></a-table-column>
        <a-table-column data-index="signatureCount" :title="t('ui.passkeySignatureCount')"><template #default="{ text }">{{ text || 0 }}</template></a-table-column>
        <a-table-column :title="t('ui.passkeyCreatedDate')"><template #default="{ record }">{{ formatDate(record.createdDate) }}</template></a-table-column>
        <a-table-column :title="t('ui.passkeyLastUsedDate')"><template #default="{ record }">{{ formatDate(record.lastUsedDate) }}</template></a-table-column>
        <a-table-column :title="t('portal.mxk.text.action')"><template #default="{ record }"><a-button danger type="link" @click="remove(record)">{{ t('ui.passkeyDelete') }}</a-button></template></a-table-column>
      </a-table>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.passkey-description { margin-bottom: 24px; color: rgba(0, 0, 0, .55); line-height: 1.7; }
.passkey-credential { max-width: 420px; margin-bottom: 4px; overflow-wrap: anywhere; font-family: Monaco, Menlo, Consolas, monospace; font-size: 12px; }
</style>

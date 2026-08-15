<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost, adminPut } from '../api'

interface AppRow { id?: string; appName?: string; protocol?: string; category?: string; loginUrl?: string; status?: number; [key: string]: unknown }
interface Field { key: string; label: string; type?: 'text' | 'number' | 'textarea' | 'select'; options?: string[]; required?: boolean }

const protocols = [
  { value: 'OAuth_v2.0', label: 'OAuth 2.0' }, { value: 'OAuth_v2.1', label: 'OAuth 2.1' },
  { value: 'OpenID_Connect_v1.0', label: 'OpenID Connect' }, { value: 'SAML_v2.0', label: 'SAML 2.0' },
  { value: 'CAS', label: 'CAS' }, { value: 'JWT', label: 'JWT' }, { value: 'Token_Based', label: 'Token Based' },
  { value: 'Form_Based', label: 'Form Based' }, { value: 'Extend_API', label: 'Extend API' }, { value: 'Basic', label: 'Basic' },
]

const commonFields: Field[] = [
  { key: 'appName', label: '应用名称', required: true }, { key: 'loginUrl', label: '登录地址' },
  { key: 'category', label: '应用分类' }, { key: 'vendor', label: '厂商' }, { key: 'vendorUrl', label: '厂商地址' },
  { key: 'logoutUrl', label: '退出地址' }, { key: 'logoutType', label: '退出类型' },
  { key: 'description', label: '说明', type: 'textarea' }, { key: 'iconBase64', label: '图标 Base64/URL', type: 'textarea' },
  { key: 'sortIndex', label: '排序', type: 'number' }, { key: 'visible', label: '可见性' }, { key: 'status', label: '状态', type: 'select', options: ['0', '1'] },
]

const protocolFields: Record<string, Field[]> = {
  oauth20: [
    { key: 'clientId', label: 'Client ID' }, { key: 'clientSecret', label: 'Client Secret' },
    { key: 'registeredRedirectUris', label: '回调地址', type: 'textarea' }, { key: 'scope', label: 'Scope' },
    { key: 'authorizedGrantTypes', label: '授权类型' }, { key: 'accessTokenValiditySeconds', label: '访问令牌有效期', type: 'number' },
    { key: 'refreshTokenValiditySeconds', label: '刷新令牌有效期', type: 'number' }, { key: 'approvalPrompt', label: '授权确认', type: 'select', options: ['force', 'auto'] },
    { key: 'pkce', label: 'PKCE' }, { key: 'issuer', label: 'Issuer' }, { key: 'audience', label: 'Audience' },
    { key: 'subject', label: 'Subject' }, { key: 'algorithm', label: '加密算法' }, { key: 'algorithmKey', label: '加密密钥', type: 'textarea' },
    { key: 'signature', label: '签名算法' }, { key: 'signatureKey', label: '签名密钥', type: 'textarea' },
  ],
  saml20: [
    { key: 'entityId', label: 'Entity ID' }, { key: 'spAcsUrl', label: 'ACS 地址' }, { key: 'issuer', label: 'Issuer' },
    { key: 'audience', label: 'Audience' }, { key: 'binding', label: 'Binding' }, { key: 'nameidFormat', label: 'NameID Format' },
    { key: 'nameIdConvert', label: 'NameID 转换' }, { key: 'signature', label: '签名算法' }, { key: 'digestMethod', label: '摘要算法' },
    { key: 'encrypted', label: '加密方式' }, { key: 'validityInterval', label: '有效期', type: 'number' },
    { key: 'certIssuer', label: '证书颁发者' }, { key: 'certSubject', label: '证书主题' }, { key: 'certExpiration', label: '证书有效期' },
  ],
  cas: [{ key: 'service', label: 'Service' }, { key: 'callbackUrl', label: '回调地址' }, { key: 'casUser', label: 'CAS 用户属性' }, { key: 'expires', label: '有效期', type: 'number' }],
  jwt: [
    { key: 'redirectUri', label: '回调地址' }, { key: 'jwtName', label: 'JWT 名称' }, { key: 'tokenType', label: 'Token 类型' },
    { key: 'issuer', label: 'Issuer' }, { key: 'audience', label: 'Audience' }, { key: 'subject', label: 'Subject' },
    { key: 'algorithm', label: '加密算法' }, { key: 'algorithmKey', label: '加密密钥', type: 'textarea' },
    { key: 'signature', label: '签名算法' }, { key: 'signatureKey', label: '签名密钥', type: 'textarea' }, { key: 'expires', label: '有效期', type: 'number' },
  ],
  tokenbased: [{ key: 'redirectUri', label: '回调地址' }, { key: 'tokenType', label: 'Token 类型' }, { key: 'cookieName', label: 'Cookie 名称' }, { key: 'algorithm', label: '算法' }, { key: 'algorithmKey', label: '算法密钥', type: 'textarea' }, { key: 'userPropertys', label: '用户属性' }, { key: 'expires', label: '有效期', type: 'number' }],
  formbased: [{ key: 'redirectUri', label: '回调地址' }, { key: 'usernameMapping', label: '用户名字段' }, { key: 'passwordMapping', label: '密码字段' }, { key: 'passwordAlgorithm', label: '密码算法' }, { key: 'authorizeView', label: '认证页面' }, { key: 'credential', label: '凭证模式' }, { key: 'sharedUsername', label: '共享账号' }, { key: 'sharedPassword', label: '共享密码' }],
  extendapi: [{ key: 'principal', label: '接口账号' }, { key: 'credentials', label: '接口凭证' }, { key: 'credential', label: '凭证模式' }, { key: 'systemUserAttr', label: '系统用户属性' }, { key: 'sharedUsername', label: '共享账号' }, { key: 'sharedPassword', label: '共享密码' }],
  basic: [],
}

const rows = ref<AppRow[]>([])
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const modalOpen = ref(false)
const editing = ref(false)
const search = reactive({ appName: '', protocol: '' })
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const form = reactive<AppRow>({})

function endpoint(protocol?: string) {
  if (['OAuth_v2.0', 'OAuth_v2.1', 'OpenID_Connect_v1.0'].includes(protocol || '')) return 'oauth20'
  return ({ 'SAML_v2.0': 'saml20', CAS: 'cas', JWT: 'jwt', Token_Based: 'tokenbased', Form_Based: 'formbased', Extend_API: 'extendapi', Basic: 'basic' } as Record<string, string>)[protocol || ''] || 'basic'
}

const fields = computed(() => [...commonFields, ...(protocolFields[endpoint(form.protocol)] || [])])

function normalizePage(data: unknown) {
  const value = (data || {}) as Record<string, unknown>
  const list = Array.isArray(value.rows) ? value.rows as AppRow[] : Array.isArray(data) ? data as AppRow[] : []
  return { rows: list, total: typeof value.records === 'number' ? value.records : list.length }
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await adminGet<unknown>('/apps/fetch', { ...search, pageNumber: page.current, pageSize: page.pageSize })
    const result = normalizePage(data); rows.value = result.rows; page.total = result.total
  } catch (err) { error.value = err instanceof Error ? err.message : '应用列表加载失败' } finally { loading.value = false }
}

async function openEditor(row?: AppRow, protocol?: string) {
  Object.keys(form).forEach(key => delete form[key])
  editing.value = Boolean(row?.id)
  const selectedProtocol = row?.protocol || protocol || 'Basic'
  const api = endpoint(selectedProtocol)
  try {
    const data = row?.id ? await adminGet<AppRow>(`${api === 'basic' ? '/apps' : `/apps/${api}`}/get/${row.id}`) : await adminGet<AppRow>(`${api === 'basic' ? '/apps' : `/apps/${api}`}/init`)
    Object.assign(form, data || {}, { protocol: row?.protocol || selectedProtocol })
    if (!row && api === 'oauth20') {
      form.clientId = form.clientId || form.id
      form.clientSecret = form.clientSecret || form.secret
    }
  } catch (err) {
    if (row) Object.assign(form, row)
    else { error.value = err instanceof Error ? err.message : '应用初始化失败'; return }
  }
  modalOpen.value = true
}

async function save() {
  if (!form.appName || !form.protocol) { message.warning('请填写应用名称并选择协议'); return }
  saving.value = true
  const api = endpoint(form.protocol)
  const base = api === 'basic' ? '/apps' : `/apps/${api}`
  try {
    if (editing.value) await adminPut(`${base}/update`, form)
    else await adminPost(`${base}/add`, form)
    message.success(editing.value ? '应用修改成功' : '应用新增成功'); modalOpen.value = false; await load()
  } catch (err) { message.error(err instanceof Error ? err.message : '应用保存失败') } finally { saving.value = false }
}

function remove(row: AppRow) {
  if (!row.id) return
  Modal.confirm({ title: `确认删除应用 ${row.appName || ''}？`, async onOk() { try { await adminDelete('/apps/delete', { ids: row.id }); message.success('应用已删除'); await load() } catch (err) { message.error(err instanceof Error ? err.message : '删除失败') } } })
}

function openPermission(row: AppRow, target: 'permission' | 'resources') {
  if (!row.id) return
  router.push({ path: `/admin/${target}`, query: { appId: row.id, appName: String(row.appName || '') } })
}

function onTableChange(pagination: { current?: number; pageSize?: number }) { page.current = pagination.current || 1; page.pageSize = pagination.pageSize || 10; load() }
function component(field: Field) { return field.type === 'textarea' ? 'a-textarea' : field.type === 'number' ? 'a-input-number' : field.type === 'select' ? 'a-select' : 'a-input' }
onMounted(load)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><div><h1>应用管理</h1><small>基础信息与协议配置统一维护</small></div><a-dropdown><a-button type="primary">新增应用</a-button><template #overlay><a-menu><a-menu-item v-for="item in protocols" :key="item.value" @click="openEditor(undefined, item.value)">{{ item.label }}</a-menu-item></a-menu></template></a-dropdown></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card class="admin-page-card"><a-form layout="inline" @submit.prevent="load"><a-form-item label="应用名称"><a-input v-model:value="search.appName" allow-clear /></a-form-item><a-form-item label="协议"><a-select v-model:value="search.protocol" allow-clear style="width: 200px"><a-select-option v-for="item in protocols" :key="item.value" :value="item.value">{{ item.label }}</a-select-option></a-select></a-form-item><a-button type="primary" html-type="submit">查询</a-button></a-form></a-card>
    <a-card><a-table :data-source="rows" :loading="loading" row-key="id" :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true }" @change="onTableChange"><a-table-column title="应用名称" data-index="appName" /><a-table-column title="协议" data-index="protocol" /><a-table-column title="分类" data-index="category" /><a-table-column title="登录地址" data-index="loginUrl" /><a-table-column title="状态" data-index="status" /><a-table-column title="操作" :width="300"><template #default="{ record }"><a-button type="link" @click="openEditor(record)">编辑</a-button><a-button type="link" @click="openPermission(record, 'resources')">资源</a-button><a-button type="link" @click="openPermission(record, 'permission')">用户组权限</a-button><a-button danger type="link" @click="remove(record)">删除</a-button></template></a-table-column></a-table></a-card>
    <a-modal v-model:open="modalOpen" :title="editing ? '编辑应用' : '新增应用'" width="920px" :confirm-loading="saving" @ok="save"><a-alert type="info" show-icon :message="`当前协议：${form.protocol || ''}`" /><a-form layout="vertical"><a-row :gutter="16"><a-col v-for="field in fields" :key="field.key" :xs="24" :md="field.type === 'textarea' ? 24 : 12"><a-form-item :label="field.label" :required="field.required"><component :is="component(field)" v-model:value="form[field.key]" :style="{ width: '100%' }"><template v-if="field.type === 'select'"><a-select-option v-for="option in field.options" :key="option" :value="field.key === 'status' ? Number(option) : option">{{ option }}</a-select-option></template></component></a-form-item></a-col></a-row></a-form></a-modal>
  </DefaultLayout>
</template>

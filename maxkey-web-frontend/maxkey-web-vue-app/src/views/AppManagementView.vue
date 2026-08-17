<script setup lang="ts">
import { CheckCircleFilled } from '@ant-design/icons-vue'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import ApplicationEditor from '../components/ApplicationEditor.vue'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost, adminPut } from '../api'

interface AppRow {
  id?: string
  appName?: string
  protocol?: string
  category?: string
  iconBase64?: string
  sortIndex?: number
  status?: number
  disabled?: boolean
  [key: string]: unknown
}

interface ProtocolOption {
  value: string
  label: string
  image: string
  descriptionKey: string
}

const { t } = useI18n({ useScope: 'global' })

const protocols = [
  { value: '', label: 'ALL' },
  { value: 'OAuth_v2.0', label: 'OAuth v2.0' },
  { value: 'OAuth_v2.1', label: 'OAuth v2.1' },
  { value: 'OpenID_Connect_v1.0', label: 'OpenID Connect v1.0' },
  { value: 'SAML_v2.0', label: 'SAML v2.0' },
  { value: 'CAS', label: 'CAS' },
  { value: 'JWT', label: 'JWT' },
  { value: 'Token_Based', label: 'Token Based' },
  { value: 'Form_Based', label: 'Form Based' },
  { value: 'Extend_API', label: 'Extend API' },
  { value: 'Basic', label: 'Basic' },
]

const standardProtocols: ProtocolOption[] = [
  { value: 'OAuth_v2.0', label: 'OAuth2.x', image: 'oauth2.png', descriptionKey: 'oauth2.0' },
  { value: 'OpenID_Connect_v1.0', label: 'OpenID Connect', image: 'oidc.png', descriptionKey: 'oidc' },
  { value: 'SAML_v2.0', label: 'SAML2.0', image: 'saml.jpg', descriptionKey: 'saml2.0' },
  { value: 'CAS', label: 'CAS认证', image: 'cas.png', descriptionKey: 'cas' },
  { value: 'JWT', label: 'JWT令牌', image: 'jwt.jpg', descriptionKey: 'jwt' },
]

const customProtocols: ProtocolOption[] = [
  { value: 'Token_Based', label: '令牌认证', image: 'token.png', descriptionKey: 'tokenbased' },
  { value: 'Extend_API', label: 'API扩展认证', image: 'api.png', descriptionKey: 'extendapi' },
  { value: 'Form_Based', label: '表单认证', image: 'form.png', descriptionKey: 'formbased' },
  { value: 'Basic', label: '基本登录', image: 'basic.png', descriptionKey: 'basic' },
]

const rows = ref<AppRow[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const protocolModalOpen = ref(false)
const editorModalOpen = ref(false)
const editing = ref(false)
const selectedRowKeys = ref<string[]>([])
const search = reactive({ appName: '', protocol: '' })
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const form = reactive<AppRow>({})

function endpoint(protocol?: string) {
  const value = String(protocol || '')
  if (['OAuth_v2.0', 'OAuth_v2.1', 'OpenID_Connect_v1.0', 'oauth20', 'oauth2', 'oidc'].includes(value)) return 'oauth20'
  return ({ 'SAML_v2.0': 'saml20', saml20: 'saml20', CAS: 'cas', cas: 'cas', JWT: 'jwt', jwt: 'jwt', Token_Based: 'tokenbased', tokenbased: 'tokenbased', Form_Based: 'formbased', formbased: 'formbased', Extend_API: 'extendapi', extendapi: 'extendapi', Basic: 'basic', basic: 'basic' } as Record<string, string>)[value] || 'basic'
}

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  getCheckboxProps: (record: AppRow) => ({ disabled: record.disabled }),
  onChange: (keys: Array<string | number>) => { selectedRowKeys.value = keys.map(String) },
}))

function normalizePage(data: unknown) {
  const value = (data || {}) as Record<string, unknown>
  const list = Array.isArray(value.rows) ? value.rows as AppRow[] : Array.isArray(data) ? data as AppRow[] : []
  return { rows: list, total: typeof value.records === 'number' ? value.records : list.length }
}

function categoryLabel(category?: string) {
  return t(`admin.mxk.apps.category.${category || 'none'}`)
}

function protocolDescription(item: ProtocolOption) {
  return t(`admin.mxk.apps.protocol.${item.descriptionKey}.discription`)
}

function protocolImage(image: string) {
  return `${import.meta.env.BASE_URL}assets/protocol/${image}`
}

async function load(resetPage = false) {
  if (resetPage) page.current = 1
  loading.value = true
  error.value = ''
  selectedRowKeys.value = []
  try {
    const data = await adminGet<unknown>('/apps/fetch', { ...search, pageNumber: page.current, pageSize: page.pageSize })
    const result = normalizePage(data)
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用列表加载失败'
  } finally {
    loading.value = false
  }
}

function openProtocolSelector() {
  protocolModalOpen.value = true
}

async function selectProtocol(protocol: string) {
  protocolModalOpen.value = false
  await nextTick()
  await openEditor(undefined, protocol)
}

async function openEditor(row?: AppRow, protocol?: string) {
  Object.keys(form).forEach(key => delete form[key])
  editing.value = Boolean(row?.id)
  const selectedProtocol = row?.protocol || protocol || 'Basic'
  const api = endpoint(selectedProtocol)
  try {
    const base = api === 'basic' ? '/apps' : `/apps/${api}`
    const data = row?.id
      ? await adminGet<AppRow>(`${base}/get/${row.id}`)
      : await adminGet<AppRow>(`${base}/init`)
    Object.assign(form, protocolDefaults(selectedProtocol), data || {}, { protocol: row?.protocol || selectedProtocol })
    if (api === 'saml20') form.fileType = 'certificate'
    if (api === 'oauth20') {
      form.select_scope = splitValues(form.scope)
      form.select_authorizedGrantTypes = splitValues(form.authorizedGrantTypes)
    }
    if (api === 'tokenbased') form.select_userPropertys = splitValues(form.userPropertys)
    if (!row && api === 'oauth20') {
      form.clientId = form.clientId || form.id
      form.clientSecret = form.clientSecret || form.secret
    }
  } catch (err) {
    if (row) Object.assign(form, row)
    else {
      message.error(err instanceof Error ? err.message : '应用初始化失败')
      return
    }
  }
  editorModalOpen.value = true
}

async function save() {
  if (!form.appName || !form.protocol) {
    message.warning('请填写应用名称并选择协议')
    return
  }
  saving.value = true
  const api = endpoint(form.protocol)
  const base = api === 'basic' ? '/apps' : `/apps/${api}`
  try {
    const payload = { ...form }
    if (api === 'oauth20') {
      payload.scope = joinValues(form.select_scope)
      payload.authorizedGrantTypes = joinValues(form.select_authorizedGrantTypes)
    }
    if (api === 'tokenbased') payload.userPropertys = joinValues(form.select_userPropertys)
    if (editing.value) await adminPut(`${base}/update`, payload)
    else await adminPost(`${base}/add`, payload)
    message.success(editing.value ? '应用修改成功' : '应用新增成功')
    editorModalOpen.value = false
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '应用保存失败')
  } finally {
    saving.value = false
  }
}

async function generateSecret() {
  try {
    const type = form.protocol === 'Token_Based' ? String(form.algorithm || 'base') : 'base'
    const data = await adminGet<unknown>(`/apps/generate/secret/${type}`, form.id ? { id: form.id } : undefined)
    const value = typeof data === 'string' ? data : (data as AppRow | undefined)?.secret
    if (value) {
      form.secret = value
      form.clientSecret = value
    }
    message.success('密钥已生成')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '密钥生成失败')
  }
}

async function generateKey(fieldKey: string, type?: string) {
  try {
    const secretType = type || String(form.algorithm || form.signature || 'base')
    const data = await adminGet<unknown>(`/apps/generate/secret/${secretType}`, form.id ? { id: form.id } : undefined)
    const value = typeof data === 'string' ? data : (data as AppRow | undefined)?.secret
    if (value) form[fieldKey] = value
    message.success('密钥已生成')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '密钥生成失败')
  }
}

function confirmDelete(ids: string[], appName = '') {
  if (!ids.length) return
  Modal.confirm({
    title: t('admin.mxk.text.delete.popconfirm.title'),
    content: appName,
    cancelText: t('admin.mxk.text.delete.popconfirm.cancelText'),
    okText: t('admin.mxk.text.delete.popconfirm.okText'),
    okType: 'danger',
    async onOk() {
      try {
        await adminDelete('/apps/delete', { ids: ids.join(',') })
        message.success(t('admin.mxk.alert.delete.success'))
        await load()
      } catch (err) {
        message.error(err instanceof Error ? err.message : t('admin.mxk.alert.delete.error'))
      }
    },
  })
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const sizeChanged = pagination.pageSize && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = sizeChanged ? 1 : pagination.current || 1
  void load()
}

function splitValues(value: unknown) {
  return String(value || '').split(',').map(item => item.trim()).filter(Boolean)
}

function joinValues(value: unknown) {
  return Array.isArray(value) ? value.filter(Boolean).join(',') : String(value || '')
}

function protocolDefaults(protocol: string): AppRow {
  const common: AppRow = { category: 'none', frequently: 'no', resourceMgt: 'false', visible: '0', isAdapter: '0', logoutType: '0', isExtendAttr: '0', status: 0 }
  if (['OAuth_v2.0', 'OAuth_v2.1', 'OpenID_Connect_v1.0'].includes(protocol)) return { ...common, select_authorizedGrantTypes: ['authorization_code'], select_scope: ['read'], pkce: 'no', approvalPrompt: 'auto', accessTokenValiditySeconds: '300', refreshTokenValiditySeconds: '300', subject: 'username' }
  if (protocol === 'SAML_v2.0') return { ...common, fileType: 'certificate', validityInterval: '300', nameidFormat: 'persistent', nameIdConvert: 'original', signature: 'RSAwithSHA1', digestMethod: 'SHA1', encrypted: 'no', binding: 'Redirect-Post' }
  if (protocol === 'CAS') return { ...common, expires: 300, casUser: 'username' }
  if (protocol === 'JWT') return { ...common, expires: 300, jwtName: 'jwt', subject: 'username', tokenType: 'POST' }
  if (protocol === 'Token_Based') return { ...common, expires: 300, tokenType: 'POST', cookieName: 'ltpa_token', algorithm: 'AES', select_userPropertys: [] }
  if (protocol === 'Form_Based') return { ...common, usernameMapping: 'username', passwordMapping: 'password', passwordAlgorithm: 'NONE' }
  return common
}

onMounted(() => load())
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>应用管理</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />

    <a-card :bordered="false" class="apps-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 24, lg: 24, xl: 48, xxl: 48 }" class="apps-search-row">
          <a-col :xs="24" :md="10">
            <a-form-item :label="t('admin.mxk.apps.name')">
              <a-input v-model:value="search.appName" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="10">
            <a-form-item label="协议">
              <a-select v-model:value="search.protocol">
                <a-select-option v-for="item in protocols" :key="item.value || 'all'" :value="item.value">{{ item.label }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="4">
            <a-form-item><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card class="apps-table-card">
      <div class="table-list-toolbar">
        <a-button type="primary" @click="openProtocolSelector">{{ t('admin.mxk.text.add') }}</a-button>
        <a-button type="primary" danger :disabled="selectedRowKeys.length === 0" @click="confirmDelete(selectedRowKeys)">{{ t('admin.mxk.text.batchDelete') }}</a-button>
      </div>
      <a-table
        bordered
        size="small"
        row-key="id"
        :data-source="rows"
        :loading="loading"
        :row-selection="rowSelection"
        :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, pageSizeOptions: ['10', '20', '50'], showSizeChanger: true }"
        :scroll="{ x: 1040 }"
        @change="onTableChange"
      >
        <a-table-column align="center" :title="t('admin.mxk.apps.icon')" :width="72">
          <template #default="{ record }"><img v-if="record.iconBase64" class="application-icon" :src="record.iconBase64" alt="" /></template>
        </a-table-column>
        <a-table-column :title="t('admin.mxk.text.id')" data-index="id" :width="250" />
        <a-table-column :title="t('admin.mxk.apps.name')" data-index="appName" :width="180" />
        <a-table-column title="协议" data-index="protocol" :width="170" />
        <a-table-column title="分类" :width="180">
          <template #default="{ record }">{{ categoryLabel(record.category) }}</template>
        </a-table-column>
        <a-table-column :title="t('admin.mxk.text.sortIndex')" data-index="sortIndex" :width="72" />
        <a-table-column align="center" title="状态" :width="72">
          <template #default="{ record }"><CheckCircleFilled v-if="Number(record.status) === 1" class="enabled-icon" /></template>
        </a-table-column>
        <a-table-column align="center" :title="t('admin.mxk.text.action')" :width="180" fixed="right">
          <template #default="{ record }">
            <a-space>
              <a-button @click="openEditor(record)">{{ t('admin.mxk.text.edit') }}</a-button>
              <a-button danger @click="confirmDelete([record.id], record.appName)">删除</a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-modal v-model:open="protocolModalOpen" :title="t('admin.mxk.text.select')" width="960px" :footer="null">
      <a-tabs type="card">
        <a-tab-pane key="standard" :tab="t('admin.mxk.apps.protocol.select.standard')">
          <a-table bordered :data-source="standardProtocols" :pagination="false" row-key="value" size="middle">
            <a-table-column align="center" :title="t('admin.mxk.apps.icon')" :width="90"><template #default="{ record }"><img class="protocol-icon" :src="protocolImage(record.image)" alt="" /></template></a-table-column>
            <a-table-column title="协议" data-index="label" :width="180" />
            <a-table-column :title="t('admin.mxk.text.description')"><template #default="{ record }">{{ protocolDescription(record) }}</template></a-table-column>
            <a-table-column :title="t('admin.mxk.text.action')" :width="120"><template #default="{ record }"><a-button type="primary" @click="selectProtocol(record.value)">{{ t('admin.mxk.text.add') }}</a-button></template></a-table-column>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="custom" :tab="t('admin.mxk.apps.protocol.select.custom')">
          <a-table bordered :data-source="customProtocols" :pagination="false" row-key="value" size="middle">
            <a-table-column align="center" :title="t('admin.mxk.apps.icon')" :width="90"><template #default="{ record }"><img class="protocol-icon" :src="protocolImage(record.image)" alt="" /></template></a-table-column>
            <a-table-column title="协议" data-index="label" :width="180" />
            <a-table-column :title="t('admin.mxk.text.description')"><template #default="{ record }">{{ protocolDescription(record) }}</template></a-table-column>
            <a-table-column :title="t('admin.mxk.text.action')" :width="120"><template #default="{ record }"><a-button type="primary" @click="selectProtocol(record.value)">{{ t('admin.mxk.text.add') }}</a-button></template></a-table-column>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <a-modal v-model:open="editorModalOpen" :title="editing ? '编辑' : '新增'" :width="editing ? '800px' : '960px'" :confirm-loading="saving" cancel-text="关闭" ok-text="提交" @ok="save">
      <ApplicationEditor :form="form" :editing="editing" @generate-secret="generateSecret" @generate-key="generateKey" />
    </a-modal>
  </DefaultLayout>
</template>

<style scoped>
.apps-search-card { margin-bottom: 24px; }
.apps-search-row { width: 100%; }
.apps-search-row :deep(.ant-form-item) { width: 100%; margin-bottom: 0; }
.apps-search-row :deep(.ant-form-item-control) { flex: 1; }
.apps-search-row :deep(.ant-select) { width: 100%; }
.table-list-toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.application-icon { display: block; width: auto; height: 30px; max-width: 48px; margin: 0 auto; object-fit: contain; }
.protocol-icon { display: block; width: auto; height: 40px; max-width: 60px; margin: 0 auto; object-fit: contain; }
.enabled-icon { color: green; font-size: 16px; }
@media (max-width: 768px) {
  .apps-search-row :deep(.ant-form-item) { margin-bottom: 16px; }
}
</style>

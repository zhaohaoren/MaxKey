<script setup lang="ts">
import { CheckCircleFilled } from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost, adminPut } from '../api'

interface DataRow {
  id?: string | number
  disabled?: boolean
  [key: string]: unknown
}

interface PageData {
  rows?: DataRow[]
  records?: number
  total?: number
}

interface ListColumn {
  key: string
  label: string
  align?: 'left' | 'center'
  width?: number
  kind?: 'status' | 'boolean' | 'image'
}

interface EditorField {
  key: string
  label: string
  type?: 'text' | 'password' | 'number' | 'select' | 'switch' | 'radio' | 'textarea'
  required?: boolean
  options?: Array<{ label: string; value: string | number }>
  disabledOnEdit?: boolean
  addonAfter?: string
  placeholder?: string
  textareaWhen?: (values: DataRow) => boolean
  visible?: (values: DataRow) => boolean
}

interface ListDefinition {
  title: string
  base: string
  searchKey: string
  searchLabel: string
  columns: ListColumn[]
  fields: EditorField[]
  defaults: DataRow
}

const { t } = useI18n({ useScope: 'global' })
const route = useRoute()
const rows = ref<DataRow[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const selectedRowKeys = ref<Array<string | number>>([])
const search = reactive<Record<string, string>>({})
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const editorOpen = ref(false)
const editing = ref(false)
const form = reactive<DataRow>({})
const mappingOpen = ref(false)
const mappingEditorOpen = ref(false)
const mappingRows = ref<DataRow[]>([])
const mappingSyncId = ref('')
const mappingForm = reactive<DataRow>({})

const required = true
const definitions: Record<string, ListDefinition> = {
  synchronizers: {
    title: 'admin.mxk.menu.config.synchronizers', base: '/config/synchronizers', searchKey: 'name', searchLabel: 'admin.mxk.synchronizers.name',
    columns: [
      { key: 'name', label: 'admin.mxk.synchronizers.name' },
      { key: 'scheduler', label: 'admin.mxk.synchronizers.scheduler' },
      { key: 'status', label: 'ui.status', kind: 'status', align: 'center', width: 80 },
    ],
    defaults: { status: true, sslSwitch: false },
    fields: [
      { key: 'name', label: 'admin.mxk.synchronizers.name' },
      { key: 'sourceType', label: 'admin.mxk.synchronizers.sourceType', type: 'select', disabledOnEdit: true, options: ['SCIMV20', 'API', 'MSAD', 'LDAP', 'DB'].map(value => ({ label: value, value })) },
      { key: 'service', label: 'admin.mxk.synchronizers.service', required },
      { key: 'scheduler', label: 'admin.mxk.synchronizers.scheduler' },
      { key: 'driverClass', label: 'admin.mxk.synchronizers.driverClass', required, visible: values => values.sourceType === 'DB' },
      { key: 'providerUrl', label: 'admin.mxk.synchronizers.providerUrl', required },
      { key: 'principal', label: 'admin.mxk.synchronizers.principal', required },
      { key: 'credentials', label: 'admin.mxk.synchronizers.credentials', type: 'password', required },
      { key: 'userBasedn', label: 'admin.mxk.synchronizers.userBasedn', required, visible: values => values.sourceType === 'LDAP' || values.sourceType === 'MSAD' },
      { key: 'userFilters', label: 'admin.mxk.synchronizers.userFilters', textareaWhen: values => values.sourceType === 'DB', visible: values => ['DB', 'LDAP', 'MSAD'].includes(String(values.sourceType)) },
      { key: 'orgBasedn', label: 'admin.mxk.synchronizers.orgBasedn', required, visible: values => values.sourceType === 'LDAP' },
      { key: 'orgFilters', label: 'admin.mxk.synchronizers.orgFilters', textareaWhen: values => values.sourceType === 'DB', visible: values => values.sourceType === 'DB' || values.sourceType === 'LDAP' },
      { key: 'msadDomain', label: 'admin.mxk.synchronizers.msadDomain', required, visible: values => values.sourceType === 'MSAD' },
      { key: 'sslSwitch', label: 'admin.mxk.synchronizers.sslSwitch', type: 'switch', visible: values => values.sourceType === 'LDAP' || values.sourceType === 'MSAD' },
      { key: 'trustStore', label: 'admin.mxk.synchronizers.trustStore', visible: values => Boolean(values.sslSwitch) && (values.sourceType === 'LDAP' || values.sourceType === 'MSAD') },
      { key: 'trustStorePassword', label: 'admin.mxk.synchronizers.trustStorePassword', visible: values => Boolean(values.sslSwitch) && (values.sourceType === 'LDAP' || values.sourceType === 'MSAD') },
      { key: 'syncStartTime', label: 'admin.mxk.synchronizers.syncStartTime', addonAfter: 'admin.mxk.text.day', visible: values => values.sourceType === 'DB' || values.sourceType === 'API' },
      { key: 'resumeTime', label: 'admin.mxk.synchronizers.resumeTime' },
      { key: 'suspendTime', label: 'admin.mxk.synchronizers.suspendTime' },
      { key: 'status', label: 'ui.status', type: 'switch', required },
    ],
  },
  connectors: {
    title: 'admin.mxk.menu.config.connectors', base: '/config/connectors', searchKey: 'connName', searchLabel: 'admin.mxk.connectors.connName',
    columns: [
      { key: 'connName', label: 'admin.mxk.connectors.connName' },
      { key: 'justInTime', label: 'admin.mxk.connectors.justInTime', kind: 'boolean', align: 'center' },
      { key: 'status', label: 'ui.status', kind: 'status', align: 'center', width: 80 },
    ],
    defaults: { justInTime: true, status: true },
    fields: [
      { key: 'id', label: 'admin.mxk.text.id' },
      { key: 'connName', label: 'admin.mxk.connectors.connName' },
      { key: 'justInTime', label: 'admin.mxk.connectors.justInTime', type: 'switch', required },
      { key: 'scheduler', label: 'admin.mxk.connectors.scheduler', placeholder: '0 0 12 * * ?', visible: values => !values.justInTime },
      { key: 'providerUrl', label: 'admin.mxk.connectors.providerUrl', required },
      { key: 'principal', label: 'admin.mxk.connectors.principal', required },
      { key: 'credentials', label: 'admin.mxk.connectors.credentials', type: 'password', required },
      { key: 'status', label: 'ui.status', type: 'switch', required },
    ],
  },
  socialsprovider: {
    title: 'admin.mxk.menu.config.socialsproviders', base: '/config/socialsprovider', searchKey: 'providerName', searchLabel: 'admin.mxk.socialsproviders.providerName',
    columns: [
      { key: 'icon', label: 'admin.mxk.socialsproviders.icon', kind: 'image', align: 'center', width: 76 },
      { key: 'provider', label: 'admin.mxk.socialsproviders.provider' },
      { key: 'providerName', label: 'admin.mxk.socialsproviders.providerName' },
      { key: 'sortIndex', label: 'admin.mxk.text.sortIndex', align: 'center', width: 80 },
      { key: 'display', label: 'admin.mxk.socialsproviders.display', kind: 'status', align: 'center', width: 90 },
      { key: 'scanCode', label: 'admin.mxk.socialsproviders.scanCode', kind: 'status', align: 'center', width: 90 },
      { key: 'status', label: 'ui.status', kind: 'status', align: 'center', width: 80 },
    ],
    defaults: { status: true, scanCode: 'none', sortIndex: 1 },
    fields: [
      { key: 'id', label: 'admin.mxk.text.id' },
      { key: 'icon', label: 'admin.mxk.socialsproviders.icon', required },
      { key: 'provider', label: 'admin.mxk.socialsproviders.provider', required },
      { key: 'providerName', label: 'admin.mxk.socialsproviders.providerName', required },
      { key: 'clientId', label: 'admin.mxk.socialsproviders.clientId', required },
      { key: 'clientSecret', label: 'admin.mxk.socialsproviders.clientSecret', type: 'password', required },
      { key: 'agentId', label: 'admin.mxk.socialsproviders.agentId' },
      { key: 'scanCode', label: 'admin.mxk.socialsproviders.scanCode', type: 'radio', required, options: [{ label: 'admin.mxk.text.no', value: 'false' }, { label: 'admin.mxk.text.yes', value: 'true' }] },
      { key: 'display', label: 'admin.mxk.socialsproviders.display', type: 'radio', options: [{ label: 'admin.mxk.text.no', value: 'false' }, { label: 'admin.mxk.text.yes', value: 'true' }] },
      { key: 'sortIndex', label: 'admin.mxk.text.sortIndex', type: 'number', required },
      { key: 'status', label: 'ui.status', type: 'switch', required },
    ],
  },
}

const definition = computed(() => definitions[String(route.meta.configPage || route.params.resource || 'synchronizers')] || definitions.synchronizers)
const visibleFields = computed(() => definition.value.fields.filter(field => !field.visible || field.visible(form)))
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  getCheckboxProps: (record: DataRow) => ({ disabled: record.disabled }),
  onChange: (keys: Array<string | number>) => { selectedRowKeys.value = keys },
}))

function label(value: string) {
  return value.startsWith('admin.') || value.startsWith('ui.') ? t(value) : value
}

function normalizePage(data: unknown) {
  if (Array.isArray(data)) return { rows: data as DataRow[], total: data.length }
  const value = (data || {}) as PageData
  const list = Array.isArray(value.rows) ? value.rows : []
  return { rows: list, total: typeof value.records === 'number' ? value.records : typeof value.total === 'number' ? value.total : list.length }
}

function isEnabled(value: unknown) {
  return value === 1 || value === '1' || value === true || value === 'true'
}

async function load(resetPage = false) {
  if (resetPage) page.current = 1
  loading.value = true
  error.value = ''
  selectedRowKeys.value = []
  try {
    const params: Record<string, unknown> = { pageNumber: page.current, pageSize: page.pageSize }
    const searchValue = search[definition.value.searchKey]
    if (searchValue) params[definition.value.searchKey] = searchValue
    const result = normalizePage(await adminGet(`${definition.value.base}/fetch`, params))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '列表加载失败'
  } finally {
    loading.value = false
  }
}

function resetState() {
  Object.keys(search).forEach(key => delete search[key])
  rows.value = []
  page.current = 1
  page.total = 0
  editorOpen.value = false
  mappingOpen.value = false
  void load()
}

async function openEditor(row?: DataRow) {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, definition.value.defaults)
  editing.value = Boolean(row?.id)
  if (row?.id) {
    try {
      Object.assign(form, await adminGet<DataRow>(`${definition.value.base}/get/${row.id}`))
    } catch (err) {
      message.error(err instanceof Error ? err.message : '详情加载失败')
      return
    }
  }
  form.status = isEnabled(form.status)
  if (definition.value === definitions.synchronizers) form.sslSwitch = isEnabled(form.sslSwitch)
  if (definition.value === definitions.connectors) form.justInTime = isEnabled(form.justInTime)
  editorOpen.value = true
}

function validateEditor() {
  const missing = visibleFields.value.find(field => field.required && field.type !== 'switch' && (form[field.key] === undefined || form[field.key] === null || form[field.key] === ''))
  if (!missing) return true
  message.warning(`请填写${label(missing.label)}`)
  return false
}

function editorPayload() {
  const data = { ...form }
  data.status = form.status ? 1 : 0
  if (definition.value === definitions.synchronizers) data.sslSwitch = form.sslSwitch ? 1 : 0
  if (definition.value === definitions.connectors) data.justInTime = form.justInTime ? 1 : 0
  return data
}

async function saveEditor() {
  if (!validateEditor()) return
  saving.value = true
  try {
    if (editing.value) await adminPut(`${definition.value.base}/update`, editorPayload())
    else await adminPost(`${definition.value.base}/add`, editorPayload())
    message.success(t(editing.value ? 'admin.mxk.alert.update.success' : 'admin.mxk.alert.add.success'))
    editorOpen.value = false
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : (editing.value ? t('admin.mxk.alert.update.error') : t('admin.mxk.alert.add.error')))
  } finally {
    saving.value = false
  }
}

function confirmDelete(ids: Array<string | number>) {
  if (!ids.length) return
  Modal.confirm({
    title: t('admin.mxk.text.delete.popconfirm.title'),
    cancelText: t('admin.mxk.text.delete.popconfirm.cancelText'),
    okText: t('admin.mxk.text.delete.popconfirm.okText'),
    okType: 'danger',
    async onOk() {
      try {
        await adminDelete(`${definition.value.base}/delete`, { ids: ids.join(',') })
        message.success(t('admin.mxk.alert.delete.success'))
        await load()
      } catch (err) {
        message.error(err instanceof Error ? err.message : t('admin.mxk.alert.delete.error'))
      }
    },
  })
}

async function runSynchronizer(row: DataRow) {
  if (!row.id) return
  try {
    await adminGet('/config/synchronizers/synchr', { id: row.id })
    message.success(t('admin.mxk.alert.operate.success'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('admin.mxk.alert.operate.error'))
  }
}

async function loadMappings() {
  try {
    const data = await adminGet<unknown>(`/config/synchronizers/mapping-list/${mappingSyncId.value}`)
    mappingRows.value = Array.isArray(data) ? data as DataRow[] : []
  } catch (err) {
    mappingRows.value = []
    message.error(err instanceof Error ? err.message : '字段映射加载失败')
  }
}

async function openMappings(row: DataRow) {
  if (!row.id) return
  mappingSyncId.value = String(row.id)
  mappingOpen.value = true
  await loadMappings()
}

async function openMappingEditor(row?: DataRow) {
  Object.keys(mappingForm).forEach(key => delete mappingForm[key])
  Object.assign(mappingForm, { syncId: mappingSyncId.value, objectType: '1' })
  if (row?.id) {
    try {
      Object.assign(mappingForm, await adminGet<DataRow>(`/config/synchronizers/mapping-get/${row.id}`))
    } catch (err) {
      message.error(err instanceof Error ? err.message : '字段映射详情加载失败')
      return
    }
  }
  mappingEditorOpen.value = true
}

async function saveMapping() {
  if (!mappingForm.sourceField || !mappingForm.targetField) {
    message.warning('请填写源字段和目标字段')
    return
  }
  saving.value = true
  try {
    mappingForm.syncId = mappingSyncId.value
    if (mappingForm.id) await adminPut('/config/synchronizers/mapping-update', mappingForm)
    else await adminPost('/config/synchronizers/mapping-add', mappingForm)
    message.success(t(mappingForm.id ? 'admin.mxk.alert.update.success' : 'admin.mxk.alert.add.success'))
    mappingEditorOpen.value = false
    await loadMappings()
  } catch (err) {
    message.error(err instanceof Error ? err.message : '字段映射保存失败')
  } finally {
    saving.value = false
  }
}

function deleteMapping(row: DataRow) {
  if (!row.id) return
  Modal.confirm({
    title: t('admin.mxk.text.delete.popconfirm.title'),
    async onOk() {
      try {
        await adminGet(`/config/synchronizers/mapping-delete/${row.id}`)
        message.success(t('admin.mxk.alert.delete.success'))
        await loadMappings()
      } catch (err) {
        message.error(err instanceof Error ? err.message : t('admin.mxk.alert.delete.error'))
      }
    },
  })
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const sizeChanged = pagination.pageSize !== undefined && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = sizeChanged ? 1 : pagination.current || 1
  void load()
}

watch(() => route.fullPath, resetState)
onMounted(load)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t(definition.title) }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />

    <a-card :bordered="false" class="legacy-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }" class="legacy-search-row">
          <a-col :xs="24" :md="16">
            <a-form-item :label="label(definition.searchLabel)">
              <a-input v-model:value="search[definition.searchKey]" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8"><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card class="legacy-table-card">
      <div class="table-list-toolbar">
        <a-button type="primary" @click="openEditor()">{{ t('admin.mxk.text.add') }}</a-button>
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
        :scroll="{ x: definition === definitions.socialsprovider ? 1000 : 760 }"
        @change="onTableChange"
      >
        <a-table-column v-for="column in definition.columns" :key="column.key" :title="label(column.label)" :align="column.align || 'left'" :width="column.width">
          <template #default="{ record }">
            <img v-if="column.kind === 'image' && record[column.key]" class="provider-icon" :src="String(record[column.key])" alt="" />
            <template v-else-if="column.kind === 'status'"><CheckCircleFilled v-if="isEnabled(record[column.key])" class="enabled-icon" /></template>
            <template v-else-if="column.kind === 'boolean'">{{ t(isEnabled(record[column.key]) ? 'admin.mxk.text.yes' : 'admin.mxk.text.no') }}</template>
            <template v-else>{{ record[column.key] }}</template>
          </template>
        </a-table-column>
        <a-table-column align="center" :title="t('admin.mxk.text.action')" :width="definition === definitions.synchronizers ? 260 : 160">
          <template #default="{ record }">
            <div class="action-buttons">
              <a-button v-if="definition === definitions.synchronizers" @click="runSynchronizer(record)">{{ t('admin.mxk.text.synchr') }}</a-button>
              <a-button v-if="definition === definitions.synchronizers" @click="openMappings(record)">{{ t('admin.mxk.text.mapping') }}</a-button>
              <a-button @click="openEditor(record)">{{ t('admin.mxk.text.edit') }}</a-button>
              <a-button danger @click="confirmDelete([record.id])">{{ t('ui.delete') }}</a-button>
            </div>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-modal v-model:open="editorOpen" :title="t(editing ? 'admin.mxk.text.edit' : 'admin.mxk.text.add')" width="700px" :confirm-loading="saving" @ok="saveEditor">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item v-for="field in visibleFields" :key="field.key" :label="label(field.label)" :required="field.required">
          <a-switch v-if="field.type === 'switch'" v-model:checked="form[field.key]" checked-children="✓" un-checked-children="×" />
          <a-select v-else-if="field.type === 'select'" v-model:value="form[field.key]" :disabled="editing && field.disabledOnEdit">
            <a-select-option v-for="option in field.options" :key="option.value" :value="option.value">{{ label(option.label) }}</a-select-option>
          </a-select>
          <a-radio-group v-else-if="field.type === 'radio'" v-model:value="form[field.key]" button-style="solid">
            <a-radio-button v-for="option in field.options" :key="option.value" :value="option.value">{{ label(option.label) }}</a-radio-button>
          </a-radio-group>
          <a-input-number v-else-if="field.type === 'number'" v-model:value="form[field.key]" class="editor-number" />
          <a-textarea v-else-if="field.type === 'textarea' || field.textareaWhen?.(form)" v-model:value="form[field.key]" :rows="3" />
          <a-input-password v-else-if="field.type === 'password'" v-model:value="form[field.key]" />
          <a-input v-else v-model:value="form[field.key]" :disabled="field.key === 'id' && editing" :placeholder="field.placeholder" :addon-after="field.addonAfter ? label(field.addonAfter) : undefined" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="mappingOpen" :title="t('admin.mxk.text.mapping')" width="880px" @ok="mappingOpen = false">
      <div class="table-list-toolbar"><a-button type="primary" @click="openMappingEditor()">{{ t('admin.mxk.text.add') }}</a-button></div>
      <a-table :data-source="mappingRows" row-key="id" :pagination="false" size="small">
        <a-table-column :title="t('admin.mxk.job.mapping.sourceField')" data-index="sourceField" />
        <a-table-column :title="t('admin.mxk.job.mapping.targetField')" data-index="targetField" />
        <a-table-column :title="t('admin.mxk.job.mapping.description')" data-index="description" />
        <a-table-column align="center" :title="t('admin.mxk.job.mapping.objectType')">
          <template #default="{ record }">{{ t(record.objectType === '1' ? 'admin.mxk.menu.identities.users' : 'admin.mxk.menu.identities.organizations') }}</template>
        </a-table-column>
        <a-table-column align="center" :title="t('admin.mxk.text.action')" :width="180">
          <template #default="{ record }"><div class="action-buttons"><a-button @click="openMappingEditor(record)">{{ t('admin.mxk.text.edit') }}</a-button><a-button danger @click="deleteMapping(record)">{{ t('ui.delete') }}</a-button></div></template>
        </a-table-column>
      </a-table>
    </a-modal>

    <a-modal v-model:open="mappingEditorOpen" :title="t(mappingForm.id ? 'admin.mxk.text.edit' : 'admin.mxk.text.add')" width="620px" :confirm-loading="saving" @ok="saveMapping">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="t('admin.mxk.job.mapping.sourceField')"><a-input v-model:value="mappingForm.sourceField" /></a-form-item>
        <a-form-item :label="t('admin.mxk.job.mapping.targetField')"><a-input v-model:value="mappingForm.targetField" /></a-form-item>
        <a-form-item :label="t('admin.mxk.job.mapping.description')"><a-input v-model:value="mappingForm.description" /></a-form-item>
        <a-form-item :label="t('admin.mxk.job.mapping.objectType')">
          <a-select v-model:value="mappingForm.objectType"><a-select-option value="1">{{ t('admin.mxk.menu.identities.users') }}</a-select-option><a-select-option value="2">{{ t('admin.mxk.menu.identities.organizations') }}</a-select-option></a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </DefaultLayout>
</template>

<style scoped>
.legacy-search-card { margin-bottom: 24px; }
.legacy-search-row, .legacy-search-row :deep(.ant-form-item) { width: 100%; }
.legacy-table-card :deep(.ant-card-body) { padding: 24px; }
.table-list-toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.action-buttons { display: flex; align-items: center; justify-content: center; gap: 8px; white-space: nowrap; }
.action-buttons :deep(.ant-btn) { flex: 0 0 auto; }
.enabled-icon { color: green; }
.provider-icon { display: block; width: 32px; height: 32px; margin: 0 auto; object-fit: contain; }
.editor-number { width: 100%; }
</style>

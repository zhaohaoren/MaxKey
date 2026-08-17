<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet, get } from '../api'
import { normalizeIdentityPage, type IdentityRow } from '../identity'

interface AuditField {
  key: string
  labelKey: string
}

interface AuditColumn extends AuditField {
  portalLabelKey?: string
}

interface AuditConfig {
  titleKey: string
  endpoint: string
  search: AuditField[]
  portalSearch?: AuditField[]
  columns: AuditColumn[]
  scrollX: number
}

const configs: Record<string, AuditConfig> = {
  logins: {
    titleKey: 'mxk.menu.audit.logins',
    endpoint: '/historys/loginHistory/fetch',
    search: [
      { key: 'username', labelKey: 'mxk.users.username' },
      { key: 'displayName', labelKey: 'mxk.users.displayName' },
    ],
    portalSearch: [],
    columns: [
      { key: 'sessionId', labelKey: 'mxk.history.login.sessionId' },
      { key: 'username', labelKey: 'mxk.history.login.username' },
      { key: 'displayName', labelKey: 'mxk.history.login.displayName' },
      { key: 'message', labelKey: 'mxk.history.login.message' },
      { key: 'loginType', labelKey: 'mxk.history.login.loginType' },
      { key: 'sourceIp', labelKey: 'mxk.history.login.sourceIp' },
      { key: 'location', labelKey: 'mxk.history.login.location' },
      { key: 'browser', labelKey: 'mxk.history.login.browser' },
      { key: 'platform', labelKey: 'mxk.history.login.platform' },
      { key: 'loginTime', labelKey: 'mxk.history.login.loginTime' },
      { key: 'logoutTime', labelKey: 'mxk.history.login.logoutTime' },
    ],
    scrollX: 1500,
  },
  apps: {
    titleKey: 'mxk.menu.audit.loginapps',
    endpoint: '/historys/loginAppsHistory/fetch',
    search: [
      { key: 'username', labelKey: 'mxk.users.username' },
      { key: 'displayName', labelKey: 'mxk.users.displayName' },
    ],
    portalSearch: [],
    columns: [
      { key: 'sessionId', labelKey: 'mxk.history.loginapps.sessionId' },
      { key: 'username', labelKey: 'mxk.history.loginapps.username' },
      { key: 'displayName', labelKey: 'mxk.history.loginapps.displayName' },
      { key: 'appName', labelKey: 'mxk.history.loginapps.appName' },
      { key: 'loginTime', labelKey: 'mxk.history.loginapps.loginTime' },
    ],
    scrollX: 900,
  },
  synchronizers: {
    titleKey: 'mxk.menu.audit.synchronizer',
    endpoint: '/historys/synchronizerHistory/fetch',
    search: [
      { key: 'syncName', labelKey: 'mxk.history.synchronizer.syncName' },
      { key: 'objectName', labelKey: 'mxk.history.synchronizer.objectName' },
      { key: 'employeeNumber', labelKey: 'mxk.users.employeeNumber' },
    ],
    columns: [
      { key: 'syncId', labelKey: 'mxk.history.synchronizer.syncId' },
      { key: 'syncName', labelKey: 'mxk.history.synchronizer.syncName' },
      { key: 'objectId', labelKey: 'mxk.history.synchronizer.objectId' },
      { key: 'objectType', labelKey: 'mxk.history.synchronizer.objectType' },
      { key: 'objectName', labelKey: 'mxk.history.synchronizer.objectName' },
      { key: 'syncTime', labelKey: 'mxk.history.synchronizer.syncTime' },
      { key: 'result', labelKey: 'mxk.history.synchronizer.result' },
    ],
    scrollX: 1100,
  },
  connectors: {
    titleKey: 'mxk.menu.audit.connector',
    endpoint: '/historys/connectorHistory/fetch',
    search: [
      { key: 'conName', labelKey: 'mxk.history.connector.conName' },
      { key: 'sourceName', labelKey: 'mxk.history.connector.sourceName' },
      { key: 'employeeNumber', labelKey: 'mxk.users.employeeNumber' },
    ],
    columns: [
      { key: 'id', labelKey: 'mxk.history.connector.id' },
      { key: 'conName', labelKey: 'mxk.history.connector.conName' },
      { key: 'topic', labelKey: 'mxk.history.connector.topic' },
      { key: 'actionType', labelKey: 'mxk.history.connector.actionType' },
      { key: 'sourceId', labelKey: 'mxk.history.connector.sourceId' },
      { key: 'sourceName', labelKey: 'mxk.history.connector.sourceName' },
      { key: 'syncTime', labelKey: 'mxk.history.connector.syncTime' },
      { key: 'result', labelKey: 'mxk.history.connector.result' },
    ],
    scrollX: 1300,
  },
  systems: {
    titleKey: 'mxk.menu.audit.operate',
    endpoint: '/historys/systemLogs/fetch',
    search: [
      { key: 'username', labelKey: 'mxk.users.username' },
      { key: 'displayName', labelKey: 'mxk.users.displayName' },
      { key: 'employeeNumber', labelKey: 'mxk.users.employeeNumber' },
    ],
    columns: [
      { key: 'topic', labelKey: 'mxk.history.systemlogs.topic' },
      { key: 'message', labelKey: 'mxk.history.systemlogs.message' },
      { key: 'messageAction', labelKey: 'mxk.history.systemlogs.messageAction', portalLabelKey: 'mxk.history.systemlogs.messageType' },
      { key: 'messageResult', labelKey: 'mxk.history.systemlogs.messageResult' },
      { key: 'displayName', labelKey: 'mxk.history.systemlogs.displayName' },
      { key: 'executeTime', labelKey: 'mxk.history.systemlogs.executeTime' },
    ],
    scrollX: 1000,
  },
}

const { t } = useI18n({ useScope: 'global' })
const route = useRoute()
const mode = computed<'portal' | 'admin'>(() => route.path.startsWith('/admin/') ? 'admin' : 'portal')
const auditKey = computed(() => String(route.params.audit || route.meta.audit || 'logins'))
const config = computed(() => configs[auditKey.value] || configs.logins)
const localePrefix = computed(() => mode.value === 'admin' ? 'admin' : 'portal')
const searchFields = computed(() => mode.value === 'portal' ? config.value.portalSearch ?? config.value.search : config.value.search)
const rows = ref<IdentityRow[]>([])
const loading = ref(false)
const error = ref('')
const filters = reactive<Record<string, unknown>>({
  username: '',
  displayName: '',
  employeeNumber: '',
  syncName: '',
  objectName: '',
  conName: '',
  sourceName: '',
  startDate: null,
  endDate: null,
})
const page = reactive({ current: 1, pageSize: 10, total: 0 })
let loadSequence = 0

function label(key: string) {
  return t(`${localePrefix.value}.${key}`)
}

function columnLabel(column: AuditColumn) {
  const key = mode.value === 'portal' && column.portalLabelKey ? column.portalLabelKey : column.labelKey
  return label(key)
}

function formatDate(value: unknown) {
  if (!value || typeof value !== 'object' || !('format' in value)) return ''
  return (value as { format: (pattern: string) => string }).format('YYYY-MM-DD HH:mm:ss')
}

async function load(resetPage = false) {
  if (resetPage) page.current = 1
  const sequence = ++loadSequence
  loading.value = true
  error.value = ''
  const params: Record<string, unknown> = {
    pageNumber: page.current,
    pageSize: page.pageSize,
    startDate: formatDate(filters.startDate),
    endDate: formatDate(filters.endDate),
  }
  searchFields.value.forEach(field => { params[field.key] = filters[field.key] })

  try {
    const data = mode.value === 'admin'
      ? await adminGet<unknown>(config.value.endpoint, params)
      : await get<unknown>(config.value.endpoint, params)
    if (sequence !== loadSequence) return
    const result = normalizeIdentityPage(data)
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    if (sequence !== loadSequence) return
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : t('ui.auditLoadError')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function resetSearch() {
  Object.keys(filters).forEach(key => { filters[key] = key === 'startDate' || key === 'endDate' ? null : '' })
  void load(true)
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const pageSizeChanged = pagination.pageSize !== undefined && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = pageSizeChanged ? 1 : pagination.current || 1
  void load()
}

function rowKey(row: IdentityRow) {
  return String(row.id || row.sessionId || `${page.current}-${rows.value.indexOf(row)}`)
}

watch([auditKey, mode], () => {
  Object.keys(filters).forEach(key => { filters[key] = key === 'startDate' || key === 'endDate' ? null : '' })
  page.current = 1
  void load()
}, { immediate: true })
</script>

<template>
  <DefaultLayout :mode="mode">
    <div class="alain-default__content-title"><h1>{{ label(config.titleKey) }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />

    <a-card :bordered="false" class="audit-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }" class="audit-search-row">
          <a-col v-for="field in searchFields" :key="field.key" :xs="24" :md="8">
            <a-form-item :label="label(field.labelKey)">
              <a-input v-model:value="filters[field.key]" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :span="24" class="audit-time-filter-row">
            <a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }">
              <a-col :xs="24" :md="8">
                <a-form-item :label="label('mxk.text.startDate')">
                  <a-date-picker v-model:value="filters.startDate" show-time format="YYYY-MM-DD HH:mm:ss" />
                </a-form-item>
              </a-col>
              <a-col :xs="24" :md="8">
                <a-form-item :label="label('mxk.text.endDate')">
                  <a-date-picker v-model:value="filters.endDate" show-time format="YYYY-MM-DD HH:mm:ss" />
                </a-form-item>
              </a-col>
            </a-row>
          </a-col>
          <a-col :xs="24" :md="mode === 'portal' && searchFields.length === 0 ? 8 : 24" class="audit-search-actions">
            <a-space>
              <a-button type="primary" html-type="submit" :loading="loading">{{ label('mxk.text.query') }}</a-button>
              <a-button @click="resetSearch">{{ label('mxk.text.reset') }}</a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card class="audit-table-card">
      <a-table
        size="small"
        bordered
        :loading="loading"
        :data-source="rows"
        :row-key="rowKey"
        :pagination="{
          current: page.current,
          pageSize: page.pageSize,
          total: page.total,
          showSizeChanger: true,
          pageSizeOptions: mode === 'admin' ? ['10', '20', '50', '100'] : ['10', '20', '50'],
        }"
        :scroll="{ x: config.scrollX }"
        @change="onTableChange"
      >
        <a-table-column
          v-for="column in config.columns"
          :key="column.key"
          :data-index="column.key"
          :title="columnLabel(column)"
        />
      </a-table>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.audit-search-card {
  margin-bottom: 24px;
}

.audit-search-row {
  width: 100%;
}

.audit-search-row :deep(.ant-form-item) {
  width: 100%;
  margin-right: 0;
}

.audit-search-row :deep(.ant-form-item-control) {
  min-width: 0;
  flex: 1;
}

.audit-search-row :deep(.ant-picker) {
  width: 100%;
}

.audit-time-filter-row {
  margin-top: 8px;
}

.audit-search-actions {
  margin-top: 8px;
  text-align: right;
}

.audit-table-card :deep(.ant-card-body) {
  padding: 24px;
}

.audit-table-card :deep(.ant-table-thead > tr > th) {
  text-align: center;
}

@media (max-width: 767px) {
  .audit-search-actions {
    text-align: left;
  }
}
</style>

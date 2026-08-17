<script setup lang="ts">
import { CheckCircleFilled } from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet } from '../api'

interface AppRow {
  id?: string
  appName?: string
  protocol?: string
  category?: string
  iconBase64?: string
  sortIndex?: number
  status?: number
  disabled?: boolean
}

const { t } = useI18n({ useScope: 'global' })
const router = useRouter()
const protocols = [
  { value: '', label: 'ALL' }, { value: 'OAuth_v2.0', label: 'OAuth v2.0' },
  { value: 'OAuth_v2.1', label: 'OAuth v2.1' }, { value: 'OpenID_Connect_v1.0', label: 'OpenID Connect v1.0' },
  { value: 'SAML_v2.0', label: 'SAML v2.0' }, { value: 'CAS', label: 'CAS' },
  { value: 'JWT', label: 'JWT' }, { value: 'Token_Based', label: 'Token Based' },
  { value: 'Form_Based', label: 'Form Based' }, { value: 'Extend_API', label: 'Extend API' }, { value: 'Basic', label: 'Basic' },
]
const rows = ref<AppRow[]>([])
const loading = ref(false)
const error = ref('')
const selectedRowKeys = ref<string[]>([])
const search = reactive({ appName: '', protocol: '', resourceMgt: 'y' })
const page = reactive({ current: 1, pageSize: 10, total: 0 })
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

async function load(resetPage = false) {
  if (resetPage) page.current = 1
  loading.value = true
  error.value = ''
  selectedRowKeys.value = []
  try {
    const result = normalizePage(await adminGet<unknown>('/apps/fetch', { ...search, pageNumber: page.current, pageSize: page.pageSize }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    error.value = err instanceof Error ? err.message : '应用列表加载失败'
  } finally {
    loading.value = false
  }
}

function categoryLabel(category?: string) {
  return t(`admin.mxk.apps.category.${category || 'none'}`)
}

function openTarget(row: AppRow, target: 'permission' | 'roles' | 'resources') {
  if (!row.id) return
  const path = target === 'permission' ? '/admin/permission' : target === 'roles' ? '/admin/roles' : '/admin/resources'
  void router.push({ path, query: { appId: row.id, appName: row.appName || '' } })
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const sizeChanged = pagination.pageSize && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = sizeChanged ? 1 : pagination.current || 1
  void load()
}

onMounted(() => load())
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>应用权限</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="apps-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 24, lg: 24, xl: 48, xxl: 48 }" class="apps-search-row">
          <a-col :xs="24" :md="10"><a-form-item :label="t('admin.mxk.apps.name')"><a-input v-model:value="search.appName" /></a-form-item></a-col>
          <a-col :xs="24" :md="10"><a-form-item label="协议"><a-select v-model:value="search.protocol"><a-select-option v-for="item in protocols" :key="item.value || 'all'" :value="item.value">{{ item.label }}</a-select-option></a-select></a-form-item></a-col>
          <a-col :xs="24" :md="4"><a-form-item><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-card>
    <a-card>
      <a-table bordered size="small" row-key="id" :data-source="rows" :loading="loading" :row-selection="rowSelection" :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, pageSizeOptions: ['10', '20', '50'], showSizeChanger: true }" :scroll="{ x: 1120 }" @change="onTableChange">
        <a-table-column align="center" :title="t('admin.mxk.apps.icon')" :width="72"><template #default="{ record }"><img v-if="record.iconBase64" class="application-icon" :src="record.iconBase64" alt="" /></template></a-table-column>
        <a-table-column :title="t('admin.mxk.text.id')" data-index="id" :width="250" />
        <a-table-column :title="t('admin.mxk.apps.name')" data-index="appName" :width="180" />
        <a-table-column title="协议" data-index="protocol" :width="170" />
        <a-table-column title="分类" :width="180"><template #default="{ record }">{{ categoryLabel(record.category) }}</template></a-table-column>
        <a-table-column :title="t('admin.mxk.text.sortIndex')" data-index="sortIndex" :width="72" />
        <a-table-column align="center" title="状态" :width="72"><template #default="{ record }"><CheckCircleFilled v-if="Number(record.status) === 1" class="enabled-icon" /></template></a-table-column>
        <a-table-column align="center" :title="t('admin.mxk.text.action')" :width="270" fixed="right">
          <template #default="{ record }"><a-space><a-button @click="openTarget(record, 'permission')">{{ t('admin.mxk.apps.permission') }}</a-button><a-button @click="openTarget(record, 'roles')">{{ t('admin.mxk.apps.role') }}</a-button><a-button @click="openTarget(record, 'resources')">{{ t('admin.mxk.apps.resources') }}</a-button></a-space></template>
        </a-table-column>
      </a-table>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.apps-search-card { margin-bottom: 24px; }
.apps-search-row { width: 100%; }
.apps-search-row :deep(.ant-form-item) { width: 100%; margin-bottom: 0; }
.apps-search-row :deep(.ant-form-item-control) { flex: 1; }
.apps-search-row :deep(.ant-select) { width: 100%; }
.application-icon { display: block; width: auto; height: 30px; max-width: 48px; margin: 0 auto; object-fit: contain; }
.enabled-icon { color: green; font-size: 16px; }
@media (max-width: 768px) { .apps-search-row :deep(.ant-form-item) { margin-bottom: 16px; } }
</style>

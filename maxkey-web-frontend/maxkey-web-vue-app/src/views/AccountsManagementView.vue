<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost } from '../api'

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

const { t } = useI18n({ useScope: 'global' })
const route = useRoute()
const rows = ref<DataRow[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const selectedRowKeys = ref<Array<string | number>>([])
const search = reactive({ appId: '', appName: '', username: '' })
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const editorOpen = ref(false)
const passwordVisible = ref(false)
const form = reactive<DataRow>({})

const appSelectorOpen = ref(false)
const appSelectorTarget = ref<'filter' | 'editor'>('filter')
const appRows = ref<DataRow[]>([])
const appLoading = ref(false)
const appSearch = ref('')
const appPage = reactive({ current: 1, pageSize: 10, total: 0 })

const userSelectorOpen = ref(false)
const userRows = ref<DataRow[]>([])
const userLoading = ref(false)
const userSearch = reactive({ username: '', displayName: '' })
const userPage = reactive({ current: 1, pageSize: 10, total: 0 })
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  getCheckboxProps: (record: DataRow) => ({ disabled: record.disabled }),
  onChange: (keys: Array<string | number>) => { selectedRowKeys.value = keys },
}))

function normalizePage(data: unknown) {
  if (Array.isArray(data)) return { rows: data as DataRow[], total: data.length }
  const value = (data || {}) as PageData
  const list = Array.isArray(value.rows) ? value.rows : []
  return { rows: list, total: typeof value.records === 'number' ? value.records : typeof value.total === 'number' ? value.total : list.length }
}

async function load(resetPage = false) {
  if (resetPage) page.current = 1
  loading.value = true
  error.value = ''
  selectedRowKeys.value = []
  try {
    const result = normalizePage(await adminGet('/accounts/fetch', {
      appId: search.appId,
      appName: search.appName,
      username: search.username,
      pageNumber: page.current,
      pageSize: page.pageSize,
    }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '账号列表加载失败'
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  Object.assign(search, { appId: '', appName: '', username: '' })
}

async function loadApps(resetPage = false) {
  if (resetPage) appPage.current = 1
  appLoading.value = true
  try {
    const result = normalizePage(await adminGet('/apps/fetch', { appName: appSearch.value, pageNumber: appPage.current, pageSize: appPage.pageSize }))
    appRows.value = result.rows
    appPage.total = result.total
  } catch (err) {
    message.error(err instanceof Error ? err.message : '应用列表加载失败')
  } finally {
    appLoading.value = false
  }
}

function openAppSelector(target: 'filter' | 'editor') {
  appSelectorTarget.value = target
  appSelectorOpen.value = true
  void loadApps(true)
}

function selectApp(row: DataRow) {
  if (appSelectorTarget.value === 'filter') {
    search.appId = String(row.id || '')
    search.appName = String(row.appName || '')
    void load(true)
  } else {
    form.appId = row.id
    form.appName = row.appName
  }
  appSelectorOpen.value = false
}

async function loadUsers(resetPage = false) {
  if (resetPage) userPage.current = 1
  userLoading.value = true
  try {
    const result = normalizePage(await adminGet('/users/fetch', {
      username: userSearch.username,
      displayName: userSearch.displayName,
      pageNumber: userPage.current,
      pageSize: userPage.pageSize,
    }))
    userRows.value = result.rows
    userPage.total = result.total
  } catch (err) {
    message.error(err instanceof Error ? err.message : '用户列表加载失败')
  } finally {
    userLoading.value = false
  }
}

function openUserSelector() {
  userSelectorOpen.value = true
  void loadUsers(true)
}

function selectUser(row: DataRow) {
  form.userId = row.id
  form.username = row.username
  form.displayName = row.displayName
  userSelectorOpen.value = false
}

async function openEditor() {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, { createType: 'manual', status: 1 })
  passwordVisible.value = false
  if (search.username) {
    try {
      const user = await adminGet<DataRow>(`/users/getByUsername/${encodeURIComponent(search.username)}`)
      selectUser(user)
    } catch (err) {
      message.error(err instanceof Error ? err.message : '用户信息加载失败')
      return
    }
  }
  if (search.appId) {
    form.appId = search.appId
    form.appName = search.appName
  }
  editorOpen.value = true
}

async function generatePassword() {
  try {
    form.relatedPassword = await adminGet<string>('/users/randomPassword')
  } catch (err) {
    message.error(err instanceof Error ? err.message : '随机密码生成失败')
  }
}

async function saveEditor() {
  const missing = [
    ['userId', t('admin.mxk.accounts.displayName')],
    ['appId', t('admin.mxk.accounts.appName')],
    ['relatedUsername', t('admin.mxk.accounts.relatedUsername')],
    ['relatedPassword', t('admin.mxk.accounts.relatedPassword')],
  ].find(([key]) => !form[key])
  if (missing) {
    message.warning(`请选择或填写${missing[1]}`)
    return
  }
  saving.value = true
  try {
    await adminPost('/accounts/add', form)
    message.success(t('admin.mxk.alert.add.success'))
    editorOpen.value = false
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('admin.mxk.alert.add.error'))
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
        await adminDelete('/accounts/delete', { ids: ids.join(',') })
        message.success(t('admin.mxk.alert.delete.success'))
        await load()
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

function onAppTableChange(pagination: { current?: number; pageSize?: number }) {
  const sizeChanged = pagination.pageSize !== undefined && pagination.pageSize !== appPage.pageSize
  appPage.pageSize = pagination.pageSize || 10
  appPage.current = sizeChanged ? 1 : pagination.current || 1
  void loadApps()
}

function onUserTableChange(pagination: { current?: number; pageSize?: number }) {
  const sizeChanged = pagination.pageSize !== undefined && pagination.pageSize !== userPage.pageSize
  userPage.pageSize = pagination.pageSize || 10
  userPage.current = sizeChanged ? 1 : pagination.current || 1
  void loadUsers()
}

onMounted(() => {
  search.username = String(route.query.username || '')
  void load()
})
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t('admin.mxk.menu.accounts') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />

    <a-card :bordered="false" class="accounts-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }" class="accounts-search-row">
          <a-col :xs="24" :md="8">
            <a-form-item :label="t('admin.mxk.accounts.appName')">
              <a-input-search v-model:value="search.appName" readonly @search="openAppSelector('filter')"><template #enterButton><a-button type="primary">{{ t('admin.mxk.text.select') }}</a-button></template></a-input-search>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8"><a-form-item :label="t('admin.mxk.accounts.username')"><a-input v-model:value="search.username" /></a-form-item></a-col>
          <a-col :xs="24" :md="8"><a-space><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button><a-button html-type="reset" @click="resetSearch">{{ t('admin.mxk.text.reset') }}</a-button></a-space></a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card class="accounts-table-card">
      <div class="table-list-toolbar">
        <a-button type="primary" @click="openEditor">{{ t('admin.mxk.text.add') }}</a-button>
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
        :scroll="{ x: 850 }"
        @change="onTableChange"
      >
        <a-table-column :title="t('admin.mxk.accounts.username')" data-index="username" />
        <a-table-column :title="t('admin.mxk.accounts.displayName')" data-index="displayName" />
        <a-table-column :title="t('admin.mxk.accounts.appName')" data-index="appName" />
        <a-table-column :title="t('admin.mxk.accounts.relatedUsername')" data-index="relatedUsername" />
        <a-table-column align="center" :title="t('admin.mxk.text.action')" :width="110"><template #default="{ record }"><div class="action-buttons"><a-button danger @click="confirmDelete([record.id])">{{ t('ui.delete') }}</a-button></div></template></a-table-column>
      </a-table>
    </a-card>

    <a-modal v-model:open="editorOpen" :title="t('admin.mxk.text.add')" width="680px" :confirm-loading="saving" @ok="saveEditor">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-item :label="t('admin.mxk.accounts.displayName')" required>
          <a-input-search v-model:value="form.displayName" readonly @search="openUserSelector"><template #enterButton><a-button type="primary" :disabled="Boolean(search.username)">{{ t('admin.mxk.text.select') }}</a-button></template></a-input-search>
        </a-form-item>
        <a-form-item :label="t('admin.mxk.accounts.username')" required><a-input v-model:value="form.username" disabled /></a-form-item>
        <a-form-item :label="t('admin.mxk.accounts.appName')" required>
          <a-input-search v-model:value="form.appName" readonly @search="openAppSelector('editor')"><template #enterButton><a-button type="primary">{{ t('admin.mxk.text.select') }}</a-button></template></a-input-search>
        </a-form-item>
        <a-form-item :label="t('admin.mxk.accounts.relatedUsername')" required><a-input v-model:value="form.relatedUsername" /></a-form-item>
        <a-form-item :label="t('admin.mxk.accounts.relatedPassword')" required>
          <a-input-group compact class="password-generator"><a-input v-model:value="form.relatedPassword" :type="passwordVisible ? 'text' : 'password'" /><a-button @click="passwordVisible = !passwordVisible">{{ passwordVisible ? '隐藏' : '显示' }}</a-button><a-button type="primary" @click="generatePassword">{{ t('admin.mxk.text.generate') }}</a-button></a-input-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="appSelectorOpen" :title="t('admin.mxk.text.select')" width="760px" :footer="null">
      <a-form layout="inline" class="selector-search" @submit.prevent="loadApps(true)"><a-form-item :label="t('admin.mxk.apps.name')"><a-input v-model:value="appSearch" /></a-form-item><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-form>
      <a-table size="small" row-key="id" :data-source="appRows" :loading="appLoading" :pagination="{ current: appPage.current, pageSize: appPage.pageSize, total: appPage.total, showSizeChanger: true }" @change="onAppTableChange">
        <a-table-column :title="t('admin.mxk.apps.name')" data-index="appName" />
        <a-table-column title="协议" data-index="protocol" />
        <a-table-column :title="t('admin.mxk.text.action')" :width="90"><template #default="{ record }"><a-button type="primary" @click="selectApp(record)">{{ t('admin.mxk.text.select') }}</a-button></template></a-table-column>
      </a-table>
    </a-modal>

    <a-modal v-model:open="userSelectorOpen" :title="t('admin.mxk.text.select')" width="820px" :footer="null">
      <a-form layout="inline" class="selector-search" @submit.prevent="loadUsers(true)"><a-form-item :label="t('admin.mxk.accounts.username')"><a-input v-model:value="userSearch.username" /></a-form-item><a-form-item :label="t('admin.mxk.accounts.displayName')"><a-input v-model:value="userSearch.displayName" /></a-form-item><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-form>
      <a-table size="small" row-key="id" :data-source="userRows" :loading="userLoading" :pagination="{ current: userPage.current, pageSize: userPage.pageSize, total: userPage.total, showSizeChanger: true }" @change="onUserTableChange">
        <a-table-column :title="t('admin.mxk.accounts.username')" data-index="username" />
        <a-table-column :title="t('admin.mxk.accounts.displayName')" data-index="displayName" />
        <a-table-column :title="t('admin.mxk.text.action')" :width="90"><template #default="{ record }"><a-button type="primary" @click="selectUser(record)">{{ t('admin.mxk.text.select') }}</a-button></template></a-table-column>
      </a-table>
    </a-modal>
  </DefaultLayout>
</template>

<style scoped>
.accounts-search-card { margin-bottom: 24px; }
.accounts-search-row, .accounts-search-row :deep(.ant-form-item) { width: 100%; }
.table-list-toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.action-buttons { display: flex; align-items: center; justify-content: center; white-space: nowrap; }
.action-buttons :deep(.ant-btn) { flex: 0 0 auto; }
.password-generator { display: flex; }
.password-generator :deep(.ant-input) { width: 0; min-width: 140px; flex: 1; }
.selector-search { margin-bottom: 16px; }
</style>

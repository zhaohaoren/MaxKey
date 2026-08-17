<script setup lang="ts">
import { CheckCircleFilled } from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminGetLegacyPage, adminPost } from '../api'
import { normalizeIdentityPage, type IdentityRow } from '../identity'

const { t } = useI18n({ useScope: 'global' })
const route = useRoute()
const rowKey = (row: IdentityRow) => String(row.id)

const groupRows = ref<IdentityRow[]>([])
const groupLoading = ref(false)
const groupSearch = ref('')
const groupPage = reactive({ current: 1, pageSize: 10, total: 0 })
const activeGroupId = ref(String(route.query.groupId || ''))
const activeGroupName = ref(String(route.query.groupName || ''))

const rows = ref<IdentityRow[]>([])
const loading = ref(false)
const error = ref('')
const appName = ref('')
const selectedKeys = ref<Array<string | number>>([])
const page = reactive({ current: 1, pageSize: 10, total: 0 })

const selectorOpen = ref(false)
const selectorLoading = ref(false)
const selectorSaving = ref(false)
const selectorRows = ref<IdentityRow[]>([])
const selectorSearch = ref('')
const selectorKeys = ref<Array<string | number>>([])
const selectorPage = reactive({ current: 1, pageSize: 5, total: 0 })

const rowSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }),
  onChange: (keys: Array<string | number>) => { selectedKeys.value = keys },
}))

const selectorSelection = computed(() => ({
  selectedRowKeys: selectorKeys.value,
  getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }),
  onChange: (keys: Array<string | number>) => { selectorKeys.value = keys },
}))

async function loadGroups(reset = false) {
  if (reset) groupPage.current = 1
  groupLoading.value = true
  try {
    const result = normalizeIdentityPage(await adminGet('/access/groups/fetch', {
      groupName: groupSearch.value, pageNumber: groupPage.current, pageSize: groupPage.pageSize,
    }))
    groupRows.value = result.rows
    groupPage.total = result.total
  } catch (err) {
    groupRows.value = []
    groupPage.total = 0
    message.error(err instanceof Error ? err.message : '用户组列表加载失败')
  } finally {
    groupLoading.value = false
  }
}

async function load(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  error.value = ''
  selectedKeys.value = []
  try {
    const result = normalizeIdentityPage(await adminGetLegacyPage('/access/access/appsInGroup', {
      groupId: activeGroupId.value, groupName: activeGroupName.value, appName: appName.value,
      pageNumber: page.current, pageSize: page.pageSize,
    }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '访问控制列表加载失败'
  } finally {
    loading.value = false
  }
}

function chooseGroup(row: IdentityRow) {
  activeGroupId.value = String(row.id || '')
  activeGroupName.value = String(row.groupName || '')
  void load(true)
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
        await adminDelete('/access/access/delete', { ids: ids.join(',') })
        message.success(t('admin.mxk.alert.delete.success'))
        await load()
      } catch (err) {
        message.error(err instanceof Error ? err.message : t('admin.mxk.alert.delete.error'))
      }
    },
  })
}

async function openSelector() {
  if (!activeGroupId.value) return void message.warning('请先选择用户组')
  selectorSearch.value = ''
  selectorKeys.value = []
  selectorOpen.value = true
  await loadSelector(true)
}

async function loadSelector(reset = false) {
  if (reset) selectorPage.current = 1
  selectorLoading.value = true
  selectorKeys.value = []
  try {
    const result = normalizeIdentityPage(await adminGetLegacyPage('/access/access/appsNotInGroup', {
      groupId: activeGroupId.value, appName: selectorSearch.value,
      pageNumber: selectorPage.current, pageSize: selectorPage.pageSize,
    }))
    selectorRows.value = result.rows
    selectorPage.total = result.total
  } catch (err) {
    selectorRows.value = []
    selectorPage.total = 0
    message.error(err instanceof Error ? err.message : '可授权应用加载失败')
  } finally {
    selectorLoading.value = false
  }
}

async function addSelectedApps() {
  const selected = selectorRows.value.filter(row => selectorKeys.value.includes(row.id as string | number))
  if (!selected.length) return void message.warning('请选择要添加的应用')
  selectorSaving.value = true
  try {
    await adminPost('/access/access/add', {
      groupId: activeGroupId.value,
      appId: selected.map(row => row.id).join(','),
      appName: selected.map(row => row.appName).join(','),
    })
    message.success(t('admin.mxk.alert.add.success'))
    await Promise.all([load(), loadSelector()])
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('admin.mxk.alert.add.error'))
  } finally {
    selectorSaving.value = false
  }
}

function categoryLabel(value: unknown) {
  const category = String(value || 'none')
  return t(`admin.mxk.apps.category.${category}`)
}

function onGroupTableChange(pagination: { current?: number; pageSize?: number }) {
  const changed = pagination.pageSize !== undefined && pagination.pageSize !== groupPage.pageSize
  groupPage.pageSize = pagination.pageSize || 10
  groupPage.current = changed ? 1 : pagination.current || 1
  void loadGroups()
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const changed = pagination.pageSize !== undefined && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = changed ? 1 : pagination.current || 1
  void load()
}

function onSelectorTableChange(pagination: { current?: number; pageSize?: number }) {
  const changed = pagination.pageSize !== undefined && pagination.pageSize !== selectorPage.pageSize
  selectorPage.pageSize = pagination.pageSize || 5
  selectorPage.current = changed ? 1 : pagination.current || 1
  void loadSelector()
}

onMounted(() => { void Promise.all([loadGroups(), load()]) })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t('admin.mxk.menu.access.permissions') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="admin-master-detail-card">
      <a-row :gutter="24" class="admin-master-detail">
        <a-col :xs="24" :md="8" class="access-groups-column">
          <a-card :bordered="false" class="identity-inner-search">
            <a-form layout="inline" @submit.prevent="loadGroups(true)">
              <a-form-item :label="t('admin.mxk.groups.name')"><a-input v-model:value="groupSearch" /></a-form-item>
              <a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button>
            </a-form>
          </a-card>
          <a-table class="admin-master-table" size="small" bordered :loading="groupLoading" :data-source="groupRows" :row-key="rowKey" :scroll="{ y: 280 }"
            :pagination="{ current: groupPage.current, pageSize: groupPage.pageSize, total: groupPage.total, showSizeChanger: true, pageSizeOptions: ['10','20','50'] }" @change="onGroupTableChange">
            <a-table-column key="select" width="48" align="center"><template #default="{ record }"><a-checkbox :checked="String(record.id) === activeGroupId" @change="chooseGroup(record)" /></template></a-table-column>
            <a-table-column key="groupName" data-index="groupName" :title="t('admin.mxk.groups.name')" />
            <a-table-column key="category" :title="t('ui.groupCategory')" align="center"><template #default="{ record }">{{ t(`admin.mxk.groups.category.${String(record.category || '')}`) }}</template></a-table-column>
          </a-table>
        </a-col>
        <a-col :xs="24" :md="16" class="access-apps-column">
          <a-card :bordered="false" class="identity-inner-search">
            <a-form layout="inline" @submit.prevent="load(true)">
              <a-form-item :label="t('admin.mxk.roles.name')"><a-input :value="activeGroupName" disabled /></a-form-item>
              <a-form-item :label="t('admin.mxk.apps.name')"><a-input v-model:value="appName" /></a-form-item>
              <a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button>
            </a-form>
          </a-card>
          <a-card :bordered="false" class="access-table-card">
            <div class="identity-toolbar">
              <a-button type="primary" @click="openSelector">{{ t('admin.mxk.text.add') }}</a-button>
              <a-button danger type="primary" :disabled="!selectedKeys.length" @click="confirmDelete(selectedKeys)">{{ t('admin.mxk.text.batchDelete') }}</a-button>
            </div>
            <a-table size="small" bordered :loading="loading" :data-source="rows" :row-key="rowKey" :row-selection="rowSelection"
              :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true, pageSizeOptions: ['10','20','50'] }" :scroll="{ x: 720 }" @change="onTableChange">
              <a-table-column key="groupName" data-index="groupName" :title="t('admin.mxk.roles.name')" :width="140" ellipsis />
              <a-table-column key="icon" :title="t('admin.mxk.apps.icon')" align="center" width="70"><template #default="{ record }"><img v-if="record.iconBase64" class="access-app-icon" :src="String(record.iconBase64)" alt="" /></template></a-table-column>
              <a-table-column key="appName" data-index="appName" :title="t('admin.mxk.apps.name')" :width="180" ellipsis />
              <a-table-column key="category" :title="t('ui.appCategory')" :width="190" ellipsis><template #default="{ record }">{{ categoryLabel(record.category) }}</template></a-table-column>
              <a-table-column key="action" :title="t('admin.mxk.text.action')" align="center" width="90" fixed="right"><template #default="{ record }"><a-button size="small" danger @click="confirmDelete([record.id])">{{ t('ui.delete') }}</a-button></template></a-table-column>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

    <a-modal v-model:open="selectorOpen" :title="t('admin.mxk.text.add')" width="700px" :footer="null">
      <a-card :bordered="false" class="access-selector-card">
        <a-form layout="inline" class="access-selector-search" @submit.prevent="loadSelector(true)">
          <a-form-item :label="t('admin.mxk.apps.name')"><a-input v-model:value="selectorSearch" /></a-form-item>
          <a-space><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button><a-button type="primary" :loading="selectorSaving" @click="addSelectedApps">{{ t('admin.mxk.text.confirm') }}</a-button></a-space>
        </a-form>
        <a-table bordered :loading="selectorLoading" :data-source="selectorRows" :row-key="rowKey" :row-selection="selectorSelection"
          :pagination="{ current: selectorPage.current, pageSize: selectorPage.pageSize, total: selectorPage.total, showSizeChanger: true, pageSizeOptions: ['5','15','50'] }" :scroll="{ x: 650 }" @change="onSelectorTableChange">
          <a-table-column key="icon" :title="t('admin.mxk.apps.icon')" align="center" width="70"><template #default="{ record }"><img v-if="record.iconBase64" class="access-app-icon" :src="String(record.iconBase64)" alt="" /></template></a-table-column>
          <a-table-column key="appName" data-index="appName" :title="t('admin.mxk.apps.name')" />
          <a-table-column key="category" :title="t('ui.appCategory')"><template #default="{ record }">{{ categoryLabel(record.category) }}</template></a-table-column>
          <a-table-column key="sortIndex" data-index="sortIndex" :title="t('admin.mxk.text.sortIndex')" align="center" width="80" />
          <a-table-column key="status" :title="t('ui.status')" align="center" width="70"><template #default="{ record }"><CheckCircleFilled v-if="Number(record.status) === 1" class="identity-status-active" /></template></a-table-column>
        </a-table>
      </a-card>
    </a-modal>
  </DefaultLayout>
</template>

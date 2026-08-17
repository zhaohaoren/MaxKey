<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost } from '../api'
import { normalizeIdentityPage, type IdentityRow } from '../identity'

const { t } = useI18n({ useScope: 'global' })
const route = useRoute()
const groupRows = ref<IdentityRow[]>([])
const groupLoading = ref(false)
const groupSearch = ref('')
const groupPage = reactive({ current: 1, pageSize: 10, total: 0 })
const activeGroupId = ref(String(route.query.groupId || ''))
const activeGroupName = ref(String(route.query.groupName || ''))

const rows = ref<IdentityRow[]>([])
const loading = ref(false)
const error = ref('')
const username = ref(String(route.query.username || ''))
const selectedKeys = ref<Array<string | number>>([])
const page = reactive({ current: 1, pageSize: 10, total: 0 })

const selectorOpen = ref(false)
const selectorLoading = ref(false)
const selectorSaving = ref(false)
const selectorRows = ref<IdentityRow[]>([])
const selectorSearch = ref('')
const selectorKeys = ref<Array<string | number>>([])
const selectorPage = reactive({ current: 1, pageSize: 5, total: 0 })
const selectorMode = ref<'users' | 'groups'>('users')

const rowSelection = computed(() => ({ selectedRowKeys: selectedKeys.value, getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }), onChange: (keys: Array<string | number>) => { selectedKeys.value = keys } }))
const selectorSelection = computed(() => ({ selectedRowKeys: selectorKeys.value, getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }), onChange: (keys: Array<string | number>) => { selectorKeys.value = keys } }))
const rowKey = (row: IdentityRow) => String(row.id)

async function loadGroups(reset = false) {
  if (reset) groupPage.current = 1
  groupLoading.value = true
  try {
    const result = normalizeIdentityPage(await adminGet('/access/groups/fetch', { groupName: groupSearch.value, pageNumber: groupPage.current, pageSize: groupPage.pageSize }))
    groupRows.value = result.rows
    groupPage.total = result.total
  } catch (err) { message.error(err instanceof Error ? err.message : '用户组列表加载失败') }
  finally { groupLoading.value = false }
}

async function load(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  error.value = ''
  selectedKeys.value = []
  try {
    const result = normalizeIdentityPage(await adminGet('/access/groupmembers/memberIn', {
      groupId: activeGroupId.value, groupName: activeGroupName.value, username: username.value,
      pageNumber: page.current, pageSize: page.pageSize,
    }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '用户组成员加载失败'
  } finally { loading.value = false }
}

function chooseGroup(row: IdentityRow) {
  activeGroupId.value = String(row.id || '')
  activeGroupName.value = String(row.groupName || '')
  void load(true)
}

function resetSearch() {
  username.value = ''
  activeGroupId.value = ''
  activeGroupName.value = ''
  void load(true)
}

function confirmDelete(ids: Array<string | number>) {
  if (!ids.length) return
  Modal.confirm({ title: t('admin.mxk.text.delete.popconfirm.title'), cancelText: t('admin.mxk.text.delete.popconfirm.cancelText'), okText: t('admin.mxk.text.delete.popconfirm.okText'), okType: 'danger', async onOk() {
    try { await adminDelete('/access/groupmembers/delete', { ids: ids.join(',') }); message.success(t('admin.mxk.alert.delete.success')); await load() }
    catch (err) { message.error(err instanceof Error ? err.message : t('admin.mxk.alert.delete.error')) }
  } })
}

async function openSelector() {
  if (username.value) selectorMode.value = 'groups'
  else if (activeGroupId.value) selectorMode.value = 'users'
  else { message.warning('请先选择用户组或输入登录账号'); return }
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
    const path = selectorMode.value === 'users' ? '/access/groupmembers/memberNotIn' : '/access/groupmembers/noMember'
    const params = selectorMode.value === 'users'
      ? { groupId: activeGroupId.value, username: selectorSearch.value, pageNumber: selectorPage.current, pageSize: selectorPage.pageSize }
      : { username: username.value, groupName: selectorSearch.value, pageNumber: selectorPage.current, pageSize: selectorPage.pageSize }
    const result = normalizeIdentityPage(await adminGet(path, params))
    selectorRows.value = result.rows
    selectorPage.total = result.total
  } catch (err) { message.error(err instanceof Error ? err.message : '可选成员加载失败') }
  finally { selectorLoading.value = false }
}

async function saveSelector() {
  const selected = selectorRows.value.filter(row => selectorKeys.value.includes(row.id as string | number))
  if (!selected.length) return void message.warning('请选择要添加的数据')
  selectorSaving.value = true
  try {
    if (selectorMode.value === 'users') {
      await adminPost('/access/groupmembers/add', {
        type: 'USER', groupId: activeGroupId.value, groupName: activeGroupName.value,
        memberId: selected.map(row => row.id).join(','), memberName: selected.map(row => row.username).join(','),
      })
    } else {
      await adminPost('/access/groupmembers/addMember2Groups', {
        username: username.value, groupId: selected.map(row => row.id).join(','), groupName: selected.map(row => row.groupName).join(','),
      })
    }
    message.success(t('admin.mxk.alert.operate.success'))
    await Promise.all([load(), loadSelector()])
  } catch (err) { message.error(err instanceof Error ? err.message : t('admin.mxk.alert.operate.error')) }
  finally { selectorSaving.value = false }
}

function categoryLabel(value: unknown) { return t(`admin.mxk.groups.category.${String(value || '')}`) }
function onGroupTableChange(p: { current?: number; pageSize?: number }) { const changed = p.pageSize !== undefined && p.pageSize !== groupPage.pageSize; groupPage.pageSize = p.pageSize || 10; groupPage.current = changed ? 1 : p.current || 1; void loadGroups() }
function onTableChange(p: { current?: number; pageSize?: number }) { const changed = p.pageSize !== undefined && p.pageSize !== page.pageSize; page.pageSize = p.pageSize || 10; page.current = changed ? 1 : p.current || 1; void load() }
function onSelectorTableChange(p: { current?: number; pageSize?: number }) { const changed = p.pageSize !== undefined && p.pageSize !== selectorPage.pageSize; selectorPage.pageSize = p.pageSize || 5; selectorPage.current = changed ? 1 : p.current || 1; void loadSelector() }

onMounted(() => { void Promise.all([loadGroups(), load()]) })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t('admin.mxk.menu.identities.groupmembers') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="admin-master-detail-card">
      <a-row :gutter="24" class="admin-master-detail">
        <a-col :xs="24" :md="8" class="identity-members-groups">
          <a-card :bordered="false" class="identity-inner-search"><a-form layout="inline" @submit.prevent="loadGroups(true)"><a-form-item :label="t('admin.mxk.groups.name')"><a-input v-model:value="groupSearch" /></a-form-item><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-form></a-card>
          <a-table class="admin-master-table" size="small" bordered :loading="groupLoading" :data-source="groupRows" :row-key="rowKey" :scroll="{ y: 280 }" :pagination="{ current: groupPage.current, pageSize: groupPage.pageSize, total: groupPage.total, showSizeChanger: true, pageSizeOptions: ['10','20','50'] }" @change="onGroupTableChange">
            <a-table-column key="select" width="48" align="center"><template #default="{ record }"><a-checkbox :checked="String(record.id) === activeGroupId" @change="chooseGroup(record)" /></template></a-table-column>
            <a-table-column key="groupName" data-index="groupName" :title="t('admin.mxk.groups.name')" />
            <a-table-column key="category" :title="t('ui.groupCategory')" align="center"><template #default="{ record }">{{ categoryLabel(record.category) }}</template></a-table-column>
          </a-table>
        </a-col>
        <a-col :xs="24" :md="16" class="identity-members-list">
          <a-card :bordered="false" class="identity-inner-search"><a-form layout="inline" @submit.prevent="load(true)"><a-form-item :label="t('admin.mxk.groups.name')"><a-input :value="activeGroupName" disabled /></a-form-item><a-form-item :label="t('admin.mxk.users.username')"><a-input v-model:value="username" /></a-form-item><a-space><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button><a-button @click="resetSearch">{{ t('admin.mxk.text.reset') }}</a-button></a-space></a-form></a-card>
          <a-card :bordered="false" class="identity-members-table-card">
            <div class="identity-toolbar"><a-button type="primary" @click="openSelector">{{ t('admin.mxk.text.add') }}</a-button><a-button danger type="primary" :disabled="!selectedKeys.length" @click="confirmDelete(selectedKeys)">{{ t('admin.mxk.text.batchDelete') }}</a-button></div>
            <a-table size="small" bordered :loading="loading" :data-source="rows" :row-key="rowKey" :row-selection="rowSelection" :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true, pageSizeOptions: ['10','20','50'] }" :scroll="{ x: 900 }" @change="onTableChange">
              <a-table-column key="groupName" data-index="groupName" :title="t('admin.mxk.groups.name')" :width="150" ellipsis />
              <a-table-column key="username" data-index="username" :title="t('admin.mxk.users.username')" :width="120" ellipsis />
              <a-table-column key="displayName" data-index="displayName" :title="t('admin.mxk.users.displayName')" :width="110" ellipsis />
              <a-table-column key="department" data-index="department" :title="t('admin.mxk.users.department')" :width="150" ellipsis />
              <a-table-column key="jobTitle" data-index="jobTitle" :title="t('admin.mxk.users.jobTitle')" :width="130" ellipsis />
              <a-table-column key="gender" :title="t('ui.userGender')" align="center" :width="72"><template #default="{ record }">{{ t(Number(record.gender) === 1 ? 'admin.mxk.users.gender.female' : 'admin.mxk.users.gender.male') }}</template></a-table-column>
              <a-table-column key="action" :title="t('admin.mxk.text.action')" align="center" width="90" fixed="right"><template #default="{ record }"><a-button v-if="record.category === 'static'" size="small" danger @click="confirmDelete([record.id])">{{ t('ui.delete') }}</a-button></template></a-table-column>
            </a-table>
          </a-card>
        </a-col>
      </a-row>
    </a-card>
    <a-modal v-model:open="selectorOpen" :title="t('admin.mxk.text.add')" width="700px" :confirm-loading="selectorSaving" :ok-text="t('admin.mxk.text.confirm')" :cancel-text="t('admin.mxk.text.close')" @ok="saveSelector">
      <a-form layout="inline" class="identity-selector-search" @submit.prevent="loadSelector(true)"><a-form-item :label="t(selectorMode === 'users' ? 'admin.mxk.users.username' : 'admin.mxk.groups.name')"><a-input v-model:value="selectorSearch" /></a-form-item><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-form>
      <a-table size="small" bordered :loading="selectorLoading" :data-source="selectorRows" :row-key="rowKey" :row-selection="selectorSelection" :pagination="{ current: selectorPage.current, pageSize: selectorPage.pageSize, total: selectorPage.total, showSizeChanger: true, pageSizeOptions: ['5','15','50'] }" @change="onSelectorTableChange">
        <template v-if="selectorMode === 'users'"><a-table-column key="username" data-index="username" :title="t('admin.mxk.users.username')" /><a-table-column key="displayName" data-index="displayName" :title="t('admin.mxk.users.displayName')" /><a-table-column key="employeeNumber" data-index="employeeNumber" :title="t('admin.mxk.users.employeeNumber')" /><a-table-column key="department" data-index="department" :title="t('admin.mxk.users.department')" /><a-table-column key="jobTitle" data-index="jobTitle" :title="t('admin.mxk.users.jobTitle')" /><a-table-column key="gender" :title="t('ui.userGender')" align="center"><template #default="{ record }">{{ t(Number(record.gender) === 1 ? 'admin.mxk.users.gender.female' : 'admin.mxk.users.gender.male') }}</template></a-table-column></template>
        <template v-else><a-table-column key="groupName" data-index="groupName" :title="t('admin.mxk.groups.name')" /><a-table-column key="category" :title="t('ui.groupCategory')" align="center"><template #default="{ record }">{{ categoryLabel(record.category) }}</template></a-table-column></template>
      </a-table>
    </a-modal>
  </DefaultLayout>
</template>

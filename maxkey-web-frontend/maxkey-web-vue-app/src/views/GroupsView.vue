<script setup lang="ts">
import { DownOutlined } from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminDelete, adminGet, adminPost, adminPut } from '../api'
import { buildOrganizationTree, normalizeIdentityPage, type IdentityRow, type IdentityTreeNode } from '../identity'

const { t } = useI18n({ useScope: 'global' })
const router = useRouter()
const rows = ref<IdentityRow[]>([])
const nodes = ref<IdentityTreeNode[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const search = ref('')
const selectedKeys = ref<Array<string | number>>([])
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const editorOpen = ref(false)
const editing = ref(false)
const form = reactive<IdentityRow>({})
const selectedOrganizations = ref<string[]>([])

const rowSelection = computed(() => ({ selectedRowKeys: selectedKeys.value, getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }), onChange: (keys: Array<string | number>) => { selectedKeys.value = keys } }))
const rowKey = (row: IdentityRow) => String(row.id)

async function load(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  error.value = ''
  selectedKeys.value = []
  try {
    const result = normalizeIdentityPage(await adminGet('/access/groups/fetch', { groupName: search.value, pageNumber: page.current, pageSize: page.pageSize }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '用户组列表加载失败'
  } finally { loading.value = false }
}

async function loadTree() {
  try { nodes.value = buildOrganizationTree(await adminGet('/orgs/tree')) }
  catch (err) { message.error(err instanceof Error ? err.message : '组织树加载失败') }
}

async function openEditor(row?: IdentityRow) {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, { category: 'static' })
  selectedOrganizations.value = []
  editing.value = Boolean(row?.id)
  if (row?.id) {
    try {
      const detail = await adminGet<IdentityRow>(`/access/groups/get/${row.id}`)
      Object.assign(form, detail)
      selectedOrganizations.value = String(detail.orgIdsList || '').split(',').filter(Boolean)
    } catch (err) { message.error(err instanceof Error ? err.message : '用户组信息加载失败'); return }
  }
  editorOpen.value = true
}

async function save() {
  if (!String(form.groupName || '').trim()) return void message.warning(`请填写${t('admin.mxk.groups.name')}`)
  saving.value = true
  try {
    const payload = { ...form, orgIdsList: selectedOrganizations.value.length ? `${selectedOrganizations.value.join(',')},` : '' }
    if (editing.value) await adminPut('/access/groups/update', payload)
    else await adminPost('/access/groups/add', payload)
    message.success(t(editing.value ? 'admin.mxk.alert.update.success' : 'admin.mxk.alert.add.success'))
    editorOpen.value = false
    await load()
  } catch (err) { message.error(err instanceof Error ? err.message : t(editing.value ? 'admin.mxk.alert.update.error' : 'admin.mxk.alert.add.error')) }
  finally { saving.value = false }
}

function confirmDelete(ids: Array<string | number>) {
  if (!ids.length) return
  Modal.confirm({ title: t('admin.mxk.text.delete.popconfirm.title'), cancelText: t('admin.mxk.text.delete.popconfirm.cancelText'), okText: t('admin.mxk.text.delete.popconfirm.okText'), okType: 'danger', async onOk() {
    try { await adminDelete('/access/groups/delete', { ids: ids.join(',') }); message.success(t('admin.mxk.alert.delete.success')); await load() }
    catch (err) { message.error(err instanceof Error ? err.message : t('admin.mxk.alert.delete.error')) }
  } })
}

function canDelete(row: IdentityRow) { return !['ROLE_ADMINISTRATORS', 'ROLE_ALL_USER', 'ROLE_MANAGERS'].includes(String(row.roleCode || row.id || '')) }
function openMembers(row: IdentityRow) { void router.push({ path: '/admin/groupmembers', query: { groupId: String(row.id || ''), groupName: String(row.groupName || '') } }) }
function openPermissions(row: IdentityRow) { void router.push({ path: '/admin/access', query: { groupId: String(row.id || ''), groupName: String(row.groupName || '') } }) }
function categoryLabel(value: unknown) { return t(`admin.mxk.groups.category.${String(value || '')}`) }
function onTableChange(pagination: { current?: number; pageSize?: number }) { const changed = pagination.pageSize !== undefined && pagination.pageSize !== page.pageSize; page.pageSize = pagination.pageSize || 10; page.current = changed ? 1 : pagination.current || 1; void load() }

onMounted(() => { void Promise.all([load(), loadTree()]) })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t('admin.mxk.menu.identities.groups') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="identity-search-card"><a-form layout="inline" @submit.prevent="load(true)"><a-row :gutter="{ xs: 8, sm: 8, md: 8, lg: 24, xl: 48, xxl: 48 }" class="identity-search-row"><a-col :xs="24" :md="16"><a-form-item :label="t('admin.mxk.groups.name')"><a-input v-model:value="search" /></a-form-item></a-col><a-col :xs="24" :md="8" class="identity-search-action"><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-col></a-row></a-form></a-card>
    <a-card>
      <div class="identity-toolbar"><a-button type="primary" @click="openEditor()">{{ t('admin.mxk.text.add') }}</a-button><a-button danger type="primary" :disabled="!selectedKeys.length" @click="confirmDelete(selectedKeys)">{{ t('admin.mxk.text.batchDelete') }}</a-button></div>
      <a-table size="small" bordered :loading="loading" :data-source="rows" :row-key="rowKey" :row-selection="rowSelection" :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true, pageSizeOptions: ['10','20','50'] }" :scroll="{ x: 760 }" @change="onTableChange">
        <a-table-column key="groupName" data-index="groupName" :title="t('admin.mxk.groups.name')" />
        <a-table-column key="category" :title="t('ui.groupCategory')" align="center"><template #default="{ record }">{{ categoryLabel(record.category) }}</template></a-table-column>
        <a-table-column key="description" data-index="description" :title="t('admin.mxk.text.description')" />
        <a-table-column key="action" :title="t('admin.mxk.text.action')" align="center" width="205" fixed="right"><template #default="{ record }"><a-space><a-button size="small" @click="openEditor(record)">{{ t('admin.mxk.text.edit') }}</a-button><a-dropdown><a-button size="small">{{ t('admin.mxk.text.moreaction') }} <DownOutlined /></a-button><template #overlay><a-menu><a-menu-item @click="openMembers(record)">{{ t('admin.mxk.groups.member') }}</a-menu-item><a-menu-item @click="openPermissions(record)">{{ t('admin.mxk.groups.permissions') }}</a-menu-item><a-menu-divider v-if="canDelete(record)" /><a-menu-item v-if="canDelete(record)" danger @click="confirmDelete([record.id])">{{ t('ui.delete') }}</a-menu-item></a-menu></template></a-dropdown></a-space></template></a-table-column>
      </a-table>
    </a-card>
    <a-modal v-model:open="editorOpen" :title="t(editing ? 'admin.mxk.text.edit' : 'admin.mxk.text.add')" width="560px" :confirm-loading="saving" :ok-text="t('admin.mxk.text.submit')" :cancel-text="t('admin.mxk.text.close')" @ok="save">
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }"><a-form-item required :label="t('admin.mxk.text.id')"><a-input v-model:value="form.groupCode" /></a-form-item><a-form-item required :label="t('admin.mxk.groups.name')"><a-input v-model:value="form.groupName" /></a-form-item><a-form-item :label="t('ui.groupCategory')"><a-radio-group v-model:value="form.category" button-style="solid"><a-radio-button value="static">{{ t('admin.mxk.groups.category.static') }}</a-radio-button><a-radio-button value="dynamic">{{ t('admin.mxk.groups.category.dynamic') }}</a-radio-button></a-radio-group></a-form-item><a-form-item v-if="form.category === 'dynamic'" :label="t('admin.mxk.groups.orgIdsList')"><a-tree-select v-model:value="selectedOrganizations" multiple tree-checkable tree-check-strictly tree-default-expand-all :max-tag-count="3" :tree-data="nodes" /></a-form-item><a-form-item :label="t('admin.mxk.text.description')"><a-textarea v-model:value="form.description" :rows="4" /></a-form-item></a-form>
    </a-modal>
  </DefaultLayout>
</template>

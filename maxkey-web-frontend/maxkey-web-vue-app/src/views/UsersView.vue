<script setup lang="ts">
import { CheckCircleFilled, CloseCircleFilled, DownOutlined, LockFilled, StopFilled, WarningFilled } from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import IdentityTree from '../components/IdentityTree.vue'
import UserEditor from '../components/UserEditor.vue'
import { adminDelete, adminGet, adminPut } from '../api'
import { buildOrganizationTree, normalizeIdentityPage, type IdentityRow, type IdentityTreeNode } from '../identity'

const { t } = useI18n({ useScope: 'global' })
const router = useRouter()
const rows = ref<IdentityRow[]>([])
const nodes = ref<IdentityTreeNode[]>([])
const loading = ref(false)
const error = ref('')
const search = ref('')
const selectedKeys = ref<Array<string | number>>([])
const selectedNode = ref<IdentityTreeNode>()
const page = reactive({ current: 1, pageSize: 10, total: 0 })
const editorOpen = ref(false)
const editingId = ref('')
const passwordOpen = ref(false)
const passwordVisible = ref(false)
const passwordSaving = ref(false)
const passwordForm = reactive<IdentityRow>({})
const mfaOpen = ref(false)
const mfaSaving = ref(false)
const mfaForm = reactive<IdentityRow>({})

const rowSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }),
  onChange: (keys: Array<string | number>) => { selectedKeys.value = keys },
}))
const rowKey = (row: IdentityRow) => String(row.id)

async function load(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  error.value = ''
  selectedKeys.value = []
  try {
    const result = normalizeIdentityPage(await adminGet('/users/fetch', {
      username: search.value, departmentId: selectedNode.value?.key || '', pageNumber: page.current, pageSize: page.pageSize,
    }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadTree() {
  try { nodes.value = buildOrganizationTree(await adminGet('/orgs/tree')) }
  catch (err) { message.error(err instanceof Error ? err.message : '组织树加载失败') }
}

function selectNode(node: IdentityTreeNode) {
  selectedNode.value = node
  void load(true)
}

function openEditor(row?: IdentityRow) {
  editingId.value = row?.id ? String(row.id) : ''
  editorOpen.value = true
}

function confirmDelete(ids: Array<string | number>) {
  if (!ids.length) return
  Modal.confirm({
    title: t('admin.mxk.text.delete.popconfirm.title'), cancelText: t('admin.mxk.text.delete.popconfirm.cancelText'),
    okText: t('admin.mxk.text.delete.popconfirm.okText'), okType: 'danger',
    async onOk() {
      try { await adminDelete('/users/delete', { ids: ids.join(',') }); message.success(t('admin.mxk.alert.delete.success')); await load() }
      catch (err) { message.error(err instanceof Error ? err.message : t('admin.mxk.alert.delete.error')) }
    },
  })
}

async function updateStatus(row: IdentityRow, status: number) {
  try {
    await adminGet('/users/updateStatus', { id: row.id, status })
    message.success(t('admin.mxk.alert.operate.success'))
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('admin.mxk.alert.operate.error'))
  }
}

function openPassword(row: IdentityRow) {
  Object.keys(passwordForm).forEach(key => delete passwordForm[key])
  Object.assign(passwordForm, { id: row.id, userId: row.id, username: row.username, displayName: row.displayName, password: '', confirmPassword: '' })
  passwordVisible.value = false
  passwordOpen.value = true
}

async function generatePassword() {
  try {
    const password = await adminGet<string>('/users/randomPassword')
    passwordForm.password = password
    passwordForm.confirmPassword = password
  } catch (err) { message.error(err instanceof Error ? err.message : '随机密码生成失败') }
}

async function savePassword() {
  if (!passwordForm.password) return void message.warning(`请填写${t('admin.mxk.password.password')}`)
  if (passwordForm.password !== passwordForm.confirmPassword) return void message.warning('两次输入的密码不一致')
  passwordSaving.value = true
  try {
    await adminPut('/users/changePassword', passwordForm)
    message.success(t('admin.mxk.alert.operate.success'))
    passwordOpen.value = false
  } catch (err) { message.error(err instanceof Error ? err.message : t('admin.mxk.alert.operate.error')) }
  finally { passwordSaving.value = false }
}

async function openMfa(row: IdentityRow) {
  try {
    const detail = await adminGet<IdentityRow>(`/users/get/${row.id}`)
    Object.keys(mfaForm).forEach(key => delete mfaForm[key])
    Object.assign(mfaForm, detail, { authnType: String(detail.authnType ?? '0') })
    mfaOpen.value = true
  } catch (err) { message.error(err instanceof Error ? err.message : '用户信息加载失败') }
}

async function saveMfa() {
  mfaSaving.value = true
  try {
    await adminPut('/users/updateAuthnType', mfaForm)
    message.success(t('admin.mxk.alert.operate.success'))
    mfaOpen.value = false
  } catch (err) { message.error(err instanceof Error ? err.message : t('admin.mxk.alert.operate.error')) }
  finally { mfaSaving.value = false }
}

function openGroups(row: IdentityRow) {
  void router.push({ path: '/admin/groupmembers', query: { username: String(row.username || ''), userId: String(row.id || '') } })
}

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  const sizeChanged = pagination.pageSize !== undefined && pagination.pageSize !== page.pageSize
  page.pageSize = pagination.pageSize || 10
  page.current = sizeChanged ? 1 : pagination.current || 1
  void load()
}

function statusTitle(status: number) {
  return t(`admin.mxk.users.status.${({ 1: 'active', 2: 'inactive', 4: 'forbidden', 5: 'lock', 9: 'delete' } as Record<number, string>)[status]}`)
}

onMounted(() => { void Promise.all([load(), loadTree()]) })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t('admin.mxk.menu.identities.users') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="identity-search-card">
      <a-form layout="inline" @submit.prevent="load(true)"><a-row :gutter="{ xs: 8, sm: 8, md: 24, lg: 24, xl: 48, xxl: 48 }" class="identity-search-row">
        <a-col :xs="24" :md="16"><a-form-item :label="t('admin.mxk.users.username')"><a-input v-model:value="search" /></a-form-item></a-col>
        <a-col :xs="24" :md="8" class="identity-search-action"><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-col>
      </a-row></a-form>
    </a-card>
    <a-card :bordered="false">
      <div class="identity-toolbar"><a-button type="primary" @click="openEditor()">{{ t('admin.mxk.text.add') }}</a-button><a-button danger type="primary" :disabled="!selectedKeys.length" @click="confirmDelete(selectedKeys)">{{ t('admin.mxk.text.batchDelete') }}</a-button></div>
      <a-row :gutter="24">
        <a-col :xs="24" :md="6" class="identity-tree-column"><IdentityTree :nodes="nodes" :selected-key="selectedNode?.key" @select="selectNode" /></a-col>
        <a-col :xs="24" :md="18" class="identity-table-column">
          <a-table size="small" bordered :loading="loading" :data-source="rows" :row-key="rowKey" :row-selection="rowSelection"
            :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }" :scroll="{ x: 1150 }" @change="onTableChange">
            <a-table-column key="username" data-index="username" :title="t('admin.mxk.users.username')" />
            <a-table-column key="displayName" data-index="displayName" :title="t('admin.mxk.users.displayName')" />
            <a-table-column key="employeeNumber" data-index="employeeNumber" :title="t('admin.mxk.users.employeeNumber')" />
            <a-table-column key="department" data-index="department" :title="t('admin.mxk.users.department')" />
            <a-table-column key="jobTitle" data-index="jobTitle" :title="t('admin.mxk.users.jobTitle')" />
            <a-table-column key="gender" :title="t('ui.userGender')" align="center" width="70"><template #default="{ record }">{{ t(Number(record.gender) === 1 ? 'admin.mxk.users.gender.female' : 'admin.mxk.users.gender.male') }}</template></a-table-column>
            <a-table-column key="status" :title="t('ui.status')" align="center" width="70"><template #default="{ record }"><span :title="statusTitle(Number(record.status))"><CheckCircleFilled v-if="Number(record.status) === 1" class="identity-status-active" /><WarningFilled v-else-if="Number(record.status) === 2" class="identity-status-muted" /><StopFilled v-else-if="Number(record.status) === 4" class="identity-status-muted" /><LockFilled v-else-if="Number(record.status) === 5" class="identity-status-lock" /><CloseCircleFilled v-else-if="Number(record.status) === 9" class="identity-status-delete" /></span></template></a-table-column>
            <a-table-column key="action" :title="t('admin.mxk.text.action')" align="center" width="190" fixed="right">
              <template #default="{ record }"><a-space><a-button size="small" @click="openEditor(record)">{{ t('admin.mxk.text.edit') }}</a-button>
                <a-dropdown :disabled="Number(record.status) === 9"><a-button size="small">{{ t('admin.mxk.text.moreaction') }} <DownOutlined /></a-button><template #overlay><a-menu>
                  <a-menu-item v-if="Number(record.status) === 1" @click="openGroups(record)">{{ t('admin.mxk.text.groups') }}</a-menu-item>
                  <a-menu-item v-if="Number(record.status) === 1" @click="openPassword(record)">{{ t('admin.mxk.text.changepassword') }}</a-menu-item>
                  <a-menu-item v-if="Number(record.status) === 1" @click="openMfa(record)">{{ t('ui.userAuthnType') }}</a-menu-item>
                  <a-menu-item v-if="Number(record.status) === 1" @click="updateStatus(record, 5)">{{ t('admin.mxk.text.lock') }}</a-menu-item>
                  <a-menu-item v-if="Number(record.status) === 1" @click="updateStatus(record, 4)">{{ t('admin.mxk.text.disable') }}</a-menu-item>
                  <a-menu-item v-if="[2,4].includes(Number(record.status))" @click="updateStatus(record, 1)">{{ t('admin.mxk.text.enable') }}</a-menu-item>
                  <a-menu-item v-if="Number(record.status) === 5" @click="updateStatus(record, 1)">{{ t('admin.mxk.text.unlock') }}</a-menu-item>
                  <a-menu-divider /><a-menu-item danger @click="confirmDelete([record.id])">{{ t('ui.delete') }}</a-menu-item>
                </a-menu></template></a-dropdown>
              </a-space></template>
            </a-table-column>
          </a-table>
        </a-col>
      </a-row>
    </a-card>
    <UserEditor v-model:open="editorOpen" :user-id="editingId" :parent-key="selectedNode?.key" :nodes="nodes" @saved="load" />
    <a-modal v-model:open="passwordOpen" :title="t('admin.mxk.text.changepassword')" width="450px" :confirm-loading="passwordSaving" :ok-text="t('admin.mxk.text.submit')" :cancel-text="t('admin.mxk.text.close')" @ok="savePassword">
      <a-form :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }"><a-form-item :label="t('admin.mxk.password.username')"><a-input v-model:value="passwordForm.username" disabled /></a-form-item><a-form-item :label="t('admin.mxk.password.displayName')"><a-input v-model:value="passwordForm.displayName" disabled /></a-form-item><a-form-item required :label="t('admin.mxk.password.password')"><a-input-group compact><a-input v-model:value="passwordForm.password" :type="passwordVisible ? 'text' : 'password'" style="width: calc(100% - 72px)" /><a-button type="primary" @click="generatePassword">{{ t('admin.mxk.text.generate') }}</a-button></a-input-group></a-form-item><a-form-item required :label="t('admin.mxk.password.confirmPassword')"><a-input-password v-model:value="passwordForm.confirmPassword" /></a-form-item></a-form>
    </a-modal>
    <a-modal v-model:open="mfaOpen" :title="t('ui.userAuthnType')" width="450px" :confirm-loading="mfaSaving" :ok-text="t('admin.mxk.text.submit')" :cancel-text="t('admin.mxk.text.close')" @ok="saveMfa">
      <a-form :label-col="{ span: 7 }" :wrapper-col="{ span: 17 }"><a-form-item :label="t('admin.mxk.users.username')"><a-input v-model:value="mfaForm.username" disabled /></a-form-item><a-form-item :label="t('admin.mxk.users.displayName')"><a-input v-model:value="mfaForm.displayName" disabled /></a-form-item><a-form-item required :label="t('ui.userAuthnType')"><a-select v-model:value="mfaForm.authnType"><a-select-option v-for="value in ['0','1','2','3']" :key="value" :value="value">{{ t(`admin.mxk.users.authnType.${value}`) }}</a-select-option></a-select></a-form-item></a-form>
    </a-modal>
  </DefaultLayout>
</template>

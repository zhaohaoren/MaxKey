<script setup lang="ts">
import { CheckCircleFilled } from '@ant-design/icons-vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import IdentityTree from '../components/IdentityTree.vue'
import OrganizationEditor from '../components/OrganizationEditor.vue'
import { adminDelete, adminGet } from '../api'
import { buildOrganizationTree, normalizeIdentityPage, type IdentityRow, type IdentityTreeNode } from '../identity'

const { t } = useI18n({ useScope: 'global' })
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

const rowSelection = computed(() => ({
  selectedRowKeys: selectedKeys.value,
  getCheckboxProps: (row: IdentityRow) => ({ disabled: row.disabled }),
  onChange: (keys: Array<string | number>) => { selectedKeys.value = keys },
}))
const rowKey = (row: IdentityRow) => String(row.id)

const typeLabels: Record<string, string> = {
  company: 'admin.mxk.organizations.type.company', division: 'admin.mxk.organizations.type.division',
  department: 'admin.mxk.organizations.type.department', team: 'admin.mxk.organizations.type.team',
  entity: 'admin.mxk.organizations.type.entity', virtual: 'admin.mxk.organizations.type.virtual',
}

async function load(reset = false) {
  if (reset) page.current = 1
  loading.value = true
  error.value = ''
  selectedKeys.value = []
  try {
    const result = normalizeIdentityPage(await adminGet('/orgs/fetch', {
      orgName: search.value, parentId: selectedNode.value?.key || '', pageNumber: page.current, pageSize: page.pageSize,
    }))
    rows.value = result.rows
    page.total = result.total
  } catch (err) {
    rows.value = []
    page.total = 0
    error.value = err instanceof Error ? err.message : '组织列表加载失败'
  } finally {
    loading.value = false
  }
}

async function loadTree() {
  try {
    nodes.value = buildOrganizationTree(await adminGet('/orgs/tree'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : '组织树加载失败')
  }
}

function selectNode(node: IdentityTreeNode) {
  selectedNode.value = node
  void load(true)
}

function openEditor(row?: IdentityRow) {
  editingId.value = row?.id ? String(row.id) : ''
  editorOpen.value = true
}

function canDelete(row: IdentityRow) {
  return row.parentId != null && row.parentId !== '-1' && row.parentId !== '0' && String(row.id) !== String(row.instId)
}

function confirmDelete(ids: Array<string | number>) {
  if (!ids.length) return
  Modal.confirm({
    title: t('admin.mxk.text.delete.popconfirm.title'),
    cancelText: t('admin.mxk.text.delete.popconfirm.cancelText'),
    okText: t('admin.mxk.text.delete.popconfirm.okText'), okType: 'danger',
    async onOk() {
      try {
        await adminDelete('/orgs/delete', { ids: ids.join(',') })
        message.success(t('admin.mxk.alert.delete.success'))
        await Promise.all([load(), loadTree()])
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

async function onSaved() {
  await Promise.all([load(), loadTree()])
}

onMounted(() => { void Promise.all([load(), loadTree()]) })
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t('admin.mxk.menu.identities.organizations') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card :bordered="false" class="identity-search-card">
      <a-form layout="inline" @submit.prevent="load(true)">
        <a-row :gutter="{ xs: 8, sm: 8, md: 24, lg: 24, xl: 48, xxl: 48 }" class="identity-search-row">
          <a-col :xs="24" :md="16"><a-form-item :label="t('admin.mxk.organizations.name')"><a-input v-model:value="search" /></a-form-item></a-col>
          <a-col :xs="24" :md="8" class="identity-search-action"><a-button type="primary" html-type="submit">{{ t('admin.mxk.text.query') }}</a-button></a-col>
        </a-row>
      </a-form>
    </a-card>
    <a-card :bordered="false">
      <div class="identity-toolbar">
        <a-button type="primary" @click="openEditor()">{{ t('admin.mxk.text.add') }}</a-button>
        <a-button danger type="primary" :disabled="!selectedKeys.length" @click="confirmDelete(selectedKeys)">{{ t('admin.mxk.text.batchDelete') }}</a-button>
      </div>
      <a-row :gutter="24">
        <a-col :xs="24" :md="6" class="identity-tree-column"><IdentityTree :nodes="nodes" :selected-key="selectedNode?.key" @select="selectNode" /></a-col>
        <a-col :xs="24" :md="18" class="identity-table-column">
          <a-table size="small" bordered :loading="loading" :data-source="rows" :row-key="rowKey" :row-selection="rowSelection"
            :pagination="{ current: page.current, pageSize: page.pageSize, total: page.total, showSizeChanger: true, pageSizeOptions: ['10', '20', '50'] }" :scroll="{ x: 800 }" @change="onTableChange">
            <a-table-column key="orgCode" data-index="orgCode" :title="t('admin.mxk.organizations.code')" />
            <a-table-column key="orgName" data-index="orgName" :title="t('admin.mxk.organizations.name')" />
            <a-table-column key="type" :title="t('ui.organizationType')" align="center"><template #default="{ record }">{{ t(typeLabels[String(record.type)] || String(record.type || '')) }}</template></a-table-column>
            <a-table-column key="sortIndex" data-index="sortIndex" :title="t('admin.mxk.text.sortIndex')" align="center" width="90" />
            <a-table-column key="status" :title="t('ui.status')" align="center" width="80"><template #default="{ record }"><CheckCircleFilled v-if="Number(record.status) === 1" class="identity-status-active" /></template></a-table-column>
            <a-table-column key="action" :title="t('admin.mxk.text.action')" align="center" width="170" fixed="right">
              <template #default="{ record }"><a-space><a-button size="small" @click="openEditor(record)">{{ t('admin.mxk.text.edit') }}</a-button><a-button v-if="canDelete(record)" size="small" danger @click="confirmDelete([record.id])">{{ t('ui.delete') }}</a-button></a-space></template>
            </a-table-column>
          </a-table>
        </a-col>
      </a-row>
    </a-card>
    <OrganizationEditor v-model:open="editorOpen" :organization-id="editingId" :parent-key="selectedNode?.key" :nodes="nodes" @saved="onSaved" />
  </DefaultLayout>
</template>

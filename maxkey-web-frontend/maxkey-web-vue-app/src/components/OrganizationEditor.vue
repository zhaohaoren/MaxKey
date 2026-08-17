<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { adminGet, adminPost, adminPut } from '../api'
import { findTreeNode, isEnabled, type IdentityRow, type IdentityTreeNode } from '../identity'

const props = defineProps<{
  open: boolean
  organizationId?: string
  parentKey?: string
  nodes: IdentityTreeNode[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const { t } = useI18n({ useScope: 'global' })
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('basic')
const form = reactive<IdentityRow>({})

function resetForm() {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, { type: 'department', sortIndex: 11, status: true })
  if (props.parentKey) {
    const parent = findTreeNode(props.nodes, props.parentKey)
    form.parentId = props.parentKey
    form.parentName = parent?.title || ''
  }
}

async function initialize() {
  resetForm()
  activeTab.value = 'basic'
  if (!props.organizationId) return
  loading.value = true
  try {
    const detail = await adminGet<IdentityRow>(`/orgs/get/${props.organizationId}`)
    Object.assign(form, detail, { status: isEnabled(detail.status) })
  } catch (error) {
    message.error(error instanceof Error ? error.message : '组织信息加载失败')
    emit('update:open', false)
  } finally {
    loading.value = false
  }
}

function onParentChange(key: string) {
  form.parentName = findTreeNode(props.nodes, key)?.title || ''
}

async function save() {
  const required = [
    ['orgCode', t('admin.mxk.organizations.code')],
    ['orgName', t('admin.mxk.organizations.name')],
    ['fullName', t('admin.mxk.organizations.fullName')],
    ['type', t('ui.organizationType')],
  ].find(([key]) => !String(form[key] ?? '').trim())
  if (required) {
    message.warning(`请填写${required[1]}`)
    return
  }
  saving.value = true
  try {
    const payload = { ...form, status: form.status ? 1 : 0 }
    if (props.organizationId) await adminPut('/orgs/update', payload)
    else await adminPost('/orgs/add', payload)
    message.success(t(props.organizationId ? 'admin.mxk.alert.update.success' : 'admin.mxk.alert.add.success'))
    emit('update:open', false)
    emit('saved')
  } catch (error) {
    message.error(error instanceof Error ? error.message : t(props.organizationId ? 'admin.mxk.alert.update.error' : 'admin.mxk.alert.add.error'))
  } finally {
    saving.value = false
  }
}

watch(() => props.open, value => { if (value) void initialize() })
</script>

<template>
  <a-modal
    :open="open"
    :title="t(organizationId ? 'admin.mxk.text.edit' : 'admin.mxk.text.add')"
    :confirm-loading="saving"
    width="620px"
    :ok-text="t('admin.mxk.text.submit')"
    :cancel-text="t('admin.mxk.text.close')"
    @ok="save"
    @cancel="emit('update:open', false)"
  >
    <a-spin :spinning="loading">
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="basic" :tab="t('admin.mxk.organizations.tab.basic')">
          <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-form-item required :label="t('admin.mxk.organizations.code')"><a-input v-model:value="form.orgCode" /></a-form-item>
            <a-form-item required :label="t('admin.mxk.organizations.name')"><a-input v-model:value="form.orgName" /></a-form-item>
            <a-form-item required :label="t('admin.mxk.organizations.fullName')"><a-input v-model:value="form.fullName" /></a-form-item>
            <a-form-item required :label="t('ui.organizationType')">
              <a-select v-model:value="form.type">
                <a-select-option value="company">{{ t('admin.mxk.organizations.type.company') }}</a-select-option>
                <a-select-option value="division">{{ t('admin.mxk.organizations.type.division') }}</a-select-option>
                <a-select-option value="department">{{ t('admin.mxk.organizations.type.department') }}</a-select-option>
                <a-select-option value="team">{{ t('admin.mxk.organizations.type.team') }}</a-select-option>
                <a-select-option value="entity">{{ t('admin.mxk.organizations.type.entity') }}</a-select-option>
                <a-select-option value="virtual">{{ t('admin.mxk.organizations.type.virtual') }}</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item :label="t('admin.mxk.organizations.parentName')">
              <a-tree-select v-model:value="form.parentId" show-search tree-default-expand-all :tree-data="nodes" @change="onParentChange" />
            </a-form-item>
            <a-form-item :label="t('admin.mxk.text.sortIndex')"><a-input v-model:value="form.sortIndex" /></a-form-item>
            <a-form-item :label="t('ui.status')"><a-switch v-model:checked="form.status" checked-children="✓" un-checked-children="×" /></a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="extra" :tab="t('admin.mxk.organizations.tab.extra')">
          <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-form-item :label="t('admin.mxk.organizations.codePath')"><a-input v-model:value="form.codePath" /></a-form-item>
            <a-form-item :label="t('admin.mxk.organizations.namePath')"><a-input v-model:value="form.namePath" /></a-form-item>
            <a-form-item :label="t('admin.mxk.organizations.level')"><a-input v-model:value="form.level" /></a-form-item>
            <a-form-item :label="t('admin.mxk.organizations.division')"><a-input v-model:value="form.division" /></a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="address" :tab="t('admin.mxk.organizations.tab.address')">
          <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-form-item v-for="key in ['country', 'region', 'locality', 'street', 'address']" :key="key" :label="t(`admin.mxk.organizations.${key}`)">
              <a-input v-model:value="form[key]" />
            </a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="contact" :tab="t('admin.mxk.organizations.tab.contact')">
          <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
            <a-form-item v-for="key in ['contact', 'phone', 'email', 'fax', 'postalCode']" :key="key" :label="t(`admin.mxk.organizations.${key}`)">
              <a-input v-model:value="form[key]" />
            </a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </a-modal>
</template>

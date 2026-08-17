<script setup lang="ts">
import { PlusOutlined } from '@ant-design/icons-vue'
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { adminGet, adminPost, adminPut, postFormData } from '../api'
import { findTreeNode, type IdentityRow, type IdentityTreeNode } from '../identity'

interface UserField { key: string; label: string }

const props = defineProps<{
  open: boolean
  userId?: string
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
const generating = ref(false)
const activeTab = ref('basic')
const form = reactive<IdentityRow>({})
const previewImage = ref('')
const fileList = ref<Array<{ uid: string; name: string; status: string; url?: string }>>([])

const personalFields: UserField[] = [
  { key: 'familyName', label: 'familyName' }, { key: 'middleName', label: 'middleName' },
  { key: 'givenName', label: 'givenName' }, { key: 'nickName', label: 'nickName' },
  { key: 'idCardNo', label: 'idCardNo' }, { key: 'birthDate', label: 'birthDate' },
  { key: 'education', label: 'education' }, { key: 'graduateFrom', label: 'graduateFrom' },
  { key: 'graduateDate', label: 'graduateDate' }, { key: 'startWorkDate', label: 'startWorkDate' },
  { key: 'timeZone', label: 'timeZone' }, { key: 'preferredLanguage', label: 'preferredLanguage' },
  { key: 'webSite', label: 'website' }, { key: 'defineIm', label: 'ims' },
]

const businessFields: UserField[] = [
  { key: 'organization', label: 'organization' }, { key: 'division', label: 'division' },
  { key: 'costCenter', label: 'costCenter' }, { key: 'jobLevel', label: 'jobLevel' },
  { key: 'jobTitle', label: 'jobTitle' }, { key: 'manager', label: 'manager' },
  { key: 'assistant', label: 'assistant' }, { key: 'workOfficeName', label: 'workOfficeName' },
  { key: 'entryDate', label: 'entryDate' }, { key: 'quitDate', label: 'quitDate' },
  { key: 'workPhoneNumber', label: 'workPhoneNumber' }, { key: 'workEmail', label: 'workEmail' },
  { key: 'workCountry', label: 'workCountry' }, { key: 'workRegion', label: 'workRegion' },
  { key: 'workLocality', label: 'workLocality' }, { key: 'workStreetAddress', label: 'workStreetAddress' },
  { key: 'workPostalCode', label: 'workPostalCode' }, { key: 'workFax', label: 'workFax' },
]

const homeFields: UserField[] = [
  { key: 'homeEmail', label: 'homeEmail' }, { key: 'homePhoneNumber', label: 'homePhoneNumber' },
  { key: 'homeFax', label: 'homeFax' }, { key: 'homePostalCode', label: 'homePostalCode' },
  { key: 'homeCountry', label: 'homeCountry' }, { key: 'homeRegion', label: 'homeRegion' },
  { key: 'homeLocality', label: 'homeLocality' }, { key: 'homeStreetAddress', label: 'homeStreetAddress' },
]

const editing = computed(() => Boolean(props.userId))

function resetForm() {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, {
    status: '1', sortIndex: 1, gender: '1', married: '0', idType: '0',
    userType: 'EMPLOYEE', userState: 'RESIDENT',
  })
  previewImage.value = ''
  fileList.value = []
  if (props.parentKey) {
    form.departmentId = props.parentKey
    form.department = findTreeNode(props.nodes, props.parentKey)?.title || ''
  }
}

async function initialize() {
  resetForm()
  activeTab.value = 'basic'
  if (!props.userId) return
  loading.value = true
  try {
    const detail = await adminGet<IdentityRow>(`/users/get/${props.userId}`)
    Object.assign(form, detail, {
      status: String(detail.status ?? '1'), gender: String(detail.gender ?? '1'),
      married: String(detail.married ?? '0'), idType: String(detail.idType ?? '0'),
    })
    previewImage.value = String(detail.pictureBase64 || detail.picture || '')
    if (previewImage.value) fileList.value = [{ uid: String(detail.id || '-1'), name: String(detail.displayName || 'avatar'), status: 'done', url: previewImage.value }]
  } catch (error) {
    message.error(error instanceof Error ? error.message : '用户信息加载失败')
    emit('update:open', false)
  } finally {
    loading.value = false
  }
}

async function generatePassword() {
  generating.value = true
  try {
    form.password = await adminGet<string>('/users/randomPassword')
  } catch (error) {
    message.error(error instanceof Error ? error.message : '随机密码生成失败')
  } finally {
    generating.value = false
  }
}

function departmentChanged(key: string) {
  form.department = findTreeNode(props.nodes, key)?.title || ''
}

async function uploadAvatar(options: { file: File; onSuccess?: (data: unknown) => void; onError?: (error: Error) => void }) {
  const body = new FormData()
  body.append('uploadFile', options.file)
  try {
    const pictureId = await postFormData<string>('/file/upload/', body)
    form.pictureId = pictureId
    previewImage.value = URL.createObjectURL(options.file)
    fileList.value = [{ uid: pictureId || String(Date.now()), name: options.file.name, status: 'done', url: previewImage.value }]
    options.onSuccess?.(pictureId)
  } catch (error) {
    const uploadError = error instanceof Error ? error : new Error('头像上传失败')
    message.error(uploadError.message)
    options.onError?.(uploadError)
  }
}

function removeAvatar() {
  fileList.value = []
  previewImage.value = ''
  form.pictureId = ''
  return true
}

async function save() {
  const required = [
    ['displayName', t('admin.mxk.users.displayName')], ['username', t('admin.mxk.users.username')],
    ...(!editing.value ? [['password', t('admin.mxk.users.password')]] : []),
    ['userType', t('ui.userType')], ['userState', t('ui.userState')],
  ].find(([key]) => !String(form[key] ?? '').trim())
  if (required) {
    message.warning(`请填写${required[1]}`)
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form, status: Number(form.status), gender: Number(form.gender),
      married: Number(form.married), idType: Number(form.idType), sortIndex: Number(form.sortIndex || 1),
    }
    if (editing.value) await adminPut('/users/update', payload)
    else await adminPost('/users/add', payload)
    message.success(t(editing.value ? 'admin.mxk.alert.update.success' : 'admin.mxk.alert.add.success'))
    emit('update:open', false)
    emit('saved')
  } catch (error) {
    message.error(error instanceof Error ? error.message : t(editing.value ? 'admin.mxk.alert.update.error' : 'admin.mxk.alert.add.error'))
  } finally {
    saving.value = false
  }
}

watch(() => props.open, value => { if (value) void initialize() })
</script>

<template>
  <a-modal :open="open" :title="t(editing ? 'admin.mxk.text.edit' : 'admin.mxk.text.add')" width="750px" :confirm-loading="saving"
    :ok-text="t('admin.mxk.text.submit')" :cancel-text="t('admin.mxk.text.close')" @ok="save" @cancel="emit('update:open', false)">
    <a-spin :spinning="loading">
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane key="basic" :tab="t('admin.mxk.users.tab.basic')">
          <a-row :gutter="24">
            <a-col :span="15">
              <a-form :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
                <a-form-item required :label="t('admin.mxk.users.displayName')"><a-input v-model:value="form.displayName" /></a-form-item>
                <a-form-item required :label="t('admin.mxk.users.username')"><a-input v-model:value="form.username" /></a-form-item>
                <a-form-item v-if="!editing" required :label="t('admin.mxk.users.password')">
                  <a-input-group compact><a-input v-model:value="form.password" style="width: calc(100% - 72px)" /><a-button type="primary" :loading="generating" @click="generatePassword">{{ t('admin.mxk.text.generate') }}</a-button></a-input-group>
                </a-form-item>
                <a-form-item :label="t('ui.userGender')"><a-radio-group v-model:value="form.gender" button-style="solid"><a-radio-button value="2">{{ t('admin.mxk.users.gender.male') }}</a-radio-button><a-radio-button value="1">{{ t('admin.mxk.users.gender.female') }}</a-radio-button></a-radio-group></a-form-item>
              </a-form>
            </a-col>
            <a-col :span="9">
              <a-form-item :label="t('admin.mxk.users.picture')">
                <a-upload v-model:file-list="fileList" list-type="picture-card" :custom-request="uploadAvatar" :max-count="1" @remove="removeAvatar">
                  <div v-if="fileList.length < 1"><PlusOutlined /><div class="identity-upload-label">{{ t('admin.mxk.text.upload') }}</div></div>
                </a-upload>
              </a-form-item>
            </a-col>
          </a-row>
          <a-form class="identity-two-column-form" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-form-item :label="t('admin.mxk.users.employeeNumber')"><a-input v-model:value="form.employeeNumber" /></a-form-item>
            <a-form-item :label="t('admin.mxk.users.windowsAccount')"><a-input v-model:value="form.windowsAccount" /></a-form-item>
            <a-form-item :label="t('admin.mxk.users.mobile')"><a-input v-model:value="form.mobile" /></a-form-item>
            <a-form-item :label="t('admin.mxk.users.email')"><a-input v-model:value="form.email" /></a-form-item>
            <a-form-item required :label="t('ui.userType')"><a-select v-model:value="form.userType"><a-select-option v-for="value in ['EMPLOYEE','SUPPLIER','CUSTOMER','CONTRACTOR','DEALER','PARTNER','EXTERNAL','INTERN','TEMP']" :key="value" :value="value">{{ t(`admin.mxk.users.userType.${value.toLowerCase()}`) }}</a-select-option></a-select></a-form-item>
            <a-form-item required :label="t('ui.userState')"><a-select v-model:value="form.userState"><a-select-option v-for="value in ['RESIDENT','WITHDRAWN','INACTIVE','RETIREE']" :key="value" :value="value">{{ t(`admin.mxk.users.userstate.${value.toLowerCase()}`) }}</a-select-option></a-select></a-form-item>
            <a-form-item required :label="t('admin.mxk.text.sortIndex')"><a-input-number v-model:value="form.sortIndex" :min="1" :max="100000" /></a-form-item>
            <a-form-item required :label="t('ui.status')"><a-select v-model:value="form.status"><a-select-option v-for="value in ['1','2','4','5','9']" :key="value" :value="value">{{ t(`admin.mxk.users.status.${({1:'active',2:'inactive',4:'forbidden',5:'lock',9:'delete'} as Record<string,string>)[value]}`) }}</a-select-option></a-select></a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="personal" :tab="t('admin.mxk.users.tab.personal')">
          <a-form class="identity-two-column-form" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-form-item v-for="field in personalFields.slice(0, 4)" :key="field.key" :label="t(`admin.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
            <a-form-item :label="t('ui.userIdType')"><a-select v-model:value="form.idType"><a-select-option v-for="value in ['0','1','2','3','4']" :key="value" :value="value">{{ t(`admin.mxk.users.idtype.${({0:'unknown',1:'idcard',2:'passport',3:'studentcard',4:'militarycard'} as Record<string,string>)[value]}`) }}</a-select-option></a-select></a-form-item>
            <a-form-item :label="t('admin.mxk.users.idCardNo')"><a-input v-model:value="form.idCardNo" /></a-form-item>
            <a-form-item :label="t('ui.userMarried')"><a-select v-model:value="form.married"><a-select-option v-for="value in ['0','1','2','3','4']" :key="value" :value="value">{{ t(`admin.mxk.users.married.${({0:'unknown',1:'single',2:'married',3:'divorce',4:'widowed'} as Record<string,string>)[value]}`) }}</a-select-option></a-select></a-form-item>
            <a-form-item v-for="field in personalFields.slice(5)" :key="field.key" :label="t(`admin.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="business" :tab="t('ui.userBusinessTab')">
          <a-form class="identity-two-column-form" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-form-item v-for="field in businessFields.slice(0, 2)" :key="field.key" :label="t(`admin.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
            <a-form-item :label="t('admin.mxk.users.departmentId')"><a-input v-model:value="form.departmentId" disabled /></a-form-item>
            <a-form-item :label="t('admin.mxk.users.department')"><a-tree-select v-model:value="form.departmentId" show-search tree-default-expand-all :tree-data="nodes" @change="departmentChanged" /></a-form-item>
            <a-form-item v-for="field in businessFields.slice(2)" :key="field.key" :label="t(`admin.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
          </a-form>
        </a-tab-pane>
        <a-tab-pane key="home" :tab="t('admin.mxk.users.tab.home')">
          <a-form class="identity-two-column-form" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
            <a-form-item v-for="field in homeFields" :key="field.key" :label="t(`admin.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </a-modal>
</template>

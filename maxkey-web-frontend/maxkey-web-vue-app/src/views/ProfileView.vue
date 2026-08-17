<script setup lang="ts">
import { PlusOutlined } from '@ant-design/icons-vue'
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import DefaultLayout from '../components/DefaultLayout.vue'
import { auth } from '../auth'
import { get, postFormData, put } from '../api'
import type { IdentityRow } from '../identity'

interface ProfileField { key: string; label: string }

const { t } = useI18n({ useScope: 'global' })
const loading = ref(false)
const saving = ref(false)
const activeTab = ref('basic')
const error = ref('')
const form = reactive<IdentityRow>({})
const fileList = ref<Array<{ uid: string; name: string; status: string; url?: string }>>([])

const personalFields: ProfileField[] = [
  { key: 'familyName', label: 'familyName' }, { key: 'middleName', label: 'middleName' },
  { key: 'givenName', label: 'givenName' }, { key: 'nickName', label: 'nickName' },
  { key: 'idCardNo', label: 'idCardNo' }, { key: 'birthDate', label: 'birthDate' },
  { key: 'education', label: 'education' }, { key: 'graduateFrom', label: 'graduateFrom' },
  { key: 'graduateDate', label: 'graduateDate' }, { key: 'startWorkDate', label: 'startWorkDate' },
  { key: 'timeZone', label: 'timeZone' }, { key: 'preferredLanguage', label: 'preferredLanguage' },
  { key: 'webSite', label: 'website' }, { key: 'defineIm', label: 'ims' },
]

const businessFields: ProfileField[] = [
  { key: 'organization', label: 'organization' }, { key: 'division', label: 'division' },
  { key: 'departmentId', label: 'departmentId' }, { key: 'department', label: 'department' },
  { key: 'costCenter', label: 'costCenter' }, { key: 'jobLevel', label: 'jobLevel' },
  { key: 'jobTitle', label: 'jobTitle' }, { key: 'manager', label: 'manager' },
  { key: 'assistant', label: 'assistant' }, { key: 'workOfficeName', label: 'workOfficeName' },
  { key: 'entryDate', label: 'entryDate' }, { key: 'quitDate', label: 'quitDate' },
]

const businessExtraFields: ProfileField[] = [
  { key: 'workPhoneNumber', label: 'workPhoneNumber' }, { key: 'workEmail', label: 'workEmail' },
  { key: 'workCountry', label: 'workCountry' }, { key: 'workRegion', label: 'workRegion' },
  { key: 'workLocality', label: 'workLocality' }, { key: 'workStreetAddress', label: 'workStreetAddress' },
  { key: 'workPostalCode', label: 'workPostalCode' }, { key: 'workFax', label: 'workFax' },
]

const homeFields: ProfileField[] = [
  { key: 'homeEmail', label: 'homeEmail' }, { key: 'homePhoneNumber', label: 'homePhoneNumber' },
  { key: 'homeFax', label: 'homeFax' }, { key: 'homePostalCode', label: 'homePostalCode' },
  { key: 'homeCountry', label: 'homeCountry' }, { key: 'homeRegion', label: 'homeRegion' },
  { key: 'homeLocality', label: 'homeLocality' }, { key: 'homeStreetAddress', label: 'homeStreetAddress' },
]

function normalizeProfile(data: IdentityRow) {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, data, {
    gender: String(data.gender ?? '1'),
    status: String(data.status ?? '1'),
    married: String(data.married ?? '0'),
    idType: String(data.idType ?? '0'),
  })
  const picture = String(data.pictureBase64 || data.picture || '')
  fileList.value = picture ? [{ uid: String(data.id || '-1'), name: String(data.displayName || 'avatar'), status: 'done', url: picture }] : []
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    normalizeProfile(await get<IdentityRow>('/users/profile/get'))
  } catch (err) {
    error.value = err instanceof Error ? err.message : t('ui.profileLoadError')
  } finally {
    loading.value = false
  }
}

async function uploadAvatar(options: { file: File; onSuccess?: (data: unknown) => void; onError?: (error: Error) => void }) {
  const body = new FormData()
  body.append('uploadFile', options.file)
  try {
    const pictureId = await postFormData<string>('/file/upload/', body)
    form.pictureId = pictureId
    const preview = URL.createObjectURL(options.file)
    fileList.value = [{ uid: pictureId || String(Date.now()), name: options.file.name, status: 'done', url: preview }]
    options.onSuccess?.(pictureId)
  } catch (err) {
    const error = err instanceof Error ? err : new Error(t('ui.avatarUploadError'))
    message.error(error.message)
    options.onError?.(error)
  }
}

function removeAvatar() {
  fileList.value = []
  form.pictureId = ''
  return true
}

async function save() {
  if (!String(form.displayName || '').trim()) {
    message.warning(t('ui.displayNameRequired'))
    return
  }
  saving.value = true
  try {
    await put('/users/profile/update', {
      ...form,
      gender: Number(form.gender),
      status: Number(form.status),
      married: Number(form.married),
      idType: Number(form.idType),
      sortIndex: Number(form.sortIndex || 1),
    })
    const fresh = await get<IdentityRow>('/users/profile/get')
    normalizeProfile(fresh)
    const token = auth.token.value
    if (token) auth.signIn({ ...token, displayName: String(fresh.displayName || token.displayName || ''), email: String(fresh.email || token.email || ''), avatar: String(fresh.pictureBase64 || token.avatar || '') })
    message.success(t('portal.mxk.alert.operate.success'))
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('portal.mxk.alert.operate.error'))
  } finally {
    saving.value = false
  }
}

onMounted(() => { void load() })
</script>

<template>
  <DefaultLayout mode="portal">
    <div class="alain-default__content-title"><h1>{{ t('portal.mxk.menu.config.profile') }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card class="portal-profile-card">
      <a-spin :spinning="loading">
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="basic" :tab="t('portal.mxk.users.tab.basic')">
            <a-form class="portal-profile-grid" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-form-item required :label="t('portal.mxk.users.displayName')"><a-input v-model:value="form.displayName" /></a-form-item>
              <a-form-item required :label="t('portal.mxk.users.username')"><a-input v-model:value="form.username" disabled /></a-form-item>
              <a-form-item :label="t('ui.userGender')"><a-radio-group v-model:value="form.gender" button-style="solid"><a-radio-button value="2">{{ t('portal.mxk.users.gender.male') }}</a-radio-button><a-radio-button value="1">{{ t('portal.mxk.users.gender.female') }}</a-radio-button></a-radio-group></a-form-item>
              <a-form-item class="portal-profile-avatar" :label="t('portal.mxk.users.picture')">
                <a-upload v-model:file-list="fileList" list-type="picture-card" :custom-request="uploadAvatar" :max-count="1" @remove="removeAvatar">
                  <div v-if="fileList.length < 1"><PlusOutlined /><div class="profile-upload-label">{{ t('portal.mxk.text.upload') }}</div></div>
                </a-upload>
              </a-form-item>
              <a-form-item :label="t('portal.mxk.users.employeeNumber')"><a-input v-model:value="form.employeeNumber" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.users.windowsAccount')"><a-input v-model:value="form.windowsAccount" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.users.mobile')"><a-input v-model:value="form.mobile" disabled /></a-form-item>
              <a-form-item :label="t('portal.mxk.users.email')"><a-input v-model:value="form.email" disabled /></a-form-item>
              <a-form-item :label="t('ui.userType')"><a-select v-model:value="form.userType" disabled><a-select-option v-for="value in ['EMPLOYEE','SUPPLIER','CUSTOMER','CONTRACTOR','DEALER','PARTNER','EXTERNAL','INTERN','TEMP']" :key="value" :value="value">{{ t(`portal.mxk.users.userType.${value.toLowerCase()}`) }}</a-select-option></a-select></a-form-item>
              <a-form-item :label="t('ui.userState')"><a-select v-model:value="form.userState" disabled><a-select-option v-for="value in ['RESIDENT','WITHDRAWN','INACTIVE','RETIREE']" :key="value" :value="value">{{ t(`portal.mxk.users.userstate.${value.toLowerCase()}`) }}</a-select-option></a-select></a-form-item>
              <a-form-item :label="t('portal.mxk.text.sortIndex')"><a-input-number v-model:value="form.sortIndex" disabled /></a-form-item>
              <a-form-item :label="t('ui.status')"><a-select v-model:value="form.status" disabled><a-select-option v-for="value in ['1','2','4','5','9']" :key="value" :value="value">{{ t(`portal.mxk.users.status.${({1:'active',2:'inactive',4:'forbidden',5:'lock',9:'delete'} as Record<string,string>)[value]}`) }}</a-select-option></a-select></a-form-item>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="personal" :tab="t('portal.mxk.users.tab.personal')">
            <a-form class="portal-profile-grid" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-form-item v-for="field in personalFields.slice(0, 4)" :key="field.key" :label="t(`portal.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
              <a-form-item :label="t('ui.userIdType')"><a-select v-model:value="form.idType"><a-select-option v-for="value in ['0','1','2','3','4']" :key="value" :value="value">{{ t(`portal.mxk.users.idtype.${({0:'unknown',1:'idcard',2:'passport',3:'studentcard',4:'militarycard'} as Record<string,string>)[value]}`) }}</a-select-option></a-select></a-form-item>
              <a-form-item :label="t('portal.mxk.users.idCardNo')"><a-input v-model:value="form.idCardNo" /></a-form-item>
              <a-form-item :label="t('ui.userMarried')"><a-select v-model:value="form.married"><a-select-option v-for="value in ['0','1','2','3','4']" :key="value" :value="value">{{ t(`portal.mxk.users.married.${({0:'unknown',1:'single',2:'married',3:'divorce',4:'widowed'} as Record<string,string>)[value]}`) }}</a-select-option></a-select></a-form-item>
              <a-form-item v-for="field in personalFields.slice(5)" :key="field.key" :label="t(`portal.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="business" :tab="t('ui.userBusinessTab')">
            <a-form class="portal-profile-grid" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-form-item v-for="field in businessFields" :key="field.key" :label="t(`portal.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" disabled /></a-form-item>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="business-extra" :tab="t('portal.mxk.users.tab.business.extra')">
            <a-form class="portal-profile-grid" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-form-item v-for="field in businessExtraFields" :key="field.key" :label="t(`portal.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="home" :tab="t('portal.mxk.users.tab.home')">
            <a-form class="portal-profile-grid" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
              <a-form-item v-for="field in homeFields" :key="field.key" :label="t(`portal.mxk.users.${field.label}`)"><a-input v-model:value="form[field.key]" /></a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
        <div class="portal-profile-submit"><a-button type="primary" :loading="saving" @click="save">{{ t('portal.mxk.text.submit') }}</a-button></div>
      </a-spin>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.portal-profile-card { width: 100%; }
.portal-profile-card :deep(.ant-tabs-content-holder) { width: 100%; max-width: 1120px; margin: 0 auto; }
.portal-profile-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 32px; }
.portal-profile-grid :deep(.ant-form-item) { min-width: 0; }
.portal-profile-grid :deep(.ant-form-item-label) { text-align: right; }
.portal-profile-grid :deep(.ant-form-item-label > label) { width: 100%; justify-content: flex-end; }
.portal-profile-grid :deep(.ant-input-number), .portal-profile-grid :deep(.ant-select) { width: 100%; }
.portal-profile-avatar :deep(.ant-form-item-control-input) { min-height: 104px; }
.portal-profile-submit { width: 100%; max-width: 1120px; margin: 16px auto 0; text-align: right; }
.profile-upload-label { margin-top: 8px; }
@media (max-width: 680px) { .portal-profile-grid { grid-template-columns: 1fr; } }
</style>

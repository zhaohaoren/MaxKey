<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import DefaultLayout from '../components/DefaultLayout.vue'
import { adminGet, adminPut } from '../api'

type FormValue = string | number | boolean | null | undefined
type ConfigForm = Record<string, FormValue>

interface FieldOption {
  label: string
  value: string | number
}

interface ConfigField {
  key: string
  label: string
  type?: 'text' | 'password' | 'number' | 'select' | 'switch'
  required?: boolean
  min?: number
  max?: number
  addonAfter?: string
  options?: FieldOption[]
  switchFormat?: 'number' | 'yes-no'
  visible?: (values: ConfigForm) => boolean
}

interface SingletonDefinition {
  title: string
  base: string
  defaults?: ConfigForm
  fields: ConfigField[]
  testable?: boolean
}

const { t } = useI18n({ useScope: 'global' })
const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const error = ref('')
const form = reactive<ConfigForm>({})

const required = true
const definitions: Record<string, SingletonDefinition> = {
  institutions: {
    title: 'admin.mxk.menu.config.institutions',
    base: '/config/institutions',
    fields: [
      { key: 'name', label: 'admin.mxk.institutions.name', required },
      { key: 'fullName', label: 'admin.mxk.institutions.fullName', required },
      { key: 'logo', label: 'admin.mxk.institutions.logo', required },
      { key: 'defaultUri', label: 'admin.mxk.institutions.defaultUri' },
      { key: 'domain', label: 'admin.mxk.institutions.domain', required },
      { key: 'frontTitle', label: 'admin.mxk.institutions.frontTitle', required },
      { key: 'consoleDomain', label: 'admin.mxk.institutions.consoleDomain', required },
      { key: 'consoleTitle', label: 'admin.mxk.institutions.consoleTitle', required },
      { key: 'contact', label: 'admin.mxk.institutions.contact' },
      { key: 'phone', label: 'admin.mxk.institutions.phone' },
      { key: 'email', label: 'admin.mxk.institutions.email' },
      { key: 'address', label: 'admin.mxk.institutions.address' },
    ],
  },
  ldapcontext: {
    title: 'admin.mxk.menu.config.ldapcontext',
    base: '/config/ldapcontext',
    defaults: { product: 'ActiveDirectory', status: false, accountMapping: false, sslSwitch: false },
    testable: true,
    fields: [
      { key: 'product', label: 'admin.mxk.ldapcontext.product', type: 'select', required, options: ['ActiveDirectory', 'OpenLDAP', 'StandardLDAP'].map(value => ({ label: value, value })) },
      { key: 'status', label: 'ui.status', type: 'switch', switchFormat: 'number', required },
      { key: 'providerUrl', label: 'admin.mxk.ldapcontext.providerUrl', required },
      { key: 'accountMapping', label: 'admin.mxk.ldapcontext.accountMapping', type: 'switch', switchFormat: 'yes-no', required },
      { key: 'principal', label: 'admin.mxk.ldapcontext.principal', required },
      { key: 'credentials', label: 'admin.mxk.ldapcontext.credentials', type: 'password', required },
      { key: 'basedn', label: 'admin.mxk.ldapcontext.basedn', required, visible: values => values.product !== 'ActiveDirectory' },
      { key: 'filters', label: 'admin.mxk.ldapcontext.filters', required, visible: values => values.product !== 'ActiveDirectory' },
      { key: 'msadDomain', label: 'admin.mxk.ldapcontext.msadDomain', visible: values => values.product === 'ActiveDirectory' },
      { key: 'sslSwitch', label: 'admin.mxk.ldapcontext.sslSwitch', type: 'switch', switchFormat: 'number' },
      { key: 'trustStore', label: 'admin.mxk.ldapcontext.trustStore', visible: values => Boolean(values.sslSwitch) },
      { key: 'trustStorePassword', label: 'admin.mxk.ldapcontext.trustStorePassword', visible: values => Boolean(values.sslSwitch) },
    ],
  },
  emailsenders: {
    title: 'admin.mxk.menu.config.emailsenders',
    base: '/config/emailsenders',
    defaults: { protocol: 'smtp', encoding: 'utf-8', status: false, sslSwitch: false },
    fields: [
      { key: 'id', label: 'admin.mxk.text.id', required },
      { key: 'status', label: 'ui.status', type: 'switch', switchFormat: 'number', required },
      { key: 'smtpHost', label: 'admin.mxk.emailsenders.smtpHost', required },
      { key: 'port', label: 'admin.mxk.emailsenders.port', type: 'number', min: 1, max: 99999, required },
      { key: 'account', label: 'admin.mxk.emailsenders.account', required },
      { key: 'credentials', label: 'admin.mxk.emailsenders.credentials', type: 'password', required },
      { key: 'protocol', label: 'admin.mxk.emailsenders.protocol', required },
      { key: 'encoding', label: 'admin.mxk.emailsenders.encoding', required },
      { key: 'sender', label: 'admin.mxk.emailsenders.sender', required },
      { key: 'sslSwitch', label: 'admin.mxk.emailsenders.sslSwitch', type: 'switch', switchFormat: 'number', required },
    ],
  },
  smsprovider: {
    title: 'admin.mxk.menu.config.smsproviders',
    base: '/config/smsprovider',
    defaults: { provider: 'aliyun', status: false },
    fields: [
      {
        key: 'provider', label: 'admin.mxk.smsprovider.provider', type: 'select', required,
        options: ['aliyun', 'tencentcloud', 'neteasesms', 'email'].map(value => ({ label: `admin.mxk.smsprovider.name.${value}`, value })),
      },
      { key: 'status', label: 'ui.status', type: 'switch', switchFormat: 'number', required },
      { key: 'message', label: 'admin.mxk.smsprovider.message', required, visible: values => values.provider !== 'email' },
      { key: 'templateId', label: 'admin.mxk.smsprovider.templateId', required, visible: values => values.provider !== 'email' },
      { key: 'appKey', label: 'admin.mxk.smsprovider.appKey', required, visible: values => values.provider !== 'email' },
      { key: 'appSecret', label: 'admin.mxk.smsprovider.appSecret', type: 'password', required, visible: values => values.provider !== 'email' },
      { key: 'signName', label: 'admin.mxk.smsprovider.signName', required, visible: values => values.provider !== 'email' },
      { key: 'smsSdkAppId', label: 'admin.mxk.smsprovider.smsSdkAppId', visible: values => values.provider === 'tencentcloud' },
    ],
  },
  passwordpolicy: {
    title: 'admin.mxk.menu.config.passwordpolicy',
    base: '/config/passwordpolicy',
    fields: [
      { key: 'minLength', label: 'admin.mxk.passwordpolicy.minLength', type: 'number', min: 0, max: 10, required },
      { key: 'maxLength', label: 'admin.mxk.passwordpolicy.maxLength', type: 'number', min: 0, max: 10, required },
      { key: 'lowerCase', label: 'admin.mxk.passwordpolicy.lowerCase', type: 'number', min: 0, max: 10, required },
      { key: 'upperCase', label: 'admin.mxk.passwordpolicy.upperCase', type: 'number', min: 0, max: 10, required },
      { key: 'digits', label: 'admin.mxk.passwordpolicy.digits', type: 'number', min: 0, max: 10, required },
      { key: 'specialChar', label: 'admin.mxk.passwordpolicy.specialChar', type: 'number', min: 0, max: 10, required },
      { key: 'attempts', label: 'admin.mxk.passwordpolicy.attempts', type: 'number', min: 0, max: 10, required },
      { key: 'duration', label: 'admin.mxk.passwordpolicy.duration', type: 'number', min: 0, max: 720, addonAfter: 'admin.mxk.text.minute', required },
      { key: 'occurances', label: 'admin.mxk.passwordpolicy.occurances', type: 'number', min: 0, max: 10, required },
      { key: 'expiration', label: 'admin.mxk.passwordpolicy.expiration', type: 'number', min: 0, max: 365, addonAfter: 'admin.mxk.text.day', required },
      { key: 'history', label: 'admin.mxk.passwordpolicy.history', type: 'number', min: 0, max: 10, required },
      { key: 'username', label: 'admin.mxk.passwordpolicy.username', type: 'switch', switchFormat: 'number', required },
      { key: 'dictionary', label: 'admin.mxk.passwordpolicy.dictionary', type: 'switch', switchFormat: 'number', required },
      { key: 'alphabetical', label: 'admin.mxk.passwordpolicy.alphabetical', type: 'switch', switchFormat: 'number', required },
      { key: 'numerical', label: 'admin.mxk.passwordpolicy.numerical', type: 'switch', switchFormat: 'number', required },
      { key: 'qwerty', label: 'admin.mxk.passwordpolicy.qwerty', type: 'switch', switchFormat: 'number', required },
    ],
  },
}

const definition = computed(() => definitions[String(route.meta.configPage || route.params.resource || 'institutions')] || definitions.institutions)
const visibleFields = computed(() => definition.value.fields.filter(field => !field.visible || field.visible(form)))

function label(value: string) {
  return value.startsWith('admin.') || value.startsWith('ui.') ? t(value) : value
}

function resetForm() {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, definition.value.defaults || {})
}

function switchToBoolean(field: ConfigField, value: FormValue) {
  if (field.switchFormat === 'yes-no') return value === 'YES'
  return value === 1 || value === '1' || value === true
}

async function load() {
  resetForm()
  loading.value = true
  error.value = ''
  try {
    const data = await adminGet<ConfigForm>(`${definition.value.base}/get`)
    if (data) Object.assign(form, data)
    definition.value.fields.filter(field => field.type === 'switch').forEach(field => {
      form[field.key] = switchToBoolean(field, form[field.key])
    })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '配置加载失败'
  } finally {
    loading.value = false
  }
}

function validate() {
  const missing = visibleFields.value.find(field => field.required && field.key !== 'id' && field.type !== 'switch' && (form[field.key] === undefined || form[field.key] === null || form[field.key] === ''))
  if (!missing) return true
  message.warning(`请填写${label(missing.label)}`)
  return false
}

function payload() {
  const data: ConfigForm = { ...form }
  definition.value.fields.filter(field => field.type === 'switch').forEach(field => {
    data[field.key] = field.switchFormat === 'yes-no' ? (form[field.key] ? 'YES' : 'NO') : (form[field.key] ? 1 : 0)
  })
  return data
}

async function submit() {
  if (!validate()) return
  saving.value = true
  try {
    await adminPut(`${definition.value.base}/update`, payload())
    message.success(t('admin.mxk.alert.update.success'))
    await load()
  } catch (err) {
    message.error(err instanceof Error ? err.message : t('admin.mxk.alert.update.error'))
  } finally {
    saving.value = false
  }
}

async function testConnection() {
  testing.value = true
  try {
    await adminGet(`${definition.value.base}/test`)
    message.success('LDAP 连接测试成功')
  } catch (err) {
    message.error(err instanceof Error ? err.message : 'LDAP 连接测试失败')
  } finally {
    testing.value = false
  }
}

watch(() => route.fullPath, load)
onMounted(load)
</script>

<template>
  <DefaultLayout mode="admin">
    <div class="alain-default__content-title"><h1>{{ t(definition.title) }}</h1></div>
    <a-alert v-if="error" type="error" show-icon :message="error" />
    <a-card class="config-form-card">
      <a-spin :spinning="loading">
        <a-form class="legacy-config-form" :label-col="{ xs: 24, sm: 6 }" :wrapper-col="{ xs: 24, sm: 18 }" @submit.prevent="submit">
          <a-form-item
            v-for="field in visibleFields"
            :key="field.key"
            :label="label(field.label)"
            :required="field.required"
          >
            <a-switch v-if="field.type === 'switch'" v-model:checked="form[field.key]" checked-children="✓" un-checked-children="×" />
            <a-select v-else-if="field.type === 'select'" v-model:value="form[field.key]" class="config-control">
              <a-select-option v-for="option in field.options" :key="option.value" :value="option.value">{{ label(option.label) }}</a-select-option>
            </a-select>
            <a-input-number
              v-else-if="field.type === 'number'"
              v-model:value="form[field.key]"
              :min="field.min"
              :max="field.max"
              :addon-after="field.addonAfter ? label(field.addonAfter) : undefined"
              class="config-number-control"
            />
            <a-input-password v-else-if="field.type === 'password'" v-model:value="form[field.key]" class="config-control" />
            <a-input v-else v-model:value="form[field.key]" :disabled="field.key === 'id'" class="config-control" />
          </a-form-item>
          <div class="config-form-actions">
            <a-space>
              <a-button type="primary" html-type="submit" :loading="saving">{{ t('admin.mxk.text.submit') }}</a-button>
              <a-button v-if="definition.testable" :loading="testing" @click="testConnection">{{ t('admin.mxk.text.test') }}</a-button>
            </a-space>
          </div>
        </a-form>
      </a-spin>
    </a-card>
  </DefaultLayout>
</template>

<style scoped>
.legacy-config-form { max-width: 980px; margin: 0 auto; }
.config-form-actions { display: flex; justify-content: flex-end; margin-top: 16px; }
.config-control { width: min(100%, 640px); }
.config-number-control { width: 180px; }
.config-form-card :deep(.ant-card-body) { padding: 24px; }
@media (max-width: 576px) {
  .config-number-control { width: 100%; }
}
</style>

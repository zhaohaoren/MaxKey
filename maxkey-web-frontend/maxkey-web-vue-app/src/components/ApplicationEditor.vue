<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { PlusOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { adminGet, postFormData } from '../api'

type AppForm = Record<string, any>
type Control = 'input' | 'textarea' | 'select' | 'radio' | 'upload'
interface Option { value: string; label: string }
interface FieldSpec {
  key: string
  label: string
  control?: Control
  options?: Option[]
  required?: boolean
  full?: boolean
  multiple?: boolean
  readonly?: boolean
  suffix?: string
  generate?: boolean
  hidden?: (form: AppForm) => boolean
}
interface TabSpec { key: string; title: string; fields: FieldSpec[] }
interface ExtraAttr { id: string; attr: string; type: string; value: string }
interface AdapterRow { id?: string; name?: string; protocol?: string; adapter?: string; sortIndex?: number }
interface UploadRequest { file: File; onSuccess?: (data: unknown) => void; onError?: (error: Error) => void }

const props = defineProps<{ form: AppForm; editing: boolean }>()
const emit = defineEmits<{ generateSecret: []; generateKey: [field: string, type: string] }>()

const categoryOptions: Option[] = [
  ['none', '暂无'], ['1011', '企业服务/数据分析'], ['1012', '企业服务/电子办公'], ['1013', '企业服务/综合OA'],
  ['1014', '企业服务/工商法律'], ['1015', '企业服务/营销创意'], ['1016', '企业服务/行政服务'], ['1017', '企业服务/企业福利'],
  ['1111', '团队协作/项目管理'], ['1112', '团队协作/敏捷研发'], ['1113', '团队协作/设计工具'], ['1114', '团队协作/待办工具'],
  ['1211', '人力资源/综合人事'], ['1212', '人力资源/招聘管理'], ['1213', '人力资源/背景调查'], ['1214', '人力资源/员工激励'],
  ['1215', '人力资源/企业文化'], ['1311', '考试培训'], ['1411', '商旅出行'], ['1511', '财务管理/综合财务'],
  ['1512', '财务管理/费控报销'], ['1611', '表单流程'], ['1711', '表单流程/业务流程'], ['1712', '表单流程/问卷调研'],
  ['1811', '供应链/资产管理'], ['1812', '供应链/进销存'], ['1911', '客户关系/客户服务'], ['1912', '客户关系/客户管理'],
].map(([value, label]) => ({ value, label }))

const yesNo: Option[] = [{ value: 'yes', label: '是' }, { value: 'no', label: '否' }]
const zeroOne: Option[] = [{ value: '0', label: '否' }, { value: '1', label: '是' }]
const userProperties: Option[] = [
  ['username', '登录账号'], ['employeeNumber', '工号'], ['email', '电子邮箱'], ['mobile', '手机号码'], ['windowsaccount', 'Windows账号'], ['userId', '用户编码'],
].map(([value, label]) => ({ value, label }))
const tokenUserProperties: Option[] = [
  ['userId', '用户编码'], ['username', '登录账号'], ['displayName', '姓名'], ['gender', '性别'], ['idtype', '证件类型'],
  ['idCardNo', '证件号码'], ['mobile', '手机号码'], ['email', '电子邮箱'], ['userType', '用户类型'], ['employeeNumber', '工号'],
  ['jobTitle', '职位'], ['departmentId', '部门编码'], ['department', '部门'], ['windowsAccount', 'Windows账号'],
].map(([value, label]) => ({ value, label }))
const signatureOptions = ['NONE', 'RS256', 'RS384', 'RS512', 'HS256', 'HS384', 'HS512'].map(value => ({ value, label: value }))
const encryptionAlgorithms = ['NONE', 'RSA1_5', 'RSA_OAEP', 'RSA-OAEP-256', 'A128KW', 'A192KW', 'A256KW', 'A128GCMKW', 'A192GCMKW', 'A256GCMKW'].map(value => ({ value, label: value }))
const encryptionMethods = ['A128GCM', 'A192GCM', 'A256GCM', 'A128CBC-HS256', 'A192CBC-HS384', 'A256CBC-HS512', 'XC20P'].map(value => ({ value, label: value }))

const oauthFields: FieldSpec[] = [
  { key: 'registeredRedirectUris', label: '认证地址', control: 'textarea', required: true, full: true },
  { key: 'select_authorizedGrantTypes', label: '授权方式', control: 'select', required: true, full: true, multiple: true, options: ['authorization_code', 'password', 'client_credentials', 'implicit', 'id_token', 'token', 'refresh_token'].map(value => ({ value, label: value })) },
  { key: 'subject', label: '主题(Subject)', control: 'select', required: true, options: userProperties },
  { key: 'select_scope', label: '作用域', control: 'select', required: true, multiple: true, options: ['read', 'write', 'trust', 'openid', 'profile', 'email', 'phone', 'address', 'all'].map(value => ({ value, label: value })) },
  { key: 'approvalPrompt', label: '许可确认', control: 'radio', required: true, options: [{ value: 'force', label: '强制' }, { value: 'auto', label: '自动' }] },
  { key: 'pkce', label: 'PKCE', control: 'radio', required: true, options: yesNo },
  { key: 'accessTokenValiditySeconds', label: 'accessToken有效期', required: true, suffix: '秒' },
  { key: 'refreshTokenValiditySeconds', label: 'refreshToken有效期', required: true, suffix: '秒' },
]

const oidcFields: FieldSpec[] = [
  { key: 'issuer', label: '签发人(Issuer)' }, { key: 'audience', label: '受众(Audience)' },
  { key: 'signature', label: '签名算法', control: 'select', options: signatureOptions, generate: true },
  { key: 'userInfoResponse', label: '用户接口类型', control: 'select', options: ['NORMAL', 'SIGNING', 'ENCRYPTION', 'SIGNING_ENCRYPTION'].map(value => ({ value, label: value })) },
  { key: 'signatureKey', label: '签名密钥', control: 'textarea', full: true },
  { key: 'algorithm', label: '加密算法', control: 'select', options: encryptionAlgorithms, generate: true },
  { key: 'encryptionMethod', label: '加密方法', control: 'select', options: encryptionMethods },
  { key: 'algorithmKey', label: '秘钥', control: 'textarea', full: true },
]

const protocolTabs = computed<TabSpec[]>(() => {
  const protocol = String(props.form.protocol || '')
  if (['OAuth_v2.0', 'OAuth_v2.1', 'OpenID_Connect_v1.0'].includes(protocol)) {
    const tabs = [{ key: 'oauth', title: 'OAuth 2.0配置', fields: oauthFields }]
    if (protocol === 'OpenID_Connect_v1.0') tabs.push({ key: 'oidc', title: 'OpenID Connect配置', fields: oidcFields })
    return tabs
  }
  if (protocol === 'SAML_v2.0') return [{ key: 'saml', title: 'SAML V2.0配置', fields: [
    { key: 'spAcsUrl', label: 'ACS Url', required: true, full: true },
    { key: 'binding', label: 'Binding', control: 'select', required: true, options: ['Redirect-Post', 'Post-Post', 'IdpInit-Post', 'Redirect-PostSimpleSign', 'Post-PostSimpleSign', 'IdpInit-PostSimpleSign'].map(value => ({ value, label: value })) },
    { key: 'entityId', label: 'Entity Id', required: true }, { key: 'audience', label: 'Audience', required: true },
    { key: 'issuer', label: 'Issuer', required: true },
    { key: 'signature', label: '签名算法', control: 'select', required: true, options: ['RSAwithSHA1', 'RSAwithSHA256', 'RSAwithSHA384', 'RSAwithSHA512', 'RSAwithMD5', 'RSAwithRIPEMD160', 'DSAwithSHA1', 'ECDSAwithSHA1', 'ECDSAwithSHA256', 'ECDSAwithSHA384', 'ECDSAwithSHA512', 'HMAC-MD5', 'HMAC-SHA1', 'HMAC-SHA256', 'HMAC-SHA384', 'HMAC-SHA512', 'HMAC-RIPEMD160'].map(value => ({ value, label: value })) },
    { key: 'digestMethod', label: '摘要方法', control: 'select', required: true, options: ['MD5', 'SHA1', 'SHA256', 'SHA384', 'SHA512', 'RIPEMD-160'].map(value => ({ value, label: value })) },
    { key: 'encrypted', label: '加密', control: 'select', required: true, options: [{ value: 'no', label: '不加密' }, { value: 'yes', label: '加密' }] },
    { key: 'validityInterval', label: '有效期', required: true, suffix: '秒' },
    { key: 'nameidFormat', label: 'Nameid Format', control: 'select', required: true, options: ['persistent', 'transient', 'emailAddress', 'X509SubjectName', 'WindowsDomainQualifiedName', 'unspecified', 'entity'].map(value => ({ value, label: value })) },
    { key: 'nameIdConvert', label: 'NameId转换', control: 'select', required: true, options: [{ value: 'original', label: '原始' }, { value: 'uppercase', label: '大写' }, { value: 'lowercase', label: '小写' }] },
    { key: 'fileType', label: '上传证书类型', control: 'select', required: true, options: [{ value: 'certificate', label: '证书' }, { value: 'metadata_file', label: 'SAML元数据文件' }] },
    { key: 'metaFileId', label: 'SAML元数据', control: 'upload', required: true },
    { key: 'certIssuer', label: '证书颁发者', required: true, readonly: true }, { key: 'certExpiration', label: '证书有效期', required: true, readonly: true },
    { key: 'certSubject', label: '证书主题', required: true, readonly: true, full: true },
  ] }]
  if (protocol === 'CAS') return [{ key: 'cas', title: 'CAS配置', fields: [
    { key: 'service', label: '服务', required: true, full: true }, { key: 'callbackUrl', label: '返回地址', required: true, full: true },
    { key: 'casUser', label: '返回账号', control: 'select', required: true, options: userProperties }, { key: 'expires', label: '过期时间', required: true, suffix: '秒' },
  ] }]
  if (protocol === 'JWT') return [{ key: 'jwt', title: 'JWT配置', fields: [
    { key: 'redirectUri', label: '认证地址', required: true, full: true }, { key: 'subject', label: '主题(Subject)', control: 'select', required: true, options: userProperties },
    { key: 'tokenType', label: '令牌类型', control: 'select', required: true, options: ['GET', 'POST', 'LTPA'].map(value => ({ value, label: value })) },
    { key: 'jwtName', label: '名称', required: true }, { key: 'expires', label: '过期时间', required: true, suffix: '秒' },
    { key: 'issuer', label: '签发人(Issuer)' }, { key: 'audience', label: '受众(Audience)' },
    { key: 'signature', label: '签名算法', control: 'select', required: true, options: signatureOptions, generate: true },
    { key: 'signatureKey', label: '签名密钥', control: 'textarea', required: true, full: true },
    { key: 'algorithm', label: '加密算法', control: 'select', options: encryptionAlgorithms, generate: true },
    { key: 'encryptionMethod', label: '加密方法', control: 'select', options: encryptionMethods }, { key: 'algorithmKey', label: '秘钥', control: 'textarea', full: true },
  ] }]
  if (protocol === 'Token_Based') return [{ key: 'token', title: '令牌配置', fields: [
    { key: 'redirectUri', label: '认证地址', required: true, full: true },
    { key: 'tokenType', label: '令牌类型', control: 'select', required: true, options: ['POST', 'GET', 'LTPA'].map(value => ({ value, label: value })) },
    { key: 'cookieName', label: 'Cookie名称', required: true },
    { key: 'algorithm', label: '加密算法', control: 'select', required: true, generate: true, options: ['DES', 'DESede', 'Blowfish', 'AES'].map(value => ({ value, label: value })) },
    { key: 'expires', label: '过期时间', required: true, suffix: '秒' },
    { key: 'select_userPropertys', label: '令牌内容', control: 'select', required: true, full: true, multiple: true, options: tokenUserProperties },
  ] }]
  if (protocol === 'Form_Based') return [{ key: 'form', title: '表单配置', fields: [
    { key: 'redirectUri', label: '认证地址', required: true, full: true }, { key: 'usernameMapping', label: '登录名映射', required: true },
    { key: 'passwordMapping', label: '登录凭证映射', required: true }, { key: 'authorizeView', label: '认证视图' },
    { key: 'passwordAlgorithm', label: '密码算法', control: 'select', options: ['NONE', 'MD5', 'SHA', 'SHA-1', 'SHA-256', 'SHA-384', 'SHA-512', 'MD5-HEX', 'SHA-HEX', 'SHA-1-HEX', 'SHA-256-HEX', 'SHA-384-HEX', 'SHA-512-HEX'].map(value => ({ value, label: value })) },
    { key: 'credential', label: '凭证类型', control: 'radio', full: true, options: [{ value: 'user_defined', label: '用户自定义' }, { value: 'shared', label: '应用共享' }, { value: 'system', label: '系统配置' }] },
    { key: 'systemUserAttr', label: '系统属性', control: 'select', required: true, options: userProperties, hidden: form => form.credential !== 'system' },
    { key: 'sharedUsername', label: '共享用户名', required: true, hidden: form => form.credential !== 'shared' }, { key: 'sharedPassword', label: '共享凭证', required: true, hidden: form => form.credential !== 'shared' },
  ] }]
  if (protocol === 'Extend_API') return [{ key: 'api', title: 'API配置', fields: [
    { key: 'principal', label: '凭证', required: true }, { key: 'credentials', label: '秘钥', required: true },
    { key: 'credential', label: '凭证类型', control: 'radio', full: true, options: [{ value: 'user-defined', label: '用户自定义' }, { value: 'shared', label: '应用共享' }, { value: 'system', label: '系统配置' }] },
    { key: 'systemUserAttr', label: '系统属性', control: 'select', required: true, options: userProperties, hidden: form => form.credential !== 'system' },
    { key: 'sharedUsername', label: '共享用户名', required: true, hidden: form => form.credential !== 'shared' }, { key: 'sharedPassword', label: '共享凭证', required: true, hidden: form => form.credential !== 'shared' },
  ] }]
  return []
})

const extraFields: FieldSpec[] = [
  { key: 'logoutUrl', label: '注销地址' }, { key: 'logoutType', label: '注销方式', control: 'select', options: [{ value: '0', label: '无' }, { value: '1', label: '后台' }, { value: '2', label: '前台' }] },
  { key: 'visible', label: '权限范围', control: 'select', options: [{ value: '0', label: '隐藏' }, { value: '1', label: '所有用户' }, { value: '2', label: '内部用户' }, { value: '3', label: '外部用户' }] },
  { key: 'sortIndex', label: '排序' }, { key: 'vendor', label: '供应商' }, { key: 'vendorUrl', label: '供应商网址' },
  { key: 'isAdapter', label: '适配', control: 'select', options: [{ value: '0', label: '禁用' }, { value: '1', label: '启用' }] },
  { key: 'resourceMgt', label: '资源管理', control: 'select', options: [{ value: 'n', label: '否' }, { value: 'y', label: '是' }] },
  { key: 'openapiRight', label: 'OpenAPI 权限', control: 'select', options: [{ value: 'read', label: 'Read' }, { value: 'write', label: 'Write' }] },
  { key: 'inducer', label: '引导', hidden: form => !['OAuth_v2.0', 'OAuth_v2.1', 'OpenID_Connect_v1.0'].includes(String(form.protocol)) },
  { key: 'description', label: '描述', full: true },
]

const iconFileList = computed(() => props.form.iconBase64 ? [{ uid: String(props.form.id || '-1'), name: String(props.form.appName || 'icon'), status: 'done', url: props.form.iconBase64 }] : [])
const extraAttrs = ref<ExtraAttr[]>([])
const adapterModalOpen = ref(false)
const adapterLoading = ref(false)
const adapterRows = ref<AdapterRow[]>([])
const adapterName = ref('')
const selectedAdapterKeys = ref<string[]>([])

watch(() => props.form.id, () => {
  try {
    const parsed = props.form.extendAttr ? JSON.parse(String(props.form.extendAttr)) : []
    extraAttrs.value = Array.isArray(parsed) ? parsed.map((item, index) => ({ id: String(index + 1), attr: item.attr || '', type: item.type || 'string', value: item.value || '' })) : []
  } catch { extraAttrs.value = [] }
}, { immediate: true })

watch(extraAttrs, value => {
  props.form.extendAttr = JSON.stringify(value.map(({ attr, type, value: itemValue }) => ({ attr, type, value: itemValue })))
}, { deep: true })

function changeField(field: FieldSpec, value: unknown) {
  props.form[field.key] = value
  if (field.generate && value) emit('generateKey', field.key === 'signature' ? 'signatureKey' : 'algorithmKey', String(value))
}

async function upload(request: UploadRequest, target: 'iconId' | 'metaFileId') {
  const body = new FormData()
  body.append('uploadFile', request.file)
  try {
    const id = await postFormData<string>('/file/upload/', body)
    props.form[target] = id
    if (target === 'iconId') props.form.iconBase64 = URL.createObjectURL(request.file)
    request.onSuccess?.(id)
  } catch (error) {
    const uploadError = error instanceof Error ? error : new Error('上传失败')
    request.onError?.(uploadError)
    message.error(uploadError.message)
  }
}

function uploadIconRequest(request: UploadRequest) {
  return upload(request, 'iconId')
}

function uploadMetadataRequest(request: UploadRequest) {
  return upload(request, 'metaFileId')
}

function removeIcon() {
  props.form.iconId = ''
  props.form.iconBase64 = ''
  return true
}

function addExtraAttr() {
  const id = String(extraAttrs.value.length ? Math.max(...extraAttrs.value.map(item => Number(item.id) || 0)) + 1 : 1)
  extraAttrs.value.push({ id, attr: `Attr ${id}`, type: 'string', value: `value ${id}` })
}

async function loadAdapters() {
  adapterLoading.value = true
  try {
    const protocol = ['OAuth_v2.0', 'OAuth_v2.1', 'OpenID_Connect_v1.0'].includes(String(props.form.protocol)) ? 'OAuth_v2.0' : props.form.protocol
    const data = await adminGet<any>('/adapters/fetch', { name: adapterName.value, protocol, pageNumber: 1, pageSize: 50 })
    adapterRows.value = Array.isArray(data?.rows) ? data.rows : []
  } catch (error) {
    message.error(error instanceof Error ? error.message : '适配器加载失败')
  } finally { adapterLoading.value = false }
}

async function openAdapters() {
  adapterName.value = ''
  selectedAdapterKeys.value = []
  adapterModalOpen.value = true
  await loadAdapters()
}

function selectAdapter() {
  const selected = adapterRows.value.find(item => item.id && selectedAdapterKeys.value.includes(item.id))
  if (!selected) return message.warning('请选择适配器')
  props.form.adapterId = selected.id
  props.form.adapterName = selected.name
  props.form.adapter = selected.adapter
  adapterModalOpen.value = false
}
</script>

<template>
  <a-tabs class="application-editor-tabs">
    <a-tab-pane key="basic" tab="基本信息">
      <a-form class="legacy-editor-form" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="编码" required><a-input :value="form.id" readonly /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="应用秘钥" required><a-input-search :value="form.secret" readonly enter-button="生成" @search="emit('generateSecret')" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="图标" required>
              <a-upload list-type="picture-card" name="uploadFile" :file-list="iconFileList as any" :custom-request="uploadIconRequest as any" :max-count="1" @remove="removeIcon">
                <div v-if="!form.iconBase64"><PlusOutlined /><div class="upload-text">Upload</div></div>
              </a-upload>
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="应用名称" required><a-input v-model:value="form.appName" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="常用"><a-radio-group v-model:value="form.frequently" option-type="button" button-style="solid" :options="yesNo" /></a-form-item></a-col>
        </a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="协议" required><a-input v-model:value="form.protocol" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="分类"><a-select v-model:value="form.category" :options="categoryOptions" /></a-form-item></a-col>
        </a-row>
        <a-row><a-col :span="24"><a-form-item label="登录地址" required :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }"><a-input v-model:value="form.loginUrl" /></a-form-item></a-col></a-row>
        <a-row :gutter="24">
          <a-col :span="12"><a-form-item label="状态" required><a-switch v-model:checked="form.status" :checked-value="1" :un-checked-value="0" /></a-form-item></a-col>
          <a-col :span="12"><a-form-item label="配置扩展属性"><a-radio-group v-model:value="form.isExtendAttr" option-type="button" button-style="solid" :options="zeroOne" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-tab-pane>

    <a-tab-pane v-for="tab in protocolTabs" :key="tab.key" :tab="tab.title">
      <a-form class="legacy-editor-form" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="24">
          <a-col v-for="field in tab.fields.filter(item => !item.hidden?.(form))" :key="field.key" :span="field.full ? 24 : 12">
            <a-form-item :label="field.label" :required="field.required" :label-col="field.full ? { span: 4 } : { span: 8 }" :wrapper-col="field.full ? { span: 20 } : { span: 16 }">
              <a-upload v-if="field.control === 'upload'" :show-upload-list="true" :custom-request="uploadMetadataRequest as any"><a-button><UploadOutlined />上传</a-button></a-upload>
              <a-textarea v-else-if="field.control === 'textarea'" :value="form[field.key]" :readonly="field.readonly" @update:value="changeField(field, $event)" />
              <a-select v-else-if="field.control === 'select'" :value="form[field.key]" :mode="field.multiple ? 'multiple' : undefined" :options="field.options" @update:value="changeField(field, $event)" />
              <a-radio-group v-else-if="field.control === 'radio'" :value="form[field.key]" option-type="button" button-style="solid" :options="field.options" @update:value="changeField(field, $event)" />
              <a-input v-else :value="form[field.key]" :readonly="field.readonly" @update:value="changeField(field, $event)"><template v-if="field.suffix" #addonAfter>{{ field.suffix }}</template></a-input>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-tab-pane>

    <a-tab-pane key="extra" tab="扩展信息">
      <a-form class="legacy-editor-form" :label-col="{ span: 8 }" :wrapper-col="{ span: 16 }">
        <a-row :gutter="24">
          <a-col v-for="field in extraFields.filter(item => !item.hidden?.(form))" :key="field.key" :span="field.full ? 24 : 12">
            <a-form-item :label="field.label" :label-col="field.full ? { span: 4 } : { span: 8 }" :wrapper-col="field.full ? { span: 20 } : { span: 16 }">
              <a-select v-if="field.control === 'select'" v-model:value="form[field.key]" :options="field.options" />
              <a-input v-else v-model:value="form[field.key]" />
            </a-form-item>
          </a-col>
          <a-col :span="12"><a-form-item label="适配器"><a-input-search v-model:value="form.adapterName" enter-button="选择" @search="openAdapters" /></a-form-item></a-col>
        </a-row>
      </a-form>
    </a-tab-pane>

    <a-tab-pane v-if="editing && form.isExtendAttr === '1'" key="custom" tab="自定义属性">
      <a-button type="primary" class="extra-add" @click="addExtraAttr">新增</a-button>
      <a-table bordered :data-source="extraAttrs" :pagination="false" row-key="id">
        <a-table-column title="属性" :width="180"><template #default="{ record }"><a-input v-model:value="record.attr" /></template></a-table-column>
        <a-table-column title="类型" :width="140"><template #default="{ record }"><a-select v-model:value="record.type" style="width: 100%" :options="[{ value: 'string', label: 'string' }, { value: 'boolean', label: 'boolean' }, { value: 'integer', label: 'integer' }]" /></template></a-table-column>
        <a-table-column title="值"><template #default="{ record }"><a-input v-model:value="record.value" /></template></a-table-column>
        <a-table-column title="操作" :width="100"><template #default="{ record }"><a-button danger @click="extraAttrs = extraAttrs.filter(item => item.id !== record.id)">删除</a-button></template></a-table-column>
      </a-table>
    </a-tab-pane>
  </a-tabs>

  <a-modal v-model:open="adapterModalOpen" title="选择" width="720px" :footer="null">
    <a-card :bordered="false">
      <a-form layout="inline" @submit.prevent="loadAdapters"><a-form-item label="名称"><a-input v-model:value="adapterName" /></a-form-item><a-button type="primary" html-type="submit">查询</a-button><a-button type="primary" class="adapter-confirm" @click="selectAdapter">确定</a-button></a-form>
      <a-table bordered size="small" row-key="id" :loading="adapterLoading" :data-source="adapterRows" :pagination="{ pageSize: 5 }" :row-selection="{ type: 'radio', selectedRowKeys: selectedAdapterKeys, onChange: (keys: Array<string | number>) => selectedAdapterKeys = keys.map(String) }">
        <a-table-column title="名称" data-index="name" /><a-table-column title="协议" data-index="protocol" /><a-table-column title="排序" data-index="sortIndex" />
      </a-table>
    </a-card>
  </a-modal>
</template>

<style scoped>
.application-editor-tabs { width: 100%; }
.legacy-editor-form { min-height: 280px; padding: 12px 10px 0; }
.legacy-editor-form :deep(.ant-form-item) { margin-bottom: 18px; }
.legacy-editor-form :deep(.ant-select), .legacy-editor-form :deep(.ant-input-number) { width: 100%; }
.upload-text { margin-top: 8px; }
.extra-add { margin-bottom: 12px; }
.adapter-confirm { margin-left: 8px; }
@media (max-width: 768px) {
  .legacy-editor-form :deep(.ant-col-12) { max-width: 100%; flex: 0 0 100%; }
}
</style>

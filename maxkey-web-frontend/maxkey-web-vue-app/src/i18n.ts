import { computed } from 'vue'
import { createI18n, type LocaleMessageValue, type VueMessageType } from 'vue-i18n'
import enUS from 'ant-design-vue/es/locale/en_US'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import zhTW from 'ant-design-vue/es/locale/zh_TW'

export type SupportedLanguage = 'zh-CN' | 'zh-TW' | 'en-US'
type MessageTree = Record<string, LocaleMessageValue<VueMessageType>>

const DEFAULT_LANGUAGE: SupportedLanguage = 'zh-CN'
const LANGUAGE_KEY = 'maxkey_layout_language'

export const supportedLanguages: Array<{ code: SupportedLanguage; name: string; abbr: string }> = [
  { code: 'zh-CN', name: '简体中文', abbr: 'CN' },
  { code: 'zh-TW', name: '繁體中文', abbr: 'TW' },
  { code: 'en-US', name: 'English', abbr: 'EN' },
]

const uiMessages: Record<SupportedLanguage, MessageTree> = {
  'zh-CN': {
    returnPortal: '应用中心', adminNavigation: '管理导航', noEmail: '未设置企业邮箱', clearTitle: '确定清理所有本地缓存？',
    clearSuccess: '本地缓存已清理', roles: '角色', permissionRole: '角色权限', roleMembers: '角色成员',
    portalSubtitle: '已授权的应用列表', portalLoading: '正在加载…', unnamedApp: '未命名应用',
    configureAccount: '配置应用账号', noApps: '暂无可访问应用', appLoadError: '应用列表加载失败',
    portalCenter: '应用中心', adminCenter: '管理后台', adminPortalEntry: '管理后台', portalWelcome: '欢迎回来，{name}',
    portalHeroDescription: '从一个安全入口访问所有已授权的企业应用', portalIdentityVerified: '身份已认证',
    portalApplications: '我的应用', portalApplicationsHint: '已为你授权 {count} 个应用', portalSearchPlaceholder: '搜索应用名称',
    createGroup: '新建分组', renameGroup: '重命名分组', deleteGroup: '删除分组', groupName: '分组名称', groupNamePlaceholder: '请输入分组名称', groupNameRequired: '请输入分组名称', unnamedGroup: '未命名分组',
    ungrouped: '未分组', dropAppsHere: '将应用拖到这里', clearFiltersToSort: '清除搜索和分类筛选后可拖动排序', noMatchingAppsInGroup: '该分组没有匹配的应用', deleteGroupConfirm: '确定删除“{name}”吗？组内应用将移回未分组。',
    moveUp: '上移', moveDown: '下移', moveToGroup: '移动到分组', appActions: '应用操作', confirm: '确定', cancel: '取消',
    portalAvailableApps: '可访问应用', portalAccountStatus: '账号状态', portalEmailStatus: '企业邮箱', portalActive: '正常', portalBound: '已绑定', portalUnbound: '未绑定',
    dashboardSubtitle: '身份、应用和访问概况', refresh: '刷新', dashboardLoadError: '控制台数据加载失败', auditLoadError: '日志数据加载失败',
    languageLoadError: '语言包加载失败', status: '状态', delete: '删除', organizationType: '类型', groupCategory: '类型',
    userGender: '性别', userType: '用户类型', userState: '用户状态', userIdType: '证件类型',
    userMarried: '婚姻状态', userBusinessTab: '机构信息', userAuthnType: '二次认证', appCategory: '分类',
    allCategories: '全部分类', credentialLoadError: '应用账号加载失败', credentialRequired: '请输入应用账号和密码', passwordMismatch: '两次输入的密码不一致',
    sessionLoadError: '会话列表加载失败', terminateSessionsTitle: '确定终止选中的会话？', profileLoadError: '个人资料加载失败', avatarUploadError: '头像上传失败', displayNameRequired: '请输入姓名',
    timebasedLoadError: '动态口令配置加载失败', timebasedGenerateError: '动态口令密钥生成失败', generateSecretFirst: '请先生成共享密钥', otpRequired: '请输入一次性密码', otpVerifySuccess: '验证成功', otpVerifyError: '验证失败', noOtpSecret: '尚未生成动态口令', otpPlaceholder: '请在生成后输入一次性密码用于验证',
    currentUserMissing: '无法获取当前用户，请重新登录', passkeyLoadError: 'Passkey 列表加载失败', passkeyUnsupported: '您的浏览器不支持 WebAuthn/Passkey 功能', passkeyCreateError: '凭证创建失败', passkeyRegisterSuccess: 'Passkey 注册成功', passkeyCancelled: 'Passkey 注册被取消或失败', passkeySecurityError: '安全错误，请检查 HTTPS 连接', passkeyRegisterError: 'Passkey 注册失败',
    passkeyDeleteTitle: '确认删除', passkeyDeleteContent: '确定要删除这个 Passkey 吗？此操作不可撤销。', passkeyDeleteSuccess: 'Passkey 删除成功', passkeyDeleteError: 'Passkey 删除失败', passkeyManagement: 'Passkey 管理', passkeyDescription: 'Passkey 是一种更安全、更便捷的登录方式，使用您的设备生物识别或 PIN 码进行身份验证。', passkeyRegister: '注册新的 Passkey', passkeyRegistered: '已注册的 Passkey', passkeyCredential: '凭证信息', platformAuthenticator: '平台认证器', crossPlatformAuthenticator: '跨平台认证器', passkeySignatureCount: '签名统计', passkeyCreatedDate: '创建时间', passkeyLastUsedDate: '最近访问时间', passkeyDelete: '删除 Passkey',
  },
  'zh-TW': {
    returnPortal: '應用中心', adminNavigation: '管理導航', noEmail: '未設定企業郵箱', clearTitle: '確定清理所有本地快取？',
    clearSuccess: '本地快取已清理', roles: '角色', permissionRole: '角色權限', roleMembers: '角色成員',
    portalSubtitle: '已授權的應用列表', portalLoading: '正在載入…', unnamedApp: '未命名應用',
    configureAccount: '配置應用賬號', noApps: '暫無可訪問應用', appLoadError: '應用列表載入失敗',
    portalCenter: '應用中心', adminCenter: '管理後台', adminPortalEntry: '管理後台', portalWelcome: '歡迎回來，{name}',
    portalHeroDescription: '從一個安全入口訪問所有已授權的企業應用', portalIdentityVerified: '身份已認證',
    portalApplications: '我的應用', portalApplicationsHint: '已為你授權 {count} 個應用', portalSearchPlaceholder: '搜索應用名稱',
    createGroup: '新增分組', renameGroup: '重新命名分組', deleteGroup: '刪除分組', groupName: '分組名稱', groupNamePlaceholder: '請輸入分組名稱', groupNameRequired: '請輸入分組名稱', unnamedGroup: '未命名分組',
    ungrouped: '未分組', dropAppsHere: '將應用拖到這裡', clearFiltersToSort: '清除搜索和分類篩選後可拖動排序', noMatchingAppsInGroup: '該分組沒有匹配的應用', deleteGroupConfirm: '確定刪除「{name}」嗎？組內應用將移回未分組。',
    moveUp: '上移', moveDown: '下移', moveToGroup: '移動到分組', appActions: '應用操作', confirm: '確定', cancel: '取消',
    portalAvailableApps: '可訪問應用', portalAccountStatus: '賬號狀態', portalEmailStatus: '企業郵箱', portalActive: '正常', portalBound: '已綁定', portalUnbound: '未綁定',
    dashboardSubtitle: '身份、應用和訪問概況', refresh: '重新整理', dashboardLoadError: '控制台資料載入失敗', auditLoadError: '日誌資料載入失敗',
    languageLoadError: '語言包載入失敗', status: '狀態', delete: '刪除', organizationType: '類型', groupCategory: '類型',
    userGender: '性別', userType: '用戶類型', userState: '用戶狀態', userIdType: '證件類型',
    userMarried: '婚姻狀態', userBusinessTab: '機構信息', userAuthnType: '二次認證', appCategory: '分類',
    allCategories: '全部分類', credentialLoadError: '應用賬號載入失敗', credentialRequired: '請輸入應用賬號和密碼', passwordMismatch: '兩次輸入的密碼不一致',
    sessionLoadError: '會話列表載入失敗', terminateSessionsTitle: '確定終止選中的會話？', profileLoadError: '個人資料載入失敗', avatarUploadError: '頭像上傳失敗', displayNameRequired: '請輸入姓名',
    timebasedLoadError: '動態口令配置載入失敗', timebasedGenerateError: '動態口令密鑰生成失敗', generateSecretFirst: '請先生成共享密鑰', otpRequired: '請輸入一次性密碼', otpVerifySuccess: '驗證成功', otpVerifyError: '驗證失敗', noOtpSecret: '尚未生成動態口令', otpPlaceholder: '請在生成後輸入一次性密碼用於驗證',
    currentUserMissing: '無法獲取當前用戶，請重新登入', passkeyLoadError: 'Passkey 列表載入失敗', passkeyUnsupported: '您的瀏覽器不支持 WebAuthn/Passkey 功能', passkeyCreateError: '憑證創建失敗', passkeyRegisterSuccess: 'Passkey 註冊成功', passkeyCancelled: 'Passkey 註冊被取消或失敗', passkeySecurityError: '安全錯誤，請檢查 HTTPS 連接', passkeyRegisterError: 'Passkey 註冊失敗',
    passkeyDeleteTitle: '確認刪除', passkeyDeleteContent: '確定要刪除這個 Passkey 嗎？此操作不可撤銷。', passkeyDeleteSuccess: 'Passkey 刪除成功', passkeyDeleteError: 'Passkey 刪除失敗', passkeyManagement: 'Passkey 管理', passkeyDescription: 'Passkey 是一種更安全、更便捷的登入方式，使用您的設備生物識別或 PIN 碼進行身份驗證。', passkeyRegister: '註冊新的 Passkey', passkeyRegistered: '已註冊的 Passkey', passkeyCredential: '憑證信息', platformAuthenticator: '平台認證器', crossPlatformAuthenticator: '跨平台認證器', passkeySignatureCount: '簽名統計', passkeyCreatedDate: '創建時間', passkeyLastUsedDate: '最近訪問時間', passkeyDelete: '刪除 Passkey',
  },
  'en-US': {
    returnPortal: 'Application Center', adminNavigation: 'Management', noEmail: 'Enterprise email not set', clearTitle: 'Clear all local storage?',
    clearSuccess: 'Local storage cleared', roles: 'Roles', permissionRole: 'Role Permissions', roleMembers: 'Role Members',
    portalSubtitle: 'Authorized applications', portalLoading: 'Loading…', unnamedApp: 'Unnamed application',
    configureAccount: 'Configure account', noApps: 'No accessible applications', appLoadError: 'Failed to load applications',
    portalCenter: 'Application Center', adminCenter: 'Admin Console', adminPortalEntry: 'Administration', portalWelcome: 'Welcome back, {name}',
    portalHeroDescription: 'Access every authorized enterprise application from one secure entry point', portalIdentityVerified: 'Identity verified',
    portalApplications: 'My Applications', portalApplicationsHint: '{count} applications are available to you', portalSearchPlaceholder: 'Search applications',
    createGroup: 'New group', renameGroup: 'Rename group', deleteGroup: 'Delete group', groupName: 'Group name', groupNamePlaceholder: 'Enter a group name', groupNameRequired: 'Enter a group name', unnamedGroup: 'Unnamed group',
    ungrouped: 'Ungrouped', dropAppsHere: 'Drag applications here', clearFiltersToSort: 'Clear search and category filters to drag and reorder', noMatchingAppsInGroup: 'No matching applications in this group', deleteGroupConfirm: 'Delete "{name}"? Its applications will move to Ungrouped.',
    moveUp: 'Move up', moveDown: 'Move down', moveToGroup: 'Move to group', appActions: 'Application actions', confirm: 'OK', cancel: 'Cancel',
    portalAvailableApps: 'Available apps', portalAccountStatus: 'Account status', portalEmailStatus: 'Enterprise email', portalActive: 'Active', portalBound: 'Bound', portalUnbound: 'Not bound',
    dashboardSubtitle: 'Identity, application and access overview', refresh: 'Refresh', dashboardLoadError: 'Failed to load dashboard data', auditLoadError: 'Failed to load audit data',
    languageLoadError: 'Failed to load language resources', status: 'Status', delete: 'Delete', organizationType: 'Type', groupCategory: 'Type',
    userGender: 'Gender', userType: 'User Type', userState: 'User State', userIdType: 'ID Type',
    userMarried: 'Marital Status', userBusinessTab: 'Organization', userAuthnType: 'Two-factor Authentication', appCategory: 'Category',
    allCategories: 'All categories', credentialLoadError: 'Failed to load application credentials', credentialRequired: 'Enter the application account and password', passwordMismatch: 'The passwords do not match',
    sessionLoadError: 'Failed to load sessions', terminateSessionsTitle: 'Terminate the selected sessions?', profileLoadError: 'Failed to load profile', avatarUploadError: 'Failed to upload avatar', displayNameRequired: 'Enter a display name',
    timebasedLoadError: 'Failed to load authenticator settings', timebasedGenerateError: 'Failed to generate an authenticator secret', generateSecretFirst: 'Generate a shared secret first', otpRequired: 'Enter a one-time password', otpVerifySuccess: 'Verification succeeded', otpVerifyError: 'Verification failed', noOtpSecret: 'No authenticator secret generated', otpPlaceholder: 'Enter a one-time password after generating the secret',
    currentUserMissing: 'Unable to identify the current user. Sign in again.', passkeyLoadError: 'Failed to load Passkeys', passkeyUnsupported: 'This browser does not support WebAuthn/Passkeys', passkeyCreateError: 'Failed to create the credential', passkeyRegisterSuccess: 'Passkey registered', passkeyCancelled: 'Passkey registration was cancelled or failed', passkeySecurityError: 'Security error. Check the HTTPS connection.', passkeyRegisterError: 'Failed to register Passkey',
    passkeyDeleteTitle: 'Confirm deletion', passkeyDeleteContent: 'Delete this Passkey? This action cannot be undone.', passkeyDeleteSuccess: 'Passkey deleted', passkeyDeleteError: 'Failed to delete Passkey', passkeyManagement: 'Passkey Management', passkeyDescription: 'Passkeys provide safer and easier sign-in using your device biometrics or PIN.', passkeyRegister: 'Register a new Passkey', passkeyRegistered: 'Registered Passkeys', passkeyCredential: 'Credential', platformAuthenticator: 'Platform authenticator', crossPlatformAuthenticator: 'Cross-platform authenticator', passkeySignatureCount: 'Signature count', passkeyCreatedDate: 'Created', passkeyLastUsedDate: 'Last used', passkeyDelete: 'Delete Passkey',
  },
}

function initialLanguage(): SupportedLanguage {
  const saved = localStorage.getItem(LANGUAGE_KEY)
  if (supportedLanguages.some(item => item.code === saved)) return saved as SupportedLanguage
  const browserLanguage = navigator.language === 'zh-TW' || navigator.language === 'zh-HK' ? 'zh-TW' : navigator.language.startsWith('en') ? 'en-US' : DEFAULT_LANGUAGE
  return browserLanguage
}

export const i18n = createI18n({
  legacy: false,
  locale: initialLanguage(),
  fallbackLocale: DEFAULT_LANGUAGE,
  messages: {},
  missingWarn: import.meta.env.DEV,
  fallbackWarn: import.meta.env.DEV,
})

const loadedLanguages = new Set<SupportedLanguage>()
const loadingLanguages = new Map<SupportedLanguage, Promise<void>>()
let switchRequest = 0

function setNestedValue(target: MessageTree, path: string[], value: LocaleMessageValue<VueMessageType>) {
  let current = target
  path.forEach((segment, index) => {
    if (index === path.length - 1) current[segment] = value
    else {
      const child = current[segment]
      if (!child || typeof child !== 'object' || Array.isArray(child)) current[segment] = {}
      current = current[segment] as MessageTree
    }
  })
}

// Legacy JSON contains both nested objects and dotted keys; normalize it once at the compatibility boundary.
function normalizeLegacyMessages(source: MessageTree): MessageTree {
  const normalized: MessageTree = {}
  Object.entries(source).forEach(([key, value]) => {
    const normalizedValue = value && typeof value === 'object' && !Array.isArray(value)
      ? normalizeLegacyMessages(value as MessageTree)
      : value
    setNestedValue(normalized, key.split('.'), normalizedValue)
  })
  return normalized
}

function legacyLabel(source: MessageTree, path: string[]): string {
  let value: LocaleMessageValue<VueMessageType> = source
  for (const key of path) {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return ''
    value = (value as MessageTree)[key]
  }
  return typeof value === 'string' ? value : ''
}

async function fetchMessages(path: string): Promise<MessageTree> {
  const response = await fetch(`${import.meta.env.BASE_URL}${path}`)
  if (!response.ok) throw new Error(`Unable to load locale messages: ${path}`)
  return response.json() as Promise<MessageTree>
}

async function ensureLanguageLoaded(code: SupportedLanguage): Promise<void> {
  if (loadedLanguages.has(code)) return
  const pending = loadingLanguages.get(code)
  if (pending) return pending

  const loading = Promise.all([
    fetchMessages(`assets/i18n/${code}.json`),
    fetchMessages(`assets/i18n/admin/${code}.json`),
  ]).then(([portalSource, adminSource]) => {
    const legacy = {
      portalSettings: legacyLabel(portalSource, ['mxk', 'menu', 'config', '']),
      portalAudit: legacyLabel(portalSource, ['mxk', 'menu', 'audit', '']),
      adminIdentities: legacyLabel(adminSource, ['mxk', 'menu', 'identities', '']),
      adminAccess: legacyLabel(adminSource, ['mxk', 'menu', 'access', '']),
      adminPermissions: legacyLabel(adminSource, ['mxk', 'menu', 'permissions', '']),
      adminConfig: legacyLabel(adminSource, ['mxk', 'menu', 'config', '']),
      adminAudit: legacyLabel(adminSource, ['mxk', 'menu', 'audit', '']),
      fullscreen: legacyLabel(portalSource, ['menu.fullscreen']),
      fullscreenExit: legacyLabel(portalSource, ['menu.fullscreen.exit']),
      clearStorage: legacyLabel(portalSource, ['menu.clear.local.storage']),
      language: legacyLabel(portalSource, ['menu.lang']),
      logout: legacyLabel(portalSource, ['menu.account.logout']),
    }
    i18n.global.setLocaleMessage(code, {
      portal: normalizeLegacyMessages(portalSource),
      admin: normalizeLegacyMessages(adminSource),
      ui: { ...uiMessages[code], legacy },
    })
    loadedLanguages.add(code)
    loadingLanguages.delete(code)
  }).catch(error => {
    loadingLanguages.delete(code)
    throw error
  })

  loadingLanguages.set(code, loading)
  return loading
}

export async function setLanguage(code: SupportedLanguage): Promise<void> {
  const request = ++switchRequest
  await ensureLanguageLoaded(code)
  if (request !== switchRequest) return
  i18n.global.locale.value = code
  localStorage.setItem(LANGUAGE_KEY, code)
  document.documentElement.lang = code
}

export async function initializeI18n(): Promise<void> {
  let code = i18n.global.locale.value as SupportedLanguage
  await ensureLanguageLoaded(DEFAULT_LANGUAGE)
  if (code !== DEFAULT_LANGUAGE) {
    try {
      await ensureLanguageLoaded(code)
    } catch {
      code = DEFAULT_LANGUAGE
      i18n.global.locale.value = code
    }
  }
  document.documentElement.lang = code
  document.documentElement.removeAttribute('dir')
  localStorage.removeItem('maxkey_layout_direction')
}

export const currentLanguage = computed(() => i18n.global.locale.value as SupportedLanguage)
export const antdLocale = computed(() => ({ 'zh-CN': zhCN, 'zh-TW': zhTW, 'en-US': enUS })[currentLanguage.value])

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
    returnPortal: '返回门户', noEmail: '未设置企业邮箱', clearTitle: '确定清理所有本地缓存？',
    clearSuccess: '本地缓存已清理', roles: '角色', permissionRole: '角色权限', roleMembers: '角色成员',
    portalSubtitle: '已授权的应用列表', portalLoading: '正在加载…', unnamedApp: '未命名应用',
    configureAccount: '配置应用账号', noApps: '暂无可访问应用', appLoadError: '应用列表加载失败',
    dashboardSubtitle: '身份、应用和访问概况', refresh: '刷新', dashboardLoadError: '控制台数据加载失败',
    languageLoadError: '语言包加载失败',
  },
  'zh-TW': {
    returnPortal: '返回門戶', noEmail: '未設定企業郵箱', clearTitle: '確定清理所有本地快取？',
    clearSuccess: '本地快取已清理', roles: '角色', permissionRole: '角色權限', roleMembers: '角色成員',
    portalSubtitle: '已授權的應用列表', portalLoading: '正在載入…', unnamedApp: '未命名應用',
    configureAccount: '配置應用賬號', noApps: '暫無可訪問應用', appLoadError: '應用列表載入失敗',
    dashboardSubtitle: '身份、應用和訪問概況', refresh: '重新整理', dashboardLoadError: '控制台資料載入失敗',
    languageLoadError: '語言包載入失敗',
  },
  'en-US': {
    returnPortal: 'Back to portal', noEmail: 'Enterprise email not set', clearTitle: 'Clear all local storage?',
    clearSuccess: 'Local storage cleared', roles: 'Roles', permissionRole: 'Role Permissions', roleMembers: 'Role Members',
    portalSubtitle: 'Authorized applications', portalLoading: 'Loading…', unnamedApp: 'Unnamed application',
    configureAccount: 'Configure account', noApps: 'No accessible applications', appLoadError: 'Failed to load applications',
    dashboardSubtitle: 'Identity, application and access overview', refresh: 'Refresh', dashboardLoadError: 'Failed to load dashboard data',
    languageLoadError: 'Failed to load language resources',
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

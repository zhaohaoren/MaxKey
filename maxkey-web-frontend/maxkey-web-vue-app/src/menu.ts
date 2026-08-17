export interface MenuItem {
  key: string
  titleKey: string
  path?: string
  icon?: MenuIcon
  children?: MenuItem[]
}

export type MenuIcon =
  | 'api' | 'appstore' | 'audit' | 'check-square' | 'cluster' | 'comment' | 'contacts'
  | 'database' | 'eye' | 'file-protect' | 'history' | 'home' | 'idcard'
  | 'mail' | 'partition' | 'project' | 'radar' | 'read' | 'safety' | 'send' | 'setting'
  | 'team' | 'user'

export const portalMenus: MenuItem[] = [
  { key: 'apps', titleKey: 'portal.mxk.menu.applist', path: '/dashboard/home', icon: 'home' },
  { key: 'sessions', titleKey: 'portal.mxk.menu.sessions', path: '/access/sessions', icon: 'cluster' },
  {
    key: 'settings',
    titleKey: 'ui.legacy.portalSettings',
    icon: 'setting',
    children: [
      { key: 'profile', titleKey: 'portal.mxk.menu.config.profile', path: '/config/profile', icon: 'appstore' },
      { key: 'passkey', titleKey: 'portal.mxk.menu.config.passkey', path: '/config/passkey', icon: 'safety' },
      { key: 'mfa', titleKey: 'portal.mxk.menu.config.mfa', path: '/config/mfa', icon: 'appstore' },
      { key: 'password', titleKey: 'portal.mxk.menu.config.password', path: '/config/password', icon: 'setting' },
      { key: 'timebased', titleKey: 'portal.mxk.menu.config.timebased', path: '/config/timebased', icon: 'send' },
    ],
  },
  {
    key: 'audit', titleKey: 'ui.legacy.portalAudit', icon: 'history', children: [
      { key: 'audit-logins', titleKey: 'portal.mxk.menu.audit.logins', path: '/audit/audit-logins', icon: 'audit' },
      { key: 'audit-apps', titleKey: 'portal.mxk.menu.audit.loginapps', path: '/audit/audit-login-apps', icon: 'audit' },
    ],
  },
]

export const adminMenus: MenuItem[] = [
  { key: 'dashboard', titleKey: 'admin.mxk.menu.home', path: '/admin', icon: 'home' },
  {
    key: 'identities',
    titleKey: 'ui.legacy.adminIdentities',
    icon: 'user',
    children: [
      { key: 'orgs', titleKey: 'admin.mxk.menu.identities.organizations', path: '/admin/orgs', icon: 'cluster' },
      { key: 'users', titleKey: 'admin.mxk.menu.identities.users', path: '/admin/users', icon: 'user' },
      { key: 'groups', titleKey: 'admin.mxk.menu.identities.groups', path: '/admin/groups', icon: 'contacts' },
      { key: 'groupmembers', titleKey: 'admin.mxk.menu.identities.groupmembers', path: '/admin/groupmembers', icon: 'team' },
    ],
  },
  { key: 'apps', titleKey: 'admin.mxk.menu.apps', path: '/admin/apps', icon: 'project' },
  {
    key: 'access',
    titleKey: 'ui.legacy.adminAccess',
    icon: 'safety',
    children: [
      { key: 'access-permissions', titleKey: 'admin.mxk.menu.access.permissions', path: '/admin/access', icon: 'check-square' },
      { key: 'sessions', titleKey: 'admin.mxk.menu.sessions', path: '/admin/sessions', icon: 'eye' },
    ],
  },
  {
    key: 'permissions',
    titleKey: 'ui.legacy.adminPermissions',
    icon: 'radar',
    children: [
      { key: 'permission-apps', titleKey: 'ui.legacy.adminPermissions', path: '/admin/permissions/apps', icon: 'read' },
    ],
  },
  {
    key: 'config',
    titleKey: 'ui.legacy.adminConfig',
    icon: 'setting',
    children: [
      { key: 'institutions', titleKey: 'admin.mxk.menu.config.institutions', path: '/admin/institutions', icon: 'appstore' },
      { key: 'accounts', titleKey: 'admin.mxk.menu.accounts', path: '/admin/accounts', icon: 'idcard' },
      { key: 'synchronizers', titleKey: 'admin.mxk.menu.config.synchronizers', path: '/admin/synchronizers', icon: 'partition' },
      { key: 'connectors', titleKey: 'admin.mxk.menu.config.connectors', path: '/admin/connectors', icon: 'api' },
      { key: 'socialsprovider', titleKey: 'admin.mxk.menu.config.socialsproviders', path: '/admin/socialsprovider', icon: 'comment' },
      { key: 'ldapcontext', titleKey: 'admin.mxk.menu.config.ldapcontext', path: '/admin/ldapcontext', icon: 'database' },
      { key: 'emailsenders', titleKey: 'admin.mxk.menu.config.emailsenders', path: '/admin/emailsenders', icon: 'mail' },
      { key: 'smsprovider', titleKey: 'admin.mxk.menu.config.smsproviders', path: '/admin/smsprovider', icon: 'send' },
      { key: 'passwordpolicy', titleKey: 'admin.mxk.menu.config.passwordpolicy', path: '/admin/passwordpolicy', icon: 'file-protect' },
    ],
  },
  {
    key: 'historys', titleKey: 'ui.legacy.adminAudit', icon: 'history', children: [
      { key: 'admin-audit-logins', titleKey: 'admin.mxk.menu.audit.logins', path: '/admin/audit/logins', icon: 'audit' },
      { key: 'admin-audit-apps', titleKey: 'admin.mxk.menu.audit.loginapps', path: '/admin/audit/apps', icon: 'audit' },
      { key: 'admin-audit-sync', titleKey: 'admin.mxk.menu.audit.synchronizer', path: '/admin/audit/synchronizers', icon: 'audit' },
      { key: 'admin-audit-connectors', titleKey: 'admin.mxk.menu.audit.connector', path: '/admin/audit/connectors', icon: 'audit' },
      { key: 'admin-audit-systems', titleKey: 'admin.mxk.menu.audit.operate', path: '/admin/audit/systems', icon: 'audit' },
    ],
  },
]

export interface MenuItem {
  key: string
  titleKey: string
  path?: string
  children?: MenuItem[]
}

export const portalMenus: MenuItem[] = [
  { key: 'apps', titleKey: 'portal.mxk.menu.applist', path: '/dashboard/home' },
  { key: 'sessions', titleKey: 'portal.mxk.menu.sessions', path: '/access/sessions' },
  {
    key: 'settings',
    titleKey: 'ui.legacy.portalSettings',
    children: [
      { key: 'profile', titleKey: 'portal.mxk.menu.config.profile', path: '/config/profile' },
      { key: 'passkey', titleKey: 'portal.mxk.menu.config.passkey', path: '/config/passkey' },
      { key: 'mfa', titleKey: 'portal.mxk.menu.config.mfa', path: '/config/mfa' },
      { key: 'timebased', titleKey: 'portal.mxk.menu.config.timebased', path: '/config/timebased' },
      { key: 'password', titleKey: 'portal.mxk.menu.config.password', path: '/config/password' },
    ],
  },
  {
    key: 'audit', titleKey: 'ui.legacy.portalAudit', children: [
      { key: 'audit-logins', titleKey: 'portal.mxk.menu.audit.logins', path: '/audit/audit-logins' },
      { key: 'audit-apps', titleKey: 'portal.mxk.menu.audit.loginapps', path: '/audit/audit-login-apps' },
      { key: 'audit-systems', titleKey: 'portal.mxk.menu.audit.operate', path: '/audit/audit-system-logs' },
    ],
  },
]

export const adminMenus: MenuItem[] = [
  { key: 'dashboard', titleKey: 'admin.mxk.menu.home', path: '/admin' },
  {
    key: 'identities',
    titleKey: 'ui.legacy.adminIdentities',
    children: [
      { key: 'orgs', titleKey: 'admin.mxk.menu.identities.organizations', path: '/admin/orgs' },
      { key: 'users', titleKey: 'admin.mxk.menu.identities.users', path: '/admin/users' },
      { key: 'groups', titleKey: 'admin.mxk.menu.identities.groups', path: '/admin/groups' },
      { key: 'groupmembers', titleKey: 'admin.mxk.menu.identities.groupmembers', path: '/admin/groupmembers' },
    ],
  },
  { key: 'accounts', titleKey: 'admin.mxk.menu.accounts', path: '/admin/accounts' },
  { key: 'apps', titleKey: 'admin.mxk.menu.apps', path: '/admin/apps' },
  { key: 'institutions', titleKey: 'admin.mxk.menu.config.institutions', path: '/admin/institutions' },
  { key: 'access', titleKey: 'ui.legacy.adminAccess', path: '/admin/access' },
  {
    key: 'permissions',
    titleKey: 'ui.legacy.adminPermissions',
    children: [
      { key: 'roles', titleKey: 'ui.roles', path: '/admin/roles' },
      { key: 'permission', titleKey: 'admin.mxk.menu.access.permissions', path: '/admin/permission' },
      { key: 'resources', titleKey: 'admin.mxk.menu.permissions.resources', path: '/admin/resources' },
      { key: 'permissionRole', titleKey: 'ui.permissionRole', path: '/admin/permissionRole' },
      { key: 'roleMembers', titleKey: 'ui.roleMembers', path: '/admin/roleMembers' },
    ],
  },
  { key: 'synchronizers', titleKey: 'admin.mxk.menu.config.synchronizers', path: '/admin/synchronizers' },
  {
    key: 'config',
    titleKey: 'ui.legacy.adminConfig',
    children: [
      { key: 'connectors', titleKey: 'admin.mxk.menu.config.connectors', path: '/admin/connectors' },
      { key: 'adapters', titleKey: 'admin.mxk.menu.config.adapters', path: '/admin/adapters' },
      { key: 'accountsstrategy', titleKey: 'admin.mxk.menu.config.accountsstrategys', path: '/admin/accountsstrategy' },
      { key: 'socialsprovider', titleKey: 'admin.mxk.menu.config.socialsproviders', path: '/admin/socialsprovider' },
      { key: 'ldapcontext', titleKey: 'admin.mxk.menu.config.ldapcontext', path: '/admin/ldapcontext' },
      { key: 'emailsenders', titleKey: 'admin.mxk.menu.config.emailsenders', path: '/admin/emailsenders' },
      { key: 'smsprovider', titleKey: 'admin.mxk.menu.config.smsproviders', path: '/admin/smsprovider' },
      { key: 'passwordpolicy', titleKey: 'admin.mxk.menu.config.passwordpolicy', path: '/admin/passwordpolicy' },
    ],
  },
  {
    key: 'historys', titleKey: 'ui.legacy.adminAudit', children: [
      { key: 'admin-audit-logins', titleKey: 'admin.mxk.menu.audit.logins', path: '/admin/audit/logins' },
      { key: 'admin-audit-apps', titleKey: 'admin.mxk.menu.audit.loginapps', path: '/admin/audit/apps' },
      { key: 'admin-audit-systems', titleKey: 'admin.mxk.menu.audit.operate', path: '/admin/audit/systems' },
      { key: 'admin-audit-sync', titleKey: 'admin.mxk.menu.audit.synchronizer', path: '/admin/audit/synchronizers' },
      { key: 'admin-audit-connectors', titleKey: 'admin.mxk.menu.audit.connector', path: '/admin/audit/connectors' },
    ],
  },
]

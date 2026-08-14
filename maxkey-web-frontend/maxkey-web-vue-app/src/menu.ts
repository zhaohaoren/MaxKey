export interface MenuItem {
  key: string
  title: string
  path?: string
  children?: MenuItem[]
}

export const portalMenus: MenuItem[] = [
  { key: 'apps', title: '应用', path: '/dashboard/home' },
  { key: 'sessions', title: '会话', path: '/access/sessions' },
  {
    key: 'settings',
    title: '设置',
    children: [
      { key: 'profile', title: '我的资料', path: '/config/profile' },
      { key: 'passkey', title: 'Passkey 注册', path: '/config/passkey' },
      { key: 'mfa', title: '二次认证', path: '/config/mfa' },
      { key: 'password', title: '密码修改', path: '/config/password' },
    ],
  },
  { key: 'audit', title: '审计', path: '/audit/audit-logins' },
]

export const adminMenus: MenuItem[] = [
  { key: 'dashboard', title: '首页', path: '/admin' },
  {
    key: 'identities',
    title: '身份管理',
    children: [
      { key: 'orgs', title: '组织', path: '/admin/orgs' },
      { key: 'users', title: '用户', path: '/admin/users' },
      { key: 'groups', title: '用户组', path: '/admin/groups' },
      { key: 'groupmembers', title: '用户组成员', path: '/admin/groupmembers' },
    ],
  },
  { key: 'accounts', title: '账号凭证', path: '/admin/accounts' },
  { key: 'apps', title: '应用管理', path: '/admin/apps' },
  { key: 'institutions', title: '机构配置', path: '/admin/institutions' },
  { key: 'access', title: '访问控制', path: '/admin/access' },
  {
    key: 'permissions',
    title: '权限管理',
    children: [
      { key: 'roles', title: '角色', path: '/admin/roles' },
      { key: 'permission', title: '权限', path: '/admin/permission' },
      { key: 'resources', title: '资源', path: '/admin/resources' },
      { key: 'permissionRole', title: '角色权限', path: '/admin/permissionRole' },
      { key: 'roleMembers', title: '角色成员', path: '/admin/roleMembers' },
    ],
  },
  { key: 'synchronizers', title: '同步器管理', path: '/admin/synchronizers' },
  {
    key: 'config',
    title: '系统配置',
    children: [
      { key: 'connectors', title: '连接器', path: '/admin/connectors' },
      { key: 'adapters', title: '适配器', path: '/admin/adapters' },
      { key: 'accountsstrategy', title: '账号策略', path: '/admin/accountsstrategy' },
      { key: 'socialsprovider', title: '社会化登录', path: '/admin/socialsprovider' },
      { key: 'ldapcontext', title: 'LDAP', path: '/admin/ldapcontext' },
      { key: 'emailsenders', title: '邮件发送器', path: '/admin/emailsenders' },
      { key: 'smsprovider', title: '短信服务商', path: '/admin/smsprovider' },
      { key: 'passwordpolicy', title: '密码策略', path: '/admin/passwordpolicy' },
    ],
  },
  { key: 'historys', title: '日志审计', path: '/admin/historys' },
]

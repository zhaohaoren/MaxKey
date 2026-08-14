export interface ResourceField {
  key: string
  label: string
  type?: 'text' | 'number' | 'textarea' | 'select' | 'switch'
  options?: Array<{ label: string; value: string | number }>
  required?: boolean
}

export interface AdminResource {
  key: string
  title: string
  base: string
  searchFields?: string[]
  columns: Array<{ key: string; label: string }>
  fields: ResourceField[]
  deletePath?: string
  fetchPath?: string
  tree?: boolean
}

const commonStatus: ResourceField = {
  key: 'status',
  label: '状态',
  type: 'select',
  options: [
    { label: '启用', value: 1 },
    { label: '停用', value: 2 },
  ],
}

export const adminResources: AdminResource[] = [
  {
    key: 'users', title: '用户管理', base: '/admin/users', searchFields: ['username', 'displayName'],
    columns: [{ key: 'username', label: '用户名' }, { key: 'displayName', label: '显示名称' }, { key: 'email', label: '邮箱' }, { key: 'status', label: '状态' }],
    fields: [
      { key: 'username', label: '用户名', required: true }, { key: 'displayName', label: '显示名称', required: true },
      { key: 'email', label: '邮箱' }, { key: 'mobile', label: '手机号' }, { key: 'employeeNumber', label: '员工编号' }, commonStatus,
    ],
  },
  {
    key: 'apps', title: '应用管理', base: '/admin/apps', searchFields: ['appName', 'protocol'],
    columns: [{ key: 'appName', label: '应用名称' }, { key: 'protocol', label: '协议' }, { key: 'category', label: '分类' }, { key: 'status', label: '状态' }],
    fields: [
      { key: 'appName', label: '应用名称', required: true }, { key: 'protocol', label: '协议', type: 'select', required: true, options: [{ label: 'OAuth20', value: 'oauth20' }, { label: 'OIDC', value: 'oidc' }, { label: 'SAML20', value: 'saml20' }, { label: 'CAS', value: 'cas' }, { label: 'JWT', value: 'jwt' }] },
      { key: 'category', label: '分类' }, { key: 'loginUrl', label: '登录地址' }, { key: 'sortIndex', label: '排序', type: 'number' }, commonStatus,
    ],
  },
  {
    key: 'orgs', title: '组织管理', base: '/admin/orgs', searchFields: ['orgName', 'displayName'], tree: true,
    columns: [{ key: 'orgCode', label: '组织编码' }, { key: 'orgName', label: '组织名称' }, { key: 'type', label: '类型' }, { key: 'sortIndex', label: '排序' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'orgCode', label: '组织编码', required: true }, { key: 'orgName', label: '组织名称', required: true }, { key: 'displayName', label: '显示名称' }, { key: 'parentId', label: '上级组织' }, { key: 'type', label: '类型' }, { key: 'sortIndex', label: '排序', type: 'number' }, commonStatus],
  },
  {
    key: 'groups', title: '用户组管理', base: '/admin/access/groups', searchFields: ['name', 'description'],
    columns: [{ key: 'name', label: '名称' }, { key: 'description', label: '描述' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'name', label: '名称', required: true }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus],
  },
  {
    key: 'groupmembers', title: '用户组成员', base: '/admin/access/groupmembers', searchFields: ['username', 'groupName'],
    columns: [{ key: 'groupName', label: '用户组' }, { key: 'username', label: '用户' }, { key: 'id', label: '标识' }],
    fields: [{ key: 'groupId', label: '用户组标识', required: true }, { key: 'memberId', label: '成员标识', required: true }],
  },
  {
    key: 'accounts', title: '账号凭证', base: '/admin/accounts', searchFields: ['username', 'appName'],
    columns: [{ key: 'username', label: '用户名' }, { key: 'appName', label: '应用' }, { key: 'credential', label: '凭证' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'username', label: '用户名', required: true }, { key: 'appName', label: '应用' }, { key: 'credential', label: '凭证' }, commonStatus],
  },
  {
    key: 'access', title: '访问控制', base: '/admin/access/access', searchFields: ['name'],
    columns: [{ key: 'name', label: '名称' }, { key: 'type', label: '类型' }, { key: 'description', label: '描述' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'name', label: '名称', required: true }, { key: 'type', label: '类型' }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus],
  },
  {
    key: 'roles', title: '角色管理', base: '/admin/permissions/roles', searchFields: ['name', 'description'],
    columns: [{ key: 'name', label: '角色名称' }, { key: 'description', label: '描述' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'name', label: '角色名称', required: true }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus],
  },
  {
    key: 'permission', title: '权限管理', base: '/admin/permissions/permission', searchFields: ['name', 'code'],
    columns: [{ key: 'name', label: '权限名称' }, { key: 'code', label: '权限编码' }, { key: 'description', label: '描述' }],
    fields: [{ key: 'name', label: '权限名称', required: true }, { key: 'code', label: '权限编码', required: true }, { key: 'description', label: '描述', type: 'textarea' }],
  },
  {
    key: 'resources', title: '资源管理', base: '/admin/permissions/resources', searchFields: ['name', 'uri'],
    columns: [{ key: 'name', label: '资源名称' }, { key: 'uri', label: '资源地址' }, { key: 'method', label: '请求方法' }],
    fields: [{ key: 'name', label: '资源名称', required: true }, { key: 'uri', label: '资源地址', required: true }, { key: 'method', label: '请求方法' }, { key: 'description', label: '描述', type: 'textarea' }],
  },
  {
    key: 'permissionRole', title: '角色权限关联', base: '/admin/permissions/permissionRole',
    columns: [{ key: 'roleName', label: '角色' }, { key: 'permissionName', label: '权限' }, { key: 'id', label: '标识' }],
    fields: [{ key: 'roleId', label: '角色标识', required: true }, { key: 'permissionId', label: '权限标识', required: true }],
  },
  {
    key: 'roleMembers', title: '角色成员', base: '/admin/permissions/rolemembers',
    columns: [{ key: 'roleName', label: '角色' }, { key: 'username', label: '用户' }, { key: 'id', label: '标识' }],
    fields: [{ key: 'roleId', label: '角色标识', required: true }, { key: 'memberId', label: '成员标识', required: true }],
  },
  {
    key: 'sessions', title: '在线会话', base: '/admin/access/session', searchFields: ['username', 'host'], deletePath: '/admin/access/session/terminate',
    columns: [{ key: 'username', label: '用户名' }, { key: 'host', label: '主机' }, { key: 'loginTime', label: '登录时间' }, { key: 'id', label: '标识' }],
    fields: [],
  },
  {
    key: 'synchronizers', title: '同步器管理', base: '/admin/config/synchronizers', searchFields: ['name', 'type'],
    columns: [{ key: 'name', label: '名称' }, { key: 'type', label: '类型' }, { key: 'provider', label: '提供方' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'name', label: '名称', required: true }, { key: 'type', label: '类型', required: true }, { key: 'provider', label: '提供方' }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus],
  },
  { key: 'institutions', title: '机构配置', base: '/admin/config/institutions', columns: [{ key: 'name', label: '名称' }, { key: 'code', label: '编码' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'code', label: '编码' }, commonStatus] },
  { key: 'connectors', title: '连接器配置', base: '/admin/config/connectors', columns: [{ key: 'name', label: '名称' }, { key: 'type', label: '类型' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'type', label: '类型' }, { key: 'configuration', label: '配置', type: 'textarea' }, commonStatus] },
  { key: 'adapters', title: '适配器配置', base: '/admin/config/adapters', columns: [{ key: 'name', label: '名称' }, { key: 'type', label: '类型' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'type', label: '类型' }, { key: 'configuration', label: '配置', type: 'textarea' }, commonStatus] },
  { key: 'accountsstrategy', title: '账号策略', base: '/admin/config/accountsstrategy', columns: [{ key: 'name', label: '名称' }, { key: 'description', label: '描述' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus] },
  { key: 'socialsprovider', title: '社会化登录', base: '/admin/config/socialsprovider', columns: [{ key: 'provider', label: '提供方' }, { key: 'clientId', label: '客户端标识' }, { key: 'status', label: '状态' }], fields: [{ key: 'provider', label: '提供方', required: true }, { key: 'clientId', label: '客户端标识' }, { key: 'clientSecret', label: '客户端密钥' }, commonStatus] },
  { key: 'ldapcontext', title: 'LDAP 配置', base: '/admin/config/ldapcontext', columns: [{ key: 'providerUrl', label: '地址' }, { key: 'baseDn', label: 'Base DN' }, { key: 'status', label: '状态' }], fields: [{ key: 'providerUrl', label: '地址' }, { key: 'baseDn', label: 'Base DN' }, { key: 'principal', label: '账号' }, { key: 'credentials', label: '密码' }, commonStatus] },
  { key: 'emailsenders', title: '邮件发送器', base: '/admin/config/emailsenders', columns: [{ key: 'name', label: '名称' }, { key: 'host', label: '服务器' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'host', label: '服务器' }, { key: 'port', label: '端口', type: 'number' }, { key: 'username', label: '账号' }, commonStatus] },
  { key: 'smsprovider', title: '短信服务商', base: '/admin/config/smsprovider', columns: [{ key: 'provider', label: '提供方' }, { key: 'accessKey', label: '访问标识' }, { key: 'status', label: '状态' }], fields: [{ key: 'provider', label: '提供方', required: true }, { key: 'accessKey', label: '访问标识' }, { key: 'secretKey', label: '访问密钥' }, commonStatus] },
  { key: 'passwordpolicy', title: '密码策略', base: '/admin/config/passwordpolicy', columns: [{ key: 'name', label: '名称' }, { key: 'minLength', label: '最小长度' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称' }, { key: 'minLength', label: '最小长度', type: 'number' }, { key: 'maxLength', label: '最大长度', type: 'number' }, commonStatus] },
  { key: 'historys', title: '历史审计', base: '/admin/historys', searchFields: ['username', 'action'], columns: [{ key: 'username', label: '用户' }, { key: 'action', label: '操作' }, { key: 'createTime', label: '时间' }, { key: 'result', label: '结果' }], fields: [] },
]

export function getAdminResource(key: string): AdminResource {
  return adminResources.find(item => item.key === key) || adminResources[0]
}

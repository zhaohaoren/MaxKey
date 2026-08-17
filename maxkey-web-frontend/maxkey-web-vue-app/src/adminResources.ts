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
  treeFilterKey?: string
  importable?: boolean
  singleton?: boolean
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
    key: 'users', title: '用户管理', base: '/users', searchFields: ['username', 'displayName'], tree: true, treeFilterKey: 'departmentId', importable: true,
    columns: [{ key: 'username', label: '用户名' }, { key: 'displayName', label: '显示名称' }, { key: 'email', label: '邮箱' }, { key: 'status', label: '状态' }],
    fields: [
      { key: 'username', label: '用户名', required: true }, { key: 'displayName', label: '显示名称', required: true },
      { key: 'password', label: '初始密码' }, { key: 'email', label: '邮箱' }, { key: 'mobile', label: '手机号' },
      { key: 'employeeNumber', label: '员工编号' }, { key: 'windowsAccount', label: 'Windows 账号' },
      { key: 'departmentId', label: '部门标识' }, { key: 'department', label: '部门名称' },
      { key: 'jobTitle', label: '职位' }, { key: 'userType', label: '用户类型' }, { key: 'userState', label: '用户状态' },
      { key: 'gender', label: '性别', type: 'select', options: [{ label: '男', value: 2 }, { label: '女', value: 1 }] },
      { key: 'sortIndex', label: '排序', type: 'number' }, { key: 'familyName', label: '姓' }, { key: 'middleName', label: '中间名' },
      { key: 'givenName', label: '名' }, { key: 'nickName', label: '昵称' }, { key: 'idType', label: '证件类型' }, { key: 'idCardNo', label: '证件号码' },
      { key: 'country', label: '国家' }, { key: 'province', label: '省份' }, { key: 'city', label: '城市' }, { key: 'address', label: '地址' },
      { key: 'phone', label: '固定电话' }, { key: 'postalCode', label: '邮编' }, { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 1 }, { label: '未激活', value: 2 }, { label: '禁用', value: 4 }, { label: '锁定', value: 5 }, { label: '删除', value: 9 }] },
    ],
  },
  {
    key: 'apps', title: '应用管理', base: '/apps', searchFields: ['appName', 'protocol'],
    columns: [{ key: 'appName', label: '应用名称' }, { key: 'protocol', label: '协议' }, { key: 'category', label: '分类' }, { key: 'status', label: '状态' }],
    fields: [
      { key: 'appName', label: '应用名称', required: true }, { key: 'protocol', label: '协议', type: 'select', required: true, options: [{ label: 'OAuth20', value: 'oauth20' }, { label: 'OIDC', value: 'oidc' }, { label: 'SAML20', value: 'saml20' }, { label: 'CAS', value: 'cas' }, { label: 'JWT', value: 'jwt' }] },
      { key: 'category', label: '分类' }, { key: 'loginUrl', label: '登录地址' }, { key: 'sortIndex', label: '排序', type: 'number' }, commonStatus,
    ],
  },
  {
    key: 'orgs', title: '组织管理', base: '/orgs', searchFields: ['orgName', 'fullName'], tree: true, treeFilterKey: 'parentId', importable: true,
    columns: [{ key: 'orgCode', label: '组织编码' }, { key: 'orgName', label: '组织名称' }, { key: 'type', label: '类型' }, { key: 'sortIndex', label: '排序' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'orgCode', label: '组织编码', required: true }, { key: 'orgName', label: '组织名称', required: true }, { key: 'fullName', label: '组织全称' }, { key: 'parentId', label: '上级组织' }, { key: 'type', label: '类型' }, { key: 'category', label: '分类' }, { key: 'sortIndex', label: '排序', type: 'number' }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus],
  },
  {
    key: 'groups', title: '用户组管理', base: '/access/groups', searchFields: ['groupName', 'groupCode'],
    columns: [{ key: 'groupCode', label: '用户组编码' }, { key: 'groupName', label: '用户组名称' }, { key: 'description', label: '描述' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'groupCode', label: '用户组编码', required: true }, { key: 'groupName', label: '用户组名称', required: true }, { key: 'category', label: '分类' }, { key: 'description', label: '描述', type: 'textarea' }, { key: 'sortIndex', label: '排序', type: 'number' }, commonStatus],
  },
  {
    key: 'groupmembers', title: '用户组成员', base: '/access/groupmembers', searchFields: ['username', 'groupName'],
    columns: [{ key: 'groupName', label: '用户组' }, { key: 'username', label: '用户' }, { key: 'id', label: '标识' }],
    fields: [{ key: 'groupId', label: '用户组标识', required: true }, { key: 'memberId', label: '成员标识', required: true }],
  },
  {
    key: 'accounts', title: '账号凭证', base: '/accounts', searchFields: ['username', 'appName'],
    columns: [{ key: 'username', label: '用户名' }, { key: 'appName', label: '应用' }, { key: 'relatedUsername', label: '应用账号' }, { key: 'createType', label: '创建类型' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'strategyId', label: '账号策略标识' }, { key: 'appId', label: '应用标识', required: true }, { key: 'appName', label: '应用名称' }, { key: 'userId', label: '用户标识', required: true }, { key: 'username', label: '用户名' }, { key: 'displayName', label: '显示名称' }, { key: 'relatedUsername', label: '应用账号', required: true }, { key: 'relatedPassword', label: '应用密码', required: true }, { key: 'createType', label: '创建类型' }, commonStatus],
  },
  {
    key: 'access', title: '访问控制', base: '/access/access', searchFields: ['groupName', 'appName'],
    columns: [{ key: 'groupName', label: '用户组' }, { key: 'appName', label: '应用' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'groupId', label: '用户组标识', required: true }, { key: 'groupName', label: '用户组名称' }, { key: 'appId', label: '应用标识', required: true }, { key: 'appName', label: '应用名称' }],
  },
  {
    key: 'roles', title: '角色管理', base: '/permissions/roles', searchFields: ['roleName', 'roleCode'],
    columns: [{ key: 'roleCode', label: '角色编码' }, { key: 'roleName', label: '角色名称' }, { key: 'appName', label: '应用' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'roleCode', label: '角色编码', required: true }, { key: 'roleName', label: '角色名称', required: true }, { key: 'appId', label: '应用标识' }, { key: 'category', label: '分类' }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus],
  },
  {
    key: 'permission', title: '权限管理', base: '/permissions/permission', searchFields: ['appId', 'groupId'],
    columns: [{ key: 'appId', label: '应用标识' }, { key: 'groupId', label: '用户组标识' }, { key: 'resourceId', label: '资源标识' }, { key: 'status', label: '状态' }],
    fields: [{ key: 'appId', label: '应用标识', required: true }, { key: 'groupId', label: '用户组标识', required: true }, { key: 'resourceId', label: '资源标识', required: true }, commonStatus],
  },
  {
    key: 'resources', title: '资源管理', base: '/permissions/resources', searchFields: ['resourceName', 'appId'],
    columns: [{ key: 'resourceName', label: '资源名称' }, { key: 'resourceUrl', label: '资源地址' }, { key: 'resourceAction', label: '资源动作' }, { key: 'appName', label: '应用' }],
    fields: [{ key: 'resourceName', label: '资源名称', required: true }, { key: 'resourceUrl', label: '资源地址', required: true }, { key: 'resourceType', label: '资源类型' }, { key: 'resourceAction', label: '资源动作' }, { key: 'permission', label: '权限表达式' }, { key: 'appId', label: '应用标识' }, { key: 'parentId', label: '父资源标识' }, { key: 'sortIndex', label: '排序', type: 'number' }, { key: 'description', label: '描述', type: 'textarea' }],
  },
  {
    key: 'permissionRole', title: '角色权限关联', base: '/permissions/permissionRole',
    columns: [{ key: 'roleName', label: '角色' }, { key: 'permissionName', label: '权限' }, { key: 'id', label: '标识' }],
    fields: [{ key: 'roleId', label: '角色标识', required: true }, { key: 'permissionId', label: '权限标识', required: true }],
  },
  {
    key: 'roleMembers', title: '角色成员', base: '/permissions/rolemembers',
    columns: [{ key: 'roleName', label: '角色' }, { key: 'username', label: '用户' }, { key: 'id', label: '标识' }],
    fields: [{ key: 'roleId', label: '角色标识', required: true }, { key: 'memberId', label: '成员标识', required: true }],
  },
  {
    key: 'sessions', title: '在线会话', base: '/access/session', searchFields: ['username', 'host'], deletePath: '/access/session/terminate',
    columns: [{ key: 'username', label: '用户名' }, { key: 'host', label: '主机' }, { key: 'loginTime', label: '登录时间' }, { key: 'id', label: '标识' }],
    fields: [],
  },
  {
    key: 'synchronizers', title: '同步器管理', base: '/config/synchronizers', searchFields: ['name', 'sourceType'],
    columns: [{ key: 'name', label: '名称' }, { key: 'sourceType', label: '数据源类型' }, { key: 'service', label: '同步服务' }, { key: 'status', label: '状态' }],
    fields: [
      { key: 'name', label: '名称', required: true }, { key: 'sourceType', label: '数据源类型', required: true },
      { key: 'service', label: '同步服务 Bean', required: true }, { key: 'scheduler', label: '调度表达式' },
      { key: 'resumeTime', label: '开始时间' }, { key: 'suspendTime', label: '停止时间' },
      { key: 'providerUrl', label: '数据源地址' }, { key: 'driverClass', label: '驱动类' },
      { key: 'principal', label: '连接账号' }, { key: 'credentials', label: '连接密码' },
      { key: 'userBasedn', label: '用户 Base DN' }, { key: 'userFilters', label: '用户过滤器' },
      { key: 'orgBasedn', label: '组织 Base DN' }, { key: 'orgFilters', label: '组织过滤器' },
      { key: 'msadDomain', label: 'AD 域' }, { key: 'syncStartTime', label: '同步开始时间' }, { key: 'trustStore', label: '信任库' },
      { key: 'trustStorePassword', label: '信任库密码' }, { key: 'sslSwitch', label: '启用 SSL', type: 'switch' },
      { key: 'description', label: '描述', type: 'textarea' }, commonStatus,
    ],
  },
  { key: 'institutions', title: '机构配置', base: '/config/institutions', singleton: true, columns: [{ key: 'name', label: '名称' }, { key: 'code', label: '编码' }, { key: 'domain', label: '域名' }, { key: 'email', label: '邮箱' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'fullName', label: '全称' }, { key: 'code', label: '编码' }, { key: 'domain', label: '域名' }, { key: 'frontTitle', label: '前台标题' }, { key: 'consoleTitle', label: '控制台标题' }, { key: 'consoleDomain', label: '控制台域名' }, { key: 'defaultUri', label: '默认地址' }, { key: 'address', label: '地址' }, { key: 'contact', label: '联系人' }, { key: 'phone', label: '电话' }, { key: 'email', label: '邮箱' }, { key: 'logo', label: 'Logo' }, commonStatus] },
  { key: 'connectors', title: '连接器配置', base: '/config/connectors', searchFields: ['connName', 'providerUrl'], columns: [{ key: 'connName', label: '名称' }, { key: 'providerUrl', label: '服务地址' }, { key: 'principal', label: '连接账号' }, { key: 'status', label: '状态' }], fields: [{ key: 'connName', label: '名称', required: true }, { key: 'providerUrl', label: '服务地址', required: true }, { key: 'principal', label: '连接账号' }, { key: 'credentials', label: '连接密码' }, { key: 'filters', label: '过滤条件' }, { key: 'scheduler', label: '调度表达式' }, { key: 'justInTime', label: '即时同步', type: 'select', options: [{ label: '启用', value: 1 }, { label: '停用', value: 0 }] }, commonStatus] },
  { key: 'adapters', title: '适配器配置', base: '/config/adapters', searchFields: ['name', 'protocol'], columns: [{ key: 'name', label: '名称' }, { key: 'protocol', label: '协议' }, { key: 'adapter', label: '适配器实现' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'protocol', label: '协议', required: true }, { key: 'adapter', label: '适配器实现', required: true }, commonStatus] },
  { key: 'accountsstrategy', title: '账号策略', base: '/config/accountsstrategy', columns: [{ key: 'name', label: '名称' }, { key: 'appName', label: '应用' }, { key: 'createType', label: '创建方式' }, { key: 'status', label: '状态' }], fields: [{ key: 'name', label: '名称', required: true }, { key: 'appId', label: '应用标识' }, { key: 'appName', label: '应用名称' }, { key: 'createType', label: '创建方式' }, { key: 'suffixes', label: '账号后缀' }, { key: 'filters', label: '匹配过滤器' }, { key: 'mapping', label: '账号映射' }, { key: 'orgIdsList', label: '组织范围' }, { key: 'description', label: '描述', type: 'textarea' }, commonStatus] },
  { key: 'socialsprovider', title: '社会化登录', base: '/config/socialsprovider', columns: [{ key: 'provider', label: '提供方' }, { key: 'providerName', label: '显示名称' }, { key: 'clientId', label: '客户端标识' }, { key: 'status', label: '状态' }], fields: [{ key: 'provider', label: '提供方', required: true }, { key: 'providerName', label: '显示名称' }, { key: 'clientId', label: '客户端标识' }, { key: 'clientSecret', label: '客户端密钥' }, { key: 'agentId', label: 'Agent ID' }, { key: 'icon', label: '图标' }, { key: 'display', label: '显示名称' }, { key: 'sortIndex', label: '排序', type: 'number' }, { key: 'scanCode', label: '扫码登录', type: 'switch' }, commonStatus] },
  { key: 'ldapcontext', title: 'LDAP 配置', base: '/config/ldapcontext', singleton: true, columns: [{ key: 'providerUrl', label: '地址' }, { key: 'basedn', label: 'Base DN' }, { key: 'status', label: '状态' }], fields: [{ key: 'product', label: '产品类型', type: 'select', required: true, options: [{ label: 'ActiveDirectory', value: 'ActiveDirectory' }, { label: 'OpenLDAP', value: 'OpenLDAP' }, { label: 'StandardLDAP', value: 'StandardLDAP' }] }, { key: 'providerUrl', label: '地址', required: true }, { key: 'basedn', label: 'Base DN' }, { key: 'msadDomain', label: 'AD 域' }, { key: 'principal', label: '账号', required: true }, { key: 'credentials', label: '密码', required: true }, { key: 'filters', label: '过滤条件' }, { key: 'trustStore', label: '信任库' }, { key: 'trustStorePassword', label: '信任库密码' }, { key: 'sslSwitch', label: '启用 SSL', type: 'switch' }, { key: 'accountMapping', label: '账号映射', type: 'switch' }, commonStatus] },
  { key: 'emailsenders', title: '邮件发送器', base: '/config/emailsenders', singleton: true, columns: [{ key: 'sender', label: '发件人' }, { key: 'smtpHost', label: 'SMTP 服务器' }, { key: 'status', label: '状态' }], fields: [{ key: 'sender', label: '发件人', required: true }, { key: 'smtpHost', label: 'SMTP 服务器' }, { key: 'port', label: '端口', type: 'number' }, { key: 'account', label: '账号' }, { key: 'credentials', label: '密码' }, { key: 'protocol', label: '协议' }, { key: 'encoding', label: '编码' }, { key: 'sslSwitch', label: '启用 SSL', type: 'switch' }, commonStatus] },
  { key: 'smsprovider', title: '短信服务商', base: '/config/smsprovider', singleton: true, columns: [{ key: 'provider', label: '提供方' }, { key: 'appKey', label: 'AppKey' }, { key: 'status', label: '状态' }], fields: [{ key: 'provider', label: '提供方', required: true }, { key: 'appKey', label: 'AppKey' }, { key: 'appSecret', label: 'AppSecret' }, { key: 'smsSdkAppId', label: 'SDK AppId' }, { key: 'signName', label: '签名' }, { key: 'templateId', label: '模板 ID' }, { key: 'message', label: '消息模板', type: 'textarea' }, commonStatus] },
  { key: 'passwordpolicy', title: '密码策略', base: '/config/passwordpolicy', singleton: true, columns: [{ key: 'minLength', label: '最小长度' }, { key: 'maxLength', label: '最大长度' }, { key: 'expiration', label: '有效期' }], fields: [{ key: 'minLength', label: '最小长度', type: 'number' }, { key: 'maxLength', label: '最大长度', type: 'number' }, { key: 'expiration', label: '有效期', type: 'number' }, { key: 'history', label: '历史密码数', type: 'number' }, { key: 'attempts', label: '最大尝试次数', type: 'number' }, { key: 'duration', label: '锁定时长', type: 'number' }, { key: 'digits', label: '数字要求', type: 'number' }, { key: 'lowerCase', label: '小写字母要求', type: 'number' }, { key: 'upperCase', label: '大写字母要求', type: 'number' }, { key: 'specialChar', label: '特殊字符要求', type: 'number' }, { key: 'alphabetical', label: '禁止连续字母', type: 'switch' }, { key: 'dictionary', label: '字典检查', type: 'switch' }, { key: 'numerical', label: '禁止连续数字', type: 'switch' }, { key: 'qwerty', label: '键盘序列检查', type: 'switch' }, { key: 'username', label: '禁止包含用户名', type: 'switch' }] },
  { key: 'historys', title: '历史审计', base: '/historys', searchFields: ['username', 'action'], columns: [{ key: 'username', label: '用户' }, { key: 'action', label: '操作' }, { key: 'createTime', label: '时间' }, { key: 'result', label: '结果' }], fields: [] },
]

export function getAdminResource(key: string): AdminResource {
  return adminResources.find(item => item.key === key) || adminResources[0]
}

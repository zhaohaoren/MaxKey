import { createRouter, createWebHashHistory } from 'vue-router'
import { auth } from './auth'
import AdminView from './views/AdminView.vue'
import LoginView from './views/LoginView.vue'
import PortalView from './views/PortalView.vue'
import PortalFeatureView from './views/PortalFeatureView.vue'
import PublicAuthView from './views/PublicAuthView.vue'
import TfaView from './views/TfaView.vue'
import AuditView from './views/AuditView.vue'
import AuthzView from './views/AuthzView.vue'
import AuthCallbackView from './views/AuthCallbackView.vue'
import AdminDashboardView from './views/AdminDashboardView.vue'
import PermissionRoleView from './views/PermissionRoleView.vue'
import AppManagementView from './views/AppManagementView.vue'
import MemberAssignmentView from './views/MemberAssignmentView.vue'
import AccessAssignmentView from './views/AccessAssignmentView.vue'
import GroupPermissionView from './views/GroupPermissionView.vue'
import ResourceManagementView from './views/ResourceManagementView.vue'

const routerBase = '/maxkey/'

// OAuth providers return to a normal URL, while this app uses hash routing.
// Move direct app paths into the hash before Vue Router reads the location.
if (!window.location.hash && window.location.pathname.startsWith(routerBase) && window.location.pathname !== routerBase) {
  const routePath = `/${window.location.pathname.slice(routerBase.length)}`
  window.history.replaceState(null, '', `${routerBase}#${routePath}${window.location.search}`)
}

const router = createRouter({
  history: createWebHashHistory(routerBase),
  routes: [
    { path: '/', redirect: '/dashboard/home' },
    { path: '/login', redirect: '/passport/login' },
    { path: '/passport/login', component: LoginView },
    { path: '/passport/tfa', component: TfaView },
    { path: '/passport/forgot', component: PublicAuthView },
    { path: '/passport/register', component: PublicAuthView },
    { path: '/passport/register-result', component: PublicAuthView },
    { path: '/passport/callback/:provider', component: AuthCallbackView, meta: { callbackMode: 'social' } },
    { path: '/passport/jwt/auth', component: AuthCallbackView, meta: { callbackMode: 'jwt' } },
    { path: '/passport/trust/auth', component: AuthCallbackView, meta: { callbackMode: 'trust' } },
    { path: '/passport/logout', component: AuthCallbackView, meta: { callbackMode: 'logout' } },
    { path: '/dashboard/home', component: PortalView, meta: { requiresAuth: true } },
    { path: '/access/sessions', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/profile', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/password', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/mfa', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/timebased', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/passkey', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/audit/audit-logins', component: AuditView, meta: { requiresAuth: true, audit: 'logins' } },
    { path: '/audit/audit-login-apps', component: AuditView, meta: { requiresAuth: true, audit: 'apps' } },
    { path: '/audit/audit-system-logs', component: AuditView, meta: { requiresAuth: true, audit: 'systems' } },
    { path: '/authz/credential', component: AuthzView, meta: { requiresAuth: true, authz: 'credential' } },
    { path: '/authz/oauth2approve', component: AuthzView, meta: { requiresAuth: true, authz: 'oauth' } },
    { path: '/authz/mgt', component: AuthzView, meta: { requiresAuth: true, authz: 'mgt' } },
    { path: '/portal', redirect: '/dashboard/home' },
    { path: '/admin', component: AdminDashboardView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/apps', component: AppManagementView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/groupmembers', component: MemberAssignmentView, meta: { requiresAuth: true, requiresAdmin: true, assignment: 'group' } },
    { path: '/admin/roleMembers', component: MemberAssignmentView, meta: { requiresAuth: true, requiresAdmin: true, assignment: 'role' } },
    { path: '/admin/permissionRole', component: PermissionRoleView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/access', component: AccessAssignmentView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/permission', component: GroupPermissionView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/resources', component: ResourceManagementView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/config/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/permissions/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/access/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/audit/:audit', component: AuditView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/:resource?', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/idm/organizations', redirect: '/admin/orgs' },
    { path: '/idm/users', redirect: '/admin/users' },
    { path: '/idm/groups', redirect: '/admin/groups' },
    { path: '/idm/groupmembers', redirect: '/admin/groupmembers' },
    { path: '/organizations', redirect: '/admin/orgs' },
    { path: '/users', redirect: '/admin/users' },
    { path: '/apps', redirect: '/admin/apps' },
    { path: '/accounts', redirect: '/admin/accounts' },
    { path: '/access', redirect: '/admin/access' },
    { path: '/access/access', redirect: '/admin/access' },
    { path: '/access/permissions', redirect: '/admin/access' },
    { path: '/access/groups', redirect: '/admin/groups' },
    { path: '/access/groupmembers', redirect: '/admin/groupmembers' },
    { path: '/permissions/roles', redirect: '/admin/roles' },
    { path: '/permissions/rolemembers', redirect: '/admin/roleMembers' },
    { path: '/permissions/resources', redirect: '/admin/resources' },
    { path: '/permissions/apps', redirect: '/admin/apps' },
    { path: '/permissions/apps/permission', redirect: to => ({ path: '/admin/permission', query: to.query }) },
    { path: '/permissions/apps/resources', redirect: to => ({ path: '/admin/resources', query: to.query }) },
    { path: '/permissions/apps/roles', redirect: to => ({ path: '/admin/roles', query: to.query }) },
    { path: '/permissions/apps/rolemembers', redirect: to => ({ path: '/admin/roleMembers', query: to.query }) },
    { path: '/config/institutions', redirect: '/admin/institutions' },
    { path: '/config/synchronizers', redirect: '/admin/synchronizers' },
    { path: '/config/connectors', redirect: '/admin/connectors' },
    { path: '/config/adapters', redirect: '/admin/adapters' },
    { path: '/config/socialsproviders', redirect: '/admin/socialsprovider' },
    { path: '/config/accountsstrategys', redirect: '/admin/accountsstrategy' },
    { path: '/config/ldapcontext', redirect: '/admin/ldapcontext' },
    { path: '/config/emailsender', redirect: '/admin/emailsenders' },
    { path: '/config/smsprovider', redirect: '/admin/smsprovider' },
    { path: '/config/passwordpolicy', redirect: '/admin/passwordpolicy' },
    { path: '/:pathMatch(.*)*', redirect: '/portal' },
  ],
})

router.beforeEach(to => {
  if (to.meta.requiresAuth && !auth.isAuthenticated.value) {
    return { path: '/passport/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin.value) {
    return { path: '/portal' }
  }
  return true
})

window.addEventListener('maxkey:unauthorized', () => {
  if (router.currentRoute.value.path !== '/passport/login') router.push('/passport/login')
})

export default router

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
import PermissionAppsView from './views/PermissionAppsView.vue'
import MemberAssignmentView from './views/MemberAssignmentView.vue'
import AccessAssignmentView from './views/AccessAssignmentView.vue'
import GroupPermissionView from './views/GroupPermissionView.vue'
import ResourceManagementView from './views/ResourceManagementView.vue'
import NoticesView from './views/NoticesView.vue'
import ConfigSingletonView from './views/ConfigSingletonView.vue'
import ConfigListView from './views/ConfigListView.vue'
import AccountsManagementView from './views/AccountsManagementView.vue'
import OrganizationsView from './views/OrganizationsView.vue'
import UsersView from './views/UsersView.vue'
import GroupsView from './views/GroupsView.vue'
import GroupMembersView from './views/GroupMembersView.vue'
import SessionsView from './views/SessionsView.vue'
import PortalSessionsView from './views/PortalSessionsView.vue'
import ProfileView from './views/ProfileView.vue'
import MfaView from './views/MfaView.vue'
import TimebasedView from './views/TimebasedView.vue'
import PasskeyView from './views/PasskeyView.vue'

const routerBase = '/maxkey/'

// OAuth providers return to a normal URL, while this app uses hash routing.
// Move direct app paths into the hash before Vue Router reads the location.
if (!window.location.hash && window.location.pathname.startsWith(routerBase) && window.location.pathname !== routerBase) {
  const routePath = `/${window.location.pathname.slice(routerBase.length)}`
  window.history.replaceState(null, '', `${routerBase}#${routePath}${window.location.search}`)
}

const router = createRouter({
  history: createWebHashHistory(routerBase),
  scrollBehavior: () => ({ top: 0 }),
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
    { path: '/access/sessions', component: PortalSessionsView, meta: { requiresAuth: true } },
    { path: '/config/profile', component: ProfileView, meta: { requiresAuth: true } },
    { path: '/config/password', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/mfa', component: MfaView, meta: { requiresAuth: true } },
    { path: '/config/timebased', component: TimebasedView, meta: { requiresAuth: true } },
    { path: '/config/passkey', component: PasskeyView, meta: { requiresAuth: true } },
    { path: '/audit/audit-logins', component: AuditView, meta: { requiresAuth: true, audit: 'logins' } },
    { path: '/audit/audit-login-apps', component: AuditView, meta: { requiresAuth: true, audit: 'apps' } },
    { path: '/audit/audit-system-logs', component: AuditView, meta: { requiresAuth: true, audit: 'systems' } },
    { path: '/authz/credential', component: AuthzView, meta: { requiresAuth: true, authz: 'credential' } },
    { path: '/authz/oauth2approve', component: AuthzView, meta: { requiresAuth: true, authz: 'oauth' } },
    { path: '/authz/mgt', component: AuthzView, meta: { requiresAuth: true, authz: 'mgt' } },
    { path: '/portal', redirect: '/dashboard/home' },
    { path: '/admin', component: AdminDashboardView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/apps', component: AppManagementView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/orgs', component: OrganizationsView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/users', component: UsersView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/groups', component: GroupsView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/groupmembers', component: GroupMembersView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/roleMembers', component: MemberAssignmentView, meta: { requiresAuth: true, requiresAdmin: true, assignment: 'role' } },
    { path: '/admin/permissionRole', component: PermissionRoleView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/access', component: AccessAssignmentView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/sessions', component: SessionsView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/notices', component: NoticesView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/institutions', component: ConfigSingletonView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'institutions' } },
    { path: '/admin/accounts', component: AccountsManagementView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/synchronizers', component: ConfigListView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'synchronizers' } },
    { path: '/admin/connectors', component: ConfigListView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'connectors' } },
    { path: '/admin/socialsprovider', component: ConfigListView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'socialsprovider' } },
    { path: '/admin/ldapcontext', component: ConfigSingletonView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'ldapcontext' } },
    { path: '/admin/emailsenders', component: ConfigSingletonView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'emailsenders' } },
    { path: '/admin/smsprovider', component: ConfigSingletonView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'smsprovider' } },
    { path: '/admin/passwordpolicy', component: ConfigSingletonView, meta: { requiresAuth: true, requiresAdmin: true, configPage: 'passwordpolicy' } },
    { path: '/admin/permission', component: GroupPermissionView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/resources', component: ResourceManagementView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/permissions/apps', component: PermissionAppsView, meta: { requiresAuth: true, requiresAdmin: true } },
    {
      path: '/admin/config/:resource',
      redirect: to => {
        const resource = String(to.params.resource || '')
        const target = ({ socialsproviders: 'socialsprovider', emailsender: 'emailsenders' } as Record<string, string>)[resource] || resource
        return { path: `/admin/${target}`, query: to.query }
      },
    },
    { path: '/admin/permissions/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/access/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/audit/:audit', component: AuditView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
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
    { path: '/permissions/permission', redirect: to => ({ path: '/admin/permission', query: to.query }) },
    { path: '/permissions/apps', redirect: '/admin/permissions/apps' },
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
    { path: '/config/notices', redirect: '/admin/notices' },
    { path: '/audit/audit-synchronizer', redirect: '/admin/audit/synchronizers' },
    { path: '/audit/audit-connector', redirect: '/admin/audit/connectors' },
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

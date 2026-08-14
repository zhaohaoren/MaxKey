import { createRouter, createWebHashHistory } from 'vue-router'
import { auth } from './auth'
import AdminView from './views/AdminView.vue'
import LoginView from './views/LoginView.vue'
import PortalView from './views/PortalView.vue'
import PortalFeatureView from './views/PortalFeatureView.vue'
import PublicAuthView from './views/PublicAuthView.vue'
import TfaView from './views/TfaView.vue'

const router = createRouter({
  history: createWebHashHistory('/maxkey/'),
  routes: [
    { path: '/', redirect: '/dashboard/home' },
    { path: '/login', redirect: '/passport/login' },
    { path: '/passport/login', component: LoginView },
    { path: '/passport/tfa', component: TfaView },
    { path: '/passport/forgot', component: PublicAuthView },
    { path: '/passport/register', component: PublicAuthView },
    { path: '/passport/register-result', component: PublicAuthView },
    { path: '/dashboard/home', component: PortalView, meta: { requiresAuth: true } },
    { path: '/access/sessions', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/profile', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/password', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/mfa', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/timebased', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/config/passkey', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/audit/audit-logins', component: PortalFeatureView, meta: { requiresAuth: true } },
    { path: '/portal', redirect: '/dashboard/home' },
    { path: '/admin/config/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/permissions/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/access/:resource', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
    { path: '/admin/:resource?', component: AdminView, meta: { requiresAuth: true, requiresAdmin: true } },
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
  if ((to.path === '/login' || to.path === '/passport/login') && auth.isAuthenticated.value) {
    return { path: '/dashboard/home' }
  }
  return true
})

window.addEventListener('maxkey:unauthorized', () => {
  if (router.currentRoute.value.path !== '/passport/login') router.push('/passport/login')
})

export default router

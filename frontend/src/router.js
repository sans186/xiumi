import { createRouter, createWebHistory } from 'vue-router'
import { store, loadMe } from './store'
import { token } from './api'

const routes = [
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { public: true } },
  {
    path: '/change-password',
    name: 'change-password',
    component: () => import('./views/ChangePwdView.vue'),
    meta: { needLogin: true, allowMustChange: true }
  },
  { path: '/', name: 'hall', component: () => import('./views/TaskHallView.vue') },
  { path: '/tasks/new', name: 'task-create', component: () => import('./views/TaskCreateView.vue') },
  { path: '/tasks/:id', name: 'task-detail', component: () => import('./views/TaskDetailView.vue') },
  { path: '/review', name: 'review', component: () => import('./views/ReviewView.vue') },
  { path: '/rank', name: 'rank', component: () => import('./views/RankView.vue') },
  { path: '/members', name: 'members', component: () => import('./views/MembersView.vue') },
  { path: '/groups', name: 'groups', component: () => import('./views/GroupsView.vue') },
  { path: '/notices', name: 'notices', component: () => import('./views/NoticeView.vue') },
  { path: '/settings', name: 'settings', component: () => import('./views/SettingsView.vue') },
  { path: '/me', name: 'me', component: () => import('./views/MeView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.meta.public) return true
  if (!token.get()) return { name: 'login', query: { redirect: to.fullPath } }
  if (!store.user) await loadMe()
  if (!store.user) return { name: 'login' }
  if (store.user.mustChange === 1 && !to.meta.allowMustChange) {
    return { name: 'change-password' }
  }
  if (to.name === 'members' || to.name === 'groups' || to.name === 'settings') {
    if (!store.permissions.canManage) return { name: 'hall' }
  }
  return true
})

export default router

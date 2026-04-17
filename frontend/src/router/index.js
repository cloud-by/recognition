import { createRouter, createWebHistory } from 'vue-router'
import { isAdminUser, isLoggedIn, isManagerUser, isTeacherUser } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: () => import('@/views/HomeView.vue') },
    { path: '/problems', component: () => import('@/views/ProblemsView.vue') },
    { path: '/problems/:id', component: () => import('@/views/ProblemDetailView.vue') },
    { path: '/problems/:id/submit', component: () => import('@/views/SubmitView.vue') },
    { path: '/submissions', component: () => import('@/views/SubmissionsView.vue') },
    { path: '/contests', component: () => import('@/views/ContestsView.vue') },
    { path: '/contests/manage', component: () => import('@/views/ContestManageView.vue'), meta: { requiresManager: true } },
    { path: '/contests/history', component: () => import('@/views/ContestHistoryView.vue'), meta: { requiresManager: true } },
    { path: '/classes/manage', component: () => import('@/views/ClassManageView.vue'), meta: { requiresTeacher: true } },
    { path: '/admin', component: () => import('@/views/AdminView.vue'), meta: { requiresAdmin: true } },
    { path: '/admin/problems/create', component: () => import('@/views/ProblemCreateView.vue'), meta: { requiresAdmin: true } },
    { path: '/login', component: () => import('@/views/LoginView.vue') },
    { path: '/register', component: () => import('@/views/RegisterView.vue') },
  ],
})

router.beforeEach((to) => {
  if ((to.meta.requiresTeacher || to.meta.requiresManager || to.meta.requiresAdmin) && !isLoggedIn()) {
    return '/login'
  }
  if (to.meta.requiresAdmin && !isAdminUser()) return '/login'
  if (to.meta.requiresTeacher && !isTeacherUser()) return '/login'
  if (to.meta.requiresManager && !isManagerUser()) return '/login'
  return true
})

export default router

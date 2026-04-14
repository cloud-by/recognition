import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: () => import('@/views/HomeView.vue') },
    { path: '/problems', component: () => import('@/views/ProblemsView.vue') },
    { path: '/problems/:id', component: () => import('@/views/ProblemDetailView.vue') },
    { path: '/submissions', component: () => import('@/views/SubmissionsView.vue') },
    { path: '/contests', component: () => import('@/views/ContestsView.vue') },
    { path: '/admin', component: () => import('@/views/AdminView.vue') },
  ],
})

export default router

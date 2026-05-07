import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Library',
      component: () => import('../views/LibraryView.vue'),
    },
    {
      path: '/asset/:id',
      name: 'AssetDetail',
      component: () => import('../views/AssetDetailView.vue'),
    },
    {
      path: '/workshop',
      name: 'Workshop',
      component: () => import('../views/WorkshopLayout.vue'),
      redirect: '/workshop/templates',
      children: [
        {
          path: 'templates',
          name: 'WorkshopTemplates',
          component: () => import('../components/workshop/TemplateBuilder.vue'),
        },
        {
          path: 'tasks',
          name: 'WorkshopTasks',
          component: () => import('../components/workshop/TaskCenter.vue'),
        },
        {
          path: 'tasks/:id',
          name: 'TaskDetail',
          component: () => import('../views/TaskDetailView.vue'),
        },
        {
          path: 'records',
          name: 'WorkshopRecords',
          component: () => import('../views/TaskHistoryView.vue'),
        },
        {
          path: 'records/:id',
          name: 'RecordDetail',
          component: () => import('../views/TaskDetailView.vue'),
        },
      ],
    },
  ],
})

export default router

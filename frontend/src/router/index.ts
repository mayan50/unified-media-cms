import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Library',
      component: () => import('../views/library/LibraryView.vue'),
    },
    {
      path: '/asset/:id',
      name: 'AssetDetail',
      component: () => import('../views/library/AssetDetailView.vue'),
    },
    {
      path: '/settings',
      component: () => import('../views/SettingsLayout.vue'),
      redirect: '/settings/storage',
      children: [
        {
          path: 'storage',
          name: 'SettingsStorage',
          component: () => import('../views/settings/SettingsStorageView.vue'),
        },
        {
          path: 'llm',
          name: 'SettingsLlm',
          component: () => import('../views/settings/SettingsLlmView.vue'),
        },
        {
          path: 'categories-tags',
          name: 'SettingsCategoriesTags',
          component: () => import('../views/settings/SettingsCategoriesTagsView.vue'),
        },
        {
          path: 'plugins',
          name: 'SettingsPlugins',
          component: () => import('../views/settings/SettingsPluginView.vue'),
        },
      ],
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
          component: () => import('../views/workshop/TemplateBuilder.vue'),
        },
        {
          path: 'jobs',
          name: 'WorkshopJobs',
          component: () => import('../views/workshop/JobCenter.vue'),
        },
        {
          path: 'jobs/:id',
          name: 'JobDetail',
          component: () => import('../views/workshop/JobDetailView.vue'),
        },
        {
          path: 'tasks',
          name: 'WorkshopTasks',
          component: () => import('../views/workshop/TaskListView.vue'),
        },
        {
          path: 'tasks/:id',
          name: 'TaskDetail',
          component: () => import('../views/workshop/TaskDetailView.vue'),
        },
      ],
    },
  ],
})

export default router

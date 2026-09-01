import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '仪表盘' },
      },
      {
        path: 'masterdata/products',
        name: 'products',
        component: () => import('@/views/masterdata/ProductList.vue'),
        meta: { title: '商品档案' },
      },
      {
        path: 'masterdata/categories',
        name: 'categories',
        component: () => import('@/views/masterdata/CategoryList.vue'),
        meta: { title: '商品分类' },
      },
      {
        path: 'masterdata/warehouses',
        name: 'warehouses',
        component: () => import('@/views/masterdata/WarehouseList.vue'),
        meta: { title: '仓库档案' },
      },
      {
        path: 'masterdata/customers',
        name: 'customers',
        component: () => import('@/views/masterdata/CustomerList.vue'),
        meta: { title: '客户档案' },
      },
      {
        path: 'masterdata/suppliers',
        name: 'suppliers',
        component: () => import('@/views/masterdata/SupplierList.vue'),
        meta: { title: '供应商档案' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title as string} - 贸易ERP` : '贸易ERP'

  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/'
  }
})

export default router

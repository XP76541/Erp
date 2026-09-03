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
        path: 'purchase/inbounds',
        name: 'purchaseInbounds',
        component: () => import('@/views/purchase/PurchaseInboundList.vue'),
        meta: { title: '采购入库单' },
      },
      {
        path: 'purchase/returns',
        name: 'purchaseReturns',
        component: () => import('@/views/purchase/PurchaseReturnList.vue'),
        meta: { title: '采购退货单' },
      },
      {
        path: 'finance/payments',
        name: 'payments',
        component: () => import('@/views/finance/PaymentList.vue'),
        meta: { title: '供应商付款' },
      },
      {
        path: 'finance/payables',
        name: 'payables',
        component: () => import('@/views/finance/PayableList.vue'),
        meta: { title: '付款核销' },
      },
      {
        path: 'masterdata/suppliers',
        name: 'suppliers',
        component: () => import('@/views/masterdata/SupplierList.vue'),
        meta: { title: '供应商档案' },
      },
      {
        path: 'sales/orders',
        name: 'salesOrders',
        component: () => import('@/views/sales/SalesOrderList.vue'),
        meta: { title: '销售订单' },
      },
      {
        path: 'sales/outbounds',
        name: 'salesOutbounds',
        component: () => import('@/views/sales/SalesOutboundList.vue'),
        meta: { title: '销售出库单' },
      },
      {
        path: 'finance/aging-report',
        name: 'agingReport',
        component: () => import('@/views/finance/AgingReport.vue'),
        meta: { title: '账龄分析' },
      },
      {
        path: 'finance/receivables',
        name: 'receivables',
        component: () => import('@/views/finance/ReceivableList.vue'),
        meta: { title: '应收核销' },
      },
      {
        path: 'inventory/ledgers',
        name: 'inventoryLedgers',
        component: () => import('@/views/inventory/InventoryLedgerList.vue'),
        meta: { title: '出入库流水' },
      },
      {
        path: 'inventory/stocks',
        name: 'inventoryStocks',
        component: () => import('@/views/inventory/InventoryStockList.vue'),
        meta: { title: '即时库存' },
      },
      {
        path: 'inventory/transfers',
        name: 'inventoryTransfers',
        component: () => import('@/views/inventory/InventoryTransferList.vue'),
        meta: { title: '库存调拨' },
      },
      {
        path: 'inventory/checks',
        name: 'inventoryChecks',
        component: () => import('@/views/inventory/InventoryCheckList.vue'),
        meta: { title: '库存盘点' },
      },
      {
        path: 'system/users',
        name: 'systemUsers',
        component: () => import('@/views/system/UserManagement.vue'),
        meta: { title: '用户管理' },
      },
      {
        path: 'system/roles',
        name: 'systemRoles',
        component: () => import('@/views/system/RoleManagement.vue'),
        meta: { title: '角色权限' },
      },
      {
        path: 'system/logs',
        name: 'systemLogs',
        component: () => import('@/views/system/OperationLogList.vue'),
        meta: { title: '操作日志' },
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

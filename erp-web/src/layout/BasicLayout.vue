<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">贸易 ERP</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="#c8c9cc"
        active-text-color="#ffffff"
      >
        <el-menu-item v-if="canAccess('/dashboard')" index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <el-sub-menu v-if="visibleChildren('masterdata').length" index="masterdata">
          <template #title>
            <el-icon><Goods /></el-icon>
            <span>基础数据</span>
          </template>
          <el-menu-item v-if="canAccess('/masterdata/products')" index="/masterdata/products">商品档案</el-menu-item>
          <el-menu-item v-if="canAccess('/masterdata/categories')" index="/masterdata/categories">商品分类</el-menu-item>
          <el-menu-item v-if="canAccess('/masterdata/warehouses')" index="/masterdata/warehouses">仓库档案</el-menu-item>
          <el-menu-item v-if="canAccess('/masterdata/customers')" index="/masterdata/customers">客户档案</el-menu-item>
          <el-menu-item v-if="canAccess('/masterdata/suppliers')" index="/masterdata/suppliers">供应商档案</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visibleChildren('sales').length" index="sales">
          <template #title>
            <el-icon><SoldOut /></el-icon>
            <span>销售管理</span>
          </template>
          <el-menu-item v-if="canAccess('/sales/orders')" index="/sales/orders">销售订单</el-menu-item>
          <el-menu-item v-if="canAccess('/sales/outbounds')" index="/sales/outbounds">销售出库单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visibleChildren('purchase').length" index="purchase">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>采购管理</span>
          </template>
          <el-menu-item v-if="canAccess('/purchase/inbounds')" index="/purchase/inbounds">采购入库单</el-menu-item>
          <el-menu-item v-if="canAccess('/purchase/returns')" index="/purchase/returns">采购退货单</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visibleChildren('finance').length" index="finance">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>财务管理</span>
          </template>
          <el-menu-item v-if="canAccess('/finance/payables')" index="/finance/payables">应付账款</el-menu-item>
          <el-menu-item v-if="canAccess('/finance/payments')" index="/finance/payments">供应商付款</el-menu-item>
          <el-menu-item v-if="canAccess('/finance/receivables')" index="/finance/receivables">应收核销</el-menu-item>
          <el-menu-item v-if="canAccess('/finance/aging-report')" index="/finance/aging-report">应收账龄</el-menu-item>
          <el-menu-item v-if="canAccess('/finance/reports')" index="/finance/reports">经营报表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visibleChildren('inventory').length" index="inventory">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>库存管理</span>
          </template>
          <el-menu-item v-if="canAccess('/inventory/stocks')" index="/inventory/stocks">即时库存</el-menu-item>
          <el-menu-item v-if="canAccess('/inventory/ledgers')" index="/inventory/ledgers">出入库流水</el-menu-item>
          <el-menu-item v-if="canAccess('/inventory/transfers')" index="/inventory/transfers">库存调拨</el-menu-item>
          <el-menu-item v-if="canAccess('/inventory/checks')" index="/inventory/checks">库存盘点</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="visibleChildren('system').length" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-if="canAccess('/system/users')" index="/system/users">用户管理</el-menu-item>
          <el-menu-item v-if="canAccess('/system/roles')" index="/system/roles">角色权限</el-menu-item>
          <el-menu-item v-if="canAccess('/system/logs')" index="/system/logs">操作日志</el-menu-item>
          <el-menu-item v-if="canAccess('/system/backups')" index="/system/backups">数据库备份</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ route.meta.title }}</div>
        <el-dropdown @command="handleCommand">
          <span class="user">
            <el-icon><UserFilled /></el-icon>
            {{ userStore.realName || '未登录' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Box,
  Goods,
  Odometer,
  Setting,
  ShoppingCart,
  SoldOut,
  UserFilled,
  Wallet,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const canAccess = (path: string) => {
  const routeRecord = router.getRoutes().find((record) => record.path === path)
  const roles = routeRecord?.meta.roles as string[] | undefined
  return !roles?.length || userStore.hasAnyRole(...roles)
}

const visibleChildren = (group: string) =>
  router.getRoutes().filter((record) => record.path.startsWith(`/${group}/`) && canAccess(record.path))

async function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    await router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #001529;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.aside :deep(.el-menu) {
  border-right: none;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header-title {
  font-size: 16px;
  color: #303133;
}

.user {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #606266;
}

.main {
  background: #f0f2f5;
}
</style>

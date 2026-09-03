<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="单号"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
          @clear="load"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px" @change="handleSearch">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已审核" value="AUDITED" />
          <el-option label="作废" value="VOID" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreateFromOrder">创建出库单</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="docNo" label="单号" width="160" />
        <el-table-column label="销售订单" width="160">
          <template #default="{ row }">{{ row.orderDocNo || '—' }}</template>
        </el-table-column>
        <el-table-column label="客户" min-width="160">
          <template #default="{ row }">{{ customerName(row.customerId) }}</template>
        </el-table-column>
        <el-table-column label="发货仓库" width="110">
          <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
        </el-table-column>
        <el-table-column prop="bizDate" label="业务日期" width="110" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="handleAudit(row)">审核</el-button>
            <el-button link type="info" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @change="load"
      />
    </el-card>

    <!-- 从订单创建出库单 -->
    <el-dialog v-model="createDialogVisible" title="创建销售出库单" width="800px">
      <el-alert title="请选择要发货的销售订单" type="info" :closable="false" class="order-alert" />

      <el-form ref="orderFormRef" :model="orderQuery" :rules="orderRules" label-width="80px" class="mt-20">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="客户" prop="customerId">
              <el-select v-model="orderQuery.customerId" clearable placeholder="选择客户" @change="loadOrderList">
                <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务日期" prop="bizDate">
              <el-date-picker
                v-model="orderQuery.bizDate"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                @change="loadOrderList"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 订单列表 -->
      <el-table v-loading="orderLoading" :data="orderList" border size="small" @row-click="selectOrder">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="docNo" label="订单号" width="160" />
        <el-table-column label="客户" width="150">
          <template #default="{ row }">{{ customerName(row.customerId) }}</template>
        </el-table-column>
        <el-table-column prop="bizDate" label="业务日期" width="110" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag type="success" size="small">已审核</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发货状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getShipStatusType(row.shipStatus)" size="small">
              {{ getShipStatusText(row.shipStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100" align="right">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
        </el-table-column>
      </el-table>

      <!-- 出库单信息 -->
      <div v-if="selectedOrder" class="outbound-info">
        <h4>出库单信息</h4>
        <el-form ref="outboundFormRef" :model="outboundForm" :rules="outboundRules" label-width="90px">
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="出库仓库" prop="warehouseId">
                <el-select v-model="outboundForm.warehouseId" style="width: 100%">
                  <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id!" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="业务日期" prop="bizDate">
                <el-date-picker v-model="outboundForm.bizDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 发货明细 -->
          <el-table :data="outboundForm.items" border size="small">
            <el-table-column label="商品" width="200">
              <template #default="{ row }">
                {{ productName(row.productId) }}
              </template>
            </el-table-column>
            <el-table-column label="订单数量" width="100" align="right">
              <template #default="{ row }">{{ row.orderedQty?.toFixed(4) }}</template>
            </el-table-column>
            <el-table-column label="已发货" width="100" align="right">
              <template #default="{ row }">{{ row.shippedQty?.toFixed(4) }}</template>
            </el-table-column>
            <el-table-column label="本次发货" width="120">
              <template #default="{ $index }">
                <el-input-number
                  v-model="outboundForm.items[$index].qty"
                  :min="0.0001"
                  :max="outboundForm.items[$index].remainingQty"
                  :precision="4"
                  :controls="false"
                  size="small"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="outboundForm.items.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button plain size="small" :icon="Plus" class="add-line" @click="addOutboundItem">添加发货行</el-button>

          <el-form-item label="备注" prop="remark" class="remark">
            <el-input v-model="outboundForm.remark" maxlength="500" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="creating"
          :disabled="!selectedOrder"
          @click="handleCreateOutbound"
        >
          创建出库单
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog v-model="auditDialogVisible" title="审核销售出库单" width="500px">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="80px">
        <el-form-item label="操作" prop="action">
          <el-radio-group v-model="auditForm.action">
            <el-radio label="audit">通过</el-radio>
            <el-radio label="reject">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="auditForm.remark" type="textarea" :rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditing" @click="handleAuditSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { salesApi } from '@/api/sales'
import type { SalesOutbound, SalesOutboundListResponse, SalesOutboundCreateRequest } from '@/api/sales'
import { customerApi } from '@/api/customer'
import type { Customer } from '@/api/customer'
import { warehouseApi } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'
import { productApi } from '@/api/product'
import type { Product } from '@/api/product'

const loading = ref(false)
const orderLoading = ref(false)
const creating = ref(false)
const auditing = ref(false)
const list = ref<SalesOutboundListResponse[]>([])
const total = ref(0)
const customers = ref<Customer[]>([])
const warehouses = ref<Warehouse[]>([])
const products = ref<Product[]>([])
const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const createDialogVisible = ref(false)
const selectedOrder = ref<SalesOrderListResponse | null>(null)
const currentOutbound = ref<SalesOutbound | null>(null)
const orderList = ref<SalesOrderListResponse[]>([])
const orderQuery = reactive({
  customerId: undefined as number | undefined,
  bizDate: [] as string[],
})

const orderRules: FormRules = {}

const outboundDialogVisible = ref(false)
const outboundFormRef = ref<FormInstance>()
const outboundForm = reactive({
  orderId: 0,
  warehouseId: undefined as number | undefined,
  bizDate: new Date().toISOString().slice(0, 10),
  remark: '',
  items: [] as { orderItemId: number; productId: number; qty: number; remainingQty: number; orderedQty?: number; shippedQty?: number }[],
})

const outboundRules: FormRules = {
  warehouseId: [{ required: true, message: '请选择出库仓库', trigger: 'change' }],
}

const auditDialogVisible = ref(false)
const auditFormRef = ref<FormInstance>()
const auditForm = reactive({
  action: 'audit' as 'audit' | 'reject',
  remark: '',
})

const auditRules: FormRules = {
  action: [{ required: true, message: '请选择操作', trigger: 'change' }],
}

const customerMap = computed(() => new Map(customers.value.map((c) => [c.id!, c.name])))
const productMap = computed(() => new Map(products.value.map((p) => [p.id!, p.name])))

function customerName(id?: number) {
  return id ? (customerMap.value.get(id) ?? '—') : '—'
}

function warehouseName(id?: number) {
  const warehouse = warehouses.value.find(w => w.id === id)
  return warehouse?.name || '—'
}

function productName(id?: number) {
  return id ? (productMap.value.get(id) ?? '—') : '—'
}

function getStatusType(status: string) {
  switch (status) {
    case 'DRAFT': return 'warning'
    case 'AUDITED': return 'success'
    case 'VOID': return 'info'
    default: return ''
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'DRAFT': return '草稿'
    case 'AUDITED': return '已审核'
    case 'VOID': return '作废'
    default: return status
  }
}

function getShipStatusType(status: string) {
  switch (status) {
    case 'UN_SHIPPED': return 'info'
    case 'PART_SHIPPED': return 'warning'
    case 'SHIPPED': return 'success'
    default: return ''
  }
}

function getShipStatusText(status: string) {
  switch (status) {
    case 'UN_SHIPPED': return '未发货'
    case 'PART_SHIPPED': return '部分发货'
    case 'SHIPPED': return '已发货'
    default: return status
  }
}

async function load() {
  loading.value = true
  try {
    const data = await salesApi.getOutbounds({ ...query, status: query.status || undefined })
    list.value = data.records
    total.value = data.total
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  load()
}

async function openCreateFromOrder() {
  createDialogVisible.value = true
  orderQuery.customerId = undefined
  orderQuery.bizDate = []
  orderList.value = []
  selectedOrder.value = null

  // 加载客户列表
  if (!customers.value.length) {
    const data = await customerApi.page({ page: 1, size: 200 })
    customers.value = data.records
  }
}

async function loadOrderList() {
  if (!orderQuery.customerId && (!orderQuery.bizDate || orderQuery.bizDate.length !== 2)) {
    orderList.value = []
    return
  }

  orderLoading.value = true
  try {
    const startDate = orderQuery.bizDate?.[0]
    const endDate = orderQuery.bizDate?.[1]

    if (startDate && endDate) {
      const data = await salesApi.getOrdersByDateRange(startDate, endDate)
      orderList.value = data.filter(order =>
        (!orderQuery.customerId || order.customerId === orderQuery.customerId) &&
        order.status === 'AUDITED' &&
        order.shipStatus !== 'SHIPPED'
      )
    } else if (orderQuery.customerId) {
      const data = await salesApi.getOrdersByCustomer(orderQuery.customerId)
      orderList.value = data.filter(order =>
        order.status === 'AUDITED' &&
        order.shipStatus !== 'SHIPPED'
      )
    }
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    orderLoading.value = false
  }
}

function selectOrder(row: SalesOutboundListResponse) {
  selectedOrder.value = row

  // 加载订单详情
  salesApi.getOrderDetail(row.id!).then(detail => {
    // 加载仓库列表
    if (!warehouses.value.length) {
      warehouseApi.listAll().then(data => {
        warehouses.value = data
      })
    }

    // 加载产品列表
    if (!products.value.length) {
      productApi.page({ page: 1, size: 500 }).then(data => {
        products.value = data.records
      })
    }

    // 初始化出库单表单
    outboundForm.orderId = row.id!
    outboundForm.warehouseId = undefined
    outboundForm.bizDate = new Date().toISOString().slice(0, 10)
    outboundForm.remark = ''
    outboundForm.items = detail.items.map(item => ({
      orderItemId: item.id!,
      productId: item.productId,
      qty: 0,
      remainingQty: (item.qty - (item.shippedQty || 0)),
      orderedQty: item.qty,
      shippedQty: item.shippedQty || 0
    }))
  })
}

function addOutboundItem() {
  outboundForm.items.push({
    orderItemId: 0,
    productId: undefined,
    qty: 1,
    remainingQty: 0
  })
}

async function handleCreateOutbound() {
  const valid = await outboundFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!outboundForm.items.length || outboundForm.items.some((i) => !i.productId)) {
    ElMessage.warning('请为每一行选择商品')
    return
  }

  creating.value = true
  try {
    const result = await salesApi.createOutboundFromOrder(selectedOrder.value!.id!, {
      orderId: selectedOrder.value!.id!,
      warehouseId: outboundForm.warehouseId!,
      bizDate: outboundForm.bizDate,
      remark: outboundForm.remark,
      items: outboundForm.items.map((i) => ({
        orderItemId: i.orderItemId,
        qty: i.qty
      }))
    })
    ElMessage.success('出库单创建成功: ' + result.outboundDocNo)
    createDialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    creating.value = false
  }
}

async function handleAudit(row: SalesOutbound) {
  currentOutbound.value = row
  auditForm.action = 'audit'
  auditForm.remark = ''
  auditDialogVisible.value = true
}

async function handleAuditSubmit() {
  const valid = await auditFormRef.value?.validate().catch(() => false)
  if (!valid) return

  auditing.value = true
  try {
    await salesApi.auditOutbound(currentOutbound.value.id!, auditForm)
    ElMessage.success(auditForm.action === 'audit' ? '审核成功' : '驳回成功')
    auditDialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    auditing.value = false
  }
}

function viewDetail(row: SalesOutbound) {
  ElMessageBox.alert(`出库单号: ${row.docNo}\n客户: ${customerName(row.customerId)}\n业务日期: ${row.bizDate}\n状态: ${getStatusText(row.status)}`, '出库单详情', {
    confirmButtonText: '确定'
  })
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.add-line {
  margin-top: 8px;
}

.remark {
  margin-top: 16px;
}

.order-alert {
  margin-bottom: 16px;
}

.outbound-info {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.mt-20 {
  margin-top: 20px;
}
</style>
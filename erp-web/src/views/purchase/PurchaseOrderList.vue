<template>
  <div class="purchase-order-list">
    <!-- 搜索和操作栏 -->
    <el-card class="search-card">
      <div class="search-bar">
        <el-form :model="searchForm" ref="searchFormRef" :inline="true">
          <el-form-item label="单号" prop="keyword">
            <el-input v-model="searchForm.keyword" placeholder="请输入采购单号" clearable />
          </el-form-item>
          <el-form-item label="供应商" prop="supplierId">
            <el-select v-model="searchForm.supplierId" placeholder="请选择供应商" clearable>
              <el-option
                v-for="supplier in suppliers"
                :key="supplier.id"
                :label="supplier.name"
                :value="supplier.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已审核" value="AUDITED" />
              <el-option label="已作废" value="VOID" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="resetSearch">重置</el-button>
            <el-button type="primary" @click="handleCreate">新建采购订单</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 列表 -->
    <el-card>
      <el-table v-loading="loading" :data="tableData" border style="width: 100%">
        <el-table-column prop="docNo" label="采购单号" width="180" />
        <el-table-column prop="supplierName" label="供应商" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="bizDate" label="业务日期" width="120" />
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="{ row }">
            <span>¥ {{ formatAmount(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'DRAFT'"
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              type="success"
              size="small"
              @click="handleAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT'"
              type="warning"
              size="small"
              @click="handleReject(row)"
            >
              驳回
            </el-button>
            <el-button
              type="info"
              size="small"
              @click="handleView(row)"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 审核对话框 -->
    <el-dialog
      v-model="auditDialogVisible"
      title="审核采购订单"
      width="500px"
      @close="closeAuditDialog"
    >
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules">
        <el-form-item label="审核意见" prop="remark">
          <el-input
            v-model="auditForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入审核意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeAuditDialog">取消</el-button>
        <el-button type="primary" @click="submitAudit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="驳回采购订单"
      width="500px"
      @close="closeRejectDialog"
    >
      <el-form ref="rejectFormRef" :model="rejectForm" :rules="rejectRules">
        <el-form-item label="驳回意见" prop="remark">
          <el-input
            v-model="rejectForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入驳回意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeRejectDialog">取消</el-button>
        <el-button type="danger" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>

    <!-- 采购订单表单 -->
    <PurchaseOrderForm
      v-model:visible="orderFormVisible"
      :order-id="currentOrderId"
      @success="handleFormSuccess"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseOrderApi } from '@/api/purchase'
import { supplierApi } from '@/api/supplier'
import PurchaseOrderForm from './components/PurchaseOrderForm.vue'

const loading = ref(false)
const tableData = ref([])
const suppliers = ref([])

// 搜索表单
const searchForm = ref({
  keyword: '',
  supplierId: null,
  status: ''
})

// 分页
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

// 表单相关
const orderFormVisible = ref(false)
const currentOrderId = ref(null)

// 审核对话框
const auditDialogVisible = ref(false)
const auditForm = ref({
  id: null,
  remark: '',
  ip: ''
})

// 驳回对话框
const rejectDialogVisible = ref(false)
const rejectForm = ref({
  id: null,
  remark: '',
  ip: ''
})

// 表单验证规则
const auditRules = {
  remark: [{ required: true, message: '请输入审核意见', trigger: 'blur' }]
}

const rejectRules = {
  remark: [{ required: true, message: '请输入驳回意见', trigger: 'blur' }]
}

// 获取供应商列表
const getSuppliers = async () => {
  try {
    const response = await supplierApi.page({ page: 1, size: 200 })
    suppliers.value = response.records || []
  } catch (error) {
    console.error('获取供应商列表失败:', error)
  }
}

// 获取采购订单列表
const getPurchaseOrders = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.size,
      ...searchForm.value
    }
    const response = await purchaseOrderApi.list(params)
    tableData.value = response.records || []
    pagination.value.total = response.total || 0
  } catch (error) {
    console.error('获取采购订单列表失败:', error)
    ElMessage.error('获取采购订单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.page = 1
  getPurchaseOrders()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    keyword: '',
    supplierId: null,
    status: ''
  }
  handleSearch()
}

// 新建
const handleCreate = () => {
  currentOrderId.value = null
  orderFormVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  currentOrderId.value = row.id
  orderFormVisible.value = true
}

// 查看
const handleView = (row) => {
  ElMessage.info('查看采购订单详情功能开发中...')
}

// 审核
const handleAudit = (row) => {
  auditForm.value.id = row.id
  auditDialogVisible.value = true
}

// 提交审核
const submitAudit = async () => {
  try {
    await purchaseOrderApi.audit(auditForm.value.id, {
      remark: auditForm.value.remark,
      ip: '127.0.0.1'
    })
    ElMessage.success('审核成功')
    closeAuditDialog()
    getPurchaseOrders()
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  }
}

// 关闭审核对话框
const closeAuditDialog = () => {
  auditDialogVisible.value = false
  auditForm.value = {
    id: null,
    remark: '',
    ip: ''
  }
}

// 驳回
const handleReject = (row) => {
  rejectForm.value.id = row.id
  rejectDialogVisible.value = true
}

// 提交驳回
const submitReject = async () => {
  try {
    await purchaseOrderApi.reject(rejectForm.value.id, {
      remark: rejectForm.value.remark,
      ip: '127.0.0.1'
    })
    ElMessage.success('驳回成功')
    closeRejectDialog()
    getPurchaseOrders()
  } catch (error) {
    console.error('驳回失败:', error)
    ElMessage.error('驳回失败')
  }
}

// 关闭驳回对话框
const closeRejectDialog = () => {
  rejectDialogVisible.value = false
  rejectForm.value = {
    id: null,
    remark: '',
    ip: ''
  }
}

// 状态类型
const getStatusType = (status) => {
  switch (status) {
    case 'DRAFT':
      return 'info'
    case 'AUDITED':
      return 'success'
    case 'VOID':
      return 'danger'
    default:
      return 'info'
  }
}

// 状态文本
const getStatusText = (status) => {
  switch (status) {
    case 'DRAFT':
      return '草稿'
    case 'AUDITED':
      return '已审核'
    case 'VOID':
      return '已作废'
    default:
      return status
  }
}

// 格式化金额
const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return Number(amount).toFixed(2)
}

// 分页
const handleSizeChange = (val) => {
  pagination.value.size = val
  getPurchaseOrders()
}

const handleCurrentChange = (val) => {
  pagination.value.page = val
  getPurchaseOrders()
}

// 表单成功回调
const handleFormSuccess = () => {
  orderFormVisible.value = false
  currentOrderId.value = null
  getPurchaseOrders()
}

// 初始化
onMounted(() => {
  getSuppliers()
  getPurchaseOrders()
})
</script>

<style scoped>
.purchase-order-list {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}
</style>
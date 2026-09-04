<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="单号/客户"
          clearable
          style="width: 180px"
          @keyup.enter="handleSearch"
          @clear="load"
        />
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 130px" @change="handleSearch">
          <el-option label="未结算" value="UNSETTLED" />
          <el-option label="部分结算" value="PARTIAL" />
          <el-option label="已结算" value="SETTLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
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
        <el-table-column prop="businessDate" label="业务日期" width="110" align="center" />
        <el-table-column prop="dueDate" label="到期日" width="110" align="center" />
        <el-table-column label="应收金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="已收金额" width="120" align="right">
          <template #default="{ row }">¥{{ row.paidAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="余额" width="120" align="right">
          <template #default="{ row }">¥{{ row.remainingAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="账龄" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getAgingType(row.agingBucket)" size="small">
              {{ getAgingText(row.agingBucket) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超期" width="80" align="center">
          <template #default="{ row }">
            <span v-if="row.daysOverdue && row.daysOverdue > 0" class="overdue">
              {{ row.daysOverdue }}天
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'UNSETTLED' || row.status === 'PARTIAL'" link type="primary" @click="createCollection(row)">收款核销</el-button>
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

    <!-- 应收账龄分析 -->
    <el-card shadow="never" class="mt-20">
      <template #header>
        <div class="card-header">
          <span>应收账龄分析</span>
          <el-button type="primary" plain size="small" @click="refreshAging">刷新</el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">未到期</div>
            <div class="stat-value">¥{{ agingData.find(d => d.agingBucket === '未到期')?.totalRemaining?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">1-30天</div>
            <div class="stat-value">¥{{ agingData.find(d => d.agingBucket === '1-30天')?.totalRemaining?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">31-60天</div>
            <div class="stat-value">¥{{ agingData.find(d => d.agingBucket === '31-60天')?.totalRemaining?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card">
            <div class="stat-title">90天以上</div>
            <div class="stat-value">¥{{ agingData.find(d => d.agingBucket === '90天以上')?.totalRemaining?.toFixed(2) || '0.00' }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 账龄分析图表 -->
      <div ref="agingChartRef" class="aging-chart"></div>
    </el-card>

    <!-- 收款核销对话框 -->
    <el-dialog v-model="collectionDialogVisible" title="收款核销" width="900px">
      <el-alert title="请选择要核销的应收账款" type="info" :closable="false" class="order-alert" />

      <el-form ref="collectionFormRef" :model="collectionForm" :rules="collectionRules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="收款日期" prop="businessDate">
              <el-date-picker v-model="collectionForm.businessDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收款金额" prop="amount">
              <el-input-number v-model="collectionForm.amount" :min="0" :precision="2" :controls="false" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收款方式" prop="paymentMethod">
              <el-select v-model="collectionForm.paymentMethod" style="width: 100%">
                <el-option label="现金" value="CASH" />
                <el-option label="银行转账" value="BANK" />
                <el-option label="支票" value="CHEQUE" />
                <el-option label="其他" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="核销明细" prop="allocations">
          <el-table :data="collectionForm.allocations" border size="small">
            <el-table-column label="应收单号" width="160">
              <template #default="{ row }">{{ row.receivableDocNo }}</template>
            </el-table-column>
            <el-table-column label="应收金额" width="120" align="right">
              <template #default="{ row }">¥{{ row.receivableAmount?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="已收金额" width="120" align="right">
              <template #default="{ row }">¥{{ row.paidAmount?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="本次核销" width="120">
              <template #default="{ $index }">
                <el-input-number
                  v-model="collectionForm.allocations[$index].allocatedAmount"
                  :min="0"
                  :max="collectionForm.allocations[$index].remainingAmount"
                  :precision="2"
                  :controls="false"
                  size="small"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="余额" width="100" align="right">
              <template #default="{ row }">¥{{ row.remainingAmount?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="collectionForm.allocations.splice($index, 1)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="collectionForm.remark" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreateCollection">创建收款单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { financeApi } from '@/api/finance'
import type { ReceivableListResponse, AgingAnalysisResponse } from '@/api/finance'
import { customerApi } from '@/api/customer'
import type { Customer } from '@/api/customer'

const loading = ref(false)
const agingLoading = ref(false)
const creating = ref(false)
const list = ref<ReceivableListResponse[]>([])
const total = ref(0)
const customers = ref<Customer[]>([])
const agingData = ref<AgingAnalysisResponse[]>([])

const query = reactive({ page: 1, size: 10, keyword: '', status: '' })

const collectionDialogVisible = ref(false)
const collectionFormRef = ref<FormInstance>()
const collectionForm = reactive({
  businessDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  paymentMethod: 'CASH',
  remark: '',
  allocations: [] as any[]
})

const collectionRules: FormRules = {
  businessDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入收款金额', trigger: 'blur' }],
  paymentMethod: [{ required: true, message: '请选择收款方式', trigger: 'change' }]
}

const customerMap = computed(() => new Map(customers.value.map((c) => [c.id!, c.name])))

function customerName(id?: number) {
  return id ? (customerMap.value.get(id) ?? '—') : '—'
}

function getStatusType(status: string) {
  switch (status) {
    case 'UNSETTLED': return 'warning'
    case 'PARTIAL': return 'info'
    case 'SETTLED': return 'success'
    default: return ''
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'UNSETTLED': return '未结算'
    case 'PARTIAL': return '部分结算'
    case 'SETTLED': return '已结算'
    default: return status
  }
}

function getAgingType(bucket: string) {
  switch (bucket) {
    case '未到期': return 'success'
    case '1-30天': return 'warning'
    case '31-60天': return 'danger'
    case '90天以上': return 'danger'
    default: return ''
  }
}

function getAgingText(bucket: string) {
  switch (bucket) {
    case '未到期': return '未到期'
    case '1-30天': return '1-30天'
    case '31-60天': return '31-60天'
    case '61-90天': return '61-90天'
    case '90天以上': return '90天以上'
    default: return bucket || '-'
  }
}

async function load() {
  loading.value = true
  try {
    const data = await financeApi.getReceivables({ ...query, status: query.status || undefined })
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

async function refreshAging() {
  agingLoading.value = true
  try {
    const data = await financeApi.getAgingAnalysis()
    agingData.value = data
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    agingLoading.value = false
  }
}

async function createCollection(row: ReceivableListResponse) {
  // 加载客户待收款明细
  const data = await financeApi.getReceivablesByCustomer(row.customerId)
  const receivables = data.records.filter(r => r.status === 'UNSETTLED' || r.status === 'PARTIAL')
  collectionForm.businessDate = new Date().toISOString().slice(0, 10)
  collectionForm.amount = 0
  collectionForm.remark = ''
  collectionForm.allocations = receivables.map(r => ({
    receivableId: r.id,
    customerId: r.customerId,
    receivableDocNo: r.docNo,
    receivableAmount: r.amount,
    paidAmount: r.paidAmount,
    remainingAmount: r.remainingAmount,
    allocatedAmount: 0
  }))

  collectionDialogVisible.value = true
}

async function handleCreateCollection() {
  const valid = await collectionFormRef.value?.validate().catch(() => false)
  if (!valid) return

  // 计算核销总额
  const totalAllocated = collectionForm.allocations.reduce((sum, item) => sum + Number(item.allocatedAmount || 0), 0)
  const amount = Number(collectionForm.amount || 0)
  if (amount <= 0) {
    ElMessage.warning('收款金额必须大于0')
    return
  }
  if (totalAllocated <= 0) {
    ElMessage.warning('请至少填写一笔核销金额')
    return
  }
  if (totalAllocated > amount) {
    ElMessage.warning('核销总额不能超过收款金额')
    return
  }

  creating.value = true
  try {
    await financeApi.settleReceivables({
      customerId: collectionForm.allocations[0].customerId,
      bizDate: collectionForm.businessDate,
      amount,
      method: collectionForm.paymentMethod,
      remark: collectionForm.remark,
      allocations: collectionForm.allocations.filter(a => Number(a.allocatedAmount || 0) > 0).map(a => ({
        receivableId: a.receivableId,
        amount: Number(a.allocatedAmount)
      }))
    })
    ElMessage.success('收款单创建成功')
    collectionDialogVisible.value = false
    await load()
    await refreshAging()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    creating.value = false
  }
}

function viewDetail(row: ReceivableListResponse) {
  ElMessageBox.alert(
    `应收单号: ${row.docNo}\n` +
    `销售订单: ${row.orderDocNo || '—'}\n` +
    `客户: ${customerName(row.customerId)}\n` +
    `业务日期: ${row.businessDate}\n` +
    `到期日: ${row.dueDate}\n` +
    `应收金额: ¥${row.amount?.toFixed(2)}\n` +
    `已收金额: ¥${row.paidAmount?.toFixed(2)}\n` +
    `余额: ¥${row.remainingAmount?.toFixed(2)}\n` +
    `状态: ${getStatusText(row.status)}`,
    '应收账款详情',
    { confirmButtonText: '确定' }
  )
}

async function loadOptions() {
  try {
    const data = await customerApi.page({ page: 1, size: 200 })
    customers.value = data.records
  } catch {
    // 错误提示由 http 拦截器统一处理
  }
}

onMounted(() => {
  load()
  refreshAging()
  loadOptions()
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mt-20 {
  margin-top: 20px;
}

.stat-card {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  text-align: center;
}

.stat-title {
  color: #666;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

.aging-chart {
  margin-top: 20px;
  height: 300px;
}

.order-alert {
  margin-bottom: 16px;
}

.overdue {
  color: #f56c6c;
  font-weight: bold;
}
</style>
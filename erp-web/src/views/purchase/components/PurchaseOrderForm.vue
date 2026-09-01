<template>
  <el-dialog
    :title="title"
    v-model="dialogVisible"
    width="900px"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <!-- 基本信息 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="采购单号" prop="docNo">
            <el-input v-model="form.docNo" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="业务日期" prop="bizDate">
            <el-date-picker
              v-model="form.bizDate"
              type="date"
              placeholder="请选择业务日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="供应商" prop="supplierId">
            <el-select
              v-model="form.supplierId"
              placeholder="请选择供应商"
              filterable
              @change="handleSupplierChange"
            >
              <el-option
                v-for="supplier in suppliers"
                :key="supplier.id"
                :label="supplier.name"
                :value="supplier.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="采购仓库" prop="warehouseId">
            <el-select
              v-model="form.warehouseId"
              placeholder="请选择仓库"
              filterable
              :disabled="!form.supplierId"
            >
              <el-option
                v-for="warehouse in warehouses"
                :key="warehouse.id"
                :label="warehouse.name"
                :value="warehouse.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="请输入备注信息"
        />
      </el-form-item>

      <!-- 明细表格 -->
      <div class="detail-section">
        <div class="section-header">
          <span>采购明细</span>
          <el-button type="primary" size="small" @click="addDetailItem">
            添加商品
          </el-button>
        </div>

        <el-table :data="form.items" border style="width: 100%">
          <el-table-column type="index" label="序号" width="50" align="center" />
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <el-select
                v-model="row.productId"
                placeholder="请选择商品"
                filterable
                @change="(val) => handleProductChange(val, row)"
                style="width: 100%"
              >
                <el-option
                  v-for="product in products"
                  :key="product.id"
                  :label="product.name"
                  :value="product.id"
                >
                  <span>{{ product.name }}</span>
                  <span style="float: right; color: #8492a6; font-size: 13px">
                    {{ product.spec }}
                  </span>
                </el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="规格" width="120">
            <template #default="{ row }">
              <span>{{ row.spec || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单位" width="80">
            <template #default="{ row }">
              <span>{{ row.unit || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100">
            <template #default="{ row }">
              <el-input-number
                v-model="row.qty"
                :min="0.001"
                :precision="3"
                :step="0.1"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              <el-input-number
                v-model="row.price"
                :min="0.01"
                :precision="4"
                :step="0.1"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120">
            <template #default="{ row }">
              <span>{{ formatAmount(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" width="150">
            <template #default="{ row }">
              <el-input v-model="row.note" placeholder="请输入备注" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{ $index }">
              <el-button
                type="danger"
                size="small"
                @click="removeDetailItem($index)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 合计 -->
      <div class="total-section">
        <span>合计金额：</span>
        <span class="total-amount">¥ {{ formatAmount(form.totalAmount) }}</span>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { purchaseOrderApi, supplierApi, productApi, warehouseApi } from '@/api/purchase'
import { supplierApi as masterSupplierApi } from '@/api/masterdata'
import { productApi as masterProductApi } from '@/api/masterdata'
import { warehouseApi as masterWarehouseApi } from '@/api/masterdata'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  orderId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'success'])

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const title = computed(() => props.orderId ? '编辑采购订单' : '新建采购订单')

const formRef = ref()
const submitting = ref(false)
const suppliers = ref([])
const products = ref([])
const warehouses = ref([])

// 表单数据
const form = ref({
  docNo: '',
  supplierId: null,
  warehouseId: null,
  bizDate: new Date().toISOString().split('T')[0],
  remark: '',
  items: [],
  totalAmount: 0
})

// 表单验证规则
const rules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  bizDate: [{ required: true, message: '请选择业务日期', trigger: 'change' }]
}

// 监听明细变化，计算总金额
watch(() => form.value.items, (items) => {
  const total = items.reduce((sum, item) => {
    const amount = (item.qty || 0) * (item.price || 0)
    return sum + amount
  }, 0)
  form.value.totalAmount = Number(total.toFixed(2))
}, { deep: true })

// 初始化
const init = async () => {
  // 获取基础数据
  await Promise.all([
    getSuppliers(),
    getProducts(),
    getWarehouses()
  ])

  // 如果是编辑模式，加载数据
  if (props.orderId) {
    await loadOrderData()
  } else {
    // 生成单号
    generateDocNo()
  }
}

// 获取供应商列表
const getSuppliers = async () => {
  try {
    const response = await masterSupplierApi.list()
    suppliers.value = response.data || []
  } catch (error) {
    console.error('获取供应商列表失败:', error)
  }
}

// 获取商品列表
const getProducts = async () => {
  try {
    const response = await masterProductApi.list({ status: 'ACTIVE' })
    products.value = response.data || []
  } catch (error) {
    console.error('获取商品列表失败:', error)
  }
}

// 获取仓库列表
const getWarehouses = async () => {
  try {
    const response = await masterWarehouseApi.list()
    warehouses.value = response.data || []
  } catch (error) {
    console.error('获取仓库列表失败:', error)
  }
}

// 加载订单数据
const loadOrderData = async () => {
  try {
    const response = await purchaseOrderApi.detail(props.orderId)
    const data = response.data

    form.value = {
      docNo: data.order.docNo,
      supplierId: data.order.supplierId,
      warehouseId: data.order.warehouseId,
      bizDate: data.order.bizDate,
      remark: data.order.remark,
      items: data.items.map(item => ({
        productId: item.productId,
        qty: item.qty,
        price: item.price,
        amount: item.amount,
        note: item.note,
        spec: products.value.find(p => p.id === item.productId)?.spec || '',
        unit: products.value.find(p => p.id === item.productId)?.unit || ''
      })),
      totalAmount: data.order.totalAmount
    }
  } catch (error) {
    console.error('加载订单数据失败:', error)
    ElMessage.error('加载订单数据失败')
  }
}

// 生成单号
const generateDocNo = async () => {
  try {
    // 调用后端接口生成单号
    const response = await purchaseOrderApi.generateDocNo()
    form.value.docNo = response.data
  } catch (error) {
    console.error('生成单号失败:', error)
  }
}

// 供应商变化
const handleSupplierChange = (supplierId) => {
  // 重置仓库和明细
  form.value.warehouseId = null
  form.value.items = []

  // 根据供应商获取可用仓库（可选逻辑）
}

// 商品变化
const handleProductChange = (productId, item) => {
  const product = products.value.find(p => p.id === productId)
  if (product) {
    item.spec = product.spec || ''
    item.unit = product.unit || ''

    // 设置默认单价（可选）
    if (!item.price && product.price) {
      item.price = product.price
    }
  }
}

// 添加明细
const addDetailItem = () => {
  form.value.items.push({
    productId: null,
    qty: 1,
    price: 0,
    amount: 0,
    note: '',
    spec: '',
    unit: ''
  })
}

// 删除明细
const removeDetailItem = (index) => {
  form.value.items.splice(index, 1)
}

// 格式化金额
const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return Number(amount).toFixed(2)
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    const submitData = {
      supplierId: form.value.supplierId,
      warehouseId: form.value.warehouseId,
      bizDate: form.value.bizDate,
      remark: form.value.remark,
      items: form.value.items.filter(item => item.productId && item.qty > 0)
    }

    if (props.orderId) {
      // 更新
      await purchaseOrderApi.update(props.orderId, submitData)
    } else {
      // 新建
      await purchaseOrderApi.create(submitData)
    }

    ElMessage.success('保存成功')
    emit('success')
    dialogVisible.value = false
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false
  formRef.value?.resetFields()
  form.value = {
    docNo: '',
    supplierId: null,
    warehouseId: null,
    bizDate: new Date().toISOString().split('T')[0],
    remark: '',
    items: [],
    totalAmount: 0
  }
}

// 显示时初始化
watch(dialogVisible, (visible) => {
  if (visible) {
    init()
  }
})
</script>

<style scoped>
.detail-section {
  margin-top: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: bold;
}

.total-section {
  margin-top: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.total-amount {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  margin-left: 8px;
}
</style>
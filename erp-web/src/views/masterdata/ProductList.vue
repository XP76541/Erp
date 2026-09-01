<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="编码 / 名称 / 规格"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
          @clear="load"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增商品</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="spec" label="规格" min-width="120" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="salePrice" label="售价" width="100" align="right" />
        <el-table-column prop="minSalePrice" label="最低限价" width="100" align="right" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isActive === 1"
              @change="(value: boolean | string | number) => handleToggle(row, !!value)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="规格型号" prop="spec">
          <el-input v-model="form.spec" maxlength="100" />
        </el-form-item>
        <el-form-item label="计量单位" prop="unit">
          <el-input v-model="form.unit" maxlength="20" style="width: 140px" />
        </el-form-item>
        <el-form-item label="条码" prop="barcode">
          <el-input v-model="form.barcode" maxlength="50" />
        </el-form-item>
        <el-form-item label="默认进价" prop="purchasePrice">
          <el-input-number v-model="form.purchasePrice" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="默认售价" prop="salePrice">
          <el-input-number v-model="form.salePrice" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="最低限价" prop="minSalePrice">
          <el-input-number v-model="form.minSalePrice" :min="0" :precision="2" :controls="false" />
          <span class="form-tip">0 表示不限制</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { productApi } from '@/api/product'
import type { Product } from '@/api/product'

const loading = ref(false)
const saving = ref(false)
const list = ref<Product[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Product>(defaultForm())

function defaultForm(): Product {
  return {
    name: '',
    spec: '',
    unit: '',
    barcode: '',
    purchasePrice: 0,
    salePrice: 0,
    minSalePrice: 0,
  }
}

const rules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const data = await productApi.page({ ...query })
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

function openCreate() {
  Object.assign(form, defaultForm(), { id: undefined, code: undefined })
  dialogVisible.value = true
}

function openEdit(row: Product) {
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (form.id) {
      await productApi.update(form.id, form)
    } else {
      await productApi.create(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleToggle(row: Product, active: boolean) {
  try {
    await productApi.toggleStatus(row.id!, active)
    ElMessage.success(active ? '已启用' : '已停用')
    await load()
  } catch {
    // 错误提示由 http 拦截器统一处理
  }
}

onMounted(load)
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

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

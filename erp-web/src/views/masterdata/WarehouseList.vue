<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增仓库</el-button>
        <span class="tip">仓库编码创建后不可修改;停用仓库不影响历史单据</span>
        <el-input v-model="keyword" placeholder="编码 / 名称" clearable style="width: 200px" />
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="filteredList" border stripe>
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column prop="type" label="类型" width="100" align="center" />
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
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑仓库' : '新增仓库'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="仓库编码" prop="code">
          <el-input v-model="form.code" maxlength="30" :disabled="!!form.id" />
          <span class="form-tip">创建后不可修改</span>
        </el-form-item>
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="仓库类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="正品仓" value="正品仓" />
            <el-option label="次品仓" value="次品仓" />
            <el-option label="样品仓" value="样品仓" />
          </el-select>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { warehouseApi } from '@/api/warehouse'
import type { Warehouse } from '@/api/warehouse'

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const list = ref<Warehouse[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<Warehouse>(defaultForm())

function defaultForm(): Warehouse {
  return { code: '', name: '', type: '正品仓' }
}

const rules: FormRules = {
  code: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
}

const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(
    (item) => item.code.toLowerCase().includes(kw) || item.name.includes(kw),
  )
})

async function load() {
  loading.value = true
  try {
    list.value = await warehouseApi.listAll()
  } catch {
    // 错误提示由 http 拦截器统一处理
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, defaultForm(), { id: undefined })
  dialogVisible.value = true
}

function openEdit(row: Warehouse) {
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (form.id) {
      await warehouseApi.update(form.id, form)
    } else {
      await warehouseApi.create(form)
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

async function handleToggle(row: Warehouse, active: boolean) {
  try {
    await warehouseApi.toggleStatus(row.id!, active)
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
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tip {
  flex: 1;
  color: #909399;
  font-size: 12px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

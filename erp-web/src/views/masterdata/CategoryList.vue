<template>
  <div class="page">
    <el-card shadow="never">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" plain :icon="Plus" @click="openCreate">新增分类</el-button>
        <span class="tip">仅支持两级;停用上级前需先停用其下子分类</span>
      </div>

      <!-- 树形列表 -->
      <el-table v-loading="loading" :data="treeData" row-key="id" border default-expand-all>
        <el-table-column prop="name" label="分类名称" min-width="240" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
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

    <!-- 弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" style="width: 100%">
            <el-option label="根分类(无上级)" :value="0" />
            <el-option
              v-for="item in rootOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id!"
            />
          </el-select>
          <span class="form-tip">编辑子分类时可在根分类间移动</span>
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="form-tip">数字小的排前面</span>
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
import { categoryApi } from '@/api/category'
import type { ProductCategory } from '@/api/category'

const loading = ref(false)
const saving = ref(false)
const list = ref<ProductCategory[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<ProductCategory>(defaultForm())

function defaultForm(): ProductCategory {
  return { parentId: 0, name: '', sort: 0 }
}

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

const dialogTitle = computed(() => (form.id ? '编辑分类' : '新增分类'))

/** 树形数据:根分类挂子分类 */
const treeData = computed(() => {
  const roots = list.value.filter((item) => item.parentId === 0)
  return roots.map((root) => ({
    ...root,
    children: list.value.filter((item) => item.parentId === root.id),
  }))
})

/** 上级分类可选项:所有根分类(编辑时排除自己) */
const rootOptions = computed(() =>
  list.value.filter((item) => item.parentId === 0 && item.id !== form.id),
)

async function load() {
  loading.value = true
  try {
    list.value = await categoryApi.listAll()
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

function openEdit(row: ProductCategory) {
  Object.assign(form, defaultForm(), row, { children: undefined })
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (form.id) {
      await categoryApi.update(form.id, form)
    } else {
      await categoryApi.create(form)
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

async function handleToggle(row: ProductCategory, active: boolean) {
  try {
    await categoryApi.toggleStatus(row.id!, active)
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
  color: #909399;
  font-size: 12px;
}

.form-tip {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
</style>

<template>
  <div class="page"><el-card shadow="never"><template #header><div class="header"><span>数据库备份</span><el-button type="primary" :loading="creating" @click="create">立即备份</el-button></div></template><el-alert title="备份文件保存在服务端配置目录，仅管理员可创建和下载。" type="info" :closable="false" class="hint"/><el-table v-loading="loading" :data="rows" border stripe><el-table-column prop="fileName" label="文件名" min-width="360"/><el-table-column prop="size" label="大小" width="140"><template #default="{row}">{{ formatSize(row.size) }}</template></el-table-column><el-table-column prop="createdAt" label="创建时间" width="190"/><el-table-column label="操作" width="100"><template #default="{row}"><el-button link type="primary" @click="download(row.fileName)">下载</el-button></template></el-table-column></el-table></el-card></div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '@/api/system'
import type { BackupInfo } from '@/api/system'
const loading = ref(false), creating = ref(false), rows = ref<BackupInfo[]>([])
async function load() { loading.value = true; try { rows.value = await systemApi.backups() } catch {} finally { loading.value = false } }
async function create() { creating.value = true; try { await systemApi.createBackup(); ElMessage.success('备份创建成功'); await load() } catch {} finally { creating.value = false } }
async function download(fileName: string) { const response = await fetch(systemApi.downloadBackup(fileName), { headers: { Authorization: `Bearer ${localStorage.getItem('token') || ''}` } }); if (!response.ok) { ElMessage.error('下载失败'); return }; const blob = await response.blob(); const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = fileName; link.click(); URL.revokeObjectURL(link.href) }
function formatSize(size: number) { if (size < 1024) return `${size} B`; if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`; return `${(size / 1024 / 1024).toFixed(1)} MB` }
onMounted(load)
</script>
<style scoped>.header{display:flex;align-items:center;justify-content:space-between}.hint{margin-bottom:16px}</style>

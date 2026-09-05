<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
onMounted(() => {
  userStore.loadCurrentUser().catch(() => {
    // Keep the token guard and existing menu behavior if the optional role data is unavailable.
  })
})
</script>


<style>
:root {
  --erp-brand: #2f6f8f;
  --erp-brand-dark: #24566f;
  --erp-page: #f3f5f7;
  --erp-surface: #ffffff;
  --erp-text: #24313d;
  --erp-muted: #7b8792;
  --erp-border: #e5e9ed;
  --erp-radius: 8px;
  --erp-shadow: 0 2px 10px rgba(35, 52, 65, 0.05);
  --el-color-primary: var(--erp-brand);
  --el-color-primary-light-3: #6f9caf;
  --el-color-primary-light-5: #9ab8c5;
  --el-color-primary-light-7: #c4d7df;
  --el-color-primary-light-9: #edf4f6;
  --el-border-radius-base: 6px;
  --el-font-size-base: 14px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body,
#app {
  height: 100%;
  min-width: 320px;
  font-family: Inter, 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
  color: var(--erp-text);
  background: var(--erp-page);
}

body {
  font-size: 14px;
  -webkit-font-smoothing: antialiased;
}

button,
input,
textarea,
select {
  font: inherit;
}

.el-card {
  border-color: var(--erp-border);
  border-radius: var(--erp-radius);
  box-shadow: var(--erp-shadow);
}

.el-button {
  font-weight: 500;
}

.el-button--primary {
  --el-button-bg-color: var(--erp-brand);
  --el-button-border-color: var(--erp-brand);
  --el-button-hover-bg-color: var(--erp-brand-dark);
  --el-button-hover-border-color: var(--erp-brand-dark);
}

.el-input__wrapper,
.el-select__wrapper,
.el-textarea__inner {
  box-shadow: 0 0 0 1px #dfe5e9 inset;
}

.el-input__wrapper:hover,
.el-select__wrapper:hover,
.el-textarea__inner:hover {
  box-shadow: 0 0 0 1px #9ab8c5 inset;
}

.el-table {
  --el-table-border-color: var(--erp-border);
  --el-table-header-bg-color: #f7f8f9;
  --el-table-row-hover-bg-color: #f7fafb;
  color: var(--erp-text);
}

.el-table th.el-table__cell {
  color: #596773;
  font-weight: 600;
}

.el-table .el-table__cell {
  padding: 11px 0;
}

.el-pagination {
  --el-pagination-button-bg-color: transparent;
  color: var(--erp-muted);
}

.page {
  min-height: 100%;
}

.toolbar {
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination {
  padding-top: 16px;
}

:focus-visible {
  outline: 2px solid rgba(47, 111, 143, 0.45);
  outline-offset: 2px;
}

@media (max-width: 640px) {
  .el-dialog {
    width: calc(100% - 28px) !important;
  }

  .el-table {
    font-size: 13px;
  }
}
</style>

import http from './http';
import type { PageResult } from './http';

// ==================== 库存调拨相关 ====================

export const inventoryTransferApi = {
  // 分页查询库存调拨单
  getTransferList: (params: {
    keyword?: string;
    status?: string;
    warehouseId?: number;
    page?: number;
    size?: number;
  }) => http.get<PageResult<{
    id: number;
    docNo: string;
    fromWarehouseId: number;
    toWarehouseId: number;
    bizDate: string;
    status: string;
    totalAmount: number;
    fromWarehouseName: string;
    toWarehouseName: string;
    createdAt: string;
  }>>('/inventory/transfers', { params }),

  // 获取库存调拨单详情
  getTransferDetail: (id: number) =>
    http.get<{
      id: number;
      docNo: string;
      fromWarehouseId: number;
      toWarehouseId: number;
      bizDate: string;
      status: string;
      totalAmount: number;
      fromWarehouseName: string;
      toWarehouseName: string;
      items: Array<{
        id: number;
        productId: number;
        fromWarehouseId: number;
        toWarehouseId: number;
        qty: number;
        price: number;
        amount: number;
        productName: string;
        fromWarehouseName: string;
        toWarehouseName: string;
        note: string;
      }>;
      operatorName: string;
      auditAt: string;
      approvedAt: string;
      remark: string;
    }>(`/inventory/transfers/${id}`),

  // 创建库存调拨单
  createTransfer: (data: {
    fromWarehouseId: number;
    toWarehouseId: number;
    bizDate?: string;
    remark?: string;
    items: Array<{
      productId: number;
      qty: number;
      price?: number;
      note?: string;
    }>;
  }) => http.post<number>('/inventory/transfers', data),

  // 审核库存调拨单
  auditTransfer: (id: number, ip: string) =>
    http.put<void>(`/inventory/transfers/${id}/audit`, { ip }),

  // 完成库存调拨
  completeTransfer: (id: number, ip: string) =>
    http.put<void>(`/inventory/transfers/${id}/complete`, { ip }),

  // 取消库存调拨
  cancelTransfer: (id: number, ip: string) =>
    http.put<void>(`/inventory/transfers/${id}/cancel`, { ip }),

  // 根据仓库查询调拨列表
  getTransfersByWarehouse: (warehouseId: number) =>
    http.get<Array<{
      id: number;
      docNo: string;
      fromWarehouseId: number;
      toWarehouseId: number;
      bizDate: string;
      status: string;
      totalAmount: number;
      fromWarehouseName: string;
      toWarehouseName: string;
      createdAt: string;
    }>>(`/inventory/transfers/warehouse/${warehouseId}`),

  // 获取调拨统计
  getTransferStats: () =>
    http.get<{
      draftCount: number;
      auditCount: number;
      completedCount: number;
      totalAmount: number;
    }>('/inventory/transfers/stats'),

  // 获取待处理的调拨单数量
  getDraftTransferCount: () =>
    http.get<number>('/inventory/transfers/stats/draft-count'),

  // 获取已审核的调拨单数量
  getAuditTransferCount: () =>
    http.get<number>('/inventory/transfers/stats/audit-count'),

  // 获取已完成的调拨单数量
  getCompletedTransferCount: () =>
    http.get<number>('/inventory/transfers/stats/completed-count'),
};

// ==================== 库存盘点相关 ====================

export const inventoryCheckApi = {
  // 分页查询库存盘点单
  getCheckList: (params: {
    keyword?: string;
    status?: string;
    warehouseId?: number;
    page?: number;
    size?: number;
  }) => http.get<PageResult<{
    id: number;
    docNo: string;
    warehouseId: number;
    checkDate: string;
    status: string;
    checkType: string;
    totalItems: number;
    totalAmount: number;
    diffItems: number;
    diffAmount: number;
    warehouseName: string;
    createdAt: string;
  }>>('/inventory/checks', { params }),

  // 获取库存盘点单详情
  getCheckDetail: (id: number) =>
    http.get<{
      id: number;
      docNo: string;
      warehouseId: number;
      checkDate: string;
      status: string;
      checkType: string;
      totalItems: number;
      totalAmount: number;
      diffItems: number;
      diffAmount: number;
      warehouseName: string;
      operatorName: string;
      auditAt: string;
      remark: string;
      items: Array<{
        id: number;
        productId: number;
        warehouseId: number;
        systemQty: number;
        actualQty: number;
        diffQty: number;
        price: number;
        amount: number;
        status: string;
        productName: string;
        note: string;
      }>;
    }>(`/inventory/checks/${id}`),

  // 创建库存盘点单
  createCheck: (data: {
    warehouseId: number;
    checkDate?: string;
    checkType?: string;
    remark?: string;
    items?: Array<{
      productId: number;
      price?: number;
      note?: string;
    }>;
  }) => http.post<number>('/inventory/checks', data),

  // 开始盘点
  startCheck: (id: number, ip: string) =>
    http.put<void>(`/inventory/checks/${id}/start-check`, { ip }),

  // 提交盘点结果
  submitCheckResult: (id: number, data: {
    items: Array<{
      productId: number;
      actualQty: number;
      note?: string;
    }>;
  }, ip: string) =>
    http.put<void>(`/inventory/checks/${id}/submit-result`, { ...data, ip }),

  // 审核盘点单
  auditCheck: (id: number, ip: string) =>
    http.put<void>(`/inventory/checks/${id}/audit`, { ip }),

  // 取消盘点单
  cancelCheck: (id: number, ip: string) =>
    http.put<void>(`/inventory/checks/${id}/cancel`, { ip }),

  // 根据仓库查询盘点列表
  getChecksByWarehouse: (warehouseId: number) =>
    http.get<Array<{
      id: number;
      docNo: string;
      warehouseId: number;
      checkDate: string;
      status: string;
      checkType: string;
      totalItems: number;
      totalAmount: number;
      diffItems: number;
      diffAmount: number;
      warehouseName: string;
      createdAt: string;
    }>>(`/inventory/checks/warehouse/${warehouseId}`),

  // 获取盘点统计
  getCheckStats: () =>
    http.get<{
      draftCount: number;
      checkingCount: number;
      auditedCount: number;
      totalAmount: number;
      diffAmount: number;
    }>('/inventory/checks/stats'),

  // 获取待盘点的单据数量
  getDraftCheckCount: () =>
    http.get<number>('/inventory/checks/stats/draft-count'),

  // 获取盘点中的单据数量
  getCheckingCheckCount: () =>
    http.get<number>('/inventory/checks/stats/checking-count'),

  // 获取已盘点的单据数量
  getAuditedCheckCount: () =>
    http.get<number>('/inventory/checks/stats/audited-count'),
};

// ==================== 库存预警相关 ====================

export const inventoryWarningApi = {
  // 分页查询库存预警
  getWarningList: (params: {
    warningType?: string;
    warehouseId?: number;
    productId?: number;
    isActive?: boolean;
    page?: number;
    size?: number;
  }) => http.get<PageResult<{
    id: number;
    warningType: string;
    warehouseId: number;
    productId: number;
    currentQty: number;
    warningValue: number;
    isActive: boolean;
    createdAt: string;
    productName: string;
    warehouseName: string;
  }>>('/inventory/warnings', { params }),

  // 解决预警
  resolveWarning: (id: number) =>
    http.put<void>(`/inventory/warnings/${id}/resolve`),

  // 批量解决预警
  batchResolveWarnings: (ids: number[]) =>
    http.put<void>('/inventory/warnings/batch-resolve', { ids }),

  // 获取激活的预警
  getActiveWarnings: (warehouseId?: number) =>
    http.get<Array<{
      id: number;
      warningType: string;
      warehouseId: number;
      productId: number;
      currentQty: number;
      warningValue: number;
      createdAt: string;
      productName: string;
      warehouseName: string;
    }>>(`/inventory/warnings/active${warehouseId ? `?warehouseId=${warehouseId}` : ''}`),

  // 根据预警类型查询预警
  getWarningsByType: (warningType: string, warehouseId?: number) =>
    http.get<Array<{
      id: number;
      warningType: string;
      warehouseId: number;
      productId: number;
      currentQty: number;
      warningValue: number;
      createdAt: string;
      productName: string;
      warehouseName: string;
    }>>(`/inventory/warnings/type/${warningType}${warehouseId ? `?warehouseId=${warehouseId}` : ''}`),

  // 获取预警统计
  getWarningStats: () =>
    http.get<{
      stockOutCount: number;
      stockOverCount: number;
      expiringCount: number;
      spoiledCount: number;
      totalAmount: number;
    }>('/inventory/warnings/stats'),

  // 获取逾期未解决的预警
  getOverdueWarnings: () =>
    http.get<Array<{
      id: number;
      warningType: string;
      warehouseId: number;
      productId: number;
      currentQty: number;
      warningValue: number;
      createdAt: string;
      productName: string;
      warehouseName: string;
    }>>('/inventory/warnings/overdue'),

  // 获取预警配置
  getWarningConfigs: (productId?: number, warehouseId?: number) =>
    http.get<Array<{
      id: number;
      productId: number;
      warehouseId: number;
      stockOutLimit: number;
      stockOverLimit: number;
      warningLevel: string;
      isActive: boolean;
    }>>(`/inventory/warning-configs${productId || warehouseId ? `?productId=${productId}&warehouseId=${warehouseId}` : ''}`),

  // 更新预警配置
  updateWarningConfig: (id: number, data: {
    stockOutLimit?: number;
    stockOverLimit?: number;
    warningLevel?: string;
    isActive?: boolean;
  }) =>
    http.put<void>(`/inventory/warning-configs/${id}`, data),

  // 创建预警配置
  createWarningConfig: (data: {
    productId: number;
    warehouseId: number;
    stockOutLimit?: number;
    stockOverLimit?: number;
    warningLevel?: string;
    isActive?: boolean;
  }) =>
    http.post<number>('/inventory/warning-configs', data),

  // 启用/禁用预警配置
  toggleWarningConfig: (id: number, isActive: boolean) =>
    http.put<void>(`/inventory/warning-configs/${id}/toggle`, { isActive }),

  // 批量启用/禁用预警配置
  batchToggleWarningConfig: (ids: number[], isActive: boolean) =>
    http.put<void>('/inventory/warning-configs/batch-toggle', { ids, isActive }),
};
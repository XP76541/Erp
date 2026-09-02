import http, { type PageResult } from './http';

// 采购订单相关接口
export interface PurchaseInboundItemInput {
  productId: number;
  warehouseId?: number;
  qty: number;
  price: number;
  note?: string;
}

export interface PurchaseInboundCreateRequest {
  supplierId: number;
  warehouseId: number;
  bizDate?: string;
  docType?: string;
  docId?: number;
  remark?: string;
  items: PurchaseInboundItemInput[];
}

export interface PurchaseInboundListResponse {
  id: number;
  docNo: string;
  supplierId: number;
  warehouseId: number;
  bizDate: string;
  status: string;
  docType?: string;
  docId?: number;
  auditAt?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PurchaseInboundDetailResponse {
  doc: PurchaseInboundListResponse;
  items: PurchaseInboundItemInput[];
}

export interface PurchaseOrderItemInput {
  productId: number;
  qty: number;
  price: number;
  note?: string;
}

export interface PurchaseOrderCreateRequest {
  supplierId: number;
  warehouseId: number;
  bizDate?: string;
  items: PurchaseOrderItemInput[];
  remark?: string;
}

export interface PurchaseOrderUpdateQtyRequest {
  items: Array<{
    itemId: number;
    qty: number;
  }>;
}

export interface PurchaseOrderAuditRequest {
  remark?: string;
  ip: string;
}

export interface PurchaseOrderRejectRequest {
  remark?: string;
  ip: string;
}

export interface PurchaseOrderListResponse {
  id: number;
  docNo: string;
  supplierId: number;
  supplierName: string;
  warehouseId: number;
  warehouseName: string;
  bizDate: string;
  status: string;
  totalAmount: number;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PurchaseOrderDetailResponse {
  order: {
    id: number;
    docNo: string;
    supplierId: number;
    supplierName: string;
    warehouseId: number;
    warehouseName: string;
    bizDate: string;
    status: string;
    totalAmount: number;
    remark?: string;
    createdBy?: number;
    createdByName?: string;
    auditBy?: number;
    auditByName?: string;
    auditAt?: string;
    rejectBy?: number;
    rejectByName?: string;
    rejectAt?: string;
    createdAt: string;
    updatedAt: string;
  };
  items: Array<{
    id: number;
    orderId: number;
    lineNo: number;
    productId: number;
    qty: number;
    price: number;
    amount: number;
    note?: string;
    receivedQty: number;
    createdAt: string;
    updatedAt: string;
  }>;
}

// 采购订单状态枚举
export const PurchaseOrderStatus = {
  DRAFT: 'DRAFT',
  AUDITED: 'AUDITED',
  VOID: 'VOID'
} as const;

export type PurchaseOrderStatusType = typeof PurchaseOrderStatus[keyof typeof PurchaseOrderStatus];

// 采购入库单API
export const purchaseInboundApi = {
  list: (params: { keyword?: string; status?: string; page?: number; size?: number }) =>
    http.get<PageResult<PurchaseInboundListResponse>>('/purchase-inbounds', { params }),
  detail: (id: number) => http.get<PurchaseInboundDetailResponse>(`/purchase-inbounds/${id}`),
  create: (data: PurchaseInboundCreateRequest) => http.post<number>('/purchase-inbounds', data),
  audit: (id: number) => http.put<void>(`/purchase-inbounds/${id}/audit`),
};

// 采购订单API
export const purchaseOrderApi = {
  // 分页查询
  list: (params: {
    keyword?: string;
    status?: string;
    supplierId?: number;
    page?: number;
    size?: number;
  }) => http.get<PageResult<PurchaseOrderListResponse>>('/purchase/orders', { params }),

  // 获取详情
  detail: (id: number) =>
    http.get<PurchaseOrderDetailResponse>(`/purchase/orders/${id}`),

  // 创建
  create: (data: PurchaseOrderCreateRequest) =>
    http.post<number>('/purchase/orders', data),

  // 更新
  update: (id: number, data: PurchaseOrderCreateRequest) =>
    http.put<void>(`/purchase/orders/${id}`, data),

  // 审核
  audit: (id: number, data: PurchaseOrderAuditRequest) =>
    http.put<void>(`/purchase/orders/${id}/audit`, data),

  // 驳回
  reject: (id: number, data: PurchaseOrderRejectRequest) =>
    http.put<void>(`/purchase/orders/${id}/reject`, data),

  // 更新数量（仅限草稿状态）
  updateQty: (id: number, data: PurchaseOrderUpdateQtyRequest) =>
    http.put<void>(`/purchase/orders/${id}/qty`, data),

  // 获取供应商的采购订单
  getBySupplier: (supplierId: number) =>
    http.get<PurchaseOrderListResponse[]>(`/purchase/orders/supplier/${supplierId}`),

  // 统计
  getDraftCount: () =>
    http.get<number>('/purchase/orders/stats/draft-count'),

  getUnreceivedCount: () =>
    http.get<number>('/purchase/orders/stats/unreceived-count'),

  // 获取采购订单明细的入库情况
  getItemStats: (orderId: number) =>
    http.get<Array<{
      id: number;
      lineNo: number;
      orderedQty: number;
      receivedQty: number;
      remainingQty: number;
      productId: number;
      productName: string;
    }>>(`/purchase/orders/${orderId}/item-stats`),

  // 批量更新已入库数量
  updateReceivedQty: (orderId: number, items: Array<{ itemId: number; qty: number }>) =>
    http.put<void>(`/purchase/orders/${orderId}/received-qty`, { items }),
};


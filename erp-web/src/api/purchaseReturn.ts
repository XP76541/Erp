import http, { type PageResult } from './http'

export interface PurchaseReturnListResponse {
  id: number
  docNo: string
  supplierId: number
  warehouseId: number
  bizDate: string
  status: 'DRAFT' | 'AUDITED' | string
  reason?: string
  totalAmount: number
  auditBy?: number
  auditAt?: string
  createdAt?: string
}

export interface PurchaseReturnItem {
  id: number
  returnId: number
  lineNo: number
  inboundItemId: number
  productId: number
  warehouseId: number
  qty: number
  unitCost: number
  amount: number
  note?: string
}

export interface PurchaseReturnDetailResponse {
  doc: PurchaseReturnListResponse & { reason: string }
  items: PurchaseReturnItem[]
}

export interface PurchaseReturnCreateRequest {
  supplierId: number
  warehouseId: number
  bizDate?: string
  reason?: string
  items: Array<{ inboundItemId: number; qty: number; note?: string }>
}

export const purchaseReturnApi = {
  list: (params: { page?: number; size?: number; keyword?: string; status?: string; supplierId?: number }) =>
    http.get<PageResult<PurchaseReturnListResponse>>('/purchase-returns', { params }),
  detail: (id: number) => http.get<PurchaseReturnDetailResponse>(`/purchase-returns/${id}`),
  create: (data: PurchaseReturnCreateRequest) => http.post<number>('/purchase-returns', data),
  audit: (id: number) => http.put<void>(`/purchase-returns/${id}/audit`),
}

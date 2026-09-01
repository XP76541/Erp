import http from './http'
import type { PageResult } from './http'

export interface PurchaseInboundItem {
  id?: number
  lineNo?: number
  productId: number
  warehouseId?: number
  qty: number
  price: number
  amount?: number
  note?: string
}

export interface PurchaseInbound {
  id?: number
  docNo?: string
  supplierId: number
  warehouseId: number
  bizDate?: string
  status?: string
  auditBy?: number
  auditAt?: string
  remark?: string
  createdAt?: string
}

export interface PurchaseInboundDetail {
  doc: PurchaseInbound
  items: PurchaseInboundItem[]
}

export const purchaseApi = {
  page(params: { page: number; size: number; keyword?: string; status?: string }) {
    return http.get<unknown, PageResult<PurchaseInbound>>('/purchase-inbounds', { params })
  },
  detail(id: number) {
    return http.get<unknown, PurchaseInboundDetail>(`/purchase-inbounds/${id}`)
  },
  create(data: { supplierId: number; warehouseId: number; bizDate?: string; remark?: string; items: PurchaseInboundItem[] }) {
    return http.post<unknown, number>('/purchase-inbounds', data)
  },
  audit(id: number) {
    return http.put(`/purchase-inbounds/${id}/audit`)
  },
}

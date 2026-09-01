import http from './http'
import type { PageResult } from './http'

export interface Supplier {
  id?: number
  code?: string
  name: string
  contact?: string
  phone?: string
  paymentTermDays?: number
  settleType?: string
  bankName?: string
  bankAccount?: string
  isActive?: number
  createdAt?: string
}

export interface SupplierQuery {
  page: number
  size: number
  keyword?: string
}

export const supplierApi = {
  page(params: SupplierQuery) {
    return http.get<unknown, PageResult<Supplier>>('/suppliers', { params })
  },
  create(data: Supplier) {
    return http.post<unknown, number>('/suppliers', data)
  },
  update(id: number, data: Supplier) {
    return http.put(`/suppliers/${id}`, data)
  },
  toggleStatus(id: number, active: boolean) {
    return http.put(`/suppliers/${id}/status`, { active })
  },
}

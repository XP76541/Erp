import http from './http'
import type { PageResult } from './http'

export interface PaymentAllocation {
  id?: number
  paymentId?: number
  payableId: number
  amount: number
  payableDocNo?: string
  outstandingAmount?: number
}

export interface Payment {
  id?: number
  docNo?: string
  supplierId: number
  bizDate?: string
  amount: number
  method: string
  bankAccount?: string
  status?: string
  auditBy?: number
  auditAt?: string
  remark?: string
  createdAt?: string
}

export interface PaymentDetail {
  doc: Payment
  allocations: PaymentAllocation[]
}

export const paymentApi = {
  page(params: { page: number; size: number; keyword?: string }) {
    return http.get<unknown, PageResult<Payment>>('/payments', { params })
  },
  detail(id: number) {
    return http.get<unknown, PaymentDetail>(`/payments/${id}`)
  },
  create(data: {
    supplierId: number
    bizDate: string
    amount: number
    method: string
    bankAccount?: string
    remark?: string
    allocations: PaymentAllocation[]
  }) {
    return http.post<unknown, number>('/payments', data)
  },
  audit(id: number) {
    return http.put(`/payments/${id}/audit`)
  },
}
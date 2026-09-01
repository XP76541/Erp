import http from './http'
import type { PageResult } from './http'

export interface Payable {
  id: number
  supplierId: number
  docType: string
  docId: number
  docNo: string
  bizDate: string
  dueDate: string
  amount: number
  paidAmount: number
  status: string
}

export const payableApi = {
  page(params: { page: number; size: number; supplierId: number; status?: string }) {
    return http.get<unknown, PageResult<Payable>>('/payables', { params })
  },
}
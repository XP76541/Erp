import http, { type PageResult } from './http'

export interface PayableListResponse {
  id: number
  supplierId: number
  supplierName: string
  docType: string
  docId: number
  docNo: string
  bizDate: string
  dueDate: string
  amount: number
  paidAmount: number
  remainingAmount: number
  status: string
  daysOverdue: number
  agingBucket: string
  createdAt?: string
}

export interface PayableAgingResponse {
  bucket: string
  amount: number
  paidAmount: number
  remainingAmount: number
  count: number
}

export const payableApi = {
  list: (params: {
    supplierId?: number
    status?: string
    startDate?: string
    endDate?: string
    dueStartDate?: string
    dueEndDate?: string
    page?: number
    size?: number
  }) => http.get<PageResult<PayableListResponse>>('/finance/payables', { params }),
  detail: (id: number) => http.get<PayableListResponse>(`/finance/payables/${id}`),
  aging: (supplierId?: number) => http.get<PayableAgingResponse[]>('/finance/payables/aging', {
    params: supplierId ? { supplierId } : undefined,
  }),
}

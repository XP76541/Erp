import request from '@/utils/request'
import type { PageResult } from './http'

export interface Payment {
  id: number
  docNo: string
  supplierId: number
  supplierName: string
  bizDate: string
  amount: number
  allocatedAmount: number
  status: 'DRAFT' | 'AUDITED' | 'VOID'
  method?: string
  bankAccount?: string
  remark?: string
  createdAt?: string
}

export interface PaymentCreateRequest {
  supplierId: number
  bizDate?: string
  amount: number
  method?: string
  bankAccount?: string
  remark?: string
  allocations: Array<{ payableId: number; amount: number }>
}

export interface PaymentAuditRequest {
  remark?: string
  ip?: string
}

export interface PaymentListResponse extends Payment {}

export const financeApi = {
  getPayments: (params: { page?: number; size?: number; supplierId?: number; status?: string; startDate?: string; endDate?: string }) =>
    request.get<PageResult<PaymentListResponse>>('/finance/payments', { params }),
  createPayment: (data: PaymentCreateRequest) => request.post<number>('/finance/payments', data),
  auditPayment: (id: number, data: PaymentAuditRequest) => request.put<void>(`/finance/payments/${id}/audit`, data),
  // 客户应收旧页面兼容接口，供应商付款不复用这些字段
  getReceivables: (params: Record<string, unknown>) => request.get('/finance/receivables', { params }),
  getAgingAnalysis: () => request.get('/finance/receivables/aging-analysis'),
  getOverdueReceivables: () => request.get('/finance/receivables/overdue'),
  getReceivableStats: () => request.get('/finance/receivables/statistics'),
  getReceivablesByCustomer: (customerId: number) => request.get(`/finance/payments/receivables/${customerId}`),
}

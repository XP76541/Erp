import request from '@/utils/request'
import type { PageResult } from './http'

export interface AgingAnalysisResponse {
  agingBucket: string
  totalAmount: number
  totalPaid: number
  totalRemaining: number
}

export interface ReceivableListResponse {
  id: number
  docNo: string
  orderDocNo?: string
  customerId: number
  customerName?: string
  businessDate: string
  dueDate: string
  amount: number
  paidAmount: number
  remainingAmount: number
  status: string
  daysOverdue?: number
  agingBucket?: string
  createdAt?: string
}

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
  settleReceivables: (data: { customerId: number; bizDate?: string; amount: number; method?: string; bankAccount?: string; remark?: string; allocations: Array<{ receivableId: number; amount: number }>; idempotencyKey?: string }) =>
    request.post<unknown>('/finance/receivables/receipts', data),
  getReceivables: (params: { page?: number; size?: number; customerId?: number; status?: string; startDate?: string; endDate?: string }) =>
    request.get<PageResult<ReceivableListResponse>>('/finance/receivables', { params }),
  getAgingAnalysis: (cutoffDate?: string) => request.get<AgingAnalysisResponse[]>('/finance/receivables/aging-analysis', { params: { cutoffDate } }),
  getOverdueReceivables: (cutoffDate?: string) => request.get<ReceivableListResponse[]>('/finance/receivables/overdue', { params: { cutoffDate } }),
  getReceivableStats: () => request.get('/finance/receivables/statistics'),
  getReceivablesByCustomer: (customerId: number) => request.get<{ records: ReceivableListResponse[]; total: number }>('/finance/receivables', { params: { customerId, page: 1, size: 500 } }),
}

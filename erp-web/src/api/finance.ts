import request from '@/utils/request'

// 收款单相关接口
export interface Payment {
  id: number
  docNo: string
  customerId: number
  customerName: string
  businessDate: string
  amount: number
  allocatedAmount: number
  status: 'DRAFT' | 'AUDITED' | 'VOID'
  paymentMethod?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface PaymentCreateRequest {
  customerId: number
  businessDate?: string
  amount: number
  paymentMethod?: string
  remark?: string
  allocations?: Array<{
    receivableId: number
    allocatedAmount: number
    receivableDocNo?: string
    receivableAmount?: number
    remainingAmount?: number
  }>
}

export interface PaymentAuditRequest {
  action: 'audit' | 'reject'
  remark?: string
}

export interface PaymentListResponse {
  id: number
  docNo: string
  customerId: number
  customerName: string
  businessDate: string
  amount: number
  allocatedAmount: number
  status: string
  paymentMethod?: string
  createdAt?: string
}

export interface ReceivableListResponse {
  id: number
  docNo: string
  orderDocNo: string
  customerId: number
  customerName: string
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

export interface ReceivableStatisticsResponse {
  customerId: number
  customerName: string
  totalAmount: number
  totalPaid: number
  totalRemaining: number
  unsettledAmount: number
  partialAmount: number
  settledAmount: number
}

export interface AgingAnalysisResponse {
  agingBucket: string
  totalAmount: number
  totalPaid: number
  totalRemaining: number
}

// API 请求方法
export const financeApi = {
  // 收款单
  getPayments: (params: {
    page?: number
    size?: number
    customerId?: number
    status?: string
    startDate?: string
    endDate?: string
  }) => {
    return request.get('/finance/payments', { params })
  },

  createPayment: (data: PaymentCreateRequest) => {
    return request.post('/finance/payments', data)
  },

  auditPayment: (id: number, data: PaymentAuditRequest) => {
    return request.put(`/finance/payments/${id}/audit`, data)
  },

  getReceivablesByCustomer: (customerId: number) => {
    return request.get(`/finance/payments/receivables/${customerId}`)
  },

  // 应收账款
  getReceivables: (params: {
    page?: number
    size?: number
    customerId?: number
    status?: string
    startDate?: string
    endDate?: string
  }) => {
    return request.get('/finance/receivables', { params })
  },

  getReceivableStats: () => {
    return request.get('/finance/receivables/statistics')
  },

  getAgingAnalysis: () => {
    return request.get('/finance/receivables/aging-analysis')
  },

  getOverdueReceivables: () => {
    return request.get('/finance/receivables/overdue')
  },

  // 统计
  getPaymentStats: () => {
    return request.get('/finance/payments/statistics')
  }
}
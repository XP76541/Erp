import request from '@/utils/request'

// 销售订单相关接口
export interface SalesOrderItem {
  id?: number
  orderId: number
  lineNo: number
  productId: number
  qty: number
  shippedQty: number
  price: number
  amount: number
  note?: string
}

export interface SalesOrder {
  id: number
  docNo: string
  customerId: number
  salespersonId: number
  status: 'DRAFT' | 'AUDITED' | 'VOID'
  shipStatus: 'UN_SHIPPED' | 'PART_SHIPPED' | 'SHIPPED'
  totalAmount: number
  bizDate: string
  auditBy?: number
  auditAt?: string
  rejectBy?: number
  rejectAt?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface SalesOrderCreateRequest {
  customerId: number
  bizDate: string
  salespersonId: number
  remark?: string
  items: Array<{
    productId: number
    qty: number
    price: number
    note?: string
  }>
}

export interface SalesOrderAuditRequest {
  action: 'audit' | 'reject'
  remark?: string
  forceConfirm?: boolean
}

export interface SalesOrderListResponse {
  id: number
  docNo: string
  customerId: number
  customerName: string
  salespersonId: number
  salespersonName: string
  status: string
  shipStatus: string
  totalAmount: number
  bizDate: string
  createdAt?: string
  updatedAt?: string
}

// 销售出库单相关接口
export interface SalesOutboundItem {
  id?: number
  outboundId: number
  orderItemId: number
  lineNo: number
  productId: number
  qty: number
  price: number
  amount: number
  remark?: string
  createdAt?: string
  createdBy?: number
}

export interface SalesOutbound {
  id: number
  docNo: string
  orderId: number
  customerId: number
  warehouseId: number
  status: 'DRAFT' | 'AUDITED' | 'VOID'
  totalAmount: number
  bizDate: string
  auditBy?: number
  auditAt?: string
  rejectBy?: number
  rejectAt?: string
  remark?: string
  createdAt?: string
  updatedAt?: string
}

export interface SalesOutboundCreateRequest {
  orderId: number
  warehouseId: number
  bizDate?: string
  remark?: string
  items: Array<{
    orderItemId: number
    qty: number
    remark?: string
  }>
}

export interface SalesOutboundAuditRequest {
  action: 'audit' | 'reject'
  remark?: string
}

export interface SalesOutboundListResponse {
  id: number
  docNo: string
  orderId: number
  orderDocNo: string
  customerId: number
  customerName: string
  warehouseId: number
  warehouseName: string
  status: string
  totalAmount: number
  bizDate: string
  createdAt?: string
  updatedAt?: string
}

export interface SalesOutboundCreateFromOrderResponse {
  outboundId: number
  outboundDocNo: string
  items: SalesOutboundItem[]
}

// API 请求方法
export const salesApi = {
  getCustomerCreditStatus: (customerId: number, orderAmount?: number) => {
    return request.get(`/sales/customers/${customerId}/credit-status`, {
      params: { orderAmount }
    })
  },

  // 销售订单
  getOrders: (params: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    customerId?: string
  }) => {
    return request.get('/sales/orders', { params })
  },

  getOrderDetail: (id: number) => {
    return request.get(`/sales/orders/${id}`)
  },

  createOrder: (data: SalesOrderCreateRequest) => {
    return request.post('/sales/orders', data)
  },

  auditOrder: (id: number, data: SalesOrderAuditRequest) => {
    return request.put(`/sales/orders/${id}/audit`, data)
  },

  getOrdersByCustomer: (customerId: number, page?: number, size?: number) => {
    return request.get(`/sales/orders/customer/${customerId}`, {
      params: { page, size }
    })
  },

  getOrdersBySalesperson: (salespersonId: number) => {
    return request.get(`/sales/orders/salesperson/${salespersonId}`)
  },

  getOrdersByDateRange: (startDate: string, endDate: string) => {
    return request.get('/sales/orders/date-range', {
      params: { startDate, endDate }
    })
  },

  // 销售出库单
  getOutbounds: (params: {
    page?: number
    size?: number
    keyword?: string
    status?: string
    customerId?: string
  }) => {
    return request.get('/sales/outbounds', { params })
  },

  getOutboundDetail: (id: number) => {
    return request.get(`/sales/outbounds/${id}`)
  },

  createOutboundFromOrder: (orderId: number, data: SalesOutboundCreateRequest) => {
    return request.post('/sales/outbounds/from-order', data, {
      params: { orderId }
    })
  },

  auditOutbound: (id: number, data: SalesOutboundAuditRequest) => {
    return request.put(`/sales/outbounds/${id}/audit`, data)
  },

  getOutboundsByCustomer: (customerId: number) => {
    return request.get(`/sales/outbounds/customer/${customerId}`)
  },

  getOutboundsByWarehouse: (warehouseId: number) => {
    return request.get(`/sales/outbounds/warehouse/${warehouseId}`)
  },

  // 统计
  getOrderStats: () => {
    return request.get('/sales/stats/orders')
  },

  getOutboundStats: () => {
    return request.get('/sales/stats/outbounds')
  }
}
import http from './http';

// 销售日报表
export interface SalesDailyReportRequest {
  startDate?: string;
  endDate?: string;
  customerId?: number;
  salespersonId?: number;
}

export interface SalesDailyReportItem {
  docNo: string;
  businessDate: string;
  customerName: string;
  salespersonName: string;
  amount: number;
  shippedAmount: number;
  status: string;
}

export interface SalesDailyReportResponse {
  reportDate: string;
  totalOrders: number;
  totalAmount: number;
  shippedAmount: number;
  orders: SalesDailyReportItem[];
}

// 进销存汇总表
export interface InventorySummaryRequest {
  date?: string;
  warehouseId?: number;
  productId?: number;
}

export interface InventorySummaryItem {
  productId: number;
  productName: string;
  productSpec: string;
  warehouseId: number;
  warehouseName: string;
  quantity: number;
  unitCost: number;
  totalValue: number;
}

export interface InventorySummaryResponse {
  reportDate: string;
  totalProducts: number;
  totalValue: number;
  products: InventorySummaryItem[];
}

// 财务汇总表
export interface FinanceSummaryRequest {
  startDate?: string;
  endDate?: string;
}

export interface FinanceSummaryResponse {
  reportDate: string;
  totalSales: number;
  totalPurchases: number;
  totalReceivables: number;
  totalPayables: number;
  totalInventory: number;
  netProfit: number;
}

// 销售日报表API
export const salesDailyReportApi = {
  get: (params: SalesDailyReportRequest) =>
    http.post<SalesDailyReportResponse[]>('/finance/reports/sales-daily', params),
  export: (params: SalesDailyReportRequest) =>
    http.get<Blob>('/finance/reports/sales-daily/export', { params, responseType: 'blob' }),
};

// 进销存汇总表API
export const inventorySummaryApi = {
  get: (params: InventorySummaryRequest) =>
    http.post<InventorySummaryResponse>('/finance/reports/inventory-summary', params),
  export: (params: InventorySummaryRequest) =>
    http.get<Blob>('/finance/reports/inventory-summary/export', { params, responseType: 'blob' }),
};

// 财务汇总表API
export const financeSummaryApi = {
  get: (params: FinanceSummaryRequest) =>
    http.post<FinanceSummaryResponse>('/finance/reports/finance-summary', params),
  export: (params: FinanceSummaryRequest) =>
    http.get<Blob>('/finance/reports/finance-summary/export', { params, responseType: 'blob' }),
};

// 通用响应类型
interface ReportResponse<T> {
  code: number;
  message: string;
  data: T;
}
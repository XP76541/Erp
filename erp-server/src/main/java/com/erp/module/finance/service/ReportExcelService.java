package com.erp.module.finance.service;

import com.erp.module.finance.dto.ReportDtos;
import com.erp.module.finance.dto.ReceivableDtos;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ReportExcelService {
    public byte[] salesDaily(List<ReportDtos.SalesDailyReportResponse> reports) {
        return workbook("销售日报", wb -> {
            Sheet summary = wb.createSheet("日报汇总");
            Row summaryHeader = summary.createRow(0);
            String[] summaryColumns = {"日期", "单据数", "销售额", "已发货金额"};
            for (int i = 0; i < summaryColumns.length; i++) summaryHeader.createCell(i).setCellValue(summaryColumns[i]);
            int summaryRow = 1;
            for (var report : reports) {
                Row row = summary.createRow(summaryRow++);
                row.createCell(0).setCellValue(String.valueOf(report.getReportDate()));
                row.createCell(1).setCellValue(report.getTotalOrders() == null ? 0 : report.getTotalOrders());
                number(row, 2, report.getTotalAmount()); number(row, 3, report.getShippedAmount());
            }
            Sheet sheet = wb.createSheet("单据明细");
            Row header = sheet.createRow(0);
            String[] columns = {"日期", "单号", "客户", "状态", "金额", "已发货金额"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowNo = 1;
            for (var report : reports) for (var order : report.getOrders()) {
                Row row = sheet.createRow(rowNo++);
                row.createCell(0).setCellValue(String.valueOf(order.getBusinessDate()));
                row.createCell(1).setCellValue(order.getDocNo()); row.createCell(2).setCellValue(order.getCustomerName());
                row.createCell(3).setCellValue(order.getStatus()); number(row, 4, order.getAmount());
                number(row, 5, order.getShippedAmount());
            }
        });
    }

    public byte[] inventorySummary(ReportDtos.InventorySummaryResponse report) {
        return workbook("进销存汇总", wb -> {
            Sheet sheet = wb.createSheet("进销存汇总"); Row header = sheet.createRow(0);
            String[] columns = {"商品", "规格", "仓库", "数量", "单位成本", "库存金额"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowNo = 1;
            for (var item : report.getProducts()) { Row row = sheet.createRow(rowNo++);
                row.createCell(0).setCellValue(item.getProductName()); row.createCell(1).setCellValue(item.getProductSpec());
                row.createCell(2).setCellValue(item.getWarehouseName()); number(row, 3, item.getQuantity());
                number(row, 4, item.getUnitCost()); number(row, 5, item.getTotalValue()); }
        });
    }

    public byte[] statement(ReceivableDtos.StatementResponse report) {
        return workbook("客户对账单", wb -> {
            Sheet sheet = wb.createSheet("客户对账单");
            Row summary = sheet.createRow(0);
            summary.createCell(0).setCellValue("客户");
            summary.createCell(1).setCellValue(report.getCustomerName());
            summary.createCell(2).setCellValue("截止日期");
            summary.createCell(3).setCellValue(String.valueOf(report.getStatementDate()));
            Row totals = sheet.createRow(1);
            totals.createCell(0).setCellValue("期初余额"); number(totals, 1, report.getOpeningBalance());
            totals.createCell(2).setCellValue("期末余额"); number(totals, 3, report.getClosingBalance());
            Row header = sheet.createRow(3);
            String[] columns = {"日期", "单号", "类型", "金额", "已收/已付", "余额", "状态", "备注"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowNo = 4;
            if (report.getDetails() != null) for (var detail : report.getDetails()) {
                Row row = sheet.createRow(rowNo++);
                row.createCell(0).setCellValue(String.valueOf(detail.getDate()));
                row.createCell(1).setCellValue(detail.getDocNo());
                row.createCell(2).setCellValue(detail.getDocType());
                number(row, 3, detail.getAmount()); number(row, 4, detail.getPaid());
                number(row, 5, detail.getRemaining()); row.createCell(6).setCellValue(detail.getStatus());
                row.createCell(7).setCellValue(detail.getRemark() == null ? "" : detail.getRemark());
            }
        });
    }

    public byte[] financeSummary(ReportDtos.FinanceSummaryResponse report) {
        return workbook("财务汇总", wb -> {
            Sheet sheet = wb.createSheet("财务汇总"); Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("项目"); header.createCell(1).setCellValue("金额");
            String[] values = {"销售额", "采购额", "应收余额", "应付余额", "库存金额", "净利润"};
            BigDecimal[] amounts = {report.getTotalSales(), report.getTotalPurchases(), report.getTotalReceivables(),
                    report.getTotalPayables(), report.getTotalInventory(), report.getNetProfit()};
            for (int i = 0; i < values.length; i++) {
                Row row = sheet.createRow(i + 1); row.createCell(0).setCellValue(values[i]);
                if (amounts[i] != null) number(row, 1, amounts[i]);
                else row.createCell(1).setCellValue("不可用");
            }
        });
    }

    private byte[] workbook(String name, WorkbookWriter writer) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writer.write(workbook); workbook.write(out); return out.toByteArray();
        } catch (IOException e) { throw new IllegalStateException("生成Excel失败", e); }
    }
    private void number(Row row, int index, BigDecimal value) { if (value != null) row.createCell(index).setCellValue(value.doubleValue()); }
    @FunctionalInterface private interface WorkbookWriter { void write(Workbook workbook); }
}

package com.erp.module.finance.service;

import com.erp.module.finance.dto.ReportDtos;
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
            Sheet sheet = wb.createSheet("销售日报");
            Row header = sheet.createRow(0);
            String[] columns = {"日期", "单据数", "销售额", "已发货金额", "单号", "客户", "状态", "金额"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int rowNo = 1;
            for (var report : reports) for (var order : report.getOrders()) {
                Row row = sheet.createRow(rowNo++);
                row.createCell(0).setCellValue(String.valueOf(report.getReportDate()));
                row.createCell(1).setCellValue(report.getTotalOrders() == null ? 0 : report.getTotalOrders());
                number(row, 2, report.getTotalAmount()); number(row, 3, report.getShippedAmount());
                row.createCell(4).setCellValue(order.getDocNo()); row.createCell(5).setCellValue(order.getCustomerName());
                row.createCell(6).setCellValue(order.getStatus()); number(row, 7, order.getAmount());
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

    public byte[] financeSummary(ReportDtos.FinanceSummaryResponse report) {
        return workbook("财务汇总", wb -> {
            Sheet sheet = wb.createSheet("财务汇总"); Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("项目"); header.createCell(1).setCellValue("金额");
            String[][] values = {{"销售额", String.valueOf(report.getTotalSales())}, {"采购额", String.valueOf(report.getTotalPurchases())},
                    {"应收余额", String.valueOf(report.getTotalReceivables())}, {"应付余额", String.valueOf(report.getTotalPayables())},
                    {"库存金额", String.valueOf(report.getTotalInventory())}, {"净利润", String.valueOf(report.getNetProfit())}};
            for (int i = 0; i < values.length; i++) { Row row = sheet.createRow(i + 1); row.createCell(0).setCellValue(values[i][0]); row.createCell(1).setCellValue(values[i][1]); }
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

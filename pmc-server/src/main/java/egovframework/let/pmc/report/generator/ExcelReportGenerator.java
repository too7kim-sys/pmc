package egovframework.let.pmc.report.generator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

/**
 * Excel(XLSX) 보고서. SXSSF 스트리밍.
 */
@Component
public class ExcelReportGenerator implements ReportGenerator {

    @Override public String type() { return "XLSX"; }
    @Override public String extension() { return "xlsx"; }
    @Override public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public byte[] generate(ReportData data) {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("점검표");
            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle head = wb.createCellStyle();
            head.setFont(bold);

            int r = 0;
            Row title = sheet.createRow(r++);
            Cell tc = title.createCell(0);
            tc.setCellValue(data.getTitle());
            tc.setCellStyle(head);

            for (java.util.Map.Entry<String, String> e : data.getHeader().entrySet()) {
                Row hr = sheet.createRow(r++);
                hr.createCell(0).setCellValue(e.getKey());
                hr.createCell(1).setCellValue(e.getValue());
            }
            r++;

            for (ReportData.Section s : data.getSections()) {
                Row sec = sheet.createRow(r++);
                Cell sc = sec.createCell(0);
                sc.setCellValue("[" + s.getName() + "]");
                sc.setCellStyle(head);

                Row colRow = sheet.createRow(r++);
                for (int c = 0; c < s.getColumns().length; c++) {
                    Cell cell = colRow.createCell(c);
                    cell.setCellValue(s.getColumns()[c]);
                    cell.setCellStyle(head);
                }
                for (String[] row : s.getRows()) {
                    Row dr = sheet.createRow(r++);
                    for (int c = 0; c < row.length; c++) {
                        dr.createCell(c).setCellValue(row[c] == null ? "" : row[c]);
                    }
                }
                r++;
            }
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Excel 생성 실패", e);
        }
    }
}

package egovframework.let.pmc.report.generator;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * CSV 보고서. 한글 Excel 호환을 위해 UTF-8 BOM 포함.
 */
@Component
public class CsvReportGenerator implements ReportGenerator {

    @Override public String type() { return "CSV"; }
    @Override public String extension() { return "csv"; }
    @Override public String contentType() { return "text/csv; charset=UTF-8"; }

    @Override
    public byte[] generate(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append(data.getTitle()).append("\n");
        for (java.util.Map.Entry<String, String> e : data.getHeader().entrySet()) {
            sb.append(esc(e.getKey())).append(',').append(esc(e.getValue())).append("\n");
        }
        sb.append("\n");
        for (ReportData.Section s : data.getSections()) {
            sb.append("[").append(esc(s.getName())).append("]\n");
            sb.append(join(s.getColumns())).append("\n");
            for (String[] row : s.getRows()) {
                sb.append(join(row)).append("\n");
            }
            sb.append("\n");
        }
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(bom, 0, bom.length);
        out.write(body, 0, body.length);
        return out.toByteArray();
    }

    private String join(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(esc(cells[i]));
        }
        return sb.toString();
    }

    private String esc(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}

package egovframework.let.pmc.report.generator;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * PDF 보고서(OpenPDF). 한글은 CJK 내장폰트 또는 시스템 TTF로 렌더.
 */
@Component
public class PdfReportGenerator implements ReportGenerator {

    @Override public String type() { return "PDF"; }
    @Override public String extension() { return "pdf"; }
    @Override public String contentType() { return "application/pdf"; }

    @Override
    public byte[] generate(ReportData data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = koreanBaseFont();
            Font titleFont = new Font(bf, 16, Font.BOLD);
            Font headFont = new Font(bf, 10, Font.BOLD);
            Font bodyFont = new Font(bf, 9, Font.NORMAL);

            doc.add(new Paragraph(data.getTitle(), titleFont));
            doc.add(new Paragraph(" ", bodyFont));

            // 헤더(점검일/점검자/부서 등)
            for (java.util.Map.Entry<String, String> e : data.getHeader().entrySet()) {
                doc.add(new Paragraph(e.getKey() + " : " + e.getValue(), bodyFont));
            }
            doc.add(new Paragraph(" ", bodyFont));

            for (ReportData.Section s : data.getSections()) {
                doc.add(new Paragraph(s.getName(), headFont));
                PdfPTable table = new PdfPTable(s.getColumns().length);
                table.setWidthPercentage(100);
                for (String col : s.getColumns()) {
                    PdfPCell c = new PdfPCell(new Phrase(col, headFont));
                    c.setGrayFill(0.85f);
                    table.addCell(c);
                }
                for (String[] row : s.getRows()) {
                    for (String cell : row) {
                        table.addCell(new PdfPCell(new Phrase(cell == null ? "" : cell, bodyFont)));
                    }
                }
                doc.add(table);
                doc.add(new Paragraph(" ", bodyFont));
            }
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    /**
     * 한글 폰트 확보: classpath 동봉 TTF(권장) → 시스템 TTF → CJK 내장 → (최후) Helvetica.
     * 운영 한글 PDF 보장을 위해 {@code resources/fonts/NanumGothic.ttf} 동봉을 권장한다.
     * (package-private: 폰트 확보 실측 테스트용)
     */
    BaseFont koreanBaseFont() {
        // 1) classpath 동봉 TTF — 호스트 폰트와 무관하게 임베딩 보장
        try {
            java.io.InputStream in = getClass().getClassLoader().getResourceAsStream("fonts/NanumGothic.ttf");
            if (in != null) {
                byte[] ttf;
                try (java.io.InputStream is = in; java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
                    ttf = bos.toByteArray();
                }
                return BaseFont.createFont("NanumGothic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                        true, ttf, null);
            }
        } catch (Exception ignore) {
            // 다음 후보
        }
        // 2) 시스템 설치 TTF
        String[] candidates = {
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "C:/Windows/Fonts/malgun.ttf"
        };
        for (String p : candidates) {
            try {
                if (new File(p).exists()) {
                    return BaseFont.createFont(p, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignore) {
                // 다음 후보
            }
        }
        // 3) OpenPDF 내장 CJK (한국어)
        try {
            return BaseFont.createFont("HYSMyeongJo-Medium", "UniKS-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception ignore) {
            // 4) 최후: 라틴 전용(한글 불가) — 테스트가 이 폴백을 감지한다
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

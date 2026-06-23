package egovframework.let.pmc.report.generator;

import com.lowagie.text.pdf.BaseFont;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF 한글 폰트 임베딩 실측: 한글 보고서가 정상 생성되고,
 * 한글 불가 최후폴백(Helvetica/Cp1252)이 아닌 폰트가 선택되는지 검증.
 */
class PdfReportTest {

    private final PdfReportGenerator gen = new PdfReportGenerator();

    @Test
    void generatesKoreanPdf() {
        ReportData data = new ReportData("정보시스템 장애예방 점검표");
        data.addHeader("점검대상", "테스트서버-가나다");
        data.addHeader("종합판정", "위험");
        ReportData.Section s = data.addSection("[OS]", "점검항목", "수집값", "판정");
        s.addRow("CPU 사용률", "92%", "위험");
        s.addRow("메모리 사용률", "73%", "주의");

        byte[] pdf = gen.generate(data);
        assertNotNull(pdf);
        assertTrue(pdf.length > 1000, "PDF 바이트가 충분히 생성되어야 함");
        // PDF 시그니처 확인
        String head = new String(pdf, 0, 5, StandardCharsets.ISO_8859_1);
        assertTrue(head.startsWith("%PDF"), "PDF 시그니처(%PDF) 로 시작해야 함");
    }

    @Test
    void koreanCapableFontSelected() {
        BaseFont bf = gen.koreanBaseFont();
        assertNotNull(bf);
        // Cp1252(라틴 전용 Helvetica 최후폴백)이면 한글 렌더 불가 → 실측 실패로 간주
        assertNotEquals("Cp1252", bf.getEncoding(),
                "한글 가능 폰트(동봉 TTF/시스템 TTF/CJK)가 선택되어야 함 — 한글 PDF 폰트 미확보");
    }
}

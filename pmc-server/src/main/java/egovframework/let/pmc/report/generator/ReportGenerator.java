package egovframework.let.pmc.report.generator;

/**
 * 보고서 생성기 인터페이스 (PDF/Excel/CSV 구현).
 */
public interface ReportGenerator {
    /** 보고서 유형 코드 : PDF/XLSX/CSV */
    String type();
    String extension();
    String contentType();
    byte[] generate(ReportData data);
}

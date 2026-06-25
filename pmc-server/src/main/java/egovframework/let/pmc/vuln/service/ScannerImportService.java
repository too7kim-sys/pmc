package egovframework.let.pmc.vuln.service;

import java.util.List;

/**
 * 외부 취약점 스캐너 결과(JSON) → 정규화 VulnFinding 변환.
 */
public interface ScannerImportService {

    /**
     * @param scannerType TRIVY / OPENSCAP / NESSUS
     * @param serverId    대상 서버
     * @param json        스캐너 JSON export 본문
     * @return 정규화된 취약 항목 목록(source=SCANNER)
     */
    List<VulnFinding> parse(String scannerType, Long serverId, String json);
}

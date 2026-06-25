package egovframework.let.pmc.vuln.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import egovframework.let.pmc.common.ApiException;
import egovframework.let.pmc.vuln.service.ScannerImportService;
import egovframework.let.pmc.vuln.service.VulnFinding;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 외부 스캐너 JSON export → VulnFinding 정규화. 의존성 무추가(Jackson 사용).
 * 네이티브 XML(.nessus/ARF) 미지원 — JSON export 한정.
 */
@Service
public class ScannerImportServiceImpl implements ScannerImportService {

    private final ObjectMapper om = new ObjectMapper();

    @Override
    public List<VulnFinding> parse(String scannerType, Long serverId, String json) {
        if (serverId == null) {
            throw new ApiException("INVALID_REQUEST", "대상 서버를 선택하세요.");
        }
        if (scannerType == null || json == null || json.trim().isEmpty()) {
            throw new ApiException("INVALID_REQUEST", "스캐너 종류와 결과 파일이 필요합니다.");
        }
        JsonNode root;
        try {
            root = om.readTree(json);
        } catch (Exception e) {
            throw new ApiException("INVALID_REQUEST", "JSON 파싱 실패: " + e.getMessage());
        }
        String type = scannerType.trim().toUpperCase();
        switch (type) {
            case "TRIVY":    return parseTrivy(serverId, root);
            case "OPENSCAP": return parseOpenscap(serverId, root);
            case "NESSUS":   return parseNessus(serverId, root);
            default:
                throw new ApiException("INVALID_REQUEST", "지원하지 않는 스캐너: " + scannerType);
        }
    }

    /** Trivy: Results[].Vulnerabilities[] — VulnerabilityID/Title/Severity. */
    private List<VulnFinding> parseTrivy(Long serverId, JsonNode root) {
        List<VulnFinding> out = new ArrayList<>();
        JsonNode results = root.path("Results");
        if (results.isArray()) {
            for (JsonNode res : results) {
                JsonNode vulns = res.path("Vulnerabilities");
                if (!vulns.isArray()) continue;
                for (JsonNode v : vulns) {
                    String code = text(v, "VulnerabilityID");
                    if (code == null) continue;
                    String title = firstNonNull(text(v, "Title"), text(v, "PkgName"), code);
                    String sev = mapSeverity(text(v, "Severity"));
                    out.add(new VulnFinding(serverId, code, "SCANNER", "SEC",
                            "[Trivy] " + title, sev));
                }
            }
        }
        return out;
    }

    /** OpenSCAP(JSON): rule-result[] — idref/severity, result=fail 만 취약. */
    private List<VulnFinding> parseOpenscap(Long serverId, JsonNode root) {
        List<VulnFinding> out = new ArrayList<>();
        // 단순화된 정규 형태: { "rule-result": [ {idref, severity, result, title} ] }
        JsonNode arr = root.has("rule-result") ? root.path("rule-result") : root.path("ruleResults");
        if (arr.isArray()) {
            for (JsonNode r : arr) {
                String result = text(r, "result");
                if (result == null || !result.equalsIgnoreCase("fail")) continue;
                String code = firstNonNull(text(r, "idref"), text(r, "id"));
                if (code == null) continue;
                String title = firstNonNull(text(r, "title"), code);
                String sev = mapSeverity(text(r, "severity"));
                out.add(new VulnFinding(serverId, code, "SCANNER", "SEC",
                        "[OpenSCAP] " + title, sev));
            }
        }
        return out;
    }

    /** Nessus(JSON export): vulnerabilities[] — plugin_id/plugin_name/severity(또는 risk_factor). */
    private List<VulnFinding> parseNessus(Long serverId, JsonNode root) {
        List<VulnFinding> out = new ArrayList<>();
        JsonNode arr = root.has("vulnerabilities") ? root.path("vulnerabilities") : root.path("findings");
        if (arr.isArray()) {
            for (JsonNode v : arr) {
                String code = firstNonNull(text(v, "plugin_id"), text(v, "pluginId"));
                if (code == null) continue;
                String title = firstNonNull(text(v, "plugin_name"), text(v, "pluginName"),
                        text(v, "name"), "plugin-" + code);
                String rf = firstNonNull(text(v, "risk_factor"), text(v, "severity"));
                // Nessus 정보(Info) 항목은 제외
                if (rf != null && (rf.equalsIgnoreCase("none") || rf.equalsIgnoreCase("info")
                        || "0".equals(rf))) {
                    continue;
                }
                out.add(new VulnFinding(serverId, "NESSUS-" + code, "SCANNER", "SEC",
                        "[Nessus] " + title, mapSeverity(rf)));
            }
        }
        return out;
    }

    /** 스캐너 등급 → KISA 상/중/하. */
    private String mapSeverity(String raw) {
        if (raw == null) return "하";
        String s = raw.trim().toLowerCase();
        switch (s) {
            case "critical": case "high": case "4": case "3": case "상":
                return "상";
            case "medium": case "moderate": case "2": case "중":
                return "중";
            default:
                return "하";
        }
    }

    private String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        String s = v.asText();
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private String firstNonNull(String... vals) {
        for (String v : vals) if (v != null) return v;
        return null;
    }
}

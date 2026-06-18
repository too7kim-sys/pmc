package egovframework.let.pmc.ingest.service;

import java.util.List;

/**
 * Agent가 전송하는 점검 결과 엔벨로프(JSON 바인딩용).
 * docs/inspection-result.schema.json 과 동일 구조.
 */
public class IncomingResult {
    public String schemaVersion;
    public String agentId;
    public String hostname;
    public String platform;
    public Long policyId;
    public Integer policyVersion;
    public Long planId;
    public String runId;
    public String runType;
    public String startedAt;
    public String finishedAt;
    public List<IncomingItem> items;

    public static class IncomingItem {
        public String category;
        public String itemCode;
        public String name;
        public String value;
        public String unit;
        public String status;
        public String thresholdWarn;
        public String thresholdCritical;
        public String raw;
        public String collectedAt;
        public String error;
    }
}

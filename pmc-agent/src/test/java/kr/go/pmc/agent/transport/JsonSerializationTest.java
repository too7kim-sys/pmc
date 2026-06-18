package kr.go.pmc.agent.transport;

import com.fasterxml.jackson.databind.JsonNode;
import kr.go.pmc.agent.model.Category;
import kr.go.pmc.agent.model.InspectionResult;
import kr.go.pmc.agent.model.ResultItem;
import kr.go.pmc.agent.model.ResultStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InspectionResult JSON 직렬화가 inspection-result.schema.json 의 필드명과 일치하는지 검증.
 */
class JsonSerializationTest {

    private InspectionResult build() {
        InspectionResult r = new InspectionResult();
        r.setAgentId(UUID.randomUUID().toString());
        r.setHostname("web01");
        r.setPlatform("LINUX");
        r.setPolicyId(1);
        r.setPolicyVersion(2);
        r.setPlanId(3);
        r.setRunId(UUID.randomUUID().toString());
        r.setRunType("AUTO");
        r.setStartedAt(Instant.parse("2026-06-18T00:00:00Z"));
        r.setFinishedAt(Instant.parse("2026-06-18T00:00:05Z"));

        ResultItem it = new ResultItem();
        it.setCategory(Category.OS);
        it.setItemCode("OS_CPU_USAGE");
        it.setName("CPU 사용률");
        it.setValue("85");
        it.setUnit("%");
        it.setStatus(ResultStatus.WARN);
        it.setThresholdWarn("GTE 80");
        it.setThresholdCritical("GTE 90");
        it.setRaw("raw");
        it.setCollectedAt(Instant.parse("2026-06-18T00:00:03Z"));
        r.getItems().add(it);
        return r;
    }

    @Test
    void fieldNamesMatchSchema() throws Exception {
        String json = JsonMapper.get().writeValueAsString(build());
        JsonNode n = JsonMapper.get().readTree(json);

        // 엔벨로프 필수/주요 필드명
        assertEquals("1.0", n.get("schemaVersion").asText());
        assertTrue(n.has("agentId"));
        assertTrue(n.has("hostname"));
        assertEquals("LINUX", n.get("platform").asText());
        assertEquals(1, n.get("policyId").asInt());
        assertEquals(2, n.get("policyVersion").asInt());
        assertEquals(3, n.get("planId").asInt());
        assertTrue(n.has("runId"));
        assertEquals("AUTO", n.get("runType").asText());
        // Instant 는 ISO-8601 문자열로
        assertEquals("2026-06-18T00:00:00Z", n.get("startedAt").asText());
        assertTrue(n.get("items").isArray());

        JsonNode item = n.get("items").get(0);
        assertEquals("OS", item.get("category").asText());
        assertEquals("OS_CPU_USAGE", item.get("itemCode").asText());
        assertEquals("CPU 사용률", item.get("name").asText());
        assertEquals("85", item.get("value").asText());
        assertEquals("%", item.get("unit").asText());
        assertEquals("WARN", item.get("status").asText());
        assertEquals("GTE 80", item.get("thresholdWarn").asText());
        assertEquals("GTE 90", item.get("thresholdCritical").asText());
        assertEquals("raw", item.get("raw").asText());
        assertTrue(item.has("collectedAt"));
    }

    @Test
    void roundTripsBack() throws Exception {
        InspectionResult original = build();
        String json = JsonMapper.get().writeValueAsString(original);
        InspectionResult parsed = JsonMapper.get().readValue(json, InspectionResult.class);

        assertEquals(original.getRunId(), parsed.getRunId());
        assertEquals(original.getAgentId(), parsed.getAgentId());
        assertEquals(original.getPlatform(), parsed.getPlatform());
        assertEquals(original.getStartedAt(), parsed.getStartedAt());
        assertEquals(1, parsed.getItems().size());
        assertEquals(Category.OS, parsed.getItems().get(0).getCategory());
        assertEquals(ResultStatus.WARN, parsed.getItems().get(0).getStatus());
    }

    @Test
    void nullFieldsOmitted() throws Exception {
        InspectionResult r = new InspectionResult();
        r.setRunId("x");
        r.setAgentId("y");
        String json = JsonMapper.get().writeValueAsString(r);
        // policyId 등 null 필드는 직렬화되지 않아야 함(NON_NULL)
        assertFalse(json.contains("policyId"));
        assertFalse(json.contains("\"hostname\""));
    }
}

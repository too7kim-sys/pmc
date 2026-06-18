package egovframework.let.pmc.ingest.service;

import java.util.List;
import java.util.Map;

public interface IngestService {

    /** Agent 결과 수신(멱등). 반환 {runId, duplicated, overallStatus} */
    Map<String, Object> ingest(IncomingResult result, String sourceIp);

    List<InspectionRunVO> getRunList(Long serverId);
    InspectionRunVO getRunDetail(String runId);

    /** 수동 점검 결과 입력(정기점검 MANUAL 항목) */
    void saveManualRun(InspectionRunVO run, List<ResultItemVO> items);
}

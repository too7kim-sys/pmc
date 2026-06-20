package egovframework.let.pmc.ingest.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface InspectionMapper {

    int countRun(@Param("runId") String runId);
    /** run_id 멱등 INSERT. 영향 행수 반환(0=동일 run_id 이미 존재). */
    int insertRun(InspectionRunVO vo);
    void insertResultItem(ResultItemVO vo);

    List<InspectionRunVO> selectRunList(@Param("serverId") Long serverId);
    InspectionRunVO selectRun(@Param("runId") String runId);
    List<ResultItemVO> selectResultItems(@Param("runId") String runId);

    // 정기점검 계획 연계
    void linkPlanTarget(@Param("planId") Long planId, @Param("serverId") Long serverId,
                        @Param("runId") String runId);

    // 대시보드 집계 보조
    List<Map<String, Object>> selectStatusSummary();
}

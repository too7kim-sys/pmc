package egovframework.let.pmc.dq.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DqMapper {
    List<Map<String, Object>> selectRules();

    /** 관리자 정의 점검 SQL(위반 건수 반환) 실행. ${} 사용: 관리자 전용 룰에 한함. */
    Long executeCount(@Param("sql") String sql);

    void insertResult(@Param("ruleId") Long ruleId, @Param("violationCount") Long violationCount,
                      @Param("status") String status);

    List<Map<String, Object>> selectResults();
}

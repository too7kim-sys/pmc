package egovframework.let.pmc.risk.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 문제가능성(위험) 산정 근거 집계 매퍼.
 */
@Mapper
public interface RiskMapper {

    /** 서버별 위험 산정 근거(최근 days일). */
    List<Map<String, Object>> selectServerRiskFacts(@Param("days") int days);
}

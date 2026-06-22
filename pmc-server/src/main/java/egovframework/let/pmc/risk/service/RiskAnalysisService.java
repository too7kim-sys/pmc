package egovframework.let.pmc.risk.service;

import java.util.List;

/**
 * 규칙 기반 문제가능성(위험) 분석.
 */
public interface RiskAnalysisService {

    /** 서버별 위험 점수(내림차순). days = 분석 기간(일). */
    List<RiskScoreVO> analyze(int days);
}

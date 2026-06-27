package egovframework.com.cmm.service;

import java.util.List;
import java.util.Map;

/**
 * 감사 로그 기록/조회. 행위자(로그인 사용자)와 IP 는 현재 컨텍스트에서 자동 해석한다.
 */
public interface AuditService {
    /** 현재 사용자/IP 로 감사 로그 1건 기록(실패해도 본 처리에 영향 없음). */
    void log(String action, String targetType, String targetId, String details);

    /** 지정 행위자로 기록(로그인 이벤트 등 SecurityContext 미설정 시점용). */
    void log(String actorId, String action, String targetType, String targetId, String details);

    List<Map<String, Object>> recent(int limit);
}

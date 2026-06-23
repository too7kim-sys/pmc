package egovframework.let.pmc.notify.service;

import egovframework.let.pmc.ingest.service.InspectionRunVO;

import java.util.List;
// AlertChannelVO 동일 패키지

/**
 * 이상 알림 발행(Webhook). 기본 비활성(Globals.AlertEnabled), 중복억제 적용.
 */
public interface AlertService {

    /** 점검 결과 수신 시 호출 — 종합판정 CRITICAL 또는 SVC 실패 시 통지. */
    void raiseRunAlert(InspectionRunVO run, int svcFailCount);

    /** heartbeat 누락 서버 통지(스케줄). */
    void raiseHeartbeatAlerts();

    /** 임의 알림 발송(정기 보고서 생성 통지 등). */
    void raise(String alertType, Long serverId, String refId, String severity, String title, String message);

    /** 알림 이력. */
    List<AlertLogVO> getAlertLog(int limit);

    // 알림 채널 관리
    List<AlertChannelVO> getChannels();
    void addChannel(AlertChannelVO vo);
    void setChannelEnabled(Long channelId, String enabled);
    void deleteChannel(Long channelId);
}

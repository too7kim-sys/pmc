package egovframework.let.pmc.notify.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 이상 알림 이력 + 중복억제 + heartbeat 누락 조회 매퍼.
 */
@Mapper
public interface AlertMapper {

    void insertAlert(AlertLogVO vo);

    /** 같은 type+server 가 최근 min 분 내 발송(SENT)된 건수(중복억제용). server 가 null 이면 type 기준. */
    int countRecent(@Param("alertType") String alertType, @Param("serverId") Long serverId,
                    @Param("min") int min);

    /** ACTIVE agent 중 heartbeat 가 hours 시간 이상 누락된 서버. */
    List<Map<String, Object>> selectStaleHeartbeats(@Param("hours") int hours);

    List<AlertLogVO> selectAlertLog(@Param("limit") int limit);
}

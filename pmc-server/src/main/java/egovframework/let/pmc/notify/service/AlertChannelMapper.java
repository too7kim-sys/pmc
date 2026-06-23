package egovframework.let.pmc.notify.service;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 알림 채널 관리 매퍼.
 */
@Mapper
public interface AlertChannelMapper {

    List<AlertChannelVO> selectChannels();          // 전체(관리 화면)
    List<AlertChannelVO> selectEnabledChannels();   // 활성(발송 대상)
    void insertChannel(AlertChannelVO vo);
    void updateEnabled(@Param("channelId") Long channelId, @Param("enabled") String enabled);
    void deleteChannel(@Param("channelId") Long channelId);
}

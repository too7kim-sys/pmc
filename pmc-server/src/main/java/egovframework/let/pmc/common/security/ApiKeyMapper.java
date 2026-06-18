package egovframework.let.pmc.common.security;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Agent API 키 해시 검증 매퍼.
 */
@Mapper
public interface ApiKeyMapper {

    /** api_key_hash 로 ACTIVE 상태 agent_id 조회 (없으면 null) */
    String selectActiveAgentIdByKeyHash(@Param("keyHash") String keyHash);
}

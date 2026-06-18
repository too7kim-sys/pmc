package egovframework.let.pmc.common.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Agent API 키 검증. 평문 키를 sha-256 해시하여 pmc_agent.api_key_hash 와 대조.
 */
@Service
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;

    @Autowired
    public ApiKeyService(ApiKeyMapper apiKeyMapper) {
        this.apiKeyMapper = apiKeyMapper;
    }

    /** 유효하면 agentId, 아니면 null */
    public String resolveAgentId(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isEmpty()) {
            return null;
        }
        return apiKeyMapper.selectActiveAgentIdByKeyHash(Hashing.sha256(rawApiKey));
    }
}

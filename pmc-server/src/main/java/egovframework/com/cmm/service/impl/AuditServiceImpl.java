package egovframework.com.cmm.service.impl;

import egovframework.com.cmm.service.AuditMapper;
import egovframework.com.cmm.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 감사 로그 서비스. 기록 실패는 삼켜서(로그만) 업무 처리에 영향을 주지 않는다.
 */
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditMapper auditMapper;

    @Autowired
    public AuditServiceImpl(AuditMapper auditMapper) {
        this.auditMapper = auditMapper;
    }

    @Override
    public void log(String action, String targetType, String targetId, String details) {
        log(currentActor(), action, targetType, targetId, details);
    }

    @Override
    public void log(String actorId, String action, String targetType, String targetId, String details) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("actorId", trim(actorId, 40));
            p.put("action", trim(action, 60));
            p.put("targetType", trim(targetType, 40));
            p.put("targetId", trim(targetId, 60));
            p.put("details", details);
            p.put("actorIp", clientIp());
            auditMapper.insertAudit(p);
        } catch (Exception e) {
            log.warn("감사 로그 기록 실패(action={}): {}", action, e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> recent(int limit) {
        return auditMapper.selectAuditLog(limit);
    }

    private String currentActor() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null ? a.getName() : null;
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attr =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr == null) return null;
            HttpServletRequest req = attr.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                int comma = xff.indexOf(',');
                return trim(comma > 0 ? xff.substring(0, comma) : xff, 45);
            }
            return trim(req.getRemoteAddr(), 45);
        } catch (Exception e) {
            return null;
        }
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}

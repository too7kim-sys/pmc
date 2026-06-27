package egovframework.let.pmc.common.security;

import egovframework.com.cmm.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 폼 로그인 성공/실패 이벤트 처리:
 *  - 성공: fail_cnt 초기화·last_login 갱신, 감사로그(LOGIN_SUCCESS)
 *  - 실패: fail_cnt 누적·임계치 도달 시 계정 잠금(BadCredentials), 감사로그(LOGIN_FAIL)
 * Agent 토큰 인증은 ProviderManager 를 거치지 않아 이벤트가 발생하지 않는다(영향 없음).
 */
@Component
public class LoginEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoginEventListener.class);

    private final UserMapper userMapper;
    private final AuditService auditService;

    @Value("${Globals.LoginFailLockCnt:5}")
    private int loginFailLockCnt;

    @Autowired
    public LoginEventListener(UserMapper userMapper, AuditService auditService) {
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String id = name(event.getAuthentication());
        if (id == null) return;
        try {
            userMapper.updateLoginSuccess(id);
        } catch (Exception e) {
            log.warn("로그인 성공 처리 실패(id={}): {}", id, e.getMessage());
        }
        auditService.log(id, "LOGIN_SUCCESS", "USER", id, null);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String id = name(event.getAuthentication());
        String cause = event.getException() != null
                ? event.getException().getClass().getSimpleName() : "AuthError";
        if (id != null && event instanceof AuthenticationFailureBadCredentialsEvent) {
            try {
                userMapper.lockIfExceeded(id, loginFailLockCnt);
            } catch (Exception e) {
                log.warn("로그인 실패 누적 처리 실패(id={}): {}", id, e.getMessage());
            }
        }
        auditService.log(id, "LOGIN_FAIL", "USER", id, cause);
    }

    private String name(org.springframework.security.core.Authentication a) {
        return a != null && a.getName() != null ? a.getName() : null;
    }
}

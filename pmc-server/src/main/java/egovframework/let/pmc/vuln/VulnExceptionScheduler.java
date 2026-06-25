package egovframework.let.pmc.vuln;

import egovframework.let.pmc.vuln.service.VulnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 취약점 예외(수용/면제) 만료 처리(재발방지). 기존 pmcScheduler 사용.
 * 만료된 예외를 EXPIRED 로 전환하고 대상 finding 을 EXEMPTED→OPEN 재활성한다
 * (다음 진단에서 재탐지되면 RECURRED 로직이 자동 알림).
 */
@Component
public class VulnExceptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(VulnExceptionScheduler.class);

    private final VulnService vulnService;

    @Autowired
    public VulnExceptionScheduler(VulnService vulnService) {
        this.vulnService = vulnService;
    }

    /** 매시간 만료 예외 정리. */
    @Scheduled(fixedRate = 3600000)
    public void expireExceptions() {
        try {
            int n = vulnService.expireExceptions();
            if (n > 0) {
                log.info("취약점 예외 만료 처리: {}건 → finding 재활성", n);
            }
        } catch (Exception e) {
            log.warn("취약점 예외 만료 처리 실패: {}", e.getMessage());
        }
    }
}

package egovframework.let.pmc.vuln.service.impl;

import egovframework.let.pmc.notify.service.AlertService;
import egovframework.let.pmc.vuln.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 취약점 추적(동일 취약점 관리)·재발방지 상태머신.
 * <p>
 * 지문 = (server_id, check_code, source). 상태 전이:
 * <pre>
 *  없음        → INSERT OPEN (severity 상이면 VULN_NEW 알림, 옵션)
 *  EXEMPTED+유효예외 → 발생횟수만 갱신, 알림 skip
 *  OPEN        → 발생횟수 갱신(알림 없음)
 *  FIXED/만료EXEMPTED → RECURRED 전환 + 등급 에스컬레이션 + VULN_RECUR 알림
 *  RECURRED    → 발생횟수 갱신(AlertService 중복억제로 알림 dedupe)
 *  부재(스캔에 없음, fullScan) → FIXED 자동 전환
 * </pre>
 */
@Service
public class VulnServiceImpl implements VulnService {

    private static final Logger log = LoggerFactory.getLogger(VulnServiceImpl.class);

    private final VulnMapper vulnMapper;
    private final AlertService alertService;

    @Value("${Globals.VulnNewAlertEnabled:false}")
    private boolean vulnNewAlertEnabled;

    @Autowired
    public VulnServiceImpl(VulnMapper vulnMapper, AlertService alertService) {
        this.vulnMapper = vulnMapper;
        this.alertService = alertService;
    }

    @Override
    @Transactional
    public void processFindings(Long serverId, String runId, String source,
                                List<VulnFinding> detected, boolean fullScan) {
        if (serverId == null) {
            return; // 서버 식별 불가 시 추적 불가
        }
        Set<String> presentCodes = new HashSet<>();
        if (detected != null) {
            for (VulnFinding f : detected) {
                if (f == null || f.getCheckCode() == null) continue;
                presentCodes.add(f.getCheckCode());
                processOne(serverId, runId, source, f);
            }
        }
        // 부재 항목 자동 해소(전체 스캔에서만). 부분 추가(MANUAL)는 해소하지 않음.
        if (fullScan) {
            vulnMapper.markFixedForServerExcept(serverId, source,
                    new java.util.ArrayList<>(presentCodes), "system");
        }
    }

    private void processOne(Long serverId, String runId, String source, VulnFinding f) {
        String sev = normalizeSeverity(f.getSeverity());
        VulnFindingVO existing = vulnMapper.selectFinding(serverId, f.getCheckCode(), source);

        if (existing == null) {
            VulnFindingVO vo = new VulnFindingVO();
            vo.setServerId(serverId);
            vo.setCheckCode(f.getCheckCode());
            vo.setSource(source);
            vo.setCategory(f.getCategory() == null ? "SEC" : f.getCategory());
            vo.setTitle(f.getTitle());
            vo.setSeverity(sev);
            vo.setStatus("OPEN");
            vo.setLastRunId(runId);
            vo.setRegUser("system");
            vulnMapper.insertFinding(vo);
            if (vulnNewAlertEnabled && "상".equals(sev)) {
                safeAlert("VULN_NEW", serverId, "finding#" + vo.getFindingId(),
                        sevToAlertSeverity(sev), "[취약점 신규] " + f.getTitle(),
                        hostMsg(existing, f) + " 신규 취약점 탐지(" + sev + ")");
            }
            return;
        }

        String status = existing.getStatus();
        if ("EXEMPTED".equals(status)) {
            VulnExceptionVO ex = vulnMapper.selectActiveException(existing.getFindingId());
            if (ex != null) {
                // 유효 예외 — 발생횟수만 갱신, 상태 유지, 알림 skip
                vulnMapper.updateFindingDetected(existing.getFindingId(), "EXEMPTED", sev, runId, 0, "system");
                return;
            }
            // 만료된 예외인데 아직 상태 미전환 → 재발로 처리
            recur(existing, sev, runId, f);
            return;
        }
        if ("OPEN".equals(status)) {
            vulnMapper.updateFindingDetected(existing.getFindingId(), "OPEN", sev, runId, 0, "system");
            return;
        }
        if ("FIXED".equals(status)) {
            recur(existing, sev, runId, f);
            return;
        }
        // RECURRED — 발생횟수만 갱신(알림은 AlertService 중복억제 윈도우로 dedupe)
        vulnMapper.updateFindingDetected(existing.getFindingId(), "RECURRED", sev, runId, 0, "system");
    }

    /** FIXED/만료예외 → RECURRED 전환 + 등급 에스컬레이션 + 알림. */
    private void recur(VulnFindingVO existing, String detectedSev, String runId, VulnFinding f) {
        String escalated = escalate(existing.getSeverity(), detectedSev);
        vulnMapper.updateFindingDetected(existing.getFindingId(), "RECURRED", escalated, runId, 1, "system");
        safeAlert("VULN_RECUR", existing.getServerId(), "finding#" + existing.getFindingId(),
                sevToAlertSeverity(escalated),
                "[취약점 재발] " + existing.getTitle(),
                hostMsg(existing, f) + " 조치 후 재발(등급 " + escalated + ", 재발 " + (existing.getRecurCount() + 1) + "회)");
    }

    private String hostMsg(VulnFindingVO existing, VulnFinding f) {
        String host = existing != null && existing.getHostname() != null ? existing.getHostname() : "서버";
        String code = existing != null ? existing.getCheckCode() : (f != null ? f.getCheckCode() : "");
        return host + " / " + code;
    }

    private void safeAlert(String type, Long serverId, String ref, String sev, String title, String msg) {
        try {
            alertService.raise(type, serverId, ref, sev, title, msg);
        } catch (Exception e) {
            log.warn("취약점 알림 발송 실패(type={}): {}", type, e.getMessage());
        }
    }

    @Override
    public List<VulnFindingVO> getFindings(Long serverId, String severity, String status) {
        return vulnMapper.selectFindings(serverId, severity, status);
    }

    @Override
    public List<VulnFindingVO> getFindingsPaged(Long serverId, String severity, String status,
                                                String keyword, int limit, int offset) {
        return vulnMapper.selectFindingsPaged(serverId, severity, status, keyword, limit, offset);
    }

    @Override
    public int countFindings(Long serverId, String severity, String status, String keyword) {
        return vulnMapper.countFindings(serverId, severity, status, keyword);
    }

    @Override
    public VulnFindingVO getFinding(Long findingId) {
        return vulnMapper.selectFindingById(findingId);
    }

    @Override
    public List<VulnActionVO> getActions(Long findingId) {
        return vulnMapper.selectActions(findingId);
    }

    @Override
    public VulnExceptionVO getActiveException(Long findingId) {
        return vulnMapper.selectActiveException(findingId);
    }

    @Override
    public List<Map<String, Object>> getRecurrenceSummary() {
        return vulnMapper.selectRecurrenceSummary();
    }

    @Override
    @Transactional
    public void registerAction(Long findingId, String actionUser, String actionDesc,
                               String resultingStatus, String regUser) {
        VulnActionVO vo = new VulnActionVO();
        vo.setFindingId(findingId);
        vo.setActionUser(actionUser);
        vo.setActionDesc(actionDesc);
        vo.setResultingStatus(resultingStatus);
        vo.setRegUser(regUser);
        vulnMapper.insertAction(vo);
        if (resultingStatus != null && !resultingStatus.isEmpty()) {
            vulnMapper.updateFindingStatus(findingId, resultingStatus, regUser);
        }
    }

    @Override
    @Transactional
    public void registerException(Long findingId, String reason, String approver,
                                  OffsetDateTime expiresAt, String regUser) {
        VulnExceptionVO ex = new VulnExceptionVO();
        ex.setFindingId(findingId);
        ex.setReason(reason);
        ex.setApprover(approver);
        ex.setStatus("ACTIVE");
        ex.setExpiresAt(expiresAt);
        ex.setRegUser(regUser);
        vulnMapper.insertException(ex);
        vulnMapper.updateFindingStatus(findingId, "EXEMPTED", regUser);
        // 조치이력에도 흔적
        registerActionInternal(findingId, approver, "예외(수용/면제) 등록: " + reason, null, regUser);
    }

    private void registerActionInternal(Long findingId, String user, String desc,
                                        String resultingStatus, String regUser) {
        VulnActionVO vo = new VulnActionVO();
        vo.setFindingId(findingId);
        vo.setActionUser(user);
        vo.setActionDesc(desc);
        vo.setResultingStatus(resultingStatus);
        vo.setRegUser(regUser);
        vulnMapper.insertAction(vo);
    }

    @Override
    @Transactional
    public int expireExceptions() {
        List<VulnExceptionVO> expired = vulnMapper.selectExpiredExceptions();
        int n = 0;
        for (VulnExceptionVO ex : expired) {
            vulnMapper.expireException(ex.getExceptionId());
            // 면제 만료 → finding 재활성(EXEMPTED→OPEN). 다음 스캔에서 RECURRED 로직이 재알림.
            VulnFindingVO f = vulnMapper.selectFindingById(ex.getFindingId());
            if (f != null && "EXEMPTED".equals(f.getStatus())) {
                vulnMapper.updateFindingStatus(ex.getFindingId(), "OPEN", "system");
            }
            n++;
        }
        return n;
    }

    // ---- 등급 헬퍼 ----

    private String normalizeSeverity(String s) {
        if ("상".equals(s) || "중".equals(s) || "하".equals(s)) return s;
        return "중";
    }

    /** 재발 시 등급 에스컬레이션: 기존·탐지 등급 중 높은 쪽에서 한 단계 상향(하→중→상, 상 cap). */
    private String escalate(String prev, String detected) {
        int p = rank(prev), d = rank(detected);
        int base = Math.max(p, d);
        int up = Math.min(base + 1, 3); // 1하 2중 3상
        return fromRank(up);
    }

    private int rank(String s) {
        if ("상".equals(s)) return 3;
        if ("중".equals(s)) return 2;
        return 1;
    }

    private String fromRank(int r) {
        if (r >= 3) return "상";
        if (r == 2) return "중";
        return "하";
    }

    /** 등급(상/중/하) → 알림 severity(CRITICAL/WARN). */
    private String sevToAlertSeverity(String sev) {
        return "상".equals(sev) ? "CRITICAL" : "WARN";
    }
}

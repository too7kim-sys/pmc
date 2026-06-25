-- =====================================================================
-- PMC V10 : 취약점/구성(config) 진단 — 동일 취약점 추적 + 재발방지
--   pmc_vuln_finding   : 서버별 동일 취약점을 지문(server+code+source)으로 1건 추적
--   pmc_vuln_exception : 예외(수용/면제) 등록(만료일) — 재알림 억제·자동 재활성 근거
--   pmc_vuln_action    : 조치이력(담당자/조치내용/조치일)
-- =====================================================================

-- 추적 엔티티(de-dup). 지문 = (server_id, check_code, source).
CREATE TABLE IF NOT EXISTS pmc_vuln_finding (
    finding_id        BIGSERIAL    PRIMARY KEY,
    server_id         BIGINT       NOT NULL REFERENCES pmc_server(server_id) ON DELETE CASCADE,
    check_code        VARCHAR(60)  NOT NULL,                 -- SEC_U01_ROOT_REMOTE / CVE-xxxx / 스캐너 rule id
    source            VARCHAR(10)  NOT NULL DEFAULT 'BUILTIN'
                      CHECK (source IN ('BUILTIN','SCANNER','MANUAL')),
    category          VARCHAR(10)  NOT NULL DEFAULT 'SEC',
    title             VARCHAR(300) NOT NULL,
    severity          VARCHAR(4)   NOT NULL DEFAULT '중'      -- KISA 등급(상/중/하)
                      CHECK (severity IN ('상','중','하')),
    status            VARCHAR(10)  NOT NULL DEFAULT 'OPEN'
                      CHECK (status IN ('OPEN','FIXED','RECURRED','EXEMPTED')),
    first_detected_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_detected_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    occurrence_count  INT          NOT NULL DEFAULT 1,
    recur_count       INT          NOT NULL DEFAULT 0,        -- FIXED→재탐지 누적(에스컬레이션 근거)
    last_run_id       UUID,                                   -- 마지막 탐지 run(스캐너 import 는 NULL) — 무FK(ref 패턴)
    reg_user          VARCHAR(40),
    reg_dt            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    upd_user          VARCHAR(40),
    upd_dt            TIMESTAMPTZ,
    CONSTRAINT uq_pmc_vuln_finding_fp UNIQUE (server_id, check_code, source)
);
COMMENT ON TABLE pmc_vuln_finding IS '취약점 추적 엔티티(서버별 지문 단위 동일취약점 관리·재발추적)';
CREATE INDEX IF NOT EXISTS ix_pmc_vuln_finding_status ON pmc_vuln_finding(status, severity);
CREATE INDEX IF NOT EXISTS ix_pmc_vuln_finding_server ON pmc_vuln_finding(server_id, last_detected_at DESC);

-- 예외(수용/면제) — 만료일 도래 시 스케줄러가 EXPIRED 처리 + finding 재활성.
CREATE TABLE IF NOT EXISTS pmc_vuln_exception (
    exception_id  BIGSERIAL    PRIMARY KEY,
    finding_id    BIGINT       NOT NULL REFERENCES pmc_vuln_finding(finding_id) ON DELETE CASCADE,
    reason        VARCHAR(500) NOT NULL,
    approver      VARCHAR(40),
    status        VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE','EXPIRED','REVOKED')),
    expires_at    TIMESTAMPTZ  NOT NULL,
    reg_user      VARCHAR(40),
    reg_dt        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_vuln_exception IS '취약점 예외(수용/면제) — 재알림 억제·만료 자동 재활성';
CREATE INDEX IF NOT EXISTS ix_pmc_vuln_exception_active ON pmc_vuln_exception(status, expires_at);
CREATE INDEX IF NOT EXISTS ix_pmc_vuln_exception_finding ON pmc_vuln_exception(finding_id);

-- 조치이력(담당자/조치내용/조치일).
CREATE TABLE IF NOT EXISTS pmc_vuln_action (
    action_id        BIGSERIAL    PRIMARY KEY,
    finding_id       BIGINT       NOT NULL REFERENCES pmc_vuln_finding(finding_id) ON DELETE CASCADE,
    action_user      VARCHAR(40),                            -- 담당자
    action_desc      TEXT,                                   -- 조치내용
    action_dt        TIMESTAMPTZ  NOT NULL DEFAULT now(),    -- 조치일
    resulting_status VARCHAR(10)
                     CHECK (resulting_status IS NULL OR resulting_status IN ('FIXED','OPEN','EXEMPTED')),
    reg_user         VARCHAR(40),
    reg_dt           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_vuln_action IS '취약점 조치이력';
CREATE INDEX IF NOT EXISTS ix_pmc_vuln_action_finding ON pmc_vuln_action(finding_id, action_dt DESC);

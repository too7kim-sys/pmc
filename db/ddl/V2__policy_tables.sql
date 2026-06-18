-- =====================================================================
-- PMC V2 : 점검 정책 / 점검항목 / 임계치 / 웹서비스(SVC) 점검대상
--          점검항목은 행정안전부 「정보시스템 장애예방·대응 통합표준 매뉴얼」
--          및 「정보시스템 안정성 기준」 체계(대분류/중분류, 점검주기, 판정기준,
--          자동·수동 구분)에 준한다.
-- =====================================================================

CREATE TABLE IF NOT EXISTS pmc_policy (
    policy_id      BIGSERIAL    PRIMARY KEY,
    policy_name    VARCHAR(200) NOT NULL,
    description    TEXT,
    version        INT          NOT NULL DEFAULT 1,
    schedule_cron  VARCHAR(60),                -- 기본 실행주기
    os_scope       VARCHAR(30),                -- NULL = 전체 OS
    use_yn         CHAR(1)      NOT NULL DEFAULT 'Y',
    reg_user       VARCHAR(40),
    reg_dt         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    upd_user       VARCHAR(40),
    upd_dt         TIMESTAMPTZ
);
COMMENT ON TABLE pmc_policy IS '점검 정책(헤더)';

CREATE TABLE IF NOT EXISTS pmc_policy_item (
    policy_item_id BIGSERIAL    PRIMARY KEY,
    policy_id      BIGINT       NOT NULL REFERENCES pmc_policy(policy_id) ON DELETE CASCADE,
    category       VARCHAR(10)  NOT NULL,      -- OS/WEB/WAS/DB/SW/NW/SVC (공통코드 CHK_CATEGORY)
    mid_category   VARCHAR(60),                -- 중분류
    item_code      VARCHAR(60)  NOT NULL,      -- OS_CPU_USAGE ...
    item_name      VARCHAR(200) NOT NULL,
    unit           VARCHAR(20),
    check_cycle    VARCHAR(12)  NOT NULL DEFAULT 'DAILY', -- DAILY/WEEKLY/MONTHLY/QUARTERLY
    check_method   TEXT,                       -- 점검방법 설명
    check_type     VARCHAR(10)  NOT NULL DEFAULT 'AUTO',  -- AUTO/MANUAL
    judge_criteria TEXT,                       -- 판정기준 설명
    collect_yn     CHAR(1)      NOT NULL DEFAULT 'Y',
    sort_order     INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_pmc_policy_item UNIQUE (policy_id, item_code)
);
COMMENT ON TABLE pmc_policy_item IS '정책별 점검항목(행안부 매뉴얼 준거)';

CREATE TABLE IF NOT EXISTS pmc_policy_threshold (
    threshold_id   BIGSERIAL    PRIMARY KEY,
    policy_item_id BIGINT       NOT NULL REFERENCES pmc_policy_item(policy_item_id) ON DELETE CASCADE,
    level          VARCHAR(10)  NOT NULL,      -- WARN/CRITICAL
    operator       VARCHAR(12)  NOT NULL,      -- GT/GTE/LT/LTE/EQ/RANGE/REGEX
    compare_value  VARCHAR(200),
    range_low      VARCHAR(200),
    range_high     VARCHAR(200)
);
COMMENT ON TABLE pmc_policy_threshold IS '점검항목 임계치(판정 기준값)';
CREATE INDEX IF NOT EXISTS ix_pmc_threshold_item ON pmc_policy_threshold(policy_item_id);

-- 웹서비스(SVC) 점검 대상 URL ------------------------------------------
CREATE TABLE IF NOT EXISTS pmc_policy_svc_target (
    svc_target_id   BIGSERIAL    PRIMARY KEY,
    policy_id       BIGINT       NOT NULL REFERENCES pmc_policy(policy_id) ON DELETE CASCADE,
    svc_name        VARCHAR(200) NOT NULL,
    url             VARCHAR(1000) NOT NULL,
    http_method     VARCHAR(10)  NOT NULL DEFAULT 'GET',
    expected_status INT          NOT NULL DEFAULT 200,
    expected_content VARCHAR(500),             -- 본문 검증 키워드
    timeout_ms      INT          NOT NULL DEFAULT 5000,
    ssl_check_yn    CHAR(1)      NOT NULL DEFAULT 'Y',
    use_yn          CHAR(1)      NOT NULL DEFAULT 'Y'
);
COMMENT ON TABLE pmc_policy_svc_target IS '웹서비스(SVC) 가용성 점검 대상 URL';
CREATE INDEX IF NOT EXISTS ix_pmc_svc_policy ON pmc_policy_svc_target(policy_id);

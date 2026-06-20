-- =====================================================================
-- PMC V7 : 데이터 품질 점검 룰/결과
-- =====================================================================

CREATE TABLE IF NOT EXISTS pmc_dq_rule (
    rule_id       BIGSERIAL    PRIMARY KEY,
    rule_name     VARCHAR(200) NOT NULL UNIQUE,    -- 룰 식별(시드 재실행 멱등용)
    target_table  VARCHAR(60)  NOT NULL,
    rule_type     VARCHAR(20)  NOT NULL,       -- NOTNULL/RANGE/CODE/REFERENTIAL/CUSTOM
    check_sql     TEXT         NOT NULL,        -- 위반 건수를 반환하는 SELECT count(*) ...
    severity      VARCHAR(10)  NOT NULL DEFAULT 'WARN', -- INFO/WARN/CRITICAL
    use_yn        CHAR(1)      NOT NULL DEFAULT 'Y',
    reg_dt        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_dq_rule IS '데이터 품질 점검 룰';

CREATE TABLE IF NOT EXISTS pmc_dq_result (
    result_id        BIGSERIAL   PRIMARY KEY,
    rule_id          BIGINT      NOT NULL REFERENCES pmc_dq_rule(rule_id),
    checked_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    violation_count  BIGINT      NOT NULL DEFAULT 0,
    status           VARCHAR(10) NOT NULL DEFAULT 'PASS', -- PASS/FAIL/ERROR
    sample           TEXT
);
COMMENT ON TABLE pmc_dq_result IS '데이터 품질 점검 결과';
CREATE INDEX IF NOT EXISTS ix_pmc_dq_result_rule ON pmc_dq_result(rule_id, checked_at DESC);

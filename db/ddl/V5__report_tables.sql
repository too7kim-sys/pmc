-- =====================================================================
-- PMC V5 : 생성 보고서 메타
-- =====================================================================

CREATE TABLE IF NOT EXISTS pmc_report (
    report_id    BIGSERIAL    PRIMARY KEY,
    report_type  VARCHAR(10)  NOT NULL,        -- PDF/XLSX/CSV
    scope_type   VARCHAR(20)  NOT NULL,        -- SINGLE_RUN/PLAN/SERVER_PERIOD
    plan_id      BIGINT,
    server_id    BIGINT,
    run_id       UUID,
    period_from  DATE,
    period_to    DATE,
    file_path    VARCHAR(500),
    file_name    VARCHAR(255),
    file_size    BIGINT,
    gen_status   VARCHAR(20)  NOT NULL DEFAULT 'DONE', -- PENDING/DONE/FAILED
    reg_user     VARCHAR(40),
    reg_dt       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_report IS '생성된 점검 보고서 메타';
CREATE INDEX IF NOT EXISTS ix_pmc_report_scope ON pmc_report(scope_type, reg_dt DESC);

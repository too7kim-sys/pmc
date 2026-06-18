-- =====================================================================
-- PMC V3 : 점검 실행(run) / 점검 결과 항목
-- =====================================================================

CREATE TABLE IF NOT EXISTS pmc_inspection_run (
    run_id         UUID         PRIMARY KEY,   -- Agent 생성, 멱등 키
    agent_id       UUID         REFERENCES pmc_agent(agent_id),
    server_id      BIGINT       REFERENCES pmc_server(server_id),
    policy_id      BIGINT,
    policy_version INT,
    plan_id        BIGINT,                     -- 정기점검 계획 연계(nullable)
    run_type       VARCHAR(10)  NOT NULL DEFAULT 'AUTO', -- AUTO/MANUAL
    started_at     TIMESTAMPTZ,
    finished_at    TIMESTAMPTZ,
    overall_status VARCHAR(10),                -- NORMAL/WARN/CRITICAL/ERROR
    item_count     INT          NOT NULL DEFAULT 0,
    warn_count     INT          NOT NULL DEFAULT 0,
    critical_count INT          NOT NULL DEFAULT 0,
    error_count    INT          NOT NULL DEFAULT 0,
    source_ip      VARCHAR(45),
    received_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_inspection_run IS '점검 실행 단위(run). run_id로 멱등 처리';
CREATE INDEX IF NOT EXISTS ix_pmc_run_server  ON pmc_inspection_run(server_id, received_at DESC);
CREATE INDEX IF NOT EXISTS ix_pmc_run_plan    ON pmc_inspection_run(plan_id);

CREATE TABLE IF NOT EXISTS pmc_inspection_result_item (
    result_item_id    BIGSERIAL    PRIMARY KEY,
    run_id            UUID         NOT NULL REFERENCES pmc_inspection_run(run_id) ON DELETE CASCADE,
    category          VARCHAR(10)  NOT NULL,
    item_code         VARCHAR(60)  NOT NULL,
    item_name         VARCHAR(200),
    value             VARCHAR(2000),
    unit              VARCHAR(20),
    status            VARCHAR(10)  NOT NULL DEFAULT 'NORMAL', -- NORMAL/WARN/CRITICAL/ERROR/NA
    source            VARCHAR(10)  NOT NULL DEFAULT 'AUTO',   -- AUTO/MANUAL
    input_user        VARCHAR(40),                            -- 수동 입력자
    threshold_warn    VARCHAR(200),
    threshold_critical VARCHAR(200),
    raw_text          TEXT,
    error_text        TEXT,
    collected_at      TIMESTAMPTZ
);
COMMENT ON TABLE pmc_inspection_result_item IS '점검 결과 항목(분류·항목별 수집값·판정)';
CREATE INDEX IF NOT EXISTS ix_pmc_result_run  ON pmc_inspection_result_item(run_id);
CREATE INDEX IF NOT EXISTS ix_pmc_result_item ON pmc_inspection_result_item(item_code, status);

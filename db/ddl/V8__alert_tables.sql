-- =====================================================================
-- PMC V8 : 이상 알림 이력 (Webhook 통지 + 중복억제 근거)
-- =====================================================================

CREATE TABLE IF NOT EXISTS pmc_alert_log (
    alert_id     BIGSERIAL    PRIMARY KEY,
    alert_type   VARCHAR(20)  NOT NULL,        -- RUN_CRITICAL/SVC_FAIL/HB_STALE/AUTO_REPORT
    server_id    BIGINT       REFERENCES pmc_server(server_id),
    ref_id       VARCHAR(64),                  -- run_id 등 참조 식별자
    severity     VARCHAR(10),                  -- WARN/CRITICAL 등
    title        VARCHAR(200) NOT NULL,
    message      TEXT,
    channel      VARCHAR(20)  NOT NULL DEFAULT 'WEBHOOK',
    sent_status  VARCHAR(10)  NOT NULL DEFAULT 'SENT',  -- SENT/FAILED/SKIPPED
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_alert_log IS '이상 알림 통지 이력(중복억제·감사)';
CREATE INDEX IF NOT EXISTS ix_pmc_alert_log_dedupe ON pmc_alert_log(alert_type, server_id, created_at DESC);
CREATE INDEX IF NOT EXISTS ix_pmc_alert_log_created ON pmc_alert_log(created_at DESC);

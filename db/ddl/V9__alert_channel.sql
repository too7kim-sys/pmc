-- =====================================================================
-- PMC V9 : 알림 채널(다채널 Webhook + 심각도 임계/유형 필터)
-- =====================================================================

CREATE TABLE IF NOT EXISTS pmc_alert_channel (
    channel_id    BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    channel_type  VARCHAR(20)  NOT NULL DEFAULT 'WEBHOOK',   -- WEBHOOK
    url           VARCHAR(1000) NOT NULL,
    min_severity  VARCHAR(10)  NOT NULL DEFAULT 'CRITICAL',  -- WARN(주의 이상) / CRITICAL(위험만)
    alert_types   VARCHAR(200),                              -- NULL=전체, CSV(RUN_CRITICAL,SVC_FAIL,HB_STALE,AUTO_REPORT)
    enabled       CHAR(1)      NOT NULL DEFAULT 'Y',
    reg_user      VARCHAR(40),
    reg_dt        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_alert_channel IS '이상 알림 수신 채널(심각도 임계·유형 필터)';

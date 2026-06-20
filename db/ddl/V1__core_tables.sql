-- =====================================================================
-- PMC V1 : 대상 서버 / Agent 레지스트리 / 원격 명령 큐
-- 대상 DBMS : PostgreSQL 12+
-- 명명규칙   : 앱 테이블 pmc_ 접두사, snake_case, 공통컬럼 표준화
--             (reg_user/reg_dt/upd_user/upd_dt)
-- =====================================================================

-- 점검 대상 서버 인벤토리 -------------------------------------------------
CREATE TABLE IF NOT EXISTS pmc_server (
    server_id     BIGSERIAL    PRIMARY KEY,
    hostname      VARCHAR(255) NOT NULL,
    ip_addr       VARCHAR(45),                 -- IPv4/IPv6
    os_type       VARCHAR(30)  NOT NULL,       -- LINUX/WINDOWS/AIX/HPUX/SOLARIS (공통코드 OS_TYPE)
    os_version    VARCHAR(120),
    dept_code     VARCHAR(30),                 -- 부서코드
    service_name  VARCHAR(200),                -- 담당 서비스명
    location      VARCHAR(200),                -- 설치 위치(IDC/랙)
    use_yn        CHAR(1)      NOT NULL DEFAULT 'Y',
    reg_user      VARCHAR(40),
    reg_dt        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    upd_user      VARCHAR(40),
    upd_dt        TIMESTAMPTZ
);
COMMENT ON TABLE  pmc_server IS '점검 대상 서버 인벤토리';
COMMENT ON COLUMN pmc_server.os_type IS 'LINUX/WINDOWS/AIX/HPUX/SOLARIS';

-- Agent(데몬 인스턴스) 레지스트리 ---------------------------------------
CREATE TABLE IF NOT EXISTS pmc_agent (
    agent_id        UUID         PRIMARY KEY,
    server_id       BIGINT       NOT NULL REFERENCES pmc_server(server_id),
    agent_version   VARCHAR(30),
    api_key_hash    VARCHAR(128) NOT NULL,     -- sha-256(apiKey)
    enroll_token    VARCHAR(64),               -- 1회용 등록 토큰
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/INACTIVE/REVOKED
    policy_id       BIGINT,                    -- 할당 정책
    schedule_cron   VARCHAR(60),               -- 현재 적용된 실행주기(원격제어 반영)
    last_heartbeat  TIMESTAMPTZ,
    last_run_at     TIMESTAMPTZ,
    reg_user        VARCHAR(40),
    reg_dt          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    upd_user        VARCHAR(40),
    upd_dt          TIMESTAMPTZ
);
COMMENT ON TABLE pmc_agent IS '수집 Agent(데몬) 레지스트리';
CREATE INDEX IF NOT EXISTS ix_pmc_agent_server ON pmc_agent(server_id);

-- 원격 명령 큐 (웹서비스 -> Agent) --------------------------------------
CREATE TABLE IF NOT EXISTS pmc_agent_command (
    command_id    BIGSERIAL    PRIMARY KEY,
    agent_id      UUID         NOT NULL REFERENCES pmc_agent(agent_id),
    command_type  VARCHAR(20)  NOT NULL,       -- RUN_NOW/START/STOP/SET_SCHEDULE/UPDATE_CONFIG/UPDATE_POLICY
    params        TEXT,                        -- JSON (분류범위, cron 등)
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING', -- PENDING/SENT/ACKED/DONE/FAILED
    send_count    INT          NOT NULL DEFAULT 0,         -- 전달(재전달) 횟수, 무한 재전달/재실행 방지
    result_msg    TEXT,
    requested_by  VARCHAR(40),
    requested_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    picked_at     TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ
);
COMMENT ON TABLE pmc_agent_command IS '웹서비스에서 Agent로 전달하는 원격 명령 큐';
CREATE INDEX IF NOT EXISTS ix_pmc_cmd_agent_status ON pmc_agent_command(agent_id, status);

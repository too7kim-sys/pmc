-- =====================================================================
-- PMC V4 : 정기점검 계획 및 결과 관리
-- =====================================================================

CREATE TABLE IF NOT EXISTS pmc_inspection_plan (
    plan_id        BIGSERIAL    PRIMARY KEY,
    plan_name      VARCHAR(200) NOT NULL,
    cycle          VARCHAR(12)  NOT NULL,      -- MONTHLY/QUARTERLY/HALF/YEARLY
    period_from    DATE,
    period_to      DATE,
    policy_id      BIGINT       REFERENCES pmc_policy(policy_id),
    planned_date   DATE,                       -- 점검 예정일
    inspector_id   VARCHAR(40),                -- 점검자
    approver_id    VARCHAR(40),                -- 결재자
    status         VARCHAR(15)  NOT NULL DEFAULT 'PLANNED', -- PLANNED/IN_PROGRESS/DONE/OVERDUE
    approve_status VARCHAR(15)  NOT NULL DEFAULT 'NONE',     -- NONE/REQUESTED/APPROVED/REJECTED
    approve_opinion TEXT,
    approve_dt     TIMESTAMPTZ,
    reg_user       VARCHAR(40),
    reg_dt         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    upd_user       VARCHAR(40),
    upd_dt         TIMESTAMPTZ
);
COMMENT ON TABLE pmc_inspection_plan IS '정기점검 계획(헤더)';

CREATE TABLE IF NOT EXISTS pmc_inspection_plan_target (
    plan_target_id BIGSERIAL    PRIMARY KEY,
    plan_id        BIGINT       NOT NULL REFERENCES pmc_inspection_plan(plan_id) ON DELETE CASCADE,
    server_id      BIGINT       NOT NULL REFERENCES pmc_server(server_id),
    result_status  VARCHAR(12)  NOT NULL DEFAULT 'PENDING', -- PENDING/DONE/SKIPPED
    run_id         UUID,                       -- 연계된 점검 실행
    done_dt        TIMESTAMPTZ,
    CONSTRAINT uq_pmc_plan_target UNIQUE (plan_id, server_id)
);
COMMENT ON TABLE pmc_inspection_plan_target IS '정기점검 계획 대상 서버 및 실적';
CREATE INDEX IF NOT EXISTS ix_pmc_plan_target_plan ON pmc_inspection_plan_target(plan_id);

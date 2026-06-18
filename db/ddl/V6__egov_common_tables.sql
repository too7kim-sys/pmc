-- =====================================================================
-- PMC V6 : eGovFrame 공통/인증 (사용자·권한·메뉴·공통코드·로그)
--   ※ eGov rte nexus 차단 환경으로 표준 테이블명을 유지하되 필요한 컬럼만 정의.
-- =====================================================================

-- 사용자(업무사용자) ---------------------------------------------------
CREATE TABLE IF NOT EXISTS comtnemplyrinfo (
    emplyr_id      VARCHAR(20)  PRIMARY KEY,   -- 사용자 ID
    user_nm        VARCHAR(60)  NOT NULL,      -- 성명
    password       VARCHAR(200) NOT NULL,      -- 해시(bcrypt)
    email_adres    VARCHAR(100),
    ofcps_nm       VARCHAR(60),                -- 직위
    dept_code      VARCHAR(30),                -- 부서코드
    emplyr_sttus   VARCHAR(15)  NOT NULL DEFAULT 'P', -- P:정상
    lock_at        CHAR(1)      NOT NULL DEFAULT 'N',
    fail_cnt       INT          NOT NULL DEFAULT 0,
    last_login_dt  TIMESTAMPTZ,
    pwd_chg_dt     TIMESTAMPTZ,
    reg_dt         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE comtnemplyrinfo IS '업무 사용자 정보';

-- 권한(롤) -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comtnauthorinfo (
    author_code    VARCHAR(30)  PRIMARY KEY,   -- ROLE_ADMIN/ROLE_USER
    author_nm      VARCHAR(60)  NOT NULL,
    author_de      TEXT
);
COMMENT ON TABLE comtnauthorinfo IS '권한(롤) 정보';

-- 사용자-권한 매핑 -----------------------------------------------------
CREATE TABLE IF NOT EXISTS comtnempauthor (
    emplyr_id      VARCHAR(20)  NOT NULL REFERENCES comtnemplyrinfo(emplyr_id),
    author_code    VARCHAR(30)  NOT NULL REFERENCES comtnauthorinfo(author_code),
    PRIMARY KEY (emplyr_id, author_code)
);
COMMENT ON TABLE comtnempauthor IS '사용자-권한 매핑';

-- 메뉴 -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comtnmenuinfo (
    menu_no        BIGINT       PRIMARY KEY,
    menu_nm        VARCHAR(60)  NOT NULL,
    menu_url       VARCHAR(200),
    upper_menu_no  BIGINT,
    menu_ordr      INT          NOT NULL DEFAULT 0,
    use_yn         CHAR(1)      NOT NULL DEFAULT 'Y'
);
COMMENT ON TABLE comtnmenuinfo IS 'PMC 메뉴 정보';

-- 권한-메뉴 매핑 -------------------------------------------------------
CREATE TABLE IF NOT EXISTS comtnauthormenu (
    author_code    VARCHAR(30)  NOT NULL REFERENCES comtnauthorinfo(author_code),
    menu_no        BIGINT       NOT NULL REFERENCES comtnmenuinfo(menu_no),
    PRIMARY KEY (author_code, menu_no)
);
COMMENT ON TABLE comtnauthormenu IS '권한별 메뉴 접근 매핑';

-- 공통코드 그룹 --------------------------------------------------------
CREATE TABLE IF NOT EXISTS comtccmmnclcode (
    cl_code        VARCHAR(30)  PRIMARY KEY,   -- 그룹코드
    cl_code_nm     VARCHAR(60)  NOT NULL,
    cl_code_de     TEXT,
    use_yn         CHAR(1)      NOT NULL DEFAULT 'Y'
);
COMMENT ON TABLE comtccmmnclcode IS '공통코드 그룹';

-- 공통코드 상세 --------------------------------------------------------
CREATE TABLE IF NOT EXISTS comtccmmncodedetail (
    cl_code        VARCHAR(30)  NOT NULL REFERENCES comtccmmnclcode(cl_code),
    code           VARCHAR(30)  NOT NULL,
    code_nm        VARCHAR(120) NOT NULL,
    code_de        TEXT,
    sort_ordr      INT          NOT NULL DEFAULT 0,
    use_yn         CHAR(1)      NOT NULL DEFAULT 'Y',
    PRIMARY KEY (cl_code, code)
);
COMMENT ON TABLE comtccmmncodedetail IS '공통코드 상세';

-- 접속 로그 ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS comtnloginlog (
    log_id         BIGSERIAL    PRIMARY KEY,
    emplyr_id      VARCHAR(20),
    log_type       VARCHAR(20),                -- LOGIN/LOGOUT/FAIL
    conect_ip      VARCHAR(45),
    log_dt         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE comtnloginlog IS '접속(로그인) 로그';

-- 감사 로그(관리자 작업) ----------------------------------------------
CREATE TABLE IF NOT EXISTS pmc_audit_log (
    audit_id     BIGSERIAL    PRIMARY KEY,
    actor_id     VARCHAR(40),
    action       VARCHAR(60)  NOT NULL,
    target_type  VARCHAR(40),
    target_id    VARCHAR(60),
    details      TEXT,
    actor_ip     VARCHAR(45),
    reg_dt       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
COMMENT ON TABLE pmc_audit_log IS '관리자 작업 감사 로그';

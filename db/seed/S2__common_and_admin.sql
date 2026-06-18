-- =====================================================================
-- PMC Seed S2 : 공통코드 / 권한 / 메뉴 / 기본 관리자 계정
--   기본 관리자 : admin / pmc1234!  (bcrypt)
-- =====================================================================

-- 권한 ----------------------------------------------------------------
INSERT INTO comtnauthorinfo(author_code, author_nm, author_de) VALUES
  ('ROLE_ADMIN','시스템관리자','전체 관리 권한'),
  ('ROLE_USER','일반사용자','조회/점검/보고서')
ON CONFLICT (author_code) DO NOTHING;

-- 기본 관리자 ---------------------------------------------------------
INSERT INTO comtnemplyrinfo(emplyr_id, user_nm, password, email_adres, ofcps_nm, dept_code, emplyr_sttus)
VALUES ('admin','시스템관리자','$2a$10$HFqqX38mIPrJkVCpEbMuMOcznfpiQU6qHEpZZ6E13YXW6Lg4GMr2W',
        'admin@pmc.go.kr','관리자','SYS','P')
ON CONFLICT (emplyr_id) DO NOTHING;

INSERT INTO comtnempauthor(emplyr_id, author_code) VALUES ('admin','ROLE_ADMIN')
ON CONFLICT DO NOTHING;

-- 메뉴 ----------------------------------------------------------------
INSERT INTO comtnmenuinfo(menu_no, menu_nm, menu_url, upper_menu_no, menu_ordr) VALUES
  (1000,'대시보드','/pmc/dashboard.do',NULL,1),
  (2000,'대상/Agent 관리','/pmc/agent/list.do',NULL,2),
  (2100,'Agent 원격제어','/pmc/agent/control.do',2000,1),
  (3000,'점검 정책','/pmc/policy/list.do',NULL,3),
  (4000,'정기점검 계획','/pmc/plan/list.do',NULL,4),
  (5000,'점검 이력','/pmc/inspection/list.do',NULL,5),
  (6000,'보고서','/pmc/report/list.do',NULL,6),
  (7000,'시스템 관리','/pmc/admin/user.do',NULL,7),
  (7100,'공통코드','/cmm/cmmnCode/list.do',7000,1),
  (7200,'데이터품질','/pmc/admin/dq.do',7000,2)
ON CONFLICT (menu_no) DO NOTHING;

INSERT INTO comtnauthormenu(author_code, menu_no)
SELECT 'ROLE_ADMIN', menu_no FROM comtnmenuinfo ON CONFLICT DO NOTHING;
INSERT INTO comtnauthormenu(author_code, menu_no)
SELECT 'ROLE_USER', menu_no FROM comtnmenuinfo WHERE menu_no IN (1000,5000,6000) ON CONFLICT DO NOTHING;

-- 공통코드 그룹 -------------------------------------------------------
INSERT INTO comtccmmnclcode(cl_code, cl_code_nm) VALUES
  ('CHK_CATEGORY','점검 분류'),
  ('CHK_STATUS','판정 상태'),
  ('CHK_CYCLE','점검 주기'),
  ('OS_TYPE','OS 유형'),
  ('CHK_TYPE','점검 유형'),
  ('REPORT_TYPE','보고서 유형')
ON CONFLICT (cl_code) DO NOTHING;

-- 공통코드 상세 -------------------------------------------------------
INSERT INTO comtccmmncodedetail(cl_code, code, code_nm, sort_ordr) VALUES
  ('CHK_CATEGORY','OS','운영체제/서버',1),
  ('CHK_CATEGORY','WEB','웹서버',2),
  ('CHK_CATEGORY','SVC','웹서비스(가용성)',3),
  ('CHK_CATEGORY','WAS','WAS',4),
  ('CHK_CATEGORY','DB','데이터베이스',5),
  ('CHK_CATEGORY','SW','소프트웨어/보안',6),
  ('CHK_CATEGORY','NW','네트워크',7),
  ('CHK_STATUS','NORMAL','정상',1),
  ('CHK_STATUS','WARN','주의',2),
  ('CHK_STATUS','CRITICAL','위험',3),
  ('CHK_STATUS','ERROR','오류',4),
  ('CHK_STATUS','NA','해당없음',5),
  ('CHK_CYCLE','DAILY','일일',1),
  ('CHK_CYCLE','WEEKLY','주간',2),
  ('CHK_CYCLE','MONTHLY','월간',3),
  ('CHK_CYCLE','QUARTERLY','분기',4),
  ('OS_TYPE','LINUX','Linux',1),
  ('OS_TYPE','WINDOWS','Windows',2),
  ('OS_TYPE','AIX','AIX',3),
  ('OS_TYPE','HPUX','HP-UX',4),
  ('OS_TYPE','SOLARIS','Solaris',5),
  ('CHK_TYPE','AUTO','자동(Agent)',1),
  ('CHK_TYPE','MANUAL','수동(입력)',2),
  ('REPORT_TYPE','PDF','PDF',1),
  ('REPORT_TYPE','XLSX','Excel',2),
  ('REPORT_TYPE','CSV','CSV',3)
ON CONFLICT (cl_code, code) DO NOTHING;

-- 데이터 품질 룰 예시 -------------------------------------------------
INSERT INTO pmc_dq_rule(rule_name, target_table, rule_type, check_sql, severity) VALUES
  ('임계치 누락 점검항목(AUTO)','pmc_policy_item','CUSTOM',
   'SELECT count(*) FROM pmc_policy_item i WHERE i.check_type=''AUTO'' AND i.collect_yn=''Y'' AND NOT EXISTS (SELECT 1 FROM pmc_policy_threshold t WHERE t.policy_item_id=i.policy_item_id)','WARN'),
  ('고아 결과 항목(run 없음)','pmc_inspection_result_item','REFERENTIAL',
   'SELECT count(*) FROM pmc_inspection_result_item r WHERE NOT EXISTS (SELECT 1 FROM pmc_inspection_run x WHERE x.run_id=r.run_id)','CRITICAL'),
  ('서버 OS유형 코드 무결성','pmc_server','CODE',
   'SELECT count(*) FROM pmc_server s WHERE s.os_type NOT IN (SELECT code FROM comtccmmncodedetail WHERE cl_code=''OS_TYPE'')','WARN')
ON CONFLICT DO NOTHING;

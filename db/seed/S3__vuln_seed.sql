-- =====================================================================
-- PMC S3 : 취약점 진단(SEC) 시드 — 공통코드 · 메뉴/권한 · SEC 점검항목
--   (멱등: ON CONFLICT DO NOTHING. S2/S1 이후 적용)
-- =====================================================================

-- 점검 분류에 SEC 추가
INSERT INTO comtccmmncodedetail(cl_code, code, code_nm, sort_ordr) VALUES
  ('CHK_CATEGORY','SEC','보안취약점/구성진단',8)
ON CONFLICT (cl_code, code) DO NOTHING;

-- 취약점 상태/등급/알림유형 공통코드 그룹
INSERT INTO comtccmmnclcode(cl_code, cl_code_nm) VALUES
  ('VULN_STATUS','취약점 상태'),
  ('VULN_SEVERITY','취약점 등급'),
  ('ALERT_TYPE','알림 유형')
ON CONFLICT (cl_code) DO NOTHING;

INSERT INTO comtccmmncodedetail(cl_code, code, code_nm, sort_ordr) VALUES
  ('VULN_STATUS','OPEN','미조치',1),
  ('VULN_STATUS','FIXED','조치완료',2),
  ('VULN_STATUS','RECURRED','재발',3),
  ('VULN_STATUS','EXEMPTED','예외(수용/면제)',4),
  ('VULN_SEVERITY','상','상(높음)',1),
  ('VULN_SEVERITY','중','중(보통)',2),
  ('VULN_SEVERITY','하','하(낮음)',3),
  ('ALERT_TYPE','RUN_CRITICAL','점검 위험',1),
  ('ALERT_TYPE','SVC_FAIL','웹서비스 실패',2),
  ('ALERT_TYPE','HB_STALE','heartbeat 누락',3),
  ('ALERT_TYPE','AUTO_REPORT','정기 보고서',4),
  ('ALERT_TYPE','VULN_NEW','취약점 신규',5),
  ('ALERT_TYPE','VULN_RECUR','취약점 재발',6)
ON CONFLICT (cl_code, code) DO NOTHING;

-- 메뉴 + 역할별 권한(취약점진단: ADMIN/USER 목록, 가져오기: ADMIN 전용)
INSERT INTO comtnmenuinfo(menu_no, menu_nm, menu_url, upper_menu_no, menu_ordr) VALUES
  (8000,'취약점진단','/pmc/vuln/list.do',NULL,8),
  (8100,'외부 스캐너 가져오기','/pmc/admin/vulnImport.do',8000,1)
ON CONFLICT (menu_no) DO NOTHING;

INSERT INTO comtnauthormenu(author_code, menu_no)
SELECT 'ROLE_ADMIN', menu_no FROM comtnmenuinfo WHERE menu_no IN (8000,8100) ON CONFLICT DO NOTHING;
INSERT INTO comtnauthormenu(author_code, menu_no)
SELECT 'ROLE_USER', menu_no FROM comtnmenuinfo WHERE menu_no IN (8000) ON CONFLICT DO NOTHING;

-- 기본 정책에 SEC 점검항목 적재(정책 UI 노출·isItemEnabled 토글). 임계치 없음(취약/양호 직접 판정).
INSERT INTO pmc_policy_item(policy_id, category, mid_category, item_code, item_name, unit, check_cycle, check_method, check_type, judge_criteria, sort_order)
SELECT p.policy_id, v.category, v.mid_category, v.item_code, v.item_name, v.unit, v.check_cycle, v.check_method, v.check_type, v.judge_criteria, v.sort_order
FROM pmc_policy p
CROSS JOIN (VALUES
  -- 리눅스(KISA U-코드)
  ('SEC','계정','SEC_U01_ROOT_REMOTE','root 원격 접속 제한','','MONTHLY','sshd_config PermitRootLogin','AUTO','no=양호(상)',101),
  ('SEC','계정','SEC_U02_PW_COMPLEX','패스워드 복잡도','','MONTHLY','pwquality/pam','AUTO','복잡도 적용=양호(상)',102),
  ('SEC','계정','SEC_U04_PW_MAX_DAYS','패스워드 최대 사용기간','일','MONTHLY','login.defs PASS_MAX_DAYS','AUTO','≤90=양호(중)',103),
  ('SEC','파일','SEC_U05_PASSWD_PERM','/etc/passwd 권한','','MONTHLY','파일권한','AUTO','644 이하=양호(상)',104),
  ('SEC','파일','SEC_U06_SHADOW_PERM','/etc/shadow 권한','','MONTHLY','파일권한','AUTO','그룹/기타 권한 없음=양호(상)',105),
  ('SEC','계정','SEC_U07_EMPTY_PW','빈 패스워드 계정','','MONTHLY','shadow 스캔','AUTO','없음=양호(상)',106),
  ('SEC','파일','SEC_U08_UMASK','umask 설정','','QUARTERLY','login.defs/profile','AUTO','022/027=양호(하)',107),
  ('SEC','서비스','SEC_U09_UNNEEDED_SVC','불필요 서비스','','MONTHLY','systemctl is-enabled','AUTO','telnet/rsh/ftp 비활성=양호(중)',108),
  ('SEC','파일','SEC_U10_CRON_PERM','cron 설정 권한','','QUARTERLY','파일권한','AUTO','640 이하=양호(중)',109),
  ('SEC','로그','SEC_U11_LOG_PERM','주요 로그 권한','','QUARTERLY','파일권한','AUTO','기타 권한 없음=양호(하)',110),
  -- 윈도우
  ('SEC','계정','SEC_W01_PW_MINLEN','최소 암호 길이(Win)','','MONTHLY','net accounts','AUTO','≥8=양호(상)',121),
  ('SEC','계정','SEC_W02_LOCKOUT','계정 잠금 임계값(Win)','','MONTHLY','net accounts','AUTO','1~5=양호(중)',122),
  ('SEC','계정','SEC_W03_GUEST','Guest 계정 비활성(Win)','','MONTHLY','net user guest','AUTO','비활성=양호(중)',123),
  ('SEC','감사','SEC_W04_AUDIT','감사 정책(Win)','','MONTHLY','auditpol','AUTO','감사 활성=양호(중)',124),
  -- AIX
  ('SEC','계정','SEC_A01_PW_MINLEN','최소 암호 길이(AIX)','','MONTHLY','/etc/security/user minlen','AUTO','≥8=양호(상)',131),
  ('SEC','계정','SEC_A02_PW_MAXAGE','패스워드 최대기간(AIX)','주','MONTHLY','maxage','AUTO','≤13주=양호(중)',132),
  ('SEC','계정','SEC_A03_LOGIN_RETRIES','로그인 실패 임계(AIX)','','MONTHLY','loginretries','AUTO','1~5=양호(중)',133),
  ('SEC','파일','SEC_A04_PASSWD_PERM','/etc/passwd 권한(AIX)','','MONTHLY','파일권한','AUTO','644 이하=양호(상)',134),
  -- HP-UX
  ('SEC','계정','SEC_H01_PW_MINLEN','최소 암호 길이(HP-UX)','','MONTHLY','/etc/default/security','AUTO','≥8=양호(상)',141),
  ('SEC','계정','SEC_H02_PW_MAXDAYS','패스워드 최대기간(HP-UX)','일','MONTHLY','PASSWORD_MAXDAYS','AUTO','≤90=양호(중)',142),
  ('SEC','파일','SEC_H03_PASSWD_PERM','/etc/passwd 권한(HP-UX)','','MONTHLY','파일권한','AUTO','644 이하=양호(상)',143)
) AS v(category, mid_category, item_code, item_name, unit, check_cycle, check_method, check_type, judge_criteria, sort_order)
WHERE p.policy_name='기본 점검 정책'
ON CONFLICT (policy_id, item_code) DO NOTHING;

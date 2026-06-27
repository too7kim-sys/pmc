-- =====================================================================
-- PMC S4 : 메뉴 정합화 + 권한별 접근 매핑(데이터 구동형 GNB 근거)
--   실제 화면과 1:1로 comtnmenuinfo 를 upsert 하고, ROLE_ADMIN/ROLE_USER 접근을 부여.
--   (멱등: 메뉴는 ON CONFLICT DO UPDATE, 권한매핑은 ON CONFLICT DO NOTHING)
--   ※ url 이 있는 메뉴만 GNB 에 렌더된다(7000 시스템관리는 그룹 라벨이라 url 없음).
-- =====================================================================

INSERT INTO comtnmenuinfo(menu_no, menu_nm, menu_url, upper_menu_no, menu_ordr, use_yn) VALUES
  -- 운영(일반 사용자 + 관리자)
  (1000,'대시보드',      '/pmc/dashboard.do',            NULL, 10,'Y'),
  (2000,'대상/Agent',    '/pmc/agent/list.do',           NULL, 20,'Y'),
  (2100,'Agent 원격제어','/pmc/agent/control.do',        2000, 21,'Y'),
  (3000,'점검정책',      '/pmc/policy/list.do',          NULL, 30,'Y'),
  (4000,'정기점검계획',  '/pmc/plan/list.do',            NULL, 40,'Y'),
  (5000,'점검이력',      '/pmc/inspection/list.do',      NULL, 50,'Y'),
  (5500,'모니터링',      '/pmc/monitoring/realtime.do',  NULL, 60,'Y'),
  (5600,'문제가능성',    '/pmc/risk/list.do',            NULL, 70,'Y'),
  (8000,'취약점진단',    '/pmc/vuln/list.do',            NULL, 80,'Y'),
  (8100,'취약점 가져오기','/pmc/admin/vulnImport.do',    8000, 81,'Y'),
  (6000,'보고서',        '/pmc/report/list.do',          NULL, 90,'Y'),
  -- 시스템 관리(관리자 전용 그룹)
  (7000,'시스템관리',    NULL,                           NULL,100,'Y'),
  (7100,'공통코드',      '/cmm/cmmnCode/list.do',        7000,101,'Y'),
  (7300,'메뉴관리',      '/pmc/admin/menu.do',           7000,102,'Y'),
  (7400,'권한관리',      '/pmc/admin/authority.do',      7000,103,'Y'),
  (7500,'사용자',        '/pmc/admin/user.do',           7000,104,'Y'),
  (7200,'데이터품질',    '/pmc/admin/dq.do',             7000,105,'Y'),
  (7600,'알림채널',      '/pmc/admin/alertChannel.do',   7000,106,'Y'),
  (7700,'감사로그',      '/pmc/admin/auditLog.do',       7000,107,'Y')
ON CONFLICT (menu_no) DO UPDATE
  SET menu_nm=EXCLUDED.menu_nm, menu_url=EXCLUDED.menu_url,
      upper_menu_no=EXCLUDED.upper_menu_no, menu_ordr=EXCLUDED.menu_ordr, use_yn=EXCLUDED.use_yn;

-- 관리자: 전체 메뉴 접근
INSERT INTO comtnauthormenu(author_code, menu_no)
SELECT 'ROLE_ADMIN', menu_no FROM comtnmenuinfo
ON CONFLICT DO NOTHING;

-- 일반 사용자: 운영 메뉴만(정책=ADMIN, 시스템관리/취약점가져오기 제외)
INSERT INTO comtnauthormenu(author_code, menu_no)
SELECT 'ROLE_USER', menu_no FROM comtnmenuinfo
 WHERE menu_no IN (1000,2000,2100,4000,5000,5500,5600,8000,6000)
ON CONFLICT DO NOTHING;

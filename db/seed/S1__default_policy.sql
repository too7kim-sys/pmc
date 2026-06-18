-- =====================================================================
-- PMC Seed S1 : 기본 점검 정책 + 점검항목(행정안전부 매뉴얼 준거)
-- =====================================================================

INSERT INTO pmc_policy(policy_name, description, version, schedule_cron, use_yn, reg_user)
SELECT '기본 점검 정책', '행정안전부 정보시스템 장애예방 점검매뉴얼 준거 기본 정책', 1, '0 0 6 * * *', 'Y', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM pmc_policy WHERE policy_name='기본 점검 정책');

-- 점검항목 (category, mid, code, name, unit, cycle, type, judge) ---------
INSERT INTO pmc_policy_item(policy_id, category, mid_category, item_code, item_name, unit, check_cycle, check_method, check_type, judge_criteria, sort_order)
SELECT p.policy_id, v.category, v.mid_category, v.item_code, v.item_name, v.unit, v.check_cycle, v.check_method, v.check_type, v.judge_criteria, v.sort_order
FROM pmc_policy p
CROSS JOIN (VALUES
  -- OS / 서버
  ('OS','자원','OS_CPU_USAGE','CPU 사용률','%','DAILY','top/proc 기반 사용률','AUTO','주의 80, 위험 90',1),
  ('OS','자원','OS_MEM_USAGE','메모리 사용률','%','DAILY','free/proc 기반','AUTO','주의 80, 위험 90',2),
  ('OS','자원','OS_SWAP_USAGE','스왑 사용률','%','DAILY','free 기반','AUTO','주의 30, 위험 60',3),
  ('OS','자원','OS_DISK_USAGE','디스크 사용률','%','DAILY','df -P 최대 파티션','AUTO','주의 80, 위험 90',4),
  ('OS','자원','OS_INODE_USAGE','inode 사용률','%','WEEKLY','df -i','AUTO','주의 80, 위험 90',5),
  ('OS','자원','OS_LOAD_AVG','부하(load avg 1m)','','DAILY','uptime','AUTO','코어수 대비',6),
  ('OS','상태','OS_UPTIME','가동시간','days','DAILY','uptime','AUTO','참고용',7),
  ('OS','프로세스','OS_PROC_ZOMBIE','좀비 프로세스 수','count','DAILY','ps 집계','AUTO','주의 1 이상',8),
  ('OS','프로세스','OS_KEY_PROC_ALIVE','핵심 프로세스 생존','','DAILY','ps grep','AUTO','정상=UP',9),
  ('OS','시간','OS_NTP_SYNC','시각 동기화','','DAILY','timedatectl','AUTO','synchronized=정상',10),
  ('OS','보안','OS_PATCH_LEVEL','커널/패치 수준','','MONTHLY','uname -r','AUTO','참고용',11),
  ('OS','계정','OS_LOGIN_USERS','로그인 사용자 수','count','DAILY','who','AUTO','참고용',12),
  ('OS','로그','OS_SYSLOG_ERROR','시스템 로그 오류건수','count','DAILY','journal/syslog 스캔','AUTO','주의 1 이상',13),
  -- WEB (웹서버)
  ('WEB','상태','WEB_PROC_ALIVE','웹서버 프로세스 생존','','DAILY','ps grep httpd/nginx','AUTO','정상=UP, 미설치=NA',20),
  ('WEB','버전','WEB_VERSION','웹서버 버전','','MONTHLY','-v','AUTO','참고용',21),
  ('WEB','포트','WEB_PORT_LISTEN','웹 포트 LISTEN','','DAILY','ss/netstat 80/443','AUTO','정상=LISTEN',22),
  ('WEB','보안','WEB_SSL_CERT_EXPIRY','SSL 인증서 만료(잔여일)','days','WEEKLY','openssl x509','AUTO','주의 30, 위험 7',23),
  ('WEB','로그','WEB_ERRORLOG_SCAN','에러로그 스캔','count','DAILY','error_log','AUTO','주의 1 이상',24),
  ('WEB','연결','WEB_CONN_COUNT','연결 수','count','DAILY','ss 집계','AUTO','참고용',25),
  -- SVC (웹서비스 가용성)
  ('SVC','가용성','SVC_URL_STATUS','HTTP 상태코드','','DAILY','HTTP 요청','AUTO','기대코드 불일치=위험',30),
  ('SVC','성능','SVC_RESPONSE_TIME','응답시간','ms','DAILY','HTTP 요청','AUTO','주의 3000, 위험 5000',31),
  ('SVC','정합성','SVC_CONTENT_MATCH','본문 키워드 검증','','DAILY','응답 본문','AUTO','불일치=위험',32),
  ('SVC','보안','SVC_SSL_EXPIRY','서비스 SSL 만료(잔여일)','days','WEEKLY','TLS 핸드셰이크','AUTO','주의 30, 위험 7',33),
  -- WAS
  ('WAS','상태','WAS_PROC_ALIVE','WAS 프로세스 생존','','DAILY','ps grep','AUTO','정상=UP, 미설치=NA',40),
  ('WAS','버전','WAS_VERSION','WAS 버전','','MONTHLY','version','AUTO','참고용',41),
  ('WAS','자원','WAS_HEAP_USAGE','JVM Heap 사용률','%','DAILY','jstat/jmx','AUTO','주의 80, 위험 90',42),
  ('WAS','자원','WAS_THREAD_USAGE','스레드 사용률','%','DAILY','jmx','AUTO','주의 80, 위험 90',43),
  ('WAS','자원','WAS_DATASOURCE_POOL','DataSource 풀 사용률','%','DAILY','jmx','AUTO','주의 80, 위험 90',44),
  ('WAS','배포','WAS_DEPLOYED_APP','배포 앱 상태','','WEEKLY','manager','AUTO','참고용',45),
  ('WAS','포트','WAS_PORT_LISTEN','WAS 포트 LISTEN','','DAILY','ss','AUTO','정상=LISTEN',46),
  ('WAS','GC','WAS_GC_STATUS','GC 상태','','DAILY','gc log','AUTO','참고용',47),
  -- DB
  ('DB','상태','DB_PROC_ALIVE','DB 프로세스 생존','','DAILY','ps grep','AUTO','정상=UP, 미설치=NA',50),
  ('DB','버전','DB_VERSION','DB 버전','','MONTHLY','version','AUTO','참고용',51),
  ('DB','포트','DB_LISTENER_PORT','리스너 포트 LISTEN','','DAILY','ss 5432/1521','AUTO','정상=LISTEN',52),
  ('DB','세션','DB_SESSION_COUNT','세션/연결 수','count','DAILY','쿼리','AUTO','한도 대비',53),
  ('DB','용량','DB_TABLESPACE_USAGE','테이블스페이스 사용률','%','DAILY','쿼리','AUTO','주의 80, 위험 90',54),
  ('DB','용량','DB_ARCHIVE_USAGE','아카이브 영역 사용률','%','DAILY','쿼리','AUTO','주의 80, 위험 90',55),
  ('DB','성능','DB_LOCK_WAIT','락 대기 수','count','DAILY','쿼리','AUTO','주의 1 이상',56),
  ('DB','백업','DB_BACKUP_STATUS','백업 정상 여부','','DAILY','백업 로그/매체 확인','MANUAL','정상 수행=정상',57),
  ('DB','이중화','DB_REPLICATION_LAG','복제 지연','sec','DAILY','쿼리','AUTO','주의 30, 위험 120',58),
  -- SW / 보안
  ('SW','자산','SW_PKG_INVENTORY','설치 패키지 수','count','MONTHLY','pkg manager','AUTO','참고용',60),
  ('SW','보안','SW_ANTIVIRUS_ALIVE','백신 동작','','DAILY','프로세스','AUTO','정상=UP',61),
  ('SW','보안','SW_ANTIVIRUS_PATTERN_DATE','백신 패턴 일자(경과일)','days','DAILY','패턴 파일','AUTO','주의 3, 위험 7',62),
  ('SW','보안','SW_SECURITY_AGENT_ALIVE','보안 에이전트 동작','','DAILY','프로세스','AUTO','정상=UP',63),
  ('SW','자산','SW_LICENSE_EXPIRY','라이선스 만료(잔여일)','days','MONTHLY','라이선스 확인','MANUAL','주의 30, 위험 7',64),
  -- NW
  ('NW','인터페이스','NW_INTERFACE_STATUS','인터페이스 상태','','DAILY','ip link','AUTO','UP=정상',70),
  ('NW','라우팅','NW_IP_ROUTE','기본 라우트','','DAILY','ip route','AUTO','존재=정상',71),
  ('NW','포트','NW_PORT_LISTEN','LISTEN 포트 수','count','DAILY','ss -ltn','AUTO','참고용',72),
  ('NW','보안','NW_FIREWALL_STATUS','방화벽 상태','','DAILY','firewalld/iptables','AUTO','active=정상',73),
  ('NW','DNS','NW_DNS_RESOLVE','DNS 조회','','DAILY','nslookup','AUTO','성공=정상',74),
  ('NW','연결','NW_CONNECTIVITY_CHECK','대상 연결성(ping)','','DAILY','ping','AUTO','성공=정상',75),
  ('NW','이중화','NW_NIC_BONDING','NIC 본딩 상태','','WEEKLY','/proc/net/bonding','AUTO','참고용',76),
  ('NW','트래픽','NW_TRAFFIC_RATE','트래픽 사용률','%','DAILY','지표','AUTO','주의 70, 위험 90',77)
) AS v(category, mid_category, item_code, item_name, unit, check_cycle, check_method, check_type, judge_criteria, sort_order)
WHERE p.policy_name='기본 점검 정책'
  AND NOT EXISTS (SELECT 1 FROM pmc_policy_item i WHERE i.policy_id=p.policy_id AND i.item_code=v.item_code);

-- 임계치 (수치 항목 warn/critical) -------------------------------------
INSERT INTO pmc_policy_threshold(policy_item_id, level, operator, compare_value)
SELECT i.policy_item_id, t.level, t.operator, t.cv
FROM pmc_policy_item i
JOIN pmc_policy p ON p.policy_id=i.policy_id AND p.policy_name='기본 점검 정책'
JOIN (VALUES
  ('OS_CPU_USAGE','WARN','GTE','80'),('OS_CPU_USAGE','CRITICAL','GTE','90'),
  ('OS_MEM_USAGE','WARN','GTE','80'),('OS_MEM_USAGE','CRITICAL','GTE','90'),
  ('OS_SWAP_USAGE','WARN','GTE','30'),('OS_SWAP_USAGE','CRITICAL','GTE','60'),
  ('OS_DISK_USAGE','WARN','GTE','80'),('OS_DISK_USAGE','CRITICAL','GTE','90'),
  ('OS_INODE_USAGE','WARN','GTE','80'),('OS_INODE_USAGE','CRITICAL','GTE','90'),
  ('OS_PROC_ZOMBIE','WARN','GTE','1'),
  ('OS_SYSLOG_ERROR','WARN','GTE','1'),
  ('WEB_SSL_CERT_EXPIRY','WARN','LTE','30'),('WEB_SSL_CERT_EXPIRY','CRITICAL','LTE','7'),
  ('WEB_ERRORLOG_SCAN','WARN','GTE','1'),
  ('SVC_RESPONSE_TIME','WARN','GTE','3000'),('SVC_RESPONSE_TIME','CRITICAL','GTE','5000'),
  ('SVC_SSL_EXPIRY','WARN','LTE','30'),('SVC_SSL_EXPIRY','CRITICAL','LTE','7'),
  ('WAS_HEAP_USAGE','WARN','GTE','80'),('WAS_HEAP_USAGE','CRITICAL','GTE','90'),
  ('WAS_THREAD_USAGE','WARN','GTE','80'),('WAS_THREAD_USAGE','CRITICAL','GTE','90'),
  ('WAS_DATASOURCE_POOL','WARN','GTE','80'),('WAS_DATASOURCE_POOL','CRITICAL','GTE','90'),
  ('DB_TABLESPACE_USAGE','WARN','GTE','80'),('DB_TABLESPACE_USAGE','CRITICAL','GTE','90'),
  ('DB_ARCHIVE_USAGE','WARN','GTE','80'),('DB_ARCHIVE_USAGE','CRITICAL','GTE','90'),
  ('DB_LOCK_WAIT','WARN','GTE','1'),
  ('DB_REPLICATION_LAG','WARN','GTE','30'),('DB_REPLICATION_LAG','CRITICAL','GTE','120'),
  ('SW_ANTIVIRUS_PATTERN_DATE','WARN','GTE','3'),('SW_ANTIVIRUS_PATTERN_DATE','CRITICAL','GTE','7'),
  ('SW_LICENSE_EXPIRY','WARN','LTE','30'),('SW_LICENSE_EXPIRY','CRITICAL','LTE','7'),
  ('NW_TRAFFIC_RATE','WARN','GTE','70'),('NW_TRAFFIC_RATE','CRITICAL','GTE','90')
) AS t(item_code, level, operator, cv) ON t.item_code=i.item_code
WHERE NOT EXISTS (
  SELECT 1 FROM pmc_policy_threshold x WHERE x.policy_item_id=i.policy_item_id AND x.level=t.level
);

-- 웹서비스 점검 대상 예시 ---------------------------------------------
INSERT INTO pmc_policy_svc_target(policy_id, svc_name, url, expected_status, expected_content)
SELECT p.policy_id, 'PMC 포털', 'http://localhost:8080/pmc/login.do', 200, '로그인'
FROM pmc_policy p
WHERE p.policy_name='기본 점검 정책'
  AND NOT EXISTS (SELECT 1 FROM pmc_policy_svc_target s WHERE s.policy_id=p.policy_id AND s.svc_name='PMC 포털');

# PMC — 장애예방점검 자동화 시스템

정보시스템 **장애예방 점검을 자동화**하여 수작업을 줄이고 점검 오류를 제로화하는 시스템.
각 대상 서버의 **수집 데몬(Agent)** 이 OS·WEB·WAS·DB·SW·NW 및 **웹서비스(SVC) 가용성**을
자동 수집하여 중앙 **수집/정책 웹서비스(eGovFrame)** 로 전송하고, 임계치 판정 후
표준 점검 보고서(웹 대시보드 / PDF / Excel / CSV)를 자동 생성한다.

점검 항목·점검주기·판정기준은 **행정안전부 「정보시스템 장애예방 점검매뉴얼」**
및 **「정보시스템 안정성 기준」** 체계(대분류·중분류 / 일일·주간·월간·분기 / 자동·수동 점검)에 준한다.

## 구성 (2개 프로젝트)

```
pmc/
├── pmc-agent/    수집 데몬 (표준 Java, 단일 실행 jar) — Linux/Windows/Unix(AIX·HP-UX)
├── pmc-server/   eGovFrame 기반 수집/정책 웹서비스 (Spring 5 + MyBatis + PostgreSQL)
├── db/ddl/       PostgreSQL 스키마 V1~V7  +  db/seed/ 기본 정책·공통코드·관리자
└── docs/         결과 JSON 스키마, API 계약
```

### 주요 기능
- **수집 Agent**: 플랫폼×분류 수집기 추상화, 로컬 임계치 평가, 정책 풀, 전송 재시도 + 로컬 스풀,
  스케줄러(주기 변경), 원격 명령 처리. (`--once` / `--daemon`)
- **웹서비스**: 대시보드, 대상/Agent 관리, **원격 제어(실행·종료·추가점검·실행주기·정책갱신)**,
  점검 정책·항목·임계치·웹서비스 점검대상 관리, **정기점검 계획·결과 관리(계획→실적/이행률→결재→보고서)**,
  점검 이력, 보고서(PDF/Excel/CSV), 공통기반(로그인·권한·메뉴·공통코드·데이터품질·사용자연계).
- **인증 2평면**: Agent API(무상태 토큰, `sha-256` 키 해시) / 웹 UI(세션 로그인, 역할 권한).
- **보안**: 명령 allowlist(주입 방지), gzip 요청 처리, run_id 멱등, 시큐어코딩(MyBatis 바인딩·CSRF·접근통제).

## 빌드 & 실행

요구: JDK 11(서버, eGovFrame 4.x 호환), JDK 8+(Agent 빌드/실행), Maven 3.9, PostgreSQL 12+.

```bash
# 1) DB 준비 — 편의 스크립트(PG 기동 + 롤/DB 생성 + DDL/시드 멱등 적용)
bash scripts/db-up.sh    # 앱(IDE/Tomcat·Jetty) 구동 "전에" 한 번 실행. 세션마다 PG가 내려가면 재실행.
# (수동으로 하려면 ↓ — DDL 은 버전 숫자순으로. 문자열 정렬은 V10 을 V1 앞에 두므로 sort -V 필수)
# createdb pmc   # role pmc / pwd pmc (globals.properties 참고)
# for f in $(ls db/ddl/V*.sql | sort -V) db/seed/S2*.sql db/seed/S1*.sql db/seed/S3*.sql db/seed/S4*.sql; do psql -U pmc -d pmc -f "$f"; done
# ※ 테스트(mvn test)는 인메모리 H2 사용 → DB 준비 불필요. 앱 구동만 PostgreSQL 사용.

# 2) 서버 빌드/실행 (Java 11)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
mvn -f pmc-server/pom.xml -DskipTests package          # → pmc-server/target/pmc.war
mvn -f pmc-server/pom.xml org.eclipse.jetty:jetty-maven-plugin:9.4.53.v20231009:run-war
#   → http://localhost:8080/  (기본 관리자 admin / pmc1234!)
#   운영 배포 시 Tomcat 9.x 에 pmc.war 배포

# 3) Agent 빌드/실행 (Java 8 타깃)
mvn -f pmc-agent/pom.xml package                        # → pmc-agent/target/pmc-agent-1.0.0.jar
java -jar pmc-agent/target/pmc-agent-1.0.0.jar --config conf/agent.yml --once
```

웹 UI에서 대상 서버 등록 → 등록토큰 발급 → `agent.yml`의 `enrollToken`에 입력 후 Agent 실행하면
자동 등록·수집·전송된다. 설치 스크립트(systemd / Windows procrun / SysV init.d)는
`pmc-agent/src/dist/` 참고.

## 환경 메모 (eGovFrame 의존성)
eGov 전용 nexus(maven.egovframe.go.kr)가 폐쇄망/차단된 환경을 고려하여, eGovFrame **표준 구조·규약**
(`egovframework.com`/`egovframework.let.pmc.*` 패키지, MyBatis `*_SQL_postgresql.xml`, JSP 뷰,
서비스/DAO 계층, `globals.properties`, web.xml + Spring XML 컨텍스트)은 그대로 따르되, eGov rte 전용
jar 대신 Maven Central의 **Spring 5 / Spring Security / MyBatis** 를 직접 사용한다. eGov rte 사용이
가능한 망에서는 `egovframework.com`의 경량 베이스를 eGov rte 의존성으로 교체하면 된다.

## 검증(E2E)
`docs/` 의 API 계약/스키마 기준으로 register→policy pull→collect→push→대시보드→보고서→
원격명령→정기점검계획까지 로컬 검증 완료. 자세한 절차는 개발 계획서 참조.

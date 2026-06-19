# 이클립스 개발환경 설정 가이드 (PMC)

PMC(장애예방점검 자동화 시스템)를 **이클립스에서 가져와 빌드·실행**하는 절차입니다.
**전자정부 표준프레임워크(eGovFrame) IDE** 와 **일반 Eclipse(Enterprise Java)** 를 모두 다룹니다.

PMC는 Maven 멀티모듈 프로젝트입니다.

| 모듈 | 패키징 | Java | 산출물 |
|---|---|---|---|
| `pmc-parent` (루트) | pom | — | 애그리게이터 |
| `pmc-server` | war | **11** (eGovFrame 4.x 호환) | `target/pmc.war` |
| `pmc-agent` | jar | **8** (크로스플랫폼 타깃) | `target/pmc-agent-1.0.0.jar` |

> ⚠️ 모듈마다 **컴파일 대상 JDK가 다릅니다**(server=11, agent=8). 각 pom의
> `maven.compiler.release` 에 정의되어 있으며, 이클립스 JRE 설정에서 두 버전을 모두 등록해야 합니다.

---

## 1. 사전 요구사항

| 항목 | 버전 | 비고 |
|---|---|---|
| JDK 11 | 11.x | `pmc-server` 빌드/실행. eGovFrame 4.x(`javax.servlet`) 호환 |
| JDK 8 | 8.x | `pmc-agent` 컴파일 타깃(JDK 11로도 빌드 가능하나 release=8 준수 권장) |
| Maven | 3.9+ | 이클립스 내장 m2e 사용 시 별도 설치 불필요 |
| PostgreSQL | 12+ | 기본 DB `pmc` / 계정 `pmc` / 비밀번호 `pmc` |
| Tomcat | **9.x** | 운영/서버 실행용. ⚠️ **Tomcat 10+ 불가**(jakarta 네임스페이스) |

### IDE 선택

- **전자정부 표준프레임워크 IDE**: 공공 프로젝트 표준 배포판(Eclipse 기반, m2e·Tomcat·DB 도구 내장).
  [표준프레임워크 포털](https://www.egovframe.go.kr) 에서 "개발환경(Implementation Tool)" 다운로드.
- **일반 Eclipse**: *Eclipse IDE for Enterprise Java and Web Developers* (m2e·WTP 기본 포함).

> 본 프로젝트는 eGov 전용 nexus(maven.egovframe.go.kr) 의존성을 쓰지 않고 **Maven Central** 의
> Spring 5 / Spring Security / MyBatis 를 직접 사용하므로, **일반 Eclipse 로도 그대로 동작**합니다.

---

## 2. 프로젝트 가져오기 (Import) — 두 IDE 공통

1. 소스 클론
   ```bash
   git clone <repo-url> pmc
   ```
2. 이클립스: `File > Import... > Maven > Existing Maven Projects` 선택 후 **Next**.
3. **Root Directory** 에 클론한 `pmc` 폴더 지정 → `pom.xml` 3개(루트/agent/server)가 트리에 표시됨 →
   모두 체크 후 **Finish**.
4. m2e 가 의존성을 내려받습니다(최초 수 분 소요). `pmc-parent`, `pmc-agent`, `pmc-server` 가
   워크스페이스에 등록됩니다.

> 가져오기 후 빨간 에러가 보이면 대개 **JRE 미설정**(3장) 또는 **의존성 미수신**(`Maven > Update
> Project`, Alt+F5)이 원인입니다.

---

## 3. JDK · 인코딩 설정 (가장 중요)

### 3.1 JDK 등록

`Window > Preferences > Java > Installed JREs > Add...` 로 **JDK 11 과 JDK 8 을 모두** 등록합니다
(JRE 가 아닌 **JDK** 홈을 지정).

이어서 `Installed JREs > Execution Environments` 에서 매핑을 확인합니다.

| Execution Environment | 대상 JDK |
|---|---|
| `JavaSE-11` | JDK 11 |
| `JavaSE-1.8` | JDK 8 |

### 3.2 컴파일러 레벨

각 모듈의 `maven.compiler.release` 가 m2e 에 자동 반영됩니다.

- `pmc-server/pom.xml` → `<maven.compiler.release>11</maven.compiler.release>`
- `pmc-agent/pom.xml` → `<maven.compiler.release>8</maven.compiler.release>`

`프로젝트 우클릭 > Properties > Java Compiler` 에서 server=11, agent=1.8 로 표시되는지 확인합니다.
다르면 `Maven > Update Project` 로 동기화합니다.

### 3.3 인코딩 (한글 깨짐 방지) — UTF-8 고정

`Window > Preferences` 에서:

- `General > Workspace > Text file encoding` → **UTF-8**
- `Web > JSP Files > Encoding` → **UTF-8**
- `General > Content Types > Text > Java Properties File` → **UTF-8** (globals.properties 등)

> 프로젝트 전체가 `project.build.sourceEncoding=UTF-8` 로 빌드되므로 워크스페이스도 UTF-8 로 맞춥니다.

---

## 4. Maven 빌드

- **전체 빌드**: 루트 `pmc` 우클릭 > `Run As > Maven install`
  (또는 goals 에 `clean package`).
- **모듈별 빌드**:
  - `pmc-server` → `target/pmc.war`
  - `pmc-agent` → `target/pmc-agent-1.0.0.jar` (maven-shade 로 의존성 포함 단일 실행 jar)
- m2e 의 *lifecycle mapping* 경고(maven-shade-plugin / maven-war-plugin)가 뜨면
  `Window > Preferences > Maven > Errors/Warnings` 에서 *Plugin execution not covered* 를
  **Ignore** 로 두거나, 경고의 *Quick Fix > Mark goal ... as ignored* 를 적용합니다(빌드에는 영향 없음).

---

## 5. 데이터베이스 준비 (PostgreSQL)

```bash
# DB·계정 생성 (psql)
createdb pmc
# (필요 시) CREATE ROLE pmc LOGIN PASSWORD 'pmc';  GRANT ALL ON DATABASE pmc TO pmc;

# 스키마 + 시드 적용 (V1~V7 → 공통/관리자 → 기본 정책)
for f in db/ddl/V*.sql db/seed/S2*.sql db/seed/S1*.sql; do psql -U pmc -d pmc -f "$f"; done
```

접속 정보는 `pmc-server/src/main/resources/globals.properties` 에 있습니다(환경에 맞게 수정).

```properties
Globals.Url=jdbc:postgresql://localhost:5432/pmc
Globals.UserName=pmc
Globals.Password=pmc
Globals.ReportDir=/tmp/pmc-reports
```

> 기본 관리자 계정: **admin / pmc1234!** (시드 `db/seed/S2__common_and_admin.sql`).

---

## 6. 서버 실행 (`pmc-server`)

다음 3가지 중 하나를 선택합니다.

### (A) 내장 Jetty 플러그인 — 권장 (가장 간단, 서버 등록 불필요)

`pmc-server` 우클릭 > `Run As > Maven build...` > **Goals** 에 입력:

```
org.eclipse.jetty:jetty-maven-plugin:9.4.53.v20231009:run-war
```

→ http://localhost:8080/ 접속, **admin / pmc1234!** 로그인.
(포트/컨텍스트는 `pmc-server/pom.xml` 의 jetty 플러그인 설정에서 변경.)

### (B) Tomcat 9 서버 등록 (운영 환경에 가깝게)

1. `Servers` 뷰 > 우클릭 `New > Server` > **Apache > Tomcat v9.0 Server** > 설치 경로 지정.
   - ⚠️ **Tomcat 10 이상은 사용 불가**(서블릿 API 가 `jakarta.*` 로 바뀌어 본 프로젝트의
     `javax.servlet` 과 비호환).
2. 생성된 서버 더블클릭 > `Add and Remove...` 에서 `pmc-server` 를 **Configured** 로 이동.
3. 서버 **Start** → 브라우저에서 컨텍스트 경로로 접속.

### (C) WTP Dynamic Web (수동)

`pmc-server` 를 *Dynamic Web Module* 로 인식시킨 뒤 (B)의 톰캣에 배포. 일반적으로 (A)/(B)로 충분합니다.

---

## 7. Agent 실행 / 디버그 (`pmc-agent`)

`Run > Run Configurations... > Java Application > New` 로 실행 구성을 만듭니다.

| 항목 | 값 |
|---|---|
| Project | `pmc-agent` |
| Main class | `kr.go.pmc.agent.AgentMain` |
| Program arguments | `--config pmc-agent/src/main/resources/agent.yml --once` |
| JRE | JavaSE-1.8 (JDK 8) |

- `--once` : 등록(필요 시) → 정책 풀 → 전체 점검 1회 → 전송 → 스풀 비우기 → 종료
- `--daemon` : heartbeat/명령/스케줄 점검 루프 상주
- `--config` 미지정 시 기본 경로는 `./conf/agent.yml`

연동 흐름: 웹 UI 에서 대상 서버 등록 → **등록토큰** 발급 → `agent.yml` 의 `enrollToken` 에 입력 후
Agent 실행 → 자동 등록·수집·전송. 설치 스크립트(systemd / Windows procrun / SysV init.d)는
`pmc-agent/src/dist/` 참고.

빌드된 jar 로 직접 실행하려면:

```bash
java -jar pmc-agent/target/pmc-agent-1.0.0.jar --config pmc-agent/src/main/resources/agent.yml --once
```

---

## 8. 폐쇄망 / Nexus 차단 환경

eGov 전용 nexus(maven.egovframe.go.kr)가 차단된 환경을 고려해 의존성은 **Maven Central** 만 사용합니다.
사내 미러가 있다면 `~/.m2/settings.xml` 에 미러를 설정하세요.

```xml
<settings>
  <mirrors>
    <mirror>
      <id>company-nexus</id>
      <mirrorOf>*</mirrorOf>
      <url>https://nexus.example.com/repository/maven-public/</url>
    </mirror>
  </mirrors>
</settings>
```

eGov rte 사용이 가능한 망에서는 `egovframework.com` 경량 베이스를 eGov rte 의존성으로 교체할 수 있습니다.

---

## 9. 트러블슈팅

| 증상 | 원인 / 해결 |
|---|---|
| 톰캣 기동 시 `ClassNotFound` / `NoSuchMethod` (서블릿 관련) | Tomcat 10+ 사용. **Tomcat 9.x** 로 교체(javax↔jakarta 비호환). |
| 컴파일 에러 "release version 11 not supported" 등 | JDK 미등록/미스매치. 3장대로 JDK 11·8 등록 후 `Maven > Update Project`. |
| 한글 깨짐(JSP/CSV/로그) | 워크스페이스·JSP·properties 인코딩을 **UTF-8** 로(3.3). |
| 빨간 X (의존성 못 찾음) | `Alt+F5`(Maven > Update Project), 네트워크/미러(8장) 확인. |
| m2e *Plugin execution not covered* 경고 | shade/war 플러그인 매핑 경고 — Ignore 처리(4장). 빌드에는 무해. |
| 서버 접속 시 DB 연결 실패 | PostgreSQL 기동·`globals.properties` 접속정보·`pmc` DB/계정·스키마 적용(5장) 확인. |

---

## 참고 문서

- 빌드/실행 요약·환경 메모: [`README.md`](../README.md)
- API 계약: [`docs/api-contract.md`](api-contract.md)
- 결과 JSON 스키마: [`docs/inspection-result.schema.json`](inspection-result.schema.json)
- Agent 배포/설치: [`pmc-agent/src/dist/README.md`](../pmc-agent/src/dist/README.md)

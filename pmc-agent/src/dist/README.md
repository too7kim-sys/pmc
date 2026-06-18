# PMC Agent (장애예방점검 자동화 수집 데몬)

크로스 플랫폼 standalone Java 데몬. OS/WEB/WAS/DB/SW/NW/SVC 7개 카테고리를 점검해 PMC 서버로
표준 결과 엔벨로프(`inspection-result.schema.json`)를 전송한다. **Java 8 이상**에서 동작한다.

## 1. 구성 요소

```
pmc-agent/
  lib/pmc-agent-1.0.0.jar    # 실행 가능한 shaded jar
  bin/pmc-agent.sh           # Linux/Unix 실행 스크립트
  bin/pmc-agent.bat          # Windows 실행 스크립트
  conf/agent.yml             # 설정 파일
  systemd/pmc-agent.service  # systemd 유닛
  init.d/pmc-agent           # SysV init (AIX/HP-UX)
  logs/                      # 로그 출력
  spool/                     # 미전송 결과 스풀
```

> 빌드 산출물 `target/pmc-agent-1.0.0.jar` 를 `lib/` 로 복사해 배포한다.

## 2. 실행 모드

| 모드 | 설명 |
|------|------|
| `--once`   | 등록(미등록 시) → 정책 풀 → 활성 수집기 1회 실행 → 결과 전송 → 스풀 비우기 → 종료. cron 배치/디버깅용. |
| `--daemon` | 등록 후 상주. heartbeat 루프(명령 처리, 정책 변경 감지, 스풀 재전송) + 스케줄 점검. |
| `--help`   | 도움말 |

```sh
java -jar lib/pmc-agent-1.0.0.jar --config conf/agent.yml --once
java -jar lib/pmc-agent-1.0.0.jar --config conf/agent.yml --daemon
```

## 3. 설정 키 (`agent.yml`)

| 키 | 설명 | 기본값 |
|----|------|--------|
| `serverUrl` | 서버 베이스 URL | http://localhost:8080 |
| `enrollToken` | 최초 등록용 1회 토큰 | "" |
| `agentId` / `apiKey` | 등록 성공 시 자동 기록 | - |
| `hostname` / `ipAddr` / `osType` | 비우면 자동 탐지 | auto |
| `categories` | 수집 카테고리 목록 | 전체 7종 |
| `scheduleCron` | 매일 HH:mm (예 `0 0 6 * * *`) — 있으면 우선 | "" |
| `intervalSeconds` | 인터벌 점검(초) | 300 |
| `heartbeatSeconds` | heartbeat 주기(초) | 60 |
| `timeoutMs` | 명령/HTTP 타임아웃 | 10000 |
| `spoolDir` | 미전송 결과 스풀 경로 | ./spool |
| `logLevel` | 로그 레벨 | INFO |

환경변수 오버라이드: `PMC_SERVER_URL`, `PMC_API_KEY`, `PMC_AGENT_ID`.

### 스케줄 표기
- 간이 cron: 6필드(`초 분 시 일 월 요일`) 중 분/시만 해석하는 **일일 시각** 모드.
  `0 0 6 * * *` = 매일 06:00. 그 외 형태는 인터벌 폴백.
- 인터벌: `intervalSeconds` 또는 `intervalSeconds:N`.

## 4. 설치

### Linux (systemd)
```sh
sudo useradd -r -s /sbin/nologin pmc
sudo mkdir -p /opt/pmc-agent/{lib,bin,conf,logs,spool}
sudo cp lib/pmc-agent-1.0.0.jar /opt/pmc-agent/lib/
sudo cp bin/pmc-agent.sh /opt/pmc-agent/bin/ && sudo chmod +x /opt/pmc-agent/bin/pmc-agent.sh
sudo cp conf/agent.yml /opt/pmc-agent/conf/
sudo cp systemd/pmc-agent.service /etc/systemd/system/
sudo chown -R pmc:pmc /opt/pmc-agent
sudo systemctl daemon-reload
sudo systemctl enable --now pmc-agent
sudo systemctl status pmc-agent
journalctl -u pmc-agent -f
```

### Windows (Apache procrun / prunsrv)
`commons-daemon` 의존성을 jar 에 포함하지 않으므로 외부 procrun 으로 서비스 등록한다.
```bat
prunsrv.exe //IS//PMCAgent ^
  --DisplayName="PMC Agent" ^
  --Jvm=auto ^
  --Classpath="C:\pmc-agent\lib\pmc-agent-1.0.0.jar" ^
  --StartMode=jvm --StartClass=kr.go.pmc.agent.AgentMain ^
  --StartParams="--config;C:\pmc-agent\conf\agent.yml;--daemon" ^
  --StopMode=jvm --StopClass=kr.go.pmc.agent.AgentMain --StopMethod=main ^
  --LogPath="C:\pmc-agent\logs" --StdOutput=auto --StdError=auto
sc start PMCAgent
```
대화형 테스트는 `bin\pmc-agent.bat daemon conf\agent.yml`.

### Unix (AIX / HP-UX) — SysV init.d
```sh
cp init.d/pmc-agent /etc/init.d/pmc-agent && chmod +x /etc/init.d/pmc-agent
/etc/init.d/pmc-agent start
/etc/init.d/pmc-agent status
```

## 5. 동작 메모

- **graceful degradation**: 수집기 개별 실패는 `ERROR`/`NA` 항목으로 기록되며 run 전체를 중단하지 않는다.
- **오프라인 내성**: 전송 실패 시 결과를 `spoolDir/pending/<runId>.json.gz` 로 보관하고, 재연결
  시 FIFO 로 재전송한다. 7일 경과분은 purge.
- **보안**: 외부/정책 입력을 셸 문자열로 보간하지 않는다(argv 배열 실행). SVC URL 은
  `HttpURLConnection` 으로 직접 처리한다.
- **멱등성**: 각 run 은 UUID `runId` 를 생성하며 서버가 이를 키로 upsert 한다.

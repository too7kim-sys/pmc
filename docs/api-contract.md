# PMC Agent ↔ Server REST 계약 (`/api/v1`)

모든 Agent API는 `Authorization: Bearer <apiKey>` 헤더 필요(등록 제외). 응답은 공통 엔벨로프
`ApiResponse { success, code, message, data }` 형식.

## 1. 등록 — `POST /api/v1/agents/register`
요청:
```json
{ "enrollToken": "string", "hostname": "web01", "ipAddr": "10.0.0.1",
  "osType": "LINUX", "osVersion": "Ubuntu 24.04", "agentVersion": "1.0.0" }
```
응답 `data`: `{ "agentId": "uuid", "apiKey": "raw-key-once", "policyId": 1 }`
- `enrollToken`은 웹 UI에서 대상 서버 등록 시 발급한 1회용 토큰.
- 서버는 `sha-256(apiKey)`만 저장. apiKey 평문은 이 응답에서 1회만 노출.

## 2. Heartbeat — `POST /api/v1/agents/{agentId}/heartbeat`
요청: `{ "agentVersion": "1.0.0", "policyVersion": 1 }`
응답 `data`:
```json
{ "policyVersion": 1, "policyChanged": false,
  "commands": [ { "commandId": 12, "commandType": "RUN_NOW", "params": "{\"categories\":[\"OS\"]}" } ] }
```

## 3. 정책 풀 — `GET /api/v1/policies/active?agentId={id}&ver={n}`
응답 `data`: `{ "changed": false }` 또는 전체 `PolicyDoc`:
```json
{ "policyId": 1, "version": 1, "scheduleCron": "0 0 6 * * *",
  "items": [ { "category":"OS","itemCode":"OS_CPU_USAGE","itemName":"CPU 사용률","unit":"%",
              "checkType":"AUTO","collectYn":"Y",
              "thresholds":[{"level":"WARN","operator":"GTE","compareValue":"80"},
                            {"level":"CRITICAL","operator":"GTE","compareValue":"90"}] } ],
  "svcTargets": [ { "svcName":"PMC 포털","url":"http://localhost:8080/pmc/login.do",
                    "httpMethod":"GET","expectedStatus":200,"expectedContent":"로그인",
                    "timeoutMs":5000,"sslCheckYn":"Y" } ] }
```

## 4. 결과 전송 — `POST /api/v1/inspections/results`
요청 본문: `inspection-result.schema.json` 엔벨로프. 서버는 `runId`로 멱등 upsert.
응답 `data`: `{ "runId": "uuid", "duplicated": false, "overallStatus": "WARN" }`

## 5. 명령 결과 회신 — `POST /api/v1/agents/{agentId}/commands/{commandId}/ack`
요청: `{ "status": "DONE", "resultMsg": "..." }`  (status: DONE/FAILED)

## 6. 명령 조회(폴링 대안) — `GET /api/v1/agents/{agentId}/commands`
응답 `data`: heartbeat의 `commands`와 동일 구조.

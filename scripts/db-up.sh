#!/usr/bin/env bash
# =====================================================================
# PMC 로컬 DB 기동 스크립트
#   - PostgreSQL 16 클러스터 기동(내려가 있으면)
#   - 롤(pmc) / DB(pmc) 생성(없으면) — globals.properties 와 일치
#   - DDL(V1..Vn, 버전 숫자순) + 시드(S2 → S1 → S3) 멱등 적용
# 앱(IDE/Tomcat·Jetty)을 띄우기 "전에" 한 번 실행하면 된다.
#
# 주의: 테스트(mvn test)는 인메모리 H2 를 쓰므로 이 스크립트가 필요 없다.
#       실제 앱 구동만 PostgreSQL 을 사용한다.
#
# 사용: bash scripts/db-up.sh
# 환경변수 오버라이드: PG_VER(기본 16) PG_CLUSTER(기본 main)
#                      DB_NAME/DB_USER/DB_PASS(기본 pmc/pmc/pmc) DB_HOST/DB_PORT(기본 localhost/5432)
# =====================================================================
set -uo pipefail

PG_VER="${PG_VER:-16}"
PG_CLUSTER="${PG_CLUSTER:-main}"
DB_NAME="${DB_NAME:-pmc}"
DB_USER="${DB_USER:-pmc}"
DB_PASS="${DB_PASS:-pmc}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

# 저장소 루트(스크립트 위치 기준)
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DDL_DIR="$ROOT/db/ddl"
SEED_DIR="$ROOT/db/seed"

say() { printf '\033[1;36m[db-up]\033[0m %s\n' "$*"; }
err() { printf '\033[1;31m[db-up] %s\033[0m\n' "$*" >&2; }

# postgres 슈퍼유저 명령 실행기(권한 환경에 따라 sudo 사용)
pg_super() {
  if [ "$(id -u)" = "0" ]; then sudo -u postgres "$@";
  elif command -v sudo >/dev/null 2>&1; then sudo -u postgres "$@";
  else "$@"; fi
}

# 1) 클러스터 기동 ----------------------------------------------------
if pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1; then
  say "PostgreSQL 이미 기동됨 ($DB_HOST:$DB_PORT)"
else
  say "PostgreSQL 기동 시도..."
  if command -v pg_ctlcluster >/dev/null 2>&1; then
    pg_super pg_ctlcluster "$PG_VER" "$PG_CLUSTER" start || true
  fi
  # 폴백: service 스크립트
  if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1 && command -v service >/dev/null 2>&1; then
    if [ "$(id -u)" = "0" ]; then service postgresql start || true
    else sudo service postgresql start || true; fi
  fi
  # 준비 대기(최대 ~15초)
  for i in $(seq 1 15); do
    pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1 && break
    sleep 1
  done
  if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1; then
    err "PostgreSQL 기동 실패 — 수동 확인 필요: pg_lsclusters / 로그 $(ls /var/log/postgresql/ 2>/dev/null)"
    exit 1
  fi
  say "PostgreSQL 기동 완료"
fi

# 2) 롤 / DB 생성(없으면) --------------------------------------------
if [ "$(pg_super psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" 2>/dev/null)" != "1" ]; then
  say "롤 생성: $DB_USER"
  pg_super psql -c "CREATE ROLE \"$DB_USER\" LOGIN PASSWORD '$DB_PASS';" >/dev/null
else
  say "롤 존재: $DB_USER"
fi
if [ "$(pg_super psql -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME'" 2>/dev/null)" != "1" ]; then
  say "DB 생성: $DB_NAME (owner=$DB_USER)"
  pg_super createdb -O "$DB_USER" "$DB_NAME" >/dev/null
else
  say "DB 존재: $DB_NAME"
fi

# 3) DDL + 시드 적용(멱등) -------------------------------------------
# DDL 은 버전 숫자순(V1..V10) — 문자열 정렬은 V10 을 V1 앞에 두므로 sort -V 필수.
apply() {
  local f="$1"
  local out
  out=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
        -v ON_ERROR_STOP=1 -q -f "$f" 2>&1)
  if [ $? -eq 0 ]; then
    say "OK  $(basename "$f")"
  else
    err "FAIL $(basename "$f")"
    printf '%s\n' "$out" | tail -3 >&2
    FAILED=$((FAILED+1))
  fi
}

FAILED=0
say "스키마/시드 적용..."
# shellcheck disable=SC2046
for f in $(ls "$DDL_DIR"/V*.sql | sort -V); do apply "$f"; done
for f in "$SEED_DIR"/S2*.sql "$SEED_DIR"/S1*.sql "$SEED_DIR"/S3*.sql "$SEED_DIR"/S4*.sql; do [ -f "$f" ] && apply "$f"; done

# 4) 최종 점검 -------------------------------------------------------
ADMIN=$(PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc \
  "SELECT emplyr_id||'/'||emplyr_sttus||'/len'||length(password) FROM comtnemplyrinfo WHERE emplyr_id='admin';" 2>/dev/null)
say "관리자 계정: ${ADMIN:-(없음!)}  (로그인: admin / pmc1234!)"
if [ "$FAILED" -gt 0 ]; then
  err "$FAILED 개 스크립트가 실패했습니다(위 로그 확인). 대개 기존 DB 드리프트로, 로그인/핵심기능에는 영향 없을 수 있음."
fi
say "완료. 이제 IDE(Tomcat)/Jetty 로 앱을 (재)시작하세요."

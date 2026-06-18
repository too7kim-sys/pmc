#!/bin/sh
# PMC Agent 실행 스크립트 (Linux/AIX/HP-UX/Solaris)
# 사용법: pmc-agent.sh {once|daemon} [config-path]

# 스크립트 위치 기준 홈 디렉토리 산출
PRG="$0"
while [ -h "$PRG" ]; do
  PRG=$(readlink "$PRG")
done
BIN_DIR=$(cd "$(dirname "$PRG")" && pwd)
HOME_DIR=$(cd "$BIN_DIR/.." && pwd)

JAR="$HOME_DIR/lib/pmc-agent-1.0.0.jar"
CONF="${2:-$HOME_DIR/conf/agent.yml}"

# Java 8+ 필요. JAVA_HOME 우선.
if [ -n "$JAVA_HOME" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=java
fi

JAVA_OPTS="${JAVA_OPTS:--Xms32m -Xmx128m -Dfile.encoding=UTF-8}"
export PMC_LOG_DIR="${PMC_LOG_DIR:-$HOME_DIR/logs}"

MODE="$1"
case "$MODE" in
  once)
    exec "$JAVA" $JAVA_OPTS -jar "$JAR" --config "$CONF" --once
    ;;
  daemon)
    exec "$JAVA" $JAVA_OPTS -jar "$JAR" --config "$CONF" --daemon
    ;;
  *)
    echo "사용법: $0 {once|daemon} [config-path]"
    exit 1
    ;;
esac

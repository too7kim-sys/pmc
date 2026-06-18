@echo off
REM PMC Agent 실행 스크립트 (Windows)
REM 사용법: pmc-agent.bat {once|daemon} [config-path]

setlocal
set "BIN_DIR=%~dp0"
set "HOME_DIR=%BIN_DIR%.."
set "JAR=%HOME_DIR%\lib\pmc-agent-1.0.0.jar"

if "%~2"=="" (
  set "CONF=%HOME_DIR%\conf\agent.yml"
) else (
  set "CONF=%~2"
)

if defined JAVA_HOME (
  set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA=java"
)

if not defined JAVA_OPTS set "JAVA_OPTS=-Xms32m -Xmx128m -Dfile.encoding=UTF-8"
if not defined PMC_LOG_DIR set "PMC_LOG_DIR=%HOME_DIR%\logs"

if /I "%~1"=="once" (
  "%JAVA%" %JAVA_OPTS% -jar "%JAR%" --config "%CONF%" --once
  goto :eof
)
if /I "%~1"=="daemon" (
  "%JAVA%" %JAVA_OPTS% -jar "%JAR%" --config "%CONF%" --daemon
  goto :eof
)

echo 사용법: %~nx0 {once^|daemon} [config-path]
exit /b 1

@echo off
setlocal

where mvn >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Maven is not installed or not added to PATH.
  pause
  exit /b 1
)

mvn -DskipTests compile
pause

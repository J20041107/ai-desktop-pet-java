@echo off
setlocal

set "JAVA_HOME=D:\IntelliJ IDEA 2023.3.7\jbr"
set "MAVEN_CMD=D:\IntelliJ IDEA 2023.3.7\plugins\maven\lib\maven3\bin\mvn.cmd"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%DEEPSEEK_API_KEY%"=="" (
  echo [WARN] DEEPSEEK_API_KEY is empty. You can set it with:
  echo setx DEEPSEEK_API_KEY "your-api-key"
  echo.
)

if not exist "%MAVEN_CMD%" (
  echo [ERROR] Maven not found: %MAVEN_CMD%
  pause
  exit /b 1
)

"%MAVEN_CMD%" javafx:run
pause

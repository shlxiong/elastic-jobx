@echo off
if ""%1"" == ""-p"" goto doSetPort
if ""%1"" == """" goto doStart

echo Usage:  %0 [OPTIONS]
echo   -p [port]          Server port (default: 8899)
goto end

:doSetPort
shift
set PORT=%1

:doStart
set CFG_DIR=%~dp0%..
set CLASSPATH=%CFG_DIR%
set CLASSPATH=%~dp0..\lib\*;%CLASSPATH%
set CONSOLE_MAIN=com.dangdang.ddframe.job.lite.console.ConsoleBootstrap
SET JVM_OPTS=-server -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000
echo on
if ""%PORT%"" == """" set PORT=8899
java %JVM_OPTS% -Dspring.profiles.active=test -cp "%CLASSPATH%" %CONSOLE_MAIN% %PORT%

:end

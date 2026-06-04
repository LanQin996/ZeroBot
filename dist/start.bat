@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar ZeroBot.jar %*
endlocal

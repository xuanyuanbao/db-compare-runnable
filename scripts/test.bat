@echo off
setlocal
cd /d %~dp0\..
if exist gradlew.bat (
  call gradlew.bat test
) else (
  gradle test
)

@echo off
setlocal
cd /d %~dp0\..
if exist gradlew.bat (
  call gradlew.bat bootRun
) else (
  gradle bootRun
)

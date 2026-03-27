@echo off
setlocal
cd /d %~dp0\..
call scripts\build.bat
if errorlevel 1 exit /b 1
java -cp out com.example.dbcompare.app.CompareApplication examples\demo\demo.properties

@echo off
setlocal enabledelayedexpansion
cd /d %~dp0\..
call scripts\build.bat
if errorlevel 1 exit /b 1
if exist out-test rmdir /s /q out-test
mkdir out-test
if exist .test-sources.list del /q .test-sources.list
for /r src\test\java %%f in (*.java) do echo %%f>> .test-sources.list
javac -encoding UTF-8 -cp out -d out-test @.test-sources.list
if errorlevel 1 exit /b 1
del /q .test-sources.list
java -cp out;out-test com.example.dbcompare.tests.AllTests

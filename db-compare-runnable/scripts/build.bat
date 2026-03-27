@echo off
setlocal enabledelayedexpansion
cd /d %~dp0\..
if exist out rmdir /s /q out
mkdir out
if exist .sources.list del /q .sources.list
for /r src\main\java %%f in (*.java) do echo %%f>> .sources.list
javac -encoding UTF-8 -d out @.sources.list
if errorlevel 1 exit /b 1
del /q .sources.list
echo Build ok. Classes in %cd%\out

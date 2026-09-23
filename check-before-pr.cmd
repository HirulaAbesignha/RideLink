@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\pre-pr-check.ps1"
exit /b %ERRORLEVEL%

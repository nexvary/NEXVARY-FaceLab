@echo off
setlocal
chcp 65001 >nul
if exist ".venv\Scripts\python.exe" (
  ".venv\Scripts\python.exe" providers\engine_doctor.py
) else (
  python providers\engine_doctor.py
)
if errorlevel 1 exit /b 1
pause

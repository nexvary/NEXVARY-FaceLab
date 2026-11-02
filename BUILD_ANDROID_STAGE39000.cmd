@echo off
setlocal
cd /d "%~dp0"
set LOG=build_stage39000.log
if exist "%LOG%" del /q "%LOG%"
echo NEXVARY FaceLab Stage 39500 UI Release Gate>"%LOG%"
echo [%date% %time%] Starting strict UI QA...>>"%LOG%"
where py >nul 2>nul
if %errorlevel%==0 (
  py -3 qa_ui_release_gate_39000.py >>"%LOG%" 2>&1
) else (
  python qa_ui_release_gate_39000.py >>"%LOG%" 2>&1
)
if errorlevel 1 goto :fail

echo [%date% %time%] Starting Gradle clean assembleDebug...>>"%LOG%"
call gradlew.bat clean :app:assembleDebug --stacktrace >>"%LOG%" 2>&1
if errorlevel 1 goto :fail
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo APK not found after successful Gradle command.>>"%LOG%"
  goto :fail
)
echo. 
echo ========================================
echo BUILD PASS - STAGE 39000
echo APK: app\build\outputs\apk\debug\app-debug.apk
echo Log: %LOG%
echo ========================================
exit /b 0

:fail
echo.
echo ========================================
echo BUILD FAILED - DO NOT INSTALL
echo Full log: %LOG%
echo ========================================
exit /b 1

@echo off
setlocal
cd /d "%~dp0"
echo ==================================================
echo NEXVARY FaceLab Stage 38000 - ELECTRIC UI
echo ==================================================
if exist build_stage38000.log del /q build_stage38000.log
call gradlew.bat clean :app:assembleDebug --stacktrace > build_stage38000.log 2>&1
set RC=%ERRORLEVEL%
type build_stage38000.log
if not "%RC%"=="0" (
  echo.
  echo BUILD FAILED - full log saved to build_stage38000.log
  exit /b %RC%
)
if not exist app\build\outputs\apk\debug\app-debug.apk (
  echo BUILD FAILED - Gradle returned success but APK was not found.
  exit /b 91
)
echo.
echo BUILD PASS
echo APK: app\build\outputs\apk\debug\app-debug.apk
exit /b 0

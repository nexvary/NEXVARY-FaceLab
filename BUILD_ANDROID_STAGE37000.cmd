@echo off
setlocal
cd /d "%~dp0"
echo ==============================================
echo NEXVARY FaceLab Stage 37000 - RESTRUCTURED
echo ==============================================
echo.
if exist build_stage37000.log del /q build_stage37000.log
call gradlew.bat clean :app:assembleDebug --stacktrace > build_stage37000.log 2>&1
set RC=%ERRORLEVEL%
type build_stage37000.log
if not "%RC%"=="0" (
  echo.
  echo BUILD FAILED - full log saved to build_stage37000.log
  exit /b %RC%
)
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo BUILD FAILED - Gradle returned success but APK was not found.
  exit /b 20
)
echo.
echo BUILD PASS
echo APK: app\build\outputs\apk\debug\app-debug.apk
endlocal

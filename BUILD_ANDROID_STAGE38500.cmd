@echo off
setlocal
cd /d "%~dp0"
echo ==========================================================
echo NEXVARY FaceLab Stage 39500 FINAL UI - Android Build
echo ==========================================================
call gradlew.bat clean :app:assembleDebug --stacktrace > build_stage38500.log 2>&1
if errorlevel 1 (
  echo BUILD FAILED. See build_stage38500.log
  type build_stage38500.log
  exit /b 1
)
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo BUILD FAILED: APK not found.
  exit /b 2
)
echo BUILD PASS
echo APK: app\build\outputs\apk\debug\app-debug.apk
endlocal

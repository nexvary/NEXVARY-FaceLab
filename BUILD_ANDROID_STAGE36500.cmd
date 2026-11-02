@echo off
setlocal
cd /d "%~dp0"
echo ==============================================
echo NEXVARY FaceLab Stage 36500 - UX Android 15
 echo ==============================================
call gradlew.bat clean :app:assembleDebug --stacktrace > build_stage36500.log 2>&1
if errorlevel 1 (
  echo BUILD FAILED - see build_stage36500.log
  type build_stage36500.log
  exit /b 1
)
if exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo BUILD PASS
  echo APK: app\build\outputs\apk\debug\app-debug.apk
  exit /b 0
)
echo BUILD FAILED - APK not found
exit /b 2

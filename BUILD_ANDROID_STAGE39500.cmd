@echo off
setlocal
cd /d "%~dp0"
echo ==================================================
echo NEXVARY FaceLab Stage 39500 - Compiler Hardened
echo ==================================================
call gradlew.bat clean :app:compileDebugKotlin :app:assembleDebug --stacktrace > build_stage39500.log 2>&1
if errorlevel 1 (
  type build_stage39500.log
  echo.
  echo BUILD FAILED - DO NOT INSTALL
  exit /b 1
)
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo APK missing after Gradle success.
  exit /b 2
)
echo BUILD PASS
echo APK: app\build\outputs\apk\debug\app-debug.apk
endlocal

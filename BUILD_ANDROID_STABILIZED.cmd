@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "LOG=%~dp0build_stage36000.log"
echo ==============================================================
echo NEXVARY FaceLab Stage 36000 - Stabilized Android Build
echo ==============================================================
echo.
java -version >nul 2>&1
if errorlevel 1 (
  echo ERROR: Java/JDK was not found in PATH.
  echo Android Studio JDK 17 or newer is required.
  pause
  exit /b 10
)

echo [1/3] Cleaning previous build...
call gradlew.bat clean > "%LOG%" 2>&1
if errorlevel 1 goto :failed

echo [2/3] Compiling Kotlin and Android resources...
call gradlew.bat :app:assembleDebug >> "%LOG%" 2>&1
if errorlevel 1 goto :failed

echo [3/3] Verifying APK...
if not exist "app\build\outputs\apk\debug\app-debug.apk" (
  echo ERROR: Gradle returned success but APK is missing.>> "%LOG%"
  goto :failed
)

echo.
echo BUILD PASS
echo APK: %~dp0app\build\outputs\apk\debug\app-debug.apk
echo Full log: %LOG%
explorer "app\build\outputs\apk\debug" >nul 2>&1
pause
exit /b 0

:failed
echo.
echo BUILD FAILED - no success is being claimed.
echo The complete compiler output is saved here:
echo %LOG%
echo.
echo Last 25 lines:
powershell -NoProfile -Command "if(Test-Path '%LOG%'){Get-Content -Path '%LOG%' -Tail 25}"
pause
exit /b 1

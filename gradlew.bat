@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "GRADLE_VERSION=8.10.2"
set "CACHE=%USERPROFILE%\.gradle\nexvary-bootstrap"
set "ZIP=%CACHE%\gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_DIR=%CACHE%\gradle-%GRADLE_VERSION%"
set "GRADLE=%GRADLE_DIR%\bin\gradle.bat"
if not exist "%GRADLE%" (
  echo [NEXVARY FaceLab] Preparing Gradle %GRADLE_VERSION%...
  if not exist "%CACHE%" mkdir "%CACHE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'; if ((Get-FileHash '%ZIP%' -Algorithm SHA256).Hash.ToLower() -ne '31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26') { throw 'Gradle checksum mismatch' }; Expand-Archive -Force '%ZIP%' '%CACHE%'"
  if errorlevel 1 (
    echo ERROR: Could not download Gradle. Check Internet access or proxy settings.
    exit /b 1
  )
)
call "%GRADLE%" %*
exit /b %ERRORLEVEL%

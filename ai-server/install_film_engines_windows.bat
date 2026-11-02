@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
set "ROOT=%~1"
if "%ROOT%"=="" set "ROOT=D:\NEXVARY-AI"
if not exist "%ROOT%" mkdir "%ROOT%"
where git >nul 2>nul || (echo [ERROR] Git is required. & exit /b 2)
where python >nul 2>nul || (echo [ERROR] Python is required. & exit /b 3)
echo ============================================================
echo NEXVARY FaceLab Stage 10500 - Film Engines Bootstrap
echo Target: %ROOT%
echo Personal/Friends Non-Commercial profile
echo ============================================================
call :clone https://github.com/KlingAIResearch/LivePortrait.git LivePortrait
call :clone https://github.com/Tencent/MimicMotion.git MimicMotion
call :clone https://github.com/antgroup/echomimic_v2.git EchoMimicV2
call :clone https://github.com/jj2t-qt/LivePortrait.git _skip_invalid_ >nul 2>nul
call :clone https://github.com/jjt997/MuseTalk.git MuseTalk
call :clone https://github.com/Wan-Video/Wan2.2.git Wan2.2

echo.
echo Repositories are present. Models are NOT force-downloaded by this bootstrap.
echo Each engine keeps its own environment to avoid PyTorch/CUDA conflicts.
echo Run engine_doctor_windows.bat after configuring the environments.
exit /b 0

:clone
set "URL=%~1"
set "NAME=%~2"
if "%NAME%"=="_skip_invalid_" exit /b 0
if exist "%ROOT%\%NAME%\.git" (
  echo [OK] %NAME% already exists
  exit /b 0
)
echo [CLONE] %NAME%
git clone --depth 1 "%URL%" "%ROOT%\%NAME%"
if errorlevel 1 (echo [WARN] Could not clone %NAME% & exit /b 0)
exit /b 0

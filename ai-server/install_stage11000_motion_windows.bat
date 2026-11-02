@echo off
setlocal EnableExtensions
set ROOT=%~1
if "%ROOT%"=="" set ROOT=D:\NEXVARY-AI

echo ==============================================================
echo NEXVARY FaceLab Stage 11000 - Motion Engines Windows Bootstrap
echo Root: %ROOT%
echo ==============================================================
where git >nul 2>nul || (echo [ERROR] Git is required. & exit /b 2)
where conda >nul 2>nul || (echo [ERROR] Miniconda/Anaconda is required for isolated engine environments. & exit /b 3)
where ffmpeg >nul 2>nul || echo [WARN] FFmpeg not found in PATH. LivePortrait/film assembly needs ffmpeg+ffprobe.
if not exist "%ROOT%" mkdir "%ROOT%"

if not exist "%ROOT%\LivePortrait\.git" (
  echo [1/4] Cloning official LivePortrait...
  git clone https://github.com/KlingTeam/LivePortrait "%ROOT%\LivePortrait" || exit /b 10
) else echo [1/4] LivePortrait already present.

call conda env list | findstr /I /R "^LivePortrait " >nul
if errorlevel 1 (
  echo [2/4] Creating LivePortrait Python 3.10 environment...
  call conda create -y -n LivePortrait python=3.10 || exit /b 11
)
call conda run -n LivePortrait python -m pip install -U pip || exit /b 12
call conda run -n LivePortrait python -m pip install -r "%ROOT%\LivePortrait\requirements.txt" || exit /b 13

if not exist "%ROOT%\MimicMotion\.git" (
  echo [3/4] Cloning official MimicMotion...
  git clone https://github.com/Tencent/MimicMotion "%ROOT%\MimicMotion" || exit /b 20
) else echo [3/4] MimicMotion already present.

call conda env list | findstr /I /R "^mimicmotion " >nul
if errorlevel 1 (
  echo [4/4] Creating MimicMotion environment from official environment.yaml...
  call conda env create -f "%ROOT%\MimicMotion\environment.yaml" || exit /b 21
) else echo [4/4] MimicMotion environment already present.

echo.
echo Bootstrap complete. Weights are NOT forced here.
echo Next run: download_stage11000_motion_weights_windows.bat %ROOT%
echo Then run: configure_stage11000_motion_windows.bat %ROOT%
exit /b 0

@echo off
setlocal
cd /d "%~dp0"
echo ==================================================
echo NEXVARY FaceLab - Local Video Engine Preflight
echo ==================================================
if not exist .venv\Scripts\python.exe (
  echo [ERROR] Python virtual environment not found. Run install_generative_windows.bat first.
  exit /b 1
)
.venv\Scripts\python.exe -c "import torch; print('Torch:',torch.__version__,'CUDA:',torch.cuda.is_available())"
if errorlevel 1 exit /b 1
.venv\Scripts\python.exe -c "import diffusers; print('Diffusers:',diffusers.__version__)"
if errorlevel 1 (
  echo [INFO] Installing video orchestration dependencies only. No model weights will be downloaded.
  .venv\Scripts\python.exe -m pip install -U diffusers transformers accelerate imageio imageio-ffmpeg
  if errorlevel 1 exit /b 1
)
where ffmpeg >nul 2>nul
if errorlevel 1 echo [WARN] FFmpeg not found in PATH. Configure FACELAB_FFMPEG.
echo.
echo [OK] Video runtime preflight complete.
echo IMPORTANT: set FACELAB_COGVIDEOX_MODEL to a local license-audited model directory.
echo No model weights were downloaded by this script.
endlocal

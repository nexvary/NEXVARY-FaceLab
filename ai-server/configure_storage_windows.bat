@echo off
setlocal
if "%~1"=="" (
  echo Usage: configure_storage_windows.bat D:\NEXVARY-AI
  exit /b 1
)
set "ROOT=%~1"
if not exist "%ROOT%" mkdir "%ROOT%"
if not exist "%ROOT%\hf-cache" mkdir "%ROOT%\hf-cache"
if not exist "%ROOT%\torch-cache" mkdir "%ROOT%\torch-cache"
setx FACELAB_DATA_ROOT "%ROOT%" >nul
setx HF_HOME "%ROOT%\hf-cache" >nul
setx TORCH_HOME "%ROOT%\torch-cache" >nul
echo FaceLab AI storage configured at: %ROOT%
echo Close and reopen Command Prompt before installing/running the AI server.
endlocal

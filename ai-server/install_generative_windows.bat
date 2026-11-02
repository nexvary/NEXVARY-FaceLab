@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo [FaceLab] Generative AI installer - Stage 4000
for /f "tokens=3" %%A in ('dir /-c "%CD%" ^| find "bytes free"') do set FREEBYTES=%%A
echo Working folder: %CD%
echo Recommended free space: 20 GB or more.
echo.

if not exist .venv\Scripts\python.exe (
  echo [1/5] Creating Python virtual environment...
  py -3 -m venv .venv
  if errorlevel 1 goto :fail
) else (
  echo [1/5] Existing virtual environment found.
)

call .venv\Scripts\activate.bat
if errorlevel 1 goto :fail

echo [2/5] Updating pip...
python -m pip install --upgrade pip
if errorlevel 1 goto :fail

echo [3/5] Installing core requirements...
pip install -r requirements.txt
if errorlevel 1 goto :fail

echo [4/5] Installing generative requirements...
pip install -r requirements-generative.txt
if errorlevel 1 goto :fail

echo [5/5] Verifying PyTorch...
python -c "import torch; print('Torch',torch.__version__,'CUDA',torch.cuda.is_available())"
if errorlevel 1 goto :fail

echo.
echo [SUCCESS] Generative dependencies installed and verified.
echo Start with: set ENABLE_GENERATIVE=1 ^&^& run_windows.bat
exit /b 0

:fail
echo.
echo [FAILED] Installation stopped because a required step failed.
echo Fix the error shown above, then run this installer again.
exit /b 1

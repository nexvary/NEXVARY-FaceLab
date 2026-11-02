@echo off
setlocal EnableExtensions
cd /d "%~dp0"
echo NEXVARY FaceLab Stage 4000 - Film diagnostics
echo.
where ffmpeg >nul 2>nul
if errorlevel 1 (
  echo FFmpeg PATH: NOT FOUND
) else (
  echo FFmpeg PATH: FOUND
  ffmpeg -version | findstr /b "ffmpeg version"
)
if defined FACELAB_FFMPEG echo FACELAB_FFMPEG=%FACELAB_FFMPEG%
if defined FACELAB_VIDEO_ENGINE_CMD_JSON (
  echo Local AI adapter: CONFIGURED
) else (
  echo Local AI adapter: NOT CONFIGURED - preview mode only
)
if defined FACELAB_FILM_DIR echo FACELAB_FILM_DIR=%FACELAB_FILM_DIR%
if defined FACELAB_DATA_ROOT echo FACELAB_DATA_ROOT=%FACELAB_DATA_ROOT%
echo.
if exist .venv\Scripts\python.exe (
  .venv\Scripts\python.exe -c "from providers.film import status; import json; print(json.dumps(status(),indent=2))"
) else (
  echo Python venv not found. Run install_generative_windows.bat first.
)

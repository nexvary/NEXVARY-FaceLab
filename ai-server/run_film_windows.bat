@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ==============================================
echo  NEXVARY FaceLab Stage 4000 - Film Server
echo ==============================================
echo.

where ffmpeg >nul 2>nul
if errorlevel 1 (
  if not defined FACELAB_FFMPEG (
    echo [WARNING] FFmpeg was not found in PATH and FACELAB_FFMPEG is empty.
    echo Motion Preview will not render until FFmpeg is installed/configured.
  ) else (
    if not exist "%FACELAB_FFMPEG%" (
      echo [WARNING] FACELAB_FFMPEG does not point to an existing file:
      echo %FACELAB_FFMPEG%
    ) else (
      echo [OK] FFmpeg configured: %FACELAB_FFMPEG%
    )
  )
) else (
  echo [OK] FFmpeg found in PATH.
)

if not defined FACELAB_FILM_PREVIEW_SECONDS set FACELAB_FILM_PREVIEW_SECONDS=12
if not defined FACELAB_FILM_WORKERS set FACELAB_FILM_WORKERS=1

echo Preview seconds: %FACELAB_FILM_PREVIEW_SECONDS%
echo Film workers: %FACELAB_FILM_WORKERS%
echo.

call run_windows.bat

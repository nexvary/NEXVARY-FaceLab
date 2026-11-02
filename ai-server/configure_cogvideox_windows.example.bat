@echo off
REM Copy this file to configure_cogvideox_windows.bat and edit paths.
set "FACELAB_COGVIDEOX_MODEL=D:\NEXVARY-AI\models\cogvideox-i2v"
set "FACELAB_VIDEO_ENGINE_CMD_JSON=[\"%CD%\\.venv\\Scripts\\python.exe\",\"%CD%\\adapters\\cogvideox_i2v.py\",\"--image\",\"{input}\",\"--prompt\",\"{prompt}\",\"--seconds\",\"{duration}\",\"--output\",\"{output}\",\"--seed\",\"{seed}\",\"--aspect-ratio\",\"{aspect_ratio}\"]"
echo CogVideoX local adapter configured for this CMD session.

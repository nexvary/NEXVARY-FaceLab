@echo off
setlocal EnableExtensions
set ROOT=%~1
if "%ROOT%"=="" set ROOT=D:\NEXVARY-AI
set SCRIPT=%~dp0
setx FACELAB_DATA_ROOT "%ROOT%"
setx FACELAB_LIVEPORTRAIT_DIR "%ROOT%\LivePortrait"
setx FACELAB_MIMICMOTION_DIR "%ROOT%\MimicMotion"
setx FACELAB_DRIVING_MOTIONS_DIR "%ROOT%\driving-motions"
setx FACELAB_MIMIC_CKPT "%ROOT%\MimicMotion\models\MimicMotion_1-1.pth"
setx FACELAB_LIVEPORTRAIT_CMD_JSON "[\"conda\",\"run\",\"-n\",\"LivePortrait\",\"python\",\"%SCRIPT%adapters/liveportrait_runner.py\",\"--repo\",\"%ROOT%/LivePortrait\",\"--input\",\"{input}\",\"--driving\",\"{driving}\",\"--output\",\"{output}\"]"
setx FACELAB_MIMICMOTION_CMD_JSON "[\"conda\",\"run\",\"-n\",\"mimicmotion\",\"python\",\"%SCRIPT%adapters/mimicmotion_runner.py\",\"--repo\",\"%ROOT%/MimicMotion\",\"--input\",\"{input}\",\"--driving\",\"{driving}\",\"--output\",\"{output}\"]"
if not exist "%ROOT%\driving-motions" mkdir "%ROOT%\driving-motions"
echo Stage 11000 motion environment variables saved.
echo Close and reopen Command Prompt before running the server.

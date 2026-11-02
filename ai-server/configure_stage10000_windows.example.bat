@echo off
set "FACELAB_DATA_ROOT=D:\NEXVARY-AI\data"
set "FACELAB_WAN22_DIR=D:\NEXVARY-AI\Wan2.2"
set "FACELAB_MIMICMOTION_DIR=D:\NEXVARY-AI\MimicMotion"
set "FACELAB_LIVEPORTRAIT_DIR=D:\NEXVARY-AI\LivePortrait"
set "FACELAB_ECHOMIMICV2_DIR=D:\NEXVARY-AI\EchoMimicV2"
set "FACELAB_MUSETALK_DIR=D:\NEXVARY-AI\MuseTalk"
set "FACELAB_FFMPEG=ffmpeg"
REM Example only: supply a driving motion file to the LivePortrait adapter when used.
set "FACELAB_LIVEPORTRAIT_ADAPTER=%~dp0adapters\liveportrait_runner.py"
echo Stage 10500 paths configured for this CMD session.

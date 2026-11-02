@echo off
REM NEXVARY FaceLab Stage 9500 - Personal/Friends Non-Commercial engine profile
REM Edit paths/commands to match your local cloned repositories and their official inference CLIs.
set FACELAB_WAN22_DIR=D:\NEXVARY-AI\Wan2.2
set FACELAB_MIMICMOTION_DIR=D:\NEXVARY-AI\MimicMotion
set FACELAB_LIVEPORTRAIT_DIR=D:\NEXVARY-AI\LivePortrait
set FACELAB_ECHOMIMICV2_DIR=D:\NEXVARY-AI\EchoMimicV2
set FACELAB_MUSETALK_DIR=D:\NEXVARY-AI\MuseTalk

REM Each *_CMD_JSON is an argv JSON array. Do not use cmd /c or shell quoting.
REM Example shape only - use the exact CLI of the installed version:
REM set FACELAB_WAN22_CMD_JSON=["D:\\NEXVARY-AI\\Wan2.2\\.venv\\Scripts\\python.exe","generate.py","--image","{input}","--prompt","{prompt}","--output","{output}"]

echo Engine roots configured for this CMD session.

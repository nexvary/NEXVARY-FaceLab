@echo off
setlocal EnableExtensions
set ROOT=%~1
if "%ROOT%"=="" set ROOT=D:\NEXVARY-AI
where huggingface-cli >nul 2>nul
if errorlevel 1 (
  echo Installing Hugging Face CLI into LivePortrait environment...
  call conda run -n LivePortrait python -m pip install -U "huggingface_hub[cli]" || exit /b 2
)

echo [1/2] Downloading official LivePortrait pretrained weights...
call conda run -n LivePortrait huggingface-cli download KlingTeam/LivePortrait --local-dir "%ROOT%\LivePortrait\pretrained_weights" --exclude "*.git*" "README.md" "docs" || exit /b 10

echo [2/2] MimicMotion weights need the official DWPose files plus MimicMotion_1-1.pth.
if not exist "%ROOT%\MimicMotion\models\DWPose" mkdir "%ROOT%\MimicMotion\models\DWPose"
call conda run -n mimicmotion python -m pip install -U huggingface_hub || exit /b 20
call conda run -n mimicmotion python -c "from huggingface_hub import hf_hub_download; hf_hub_download('yzd-v/DWPose','yolox_l.onnx',local_dir=r'%ROOT%\MimicMotion\models\DWPose'); hf_hub_download('yzd-v/DWPose','dw-ll_ucoco_384.onnx',local_dir=r'%ROOT%\MimicMotion\models\DWPose'); hf_hub_download('tencent/MimicMotion','MimicMotion_1-1.pth',local_dir=r'%ROOT%\MimicMotion\models')" || exit /b 21

echo.
echo NOTE: MimicMotion also uses stabilityai/stable-video-diffusion-img2vid-xt-1-1.
echo It can be downloaded on first run or pre-cached in HF_HOME.
echo Done.

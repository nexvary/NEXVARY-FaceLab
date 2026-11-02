@echo off
setlocal
set ENABLE_GENERATIVE=1
if not defined FACE_DIFFUSION_MODEL set FACE_DIFFUSION_MODEL=stable-diffusion-v1-5/stable-diffusion-inpainting
call run_windows.bat

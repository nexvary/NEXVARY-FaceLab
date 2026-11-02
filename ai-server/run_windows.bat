@echo off
setlocal
py -3 -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
pip install -r requirements.txt
if not defined FACELAB_BIND_HOST set "FACELAB_BIND_HOST=127.0.0.1"
python -m uvicorn app:app --host %FACELAB_BIND_HOST% --port 8000

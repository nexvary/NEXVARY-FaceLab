@echo off
setlocal
py -3 -m venv .venv
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
pip install -r requirements.txt
python -m uvicorn app:app --host 0.0.0.0 --port 8000

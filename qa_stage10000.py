from pathlib import Path
import ast, json, sys
root=Path(__file__).resolve().parent
checks=[]
for p in (root/'ai-server').rglob('*.py'):
    ast.parse(p.read_text(encoding='utf-8'))
checks.append('Python AST: PASS')
text=(root/'ai-server'/'app.py').read_text()
for token in ['10500.0.0','/v1/film/doctor','/v1/film/motions']:
    assert token in text, token
checks.append('Stage 10500 API wiring: PASS')
from importlib.util import spec_from_file_location,module_from_spec
sys.path.insert(0,str(root/'ai-server'))
from providers.motion_presets import catalog
assert catalog()['count']>=12
checks.append('Motion presets >= 12: PASS')
from providers.engine_doctor import report
r=report(); assert 'gpu' in r and 'engines' in r and 'storage' in r
checks.append('Engine Doctor report: PASS')
assert (root/'ai-server'/'adapters'/'liveportrait_runner.py').exists()
checks.append('LivePortrait runner: PASS')
assert (root/'ai-server'/'install_film_engines_windows.bat').exists()
checks.append('Windows multi-engine bootstrap: PASS')
print('\n'.join(checks))

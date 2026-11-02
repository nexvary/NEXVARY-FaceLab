from __future__ import annotations
import json, os, platform, shutil, subprocess, sys
from pathlib import Path
from typing import Any
from providers.film_engines import status as engine_status

def _run(cmd:list[str], timeout=12):
    try:
        p=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=timeout)
        return p.returncode,(p.stdout or p.stderr).strip()
    except Exception as e:
        return -1,str(e)

def _nvidia() -> dict[str,Any]:
    exe=shutil.which('nvidia-smi')
    if not exe:
        return {"available":False,"reason":"nvidia-smi not found"}
    rc,out=_run([exe,'--query-gpu=name,memory.total,memory.free,driver_version','--format=csv,noheader,nounits'])
    rows=[]
    if rc==0:
        for line in out.splitlines():
            parts=[x.strip() for x in line.split(',')]
            if len(parts)>=4:
                rows.append({"name":parts[0],"vram_total_mb":int(float(parts[1])),"vram_free_mb":int(float(parts[2])),"driver":parts[3]})
    return {"available":rc==0,"gpus":rows,"raw":out if rc else None}

def _torch() -> dict[str,Any]:
    try:
        import torch
        return {"installed":True,"version":torch.__version__,"cuda_available":bool(torch.cuda.is_available()),"cuda_version":getattr(torch.version,'cuda',None)}
    except Exception as e:
        return {"installed":False,"error":str(e)}

def _disk(path:Path) -> dict[str,Any]:
    try:
        u=shutil.disk_usage(path)
        return {"path":str(path),"total_gb":round(u.total/2**30,1),"free_gb":round(u.free/2**30,1)}
    except Exception as e:
        return {"path":str(path),"error":str(e)}

def report() -> dict[str,Any]:
    data_root=Path(os.getenv('FACELAB_DATA_ROOT',Path.home())).expanduser()
    gpu=_nvidia(); torch=_torch(); engines=engine_status()
    max_vram=max([g.get('vram_total_mb',0) for g in gpu.get('gpus',[])], default=0)
    tiers={
        "portrait_motion":"recommended" if max_vram>=8000 else "limited",
        "pose_motion":"recommended" if max_vram>=16000 else "limited",
        "cinematic_i2v":"recommended" if max_vram>=24000 else ("possible_with_offload" if max_vram>=12000 else "limited"),
    }
    issues=[]
    if not gpu.get('available'): issues.append('No NVIDIA GPU detected by nvidia-smi')
    if not torch.get('installed'): issues.append('PyTorch is not installed in the current server environment')
    if _disk(data_root).get('free_gb',999)<40: issues.append('Less than 40 GB free on FACELAB_DATA_ROOT')
    ready=[e['id'] for e in engines['engines'] if e.get('ready')]
    return {
        "profile":"personal-friends-noncommercial",
        "platform":platform.platform(),"python":sys.version.split()[0],
        "gpu":gpu,"torch":torch,"storage":_disk(data_root),"engines":engines['engines'],
        "ready_engines":ready,"capability_tiers":tiers,"issues":issues,
        "healthy":not any('No NVIDIA' in x for x in issues),
    }

if __name__=='__main__':
    print(json.dumps(report(),ensure_ascii=False,indent=2))

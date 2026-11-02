from __future__ import annotations
import hashlib, json, math, shutil, time
from pathlib import Path
from typing import Any

PALETTE={"blue":"#4CC9F0","cyan":"#72E6FF","green":"#70E000","amber":"#FFB703","orange":"#FB8500","red":"#EF476F","purple":"#B5179E","silver":"#C9D6E3"}

def _job(root:Path, job_id:str)->Path:
    d=root/job_id
    if not d.exists(): raise FileNotFoundError(job_id)
    return d

def _read(p:Path, default:Any):
    try:return json.loads(p.read_text(encoding='utf-8'))
    except Exception:return default

def _write(p:Path,v:Any):
    p.parent.mkdir(parents=True,exist_ok=True); t=p.with_suffix(p.suffix+'.tmp'); t.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf-8'); t.replace(p)

def _plan(d:Path):
    x=_read(d/'plan.json',None)
    if not isinstance(x,dict): raise FileNotFoundError('plan.json')
    return x

def _state(d:Path):
    x=_read(d/'timeline33000_state.json',{})
    if not isinstance(x,dict): x={}
    x.setdefault('playback',{'rate':0.0,'direction':'paused','last_command':'K','playhead_seconds':0.0})
    x.setdefault('color_tags',{})
    x.setdefault('scrub',{'playhead_seconds':0.0,'updated_at':0.0})
    return x

def scrub(root:Path,job_id:str,seconds:float,snap_seconds:float|None=None)->dict:
    d=_job(root,job_id); plan=_plan(d); total=sum(float(s.get('duration_seconds',0) or 0) for s in plan.get('shots',[]))
    t=max(0.0,min(float(seconds),total))
    if snap_seconds is not None and abs(float(snap_seconds)-t)<=0.25: t=float(snap_seconds)
    st=_state(d); st['scrub']={'playhead_seconds':round(t,3),'updated_at':time.time()}; st['playback']['playhead_seconds']=round(t,3); _write(d/'timeline33000_state.json',st)
    active=next((s for s in plan.get('shots',[]) if float(s.get('start_seconds',0))<=t<=float(s.get('end_seconds',0))),None)
    return {'playhead_seconds':round(t,3),'active_shot_id':str(active.get('id')) if active else None,'duration_seconds':round(total,3)}

def playback(root:Path,job_id:str,command:str)->dict:
    d=_job(root,job_id); c=(command or 'K').upper(); st=_state(d); pb=st['playback']; cur=float(pb.get('rate',0) or 0)
    if c=='K': rate=0.0
    elif c=='J': rate=-1.0 if cur>=0 else max(-4.0,cur*2)
    elif c=='L': rate=1.0 if cur<=0 else min(4.0,cur*2)
    else: raise ValueError('command must be J, K or L')
    pb.update({'rate':rate,'direction':'reverse' if rate<0 else 'forward' if rate>0 else 'paused','last_command':c,'updated_at':time.time()}); _write(d/'timeline33000_state.json',st)
    return {'playback':pb}

def color_tag(root:Path,job_id:str,target_type:str,target_id:str,color:str)->dict:
    d=_job(root,job_id); tt=target_type.strip().lower(); c=color.strip().lower()
    if tt not in {'shot','track'}: raise ValueError('target_type must be shot or track')
    if c not in PALETTE: raise ValueError('unsupported color tag')
    if tt=='track' and target_id not in {'video','motion','audio','subtitles'}: raise ValueError('invalid track')
    if tt=='shot' and target_id not in {str(s.get('id')) for s in _plan(d).get('shots',[])}: raise KeyError(target_id)
    st=_state(d); key=f'{tt}:{target_id}'; st['color_tags'][key]={'color':c,'hex':PALETTE[c],'updated_at':time.time()}; _write(d/'timeline33000_state.json',st)
    return {'target_type':tt,'target_id':target_id,'tag':st['color_tags'][key],'all_tags':st['color_tags']}

def curve(root:Path,job_id:str,shot_id:str,bucket:str,property_name:str,samples:int=64)->dict:
    d=_job(root,job_id); shot=next((s for s in _plan(d).get('shots',[]) if str(s.get('id'))==shot_id),None)
    if shot is None: raise KeyError(shot_id)
    name='camera_keyframes' if bucket in {'camera','camera_keyframes'} else 'motion_keyframes' if bucket in {'motion','motion_keyframes'} else None
    if not name: raise ValueError('invalid bucket')
    frames=[]
    for f in shot.get(name,[]) or []:
        vals=f.get('values') or {}
        if property_name in vals: frames.append((float(f.get('at_seconds',0)),float(vals[property_name])))
    frames.sort()
    n=max(8,min(256,int(samples))); dur=max(0.001,float(shot.get('duration_seconds',1) or 1))
    if not frames: return {'shot_id':shot_id,'bucket':name,'property':property_name,'keyframes':[],'curve':[],'sample_count':0}
    out=[]
    for i in range(n):
        t=dur*i/(n-1)
        if t<=frames[0][0]: v=frames[0][1]
        elif t>=frames[-1][0]: v=frames[-1][1]
        else:
            a,b=next((frames[j],frames[j+1]) for j in range(len(frames)-1) if frames[j][0]<=t<=frames[j+1][0]); q=(t-a[0])/max(1e-9,b[0]-a[0]); v=a[1]+(b[1]-a[1])*q
        out.append({'at_seconds':round(t,3),'value':round(v,4)})
    return {'shot_id':shot_id,'bucket':name,'property':property_name,'keyframes':[{'at_seconds':round(t,3),'value':v} for t,v in frames],'curve':out,'sample_count':len(out)}

def proxy_cache(root:Path,job_id:str,action:str='status')->dict:
    d=_job(root,job_id); cache=d/'proxy_cache'; cache.mkdir(exist_ok=True)
    if action=='prune':
        removed=0
        for f in cache.glob('*.mp4'):
            try:f.unlink();removed+=1
            except Exception:pass
        return {'action':'prune','removed':removed,'entries':0}
    files=[]
    for f in sorted(cache.glob('*.mp4')):
        files.append({'name':f.name,'bytes':f.stat().st_size,'modified_at':f.stat().st_mtime})
    return {'action':'status','entries':len(files),'bytes':sum(x['bytes'] for x in files),'items':files}

def cache_existing_proxy(root:Path,job_id:str,start:float|None,end:float|None,width:int,height:int)->dict:
    d=_job(root,job_id); src=d/'timeline_range_proxy.mp4'
    if not src.exists() or src.stat().st_size<=0: return {'cached':False,'reason':'proxy not ready'}
    raw=f'{start}|{end}|{width}|{height}|{src.stat().st_size}'.encode(); key=hashlib.sha256(raw).hexdigest()[:20]
    out=d/'proxy_cache'/f'{key}.mp4'; out.parent.mkdir(exist_ok=True)
    if not out.exists(): shutil.copy2(src,out)
    return {'cached':True,'cache_key':key,'path':out.name,'bytes':out.stat().st_size}

def studio(root:Path,job_id:str)->dict:
    d=_job(root,job_id); st=_state(d)
    return {'stage':33000,'playback':st['playback'],'scrub':st['scrub'],'color_tags':st['color_tags'],'palette':PALETTE,'proxy_cache':proxy_cache(root,job_id)}

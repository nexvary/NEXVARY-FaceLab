CAMERA_PRESETS={
 'portrait_closeup':{'camera':'85mm close-up, eye-level, subtle push-in','motion':'idle_natural'},
 'hero_orbit':{'camera':'35mm slow 20-degree orbit, stable horizon','motion':'hero_pose'},
 'walk_follow':{'camera':'35mm gimbal follow, medium full-body framing','motion':'walk_forward'},
 'travel_reveal':{'camera':'24mm wide reveal, slow dolly out','motion':'travel_reveal'},
 'dialogue_medium':{'camera':'50mm medium shot, stable eyeline','motion':'talking'},
 'reaction_cut':{'camera':'70mm reaction close-up, minimal drift','motion':'reaction'},
 'cinematic_turn':{'camera':'50mm three-quarter profile, slow arc','motion':'cinematic_turn'},
 'documentary_handheld':{'camera':'35mm restrained documentary handheld','motion':'idle_natural'},
}
TRANSITIONS={'cut':{'seconds':0.0},'fade':{'seconds':0.5},'dissolve':{'seconds':0.7},'dip_black':{'seconds':0.4},'match_cut':{'seconds':0.0}}
def catalog(): return {'camera_presets':CAMERA_PRESETS,'transitions':TRANSITIONS,'count':len(CAMERA_PRESETS)}

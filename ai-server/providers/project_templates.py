TEMPLATES={
 'friend_portrait':{'title':'Friend Portrait','preset':'cinematic','duration_seconds':30,'aspect_ratio':'9:16','motion_preset':'idle_natural','story':'A warm cinematic portrait with natural expression and subtle movement.'},
 'travel_memory':{'title':'Travel Memory','preset':'travel','duration_seconds':45,'aspect_ratio':'16:9','motion_preset':'travel_reveal','story':'A joyful travel memory with environmental reveals and natural walking.'},
 'historical_scene':{'title':'Historical Scene','preset':'historical','duration_seconds':60,'aspect_ratio':'16:9','motion_preset':'cinematic_turn','story':'A respectful historical-style scene with measured movement and stable identity.'},
 'talking_portrait':{'title':'Talking Portrait','preset':'documentary','duration_seconds':30,'aspect_ratio':'9:16','motion_preset':'talking','story':'A talking portrait framed naturally for dialogue.'},
 'hero_clip':{'title':'Hero Clip','preset':'advert','duration_seconds':30,'aspect_ratio':'16:9','motion_preset':'hero_pose','story':'A polished hero reveal with confident pose and cinematic camera.'},
}
def catalog(): return {'schema_version':1,'count':len(TEMPLATES),'templates':TEMPLATES,'personal_noncommercial':True}
def get(name): return TEMPLATES.get(name)

from pathlib import Path
from PIL import Image, ImageEnhance, ImageFilter

HERITAGE_SITES = {
    "eg_giza": ("Egypt", "Giza", "Giza Pyramids & Sphinx"),
    "eg_luxor": ("Egypt", "Luxor", "Luxor & Karnak"),
    "eg_abusimbel": ("Egypt", "Aswan", "Abu Simbel"),
    "eg_philae": ("Egypt", "Aswan", "Philae Temple"),
    "eg_cairo": ("Egypt", "Cairo", "Historic Cairo"),
    "eg_qaitbay": ("Egypt", "Alexandria", "Qaitbay Citadel"),
    "eg_saqqara": ("Egypt", "Giza", "Saqqara & Step Pyramid"),
    "eg_valleykings": ("Egypt", "Luxor", "Valley of the Kings"),
    "tr_istanbul": ("Türkiye", "Istanbul", "Historic Istanbul"),
    "tr_cappadocia": ("Türkiye", "Nevşehir", "Cappadocia"),
    "tr_ephesus": ("Türkiye", "İzmir", "Ephesus"),
    "tr_pamukkale": ("Türkiye", "Denizli", "Pamukkale & Hierapolis"),
    "tr_nemrut": ("Türkiye", "Adıyaman", "Mount Nemrut"),
    "tr_sumela": ("Türkiye", "Trabzon", "Sumela Monastery"),
    "tr_troy": ("Türkiye", "Çanakkale", "Troy"),
    "tr_ani": ("Türkiye", "Kars", "Archaeological Site of Ani"),
    "dz_timgad": ("Algeria", "Batna", "Timgad"),
    "dz_djemila": ("Algeria", "Sétif", "Djémila"),
    "dz_tipasa": ("Algeria", "Tipaza", "Tipasa"),
    "dz_kasbah": ("Algeria", "Algiers", "Kasbah of Algiers"),
    "dz_mzab": ("Algeria", "Ghardaïa", "M'Zab Valley"),
    "dz_tassili": ("Algeria", "Djanet", "Tassili n'Ajjer"),
    "dz_benihammad": ("Algeria", "M'Sila", "Beni Hammad Fort"),
    "dz_constantine": ("Algeria", "Constantine", "Constantine Bridges"),
    "sa_alula": ("Saudi Arabia", "AlUla", "Hegra / Madain Salih"),
    "sa_diriyah": ("Saudi Arabia", "Riyadh", "At-Turaif, Diriyah"),
    "sa_jeddah": ("Saudi Arabia", "Jeddah", "Historic Jeddah"),
    "sa_hail": ("Saudi Arabia", "Ha'il", "Rock Art in Ha'il"),
    "sa_ahsa": ("Saudi Arabia", "Al-Ahsa", "Al-Ahsa Oasis"),
    "sa_hima": ("Saudi Arabia", "Najran", "Hima Cultural Area"),
    "sa_riyadh": ("Saudi Arabia", "Riyadh", "Masmak Fortress"),
    "sa_kaaba": ("Saudi Arabia", "Makkah", "Masjid al-Haram"),
    "ma_marrakech": ("Morocco", "Marrakech", "Medina of Marrakech"),
    "ma_fes": ("Morocco", "Fes", "Medina of Fez"),
    "ma_aitbenhaddou": ("Morocco", "Ouarzazate", "Ait Ben Haddou"),
    "ma_essaouira": ("Morocco", "Essaouira", "Medina of Essaouira"),
    "ma_volubilis": ("Morocco", "Meknes", "Volubilis"),
    "ma_rabat": ("Morocco", "Rabat", "Historic Rabat"),
    "ma_chefchaouen": ("Morocco", "Chefchaouen", "Old Chefchaouen"),
    "ma_hassan": ("Morocco", "Rabat", "Hassan Tower"),
    "tn_carthage": ("Tunisia", "Tunis", "Carthage"),
    "tn_eljem": ("Tunisia", "El Jem", "Amphitheatre of El Jem"),
    "tn_kairouan": ("Tunisia", "Kairouan", "Kairouan"),
    "tn_tunis": ("Tunisia", "Tunis", "Medina of Tunis"),
    "tn_dougga": ("Tunisia", "Béja", "Dougga"),
    "tn_sousse": ("Tunisia", "Sousse", "Medina of Sousse"),
    "tn_bizerte": ("Tunisia", "Bizerte", "Old Bizerte"),
    "tn_matmata": ("Tunisia", "Gabès", "Matmata"),
    "it_rome": ("Italy", "Rome", "Colosseum & Historic Rome"),
    "it_venice": ("Italy", "Venice", "Venice"),
    "it_florence": ("Italy", "Florence", "Historic Florence"),
    "it_pisa": ("Italy", "Pisa", "Leaning Tower of Pisa"),
    "it_pompeii": ("Italy", "Naples", "Pompeii"),
    "it_matera": ("Italy", "Matera", "Sassi of Matera"),
    "it_siena": ("Italy", "Siena", "Historic Siena"),
    "it_verona": ("Italy", "Verona", "Historic Verona"),
    "es_alhambra": ("Spain", "Granada", "Alhambra"),
    "es_sagrada": ("Spain", "Barcelona", "Sagrada Familia"),
    "es_toledo": ("Spain", "Toledo", "Historic Toledo"),
    "es_cordoba": ("Spain", "Cordoba", "Historic Cordoba"),
    "es_seville": ("Spain", "Seville", "Historic Seville"),
    "es_segovi": ("Spain", "Segovia", "Aqueduct of Segovia"),
    "es_avila": ("Spain", "Avila", "Walls of Avila"),
    "es_santiago": ("Spain", "Santiago", "Santiago de Compostela"),
    "gr_acropolis": ("Greece", "Athens", "Acropolis of Athens"),
    "gr_delphi": ("Greece", "Delphi", "Delphi"),
    "gr_meteora": ("Greece", "Thessaly", "Meteora"),
    "gr_olympia": ("Greece", "Olympia", "Ancient Olympia"),
    "gr_mycenae": ("Greece", "Argolis", "Mycenae"),
    "gr_epidaurus": ("Greece", "Argolis", "Epidaurus"),
    "gr_rhodes": ("Greece", "Rhodes", "Medieval City of Rhodes"),
    "gr_santorini": ("Greece", "Santorini", "Santorini")
}

def status():
    try:
        import rembg # noqa
        ready=True
    except Exception: ready=False
    return {"ready":ready,"engine":"rembg + local compositor","paid_api_required":False,"sites":len(HERITAGE_SITES)}

def composite(person:Path, background:Path, work:Path, mode:str="portrait", lighting:str="day", strength:float=.7)->Path:
    try:
        from rembg import remove
    except Exception as exc:
        raise RuntimeError("Heritage compositor requires rembg. Install rembg[cpu] in the FaceLab venv.") from exc
    p=Image.open(person).convert("RGBA"); bg=Image.open(background).convert("RGB")
    # cover-crop background to person aspect
    pw,ph=p.size; bw,bh=bg.size; target=pw/ph; current=bw/bh
    if current>target:
        nw=int(bh*target); left=(bw-nw)//2; bg=bg.crop((left,0,left+nw,bh))
    else:
        nh=int(bw/target); top=(bh-nh)//2; bg=bg.crop((0,top,bw,top+nh))
    bg=bg.resize((pw,ph),Image.Resampling.LANCZOS)
    if lighting=="sunset":
        bg=ImageEnhance.Color(bg).enhance(1.15); bg=ImageEnhance.Brightness(bg).enhance(.92)
    elif lighting=="night":
        bg=ImageEnhance.Brightness(bg).enhance(.55); bg=ImageEnhance.Contrast(bg).enhance(1.08)
    if mode=="portrait": bg=bg.filter(ImageFilter.GaussianBlur(radius=max(1,int(min(pw,ph)*.006*strength))))
    fg=remove(p); alpha=fg.getchannel("A").filter(ImageFilter.GaussianBlur(radius=max(.4,1.2*strength)))
    # subtle color/light adaptation without altering identity geometry
    rgb=fg.convert("RGB"); factor=.92 if lighting=="night" else (1.03 if lighting=="day" else .98); rgb=ImageEnhance.Brightness(rgb).enhance(factor)
    fg=rgb.convert("RGBA"); fg.putalpha(alpha)
    out=Image.alpha_composite(bg.convert("RGBA"),fg).convert("RGB")
    path=work/"heritage_result.jpg"; out.save(path,"JPEG",quality=95); return path

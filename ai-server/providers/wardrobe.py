import os
import shlex
import subprocess
from pathlib import Path
from typing import Optional

from providers.diffusion_face import run_wardrobe_diffusion

WARDROBE_STYLES = {
    "formal_black_suit": "classic tailored black suit, white dress shirt, elegant formal styling",
    "formal_navy_suit": "tailored navy blue suit, crisp shirt, elegant formal styling",
    "formal_grey_suit": "tailored grey suit, refined formal styling",
    "formal_tuxedo": "black tuxedo, white formal shirt, black bow tie, premium evening tailoring",
    "formal_white_shirt": "crisp white dress shirt, professional formal look",
    "formal_blazer": "luxury tailored blazer, premium smart formal styling",
    "formal_beige_suit": "tailored beige suit, modern elegant formal styling",
    "formal_pinstripe": "refined pinstripe suit, premium business tailoring",
    "formal_three_piece": "tailored three-piece suit with waistcoat, elegant formal styling",
    "casual_white_tee": "clean white t-shirt, modern casual look",
    "casual_black_tee": "clean black t-shirt, modern casual look",
    "casual_denim_jacket": "blue denim jacket, modern casual styling",
    "casual_hoodie": "premium casual hoodie, realistic fabric",
    "casual_polo": "smart polo shirt, clean casual styling",
    "casual_flannel": "plaid flannel shirt, relaxed casual styling",
    "casual_bomber": "modern bomber jacket, premium casual styling",
    "casual_cardigan": "refined cardigan, relaxed smart casual look",
    "casual_chino": "smart casual outfit with chino styling",
    "sport_tracksuit": "modern athletic tracksuit, realistic sportswear fabric",
    "sport_gym": "premium gym training outfit, modern athletic wear",
    "sport_running": "technical running outfit, lightweight athletic fabric",
    "sport_football": "professional football kit, realistic sports jersey",
    "sport_tennis": "clean modern tennis outfit, premium sportswear",
    "sport_basketball": "basketball sports kit, realistic jersey and athletic styling",
    "sport_cycling": "technical cycling outfit, fitted performance sportswear",
    "sport_yoga": "modern yoga fitness outfit, premium stretch fabric",
    "sport_windbreaker": "lightweight sport windbreaker, modern athletic styling",
    "summer_linen": "light linen shirt, breathable premium summer styling",
    "summer_hawaiian": "tasteful printed summer shirt, relaxed vacation styling",
    "summer_resort": "premium resort outfit, elegant relaxed summer styling",
    "summer_light_dress": "light elegant summer dress, flowing realistic fabric",
    "summer_polo": "light summer polo shirt, clean warm-weather styling",
    "summer_shorts": "tasteful summer outfit with tailored shorts",
    "summer_white": "elegant all-white summer outfit, premium light fabric",
    "summer_coastal": "refined coastal summer style, linen and light layers",
    "summer_safari": "modern safari-inspired summer outfit, neutral earth tones",
    "winter_wool_coat": "premium wool winter coat, elegant cold-weather styling",
    "winter_puffer": "modern puffer jacket, realistic quilted winter fabric",
    "winter_trench": "classic trench coat, refined winter styling",
    "winter_turtleneck": "premium turtleneck sweater, elegant winter styling",
    "winter_leather": "premium leather jacket, realistic leather texture",
    "winter_parka": "modern winter parka, realistic insulated fabric",
    "winter_scarf": "elegant winter coat with scarf, layered cold-weather styling",
    "winter_knit": "premium winter knit sweater, realistic knitted texture",
    "winter_shearling": "shearling-lined winter jacket, premium realistic texture",
    "traditional_galabeya": "elegant Egyptian galabeya, refined traditional fabric and tailoring",
    "traditional_thobe": "elegant Arab thobe, premium traditional tailoring",
    "traditional_abaya": "elegant abaya, flowing premium fabric, tasteful traditional styling",
    "traditional_kaftan": "elegant kaftan, detailed traditional textile",
    "traditional_turkish": "elegant traditional Turkish-inspired outfit, refined heritage textile",
    "traditional_moroccan": "elegant Moroccan traditional outfit, detailed premium textile",
    "traditional_gulf": "refined Gulf formal traditional wear, premium fabric",
    "traditional_ottoman": "tasteful Ottoman-inspired heritage outfit, premium historical textile styling",
    "traditional_folk": "elegant folk-inspired traditional outfit, refined heritage details",
    "business_smart": "smart business casual outfit, modern professional styling",
    "business_shirt_tie": "professional dress shirt and tie, polished business look",
    "business_blazer": "professional business blazer, refined corporate styling",
    "business_tech": "modern technology professional outfit, clean premium smart casual styling",
    "business_creative": "creative professional outfit, sophisticated modern styling",
    "business_executive": "premium executive outfit, authoritative refined tailoring",
    "business_corporate": "polished corporate attire, professional premium styling",
    "business_startup": "modern startup professional outfit, clean smart casual look",
    "business_consultant": "refined consultant business outfit, premium professional styling",
    "evening_black_dress": "elegant black evening dress, premium formal fabric",
    "evening_red_dress": "elegant red evening dress, premium formal fabric",
    "evening_gown": "luxurious long evening gown, elegant flowing fabric",
    "evening_dinner_suit": "refined dinner suit, premium evening tailoring",
    "evening_velvet": "elegant velvet blazer, premium evening styling",
    "evening_cocktail": "sophisticated cocktail outfit, tasteful evening styling",
    "evening_gold": "elegant gold-toned formal evening outfit, tasteful premium styling",
    "evening_silver": "elegant silver-toned formal evening outfit, tasteful premium styling",
    "evening_premium": "premium luxury event outfit, sophisticated formal styling",
}


def _external_fashn_ready() -> bool:
    return bool(os.getenv("FASHN_VTON_COMMAND", "").strip())


def _run_external_fashn(person: Path, garment: Path, category: str, out: Path) -> Path:
    template = os.getenv("FASHN_VTON_COMMAND", "").strip()
    if not template:
        raise RuntimeError("FASHN_VTON_COMMAND is not configured")
    cmd = template.format(person=str(person), garment=str(garment), category=category, output=str(out))
    proc = subprocess.run(shlex.split(cmd), stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=int(os.getenv("FASHN_VTON_TIMEOUT", "900")))
    if proc.returncode != 0 or not out.exists():
        tail = "\n".join(proc.stdout.splitlines()[-30:])
        raise RuntimeError(f"External VTON engine failed: {tail}")
    return out


def run_wardrobe(person: Path, style_id: str, category: str, strength: float, work: Path, garment: Optional[Path] = None, color: str = "auto", accessory: str = "none", occasion: str = "work", outfit: str = "") -> Path:
    if style_id not in WARDROBE_STYLES:
        raise ValueError(f"Unknown wardrobe style: {style_id}")
    if garment is not None and _external_fashn_ready():
        out = work / "wardrobe-result.jpg"
        return _run_external_fashn(person, garment, category, out)
    prompt = WARDROBE_STYLES[style_id]
    if color and color != "auto":
        prompt += f", primary garment color {color}"
    if accessory and accessory != "none":
        prompt += f", coordinated accessory: {accessory}"
    if occasion:
        prompt += f", styling appropriate for {occasion}"
    if outfit:
        prompt += f", coordinated multi-piece outfit: {outfit}"
    return run_wardrobe_diffusion(person, prompt, strength, work, garment)


def wardrobe_status() -> dict:
    return {
        "ready": os.getenv("ENABLE_GENERATIVE", "0") == "1" or _external_fashn_ready(),
        "engine": "external FASHN-VTON adapter" if _external_fashn_ready() else "FaceLab Diffusers wardrobe fallback",
        "external_vton_configured": _external_fashn_ready(),
        "garment_reference_supported": True,
        "catalog_styles": len(WARDROBE_STYLES),
        "categories": 8,
        "outfit_composition": True,
        "ai_stylist": True,
        "multi_look_recommendations": True,
        "independent_piece_composition": True,
        "saved_dressing_rooms": True,
        "color_variants": True,
        "accessories": ["glasses", "hat", "watch", "scarf", "shoes", "bag", "necklace"],
        "fee": "none/self-hosted",
        "license_note": "FASHN VTON integration is command-adapter based. Audit and approve the exact local code and weight licenses before redistribution or commercial release.",
    }

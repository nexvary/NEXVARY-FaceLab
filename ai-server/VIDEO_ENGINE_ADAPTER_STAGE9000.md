# Local video engine adapter — Stage 9000

FaceLab never embeds a paid inference API. The render pipeline calls a local process using `FACELAB_VIDEO_ENGINE_CMD_JSON` with `shell=False`.

Place an exact, separately license-audited image-to-video model in local storage and configure the adapter. The included `adapters/cogvideox_i2v.py` is an integration adapter only; it does not contain or download model weights.

Supported placeholders: `{input}`, `{output}`, `{prompt}`, `{duration}`, `{preset}`, `{seed}`, `{aspect_ratio}`.

The final commercial release must record the exact code commit, exact model-weight artifact/hash, license, source, and any attribution/restrictions before redistributing any weights.

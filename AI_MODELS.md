# NEXVARY FaceLab — Generative model plan

Stage 0.3 separates the Android UI from heavy GPU inference. The app can stay lightweight while a local PC/server performs high-quality generation.

Recommended open-source research projects:

- SAM — Style-based Age Manipulation: age transformation from a single face image.
  https://github.com/yuval-alaluf/SAM
- e4e — Encoder for Editing: real-face inversion to editable StyleGAN latent space.
  https://github.com/omertov/encoder4editing
- HyperStyle — high-fidelity inversion + age/pose/smile edits and StyleCLIP integration.
  https://github.com/yuval-alaluf/hyperstyle
- StyleCLIP — text-driven StyleGAN editing; useful for beard, smile, hair color and semantic edits.
  https://github.com/orpatashnik/StyleCLIP
- HairFastGAN — modern high-resolution hairstyle transfer.
  https://github.com/FusionBrainLab/HairFastGAN
- HairCLIP — hairstyle and hair-color editing from text/reference images.
  https://github.com/wtybest/HairCLIP

Important: verify each model/checkpoint/data license before commercial distribution. Research-code licensing and pretrained-weight licensing may differ.

## Architecture

Android app -> HTTPS /v1/edit -> GPU AI bridge -> provider adapter -> output image.

For production, run the bridge behind authentication, TLS, request-size limits, deletion-after-processing, and explicit user consent for face uploads.

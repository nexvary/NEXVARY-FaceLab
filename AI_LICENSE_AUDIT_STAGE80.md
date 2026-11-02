# AI License Audit — Stage 80

## Current design rule

FaceLab uses only self-hosted inference in the default architecture; there is no paid cloud API dependency. Self-hosting removes per-request API fees, but hardware/electricity/hosting costs can still exist.

## Components reviewed

- **Hugging Face Diffusers** — framework code is Apache-2.0. The selected base model has its own license and must be reviewed separately.
- **IP-Adapter** — upstream project code declares Apache-2.0. Exact adapter weights and the base diffusion model used with them must still be reviewed before commercial redistribution.
- **rembg** — application/library code is MIT. The exact segmentation model asset selected by rembg must be audited separately; Stage 80 flags this explicitly instead of assuming the code license covers every weight.
- **U-2-Net upstream** — repository is Apache-2.0, but provenance/license of converted downstream ONNX assets should be confirmed when shipping those exact binaries.

## Commercial release gate

Do not bundle or auto-download a checkpoint in a commercial release until its source, model card/license, redistribution terms, and any inherited dataset/model restrictions have been recorded.

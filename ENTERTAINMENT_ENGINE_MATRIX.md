# NEXVARY FaceLab Stage 9500 — Entertainment Engine Matrix

Profile: **private personal/friends, non-commercial entertainment**.

Stage 9500 no longer rejects research/non-commercial model assets solely because they are non-commercial. It still keeps each third-party engine isolated and records its license because redistribution and model licenses remain separate issues.

| Engine | Role in Film Studio | Default routing |
|---|---|---|
| Wan 2.2 | General cinematic image-to-video | wide/general/action shots |
| MimicMotion | Pose-guided body animation | walking, gestures, pose shots |
| LivePortrait | Face/head/expression animation | close-ups and reactions |
| EchoMimic V2 | Audio-driven semi-body animation | dialogue/talking shots |
| MuseTalk | Lip-sync post-processing | dialogue finishing |
| FFmpeg | Assembly/post | all final films |

## Safety/architecture rule
Third-party repositories are **not copied into this source package**. Each engine lives in its own folder/environment. FaceLab communicates with it through an argv JSON adapter and never through `shell=True`.

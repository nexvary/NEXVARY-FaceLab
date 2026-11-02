# Local Image-to-Video Adapter — Stage 4000

FaceLab deliberately does not hard-code one research model command because CLI layouts and weight licenses vary by exact snapshot. Stage 4000 instead exposes a safe adapter configured with `FACELAB_VIDEO_ENGINE_CMD_JSON`.

Example shape:

```text
["python","D:/AI/video-engine/render.py","--image","{input}","--prompt","{prompt}","--seconds","{duration}","--output","{output}"]
```

Supported placeholders: `{input}`, `{output}`, `{prompt}`, `{duration}`, `{preset}`.

The command is parsed as a JSON array and executed without a shell. This prevents the story/prompt text from becoming a shell command. FaceLab checks that the adapter produces a non-empty `output.mp4` and reports failures through the film job status endpoint.

Only configure an adapter after auditing the exact code revision, model weights, dependencies and commercial-use terms you plan to deploy.

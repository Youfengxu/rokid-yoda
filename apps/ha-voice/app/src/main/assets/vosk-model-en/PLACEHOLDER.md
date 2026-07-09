# Put the Vosk model here

This folder must contain an unpacked Vosk model (the glasses-mic STT). It is NOT
committed (models are ~40 MB) — download it yourself:

1. Get a small English model, e.g. `vosk-model-small-en-us-0.15` from
   https://alphacephei.com/vosk/models
2. Unzip it and copy its CONTENTS into this folder, so you have:
     app/src/main/assets/vosk-model-en/
       am/  conf/  graph/  ivector/  ...  README
3. The folder name must match `Config.VOSK_MODEL_ASSET` ("vosk-model-en").

Glasses PCM is 16 kHz mono 16-bit — the small English model matches. Skip this only
if you set `Config.MIC_SOURCE = PHONE` (phone-mic STT needs no model).

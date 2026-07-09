# Put the on-glasses companion APK here as `glass.apk`

This phone app installs the on-glasses companion (apps/havoice-glass) onto the
glasses via CXR-L (`appUploadAndInstall`). It reads the APK from this assets folder
as **`glass.apk`**. That file is gitignored (build output) — produce it yourself:

```bash
# from repo root
cd apps/havoice-glass
./gradlew assembleRelease   # or assembleDebug
cp app/build/outputs/apk/release/app-release-unsigned.apk \
   ../ha-voice/app/src/main/assets/glass.apk
# (a debug APK works too; sign per your device's install policy)
```

Then rebuild the phone app so `glass.apk` is bundled. The package inside must be
`com.rokidyoda.havoiceglass` (matches `Config.GLASS_PACKAGE` / `GLASS_ENTRY`).

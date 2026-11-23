# Seamless

Seamless is an Android local-music player starter with an AMOLED-inspired black interface, a playback foreground service, and a home-screen widget.

## Build with GitHub Actions (phone-friendly)
1. Create/open the `Seamless` repository.
2. Upload the **contents** of this ZIP to the repository root (the `.github` and `app` folders must be at the root).
3. Commit to `main`.
4. Open **Actions → Build Seamless APK** and wait for the run to finish.
5. Download the `Seamless-APK` artifact from the successful run.

The workflow installs Android SDK platform 35 and build-tools 35.0.0 before building. It uses JDK 17 and Gradle 8.9.

## Included
- Local audio library with Android-version-aware storage permission requests
- Foreground playback service and persistent playback notification
- Play/pause, previous/next (wraps through the library), and track selection
- Home-screen widget controls wired to the playback service
- Accent color selection persisted across launches
- Existing Seamless artwork retained as the launcher icon

## Important status
This is an improved source project, not a certified or device-tested release. Build it with Actions and install the APK on a device to verify behavior. Lyrics display/synchronization, crossfade/gapless transitions, advanced Magic Shuffle, audio visualization, and a full theme studio are not implemented. Playback notification controls are basic; a full Android media-session implementation remains a future improvement.

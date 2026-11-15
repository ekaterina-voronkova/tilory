# Findings log

Detailed release history. Issues are the index; this file is the detail.

## v0.1.0 — first playable version (#12)
- Commit: 55a8468. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.1.0
- Build: local `gradle :app:assembleDebug` with the Android SDK at `D:\Android\Sdk` (platform 35, build-tools 35),
  Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21, JDK 21. No CI yet.
- Signing: debug key of the build computer. An update installs over the old app only if it is signed with the same
  key, so release builds must keep coming from the same keystore.
- What is in it: Own pick (Photo Picker, file picker, Share), grid presets and custom size limited by a test minimum
  tile size, drag with displacing and gluing, edge shift, reference image (off / peek / always), skip with undo,
  completion screen with zoom and time, History, settings, progress saved to one JSON file.
- Checked: 11 unit tests on the board logic pass; the app compiles. Not checked: running on any device (no emulator
  or phone was connected).
- Decisions made while building: the photo is not cropped (tiles may be slightly rectangular); displaced tiles walk
  back against the drag direction into the freed cells; storage is a JSON file instead of Room for now.

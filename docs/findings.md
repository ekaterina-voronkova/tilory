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

## v0.1.1 — thinner seams (#15)
- Commit: 5a466ac. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.1.1
- Change: seam width in `BoardView` 1.5 dp → 1 dp (#14), versionCode 2.
- Build: local, same toolchain and debug key as v0.1.0.
- Checked: unit tests pass, APK builds. Not checked: how it looks on the phone.

## v0.2.0 — four levels, fixed tile size, vibration (#19)
- Commit: 6914cee. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.2.0
- Changes: presets 12 / 30 / 63 / 108 (#16); minimum tile fixed at 40 dp, slider removed (#17); glue vibration via
  the vibrator with media usage instead of View haptic feedback, which the system drops when touch feedback is off
  (#18); two lint errors fixed. versionCode 3.
- Build: local, same toolchain and debug key. `gradle clean` failed on a locked build folder; the build without
  clean succeeded.
- Checked: 12 unit tests, lint without errors, APK badging shows 0.2.0 and the VIBRATE permission. Not checked: on the
  phone.

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

## v0.3.0 — self-update (#21)
- Commit: f8f6d49. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.3.0
- Change: self-update from GitHub releases (#20) — check once per launch (`/releases?per_page=10`, pre-releases
  included, numeric compare), DownloadManager into external files `update/`, id and version in SharedPreferences,
  0.5 s polling while on screen, DOWNLOAD_COMPLETE receiver with a "tap to install" notification, onResume fallback,
  package/version check before the FileProvider hand-off. New permissions: INTERNET, REQUEST_INSTALL_PACKAGES,
  POST_NOTIFICATIONS. versionCode 4.
- Build: local, same toolchain and debug key. A lint run failed once on a jar locked by another process; it passed
  after `gradle --stop`.
- Checked: 15 unit tests (version compare, release pick), lint without errors, APK badging shows 0.3.0 and the new
  permissions, the live releases API returns the fields the parser reads. Not checked: any of it on the phone — the
  updater can first be tried when a release newer than 0.3.0 exists.
- Retention: v0.1.0 release deleted (tag kept); on GitHub now v0.3.0, v0.2.0, v0.1.1.

## v0.3.1 — steady completion screen (#23)
- Commit: 1b50310. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.3.1
- Change: completion screen reuses the puzzle layout so the photo does not jump; solve time only in History (#22).
  versionCode 5.
- Build: local, same toolchain and debug key.
- Checked: 15 unit tests, APK badging 0.3.1. Not checked: on the phone. First release reachable by self-update (#20).
- Retention: v0.1.1 release deleted (tag kept); on GitHub now v0.3.1, v0.3.0, v0.2.0.

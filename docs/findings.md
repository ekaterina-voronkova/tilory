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

## v0.4.0 — watercolour icon, Tilory colours, completion buttons (#26)
- Commit: 31bf192. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.4.0
- Changes: adaptive icon from the owner's watercolour (layers made with ChatGPT, scaled to 60 dp of 108 dp; Play icon in
  store/icon-512.png) (#24); fixed palette from the painting with a wallpaper-colour option (#25); completion buttons
  in one bottom row over the photo area, so the photo does not move (#22). versionCode 6.
- Build: local, same toolchain and debug key.
- Checked: 15 unit tests, lint clean, APK badging 0.4.0 with the adaptive icon. Not checked: on the phone.
- Retention: v0.2.0 release deleted (tag kept); on GitHub now v0.4.0, v0.3.1, v0.3.0.

## v0.4.1 — game row at the bottom (#28)
- Commit: e1a8185. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.4.1
- Change: puzzle and completion screens share one layout with a 56 dp bottom row; completion Next puzzle centred
  (#27). versionCode 7.
- Build: local, same toolchain and debug key.
- Checked: 15 unit tests, APK badging 0.4.1. Not checked: on the phone.
- Retention: v0.3.0 release deleted (tag kept); on GitHub now v0.4.1, v0.4.0, v0.3.1.

## v0.4.2 — landscape icon, theme colours again (#31)
- Commit: 9a9d974. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.4.2
- Changes: flat landscape icon from the owner's ChatGPT set (#29); Tilory palette and colours switch removed, theme
  colours again (#30). versionCode 8.
- Build: local, same toolchain and debug key.
- Checked: 15 unit tests, lint clean, APK badging 0.4.2. Not checked: on the phone.
- Retention: v0.3.1 release deleted (tag kept); on GitHub now v0.4.2, v0.4.1, v0.4.0.

## v0.5.0 — new main screen and launch splash (#37)
- Commit: 09f4c45. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.5.0
- Changes: picture title (#32), Android 12+ splash with icon and branding, window colour instead of the old beige
  (#33), watercolour background cropped at the sides, dimmed in dark mode (#34), buttons at the bottom (#35),
  untranslated italic tagline (#36). APK grew by ~1.2 MB (background JPEG, title PNG). versionCode 9.
- Build: local, same toolchain and debug key.
- Checked: 15 unit tests, lint clean, APK badging 0.5.0. Not checked: on the phone.
- Retention: v0.4.0 release deleted (tag kept); on GitHub now v0.5.0, v0.4.2, v0.4.1.

## v0.6.0 — Surprise from phone folders (#40)
- Commit: e507047. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.6.0
- Changes:
  - Surprise from folders picked with ACTION_OPEN_DOCUMENT_TREE (#4): random pick without repeats, with solved
    documents and photo hashes kept in `AppData.surprise`. No photo catalogue: the folders are listed on each pick.
  - After a skip, a dialog asks why, with Undo; hidden photos can be restored.
  - Surprise has its own difficulty and reference image in Settings.
  - The recent-apps thumbnail is hidden on Android 13+ (`setRecentsScreenshotEnabled`, not FLAG_SECURE, so
    screenshots still work).
  - Clear History and a "Keep history" setting (#38).
  - Landscape photos turn the screen, with the game bar at the side (#39).
  - versionCode 10.
- Build: local, same toolchain and debug key. `gh release create --target` refused the short hash; the full hash
  worked.
- Checked: 19 unit tests (4 new for the Surprise pick), lint clean, APK badging 0.6.0. Not checked: on the phone.
- Retention: v0.4.1 release deleted (tag kept); on GitHub now v0.6.0, v0.5.0, v0.4.2.

## v0.7.0 — random folder first, shooting date, river-blue main buttons (#45)
- Commit: 4848fde. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.7.0
- Changes:
  - Random photo (Surprise) picks a random folder first, then a random photo in it, so every folder is equally
    likely (#42).
  - The EXIF shooting date (`DateTimeOriginal`, falling back to `DateTimeDigitized`) is read from the original at
    import with androidx.exifinterface 1.3.7. It is shown on the completion screen and in the History viewer; there
    is no file-date fallback (#43).
  - Main screen buttons in the logo's river blue (#3FBEEC, dark text #0B2F45), ordered Random photo, Pick a photo,
    From folders and cloud. "Surprise" is renamed to "Random photo" in all visible text (#44).
  - versionCode 11.
- Build: local, same toolchain and debug key.
- Checked: 23 unit tests, lint clean, APK badging 0.7.0. Not checked: on the phone.
- Retention: v0.4.2 release deleted (tag kept); on GitHub now v0.7.0, v0.6.0, v0.5.0.

## v0.7.1 — logo-style main buttons, shooting date above the buttons (#48)
- Commit: 671db4d. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.7.1
- Changes:
  - The main screen buttons are drawn like the logo letters (#46):
    - colours: sky #7DCFFA, light mountains #7C9EB8, river #3FBEEC;
    - a 2 dp cream outline #FBEEDE;
    - a soft 6 dp shadow;
    - dark text.
  - The completion screen's shooting date moved from the top of the photo area to the bottom, above the buttons
    (#47).
  - versionCode 12.
- Build: local, same toolchain and debug key.
- Checked: 23 unit tests, lint clean, APK badging 0.7.1. Not checked: on the phone.
- Retention: v0.5.0 release deleted (tag kept); on GitHub now v0.7.1, v0.7.0, v0.6.0.

## v0.7.2 — swapped button colours, lighter dark-mode dim (#51)
- Commit: 2c90ecc. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.7.2
- Changes:
  - "Pick a photo" is now river blue and "From folders and cloud" light-mountain blue (#49). The owner chose this
    from a mockup drawn with System.Drawing over the real background and logo.
  - The dark-mode scrim on the main screen painting went from 55% to 40% (#50).
  - versionCode 13.
- Build: local, same toolchain and debug key.
- Checked: 23 unit tests, lint clean, APK badging 0.7.2. Not checked: on the phone.
- Retention: v0.6.0 release deleted (tag kept); on GitHub now v0.7.2, v0.7.1, v0.7.0.

## v0.7.3 — studio logo at launch, portrait picker after a landscape puzzle (#54)
- Commit: 1b461a8. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.7.3
- Changes, versionCode 14:
  - #52 (326743c): "Next puzzle" after a landscape photo forces portrait and opens the Photo Picker only once the
    screen has turned, so the picker never appears in landscape.
  - #53 (8293370): a Compose overlay (`LaunchLogo` in `ui/TiloryApp.kt`) shows the Voronkov Family logo on a white
    rounded plate for 1.2 s, then fades out; a tap skips it. Cause: on the owner's Xiaomi the Android 12+ system
    splash (#33) shows only the background colour. The logo asset is cropped from the owner's white-background
    ChatGPT export; the transparent exports had a white halo at the edges.
- Build: local, same toolchain and debug key. Release v0.7.0 deleted (tag kept).
- Checked: unit tests and lint pass (no new warnings), APK badging shows 0.7.3 / 14. Not checked on the phone.

## v0.7.4 — one launch screen, delete a photo from History (#56)
- Commit: d36f574. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.7.4
- Changes, versionCode 15:
  - #53 (bcda38a, bff0dc8): v0.7.3 showed two screens in a row on a cold start (the system splash with the icon, then
    the logo overlay). Now the Android 12+ system splash is only the window colour (`drawable/splash_blank.xml` as the
    icon, no branding image, `splash_branding.png` removed), and `LaunchLogo` is the one launch screen: the icon
    drawn from its foreground layer and the picture title above, the studio logo (half the width, max 240 dp) a
    little above the bottom; 1.5 s, fade out, tap skips. #33 closed as superseded.
  - #55 (82b0251): "Delete from history" in the History viewer, with a confirmation; `deleteFromHistory` removes the
    entry and runs `cleanUpPhotos`; Surprise memory keeps the photo as solved.
- Build: local, same toolchain and debug key. Release v0.7.1 deleted (tag kept).
- Checked: unit tests and lint pass (no new warnings), APK badging shows 0.7.4 / 15; launch screen checked only as a
  mockup drawn from the real resources. Not checked on the phone.

## v0.8.0 — the Tilory collection (#58)
- Commit: e270271. Release: https://github.com/ekaterina-voronkova/tilory/releases/tag/v0.8.0
- Changes, versionCode 16:
  - #57 (7f17efc): 103 of the owner's photos built in under `assets/collection` (an owner-confirmed exception to the
    privacy rule, recorded in CLAUDE.md). Prepared on the build computer with System.Drawing: EXIF orientation
    applied (many sources relied on tags 6 / 8), longest side 1600 px, JPEG q82, no EXIF left (93 sources had GPS);
    360 px thumbnails; `index.json` with id and size only. Size trade-off: 1920 px q85 was 56 MB, 1600 px q82 is
    37 MB. Grid + "Random from the collection" (Mode.COLLECTION, hidden like Surprise, Surprise settings),
    `AppData.collectionSolved` for check marks and for Random.
- Build: local, same toolchain and debug key. APK 65.7 MB (was ~27 MB). Release v0.7.2 deleted (tag kept).
- Checked: 26 unit tests and lint pass, the APK holds all 207 collection assets, badging shows 0.8.0 / 16. Not
  checked on the phone.

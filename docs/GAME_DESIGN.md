# Tilory — Game Design

Status: draft for the owner's review (#1). Items marked **Proposal** are Claude's suggestions that the owner has not
decided yet; each is also listed under [Open questions](#15-open-questions).

## 1. Vision
Tilory (tile + story, "a story piece by piece") is an Android puzzle game where a picture is cut into rectangular
tiles and put back together. The pictures are the owner's own photos and drawings.

Why another puzzle game: existing ones have a fixed, repetitive picture set, few difficulty settings, no way to use
your own photos, and jigsaw-shaped pieces. Tilory uses only your pictures, only rectangles, and lets you tune the
difficulty.

- Audience: the owner first. Publishing on Google Play may come later, so nothing should block it.
- Play Store title: "Tilory: Photo Tile Puzzle".
- Package: `com.voronkova.tilory` (cannot be changed after publishing).

## 2. Modes
### Surprise (main mode)
- The game picks a random photo from the sources the owner has set up.
- The photo stays unknown until the puzzle is solved: a "closed box". No thumbnail anywhere: not on the main screen,
  not in lists, not in notifications, not in the recent-apps screen (see §12).
- The reference image is off by default (see §4).
- Solved photos are remembered and never come again in Surprise.
- Photos skipped with "just want another" may come again later; photos hidden with "never show" do not.

### Own pick
- The owner chooses a photo (see §9).
- The reference image is available, and the photo is visible in the picker, so there is nothing to hide.
- A photo already solved can be picked again.

## 3. Core mechanic
- **Board.** The picture is cut into a grid of rows × columns of equal rectangular tiles. The board always has exactly
  one cell per tile: there is no tray and no empty cell.
- **Start.** All tiles are on the board from the start, shuffled. No tile starts in its correct cell next to its
  correct neighbour (the shuffle is redone until no pair is already glued).
- **Orientation.** Every tile is upright. There is no rotation.
- **The only action is dragging.** Touch a tile and drag it; the tile (or its whole group, see below) follows the
  finger and lands on the cells under it when released.
- **Displacing.** The tiles that were on the target cells move into the cells the dragged tiles left. For a single
  tile this is a plain swap. For a group, displaced tiles fill the freed cells in reading order (row by row, left to
  right). **Proposal.**
- **Gluing.** After each drop, two tiles that touch on the board and also touch the same way in the original picture
  glue together. Glued tiles form a group: dragging any of its tiles moves the whole group.
- **Groups can break.** A group that something is dropped on can fall apart: its covered tiles are displaced like any
  others, its uncovered tiles stay where they are. After the drop the glue is worked out again from scratch: tiles that
  still touch correctly stay glued, tiles that were pulled apart are no longer glued. So a group is simply a set of
  tiles that currently touch correctly; the dragged group itself always stays whole.
- **Edges.** A group dropped partly off the board is shifted inwards to the nearest position where it fits entirely.
- **Feedback.** A short glue animation and a light vibration when tiles glue (vibration can be turned off).
- **Solved** when all tiles form a single group. Since the board has exactly as many cells as tiles, a single group
  always fills the whole board.
- **Not counted:** moves. **Counted:** solve time (see §6).
- **No "correctly placed" highlight.** Gluing is the only feedback about correctness.

## 4. Difficulty
- **Grid size.** Chosen as a number of tiles; the game picks rows × columns so that the tiles are as close to square as
  possible for the photo's proportions, then crops the photo to fit the grid (centre crop, small loss at the edges).
  Range from 3 × 3 up to about 12 × 16.
- **Presets.** **Proposal:** Easy ≈ 12 tiles (3 × 4), Medium ≈ 48 (6 × 8), Hard ≈ 108 (9 × 12), Custom (any size in
  the range). Each mode remembers its own last choice.
- **Reference image.** Off / Peek (hold a button to see the picture faded behind the board, release to hide) /
  Always (a small thumbnail in the corner). Default: Off in Surprise, Peek in Own pick.
- **The board always fits the screen.** No zoom, no panning. The largest grid offered is limited so that a tile is not
  smaller than a comfortable touch size. **Proposal:** minimum tile 40 dp.

## 5. Skip
- A Skip button is always visible during a puzzle. No confirmation, no penalty. The tiles disappear at once.
- **Undo** for about 5 seconds after skipping (a snackbar), in case it was pressed by accident; it brings the puzzle back
  exactly as it was.
- After skipping in Surprise, an optional question (can be ignored or closed):
  - "Just want another one": the photo may come again later.
  - "Never show this photo": the photo is hidden.
  - "Don't take photos from this folder": the folder is removed from the Surprise sources.
  - (Later, with the content filter, §10: "It's a screenshot / document / private", which also makes the filter
    stricter.)
- Hidden photos are listed in Settings and can be restored there. The list shows thumbnails, because the owner opens it
  on purpose.
- After skipping, the next puzzle starts right away.

## 6. Completion
1. The seams between tiles fade out and the whole photo is shown.
2. The photo opens full screen; it can be zoomed and panned.
3. Below it: the solve time, then the buttons "Next puzzle" and "History".
- Solve time counts only while the puzzle is on screen (it pauses when the app is in the background).
  **Proposal:** the timer is not shown during play, only on completion.
- The photo is added to History.

## 7. History
- A gallery of solved photos, newest first, with the date and the solve time.
- Tap opens the photo full screen.
- **Proposal:** "Play again" from History (any grid size) and "Remove from history".

## 8. Saving progress
- There is one unfinished puzzle at a time.
- Saved: the photo, the mode, the grid size, the position of every tile, the groups, the elapsed time and the
  reference-image setting.
- Saved after every drop and whenever the app goes to the background, so nothing is lost if the app is closed or killed.
- On launch, the main screen offers to continue it. In Surprise the card shows the grid size and the progress
  (largest group as a percentage of the tiles), never the picture.
- A new puzzle starts only after the current one is solved or skipped. Choosing "Own pick" while a puzzle is
  unfinished asks whether to skip the current one.

## 9. Photo sources
### Own pick
- Android Photo Picker: no permissions needed; on Android 12+ it also shows Google Photos cloud items.
- System file picker (Storage Access Framework): OneDrive and other providers usually appear here if their apps are
  installed (to check on the owner's phone).
- "Share → Tilory" from any app.

### Surprise, in stages
1. **A folder on the phone.** The owner picks one or more folders once (`ACTION_OPEN_DOCUMENT_TREE`), for example the
   camera folder or a drawings folder. No special permission; screenshots and receipts are excluded simply by not
   picking their folders. (Alternative: `READ_MEDIA_IMAGES` for the whole library; it needs a Play Store declaration
   with no guaranteed approval. Not planned.)
2. **A Google Photos album.** The Library API no longer gives access to the whole library (since 2025-03-31). With the
   Google Photos Picker API the owner picks a whole album once (for example "For puzzles"); the game downloads it and
   picks from it.
3. **A OneDrive folder** via Microsoft Graph (sign-in, pick a folder). Most work; last.

### Image handling (all sources)
- Keep a downscaled local copy inside the app: access to the original may be temporary, and the cloud is slow and needs
  the internet.
- Apply the EXIF orientation; support HEIC; downscale large photos (**Proposal:** longest side 2048 px).
- Remember each photo by a content hash, so the same photo from two sources counts once.

## 10. Content filter (later, not in the first version)
Goal: no private photos, screenshots, documents or receipts in Surprise. On the device only, never a cloud AI. Layers,
cheapest first: simple rules (folder or file name "Screenshots", no camera EXIF data, screen-like proportions) →
on-device text recognition (ML Kit OCR: a lot of text means a document, receipt or chat) → an on-device NSFW model
(TFLite) → optionally Gemini Nano on phones that support it (the supported list is mostly flagships; the Redmi 13 is
most likely not on it, to check when this stage starts). A queue of 3–5 photos checked in advance keeps the next
puzzle instant. Until this exists, choosing the Surprise folders carefully is the filter.

## 11. Screens and navigation
A list of the app's screens and how to get from one to another.

- **Main:** "Continue" (if a puzzle is unfinished), "Surprise", "Own pick", and entries to History and Settings.
- **Puzzle:** the board, Skip, the reference-image button (if enabled), a back button that returns to Main (progress
  is kept).
- **New puzzle options:** grid size (presets + custom) and the reference-image setting; shown before a puzzle starts,
  with "remember and don't ask" to start right away next time.
- **Completion:** the full photo, solve time, "Next puzzle", "History".
- **History:** the gallery; tap → full-screen photo.
- **Settings:** default difficulty per mode, reference image defaults, vibration, Surprise sources, hidden photos.
- **Surprise sources:** the folders (later: albums) in use; add or remove; how many photos each has left.
- **Hidden photos:** the list, with restore.

```
Main ──► New puzzle options ──► Puzzle ──► Completion ──► Puzzle (next)
 │                                │  ▲          │
 │                                │  └─ Undo ◄──┤ (skip)
 │                                └────────────►│
 ├──► History ──► Full-screen photo
 └──► Settings ──► Surprise sources / Hidden photos
```

## 12. Privacy
- Everything stays on the device. The first version has no internet permission at all; it is added only with the
  Google Photos and OneDrive stages, and only for those.
- No analytics, no ads, no account.
- In Surprise the screen is excluded from the recent-apps thumbnail (`FLAG_SECURE` during a Surprise puzzle) so the
  picture does not leak there. **Proposal** (it also blocks screenshots during that puzzle).
- Before Play Store publishing: a privacy policy on a website. To check: whether tilory.app or tilory.com is free.

## 13. Technical foundation
- Kotlin, Jetpack Compose, Navigation Compose, a single activity.
- Room for photos, puzzle state and history; DataStore for settings.
- The board is drawn from one bitmap: each tile is a region of it, so no tiles are saved as separate images.
- `androidx.exifinterface` for orientation; `ImageDecoder` for decoding, including HEIC.
- The owner's phone: Xiaomi Redmi 13, Android 16. It is the main test device.
- **Proposal:** minSdk 28 (Android 9; `ImageDecoder` with HEIC support), target the latest SDK. The owner's phone is far
  above it, so the choice matters only for publishing.
- Game logic (grid, shuffle, displacing, gluing, completion) in plain Kotlin with no Android dependencies, covered by
  unit tests.

## 14. Roadmap
1. **MVP:** Own pick (Photo Picker and Share), the board with dragging and gluing, grid sizes and presets, reference
   image, completion screen, saving progress, History.
2. **Surprise from phone folders:** sources screen, random pick without repeats, closed box, Skip with Undo and the
   follow-up question, hidden photos.
3. **Google Photos album** via the Picker API.
4. **Content filter** (§10).
5. **OneDrive folder** via Microsoft Graph.
6. **Publishing:** domain, privacy policy, Play Store listing.

## 15. Open questions
1. The remaining proposals: displacing order for groups (§3), presets and the 40 dp minimum tile (§4), timer hidden
   during play (§6), "Play again" in History (§7), longest side 2048 px (§9), `FLAG_SECURE` in Surprise (§12),
   minSdk 28 (§13).
2. To check on the owner's phone: whether OneDrive appears in the system file picker (§9).

Decided: groups can break when something is dropped on them (§3); the board always fits the screen (§4); the owner's
phone is a Xiaomi Redmi 13 on Android 16 (§13).

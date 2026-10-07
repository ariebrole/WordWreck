# Word-Wreck! — 8-Bit Ocean Quest

The updated Java 11 / JavaFX 13 project. This is the complete project, not the earlier two-file patch.

## Run

1. Extract this ZIP into a new folder.
2. Open `WordWreck-frontend/demo` in VS Code or IntelliJ.
3. Run `mvn clean javafx:run` in the folder containing `pom.xml`.

On Windows you can instead double-click `START-GAME.bat` if JDK 11+ and Maven are already on your PATH.
The first build needs internet to download JavaFX/Maven dependencies. After those are cached, the game can run offline; if Maven tries to connect, run `mvn -o javafx:run`. Offline gameplay does not eliminate that initial installation requirement. Your original POM versions are retained.

## Audio and sound effects update

- **Sound effects (SFX)**:
  - `Button1.wav`: Letter typing (onscreen & physical keys), navigation, and dialog buttons.
  - `Button2.wav`: Letter deletion and erase actions (Backspace / DEL).
  - `StartVoyage1.wav`: Embarking on a level, restarting, or resuming a voyage.
  - `RightWord-BuildRaft1.mp3`: Solving Word A / celebratory raft bounce.
  - `WrongWord-SharkMove1.wav`: Incorrect guess feedback and shark advance.
  - `Win1.wav`: Full victory / stage completion jingle.
  - `Lose1.wav`: Sink / game over sound.
- **Background music (BGM)**:
  - `MenuMusic1.mp3`: Looping ocean pirate theme on the Home screen.
  - `GameMusic1.wav` & `GameMusic2.wav`: Looping in-game adventure voyage tracks.
- **Audio controls**:
  - Settings dialog provides independent toggles for **Play sound effects** and **Play background music**, saved locally via Java Preferences.
- **Fault-tolerant design**:
  - `SoundManager.java` handles playback gracefully with try/catch boundaries so that unavailable audio hardware or missing audio drivers never cause game crashes.

## Shark and animation update

- Replaced the tiny fish-shaped sprite with an original 48 × 24 shark: tall swept dorsal fin, asymmetrical tail, pointed snout, gills, pale belly and prominent pectoral fin. At the same pixel scale it is roughly twice the old width/height.
- Home and board sharks now swim gently and change tail frames, with small pixel wakes. Smaller versions fit the two threat lanes without changing the screen layout.
- Animation updates at 10 frames per second. Sea waves remain slower at 2.5 frames per second to avoid excessive movement.
- Wrong guesses briefly shake the raft; solving A gives a small celebratory bounce before continuing on B. Effects do not delay or alter scoring. Final endings still open the result screen promptly.
- Settings → Animate sea and sharks disables swimming, wakes moving, shark approaches and guess effects. Switching screens or restarting resets any active raft effect.
- Fonts, layout, dictionary settings, clues and rules are preserved.
- Update PrimaryController.java and OceanArt.java together, or extract this complete ZIP into a fresh folder.
- Sprite dimensions and FXML wiring were checked, and the art preview was visually inspected. Actual animation playback still needs local JavaFX testing; this environment has no JavaFX runtime.

## Readability and larger home artwork update

- Clear mixed-case system text for clues, instructions, settings, buttons and letter tiles. Pixel lettering is retained for the logo, hero title, level headings and score.
- The home sea artwork grows from 860 × 190 to 1080 × 280 logical pixels. The home spacing/cards are tightened to retain the fixed window layout.
- Enlarged palms and islands, a lighthouse, distant coastline, sailboat and larger central raft build out the 8-bit scenery.
- Shark-track labels use clearer text and more separation from the shark path.
- This is a visual update; dictionary settings, saved scores, hints and game rules are unchanged.
- Replace App/Controller/resources as a complete project or copy the changed PrimaryController.java, OceanArt.java, primary.fxml and wordwreck.css together.
- FXML fields, handlers and canvas sizes were checked. Actual JavaFX layout and Windows font rendering still need a local run; a JavaFX runtime is unavailable here.

## Original 8-bit visual overhaul

- Display headings use the bundled original **Wreck Pixel** font, loaded by App.java. No font installation or font download is needed.
- Retro title treatment, hard-edged panels, beveled buttons/keyboard, square letter tiles and a limited ocean/sunset palette.
- Original integer-grid pixel sprites for sharks, palm islands, raft logs and compass; sea bands, block-wave animation and a sunset home scene.
- Settings → Animate sea and sharks controls both animations. The sea updates at 2.5 frames per second and stops on the results screen; it does not change gameplay timing.
- Original game logic, dictionary modes, hint definitions, vertical B history and local saved preferences remain.
- `PIXEL-ART-PREVIEW.png` previews the bundled font, sprites and palette. It is an art board, NOT a captured JavaFX screenshot.
- `design/` contains the editable font generator and art-preview generator. Python/fontTools/Pillow are needed only to regenerate those design files, not to play the game.
- The original pixel font and sprite artwork in this project may be used, modified and redistributed with your game.

This is an 8-bit-inspired visual design, not an emulator or a restriction to historical hardware. The logical window is still 1180 × 780. Fractional display/window scaling may soften pixel edges slightly.

For an existing project, replace **App.java, PrimaryController.java, OceanArt.java, primary.fxml and wordwreck.css**, and add **src/main/resources/com/example/fonts/wreck-pixel.ttf**. Keep the existing offline/settings classes and module requirements. The easiest option is to extract this complete project to a fresh folder.

Validation for this visual pass: the font was opened/rendered successfully, the supplied sprite preview was visually inspected, and FXML/controller field types and handlers match. JavaFX compilation, screen layout, font rendering on Windows, and runtime animations still need a local check because this environment has no JavaFX runtime or Maven.

## Previous edition features retained

- **Settings → online dictionary checkbox**: off by default for a new installation. OFF is Offline Practice: complete A–Z guesses can be submitted instantly; no dictionary requests are made. This does not validate English words. ON restores the real-word lookup. Incomplete guesses stay blocked in both modes.
- You can change the setting during a round without losing letters, guesses, or anchor progress. Pending lookups cannot override the new mode. No automatic mode change occurs during a network outage; turn the setting off yourself if you want practice mode.
- Settings are saved locally through Java Preferences. Best scores persist separately for practice and dictionary rounds. If offline mode is used at any time during a round, that round counts as practice. These are local convenience scores, not competitive or tamper-proof records.
- **Animate shark movement** can be disabled in Settings for reduced motion.
- **Resume voyage** brings back the current round after visiting Home. Active rounds are not saved after closing the app. Starting a new level still starts a fresh round.
- The first graphics pass adds illustrated palm islands, a compass, wooden plank details, and fuller shark shapes. All are JavaFX Canvas/CSS graphics bundled as source: no external graphics downloads, image-generation dependency, or graphics library.

### Features retained from the previous edition

- Type directly onto the highlighted raft. There is no separate input grid.
- A types across; B types down. Its revealed anchor is filled automatically and skipped when typing/deleting.
- Word B's guess history remains vertical, with attempt columns numbered 1–5.
- Captain's Hint gives a handcrafted, pirate/ocean-style definition of the active word. It never displays the answer, costs no attempt and deducts no score.
- A 1180 × 780 logical canvas keeps the whole game together without scrolling. The entire canvas scales to fit a smaller display or a maximized window, preserving its proportions. Wide screens may have blank margins.
- Custom Maximize/Restore and Close buttons; no Minimize button or manual edge resizing. Double-click the title strip to maximize/restore; drag it to move the normal window. Minimization attempts are automatically reversed when the OS permits. An OS/window manager can override app requests, so this is not kiosk mode.
- With dictionary mode ON, Enter requires a complete, approved guess. With it OFF, Enter requires a complete alphabetic guess. Physical Enter follows the same rule.
- Wrong guesses move the shark smoothly instead of making it jump.
- Best scores are stored on this computer, separately for practice and dictionary rounds.
- Updated ocean colors, gold anchor highlights, compact keyboard, side-by-side guess logs, and themed hint dialogs.

## Word validation

Offline Practice skips lookups entirely. The rules below apply only when the dictionary setting is ON.

New complete guesses are checked asynchronously through Free Dictionary API:
https://dictionaryapi.dev/

Only the typed word is included in the lookup URL. The game waits briefly after the final letter to avoid requests while editing. The screen remains responsive during lookup. Old responses cannot approve a newer guess or a different level.

- Confirmed word: Enter unlocks.
- Word not listed: Enter stays disabled; change the letters. No attempt is spent.
- Network error, timeout, rate limit or server error: Enter stays disabled and Retry Lookup becomes available. No attempt is spent.
- Editing a word immediately removes its previous approval.
- All curated level words work offline. Other words checked in this session are cached in memory. There is no full offline dictionary.

A dictionary is not an exhaustive list of every possible English word or inflection. The message therefore says "not in our dictionary". The service can be unavailable. You can later replace `DictionaryService` with your own local dictionary without redesigning the frontend.

## How to play

Type A–Z using your keyboard or the onscreen keys. Backspace removes the last entered letter. Enter submits only when approved.

A has five guesses. If A fails, a green match at its exact anchor position in ANY earlier A guess lets you continue to B. Without that anchor, the game ends immediately. B also has five guesses. Keyboard colors reset for B.

| A | B | Result |
|---|---|---|
| Solved | Solved | Full victory, 100% |
| Solved | Failed | Partial survival, 50% |
| Failed with anchor | Solved | Survival clear, 50% |
| Failed with anchor | Failed | Both sunk, 0% |
| Failed without anchor | Not reached | Immediate sink, 0% |

Duplicate guesses consume attempts. Restart starts a fresh round. Returning home and starting a level also starts a fresh round. Settings and best scores are saved locally; active rounds are not saved to disk.

## Update your existing project instead

Back up your current files, then copy these matching paths from the ZIP:

| File under `demo` | Action |
|---|---|
| `src/main/java/com/example/App.java` | Replace |
| `src/main/java/com/example/PrimaryController.java` | Replace |
| `src/main/java/com/example/DictionaryService.java` | Add |
| `src/main/java/com/example/PirateHints.java` | Keep/copy |
| `src/main/java/com/example/GameSettings.java` | Add |
| `src/main/java/com/example/OceanArt.java` | Add |
| `src/main/resources/com/example/primary.fxml` | Replace |
| `src/main/resources/com/example/wordwreck.css` | Replace |
| `src/main/java/module-info.java` | Ensure both `requires java.net.http;` and `requires java.prefs;` are in your module |

Keep your existing `opens com.example to javafx.fxml;` and `exports com.example;`. You may keep the `backend` exports/opens you previously used if that package exists. The standalone ZIP omits those lines because its demo model lives in `com.example.preview`.

Keep `com/example/preview/GameEngine.java`, `Level.java`, and `Levels.java` from the previous frontend. They are also included here, unchanged. No POM change is required. Your teammate's `backend` package is not modified.

Do not mix the new FXML with an older controller: their injected fields and event handlers must match. FXML and CSS belong under resources, not under the Java source folder.

The regular game model remains separate from the presentation. When your teammate connects the real backend, replace/adapt the preview model calls in `begin`, `submitGuess`, `render`, and `showResult`.

## Verification and limits

Completed here:
- Game logic compiled with `--release 11`; all five endings, all 20 levels, anchor persistence, fifth-attempt wins and repeated letters passed.
- Dictionary HTTP code compiled for Java 11 and passed controlled local-server tests for valid/unlisted words, caching, retryable outages, and curated offline answers.
- Every level word has a pirate hint.
- Offline tests verify zero HTTP calls and confirm offline acceptance never contaminates the online dictionary cache.
- Settings reload correctly; reduced-motion preference and mode-specific best scores persist without downgrading previous bests.
- All FXML ids, field types and action handlers match the controller. XML parses. There is no scroll pane or separate input grid.

**JavaFX compilation, rendering, actual Windows maximize/minimize behavior, and a live public-dictionary lookup could not be tested here.** Maven and JavaFX libraries are unavailable in this environment. Controlled dictionary tests do not establish availability or completeness of the external service.

Local checks after launch:
1. Confirm the whole game fits at normal size and Maximize/Restore preserves the layout.
2. At level 1, click Captain's Hint. It should describe goods carried on a ship without spending an attempt.
3. Type CARGO on the raft. After validation, Enter should unlock. Submit it.
4. B should be vertical with R already in the fourth cell. Type SHAKS (R is automatic), then Enter. Expect 100%.
5. With Dictionary ON, restart and type ZZZZZ. Enter should remain disabled after lookup. With Dictionary OFF, the same complete guess should submit as a wrong attempt. Check that short guesses remain blocked in both modes.
6. In online mode with the internet unavailable, check the retry message. Turn dictionary OFF in Settings; confirm the letters remain and you can submit immediately with no network wait.
7. Type a complete guess, then erase/change a letter while checking; an old result must not enable Enter for the changed word.
8. Check level 16's nine-letter B board and history for clipping.
9. Check Settings, reduced motion, Home → Resume, saved mode after relaunch, and separate best-score displays.
10. Check the title bar, close button, OS minimization behavior, and graphics at normal/maximized sizes.

The visual theme and shark motion are lightweight JavaFX shapes; there are no image/audio downloads, accounts, installers, or saved active rounds.

Official JavaFX setup: https://openjfx.io/openjfx-docs/maven
Stage/window behavior: https://openjfx.io/javadoc/13/javafx.graphics/javafx/stage/Stage.html

## Optional regression checks

From the extracted `WordWreck-frontend` folder, with JDK 11+ installed:

    mkdir test-out
    javac --release 11 --add-modules jdk.httpserver -d test-out demo/src/main/java/com/example/preview/*.java demo/src/main/java/com/example/DictionaryService.java demo/src/main/java/com/example/PirateHints.java demo/src/main/java/com/example/GameSettings.java checks/DictionaryServiceTest.java checks/GameSettingsTest.java demo/src/test/java/com/example/preview/GameEngineTest.java
    java -ea --add-modules jdk.httpserver -cp test-out com.example.DictionaryServiceTest
    java -ea -cp test-out com.example.GameSettingsTest
    java -ea -cp test-out com.example.preview.GameEngineTest

These are standalone assertion checks. They are not automatically run by `mvn test`.

package com.example;

import com.example.preview.GameEngine;
import com.example.preview.GameEngine.Guess;
import com.example.preview.GameEngine.Mark;
import com.example.preview.GameEngine.Outcome;
import com.example.preview.Level;
import com.example.preview.Levels;
import java.util.*;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Interpolator;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

/** Frontend controller. The preview package is a replaceable, JavaFX-free rules layer. */
public class PrimaryController {
    @FXML private VBox homePane, gamePane, resultPane, keyboard, historyA, historyB, clueACard, clueBCard;
    @FXML private HBox titleDrag;
    @FXML private GridPane crossword;
    @FXML private Pane heroArt, sharkTrack, waterArt;
    @FXML private ComboBox<String> homeLevel;
    @FXML private Label levelTitle, phaseLabel, anchorLabel, inputTitle, messageLabel;
    @FXML private Label statusA, statusB, clueA, clueB, resultTitle, resultScore, resultDetail, answersLabel;
    @FXML private Button nextButton, hintButton, retryButton, maximizeButton;
    @FXML private Label dictionaryLabel, modeLabel, homeStatus;
    @FXML private Button resumeButton;
    private final GameSettings settings = new GameSettings();
    private SoundManager soundManager;
    private boolean practiceRound;
    private Button enterButton;
    private double dragX, dragY;
    private final DictionaryService dictionary = new DictionaryService();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(400));
    private CompletableFuture<DictionaryService.Result> pending;
    private DictionaryService.Result validation;
    private String approvedWord = "";
    private long lookupVersion;
    private final Map<Character, Button> keyButtons = new HashMap<>();
    private final List<Button> inputButtons = new ArrayList<>();
    private final Canvas heroCanvas = new Canvas(), trackCanvas = new Canvas(), waterCanvas = new Canvas();
    private final DoubleProperty threatA = new SimpleDoubleProperty();
    private final DoubleProperty threatB = new SimpleDoubleProperty();
    private Timeline sharkMotion, seaMotion, guessEffect;
    private int seaFrame;
    private GameEngine game;
    private int levelIndex;
    private char[] entry;

    @FXML private void initialize() {
        soundManager = new SoundManager(settings.sound(), settings.music());
        for (int i = 1; i <= Levels.ALL.size(); i++) homeLevel.getItems().add("Level " + i);
        homeLevel.getSelectionModel().selectFirst();
        buildKeyboard();
        updateModeLabel();
        waterArt.getChildren().add(waterCanvas);
        waterArt.widthProperty().addListener((o,a,b) -> drawWater());
        waterArt.heightProperty().addListener((o,a,b) -> drawWater());
        homeLevel.valueProperty().addListener((o,a,b) -> updateHomeStatus());
        updateHomeStatus();
        titleDrag.setOnMousePressed(e -> {
            dragX = e.getScreenX() - App.window().getX();
            dragY = e.getScreenY() - App.window().getY();
        });
        titleDrag.setOnMouseDragged(e -> {
            if (!App.isExpanded()) {
                App.window().setX(e.getScreenX() - dragX);
                App.window().setY(e.getScreenY() - dragY);
            }
        });
        titleDrag.setOnMouseClicked(e -> { if (e.getClickCount() == 2) maximize(); });
        heroArt.getChildren().add(heroCanvas);
        sharkTrack.getChildren().add(trackCanvas);
        heroArt.widthProperty().addListener((o, a, b) -> drawHero());
        sharkTrack.widthProperty().addListener((o, a, b) -> drawThreats());
        threatA.addListener((o,a,b) -> drawThreats());
        threatB.addListener((o,a,b) -> drawThreats());
        gamePane.sceneProperty().addListener((o, oldScene, scene) -> {
            if (scene == null) return;
            scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
                if (!canType() || event.isControlDown() || event.isAltDown() || event.isMetaDown()) return;
                String text = event.getCharacter().toUpperCase(Locale.ROOT);
                if (text.length() == 1 && text.charAt(0) >= 'A' && text.charAt(0) <= 'Z') {
                    type(text.charAt(0)); event.consume();
                }
            });
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (!canType()) return;
                switch (event.getCode()) {
                    case ENTER: submitGuess(); event.consume(); break;
                    case BACK_SPACE: erase(); event.consume(); break;
                    default: break;
                }
            });
        });
        Platform.runLater(this::drawHero);
        syncSeaMotion();
        soundManager.playMenuMusic();
    }
    @FXML private void showHome() {
        soundManager.playSfx(SoundManager.Sfx.BUTTON_1);
        cancelLookup();
        show(homePane);
        resumeButton.setDisable(game == null || game.outcome() != Outcome.PLAYING);
        updateHomeStatus();
        soundManager.playMenuMusic();
    }
    @FXML private void resumeGame() {
        if (game == null || game.outcome() != Outcome.PLAYING) return;
        soundManager.playSfx(SoundManager.Sfx.START_VOYAGE);
        show(gamePane); entryChanged(); render();
        soundManager.playGameMusic();
    }
    private void updateModeLabel() {
        modeLabel.setText(settings.online() ? "DICTIONARY ON" : "OFFLINE PRACTICE");
    }
    private void updateHomeStatus() {
        int selected = homeLevel.getSelectionModel().getSelectedIndex();
        if (selected >= 0) homeStatus.setText("Best: " + settings.best(selected, !settings.online())
            + "% · " + (settings.online() ? "Dictionary mode" : "Offline practice"));
    }
    @FXML private void showSettings() {
        soundManager.playSfx(SoundManager.Sfx.BUTTON_1);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(App.window()); dialog.setTitle("Ship settings");
        dialog.setHeaderText("Choose how ye sail");
        CheckBox online = new CheckBox("Check guesses with the online dictionary");
        online.setSelected(settings.online());
        Label explanation = text("OFF: play offline; any complete A–Z guess is accepted.\n"
            + "ON: real-word validation; new lookups need internet.\n"
            + "Switching modes keeps your current letters and attempts.", "muted");
        CheckBox motion = new CheckBox("Animate sea and sharks");
        motion.setSelected(settings.motion());
        CheckBox sound = new CheckBox("Play sound effects");
        sound.setSelected(settings.sound());
        CheckBox music = new CheckBox("Play background music");
        music.setSelected(settings.music());
        VBox content = new VBox(14, online, explanation, motion, sound, music);
        content.setPadding(new javafx.geometry.Insets(18));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(App.class.getResource("wordwreck.css").toExternalForm());
        dialog.getDialogPane().setPrefWidth(540);
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.APPLY) {
            cancelLookup();
            settings.set(online.isSelected(), motion.isSelected(), sound.isSelected(), music.isSelected());
            soundManager.setSoundEnabled(settings.sound());
            soundManager.setMusicEnabled(settings.music());
            if (!settings.motion()) resetEffects();
            boolean saved = settings.flush();
            if (game != null && game.outcome() == Outcome.PLAYING) {
                practiceRound |= !settings.online();
                if (gamePane.isVisible()) { entryChanged(); render(); }
            }
            updateModeLabel(); updateHomeStatus(); syncSeaMotion();
            if (!saved) showDialog("Settings", "Applied for this session", "Could not save settings to disk on this computer.");
        }
    }
    @FXML private void maximize() {
        App.toggleMaximize();
        maximizeButton.setText(App.isExpanded() ? "Restore" : "Maximize");
    }
    @FXML private void closeApp() {
        cancelLookup();
        soundManager.stopMusic();
        Platform.exit();
    }
    @FXML private void startGame() { begin(homeLevel.getSelectionModel().getSelectedIndex()); }
    @FXML private void restartLevel() { begin(levelIndex); }
    @FXML private void nextLevel() {
        if (levelIndex + 1 < Levels.ALL.size()) begin(levelIndex + 1);
        else showHome();
    }
    @FXML private void reviewGame() {
        soundManager.playSfx(SoundManager.Sfx.BUTTON_1);
        show(gamePane); render();
        soundManager.playGameMusic();
    }
    private void begin(int index) {
        levelIndex = index;
        practiceRound = !settings.online();
        game = new GameEngine(Levels.ALL.get(index));
        if (sharkMotion != null) sharkMotion.stop();
        threatA.set(0); threatB.set(0);
        resetEffects();
        resetEntry(); show(gamePane); entryChanged();
        soundManager.playSfx(SoundManager.Sfx.START_VOYAGE);
        soundManager.playGameMusic();
        messageLabel.setText("Start with A. Watch the gold anchor tile — a green match there can save the junction.");
        render();
    }
    private void show(Node chosen) {
        for (Node screen : Arrays.asList(homePane, gamePane, resultPane)) {
            screen.setVisible(screen == chosen); screen.setManaged(screen == chosen);
        }
        if (!gamePane.isVisible()) resetEffects();
        syncSeaMotion();
    }
    private boolean canType() { return game != null && gamePane.isVisible() && game.outcome() == Outcome.PLAYING; }
    private boolean locked(int index) { return game.isSecond() && index == game.level().anchorB(); }
    private void resetEntry() {
        entry = new char[game.target().length()];
        if (game.isSecond()) entry[game.level().anchorB()] = game.level().down().charAt(game.level().anchorB());
    }
    private void type(char letter) {
        if (!canType()) return;
        soundManager.playSfx(SoundManager.Sfx.BUTTON_1);
        for (int i = 0; i < entry.length; i++) if (!locked(i) && entry[i] == 0) {
            entry[i] = letter; entryChanged(); return;
        }
    }
    private void erase() {
        if (!canType()) return;
        soundManager.playSfx(SoundManager.Sfx.BUTTON_2);
        for (int i = entry.length - 1; i >= 0; i--) if (!locked(i) && entry[i] != 0) {
            entry[i] = 0; entryChanged(); return;
        }
    }
    private void submitGuess() {
        if (!canType()) return;
        for (char c : entry) if (c == 0) { messageLabel.setText("Fill every empty tile before submitting. No attempt used."); return; }
        if (validation != DictionaryService.Result.VALID || !new String(entry).equals(approvedWord)) {
            return; // Physical Enter follows exactly the same gate as the button.
        }
        boolean wasSecond = game.isSecond();
        boolean correctGuess = new String(entry).equals(game.target());
        try {
            game.submit(new String(entry));
            resetEntry();
            entryChanged();
            if (game.outcome() != Outcome.PLAYING) {
                messageLabel.setText("Level finished. Use Home to pick a level, or Restart to try again.");
                render(); showResult(); return;
            }
            if (correctGuess) {
                soundManager.playSfx(SoundManager.Sfx.RIGHT_WORD);
            } else {
                soundManager.playSfx(SoundManager.Sfx.WRONG_WORD);
            }
            if (!wasSecond && game.isSecond()) {
                messageLabel.setText(game.solved(false)
                    ? "Raft A is safe! B is unlocked. The anchor is filled for you — type only the missing letters."
                    : "The anchor held! Raft A's outer logs sank. Save B to finish with 50%.");
            } else messageLabel.setText("The shark moved closer. Use the tile colors and clue for your next guess.");
            render();
            playGuessEffect(correctGuess);
        } catch (IllegalArgumentException ex) { messageLabel.setText(ex.getMessage()); }
    }
    private Label text(String value, String... classes) {
        Label label = new Label(value); label.getStyleClass().addAll(classes); return label;
    }
    private Label tile(String value, String style, double size) {
        Label label = text(value, "tile", style);
        label.setMinSize(size, size); label.setPrefSize(size, size); label.setMaxSize(size, size);
        return label;
    }
    private String markClass(Mark mark) {
        switch (mark) { case CORRECT: return "correct"; case PRESENT: return "present"; default: return "absent"; }
    }
    private void render() {
        Level level = game.level(); boolean ended = game.outcome() != Outcome.PLAYING;
        levelTitle.setText(String.format("Level %02d / 20", levelIndex + 1));
        phaseLabel.setText(ended ? "Voyage finished • " + game.score() + "%"
            : game.isSecond() ? "Word B unlocked • Finish the second raft" : "Word A active • Word B is locked");
        anchorLabel.setText(game.anchorKnown() ? "ANCHOR SECURED" : "ANCHOR AT RISK");
        statusA.setText("A / ACROSS · " + branchStatus(false));
        statusB.setText("B / DOWN · " + branchStatus(true));
        clueA.setText(level.clueA() + " (" + level.across().length() + " letters)");
        clueB.setText(level.clueB() + " (" + level.down().length() + " letters)");
        clueACard.getStyleClass().remove("active-panel"); clueBCard.getStyleClass().remove("active-panel");
        if (!ended) (game.isSecond() ? clueBCard : clueACard).getStyleClass().add("active-panel");
        clueBCard.setOpacity(!game.isSecond() && !ended ? 0.65 : 1);
        inputTitle.setText(ended ? "THIS VOYAGE HAS ENDED" : "WORD " + (game.isSecond() ? "B" : "A")
            + " · ATTEMPT " + (game.history(game.isSecond()).size() + 1) + " OF 5"
            + (game.isSecond() ? " · ANCHOR LOCKED" : ""));
        renderBoard(); renderHistory(false); renderHistory(true); renderKeyboard();
        hintButton.setDisable(ended);
        animateThreats();
        drawThreats(); Platform.runLater(() -> { drawThreats(); drawWater(); });
    }
    private String branchStatus(boolean down) {
        if (game.outcome() == Outcome.IMMEDIATE_SINK || game.wrong(down) == 5) return "SUNK";
        if (game.solved(down)) return "SAFE";
        if (down && !game.isSecond()) return "LOCKED";
        return game.history(down).size() + "/5 GUESSES";
    }
    private void renderBoard() {
        crossword.getChildren().clear(); Level l = game.level();
        double size = l.down().length() > 7 ? 24 : 32;
        int next = -1;
        for (int i = 0; i < entry.length; i++) if (entry[i] == 0) { next = i; break; }
        for (int row = 0; row < l.down().length(); row++) for (int col = 0; col < l.across().length(); col++) {
            boolean across = row == l.anchorB(), down = col == l.anchorA();
            if (!across && !down) continue;
            boolean anchor = across && down;
            String letter = ""; String style = anchor ? "anchor" : "wood";
            boolean sunk = game.outcome() == Outcome.IMMEDIATE_SINK || game.outcome() == Outcome.BOTH_SUNK
                || across && !anchor && game.wrong(false) == 5 || down && !anchor && game.wrong(true) == 5;
            if (across && game.solved(false)) letter = "" + l.across().charAt(col);
            if (down && game.solved(true)) letter = "" + l.down().charAt(row);
            if (anchor && game.anchorKnown()) letter = "" + l.across().charAt(l.anchorA());
            if (!letter.isEmpty()) style = "correct";
            if (sunk) style = "sunk";
            boolean active = game.outcome() == Outcome.PLAYING && (game.isSecond() ? down : across);
            int position = game.isSecond() ? row : col;
            if (active) {
                letter = entry[position] == 0 ? "" : "" + entry[position];
                style = locked(position) ? "correct" : anchor ? "anchor" : "entry";
            }
            Label cell = tile(letter.isEmpty() && anchor ? "◆" : letter, style, size);
            Canvas grain = new Canvas(size, size);
            GraphicsContext ink = grain.getGraphicsContext2D();
            ink.setStroke(Color.web("#172f35", 0.25)); ink.setLineWidth(1);
            ink.setFill(Color.web("#15273b", 0.3)); ink.fillRect(4, size-6, size-8, 2);
            ink.setFill(Color.web("#f9e4b8", 0.5));
            ink.fillRect(3,3,2,2); ink.fillRect(size-5,3,2,2);
            cell.setGraphic(grain); cell.setContentDisplay(ContentDisplay.CENTER);
            if (active) cell.getStyleClass().add("active-tile");
            if (active && position == next) cell.getStyleClass().add("cursor");
            if (anchor) cell.setTooltip(new Tooltip("Anchor: A letter " + (l.anchorA()+1) + " / B letter " + (l.anchorB()+1)));
            crossword.add(cell, col, row);
        }
    }
    private void cancelLookup() {
        lookupVersion++;
        debounce.stop();
        if (pending != null) pending.cancel(true);
        pending = null;
        validation = null;
        approvedWord = "";
    }
    private void entryChanged() {
        cancelLookup();
        renderBoard();
        updateEnter();
        retryButton.setDisable(true);
        if (game.outcome() != Outcome.PLAYING) {
            dictionaryLabel.setText("Voyage complete."); return;
        }
        for (char c : entry) if (c == 0) {
            dictionaryLabel.setText(settings.online() ? "Fill the raft tiles. Enter unlocks for a dictionary word."
                : "Offline practice: fill every tile. No word lookup required."); return;
        }
        String word = new String(entry);
        if (!settings.online()) {
            validation = dictionary.check(word, false).join();
            approvedWord = validation == DictionaryService.Result.VALID ? word : "";
            dictionaryLabel.setText("Offline practice · Ready to submit. Word validity is not checked.");
            updateEnter(); return;
        }
        long version = lookupVersion;
        dictionaryLabel.setText("Checking yer word with the ship's dictionary...");
        debounce.setOnFinished(e -> {
            pending = dictionary.check(word);
            pending.whenComplete((result, error) -> Platform.runLater(() -> {
                if (version != lookupVersion || !canType() || !word.equals(new String(entry))) return;
                validation = error == null ? result : DictionaryService.Result.UNAVAILABLE;
                if (validation == DictionaryService.Result.VALID) {
                    approvedWord = word;
                    dictionaryLabel.setText("Aye, that's a word! Press Enter to send it sailing.");
                } else if (validation == DictionaryService.Result.INVALID) {
                    dictionaryLabel.setText("Not in our dictionary, matey. Change yer letters — no attempt used.");
                } else {
                    dictionaryLabel.setText("No connection. Retry, or turn dictionary OFF in Settings. No attempt used.");
                }
                retryButton.setDisable(validation != DictionaryService.Result.UNAVAILABLE);
                updateEnter();
            }));
        });
        debounce.playFromStart();
    }
    @FXML private void retryWord() {
        soundManager.playSfx(SoundManager.Sfx.BUTTON_1);
        if (canType()) entryChanged();
    }
    private void updateEnter() {
        if (enterButton != null) enterButton.setDisable(!canType()
            || validation != DictionaryService.Result.VALID || !new String(entry).equals(approvedWord));
    }
    @FXML private void showHint() {
        if (!canType()) return;
        soundManager.playSfx(SoundManager.Sfx.BUTTON_1);
        showDialog("Captain's hint", "Word " + (game.isSecond() ? "B / DOWN" : "A / ACROSS")
            + " · " + entry.length + " letters", PirateHints.forWord(game.target())
            + "\n\nThis hint is free. No attempts or points lost!");
    }
    private void showDialog(String title, String heading, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(App.window());
        alert.setTitle(title); alert.setHeaderText(heading); alert.setContentText(content);
        alert.getDialogPane().getStylesheets().add(App.class.getResource("wordwreck.css").toExternalForm());
        alert.getDialogPane().setPrefWidth(520);
        alert.showAndWait();
    }
    private void renderHistory(boolean down) {
        VBox box = down ? historyB : historyA;
        box.getChildren().clear();
        int length = down ? game.level().down().length() : game.level().across().length();
        List<Guess> guesses = game.history(down);
        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);
        if (down) {
            // Each attempt is a column; letters are read from top to bottom.
            for (int attempt = 0; attempt < 5; attempt++) {
                Label number = text(Integer.toString(attempt + 1), "muted");
                number.setMinWidth(22);
                number.setAlignment(javafx.geometry.Pos.CENTER);
                grid.add(number, attempt, 0);
            }
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            for (int position = 0; position < length; position++) {
                String value = "", style = "empty";
                if (attempt < guesses.size()) {
                    Guess guess = guesses.get(attempt);
                    value = "" + guess.word().charAt(position);
                    style = markClass(guess.marks().get(position));
                } else if (down && game.anchorKnown() && position == game.level().anchorB()) {
                    value = "" + game.level().down().charAt(position);
                    style = "correct";
                }
                Label cell = tile(value, style, 22);
                grid.add(cell, down ? attempt : position, down ? position + 1 : attempt);
            }
        }
        box.getChildren().add(grid);
    }
    private void buildKeyboard() {
        for (String row : Arrays.asList("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")) {
            HBox line = new HBox(5); line.setAlignment(javafx.geometry.Pos.CENTER);
            if (row.equals("ZXCVBNM")) {
                enterButton = key("ENTER", this::submitGuess);
                line.getChildren().add(enterButton);
            }
            for (char c : row.toCharArray()) {
                Button b = key("" + c, () -> type(c)); keyButtons.put(c, b); line.getChildren().add(b);
            }
            if (row.equals("ZXCVBNM")) line.getChildren().add(key("DEL", this::erase));
            keyboard.getChildren().add(line);
        }
    }
    private Button key(String title, Runnable action) {
        Button b = new Button(title); b.getStyleClass().add("key"); b.setFocusTraversable(false);
        b.setMinWidth(title.length() > 1 ? 60 : 39);
        b.setOnAction(event -> action.run()); inputButtons.add(b); return b;
    }
    private void renderKeyboard() {
        Map<Character, Mark> marks = new HashMap<>();
        for (Guess guess : game.history(game.isSecond())) for (int i = 0; i < guess.word().length(); i++) {
            char c = guess.word().charAt(i); Mark mark = guess.marks().get(i);
            if (!marks.containsKey(c) || mark.ordinal() > marks.get(c).ordinal()) marks.put(c, mark);
        }
        if (game.isSecond()) marks.put(game.level().down().charAt(game.level().anchorB()), Mark.CORRECT);
        keyButtons.forEach((c, b) -> {
            b.getStyleClass().removeAll("correct", "present", "absent");
            if (marks.containsKey(c)) b.getStyleClass().add(markClass(marks.get(c)));
        });
        for (Button b : inputButtons) b.setDisable(game.outcome() != Outcome.PLAYING);
        updateEnter();
    }
    private void showResult() {
        soundManager.stopMusic();
        if (game.outcome() == Outcome.FULL_VICTORY || game.outcome() == Outcome.PARTIAL_SURVIVAL || game.outcome() == Outcome.SURVIVAL_CLEAR) {
            soundManager.playSfx(SoundManager.Sfx.WIN);
        } else {
            soundManager.playSfx(SoundManager.Sfx.LOSE);
        }
        settings.record(levelIndex, practiceRound, game.score());
        boolean scoreSaved = settings.flush();
        String title, detail;
        switch (game.outcome()) {
            case FULL_VICTORY: title="Both rafts made it."; detail="You solved both words and sent both sharks away. Full victory!"; break;
            case PARTIAL_SURVIVAL: title="One raft still afloat."; detail="Word A was saved. Word B ran out of attempts, but half the voyage survived."; break;
            case SURVIVAL_CLEAR: title="The anchor saved you."; detail="A's outer logs sank, but your anchor held. Solving B earned a survival clear."; break;
            case BOTH_SUNK: title="Lost to the deep."; detail="The anchor bought you another chance, but both words ran out of attempts."; break;
            default: title="The junction broke."; detail="Five misses on A without a green anchor tile caused an immediate sink.";
        }
        resultTitle.setText(title); resultDetail.setText(detail + " Best (" + (practiceRound ? "practice" : "dictionary") + "): "
            + settings.best(levelIndex, practiceRound) + "%." + (scoreSaved ? "" : " Could not save to disk.")); resultScore.setText(game.score() + "%");
        answersLabel.setText("A: " + game.level().across() + "     /     B: " + game.level().down());
        nextButton.setText(levelIndex + 1 == Levels.ALL.size() ? "Back to home" : "Next level →");
        show(resultPane);
    }
    @FXML private void showHelp() {
        soundManager.playSfx(SoundManager.Sfx.BUTTON_1);
        showDialog("How to play", "Save the words. Protect the anchor.",
            "Type directly onto the highlighted raft: A across, then B down.\n\n"
            + "Green = correct spot. Gold = elsewhere. Gray = no match.\n\n"
            + "Five wrong A guesses sink everything unless its anchor was green in an earlier guess. "
            + "If the anchor holds, B unlocks with its shared letter already filled. Type only the missing letters!\n\n"
            + "Solve both: 100%. Save one: 50%. Lose both: 0%.\n\n"
            + "Captain's hint gives a free, pirate-style definition of the active word.\n\n"
            + "Settings lets you turn dictionary checking off for offline practice. "
            + "Offline accepts any complete letter guess; online validates words. Hints work offline in both modes.\n\n"
            + "Best scores are saved separately for practice and dictionary rounds. A round that uses offline mode is practice.\n\n"
            + "Use A–Z, Backspace and Enter, or the onscreen keys. Restart begins a fresh round.");
    }
    private void drawHero() {
        double w = Math.max(heroArt.getWidth(), 500);
        heroCanvas.setWidth(w); heroCanvas.setHeight(280);
        GraphicsContext g = heroCanvas.getGraphicsContext2D();
        OceanArt.sea(g,w,280,seaFrame/4);
        OceanArt.sky(g,w,95); OceanArt.horizon(g,w,87);
        // Foreground islands, a distant beacon, and a passing ship frame the raft.
        OceanArt.island(g,26,144,1.8);
        OceanArt.island(g,118,178,1.2);
        OceanArt.island(g,w-189,134,1.5);
        OceanArt.lighthouse(g,w-294,103);
        OceanArt.sailboat(g,w*.28,92);
        OceanArt.compass(g,w-36,240);
        double ox=w/2-116, oy=153;
        g.setFont(javafx.scene.text.Font.font("Segoe UI",javafx.scene.text.FontWeight.BOLD,27));
        for(int i=0;i<5;i++) {
            OceanArt.log(g,ox+i*48,oy,44);
            g.setFill(Color.web("#fff1bd"));g.fillText("CARGO".substring(i,i+1),ox+i*48+12,oy+31);
        }
        OceanArt.log(g,ox+96,oy-48,44); OceanArt.log(g,ox+96,oy+48,44);
        g.setStroke(Color.web("#ffe4a0"));g.setLineWidth(3);g.strokeRect(ox+96,oy,44,44);
        double drift=settings.motion() ? Math.rint(Math.sin(seaFrame*Math.PI/60)*13) : 0;
        double bob=settings.motion() ? Math.rint(Math.sin(seaFrame*Math.PI/12)*2) : 0;
        OceanArt.wake(g,w*.23+drift-5,250+bob,seaFrame);
        OceanArt.shark(g,w*.23+drift,228+bob,1.2,seaFrame);
        OceanArt.wake(g,w*.72-drift-5,225-bob,seaFrame+6);
        OceanArt.shark(g,w*.72-drift,203-bob,1.1,seaFrame+6);
    }
    private void drawWater() {
        double w=waterArt.getWidth(), h=waterArt.getHeight();
        if(w<=0 || h<=0)return;
        waterCanvas.setWidth(w);waterCanvas.setHeight(h);
        GraphicsContext g=waterCanvas.getGraphicsContext2D();OceanArt.sea(g,w,h,seaFrame/4);
        OceanArt.island(g,15,h*.4,0.65);
        OceanArt.compass(g,w-45,42);
        double drift=settings.motion() ? Math.rint(Math.sin(seaFrame*Math.PI/60)*10) : 0;
        OceanArt.wake(g,w-131+drift,h-23,seaFrame);
        OceanArt.shark(g,w-126+drift,h-46,0.8,seaFrame);
    }
    private void ocean(GraphicsContext g,double w,double h) { OceanArt.sea(g,w,h,seaFrame/4); }
    private void fin(GraphicsContext g,double x,double y) { OceanArt.shark(g,x-8,y+4,0.65,seaFrame); }
    private void resetEffects() {
        if (guessEffect != null) guessEffect.stop();
        crossword.setTranslateX(0); crossword.setScaleX(1); crossword.setScaleY(1);
    }
    private void playGuessEffect(boolean correct) {
        resetEffects();
        if (!settings.motion()) return;
        if (correct) {
            guessEffect=new Timeline(
                new KeyFrame(Duration.millis(110),new KeyValue(crossword.scaleXProperty(),1.04),new KeyValue(crossword.scaleYProperty(),1.04)),
                new KeyFrame(Duration.millis(260),new KeyValue(crossword.scaleXProperty(),1),new KeyValue(crossword.scaleYProperty(),1)));
        } else {
            guessEffect=new Timeline(
                new KeyFrame(Duration.millis(50),new KeyValue(crossword.translateXProperty(),-3)),
                new KeyFrame(Duration.millis(100),new KeyValue(crossword.translateXProperty(),3)),
                new KeyFrame(Duration.millis(150),new KeyValue(crossword.translateXProperty(),-2)),
                new KeyFrame(Duration.millis(220),new KeyValue(crossword.translateXProperty(),0)));
        }
        guessEffect.play();
    }
    private void syncSeaMotion() {
        if (seaMotion == null) {
            seaMotion = new Timeline(new KeyFrame(Duration.millis(100), e -> {
                seaFrame = (seaFrame + 1) % 120;
                if (homePane.isVisible()) drawHero();
                if (gamePane.isVisible()) { drawWater(); drawThreats(); }
            }));
            seaMotion.setCycleCount(Timeline.INDEFINITE);
        }
        if (settings.motion() && (homePane.isVisible() || gamePane.isVisible())) seaMotion.play();
        else seaMotion.stop();
    }
    private void animateThreats() {
        if (sharkMotion != null) sharkMotion.stop();
        if (!settings.motion()) {
            threatA.set(game.wrong(false)); threatB.set(game.wrong(true)); return;
        }
        sharkMotion = new Timeline(new KeyFrame(Duration.millis(450),
            new KeyValue(threatA, game.wrong(false), Interpolator.EASE_BOTH),
            new KeyValue(threatB, game.wrong(true), Interpolator.EASE_BOTH)));
        sharkMotion.play();
    }
    private void drawThreats() {
        if (game == null) return;
        double w = Math.max(sharkTrack.getWidth(), 380); trackCanvas.setWidth(w); trackCanvas.setHeight(60);
        GraphicsContext g = trackCanvas.getGraphicsContext2D(); ocean(g,w,60);
        g.setFont(javafx.scene.text.Font.font("Segoe UI", 13));
        for (int row=0;row<2;row++) {
            boolean down = row==1; double y = 1+row*29;
            g.setFill(Color.web("#d2e8ee")); g.fillText((down?"B":"A") + " · " + branchStatus(down),12,y+16);
            double start = 180, end = w-48;
            for (int i=0;i<=5;i++) { g.setFill(Color.web("#426b7a")); g.fillRect(Math.rint(start+(end-start)*i/5),y+25,3,3); }
            boolean sunk = game.wrong(down)==5 || game.outcome()==Outcome.IMMEDIATE_SINK;
            g.setFill(Color.web(sunk?"#50606b":"#bd8a59")); g.fillRect(w-40,y+8,28,19);
            g.setStroke(Color.web("#f0cf8c"));g.strokeLine(w-36,y+12,w-16,y+12);
            if (game.solved(down)) {
                g.setFill(Color.web("#7edbb5")); g.fillText("SAFE",start,y+18);
            } else fin(g,start+(end-start)*(down ? threatB.get() : threatA.get())/5-10,y+3);
        }
    }
}

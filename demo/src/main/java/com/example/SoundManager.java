package com.example;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Handles sound effects and background music for Word-Wreck!
 * Built to be fault-tolerant: if audio devices or codecs are unavailable,
 * game execution continues seamlessly without exceptions.
 */
public final class SoundManager {
    public enum Sfx {
        BUTTON_1("Button1.wav"),
        BUTTON_2("Button2.wav"),
        START_VOYAGE("StartVoyage1.wav"),
        RIGHT_WORD("RightWord-BuildRaft1.mp3"),
        WRONG_WORD("WrongWord-SharkMove1.wav"),
        WIN("Win1.wav"),
        LOSE("Lose1.wav");

        private final String fileName;
        Sfx(String fileName) { this.fileName = fileName; }
        public String fileName() { return fileName; }
    }

    public enum MusicTrack {
        MENU("MenuMusic1.mp3"),
        GAME_1("GameMusic1.wav", "GameMusic1.mp3", "GameMusic1.ogg"),
        GAME_2("GameMusic2.wav", "GameMusic2.mp3", "GameMusic2.ogg");

        private final String[] candidates;
        MusicTrack(String... candidates) { this.candidates = candidates; }
        public String[] candidates() { return candidates; }
    }

    private boolean soundEnabled;
    private boolean musicEnabled;

    private final Map<Sfx, AudioClip> sfxClips = new EnumMap<>(Sfx.class);
    private final Map<MusicTrack, Media> musicMedia = new EnumMap<>(MusicTrack.class);
    private MediaPlayer currentMusicPlayer;
    private MusicTrack currentTrack;
    private int gameMusicToggle;

    public SoundManager(boolean soundEnabled, boolean musicEnabled) {
        this.soundEnabled = soundEnabled;
        this.musicEnabled = musicEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        } else if (currentTrack != null) {
            playMusic(currentTrack);
        }
    }

    public boolean isSoundEnabled() { return soundEnabled; }
    public boolean isMusicEnabled() { return musicEnabled; }

    public void playSfx(Sfx sfx) {
        if (!soundEnabled) return;
        try {
            AudioClip clip = sfxClips.computeIfAbsent(sfx, k -> {
                URL url = resolveResource(k.fileName());
                if (url == null) return null;
                try {
                    AudioClip ac = new AudioClip(url.toExternalForm());
                    ac.setVolume(0.75);
                    return ac;
                } catch (Throwable t) {
                    return null;
                }
            });
            if (clip != null) {
                clip.play();
            }
        } catch (Throwable t) {
            // Audio hardware or driver issues ignored gracefully
        }
    }

    public void playMenuMusic() {
        playMusic(MusicTrack.MENU);
    }

    public void playGameMusic() {
        MusicTrack track = (gameMusicToggle++ % 2 == 0) ? MusicTrack.GAME_1 : MusicTrack.GAME_2;
        playMusic(track);
    }

    public void playMusic(MusicTrack track) {
        this.currentTrack = track;
        if (!musicEnabled) return;
        try {
            if (currentMusicPlayer != null) {
                currentMusicPlayer.stop();
                currentMusicPlayer.dispose();
                currentMusicPlayer = null;
            }

            Media media = musicMedia.computeIfAbsent(track, k -> {
                for (String candidate : k.candidates()) {
                    URL url = resolveResource(candidate);
                    if (url != null) {
                        try {
                            return new Media(url.toExternalForm());
                        } catch (Throwable ignored) {
                            // Try next candidate
                        }
                    }
                }
                return null;
            });

            if (media != null) {
                currentMusicPlayer = new MediaPlayer(media);
                currentMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                currentMusicPlayer.setVolume(0.40);
                currentMusicPlayer.play();
            }
        } catch (Throwable t) {
            // Audio hardware or driver issues ignored gracefully
        }
    }

    public void stopMusic() {
        try {
            if (currentMusicPlayer != null) {
                currentMusicPlayer.stop();
                currentMusicPlayer.dispose();
                currentMusicPlayer = null;
            }
        } catch (Throwable t) {
            // Audio hardware or driver issues ignored gracefully
        }
    }

    private static URL resolveResource(String filename) {
        URL url = App.class.getResource("assets/sounds/" + filename);
        if (url != null) return url;
        return SoundManager.class.getResource("/com/example/assets/sounds/" + filename);
    }
}

package com.example;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/** Small local settings store; scores never mix practice and dictionary runs. */
public final class GameSettings {
    private final Preferences prefs;
    private boolean online, motion, sound, music;
    public GameSettings() { this(Preferences.userRoot().node("wordwreck/v2")); }
    GameSettings(Preferences prefs) {
        this.prefs = prefs;
        online = prefs.getBoolean("onlineDictionary", false);
        motion = prefs.getBoolean("motion", true);
        sound = prefs.getBoolean("sound", true);
        music = prefs.getBoolean("music", true);
    }
    public boolean online() { return online; }
    public boolean motion() { return motion; }
    public boolean sound() { return sound; }
    public boolean music() { return music; }
    public void set(boolean online, boolean motion) {
        set(online, motion, this.sound, this.music);
    }
    public void set(boolean online, boolean motion, boolean sound, boolean music) {
        this.online = online; this.motion = motion;
        this.sound = sound; this.music = music;
        prefs.putBoolean("onlineDictionary", online);
        prefs.putBoolean("motion", motion);
        prefs.putBoolean("sound", sound);
        prefs.putBoolean("music", music);
    }
    public int best(int level, boolean practice) { return prefs.getInt(key(level, practice), 0); }
    public void record(int level, boolean practice, int score) {
        prefs.putInt(key(level, practice), Math.max(best(level, practice), score));
    }
    public boolean flush() {
        try { prefs.flush(); return true; }
        catch (BackingStoreException ex) { return false; }
    }
    private String key(int level, boolean practice) { return (practice ? "practice." : "dictionary.") + level; }
}

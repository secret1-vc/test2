package com.example.neonlyrics;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Settings {
    public static final String[] SKY_NAMES = {"Off", "Inferno", "Matrix", "Chaos", "Pulse", "Watercolor", "Synthwave"};
    public static final String[] THEME_NAMES = {"Purple", "Pink", "Cyan", "Yellow", "Mint"};
    public static final int[] THEME_RGB = {0x8B5CF6, 0xFF2BD1, 0x2BD1FF, 0xFFE14D, 0x4DFF9E};
    public static final String[] WORD_COLOR_NAMES = {"White", "Neon mix", "Theme"};

    public static int sky = 0;
    public static float strength = 0.6f, speed = 0.5f, pattern = 0.5f;
    public static boolean showPill = true, showWords = true, autoDetect = false;
    public static float wordSize = 0.5f;
    public static int wordColorMode = 0, theme = 0;

    /** Shown in the settings screen; not saved. */
    public static volatile String nowPlaying = "Nothing playing";

    public static int themeRgb() { return THEME_RGB[theme % THEME_RGB.length]; }

    private static Path file() { return NeonLyricsClient.lyricsDir().resolve("settings.properties"); }

    public static void load() {
        try (Reader r = Files.newBufferedReader(file())) {
            Properties p = new Properties();
            p.load(r);
            sky = Integer.parseInt(p.getProperty("sky", "0"));
            strength = Float.parseFloat(p.getProperty("strength", "0.6"));
            speed = Float.parseFloat(p.getProperty("speed", "0.5"));
            pattern = Float.parseFloat(p.getProperty("pattern", "0.5"));
            showPill = Boolean.parseBoolean(p.getProperty("showPill", "true"));
            showWords = Boolean.parseBoolean(p.getProperty("showWords", "true"));
            autoDetect = Boolean.parseBoolean(p.getProperty("autoDetect", "false"));
            wordSize = Float.parseFloat(p.getProperty("wordSize", "0.5"));
            wordColorMode = Integer.parseInt(p.getProperty("wordColorMode", "0"));
            theme = Integer.parseInt(p.getProperty("theme", "0"));
        } catch (Exception ignored) {}
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("sky", String.valueOf(sky));
        p.setProperty("strength", String.valueOf(strength));
        p.setProperty("speed", String.valueOf(speed));
        p.setProperty("pattern", String.valueOf(pattern));
        p.setProperty("showPill", String.valueOf(showPill));
        p.setProperty("showWords", String.valueOf(showWords));
        p.setProperty("autoDetect", String.valueOf(autoDetect));
        p.setProperty("wordSize", String.valueOf(wordSize));
        p.setProperty("wordColorMode", String.valueOf(wordColorMode));
        p.setProperty("theme", String.valueOf(theme));
        try (Writer w = Files.newBufferedWriter(file())) {
            p.store(w, "Neon Lyrics");
        } catch (IOException ignored) {}
    }
}

package com.example.neonlyrics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses .lrc text and tracks playback time. */
public class LyricsPlayer {
    public record Line(long timeMs, String text) {}

    private static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]");

    private volatile List<Line> lines = new ArrayList<>();
    private volatile boolean playing = false;
    private volatile long startNanos = 0;
    private volatile long pausedMs = 0;

    public volatile long offsetMs = 0;

    public boolean hasLyrics() { return !lines.isEmpty(); }
    public boolean isPlaying() { return playing; }
    public List<Line> lines() { return lines; }

    public void loadFile(Path file) throws IOException {
        loadText(String.join("\n", Files.readAllLines(file)));
    }

    public void loadText(String lrc) {
        List<Line> out = new ArrayList<>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = TS.matcher(raw);
            List<Long> times = new ArrayList<>();
            int end = 0;
            while (m.find() && m.start() == end) {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                String frac = m.group(3);
                long ms = frac == null ? 0 : Long.parseLong((frac + "00").substring(0, 3));
                times.add(min * 60_000 + sec * 1000 + ms);
                end = m.end();
            }
            if (times.isEmpty()) continue;
            String text = raw.substring(end).trim();
            for (long t : times) out.add(new Line(t, text));
        }
        out.sort(Comparator.comparingLong(Line::timeMs));
        lines = out;
        stop();
    }

    public void play() { seekTo(0); playing = true; }

    public void pause() {
        if (!playing) return;
        pausedMs = positionMs();
        playing = false;
    }

    public void resume() {
        if (playing || !hasLyrics()) return;
        startNanos = System.nanoTime() - pausedMs * 1_000_000L;
        playing = true;
    }

    public void stop() { playing = false; pausedMs = 0; }

    /** Position in the song (without the user offset). */
    public long positionMs() {
        return playing ? (System.nanoTime() - startNanos) / 1_000_000L : pausedMs;
    }

    public void seekTo(long ms) {
        pausedMs = ms;
        startNanos = System.nanoTime() - ms * 1_000_000L;
    }

    /** Time used to pick the current line (position + offset). */
    public long nowMs() { return positionMs() + offsetMs; }

    public int currentIndex() {
        long t = nowMs();
        List<Line> l = lines;
        int idx = -1;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).timeMs() <= t) idx = i; else break;
        }
        return idx;
    }

    public void seekToLine(int idx) {
        List<Line> l = lines;
        if (l.isEmpty()) return;
        idx = Math.max(0, Math.min(l.size() - 1, idx));
        seekTo(Math.max(0, l.get(idx).timeMs() - offsetMs));
    }
}

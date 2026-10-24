package com.example.neonlyrics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Windows only: asks the OS which song is playing (Spotify, browser, etc.) through the
 * system media controls, then fetches lyrics and keeps them in sync.
 */
public final class MediaWatcher {
    private static final String SCRIPT = """
        [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
        Add-Type -AssemblyName System.Runtime.WindowsRuntime
        $asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
        function Await($op, $type) { $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(-1) | Out-Null; $t.Result }
        [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]
        [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties,Windows.Media.Control,ContentType=WindowsRuntime]
        $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
        $s = $mgr.GetCurrentSession()
        if ($null -eq $s) { '{}'; exit }
        $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
        $tl = $s.GetTimelineProperties()
        $pb = $s.GetPlaybackInfo()
        $status = [string]$pb.PlaybackStatus
        $pos = [int]$tl.Position.TotalMilliseconds
        if ($status -eq 'Playing') { $pos += [int]([DateTimeOffset]::UtcNow - $tl.LastUpdatedTime).TotalMilliseconds }
        @{ title = $p.Title; artist = $p.Artist; pos = $pos; status = $status } | ConvertTo-Json -Compress
        """;

    private static Thread thread;
    private static String lastKey = "";

    public static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static synchronized void start() {
        if (thread != null || !supported()) return;
        thread = new Thread(MediaWatcher::loop, "NeonLyrics-Media");
        thread.setDaemon(true);
        thread.start();
    }

    private static void loop() {
        Path script = NeonLyricsClient.lyricsDir().resolve("media.ps1");
        try { Files.writeString(script, SCRIPT, StandardCharsets.UTF_8); } catch (Exception e) { return; }
        while (true) {
            try {
                Thread.sleep(2000);
                if (!Settings.autoDetect) continue;
                tick(script);
            } catch (InterruptedException e) {
                return;
            } catch (Exception ignored) {}
        }
    }

    private static void tick(Path script) throws Exception {
        Process pr = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                "-ExecutionPolicy", "Bypass", "-File", script.toString())
                .redirectErrorStream(true).start();
        String out = new String(pr.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        pr.waitFor(8, TimeUnit.SECONDS);
        int brace = out.lastIndexOf('{');
        if (brace < 0) return;
        JsonObject o = JsonParser.parseString(out.substring(brace)).getAsJsonObject();
        if (!o.has("title")) {
            if (!lastKey.isEmpty()) { lastKey = ""; NeonLyricsClient.clearSong(); }
            return;
        }

        String title = o.get("title").getAsString();
        String artist = o.has("artist") ? o.get("artist").getAsString() : "";
        long pos = o.get("pos").getAsLong();
        boolean isPlaying = "Playing".equals(o.get("status").getAsString());
        LyricsPlayer p = NeonLyricsClient.PLAYER;

        String key = artist + " - " + title;
        if (!key.equals(lastKey)) {
            lastKey = key;
            NeonLyricsClient.loadFromWeb((artist + " " + title).trim(), isPlaying, pos);
            return;
        }
        if (!p.hasLyrics()) return;
        if (!isPlaying) { p.pause(); return; }
        if (!p.isPlaying()) { p.seekTo(pos); p.resume(); return; }
        if (Math.abs(p.positionMs() - pos) > 700) p.seekTo(pos);
    }
}

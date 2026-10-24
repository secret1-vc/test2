package com.example.neonlyrics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NeonLyricsClient implements ClientModInitializer {
    public static final LyricsPlayer PLAYER = new LyricsPlayer();
    public static volatile boolean openGui = false;

    public static Path lyricsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("neonlyrics");
    }

    @Override
    public void onInitializeClient() {
        try { Files.createDirectories(lyricsDir()); } catch (IOException ignored) {}
        Settings.load();

        HudRenderCallback.EVENT.register((graphics, tracker) ->
                LyricsHud.render(graphics, tracker.getGameTimeDeltaPartialTick(false)));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> LyricsCommand.register(dispatcher));

        // open the GUI after the chat screen has closed
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGui && client.screen == null) {
                openGui = false;
                client.setScreen(new LyricsScreen());
            }
        });

        if (Settings.autoDetect) MediaWatcher.start();
    }

    private static final java.util.concurrent.atomic.AtomicInteger GEN = new java.util.concurrent.atomic.AtomicInteger();

    /** Looks lyrics up on lrclib.net in the background, then starts them at seekMs. */
    public static void loadFromWeb(String query, boolean startPlaying, long seekMs) {
        final int myGen = GEN.incrementAndGet();
        long t0 = System.currentTimeMillis();
        // drop the previous song immediately so its lyrics never linger
        PLAYER.clear();
        Settings.nowPlaying = "Looking up: " + query;
        Settings.songTitle = query;
        Thread t = new Thread(() -> {
            try {
                LrcLib.Result r = LrcLib.search(query);
                if (myGen != GEN.get()) return; // a newer request replaced this one
                if (r == null) {
                    PLAYER.clear();
                    Settings.nowPlaying = "No lyrics found";
                    chat("No synced lyrics found for \"" + query + "\"");
                    return;
                }
                PLAYER.loadText(r.lrc());
                Settings.nowPlaying = r.title() + " - " + r.artist();
                Settings.songTitle = r.title();
                long pos = seekMs + (System.currentTimeMillis() - t0);
                PLAYER.seekTo(pos);
                if (startPlaying) PLAYER.resume();
                chat("Lyrics loaded: " + r.title() + " - " + r.artist());
            } catch (Exception e) {
                if (myGen == GEN.get()) {
                    PLAYER.clear();
                    Settings.nowPlaying = "Lookup failed";
                }
                chat("Lyrics lookup failed: " + e.getMessage());
            }
        }, "NeonLyrics-Fetch");
        t.setDaemon(true);
        t.start();
    }

    /** Called when the OS says nothing is playing any more. */
    public static void clearSong() {
        GEN.incrementAndGet();
        PLAYER.clear();
        Settings.nowPlaying = "Nothing playing";
        Settings.songTitle = "";
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("[Neon Lyrics] " + msg), false);
        });
    }
}

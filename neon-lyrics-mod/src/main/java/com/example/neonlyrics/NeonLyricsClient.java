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

        HudRenderCallback.EVENT.register((graphics, tracker) -> LyricsHud.render(graphics));
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

    /** Looks lyrics up on lrclib.net in the background, then starts them at seekMs. */
    public static void loadFromWeb(String query, boolean startPlaying, long seekMs) {
        long t0 = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            try {
                LrcLib.Result r = LrcLib.search(query);
                if (r == null) {
                    chat("No synced lyrics found for \"" + query + "\"");
                    PLAYER.stop();
                    return;
                }
                PLAYER.loadText(r.lrc());
                Settings.nowPlaying = r.title() + " - " + r.artist();
                long pos = seekMs + (System.currentTimeMillis() - t0);
                PLAYER.seekTo(pos);
                if (startPlaying) PLAYER.resume();
                chat("Lyrics loaded: " + r.title() + " - " + r.artist());
            } catch (Exception e) {
                chat("Lyrics lookup failed: " + e.getMessage());
            }
        }, "NeonLyrics-Fetch");
        t.setDaemon(true);
        t.start();
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.displayClientMessage(Component.literal("[Neon Lyrics] " + msg), false);
        });
    }
}

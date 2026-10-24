package com.example.neonlyrics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class LyricsCommand {
    private LyricsCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        LyricsPlayer p = NeonLyricsClient.PLAYER;

        d.register(literal("lyrics")
            .then(literal("gui").executes(c -> { NeonLyricsClient.openGui = true; return 1; }))
            .then(literal("search")
                .then(argument("query", StringArgumentType.greedyString()).executes(c -> {
                    String q = StringArgumentType.getString(c, "query");
                    c.getSource().sendFeedback(Component.literal("Searching lrclib.net for \"" + q + "\"..."));
                    NeonLyricsClient.loadFromWeb(q, true, 0);
                    return 1;
                })))
            .then(literal("list").executes(c -> {
                try (Stream<Path> s = Files.list(NeonLyricsClient.lyricsDir())) {
                    String names = String.join(", ", s.map(f -> f.getFileName().toString())
                            .filter(n -> n.endsWith(".lrc")).sorted().toList());
                    c.getSource().sendFeedback(Component.literal(names.isEmpty()
                            ? "No .lrc files in " + NeonLyricsClient.lyricsDir() : names));
                } catch (IOException e) {
                    c.getSource().sendError(Component.literal("Couldn't read folder: " + e.getMessage()));
                }
                return 1;
            }))
            .then(literal("play")
                .then(argument("file", StringArgumentType.greedyString()).executes(c -> {
                    String name = StringArgumentType.getString(c, "file");
                    if (!name.endsWith(".lrc")) name += ".lrc";
                    Path f = NeonLyricsClient.lyricsDir().resolve(name).normalize();
                    if (!f.startsWith(NeonLyricsClient.lyricsDir()) || !Files.exists(f)) {
                        c.getSource().sendError(Component.literal("Not found: " + name));
                        return 0;
                    }
                    try {
                        p.loadFile(f);
                        p.play();
                        Settings.nowPlaying = name.replace(".lrc", "");
                        Settings.songTitle = Settings.nowPlaying;
                        c.getSource().sendFeedback(Component.literal("Playing " + name));
                    } catch (Exception e) {
                        c.getSource().sendError(Component.literal("Failed to load: " + e.getMessage()));
                    }
                    return 1;
                })))
            .then(literal("pause").executes(c -> { p.pause(); return 1; }))
            .then(literal("resume").executes(c -> { p.resume(); return 1; }))
            .then(literal("stop").executes(c -> { p.stop(); return 1; }))
            .then(literal("offset")
                .then(argument("ms", IntegerArgumentType.integer(-30000, 30000)).executes(c -> {
                    p.offsetMs = IntegerArgumentType.getInteger(c, "ms");
                    c.getSource().sendFeedback(Component.literal("Offset " + p.offsetMs + " ms"));
                    return 1;
                })))
            .then(literal("auto").executes(c -> {
                Settings.autoDetect = !Settings.autoDetect;
                if (Settings.autoDetect) MediaWatcher.start();
                c.getSource().sendFeedback(Component.literal("Auto-detect " + (Settings.autoDetect ? "ON" : "OFF")
                        + (MediaWatcher.supported() ? "" : " (Windows only)")));
                Settings.save();
                return 1;
            }))
        );
    }
}

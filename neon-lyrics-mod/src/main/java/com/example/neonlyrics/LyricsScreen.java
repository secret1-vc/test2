package com.example.neonlyrics;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/** Turntable settings screen: Sky / Lyrics / Style / More. */
public class LyricsScreen extends Screen {
    private static final int PW = 400, PH = 232;
    private static final String[] TABS = {"Sky", "Lyrics", "Style", "More"};
    private static int tab = 0;

    private int px, py, rx;

    public LyricsScreen() {
        super(Component.literal("Lyrics Cord"));
    }

    @Override
    protected void init() {
        px = (width - PW) / 2;
        py = (height - PH) / 2;
        rx = px + 215;

        Renderable panel = (g, mx, my, d) -> paint(g);
        addRenderableOnly(panel);

        // tabs
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            Component label = Component.literal(TABS[i]).withStyle(i == tab ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE);
            addRenderableWidget(Button.builder(label, b -> { tab = idx; rebuildWidgets(); })
                    .bounds(rx + i * 43, py + 30, 41, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("x"), b -> onClose())
                .bounds(px + PW - 24, py + 8, 14, 14).build());

        // transport under the record
        addRenderableWidget(Button.builder(Component.literal("|<"), b -> {
            LyricsPlayer p = NeonLyricsClient.PLAYER;
            int i = p.currentIndex();
            if (i >= 0 && p.nowMs() - p.lines().get(i).timeMs() < 1500) i--;
            p.seekToLine(i);
        }).bounds(px + 40, py + PH - 30, 28, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Play/Pause"), b -> {
            LyricsPlayer p = NeonLyricsClient.PLAYER;
            if (!p.hasLyrics()) return;
            if (p.isPlaying()) p.pause(); else p.resume();
        }).bounds(px + 72, py + PH - 30, 64, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">|"), b -> {
            LyricsPlayer p = NeonLyricsClient.PLAYER;
            p.seekToLine(p.currentIndex() + 1);
        }).bounds(px + 140, py + PH - 30, 28, 18).build());

        switch (tab) {
            case 0 -> buildSky();
            case 1 -> buildLyrics();
            case 2 -> buildStyle();
            default -> buildMore();
        }
    }

    private void buildSky() {
        for (int i = 0; i < Settings.SKY_NAMES.length; i++) {
            final int idx = i;
            Component label = Component.literal(Settings.SKY_NAMES[i])
                    .withStyle(Settings.sky == i ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE);
            addRenderableWidget(Button.builder(label, b -> { Settings.sky = idx; rebuildWidgets(); })
                    .bounds(rx + (i % 2) * 86, py + 54 + (i / 2) * 20, 84, 18).build());
        }
        addRenderableWidget(new Slide(rx, py + 140, 170, 18, "Strength", Settings.strength, v -> Settings.strength = (float) v));
        addRenderableWidget(new Slide(rx, py + 160, 170, 18, "Speed", Settings.speed, v -> Settings.speed = (float) v));
        addRenderableWidget(new Slide(rx, py + 180, 170, 18, "Pattern", Settings.pattern, v -> Settings.pattern = (float) v));
    }

    private void buildLyrics() {
        addRenderableWidget(toggle(rx, py + 58, 170, "Top bar", () -> Settings.showPill, v -> Settings.showPill = v));
        addRenderableWidget(toggle(rx, py + 82, 170, "Floating words", () -> Settings.showWords, v -> Settings.showWords = v));
        addRenderableWidget(new Slide(rx, py + 106, 170, 18, "Word size", Settings.wordSize, v -> Settings.wordSize = (float) v));
    }

    private void buildStyle() {
        addRenderableWidget(Button.builder(Component.literal("Word color: " + Settings.WORD_COLOR_NAMES[Settings.wordColorMode]), b -> {
            Settings.wordColorMode = (Settings.wordColorMode + 1) % Settings.WORD_COLOR_NAMES.length;
            b.setMessage(Component.literal("Word color: " + Settings.WORD_COLOR_NAMES[Settings.wordColorMode]));
        }).bounds(rx, py + 58, 170, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Theme: " + Settings.THEME_NAMES[Settings.theme]), b -> {
            Settings.theme = (Settings.theme + 1) % Settings.THEME_NAMES.length;
            b.setMessage(Component.literal("Theme: " + Settings.THEME_NAMES[Settings.theme]));
        }).bounds(rx, py + 82, 170, 20).build());
    }

    private void buildMore() {
        addRenderableWidget(Button.builder(Component.literal("Lyrics earlier  (-250 ms)"), b -> NeonLyricsClient.PLAYER.offsetMs -= 250)
                .bounds(rx, py + 58, 170, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Lyrics later  (+250 ms)"), b -> NeonLyricsClient.PLAYER.offsetMs += 250)
                .bounds(rx, py + 82, 170, 20).build());
        Button auto = toggle(rx, py + 106, 170, "Auto-detect song (Windows)", () -> Settings.autoDetect, v -> {
            Settings.autoDetect = v;
            if (v) MediaWatcher.start();
        });
        auto.active = MediaWatcher.supported();
        addRenderableWidget(auto);

        Renderable help = (g, mx, my, d) -> {
            String[] t = {
                "Offset: " + NeonLyricsClient.PLAYER.offsetMs + " ms",
                "Chat: /lyrics search <artist song>",
                "Chat: /lyrics play <file.lrc>"
            };
            for (int i = 0; i < t.length; i++) Gfx.text(g, font, t[i], rx, py + 134 + i * 12, 1f, 0xA9A3C8);
        };
        addRenderableOnly(help);
    }

    private Button toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return Button.builder(Component.literal(label + ": " + (get.getAsBoolean() ? "ON" : "OFF")), b -> {
            boolean nv = !get.getAsBoolean();
            set.accept(nv);
            b.setMessage(Component.literal(label + ": " + (nv ? "ON" : "OFF")));
        }).bounds(x, y, w, 20).build();
    }

    // ---- drawing ------------------------------------------------------------------------

    private void paint(GuiGraphics g) {
        Gfx.roundRect(g, px - 4, py - 4, px + PW + 4, py + PH + 4, 0x388B5CF6);
        Gfx.roundRect(g, px, py, px + PW, py + PH, 0xF0191433);

        Gfx.text(g, font, "Lyrics Cord", rx, py + 14, 1f, 0xFFFFFF);

        int cx = px + 105, cy = py + 108, R = 70;
        double spin = NeonLyricsClient.PLAYER.isPlaying() ? System.nanoTime() / 1e9 * 1.6 : 0.3;

        Gfx.disc(g, cx, cy, R + 4, 0x30A78BFA);
        Gfx.disc(g, cx, cy, R, 0xFF17141F);
        for (int r = 30; r < R - 2; r += 5)
            Gfx.arc(g, cx, cy, r, 0xFF2B2738, 0, Math.PI * 2);
        // moving highlight so it looks like it spins
        for (int r = 30; r < R - 4; r += 5)
            Gfx.arc(g, cx, cy, r, 0xFF4A4468, spin, spin + 0.5);
        Gfx.disc(g, cx, cy, 24, 0xFF6D4AFF);
        Gfx.disc(g, cx, cy, 16, 0xFF9B86FF);
        Gfx.arc(g, cx, cy, 20, 0xFFFFFFFF, spin * 1.0, spin + 0.8);
        Gfx.disc(g, cx, cy, 3, 0xFF000000);

        // tonearm
        g.fill(cx + R + 8, cy - R + 8, cx + R + 10, cy + R - 16, 0xFFDCDCEE);
        Gfx.disc(g, cx + R + 9, cy - R + 8, 5, 0xFFF2F2FA);

        Gfx.text(g, font, Settings.nowPlaying, rx, py + PH - 16, 1f, 0x9C96BA);
    }

    @Override
    public void onClose() {
        Settings.save();
        super.onClose();
    }

    private static class Slide extends AbstractSliderButton {
        private final String label;
        private final DoubleConsumer set;

        Slide(int x, int y, int w, int h, String label, double v, DoubleConsumer set) {
            super(x, y, w, h, Component.empty(), v);
            this.label = label;
            this.set = set;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + (int) Math.round(value * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            set.accept(value);
        }
    }
}

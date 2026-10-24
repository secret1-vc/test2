package com.example.neonlyrics;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

public final class LyricsHud {
    private static final int[] NEON = {0xFF2BD1, 0x2BD1FF, 0xB66BFF, 0xFFE14D, 0x4DFF9E};
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    private LyricsHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) return;
        LyricsPlayer p = NeonLyricsClient.PLAYER;
        if (!p.hasLyrics() || !p.isPlaying()) return;

        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        double t = System.nanoTime() / 1e9;

        if (Settings.sky != 0) SkyFx.render(g, font, w, h, t);

        List<LyricsPlayer.Line> lines = p.lines();
        int i = p.currentIndex();
        long now = p.nowMs();

        if (Settings.showWords) words(g, font, lines, i, now, w, h);
        if (Settings.showPill && i >= 0) pill(g, font, lines, i, now, w);
    }

    private static long lineEnd(List<LyricsPlayer.Line> lines, int k) {
        return k + 1 < lines.size() ? lines.get(k + 1).timeMs() : lines.get(k).timeMs() + 4000;
    }

    // ---- karaoke bar ------------------------------------------------------------------

    private static void pill(GuiGraphics g, Font font, List<LyricsPlayer.Line> lines, int i, long now, int w) {
        String text = lines.get(i).text();
        long start = lines.get(i).timeMs();
        long dur = Math.max(500, lineEnd(lines, i) - start);
        float prog = Math.max(0f, Math.min(1f, (now - start) / (dur * 0.9f)));

        int pillW = Math.max(180, Math.min(340, w - 160));
        int pillH = 22;
        int px = (w - pillW) / 2, py = 6;
        int accent = Settings.themeRgb();

        Gfx.roundRect(g, px, py, px + pillW, py + pillH, 0xE61A1530);

        String clock = LocalTime.now().format(CLOCK);
        Gfx.text(g, font, clock, px - font.width(clock) - 10, py + 7, 1f, 0xE8E8F0);

        Gfx.disc(g, px + 13, py + 11, 7, 0xFF2A2548);
        Gfx.disc(g, px + 13, py + 11, 3, Gfx.argb(1f, accent));
        Gfx.text(g, font, "...", px + pillW - 22, py + 5, 1f, accent);

        if (text.isEmpty()) return;
        int avail = pillW - 56;
        int tw = font.width(text);
        float sc = tw > avail ? (float) avail / tw : 1f;
        int tx = px + 28;
        int ty = py + 7;

        int sung = (int) (text.length() * prog);
        int sungW = (int) (font.width(text.substring(0, sung)) * sc);
        if (sungW > 0)
            g.fill(tx - 3, py + 3, tx + sungW + 3, py + pillH - 3, Gfx.argb(0.95f, accent));

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(tx, ty);
        pose.scale(sc, sc);
        Gfx.text(g, font, text, 0, 0, 1f, 0x8C86A6);
        if (sung > 0) Gfx.text(g, font, text.substring(0, sung), 0, 0, 1f, 0xFFFFFF);
        pose.popMatrix();
    }

    // ---- floating words -----------------------------------------------------------------

    private static void words(GuiGraphics g, Font font, List<LyricsPlayer.Line> lines, int cur, long now, int w, int h) {
        if (cur < 0) return;
        float baseScale = 1.2f + Settings.wordSize * 1.6f;
        for (int k = Math.max(0, cur - 1); k <= cur; k++) {
            String text = lines.get(k).text();
            if (text.isEmpty()) continue;
            long start = lines.get(k).timeMs();
            long end = lineEnd(lines, k);
            long dur = Math.max(800, end - start);
            if (now > end + 1200) continue;
            float outA = now > end ? 1f - (now - end) / 1200f : 1f;
            String[] ws = text.split("\\s+");
            for (int j = 0; j < ws.length; j++) {
                long appear = start + (long) (dur * 0.85 * j / ws.length);
                if (now < appear) continue;
                float age = (now - appear) / 1000f;
                float inA = Math.min(1f, (now - appear) / 220f);
                Random r = new Random(k * 7919L + j * 104729L + 17);
                float cx = w * (0.14f + 0.72f * r.nextFloat());
                float cy = h * (0.20f + 0.50f * r.nextFloat()) - age * 3f;
                float ang = (r.nextFloat() - 0.5f) * 0.5f;
                float sc = baseScale * (0.8f + r.nextFloat() * 0.6f);
                float pop = 1f + 0.35f * Math.max(0f, 1f - (now - appear) / 160f);
                int rgb = switch (Settings.wordColorMode) {
                    case 1 -> NEON[r.nextInt(NEON.length)];
                    case 2 -> Settings.themeRgb();
                    default -> 0xFFFFFF;
                };
                drawWord(g, font, ws[j], cx, cy, ang, sc * pop, rgb, Math.min(inA, outA));
            }
        }
    }

    private static void drawWord(GuiGraphics g, Font font, String word, float cx, float cy, float ang,
                                 float scale, int rgb, float alpha) {
        if (alpha < 0.05f) return;
        Component c = Component.literal(word).withStyle(ChatFormatting.ITALIC);
        int x = -font.width(c) / 2;
        int y = -font.lineHeight / 2;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate(ang);
        pose.scale(scale, scale);
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                if (dx != 0 || dy != 0) Gfx.text(g, font, c, x + dx * 2, y + dy * 2, alpha * 0.2f, rgb);
        Gfx.text(g, font, c, x, y, alpha, Gfx.mix(rgb, 0xFFFFFF, 0.75f));
        pose.popMatrix();
    }
}

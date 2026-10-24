package com.example.neonlyrics;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Random;

/** Full-screen animated overlays (2D, drawn on the HUD). */
final class SkyFx {
    private SkyFx() {}

    static void render(GuiGraphics g, Font font, int w, int h, double t) {
        float s = Settings.strength;
        float pat = Settings.pattern;
        double tt = t * (0.3 + Settings.speed * 2.0);
        switch (Settings.sky) {
            case 1 -> inferno(g, w, h, tt, s, pat);
            case 2 -> matrix(g, font, w, h, tt, s, pat);
            case 3 -> chaos(g, w, h, tt, s, pat);
            case 4 -> pulse(g, w, h, tt, s);
            case 5 -> watercolor(g, w, h, tt, s, pat);
            case 6 -> synthwave(g, w, h, tt, s);
            default -> {}
        }
    }

    private static void inferno(GuiGraphics g, int w, int h, double t, float s, float pat) {
        float flick = (float) (0.8 + 0.2 * Math.sin(t * 7));
        g.fillGradient(0, (int) (h * 0.4), w, h, Gfx.argb(0f, 0xFF6A00), Gfx.argb(0.6f * s * flick, 0xFF3300));
        int n = (int) (25 + pat * 70);
        for (int i = 0; i < n; i++) {
            Random r = new Random(i * 7919L);
            double life = ((t * (0.25 + r.nextDouble() * 0.5)) + r.nextDouble()) % 1.0;
            int x = (int) (r.nextDouble() * w + Math.sin(t * 2 + i) * 6);
            int y = (int) (h - life * h * 0.55);
            int sz = 2 + r.nextInt(3);
            int col = r.nextBoolean() ? 0xFFB000 : 0xFF5500;
            g.fill(x, y, x + sz, y + sz * 2, Gfx.argb((float) (1 - life) * s, col));
        }
    }

    private static void matrix(GuiGraphics g, Font font, int w, int h, double t, float s, float pat) {
        String chars = "01ABCDEFGHJKLMNPQRSTUVWXYZ789";
        int colW = 12;
        for (int c = 0; c < w / colW; c++) {
            Random r = new Random(c * 104729L);
            if (r.nextFloat() > 0.25f + 0.75f * pat) continue;
            double speed = 40 + r.nextDouble() * 90;
            double head = ((t * speed + r.nextInt(2000)) % (h + 260)) - 60;
            for (int k = 0; k < 14; k++) {
                int y = (int) (head - k * 10);
                if (y < -10 || y > h) continue;
                char ch = chars.charAt((c * 31 + k * 7 + (int) (t * 9)) % chars.length());
                float a = (1f - k / 14f) * s;
                int col = k == 0 ? 0xCCFFDD : 0x00FF66;
                Gfx.text(g, font, String.valueOf(ch), c * colW + 2, y, a, col);
            }
        }
    }

    private static void chaos(GuiGraphics g, int w, int h, double t, float s, float pat) {
        Random r = new Random((long) (t * 6));
        int n = (int) (8 + pat * 40);
        for (int i = 0; i < n; i++) {
            int sz = 10 + r.nextInt((int) (30 + pat * 60));
            int x = r.nextInt(w), y = r.nextInt(h);
            int col = java.awt.Color.HSBtoRGB(r.nextFloat(), 0.8f, 1f) & 0xFFFFFF;
            g.fill(x, y, x + sz, y + sz, Gfx.argb(0.25f * s, col));
        }
    }

    private static void pulse(GuiGraphics g, int w, int h, double t, float s) {
        float beat = (float) Math.pow(Math.abs(Math.sin(t * 3)), 4);
        int rgb = Settings.themeRgb();
        float a = (0.15f + 0.5f * beat) * s;
        g.fillGradient(0, 0, w, (int) (h * 0.35), Gfx.argb(a, rgb), Gfx.argb(0f, rgb));
        g.fillGradient(0, (int) (h * 0.65), w, h, Gfx.argb(0f, rgb), Gfx.argb(a, rgb));
    }

    private static void watercolor(GuiGraphics g, int w, int h, double t, float s, float pat) {
        int blobs = 4 + (int) (pat * 5);
        for (int b = 0; b < blobs; b++) {
            double cx = w * (0.5 + 0.4 * Math.sin(t * 0.3 + b * 1.7));
            double cy = h * (0.5 + 0.35 * Math.cos(t * 0.25 + b * 2.3));
            int rgb = java.awt.Color.HSBtoRGB((float) ((b * 0.13 + t * 0.02) % 1.0), 0.5f, 1f) & 0xFFFFFF;
            for (int layer = 0; layer < 6; layer++) {
                int half = (int) (w * (0.20 - layer * 0.03));
                g.fill((int) cx - half, (int) cy - half, (int) cx + half, (int) cy + half, Gfx.argb(0.05f * s, rgb));
            }
        }
    }

    private static void synthwave(GuiGraphics g, int w, int h, double t, float s) {
        int hz = (int) (h * 0.62);
        g.fillGradient(0, 0, w, hz, Gfx.argb(0.55f * s, 0x1A0033), Gfx.argb(0.55f * s, 0xFF2BD1));
        g.fillGradient(0, hz, w, h, Gfx.argb(0.55f * s, 0x2B0050), Gfx.argb(0.15f * s, 0x100020));
        // sun with scanline gaps
        int r = (int) (h * 0.13);
        int cx = w / 2;
        for (int dy = -r; dy <= 0; dy++) {
            if (dy > -r / 2 && ((dy + 1000) % 6) < 2) continue;
            int hw = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - hw, hz + dy, cx + hw, hz + dy + 1, Gfx.argb(0.85f * s, 0xFFB347));
        }
        // moving horizontal grid
        for (int i = 0; i < 12; i++) {
            double z = ((i + t * 0.8) % 12) / 12.0;
            int y = (int) (hz + (h - hz) * z * z);
            g.fill(0, y, w, y + 1, Gfx.argb((float) (0.7 * s * z), 0xFF2BD1));
        }
        // perspective lines
        for (int k = -8; k <= 8; k += 2) {
            for (int y = hz; y < h; y += 2) {
                double z = (double) (y - hz) / (h - hz);
                int x = (int) (cx + k * (w * 0.012 + w * 0.1 * z));
                g.fill(x, y, x + 1, y + 2, Gfx.argb((float) (0.6 * s * z), 0xFF2BD1));
            }
        }
    }
}

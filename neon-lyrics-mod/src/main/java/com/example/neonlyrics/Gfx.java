package com.example.neonlyrics;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Tiny drawing helpers built only from fill() and drawString(). */
final class Gfx {
    private Gfx() {}

    static int argb(float a, int rgb) {
        int ai = (int) (Math.max(0f, Math.min(1f, a)) * 255f);
        return (ai << 24) | (rgb & 0xFFFFFF);
    }

    static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, color);
        }
    }

    static void arc(GuiGraphics g, int cx, int cy, double r, int color, double a0, double a1) {
        int n = (int) Math.max(8, (a1 - a0) * r / 1.5);
        for (int i = 0; i < n; i++) {
            double a = a0 + (a1 - a0) * i / n;
            int x = (int) Math.round(cx + Math.cos(a) * r);
            int y = (int) Math.round(cy + Math.sin(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int[] ins = {4, 2, 1, 1};
        for (int i = 0; i < ins.length; i++) {
            g.fill(x1 + ins[i], y1 + i, x2 - ins[i], y1 + i + 1, color);
            g.fill(x1 + ins[i], y2 - i - 1, x2 - ins[i], y2 - i, color);
        }
        g.fill(x1, y1 + ins.length, x2, y2 - ins.length, color);
    }

    /** Draw text, skipping near-invisible alpha (Minecraft treats alpha 0-3 as opaque). */
    static void text(GuiGraphics g, Font f, Component c, int x, int y, float alpha, int rgb) {
        if (alpha < 0.05f) return;
        g.drawString(f, c, x, y, argb(alpha, rgb), false);
    }

    static void text(GuiGraphics g, Font f, String s, int x, int y, float alpha, int rgb) {
        if (alpha < 0.05f) return;
        g.drawString(f, s, x, y, argb(alpha, rgb), false);
    }
}

package com.example.neonlyrics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Lyric words that live in the world: they spawn on the ground in front of you, hover,
 * and (optionally) drift toward you like nextbots. Drawn by projecting world positions
 * onto the HUD, so no 3D render hooks are needed.
 */
final class WorldWords {
    private WorldWords() {}

    private static final class Word {
        String text;
        double x, y, z, phase, speed;
        float size, roll;
        int rgb;
        long appearMs, endMs;
    }

    private record Draw(Word w, double sx, double sy, double depth, float scale, float alpha) {}

    private static final List<Word> WORDS = new ArrayList<>();
    private static final Random RNG = new Random();
    private static List<LyricsPlayer.Line> spawnedFor = null;
    private static int spawnedLine = -2, spawnedWords = 0;
    private static long lastNanos = 0;

    static void render(GuiGraphics g, Font font, List<LyricsPlayer.Line> lines, int cur,
                       long now, int w, int h, float partial) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        var level = mc.level;
        if (player == null || level == null) { WORDS.clear(); return; }

        if (spawnedFor != lines) {
            WORDS.clear();
            spawnedFor = lines;
            spawnedLine = -2;
            spawnedWords = 0;
        }
        if (cur < spawnedLine) { // user skipped backwards
            WORDS.clear();
            spawnedLine = -2;
            spawnedWords = 0;
        }
        WORDS.removeIf(wd -> now < wd.appearMs - 50 || now > wd.endMs + 1200);

        Vec3 eye = player.getEyePosition(partial);

        if (cur >= 0) {
            if (cur != spawnedLine) { spawnedLine = cur; spawnedWords = 0; }
            String text = lines.get(cur).text();
            if (!text.isEmpty()) {
                long start = lines.get(cur).timeMs();
                long end = LyricsHud.lineEnd(lines, cur);
                long dur = Math.max(800, end - start);
                String[] ws = text.split("\\s+");
                int n = 0;
                for (int j = 0; j < ws.length; j++) {
                    long appear = start + (long) (dur * 0.85 * j / ws.length);
                    if (now >= appear) n = j + 1;
                }
                for (int j = spawnedWords; j < n; j++) {
                    long appear = start + (long) (dur * 0.85 * j / ws.length);
                    spawn(level, eye, player.getViewYRot(partial), ws[j], appear, end);
                }
                spawnedWords = Math.max(spawnedWords, n);
            }
        }

        // move
        long nanos = System.nanoTime();
        double dt = lastNanos == 0 ? 0.016 : Math.min(0.1, (nanos - lastNanos) / 1e9);
        lastNanos = nanos;
        double t = nanos / 1e9;
        for (Word wd : WORDS) {
            if (Settings.chase) {
                double dx = eye.x - wd.x, dz = eye.z - wd.z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > 2.5) {
                    wd.x += dx / d * wd.speed * dt;
                    wd.z += dz / d * wd.speed * dt;
                }
            }
            double target = groundY(level, wd.x, wd.z, eye.y) + 0.35 + wd.size * 0.5
                    + 0.18 * Math.sin(t * 2.0 + wd.phase);
            wd.y += (target - wd.y) * Math.min(1.0, dt * 6.0);
        }

        // project
        double yaw = Math.toRadians(player.getViewYRot(partial));
        double pitch = Math.toRadians(player.getViewXRot(partial));
        double cp = Math.cos(pitch), sp = Math.sin(pitch), cy = Math.cos(yaw), sy = Math.sin(yaw);
        double fX = -sy * cp, fY = -sp, fZ = cy * cp;
        double rX = -cy, rY = 0, rZ = -sy;
        double uX = rY * fZ - rZ * fY, uY = rZ * fX - rX * fZ, uZ = rX * fY - rY * fX;
        double tanHalf = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);

        List<Draw> draws = new ArrayList<>();
        for (Word wd : WORDS) {
            double dx = wd.x - eye.x, dy = wd.y - eye.y, dz = wd.z - eye.z;
            double depth = dx * fX + dy * fY + dz * fZ;
            if (depth < 0.6) continue;
            double xr = dx * rX + dy * rY + dz * rZ;
            double yu = dx * uX + dy * uY + dz * uZ;
            double ppb = (h / 2.0) / (tanHalf * depth);
            double sx = w / 2.0 + xr * ppb;
            double sy2 = h / 2.0 - yu * ppb;
            if (sx < -300 || sx > w + 300 || sy2 < -300 || sy2 > h + 300) continue;
            float scale = (float) Math.min(14.0, wd.size * ppb / font.lineHeight);
            if (scale < 0.25f) continue;
            if (blocked(level, eye, wd)) continue;

            float inA = Math.min(1f, (now - wd.appearMs) / 250f);
            float outA = now > wd.endMs ? 1f - (now - wd.endMs) / 1200f : 1f;
            float nearA = (float) Math.max(0.0, Math.min(1.0, (depth - 0.6) / 1.5));
            draws.add(new Draw(wd, sx, sy2, depth, scale, Math.min(inA, Math.min(outA, nearA))));
        }
        draws.sort(Comparator.comparingDouble(Draw::depth).reversed());
        for (Draw d : draws) {
            float pop = 1f + 0.3f * Math.max(0f, 1f - (now - d.w().appearMs) / 160f);
            LyricsHud.drawWord(g, font, d.w().text, (float) d.sx(), (float) d.sy(), d.w().roll,
                    d.scale() * pop, d.w().rgb, d.alpha());
        }
    }

    private static void spawn(Level level, Vec3 eye, float yawDeg, String text, long appear, long end) {
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double dist = 5 + RNG.nextDouble() * 5;
        double lat = (RNG.nextDouble() - 0.5) * 9;
        Word wd = new Word();
        wd.text = text;
        wd.x = eye.x + fx * dist + rx * lat;
        wd.z = eye.z + fz * dist + rz * lat;
        wd.size = (float) ((0.9 + Settings.wordSize * 1.3) * (0.8 + RNG.nextDouble() * 0.6));
        wd.y = groundY(level, wd.x, wd.z, eye.y) + 0.35 + wd.size * 0.5;
        wd.phase = RNG.nextDouble() * 6.28;
        wd.speed = 0.9 + RNG.nextDouble() * 1.1;
        wd.roll = (float) ((RNG.nextDouble() - 0.5) * 0.5);
        wd.rgb = switch (Settings.wordColorMode) {
            case 1 -> LyricsHud.NEON[RNG.nextInt(LyricsHud.NEON.length)];
            case 2 -> Settings.themeRgb();
            default -> 0xFFFFFF;
        };
        wd.appearMs = appear;
        wd.endMs = end;
        WORDS.add(wd);
    }

    /** Top of the first solid block found scanning down from near the player's eye. */
    private static double groundY(Level level, double x, double z, double eyeY) {
        int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
        int top = (int) Math.floor(eyeY) + 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = top; y >= top - 18; y--) {
            pos.set(bx, y, bz);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return y + 1;
        }
        return eyeY - 1.6;
    }

    /** True if a solid block sits between the camera and the word. */
    private static boolean blocked(Level level, Vec3 eye, Word wd) {
        double cx = wd.x, cy = wd.y + wd.size * 0.3, cz = wd.z;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 1; i <= 8; i++) {
            double f = i / 9.0;
            pos.set((int) Math.floor(eye.x + (cx - eye.x) * f),
                    (int) Math.floor(eye.y + (cy - eye.y) * f),
                    (int) Math.floor(eye.z + (cz - eye.z) * f));
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return true;
        }
        return false;
    }
}

package com.yourcheat.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.awt.*;

public class RenderUtil {

    // ── Закруглённый прямоугольник ─────────────────────────────────
    public static void drawRoundedRect(DrawContext ctx, float x, float y, float w, float h, float r, int color) {
        int a = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        r = Math.min(r, Math.min(w, h) / 2f);

        // centre + side strips
        ctx.fill((int)(x + r), (int)y, (int)(x + w - r), (int)(y + h), color);
        ctx.fill((int)x, (int)(y + r), (int)(x + r), (int)(y + h - r), color);
        ctx.fill((int)(x + w - r), (int)(y + r), (int)(x + w), (int)(y + h - r), color);

        // corners
        fillCircleQuad(ctx, x + r,     y + r,     r, 2, color);
        fillCircleQuad(ctx, x + w - r, y + r,     r, 1, color);
        fillCircleQuad(ctx, x + r,     y + h - r, r, 3, color);
        fillCircleQuad(ctx, x + w - r, y + h - r, r, 4, color);
    }

    // overload с толщиной обводки
    public static void drawRoundedRect(DrawContext ctx, float x, float y, float w, float h, float r, int color, float border) {
        drawRoundedRect(ctx, x, y, w, h, r, color);
    }

    public static void drawRoundedRectOutline(DrawContext ctx, float x, float y, float w, float h, float r, float t, int color, int ignored) {
        // top, bottom, left, right
        ctx.fill((int)(x + r), (int)y, (int)(x + w - r), (int)(y + t), color);
        ctx.fill((int)(x + r), (int)(y + h - t), (int)(x + w - r), (int)(y + h), color);
        ctx.fill((int)x, (int)(y + r), (int)(x + t), (int)(y + h - r), color);
        ctx.fill((int)(x + w - t), (int)(y + r), (int)(x + w), (int)(y + h - r), color);
    }

    private static void fillCircleQuad(DrawContext ctx, float cx, float cy, float r, int q, int color) {
        int ri = (int) Math.ceil(r);
        for (int i = 0; i < ri; i++) {
            int lh = (int)(Math.sqrt(r * r - i * i) + 0.5);
            switch (q) {
                case 1 -> ctx.fill((int)cx + i, (int)(cy - lh), (int)cx + i + 1, (int)cy, color);
                case 2 -> ctx.fill((int)cx - i - 1, (int)(cy - lh), (int)cx - i, (int)cy, color);
                case 3 -> ctx.fill((int)cx - i - 1, (int)cy, (int)cx - i, (int)(cy + lh), color);
                case 4 -> ctx.fill((int)cx + i, (int)cy, (int)cx + i + 1, (int)(cy + lh), color);
            }
        }
    }

    // ── Blur (заглушка — настоящий blur требует шейдеры) ──────────
    public static class Blur {
        public static void drawBlur(DrawContext ctx, float x, float y, float w, float h, float r, int strength, int color) {
            // Имитируем blur полупрозрачным прямоугольником
            drawRoundedRect(ctx, x - 2, y - 2, w + 4, h + 4, r + 1, new Color(0, 0, 0, 40).getRGB());
        }
    }

    // ── Интерполяция цвета ─────────────────────────────────────────
    public static int lerpColor(int c1, int c2, float t) {
        int a1=(c1>>24)&0xFF, r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int a2=(c2>>24)&0xFF, r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        return ((int)(a1+(a2-a1)*t)<<24)|((int)(r1+(r2-r1)*t)<<16)|((int)(g1+(g2-g1)*t)<<8)|(int)(b1+(b2-b1)*t);
    }

    // ── Применить alpha к цвету ────────────────────────────────────
    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}


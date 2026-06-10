package com.yourcheat.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * Утилиты рендера для ClickGUI.
 * Все методы используют immediate-mode через Tessellator (совместимо с 1.16.5 Forge).
 */
public class DrawUtils {

    // ── Прямоугольник ──────────────────────────────────────────────
    public static void fillRect(MatrixStack ms, int x, int y, int w, int h, int color) {
        net.minecraft.client.gui.AbstractGui.fill(ms, x, y, x + w, y + h, color);
    }

    // ── Закруглённый прямоугольник (через fillRect-сектора) ────────
    public static void fillRoundRect(MatrixStack ms, int x, int y, int w, int h, int r, int color) {
        r = Math.min(r, Math.min(w, h) / 2);
        // центральный прямоугольник
        fillRect(ms, x + r, y,     w - r * 2, h,     color);
        fillRect(ms, x,     y + r, r,          h - r * 2, color);
        fillRect(ms, x + w - r, y + r, r, h - r * 2, color);
        // углы (рисуем четверть кружка как пиксельные апроксимации)
        drawCircleQuadrant(ms, x + r,     y + r,     r, 2, color); // top-left
        drawCircleQuadrant(ms, x + w - r, y + r,     r, 1, color); // top-right
        drawCircleQuadrant(ms, x + r,     y + h - r, r, 3, color); // bottom-left
        drawCircleQuadrant(ms, x + w - r, y + h - r, r, 4, color); // bottom-right
    }

    /**
     * Рисует четверть кружка (аппроксимация пикселями).
     * quadrant: 1=TR, 2=TL, 3=BL, 4=BR
     */
    private static void drawCircleQuadrant(MatrixStack ms, int cx, int cy, int r, int q, int color) {
        for (int i = 0; i < r; i++) {
            int lineH = (int)(Math.sqrt((double)r * r - (double)i * i) + 0.5);
            switch (q) {
                case 1: fillRect(ms, cx + i, cy - lineH, 1, lineH, color); break;
                case 2: fillRect(ms, cx - i - 1, cy - lineH, 1, lineH, color); break;
                case 3: fillRect(ms, cx - i - 1, cy, 1, lineH, color); break;
                case 4: fillRect(ms, cx + i, cy, 1, lineH, color); break;
            }
        }
    }

    // ── Тень (несколько полупрозрачных прямоугольников) ────────────
    public static void drawShadow(MatrixStack ms, int x, int y, int w, int h, int steps, int baseColor) {
        int a = (baseColor >> 24) & 0xFF;
        for (int i = steps; i > 0; i--) {
            int alpha = (int)(a * ((float)(steps - i + 1) / steps));
            int c = (alpha << 24) | (baseColor & 0x00FFFFFF);
            fillRect(ms, x + i, y + i, w - i * 2, h - i * 2, c);
        }
    }

    // ── Обводка (border) ───────────────────────────────────────────
    public static void drawBorder(MatrixStack ms, int x, int y, int w, int h, int thickness, int color) {
        fillRect(ms, x, y, w, thickness, color);           // top
        fillRect(ms, x, y + h - thickness, w, thickness, color); // bottom
        fillRect(ms, x, y + thickness, thickness, h - thickness * 2, color); // left
        fillRect(ms, x + w - thickness, y + thickness, thickness, h - thickness * 2, color); // right
    }

    // ── Gradient fill (top→bottom) ─────────────────────────────────
    // Использует встроенный GuiScreen.fillGradient через ClickGUI extends Screen
    // При необходимости вызывай fillGradient(ms, x, y, x+w, y+h, c1, c2) из Screen

    // ── Линия ──────────────────────────────────────────────────────
    public static void drawLine(MatrixStack ms, int x1, int y1, int x2, int y2, int color) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int w = Math.max(1, maxX - minX);
        int h = Math.max(1, maxY - minY);
        fillRect(ms, minX, minY, w, h, color);
    }

    // ── Круг (аппроксимация через scanline) ────────────────────────
    public static void fillCircle(MatrixStack ms, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int)Math.sqrt((double)(radius * radius - dy * dy));
            fillRect(ms, cx - dx, cy + dy, dx * 2, 1, color);
        }
    }

    public static void drawCircleOutline(MatrixStack ms, int cx, int cy, int radius, int thickness, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int outerDx = (int)Math.sqrt((double)(radius * radius - dy * dy));
            int innerR = radius - thickness;
            int innerDx = (innerR > 0) ? (int)Math.sqrt(Math.max(0.0, (double)(innerR * innerR - dy * dy))) : 0;
            if (outerDx > innerDx) {
                fillRect(ms, cx - outerDx, cy + dy, outerDx - innerDx, 1, color);
                fillRect(ms, cx + innerDx, cy + dy, outerDx - innerDx, 1, color);
            }
        }
    }

    // ── ARGB компоненты ────────────────────────────────────────────
    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    public static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1>>24)&0xFF, r1=(c1>>16)&0xFF, g1=(c1>>8)&0xFF, b1=c1&0xFF;
        int a2 = (c2>>24)&0xFF, r2=(c2>>16)&0xFF, g2=(c2>>8)&0xFF, b2=c2&0xFF;
        return rgba(
            (int)(r1 + (r2-r1)*t),
            (int)(g1 + (g2-g1)*t),
            (int)(b1 + (b2-b1)*t),
            (int)(a1 + (a2-a1)*t)
        );
    }

    // ── Текст по центру ────────────────────────────────────────────
    public static void drawCenteredText(MatrixStack ms, String text, int x, int y, int w, int color) {
        int textW = Minecraft.getInstance().fontRenderer.getStringWidth(text);
        Minecraft.getInstance().fontRenderer.drawString(ms, text, x + (w - textW) / 2f, y, color);
    }
}


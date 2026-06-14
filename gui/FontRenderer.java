package com.yourcheat.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Простой TTF-рендерер через java.awt.Font → BufferedImage → Minecraft texture.
 * Рендерит строки в отдельный BufferedImage и загружает как нативную текстуру.
 */
public class FontRenderer {

    public static final FontRenderer INSTANCE = new FontRenderer();

    private Font awtFont;
    private boolean ready = false;

    // Кэш: строка+размер → NativeImageBackedTexture id
    private final Map<String, net.minecraft.client.texture.NativeImage> cache = new HashMap<>();

    private FontRenderer() {
        try {
            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
            Identifier fontId = Identifier.of("yourcheat", "fonts/nexa.ttf");
            try (InputStream is = rm.open(fontId)) {
                awtFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 16f);
                ready = true;
            }
        } catch (Exception e) {
            System.err.println("[MeowlDLC] FontRenderer: failed to load Nexa font: " + e.getMessage());
            ready = false;
        }
    }

    public boolean isReady() { return ready; }

    public int getWidth(String text, float scale) {
        if (!ready) return MinecraftClient.getInstance().textRenderer.getWidth(text) * (int)scale;
        Font scaled = awtFont.deriveFont(Font.PLAIN, 16f * scale);
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dummy.createGraphics();
        g.setFont(scaled);
        int w = g.getFontMetrics().stringWidth(text);
        g.dispose();
        return w;
    }

    /**
     * Рисует строку через DrawContext используя java.awt рендеринг.
     * Создаёт BufferedImage → загружает как NativeImage → рисует через drawTexture.
     */
    public void drawString(DrawContext ctx, String text, float x, float y, float scale, int color) {
        if (!ready || text == null || text.isEmpty()) return;

        try {
            Font scaled = awtFont.deriveFont(Font.PLAIN, 16f * scale);
            BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gd = dummy.createGraphics();
            gd.setFont(scaled);
            FontMetrics fm = gd.getFontMetrics();
            int w = fm.stringWidth(text) + 4;
            int h = fm.getHeight() + 4;
            gd.dispose();

            // Рисуем в BufferedImage
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(scaled);

            int r = (color >> 16) & 0xFF;
            int gr = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int a = (color >> 24) & 0xFF;
            if (a == 0) a = 255;

            // Тень
            g.setColor(new Color(0, 0, 0, 150));
            g.drawString(text, 3, fm.getAscent() + 2);
            // Текст
            g.setColor(new Color(r, gr, b, a));
            g.drawString(text, 2, fm.getAscent() + 1);
            g.dispose();

            // Создаём NativeImage и загружаем как текстуру
            net.minecraft.client.texture.NativeImage ni =
                    new net.minecraft.client.texture.NativeImage(w, h, false);
            for (int px = 0; px < w; px++) {
                for (int py = 0; py < h; py++) {
                    int argb = img.getRGB(px, py);
                    // NativeImage использует ABGR
                    int na = (argb >> 24) & 0xFF;
                    int nr = (argb >> 16) & 0xFF;
                    int ng = (argb >> 8) & 0xFF;
                    int nb = argb & 0xFF;
                    ni.setColor(px, py, (na << 24) | (nb << 16) | (ng << 8) | nr);
                }
            }

            String key = text + "_" + scale + "_" + color;
            net.minecraft.client.texture.NativeImageBackedTexture tex =
                    new net.minecraft.client.texture.NativeImageBackedTexture(ni);
            Identifier texId = Identifier.of("yourcheat", "dyn_font_" +
                    Math.abs(key.hashCode()));
            MinecraftClient.getInstance().getTextureManager().registerTexture(texId, tex);

            ctx.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                texId, (int)x, (int)y, 0, 0, w, h, w, h
            );

        } catch (Exception e) {
            // Fallback
            ctx.drawText(MinecraftClient.getInstance().textRenderer, text,
                    (int)x, (int)y, color, true);
        }
    }
}


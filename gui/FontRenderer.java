package com.yourcheat.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * TTF-рендерер через java.awt — рисует текст в BufferedImage,
 * затем загружает как Minecraft текстуру через NativeImage.
 */
public class FontRenderer {

    public static final FontRenderer INSTANCE = new FontRenderer();
    private Font awtFont;
    private boolean ready = false;

    private FontRenderer() {}

    /** Вызывать после инициализации ResourceManager */
    public void init() {
        if (ready) return;
        try {
            var rm = MinecraftClient.getInstance().getResourceManager();
            Identifier fontId = Identifier.of("yourcheat", "fonts/nexa.ttf");
            try (InputStream is = rm.open(fontId)) {
                awtFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(Font.PLAIN, 16f);
                ready = true;
            }
        } catch (Exception e) {
            System.err.println("[MeowlDLC] Font load failed: " + e.getMessage());
        }
    }

    public boolean isReady() { return ready; }

    public int getWidth(String text, float scale) {
        if (!ready) return MinecraftClient.getInstance().textRenderer.getWidth(text) * (int)scale;
        Font f = awtFont.deriveFont(Font.PLAIN, 16f * scale);
        BufferedImage d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = d.createGraphics();
        g.setFont(f);
        int w = g.getFontMetrics().stringWidth(text);
        g.dispose();
        return w;
    }

    public void drawString(DrawContext ctx, String text, float x, float y, float scale, int color) {
        if (!ready || text == null || text.isEmpty()) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, text,
                    (int)x, (int)y, color, true);
            return;
        }
        try {
            Font scaled = awtFont.deriveFont(Font.PLAIN, 16f * scale);
            BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gd = dummy.createGraphics();
            gd.setFont(scaled);
            FontMetrics fm = gd.getFontMetrics();
            int w = fm.stringWidth(text) + 4;
            int h = fm.getHeight() + 4;
            gd.dispose();

            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(scaled);
            int cr = (color >> 16) & 0xFF, cg = (color >> 8) & 0xFF,
                cb = color & 0xFF, ca = (color >> 24) & 0xFF;
            if (ca == 0) ca = 255;
            g.setColor(new Color(0, 0, 0, 120));
            g.drawString(text, 3, fm.getAscent() + 2);
            g.setColor(new Color(cr, cg, cb, ca));
            g.drawString(text, 2, fm.getAscent() + 1);
            g.dispose();

            // NativeImage через массив пикселей
            net.minecraft.client.texture.NativeImage ni =
                    new net.minecraft.client.texture.NativeImage(w, h, false);
            for (int px = 0; px < w; px++) {
                for (int py = 0; py < h; py++) {
                    int argb = img.getRGB(px, py);
                    int na = (argb >> 24) & 0xFF;
                    int nr = (argb >> 16) & 0xFF;
                    int ng = (argb >> 8) & 0xFF;
                    int nb = argb & 0xFF;
                    // NativeImage в 1.21.4: ABGR формат
                    int abgr = (na << 24) | (nb << 16) | (ng << 8) | nr;
                    ni.setPixelColor(px, py, abgr);
                }
            }

            String key = "dyn_" + Math.abs((text + scale + color).hashCode());
            Identifier texId = Identifier.of("yourcheat", key);
            var texManager = MinecraftClient.getInstance().getTextureManager();
            var existing = texManager.getOrDefault(texId, null);
            if (existing == null) {
                var tex = new net.minecraft.client.texture.NativeImageBackedTexture(ni);
                texManager.registerTexture(texId, tex);
            }

            ctx.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured,
                texId, (int)x, (int)y, 0, 0, w, h, w, h
            );
        } catch (Exception e) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer, text,
                    (int)x, (int)y, color, true);
        }
    }
}


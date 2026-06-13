package com.yourcheat.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class SearchBar {

    private float x, y;
    private final float width = 300f, height = 24f;
    private String text = "";
    private boolean focused = false;

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }

    public void draw(DrawContext ctx, int mx, int my, float pt, float parentAlpha) {
        int rA = (int)(parentAlpha*230), tA = (int)(parentAlpha*255);
        int bg     = new Color(24, 18, 24, rA).getRGB();
        int border = new Color(200, 200, 200, (int)(parentAlpha*40)).getRGB();
        int textC  = new Color(180, 180, 180, tA).getRGB();
        int accent = RenderUtil.withAlpha(ClickGUI.ACCENT_COLOR, focused ? (int)(parentAlpha*180) : (int)(parentAlpha*40));

        RenderUtil.drawRoundedRect(ctx, x, y, width, height, 6f, bg);
        RenderUtil.drawRoundedRect(ctx, x, y, width, height, 6f, accent, 1f);

        String display = text.isEmpty() && !focused ? "Search modules..." : text + (focused ? "|" : "");
        ctx.drawText(MinecraftClient.getInstance().textRenderer, display,
                (int)(x+10), (int)(y+height/2f-4), textC, false);
    }

    public boolean mouseClicked(float mx, float my, int btn) {
        if (mx >= x && mx <= x+width && my >= y && my <= y+height) { focused = true; return true; }
        focused = false; return false;
    }

    public boolean mouseReleased(float mx, float my, int btn) { return false; }

    public boolean keyPressed(int key, int scan, int mods) {
        if (!focused) return false;
        if (key == 256 || key == 257) { focused = false; return true; }
        if (key == 259) { if (!text.isEmpty()) text = text.substring(0, text.length()-1); return true; }
        return false;
    }

    public boolean charTyped(char c, int mods) {
        if (!focused || Character.isISOControl(c)) return false;
        text += c; return true;
    }

    public String getText() { return text; }
}


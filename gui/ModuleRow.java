package com.yourcheat.gui;

import com.yourcheat.modules.IModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.List;

public class ModuleRow {

    private final IModule module;
    private float x, y, width;
    private final float baseH = 18f;
    private boolean expanded = false;
    private boolean binding = false;
    private float animH = 18f;

    public ModuleRow(IModule module) {
        this.module = module;
    }

    public void draw(DrawContext ctx, int mx, int my, float pt, float parentAlpha) {
        float targetH = getTargetHeight();
        animH += (targetH - animH) * 0.1f * (1f - pt);
        if (Math.abs(targetH - animH) < 0.1f) animH = targetH;

        int rectA   = (int)(parentAlpha * 170);
        int borderA = (int)(parentAlpha * 45);
        int textA   = (int)(parentAlpha * 255);
        float settingsAlpha = targetH == baseH ? 0f :
                parentAlpha * Math.min(1f, (animH - baseH) / Math.max(1f, targetH - baseH));

        int bg = module.isEnabled()
                ? new Color(60, 36, 60, rectA).getRGB()
                : new Color(28, 22, 28, rectA).getRGB();
        int border  = new Color(255, 255, 255, borderA).getRGB();
        int textCol = new Color(220, 220, 220, textA).getRGB();
        int dotsCol = new Color(180, 180, 180, textA).getRGB();

        // фон модуля
        RenderUtil.Blur.drawBlur(ctx, x+6, y, width-12, animH, 4f, 10, -1);
        RenderUtil.drawRoundedRectOutline(ctx, x+6, y, width-12, animH, 4f, 0.1f, border, 1);
        RenderUtil.drawRoundedRect(ctx, x+6, y, width-12, animH, 4f, bg);

        // акцентная линия слева если включён
        if (module.isEnabled()) {
            RenderUtil.drawRoundedRect(ctx, x+6, y, 2, animH, 1f,
                    RenderUtil.withAlpha(ClickGUI.ACCENT_COLOR, textA));
        }

        // название
        ctx.drawText(MinecraftClient.getInstance().textRenderer,
                module.getName(), (int)(x+12), (int)(y+4), textCol, false);

        // точки (если есть настройки)
        List<ClickGUI.Setting> settings = module.getSettings();
        if (!settings.isEmpty()) {
            ctx.drawText(MinecraftClient.getInstance().textRenderer,
                    expanded ? "▴" : "▾", (int)(x+width-22), (int)(y+4), dotsCol, false);
        }

        // настройки
        if (animH > baseH + 1 && !settings.isEmpty()) {
            int finalSA = (int)(settingsAlpha * 255);
            float sy = y + baseH - 2;
            for (ClickGUI.Setting s : settings) {
                // фон строки настройки
                ctx.fill((int)(x+8), (int)sy, (int)(x+width-8), (int)(sy+s.getHeight()),
                        new Color(22, 18, 24, (int)(parentAlpha*120)).getRGB());
                s.render(ctx, mx, my, (int)(x+8), (int)sy);
                sy += s.getHeight();
            }
        }

        // binding overlay
        if (binding) {
            ctx.fill(0, 0,
                    MinecraftClient.getInstance().getWindow().getScaledWidth(),
                    MinecraftClient.getInstance().getWindow().getScaledHeight(),
                    new Color(0, 0, 0, (int)(parentAlpha * 120)).getRGB());
            String txt = "Press any key to bind...";
            var font = MinecraftClient.getInstance().textRenderer;
            int tw = font.getWidth(txt);
            ctx.drawText(font, txt,
                    (MinecraftClient.getInstance().getWindow().getScaledWidth() - tw) / 2,
                    (MinecraftClient.getInstance().getWindow().getScaledHeight() - 9) / 2,
                    0xFFFFFFFF, false);
        }
    }

    private float getTargetHeight() {
        if (!expanded) return baseH;
        float h = baseH;
        for (ClickGUI.Setting s : module.getSettings()) h += s.getHeight();
        return h;
    }

    public float getHeight() { return animH; }

    public boolean mouseClicked(float mx, float my, int btn) {
        if (binding) return true;
        boolean hovered = mx >= x && mx <= x+width && my >= y && my <= y+baseH;
        if (hovered) {
            if (btn == 0) { module.toggle(); return true; }
            if (btn == 1) { if (!module.getSettings().isEmpty()) expanded = !expanded; return true; }
            if (btn == 2) { binding = true; return true; }
        }
        if (expanded) {
            float sy = y + baseH - 2;
            for (ClickGUI.Setting s : module.getSettings()) {
                if (s.mouseClicked(mx, my, btn, (int)(x+8), (int)sy)) return true;
                sy += s.getHeight();
            }
        }
        return false;
    }

    public void mouseDragged(double mx, double my, int btn) {
        if (expanded) {
            float sy = y + baseH - 2;
            for (ClickGUI.Setting s : module.getSettings()) {
                s.mouseDragged(mx, my, (int)(x+8), (int)sy);
                sy += s.getHeight();
            }
        }
    }

    public void mouseReleased(float mx, float my, int btn) {
        for (ClickGUI.Setting s : module.getSettings()) s.mouseReleased();
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (binding) { module.setBind(key); binding = false; return true; }
        return false;
    }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setWidth(float w) { this.width = w; }
    public IModule getModule() { return module; }
}


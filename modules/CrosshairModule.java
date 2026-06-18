package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.LivingEntity;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CrosshairModule implements IModule {

    private boolean enabled = false;

    public float length    = 4f;
    public float thickness = 2f;
    public float gap       = 2f;
    public boolean outline = true;
    public boolean dot     = false;
    public boolean colorOnHover = true;
    public boolean useClientColor = false;
    public Color customColor = new Color(255, 255, 255, 255);

    @Override public String getName()           { return "Crosshair"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Length",    length,    1f, 15f, v -> length    = v));
        list.add(new ClickGUI.SliderSetting("Thickness", thickness, 1f, 10f, v -> thickness = v));
        list.add(new ClickGUI.SliderSetting("Gap",       gap,       0f, 10f, v -> gap       = v));
        list.add(new ClickGUI.BoolSetting("Outline",          outline,        v -> outline        = v));
        list.add(new ClickGUI.BoolSetting("Center dot",       dot,            v -> dot            = v));
        list.add(new ClickGUI.BoolSetting("Red on hover",     colorOnHover,   v -> colorOnHover   = v));
        list.add(new ClickGUI.BoolSetting("Use client color", useClientColor, v -> useClientColor = v));
        list.add(new ClickGUI.ColorSetting("Custom color", customColor, c -> customColor = c));
        return list;
    }

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext ctx, RenderTickCounter counter) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        // Не рисуем если открыт инвентарь/чат или режим не первого лица
        if (mc.options.getPerspective() != Perspective.FIRST_PERSON) return;
        if (mc.currentScreen != null) return;

        int cx = ctx.getScaledWindowWidth()  / 2;
        int cy = ctx.getScaledWindowHeight() / 2;

        int color;
        if (colorOnHover && mc.targetedEntity instanceof LivingEntity) {
            color = new Color(255, 64, 64, 255).getRGB();
        } else if (useClientColor) {
            color = com.yourcheat.gui.ClickGUI.ACCENT_COLOR | 0xFF000000;
        } else {
            color = customColor.getRGB();
        }

        float l = length, t = thickness, g = gap;

        if (outline) {
            int outlineColor = 0xCC000000;
            drawCross(ctx, cx, cy, l + 1, t + 2, g - 1, outlineColor);
        }
        drawCross(ctx, cx, cy, l, t, g, color);

        if (dot) {
            int dotSize = (int) Math.max(1, thickness - 1);
            ctx.fill(cx - dotSize/2, cy - dotSize/2, cx + dotSize/2 + 1, cy + dotSize/2 + 1, color);
        }
    }

    private void drawCross(DrawContext ctx, int cx, int cy, float len, float thick, float gap, int color) {
        int halfT = (int) Math.max(1, thick / 2);
        // Верх
        ctx.fill(cx - halfT, (int)(cy - gap - len), cx + halfT, (int)(cy - gap), color);
        // Низ
        ctx.fill(cx - halfT, (int)(cy + gap), cx + halfT, (int)(cy + gap + len), color);
        // Лево
        ctx.fill((int)(cx - gap - len), cy - halfT, (int)(cx - gap), cy + halfT, color);
        // Право
        ctx.fill((int)(cx + gap), cy - halfT, (int)(cx + gap + len), cy + halfT, color);
    }
}


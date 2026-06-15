package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import com.yourcheat.gui.RenderUtil;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WatermarkModule implements IModule {

    private boolean enabled = true;

    @Override public String getName()           { return "Watermark"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }
    @Override public List<ClickGUI.Setting> getSettings() { return new ArrayList<>(); }

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext ctx, RenderTickCounter counter) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        var font = mc.textRenderer;
        int accentColor = com.yourcheat.gui.ClickGUI.ACCENT_COLOR;

        // Анимированный цвет названия
        float hue = (System.currentTimeMillis() % 4000L) / 4000f;
        // Ограничиваем в фиолетовом диапазоне
        hue = 0.72f + (float)(Math.sin(System.currentTimeMillis() / 1500.0) * 0.05f);
        int titleColor = Color.getHSBColor(hue, 0.7f, 0.95f).getRGB();

        // Основная ватермарка
        String name  = "MeowlDLC";
        String sep   = " | ";
        String fps   = mc.getCurrentFps() + " fps";
        String ping  = "";
        var handler = mc.getNetworkHandler();
        if (handler != null) {
            var entry = handler.getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency() + "ms";
        }

        int nameW = font.getWidth(name);
        int sepW  = font.getWidth(sep);
        int fpsW  = font.getWidth(fps);
        int pingW = font.getWidth(ping);

        int totalW = nameW + sepW + fpsW + (ping.isEmpty() ? 0 : sepW + pingW) + 16;
        int h = 14;

        // Фон
        RenderUtil.drawRoundedRect(ctx, 4, 4, totalW, h, 4f,
                new Color(16, 12, 18, 200).getRGB());
        // Акцент-полоска
        RenderUtil.drawRoundedRect(ctx, 4, 4, 2, h, 1f, accentColor | 0xFF000000);

        // Текст
        int tx = 10, ty = 7;
        ctx.drawText(font, name, tx, ty, titleColor, false);
        tx += nameW;
        ctx.drawText(font, sep, tx, ty, new Color(80, 80, 90, 220).getRGB(), false);
        tx += sepW;
        ctx.drawText(font, fps, tx, ty, new Color(200, 200, 200, 220).getRGB(), false);
        if (!ping.isEmpty()) {
            tx += fpsW;
            ctx.drawText(font, sep, tx, ty, new Color(80, 80, 90, 220).getRGB(), false);
            tx += sepW;
            // Цвет пинга
            int p = 0;
            try { p = Integer.parseInt(ping.replace("ms","")); } catch (Exception ignored) {}
            int pingColor = p < 80 ? 0xFF55FF55 : p < 150 ? 0xFFFFAA00 : 0xFFFF5555;
            ctx.drawText(font, ping, tx, ty, pingColor, false);
        }
    }
}


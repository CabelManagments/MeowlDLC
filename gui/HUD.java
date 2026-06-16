package com.yourcheat.gui;

import com.yourcheat.CheatMod;
import com.yourcheat.modules.IModule;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class HUD {

    private static final HUD INSTANCE = new HUD();
    public static HUD getInstance() { return INSTANCE; }

    // Плавные значения
    private float fps, x, y, z, speed, ping;
    private double lastX, lastZ;
    private long lastTime = System.currentTimeMillis();

    public void register() { HudRenderCallback.EVENT.register(this::render); }

    private void render(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        var font = mc.textRenderer;
        int accent = ClickGUI.ACCENT_COLOR;

        // Обновляем значения
        long now = System.currentTimeMillis();
        double dt = (now - lastTime) / 1000.0;
        if (dt > 0) {
            double dx = mc.player.getX() - lastX;
            double dz = mc.player.getZ() - lastZ;
            float rawSpeed = (float)(Math.sqrt(dx*dx+dz*dz)/dt);
            speed += (rawSpeed - speed) * 0.15f;
        }
        lastX = mc.player.getX(); lastZ = mc.player.getZ(); lastTime = now;
        fps   += (mc.getCurrentFps() - fps) * 0.1f;
        x     += (float)(mc.player.getX() - x) * 0.2f;
        y     += (float)(mc.player.getY() - y) * 0.2f;
        z     += (float)(mc.player.getZ() - z) * 0.2f;
        var nh = mc.getNetworkHandler();
        if (nh != null) {
            var e = nh.getPlayerListEntry(mc.player.getUuid());
            if (e != null) ping += (e.getLatency() - ping) * 0.05f;
        }

        // ── Нижний левый: XYZ + Speed ─────────────────────────────
        int blockW = 160, blockH = 32, pad = 5;
        int bx = pad, by = sh - blockH - pad;

        RenderUtil.drawRoundedRect(ctx, bx, by, blockW, blockH, 4f,
                new Color(12, 10, 15, 200).getRGB());
        // Акцент-линия слева
        RenderUtil.drawRoundedRect(ctx, bx, by, 2, blockH, 1f,
                RenderUtil.withAlpha(accent, 220));

        // XYZ
        ctx.drawText(font, "xyz", bx+6, by+5, RenderUtil.withAlpha(accent, 220), false);
        String xyzVal = String.format("  %.1f  %.1f  %.1f", (double)x, (double)y, (double)z);
        ctx.drawText(font, xyzVal, bx+6, by+5, 0xFFDDDDDD, false);

        // Speed
        String spTxt = String.format("%.2f b/s", speed);
        ctx.drawText(font, spTxt, bx+6, by+18, new Color(160,160,170,220).getRGB(), false);

        // ── Нижний правый: Ping ────────────────────────────────────
        int p = (int)ping;
        int pingColor = p < 80 ? 0xFF55FF55 : p < 150 ? 0xFFFFAA00 : 0xFFFF5555;
        String pingStr = "ping  " + p;
        int pw = font.getWidth(pingStr);
        RenderUtil.drawRoundedRect(ctx, sw-pw-16, sh-18, pw+12, 14, 4f,
                new Color(12,10,15,200).getRGB());
        ctx.drawText(font, "ping  ", sw-pw-10, sh-15, new Color(130,130,140,220).getRGB(), false);
        ctx.drawText(font, String.valueOf(p), sw-pw-10+font.getWidth("ping  "), sh-15, pingColor, false);

        // ── ArrayList: включённые модули ───────────────────────────
        IModule[] mods = {
            CheatMod.jumpCircle, CheatMod.targetESP, CheatMod.hitParticles,
            CheatMod.chinaHat, CheatMod.cape, CheatMod.wings, CheatMod.nimb,
            CheatMod.killAura, CheatMod.targetHUD, CheatMod.watermark, CheatMod.timeChanger
        };

        int ay = 4;
        for (IModule m : mods) {
            if (!m.isEnabled()) continue;
            String name = m.getName();
            int mw = font.getWidth(name);
            RenderUtil.drawRoundedRect(ctx, sw-mw-14, ay, mw+10, 13, 3f,
                    new Color(12,10,15,190).getRGB());
            // Акцент справа
            RenderUtil.drawRoundedRect(ctx, sw-4, ay, 3, 13, 1f,
                    RenderUtil.withAlpha(accent, 200));
            ctx.drawText(font, name, sw-mw-9, ay+2, 0xFFDDDDDD, false);
            ay += 15;
        }
    }
}

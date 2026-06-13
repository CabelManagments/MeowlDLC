package com.yourcheat.gui;

import com.yourcheat.CheatMod;
import com.yourcheat.modules.IModule;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class HUD {

    private static final HUD INSTANCE = new HUD();
    public static HUD getInstance() { return INSTANCE; }

    private final AnimatedValue fpsAnim   = new AnimatedValue(0, 0.05f);
    private final AnimatedValue xAnim     = new AnimatedValue(0, 0.08f);
    private final AnimatedValue yAnim     = new AnimatedValue(0, 0.08f);
    private final AnimatedValue zAnim     = new AnimatedValue(0, 0.08f);
    private final AnimatedValue speedAnim = new AnimatedValue(0, 0.05f);
    private final AnimatedValue pingAnim  = new AnimatedValue(0, 0.03f);

    private double lastX, lastZ;
    private long lastTime = System.currentTimeMillis();

    public void register() {
        HudRenderCallback.EVENT.register(this::render);
    }

    private void render(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden) return;

        int scW = ctx.getScaledWindowWidth();
        int scH = ctx.getScaledWindowHeight();
        var font = mc.textRenderer;

        // Скорость
        long now = System.currentTimeMillis();
        double dt = (now - lastTime) / 1000.0;
        if (dt > 0) {
            double dx = mc.player.getX() - lastX;
            double dz = mc.player.getZ() - lastZ;
            speedAnim.setTarget((float)(Math.sqrt(dx*dx+dz*dz)/dt));
        }
        lastX = mc.player.getX(); lastZ = mc.player.getZ(); lastTime = now;

        fpsAnim.setTarget(mc.getCurrentFps());
        xAnim.setTarget((float)mc.player.getX());
        yAnim.setTarget((float)mc.player.getY());
        zAnim.setTarget((float)mc.player.getZ());

        var entry = mc.getNetworkHandler() != null ?
                mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
        pingAnim.setTarget(entry != null ? entry.getLatency() : 0);

        int accent  = ClickGUI.ACCENT_COLOR;
        int panelBg = new Color(14, 12, 16, 160).getRGB();
        int textW   = 0xFFEEEEEE;
        int textG   = 0xFFAAAAAA;

        // Верхний левый: имя + fps + время
        String clientName = "MeowlDLC";
        int nameW = font.getWidth(clientName);
        RenderUtil.drawRoundedRect(ctx, 5, 3, nameW+14, 15, 4f, panelBg);
        RenderUtil.drawRoundedRect(ctx, 5, 3, 3, 15, 2f, RenderUtil.withAlpha(accent, 200));
        ctx.drawText(font, clientName, 12, 6, accent, false);

        String fpsText = fpsAnim.getInt() + " fps";
        int fpsW = font.getWidth(fpsText);
        RenderUtil.drawRoundedRect(ctx, nameW+22, 3, fpsW+12, 15, 4f, panelBg);
        ctx.drawText(font, fpsText, nameW+27, 6, textW, false);

        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        int timeW = font.getWidth(timeText);
        RenderUtil.drawRoundedRect(ctx, nameW+fpsW+37, 3, timeW+12, 15, 4f, panelBg);
        ctx.drawText(font, timeText, nameW+fpsW+42, 6, textG, false);

        // Нижний левый: xyz + скорость
        String xyzLabel = "xyz";
        String xyzVals  = String.format("  %.1f  %.1f  %.1f", xAnim.get(), yAnim.get(), zAnim.get());
        String speedTxt = String.format("%.1f b/s", speedAnim.get());
        int xyzW  = font.getWidth(xyzLabel+xyzVals);
        int spdW  = font.getWidth(speedTxt);
        int botW  = Math.max(xyzW, spdW)+16;

        RenderUtil.drawRoundedRect(ctx, 5, scH-33, botW, 30, 4f, panelBg);
        RenderUtil.drawRoundedRect(ctx, 5, scH-33, 3, 30, 2f, RenderUtil.withAlpha(accent, 200));
        ctx.drawText(font, xyzLabel, 12, scH-30, RenderUtil.withAlpha(accent,220), false);
        ctx.drawText(font, xyzVals, 12+font.getWidth(xyzLabel), scH-30, textW, false);
        ctx.drawText(font, speedTxt, 12, scH-20, textG, false);

        // Нижний правый: пинг
        String pingLabel = "ping  ";
        String pingVal   = String.valueOf(pingAnim.getInt());
        int pingW = font.getWidth(pingLabel+pingVal);
        RenderUtil.drawRoundedRect(ctx, scW-pingW-16, scH-18, pingW+12, 15, 4f, panelBg);
        int p = pingAnim.getInt();
        int pingColor = p<80 ? 0xFF55FF55 : p<150 ? 0xFFFFAA00 : 0xFFFF5555;
        ctx.drawText(font, pingLabel, scW-pingW-11, scH-15, textG, false);
        ctx.drawText(font, pingVal, scW-pingW-11+font.getWidth(pingLabel), scH-15, pingColor, false);

        // ArrayList включённых модулей (правый верх)
        IModule[] modules = {CheatMod.jumpCircle, CheatMod.targetESP, CheatMod.hitParticles};
        int ay = 3;
        for (IModule m : modules) {
            if (!m.isEnabled()) continue;
            String name = m.getName();
            int mw = font.getWidth(name);
            RenderUtil.drawRoundedRect(ctx, scW-mw-16, ay, mw+12, 13, 3f, panelBg);
            RenderUtil.drawRoundedRect(ctx, scW-5, ay, 3, 13, 2f, RenderUtil.withAlpha(accent,200));
            ctx.drawText(font, name, scW-mw-10, ay+2, textW, false);
            ay += 15;
        }
    }

    private static class AnimatedValue {
        private float value, target;
        private final float speed;
        AnimatedValue(float start, float speed) { value=start; target=start; this.speed=speed; }
        void setTarget(float t) { target=t; }
        float get() { value+=(target-value)*speed; return value; }
        int getInt() { return Math.round(get()); }
    }
}

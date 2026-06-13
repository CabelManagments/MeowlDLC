package com.yourcheat.gui;

import com.yourcheat.CheatMod;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * HUD — отображается поверх игры.
 * Регистрируется в CheatMod через HudRenderCallback.
 *
 * Показывает:
 *  - FPS
 *  - XYZ координаты
 *  - Скорость (блоков/сек)
 *  - Пинг
 *  - Время
 */
public class HUD {

    private static final HUD INSTANCE = new HUD();
    public static HUD getInstance() { return INSTANCE; }

    // Анимированные значения (плавное обновление как в оригинале)
    private final AnimatedValue fpsAnim   = new AnimatedValue(0, 0.05f);
    private final AnimatedValue xAnim     = new AnimatedValue(0, 0.08f);
    private final AnimatedValue yAnim     = new AnimatedValue(0, 0.08f);
    private final AnimatedValue zAnim     = new AnimatedValue(0, 0.08f);
    private final AnimatedValue speedAnim = new AnimatedValue(0, 0.05f);
    private final AnimatedValue pingAnim  = new AnimatedValue(0, 0.03f);

    // Последняя позиция для подсчёта скорости
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

        // Подсчёт скорости
        long now = System.currentTimeMillis();
        double dt = (now - lastTime) / 1000.0;
        if (dt > 0) {
            double dx = mc.player.getX() - lastX;
            double dz = mc.player.getZ() - lastZ;
            double speed = Math.sqrt(dx*dx + dz*dz) / dt;
            speedAnim.setTarget((float)(speed));
        }
        lastX = mc.player.getX(); lastZ = mc.player.getZ(); lastTime = now;

        // Обновляем анимированные значения
        fpsAnim.setTarget(mc.getCurrentFps());
        xAnim.setTarget((float) mc.player.getX());
        yAnim.setTarget((float) mc.player.getY());
        zAnim.setTarget((float) mc.player.getZ());

        // Пинг
        var entry = mc.getNetworkHandler() != null ?
                mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) : null;
        pingAnim.setTarget(entry != null ? entry.getLatency() : 0);

        // Цвет акцента из ClickGUI
        int accent = ClickGUI.ACCENT_COLOR;
        int panelBg = new Color(14, 12, 16, 160).getRGB();
        int textWhite = 0xFFEEEEEE;
        int textGray  = 0xFFAAAAAA;

        // ── Верхний левый блок: клиент + fps + время ──────────────
        String clientName = "MeowlDLC";
        int nameW = font.getWidth(clientName);

        // Фон имени клиента
        RenderUtil.drawRoundedRect(ctx, 5, 3, nameW + 14, 15, 4f, panelBg);
        RenderUtil.drawRoundedRect(ctx, 5, 3, 3, 15, 2f, RenderUtil.withAlpha(accent, 200));
        ctx.drawText(font, clientName, 12, 6, accent, false);

        // FPS блок
        String fpsText = fpsAnim.getInt() + " fps";
        int fpsW = font.getWidth(fpsText);
        RenderUtil.drawRoundedRect(ctx, nameW + 22, 3, fpsW + 12, 15, 4f, panelBg);
        ctx.drawText(font, fpsText, nameW + 27, 6, textWhite, false);

        // Время
        String timeText = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        int timeW = font.getWidth(timeText);
        RenderUtil.drawRoundedRect(ctx, nameW + fpsW + 37, 3, timeW + 12, 15, 4f, panelBg);
        ctx.drawText(font, timeText, nameW + fpsW + 42, 6, textGray, false);

        // ── Нижний левый: xyz + скорость ──────────────────────────
        String xyzText  = "xyz " + String.format("%.1f  %.1f  %.1f", xAnim.get(), yAnim.get(), zAnim.get());
        String speedText = String.format("%.1f", speedAnim.get()) + " b/s";

        int xyzW   = font.getWidth(xyzText);
        int speedW = font.getWidth(speedText);
        int bottomW = Math.max(xyzW, speedW) + 16;

        RenderUtil.drawRoundedRect(ctx, 5, scH-33, bottomW, 30, 4f, panelBg);
        RenderUtil.drawRoundedRect(ctx, 5, scH-33, 3, 30, 2f, RenderUtil.withAlpha(accent, 200));

        ctx.drawText(font, "xyz", 12, scH-30, RenderUtil.withAlpha(accent, 220), false);
        ctx.drawText(font, String.format("%.1f  %.1f  %.1f", xAnim.get(), yAnim.get(), zAnim.get()),
                12 + font.getWidth("xyz "), scH-30, textWhite, false);
        ctx.drawText(font, speedText, 12, scH-20, textGray, false);

        // ── Нижний правый: пинг ───────────────────────────────────
        String pingText = "ping  " + pingAnim.getInt();
        int pingW = font.getWidth(pingText);
        RenderUtil.drawRoundedRect(ctx, scW - pingW - 16, scH - 18, pingW + 12, 15, 4f, panelBg);

        // Цвет пинга по значению
        int pingColor;
        int p = pingAnim.getInt();
        if (p < 80)       pingColor = 0xFF55FF55;
        else if (p < 150) pingColor = 0xFFFFAA00;
        else               pingColor = 0xFFFF5555;

        ctx.drawText(font, "ping", scW - pingW - 11, scH-15, textGray, false);
        ctx.drawText(font, "  " + pingAnim.getInt(), scW - pingW - 11 + font.getWidth("ping"), scH-15, pingColor, false);

        // ── Arraytlist включённых модулей (справа сверху) ─────────
        renderArrayList(ctx, scW, accent, textWhite);
    }

    private void renderArrayList(DrawContext ctx, int scW, int accent, int textColor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        var font = mc.textRenderer;
        int panelBg = new Color(14, 12, 16, 140).getRGB();

        int y = 3;
        // Собираем включённые модули
        IModule[] modules = {CheatMod.jumpCircle, CheatMod.targetESP, CheatMod.hitParticles};
        for (var m : modules) {
            if (!m.isEnabled()) continue;
            String name = m.getName();
            int w = font.getWidth(name);
            RenderUtil.drawRoundedRect(ctx, scW - w - 16, y, w + 12, 13, 3f, panelBg);
            RenderUtil.drawRoundedRect(ctx, scW - 5, y, 3, 13, 2f, RenderUtil.withAlpha(accent, 200));
            ctx.drawText(font, name, scW - w - 10, y + 2, textColor, false);
            y += 15;
        }
    }

    // Нужен интерфейс IModule локально
    interface IModule {
        boolean isEnabled();
        String getName();
    }

    // ── Анимированное значение (как в оригинале) ──────────────────
    private static class AnimatedValue {
        private float value, target;
        private final float speed;

        AnimatedValue(float start, float speed) { value = start; target = start; this.speed = speed; }
        void setTarget(float t) { target = t; }
        float get() { value += (target - value) * speed; return value; }
        int getInt() { return Math.round(get()); }
    }
}


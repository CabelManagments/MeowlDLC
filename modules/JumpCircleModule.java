package com.yourcheat.modules;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import com.yourcheat.gui.DrawUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JumpCircle — рисует цветной круг под ногами игрока когда он прыгает.
 *
 * Подпиши класс на Forge EventBus:
 *   MinecraftForge.EVENT_BUS.register(JumpCircleModule.INSTANCE);
 *
 * Рендер происходит в RenderWorldLastEvent (3D world overlay).
 */
public class JumpCircleModule implements IClientModule {

    public static final JumpCircleModule INSTANCE = new JumpCircleModule();
    private JumpCircleModule() {}

    // ── Настройки ──────────────────────────────────────────────────
    private boolean enabled = false;
    public float radius    = 0.8f;   // радиус круга
    public float thickness = 2.0f;   // толщина обводки (имитация; в 3D = кол-во линий)
    public float alpha     = 0.85f;  // прозрачность
    public Color color     = new Color(180, 80, 220, 220); // фиолетовый по умолчанию
    // анимация fade
    private float animAlpha = 0f;
    private boolean wasOnGround = true;

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    // ── Settings для ClickGUI ──────────────────────────────────────
    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Radius",    radius,    0.3f, 2.0f, v -> radius = v));
        list.add(new ClickGUI.SliderSetting("Thickness", thickness, 0.5f, 5.0f, v -> thickness = v));
        list.add(new ClickGUI.SliderSetting("Opacity",   alpha,     0.1f, 1.0f, v -> alpha = v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        return list;
    }

    // ── Рендер ─────────────────────────────────────────────────────
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        boolean onGround = player.isOnGround();

        // Fade in/out
        if (!onGround) {
            animAlpha = Math.min(1f, animAlpha + 0.1f);
        } else {
            animAlpha = Math.max(0f, animAlpha - 0.07f);
        }
        wasOnGround = onGround;
        if (animAlpha < 0.01f) return;

        // Интерполированная позиция
        float pt = event.getPartialTicks();
        double px = player.lastTickPosX + (player.getPosX() - player.lastTickPosX) * pt;
        double py = player.lastTickPosY + (player.getPosY() - player.lastTickPosY) * pt;
        double pz = player.lastTickPosZ + (player.getPosZ() - player.lastTickPosZ) * pt;

        // Отступ камеры
        Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        double ox = px - cam.x;
        double oy = py - cam.y;
        double oz = pz - cam.z;

        MatrixStack ms = event.getMatrixStack();
        ms.push();
        ms.translate(ox, oy, oz);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(Math.max(1f, thickness));

        float r = color.getRed()   / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue()  / 255f;
        float a = (color.getAlpha() / 255f) * alpha * animAlpha;

        // Рисуем окружность в горизонтальной плоскости (Y=0 = пол игрока)
        drawCircle3D(ms, (float)radius, r, g, b, a);

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
        ms.pop();
    }

    /**
     * Рисует горизонтальную окружность через GL_LINE_LOOP.
     * segments = 64 для плавности.
     */
    private void drawCircle3D(MatrixStack ms, float radius, float r, float g, float b, float a) {
        int segments = 64;
        Matrix4f matrix = ms.getLast().getMatrix();

        com.mojang.blaze3d.vertex.BufferBuilder buf =
            com.mojang.blaze3d.vertex.Tessellator.getInstance().getBuffer();

        buf.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float x = (float)(radius * Math.cos(angle));
            float z = (float)(radius * Math.sin(angle));
            buf.pos(matrix, x, 0.05f, z).color(r, g, b, a).endVertex();
        }
        com.mojang.blaze3d.vertex.Tessellator.getInstance().draw();
    }
}


package com.yourcheat.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChinaHatModule implements IModule {

    private boolean enabled = false;
    public Color color   = new Color(255, 215, 0, 200); // золотой
    public float size    = 0.5f;
    public boolean self  = true; // показывать на себе

    @Override public String getName()           { return "ChinaHat"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Size",  size,  0.2f, 1.5f, v -> size  = v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        list.add(new ClickGUI.BoolSetting("Show on self", self, v -> self = v));
        return list;
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        float pt = ctx.tickCounter().getTickDelta(true);
        Vec3d camPos = ctx.camera().getPos();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!self && player == mc.player) continue;
            if (!player.isAlive()) continue;

            double px = player.prevX + (player.getX() - player.prevX) * pt - camPos.x;
            double py = player.prevY + (player.getY() - player.prevY) * pt - camPos.y + player.getHeight() + 0.18;
            double pz = player.prevZ + (player.getZ() - player.prevZ) * pt - camPos.z;

            MatrixStack ms = ctx.matrixStack();
            ms.push();
            ms.translate(px, py, pz);

            // Поворачиваем шляпу вместе с игроком
            float yaw = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw) * pt;
            ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(-yaw)));

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

            int r = color.getRed(), g = color.getGreen(),
                b = color.getBlue(), a = color.getAlpha();

            drawCone(ms, size, r, g, b, a);
            drawBrim(ms, size, r, g, b, a);

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            ms.pop();
        }
    }

    /** Конус — тело шляпы */
    private void drawCone(MatrixStack ms, float s, int r, int g, int b, int a) {
        int segments = 24;
        float height = s * 1.2f;
        float radius = s * 0.5f;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = ms.peek().getPositionMatrix();

        for (int i = 0; i < segments; i++) {
            double a1 = 2 * Math.PI * i / segments;
            double a2 = 2 * Math.PI * (i + 1) / segments;
            float x1 = radius * (float) Math.cos(a1), z1 = radius * (float) Math.sin(a1);
            float x2 = radius * (float) Math.cos(a2), z2 = radius * (float) Math.sin(a2);

            // Верхушка
            buf.vertex(mat, 0, height, 0).color(r, g, b, a);
            buf.vertex(mat, x1, 0, z1).color(r, g, b, (int)(a * 0.8f));
            buf.vertex(mat, x2, 0, z2).color(r, g, b, (int)(a * 0.8f));
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    /** Поля шляпы — тонкий диск */
    private void drawBrim(MatrixStack ms, float s, int r, int g, int b, int a) {
        int segments = 32;
        float innerR = s * 0.48f, outerR = s * 0.95f;
        float thickness = s * 0.04f;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = ms.peek().getPositionMatrix();

        for (int i = 0; i < segments; i++) {
            double a1 = 2 * Math.PI * i / segments;
            double a2 = 2 * Math.PI * (i + 1) / segments;
            float ix1 = innerR * (float) Math.cos(a1), iz1 = innerR * (float) Math.sin(a1);
            float ix2 = innerR * (float) Math.cos(a2), iz2 = innerR * (float) Math.sin(a2);
            float ox1 = outerR * (float) Math.cos(a1), oz1 = outerR * (float) Math.sin(a1);
            float ox2 = outerR * (float) Math.cos(a2), oz2 = outerR * (float) Math.sin(a2);
            int ba = (int)(a * 0.9f);

            // Верхняя грань
            buf.vertex(mat, ix1, thickness/2, iz1).color(r,g,b,ba);
            buf.vertex(mat, ox1, thickness/2, oz1).color(r,g,b,ba);
            buf.vertex(mat, ox2, thickness/2, oz2).color(r,g,b,ba);
            buf.vertex(mat, ix1, thickness/2, iz1).color(r,g,b,ba);
            buf.vertex(mat, ox2, thickness/2, oz2).color(r,g,b,ba);
            buf.vertex(mat, ix2, thickness/2, iz2).color(r,g,b,ba);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }
}


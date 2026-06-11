package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class JumpCircleModule implements IModule {

    private boolean enabled = false;
    public float radius    = 0.8f;
    public float thickness = 2.0f;
    public float alpha     = 0.85f;
    public Color color     = new Color(180, 80, 220);

    private float animAlpha = 0f;

    @Override public String getName() { return "JumpCircle"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Radius",    radius,    0.3f, 2.0f, v -> radius = v));
        list.add(new ClickGUI.SliderSetting("Thickness", thickness, 0.5f, 4.0f, v -> thickness = v));
        list.add(new ClickGUI.SliderSetting("Opacity",   alpha,     0.1f, 1.0f, v -> alpha = v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        return list;
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        if (player == null) return;

        boolean onGround = player.isOnGround();
        animAlpha = onGround
            ? Math.max(0f, animAlpha - 0.07f)
            : Math.min(1f, animAlpha + 0.1f);
        if (animAlpha < 0.01f) return;

        float pt = ctx.tickCounter().getTickDelta(true);
        double cx = ctx.camera().getPos().x;
        double cy = ctx.camera().getPos().y;
        double cz = ctx.camera().getPos().z;

        double px = player.prevX + (player.getX() - player.prevX) * pt - cx;
        double py = player.prevY + (player.getY() - player.prevY) * pt - cy;
        double pz = player.prevZ + (player.getZ() - player.prevZ) * pt - cz;

        MatrixStack ms = ctx.matrixStack();
        ms.push();
        ms.translate(px, py, pz);

        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLines());
        Matrix4f mat = ms.peek().getPositionMatrix();

        float r = color.getRed()   / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue()  / 255f;
        float a = alpha * animAlpha;

        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a1 = 2.0 * Math.PI * i / segments;
            double a2 = 2.0 * Math.PI * (i + 1) / segments;
            float x1 = (float)(radius * Math.cos(a1));
            float z1 = (float)(radius * Math.sin(a1));
            float x2 = (float)(radius * Math.cos(a2));
            float z2 = (float)(radius * Math.sin(a2));
            vc.vertex(mat, x1, 0.05f, z1).color(r, g, b, a).normal(ms.peek().getNormalMatrix(), 0, 1, 0);
            vc.vertex(mat, x2, 0.05f, z2).color(r, g, b, a).normal(ms.peek().getNormalMatrix(), 0, 1, 0);
        }

        vcp.draw(RenderLayer.getLines());
        ms.pop();
    }
}


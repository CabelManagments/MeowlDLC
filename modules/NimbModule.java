package com.yourcheat.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NimbModule implements IModule {

    private boolean enabled = false;

    // Режимы цвета: RAINBOW, PULSE, STATIC
    public int colorMode   = 0; // 0=Rainbow 1=Pulse 2=Static
    public Color color1    = new Color(120, 60, 255, 255);
    public Color color2    = new Color(255, 120, 60, 255);
    public float size      = 0.45f;
    public float thickness = 0.08f;
    public boolean self    = true;

    @Override public String getName()           { return "Nimb"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Size",      size,      0.2f, 1.0f, v -> size      = v));
        list.add(new ClickGUI.SliderSetting("Thickness", thickness, 0.02f,0.2f, v -> thickness = v));
        list.add(new ClickGUI.ColorSetting("Color 1", color1, c -> color1 = c));
        list.add(new ClickGUI.ColorSetting("Color 2", color2, c -> color2 = c));
        list.add(new ClickGUI.BoolSetting("Show on self", self, v -> self = v));
        return list;
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        float pt    = ctx.tickCounter().getTickDelta(true);
        Vec3d cam   = ctx.camera().getPos();
        long  t     = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (!self && player == mc.player) continue;
            if (!player.isAlive()) continue;
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;

            double px = MathHelper.lerp(pt, player.prevX, player.getX()) - cam.x;
            double py = MathHelper.lerp(pt, player.prevY, player.getY()) - cam.y + player.getHeight() + 0.15;
            double pz = MathHelper.lerp(pt, player.prevZ, player.getZ()) - cam.z;

            MatrixStack ms = ctx.matrixStack();
            ms.push();
            ms.translate(px, py, pz);

            // Нимб всегда горизонтальный
            float yaw = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw) * pt;
            ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(-yaw)));

            drawNimb(ms, t, player);

            ms.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawNimb(MatrixStack ms, long t, PlayerEntity player) {
        int segments = 64;
        float outerR = size;
        float innerR = size - thickness;

        Tessellator tess = Tessellator.getInstance();

        // Конус (тело нимба — сходится к центру)
        BufferBuilder cone = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        Matrix4f mat = ms.peek().getPositionMatrix();

        // Центральная точка
        int centerColor = getColor(t, 0, 255);
        addV(cone, mat, 0, 0.15f, 0, centerColor);

        for (int i = 0; i <= segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float x = outerR * (float)Math.cos(angle);
            float z = outerR * (float)Math.sin(angle);
            int c = getColor(t, i, (int)(255 * 0.3f));
            addV(cone, mat, x, 0, z, c);
        }
        BufferRenderer.drawWithGlobalProgram(cone.end());

        // Кольцо
        RenderSystem.lineWidth(2.5f);
        BufferBuilder ring = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float x = outerR * (float)Math.cos(angle);
            float z = outerR * (float)Math.sin(angle);
            int c = getColor(t, i, 255);
            addV(ring, mat, x, 0.05f, z, c);
        }
        BufferRenderer.drawWithGlobalProgram(ring.end());

        // Внутреннее кольцо
        BufferBuilder inner = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(2 * Math.PI * i / segments);
            float x = innerR * (float)Math.cos(angle);
            float z = innerR * (float)Math.sin(angle);
            int c = getColor(t, i, 180);
            addV(inner, mat, x, 0.05f, z, c);
        }
        BufferRenderer.drawWithGlobalProgram(inner.end());
    }

    private int getColor(long t, int i, int alpha) {
        switch (colorMode) {
            case 1: { // Pulse
                float pulse = (float)(Math.sin(t / 500.0) * 0.5 + 0.5);
                return lerpColor(color1.getRGB(), color2.getRGB(), pulse, alpha);
            }
            case 2: // Static
                return (alpha << 24) | (color1.getRGB() & 0x00FFFFFF);
            default: { // Rainbow
                float hue = ((t % 5000L) / 5000f + i / 64f) % 1f;
                Color c = Color.getHSBColor(hue, 0.8f, 0.95f);
                return (alpha << 24) | (c.getRGB() & 0x00FFFFFF);
            }
        }
    }

    private static int lerpColor(int c1, int c2, float t, int alpha) {
        int r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
        int r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
        return (alpha<<24)|((int)(r1+(r2-r1)*t)<<16)|((int)(g1+(g2-g1)*t)<<8)|(int)(b1+(b2-b1)*t);
    }

    private static void addV(BufferBuilder buf, Matrix4f mat, float x, float y, float z, int color) {
        buf.vertex(mat, x, y, z).color((color>>16)&0xFF,(color>>8)&0xFF,color&0xFF,(color>>24)&0xFF);
    }
}


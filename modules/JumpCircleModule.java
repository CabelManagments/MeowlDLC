package com.yourcheat.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JumpCircleModule implements IModule {

    private boolean enabled = false;
    public float maxScale  = 2.0f;
    public float lifetime  = 3000f;
    public Color color     = new Color(255, 255, 255, 220);
    public boolean rainbow = false;

    private boolean wasOnGround = true;
    private final List<Circle> circles = new ArrayList<>();

    @Override public String getName()           { return "JumpCircle"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; if (!v) circles.clear(); }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Max Size", maxScale, 0.5f, 5.0f, v -> maxScale = v));
        list.add(new ClickGUI.SliderSetting("Lifetime", lifetime, 500f, 5000f, v -> lifetime = v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        list.add(new ClickGUI.BoolSetting("Rainbow", rainbow, v -> rainbow = v));
        return list;
    }

    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        boolean onGround = mc.player.isOnGround();
        if (wasOnGround && !onGround) {
            circles.add(new Circle(
                new Vec3d(mc.player.getX(), mc.player.getY() + 0.02, mc.player.getZ()),
                System.currentTimeMillis()
            ));
        }
        wasOnGround = onGround;
        circles.removeIf(c -> System.currentTimeMillis() - c.spawnTime > (long) lifetime);
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled || circles.isEmpty()) return;

        Vec3d camPos = ctx.camera().getPos();
        MatrixStack ms = ctx.matrixStack();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        for (Circle circle : circles) {
            float progress = (System.currentTimeMillis() - circle.spawnTime) / lifetime;
            if (progress >= 1f) continue;

            float scale = progress * maxScale;
            float alpha = 1f - (progress * progress);

            int baseColor = rainbow
                    ? Color.HSBtoRGB(progress, 0.8f, 0.9f)
                    : color.getRGB();

            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8)  & 0xFF;
            int b = baseColor & 0xFF;
            int a = (int)(alpha * (color.getAlpha() / 255f) * 255);

            double dx = circle.pos.x - camPos.x;
            double dy = circle.pos.y - camPos.y;
            double dz = circle.pos.z - camPos.z;

            ms.push();
            ms.translate(dx, dy, dz);

            Matrix4f mat = ms.peek().getPositionMatrix();

            // Рисуем горизонтальный круг через GL_LINE_LOOP (64 сегмента)
            int segments = 64;
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
                    VertexFormats.POSITION_COLOR);

            for (int i = 0; i <= segments; i++) {
                double angle = 2.0 * Math.PI * i / segments;
                float x = scale / 2f * (float) Math.cos(angle);
                float z = scale / 2f * (float) Math.sin(angle);
                buf.vertex(mat, x, 0f, z).color(r, g, b, a);
            }

            RenderSystem.lineWidth(2.5f);
            BufferRenderer.drawWithGlobalProgram(buf.end());

            // Внутреннее кольцо (свечение)
            BufferBuilder glow = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
                    VertexFormats.POSITION_COLOR);
            float innerScale = scale / 2f * 0.88f;
            int glowA = (int)(a * 0.4f);
            for (int i = 0; i <= segments; i++) {
                double angle = 2.0 * Math.PI * i / segments;
                float x = innerScale * (float) Math.cos(angle);
                float z = innerScale * (float) Math.sin(angle);
                glow.vertex(mat, x, 0f, z).color(r, g, b, glowA);
            }
            RenderSystem.lineWidth(1.2f);
            BufferRenderer.drawWithGlobalProgram(glow.end());

            ms.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    private record Circle(Vec3d pos, long spawnTime) {}
}

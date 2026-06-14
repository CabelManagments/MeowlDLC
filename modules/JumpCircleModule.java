package com.yourcheat.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class JumpCircleModule implements IModule {

    private boolean enabled = false;

    public float maxScale = 2.0f;
    public float lifetime = 3000f;
    public Color color    = new Color(160, 80, 255, 255);
    public boolean rainbow = true;

    private boolean wasOnGround = true;
    private final List<Circle> circles = new ArrayList<>();

    private static final Identifier CIRCLE_TEXTURE =
            Identifier.of("yourcheat", "textures/circle.png");

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
                new Vec3d(mc.player.getX(),
                          Math.floor(mc.player.getY()) + 0.001,
                          mc.player.getZ()),
                System.currentTimeMillis()
            ));
        }
        wasOnGround = onGround;
        circles.removeIf(c -> System.currentTimeMillis() - c.spawnTime > (long) lifetime);
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled || circles.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d camPos  = camera.getPos();

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        // Используем RenderLayer с текстурой
        RenderLayer layer = RenderLayer.getEntityTranslucent(CIRCLE_TEXTURE);
        VertexConsumer vc = vcp.getBuffer(layer);

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
            int a = (int)(alpha * 255);

            double dx = circle.pos.x - camPos.x;
            double dy = circle.pos.y - camPos.y;
            double dz = circle.pos.z - camPos.z;

            ms.push();
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X
                    .rotationDegrees(camera.getPitch()));
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y
                    .rotationDegrees(camera.getYaw() + 180f));
            ms.translate(dx, dy, dz);
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y
                    .rotationDegrees(-camera.getYaw()));
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X
                    .rotationDegrees(90f));

            Matrix4f mat = ms.peek().getPositionMatrix();
            var nm       = ms.peek().getNormalMatrix();
            float h = scale / 2f;

            vc.vertex(mat, -h, -h, 0).color(r,g,b,a).texture(0f,0f).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(nm, 0,1,0);
            vc.vertex(mat, -h,  h, 0).color(r,g,b,a).texture(0f,1f).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(nm, 0,1,0);
            vc.vertex(mat,  h,  h, 0).color(r,g,b,a).texture(1f,1f).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(nm, 0,1,0);
            vc.vertex(mat,  h, -h, 0).color(r,g,b,a).texture(1f,0f).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(nm, 0,1,0);

            ms.pop();
        }

        vcp.draw(layer);
    }

    private record Circle(Vec3d pos, long spawnTime) {}
}

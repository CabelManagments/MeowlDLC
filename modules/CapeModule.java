package com.yourcheat.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CapeModule implements IModule {

    private boolean enabled = false;
    public Color color   = new Color(120, 40, 200, 220);
    public float size    = 1.0f;
    public boolean self  = true;

    // Текстура плаща — кладём в resources/assets/yourcheat/textures/cape.png
    // Если нет — рисуем цветной плащ
    private static final Identifier CAPE_TEXTURE =
            Identifier.of("yourcheat", "textures/cape.png");

    @Override public String getName()           { return "Cape"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Size",  size,  0.5f, 2.0f, v -> size  = v));
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
            double py = player.prevY + (player.getY() - player.prevY) * pt - camPos.y;
            double pz = player.prevZ + (player.getZ() - player.prevZ) * pt - camPos.z;

            MatrixStack ms = ctx.matrixStack();
            ms.push();
            ms.translate(px, py, pz);

            float yaw = player.prevBodyYaw + (player.bodyYaw - player.prevBodyYaw) * pt;
            ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(-yaw + 180f)));

            // Плащ позади игрока (смещение назад на 0.3)
            ms.translate(0, player.getHeight() - 0.1f, 0.3f);

            // Волнение плаща
            float wave = (float)(Math.sin(System.currentTimeMillis() / 600.0 + player.getId()) * 0.1f);
            ms.multiply(new Quaternionf().rotationX(wave));

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();

            MatrixStack.Entry entry = ms.peek();
            Matrix4f mat = entry.getPositionMatrix();

            float w = 0.5f * size, h = 0.8f * size;
            int r = color.getRed(), g = color.getGreen(),
                b = color.getBlue(), a = color.getAlpha();

            // Пробуем нарисовать с текстурой, иначе без
            try {
                VertexConsumerProvider.Immediate vcp =
                        mc.getBufferBuilders().getEntityVertexConsumers();
                VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(CAPE_TEXTURE));

                vc.vertex(mat, -w, 0, 0).color(r,g,b,a).texture(0,0)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                  .normal(entry, 0,0,-1);
                vc.vertex(mat,  w, 0, 0).color(r,g,b,a).texture(1,0)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                  .normal(entry, 0,0,-1);
                vc.vertex(mat,  w,-h, 0).color(r,g,b,a).texture(1,1)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                  .normal(entry, 0,0,-1);
                vc.vertex(mat, -w,-h, 0).color(r,g,b,a).texture(0,1)
                  .overlay(OverlayTexture.DEFAULT_UV)
                  .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                  .normal(entry, 0,0,-1);

                vcp.draw(RenderLayer.getEntityTranslucent(CAPE_TEXTURE));
            } catch (Exception e) {
                // Fallback без текстуры
                RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                buf.vertex(mat,-w, 0,0).color(r,g,b,a);
                buf.vertex(mat, w, 0,0).color(r,g,b,a);
                buf.vertex(mat, w,-h,0).color(r,g,b,a);
                buf.vertex(mat,-w,-h,0).color(r,g,b,a);
                BufferRenderer.drawWithGlobalProgram(buf.end());
            }

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            ms.pop();
        }
    }
}


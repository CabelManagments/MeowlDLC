package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class JumpCircleModule implements IModule {

    private boolean enabled = false;

    // Настройки
    public float maxScale  = 2.0f;   // максимальный размер круга
    public float lifetime  = 3000f;  // мс до исчезновения
    public Color color     = new Color(160, 80, 255, 255);
    public boolean rainbow = true;   // радужный цвет

    private boolean wasOnGround = true;
    private final List<Circle> circles = new ArrayList<>();

    // Текстура — лежит в resources/assets/yourcheat/textures/circle.png
    private static final Identifier CIRCLE_TEXTURE =
            Identifier.of("yourcheat", "textures/circle.png");

    @Override public String getName()        { return "JumpCircle"; }
    @Override public boolean isEnabled()     { return enabled; }
    @Override public void setEnabled(boolean v) {
        enabled = v;
        if (!v) circles.clear();
    }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Max Size",  maxScale, 0.5f, 5.0f, v -> maxScale  = v));
        list.add(new ClickGUI.SliderSetting("Lifetime",  lifetime, 500f, 5000f, v -> lifetime = v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        list.add(new ClickGUI.BoolSetting("Rainbow", rainbow, v -> rainbow = v));
        return list;
    }

    /**
     * Вызывается каждый тик из ClientTickEvents в CheatMod.
     * Определяем момент отрыва от земли и спавним круг.
     */
    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        boolean onGround = mc.player.isOnGround();

        // Момент прыжка: был на земле → стал в воздухе
        if (wasOnGround && !onGround) {
            Vec3d pos = new Vec3d(
                    mc.player.getX(),
                    Math.floor(mc.player.getY()) + 0.001,
                    mc.player.getZ()
            );
            circles.add(new Circle(pos, System.currentTimeMillis()));
        }

        wasOnGround = onGround;

        // Удаляем старые
        circles.removeIf(c -> System.currentTimeMillis() - c.spawnTime > (long) lifetime);
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled || circles.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d camPos  = camera.getPos();

        for (Circle circle : circles) {
            float progress = (System.currentTimeMillis() - circle.spawnTime) / lifetime;
            if (progress >= 1f) continue;

            float scale  = progress * maxScale;
            float alpha  = 1f - (progress * progress); // квадратичный фейд

            // Цвет
            int baseColor;
            if (rainbow) {
                baseColor = Color.HSBtoRGB(progress, 0.8f, 0.9f);
            } else {
                baseColor = color.getRGB();
            }
            // Применяем alpha
            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8)  & 0xFF;
            int b = baseColor & 0xFF;
            int a = (int)(alpha * 255);

            Vec3d pos = circle.pos;
            double dx = pos.x - camPos.x;
            double dy = pos.y - camPos.y;
            double dz = pos.z - camPos.z;

            MatrixStack ms = ctx.matrixStack();
            ms.push();

            // Поворачиваем к камере, потом горизонтально
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X
                    .rotationDegrees(camera.getPitch()));
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y
                    .rotationDegrees(camera.getYaw() + 180f));

            ms.translate(dx, dy, dz);

            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y
                    .rotationDegrees(-camera.getYaw()));
            ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X
                    .rotationDegrees(90f));

            // Рисуем текстурированный квад
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            var tessellator = Tessellator.getInstance();
            var buf = tessellator.begin(VertexFormat.DrawMode.QUADS,
                    net.minecraft.client.render.VertexFormats.POSITION_COLOR_TEXTURE);

            Matrix4f mat = ms.peek().getPositionMatrix();
            float h = scale / 2f;

            buf.vertex(mat, -h, -h, 0).color(r, g, b, a).texture(0f, 0f);
            buf.vertex(mat, -h,  h, 0).color(r, g, b, a).texture(0f, 1f);
            buf.vertex(mat,  h,  h, 0).color(r, g, b, a).texture(1f, 1f);
            buf.vertex(mat,  h, -h, 0).color(r, g, b, a).texture(1f, 0f);

            RenderSystem.setShaderTexture(0, CIRCLE_TEXTURE);
            RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
            net.minecraft.client.gl.GlUniform.uniform1i(
                    net.minecraft.client.gl.GlUniform.getUniformLocation(
                            RenderSystem.getShader().glRef, "Sampler0"), 0);

            try {
                net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());
            } catch (Exception ignored) {}

            RenderSystem.disableBlend();
            ms.pop();
        }
    }

    private record Circle(Vec3d pos, long spawnTime) {}
}

package com.yourcheat.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TargetESP — орбитальные кубики вокруг таргета.
 * Порт TARGETESP.java (Zenith) без внешних зависимостей.
 */
public class TargetESPModule implements IModule {

    private boolean enabled = false;

    // Настройки
    public float cubeSize      = 0.12f;
    public float cubeCount     = 20f;
    public float rotationSpeed = 4.0f;
    public float orbitSpeed    = 1.0f;
    public boolean redOnHit    = true;
    public boolean rainbow     = false;
    public Color color         = new Color(100, 60, 220, 255);

    // Runtime
    private float rotationY    = 0f;
    private float rotationX    = 0f;
    private float animValue    = 0f;   // 0..1 анимация появления
    private LivingEntity lastTarget = null;

    @Override public String getName()           { return "TargetESP"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; if (!v) { lastTarget = null; animValue = 0f; } }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Cube Size",       cubeSize,      0.05f, 0.5f,  v -> cubeSize      = v));
        list.add(new ClickGUI.SliderSetting("Cube Count",      cubeCount,     8,     30,    v -> cubeCount     = v));
        list.add(new ClickGUI.SliderSetting("Rotation Speed",  rotationSpeed, 0.5f,  15f,   v -> rotationSpeed = v));
        list.add(new ClickGUI.SliderSetting("Orbit Speed",     orbitSpeed,    0.2f,  3f,    v -> orbitSpeed    = v));
        list.add(new ClickGUI.BoolSetting("Red on hit",        redOnHit,      v -> redOnHit = v));
        list.add(new ClickGUI.BoolSetting("Rainbow",           rainbow,       v -> rainbow  = v));
        list.add(new ClickGUI.ColorSetting("Color",            color,         c -> color    = c));
        return list;
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Ищем ближайшего игрока под прицелом
        LivingEntity target = null;
        if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) {
            target = le;
        }

        // Анимация появления/исчезновения (SINE_OUT)
        float animSpeed = 0.06f;
        if (target != null) {
            lastTarget = target;
            animValue = Math.min(1f, animValue + animSpeed);
        } else {
            animValue = Math.max(0f, animValue - animSpeed);
            if (animValue == 0f) { lastTarget = null; return; }
        }
        if (lastTarget == null) return;

        // Обновляем углы вращения
        rotationY += rotationSpeed;
        rotationX += rotationSpeed * 0.375f;

        renderCubes(ctx, lastTarget, animValue);
    }

    private void renderCubes(WorldRenderContext ctx, LivingEntity target, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float pt = ctx.tickCounter().getTickDelta(true);

        Vec3d targetPos = target.getLerpedPos(pt);
        Vec3d camPos    = ctx.camera().getPos();
        Vec3d renderPos = targetPos.subtract(camPos);

        MatrixStack ms = ctx.matrixStack();
        ms.push();
        ms.translate(renderPos.x, renderPos.y, renderPos.z);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 1, 0);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        int count       = (int) cubeCount;
        float size      = cubeSize;
        float time      = (mc.player.age + pt) * 0.15f;
        float entityH   = target.getHeight();
        float entityW   = target.getWidth();
        float halfW     = entityW * 0.5f;

        // Цвет с учётом redOnHit и rainbow
        int colorInt = getColor(target, alpha);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (int i = 0; i < count; i++) {
            float seed1 = (float) Math.sin(i * 1.7f + 0.3f) * 0.5f + 0.5f;
            float seed2 = (float) Math.cos(i * 2.3f + 0.7f) * 0.5f + 0.5f;
            float seed3 = (float) Math.sin(i * 3.1f + 1.1f) * 0.5f + 0.5f;

            float angleOffset = i * (360f / count) + seed1 * 12f;
            float angle       = (time + mc.player.age) * orbitSpeed + angleOffset;
            float orbitR      = halfW + 0.25f + seed3 * 0.15f;

            float x       = orbitR * (float) Math.cos(Math.toRadians(angle));
            float z       = orbitR * (float) Math.sin(Math.toRadians(angle));
            float bobbing = (float) Math.sin(time * 0.05f + i * 0.3f) * 0.1f;
            float y       = seed2 * entityH * 1.05f + bobbing;

            addCubeVertices(buf, ms, x, y, z, size, colorInt, alpha * 0.5f, rotationY, rotationX);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        ms.pop();
    }

    private int getColor(LivingEntity target, float alpha) {
        Color base;
        if (rainbow) {
            float hue = (System.currentTimeMillis() % 5000L) / 5000f;
            base = Color.getHSBColor(hue, 0.8f, 0.9f);
        } else {
            base = color;
        }

        // redOnHit — плавно краснеет при получении урона
        if (redOnHit && target.hurtTime > 0) {
            float hp = target.hurtTime / 10f;
            int r = (int)(255 * hp + base.getRed()   * (1 - hp));
            int g = (int)(50  * hp + base.getGreen() * (1 - hp));
            int b = (int)(50  * hp + base.getBlue()  * (1 - hp));
            return new Color(r, g, b, 255).getRGB();
        }

        return base.getRGB();
    }

    private void addCubeVertices(BufferBuilder buf, MatrixStack ms,
                                 float x, float y, float z, float size,
                                 int color, float alpha, float rotY, float rotX) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(rotY)));
        ms.multiply(new Quaternionf().rotationX((float) Math.toRadians(rotX)));

        Matrix4f matrix = ms.peek().getPositionMatrix();

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b = color & 0xFF;
        int a = (int)(255 * alpha);

        float s = size / 2f;

        // Передняя грань
        addQuad(buf, matrix, -s,-s, s,  s,-s, s,  s, s, s, -s, s, s,  r, g, b, a);
        // Задняя грань
        addQuad(buf, matrix, -s,-s,-s, -s, s,-s,  s, s,-s,  s,-s,-s,  (int)(r*.9),(int)(g*.9),(int)(b*.9), a);
        // Левая грань
        addQuad(buf, matrix, -s,-s,-s, -s,-s, s, -s, s, s, -s, s,-s,  (int)(r*.8),(int)(g*.8),(int)(b*.8), a);
        // Правая грань
        addQuad(buf, matrix,  s,-s,-s,  s, s,-s,  s, s, s,  s,-s, s,  (int)(r*.8),(int)(g*.8),(int)(b*.8), a);
        // Верхняя грань
        addQuad(buf, matrix, -s, s,-s, -s, s, s,  s, s, s,  s, s,-s,  Math.min(255,(int)(r*1.1)),Math.min(255,(int)(g*1.1)),Math.min(255,(int)(b*1.1)), a);
        // Нижняя грань
        addQuad(buf, matrix, -s,-s,-s,  s,-s,-s,  s,-s, s, -s,-s, s,  (int)(r*.7),(int)(g*.7),(int)(b*.7), a);

        ms.pop();
    }

    private void addQuad(BufferBuilder buf, Matrix4f m,
                         float x1,float y1,float z1, float x2,float y2,float z2,
                         float x3,float y3,float z3, float x4,float y4,float z4,
                         int r, int g, int b, int a) {
        buf.vertex(m, x1,y1,z1).color(r,g,b,a);
        buf.vertex(m, x2,y2,z2).color(r,g,b,a);
        buf.vertex(m, x3,y3,z3).color(r,g,b,a);

        buf.vertex(m, x1,y1,z1).color(r,g,b,a);
        buf.vertex(m, x3,y3,z3).color(r,g,b,a);
        buf.vertex(m, x4,y4,z4).color(r,g,b,a);
    }
}

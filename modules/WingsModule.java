package com.yourcheat.modules;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WingsModule implements IModule {

    private boolean enabled = false;
    public boolean showSelf    = true;
    public boolean showPlayers = false;
    public float size          = 1.0f;
    public Color color         = new Color(140, 60, 220, 220);
    public boolean rainbow     = true;

    // Форма крыла (из оригинала)
    private static final float[][] SHAPE = {
        {0.08f,  0.10f,  0.88f},
        {0.28f,  0.34f,  0.78f},
        {0.56f,  0.82f,  0.62f},
        {0.86f,  0.30f,  0.52f},
        {1.14f,  0.46f,  0.40f},
        {1.24f,  0.04f,  0.30f},
        {1.02f, -0.18f,  0.28f},
        {1.18f, -0.64f,  0.22f},
        {0.86f, -0.46f,  0.20f},
        {0.80f, -0.98f,  0.14f},
        {0.54f, -0.74f,  0.16f},
        {0.30f, -1.16f,  0.12f},
        {0.10f, -0.54f,  0.18f}
    };

    private float smoothBodyYaw = 0f;
    private boolean yawInit = false;

    @Override public String getName()           { return "Wings"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; if (!v) yawInit = false; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Size",    size,  0.5f, 2.0f, v -> size = v));
        list.add(new ClickGUI.BoolSetting("Rainbow",   rainbow,     v -> rainbow     = v));
        list.add(new ClickGUI.ColorSetting("Color",    color,       c -> color       = c));
        list.add(new ClickGUI.BoolSetting("On self",   showSelf,    v -> showSelf    = v));
        list.add(new ClickGUI.BoolSetting("On players",showPlayers, v -> showPlayers = v));
        return list;
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        float pt = ctx.tickCounter().getTickDelta(true);
        Vec3d camPos = ctx.camera().getPos();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(net.minecraft.client.gl.ShaderProgramKeys.POSITION_COLOR);

        for (PlayerEntity player : mc.world.getPlayers()) {
            boolean isSelf = player == mc.player;
            if (isSelf && !showSelf) continue;
            if (!isSelf && !showPlayers) continue;
            if (!player.isAlive()) continue;
            if (hasElytra(player)) continue;
            if (isSelf && mc.options.getPerspective().isFirstPerson()) continue;

            renderPlayerWings(ctx.matrixStack(), player, pt, camPos);
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private boolean hasElytra(PlayerEntity p) {
        return p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private void renderPlayerWings(MatrixStack ms, PlayerEntity player, float pt, Vec3d cam) {
        double x = MathHelper.lerp(pt, player.prevX, player.getX()) - cam.x;
        double y = MathHelper.lerp(pt, player.prevY, player.getY()) - cam.y;
        double z = MathHelper.lerp(pt, player.prevZ, player.getZ()) - cam.z;

        float targetYaw = MathHelper.lerpAngleDegrees(pt, player.prevBodyYaw, player.bodyYaw);
        if (player == MinecraftClient.getInstance().player) {
            if (!yawInit) { smoothBodyYaw = targetYaw; yawInit = true; }
            float delta = MathHelper.wrapDegrees(targetYaw - smoothBodyYaw);
            delta = MathHelper.clamp(delta, -14f, 14f);
            smoothBodyYaw += delta;
            targetYaw = smoothBodyYaw;
        }

        float move      = MathHelper.clamp(player.limbAnimator.getSpeed(pt), 0f, 1f);
        float flapSpeed = 0.15f;
        float flapAmp   = 4.5f;
        float flap      = (float) Math.sin((player.age + pt) * flapSpeed) * flapAmp;
        float open      = (8f + flap + move * 0.18f) * 1f;
        float wingScale = size;

        int baseColor = rainbow
            ? Color.HSBtoRGB((System.currentTimeMillis() % 5000L) / 5000f, 0.8f, 0.9f)
            : color.getRGB();

        ms.push();
        ms.translate(x, y, z);
        ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(180f - targetYaw)));
        ms.translate(0f, 1.38f, 0.10f);
        ms.scale(wingScale, wingScale, wingScale);

        renderSide(ms, -1f, open, baseColor);
        renderSide(ms,  1f, open, baseColor);
        ms.pop();
    }

    private void renderSide(MatrixStack ms, float side, float open, int baseColor) {
        ms.push();
        ms.translate(side * 0.06f, -0.02f, 0.06f);
        ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(side * open)));
        ms.multiply(new Quaternionf().rotationZ((float) Math.toRadians(side * -11f)));
        ms.multiply(new Quaternionf().rotationX((float) Math.toRadians(-4f)));

        int glowColor = interpolateColor(baseColor, 0xFFFFFFFF, 0.28f);
        int coreColor = interpolateColor(baseColor, 0xFFFFFFFF, 0.55f);

        // Слой свечения (additive blend)
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                               com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        drawLayer(ms, side, 1.22f, setAlpha(glowColor, 56),  setAlpha(glowColor, 0));
        drawLayer(ms, side, 0.84f, setAlpha(coreColor, 66),  setAlpha(coreColor, 0));

        // Основной слой
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                               com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        drawLayer(ms, side, 1.0f, setAlpha(baseColor, 220), setAlpha(baseColor, 10));

        // Обводка
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
                               com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE);
        drawOutline(ms, side, 1.0f, setAlpha(baseColor, 140));
        drawRibs(ms, side, 0.96f, setAlpha(glowColor, 51));

        ms.pop();
    }

    private void drawLayer(MatrixStack ms, float side, float scale, int rootColor, int edgeColor) {
        Matrix4f mat = ms.peek().getPositionMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        for (int i = 0; i < SHAPE.length; i++) {
            float[] cur  = SHAPE[i];
            float[] next = SHAPE[(i + 1) % SHAPE.length];
            addVertex(buf, mat, 0, 0, 0, rootColor);
            addVertex(buf, mat, side * cur[0]  * scale, cur[1]  * scale, 0, applyAlpha(edgeColor, cur[2]));
            addVertex(buf, mat, side * next[0] * scale, next[1] * scale, 0, applyAlpha(edgeColor, next[2]));
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawOutline(MatrixStack ms, float side, float scale, int color) {
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.lineWidth(1.35f);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        for (float[] p : SHAPE) addVertex(buf, mat, side * p[0] * scale, p[1] * scale, 0, color);
        addVertex(buf, mat, side * SHAPE[0][0] * scale, SHAPE[0][1] * scale, 0, color);
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private void drawRibs(MatrixStack ms, float side, float scale, int color) {
        int[] ribs = {2, 4, 7, 9, 11};
        Matrix4f mat = ms.peek().getPositionMatrix();
        RenderSystem.lineWidth(0.9f);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
        for (int idx : ribs) {
            float[] p = SHAPE[idx];
            addVertex(buf, mat, 0, 0, 0, setAlpha(color, Math.max(8, (int)((alpha(color)) * 0.75f))));
            addVertex(buf, mat, side * p[0] * scale, p[1] * scale, 0, applyAlpha(color, p[2]));
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private static void addVertex(BufferBuilder buf, Matrix4f mat, float x, float y, float z, int color) {
        buf.vertex(mat, x, y, z).color(
            (color>>16)&0xFF, (color>>8)&0xFF, color&0xFF, (color>>24)&0xFF);
    }

    private static int alpha(int c)               { return (c >> 24) & 0xFF; }
    private static int setAlpha(int c, int a)     { return (MathHelper.clamp(a,0,255) << 24) | (c & 0x00FFFFFF); }
    private static int applyAlpha(int c, float m) { return setAlpha(c, Math.max(0,Math.min(255,(int)(alpha(c)*m)))); }
    private static int interpolateColor(int c1, int c2, float t) {
        int a1=(c1>>24)&0xFF,r1=(c1>>16)&0xFF,g1=(c1>>8)&0xFF,b1=c1&0xFF;
        int a2=(c2>>24)&0xFF,r2=(c2>>16)&0xFF,g2=(c2>>8)&0xFF,b2=c2&0xFF;
        return ((int)(a1+(a2-a1)*t)<<24)|((int)(r1+(r2-r1)*t)<<16)|((int)(g1+(g2-g1)*t)<<8)|(int)(b1+(b2-b1)*t);
    }
}


package com.yourcheat.modules;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.DefaultVertexFormats;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * TargetESP — вращающийся квадрат вокруг цели (entity под прицелом).
 *
 * Два режима:
 *  MODE_SPIN  — квадрат вращается вокруг хитбокса (как на референсе)
 *  MODE_CORNER — только угловые скобки, тоже вращаются
 *
 * Подпиши: MinecraftForge.EVENT_BUS.register(TargetESPModule.INSTANCE);
 */
public class TargetESPModule implements IClientModule {

    public static final TargetESPModule INSTANCE = new TargetESPModule();
    private TargetESPModule() {}

    // ── Настройки ──────────────────────────────────────────────────
    private boolean enabled  = false;
    public float lineWidth   = 1.5f;
    public float spinSpeed   = 2.0f;  // градусов в тик
    public Color color       = new Color(200, 80, 255, 200);
    public boolean cornerMode = false; // false = полный квадрат, true = скобки

    // runtime
    private float angle = 0f;
    private LivingEntity target = null;

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; if (!v) target = null; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Line Width", lineWidth, 0.5f, 4.0f, v -> lineWidth = v));
        list.add(new ClickGUI.SliderSetting("Spin Speed", spinSpeed, 0.5f, 8.0f, v -> spinSpeed = v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        list.add(new ClickGUI.BoolSetting("Corner Mode", cornerMode, v -> cornerMode = v));
        return list;
    }

    // ── Tick (обновлять угол) ──────────────────────────────────────
    // Вызывай из ClientTickEvent или updateTarget() в onRenderWorld
    public void tick() {
        angle = (angle + spinSpeed) % 360f;
        // Найти таргет под прицелом
        Minecraft mc = Minecraft.getInstance();
        if (mc.objectMouseOver != null && mc.objectMouseOver.getEntity() instanceof LivingEntity) {
            target = (LivingEntity) mc.objectMouseOver.getEntity();
        } else {
            target = null;
        }
    }

    // ── Рендер ─────────────────────────────────────────────────────
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();

        // апдейт таргета и угла
        tick();

        if (target == null || !target.isAlive()) return;

        float pt = event.getPartialTicks();
        Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView();

        double px = target.lastTickPosX + (target.getPosX() - target.lastTickPosX) * pt - cam.x;
        double py = target.lastTickPosY + (target.getPosY() - target.lastTickPosY) * pt - cam.y;
        double pz = target.lastTickPosZ + (target.getPosZ() - target.lastTickPosZ) * pt - cam.z;

        AxisAlignedBB bb = target.getBoundingBox().offset(-target.getPosX(), -target.getPosY(), -target.getPosZ());
        double w = (bb.maxX - bb.minX) / 2.0 + 0.1;
        double h = (bb.maxY - bb.minY) + 0.1;

        MatrixStack ms = event.getMatrixStack();
        ms.push();
        ms.translate(px, py, pz);

        // Вращаем вокруг Y
        ms.rotate(com.mojang.blaze3d.matrix.MatrixStack.Entry.class.cast(ms.getLast())
            .getMatrix().toString().contains("") ?
            net.minecraft.util.math.vector.Quaternion.ONE :
            new net.minecraft.util.math.vector.Quaternion(
                new net.minecraft.util.math.vector.Vector3f(0, 1, 0),
                angle,
                true
            )
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(Math.max(1f, lineWidth));

        float r = color.getRed()   / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue()  / 255f;
        float a = color.getAlpha() / 255f;

        if (cornerMode) {
            drawCornerBox(ms, (float)(-w), 0f, (float)(-w), (float)(w * 2), (float)h, r, g, b, a);
        } else {
            drawWireBox(ms, (float)(-w), 0f, (float)(-w), (float)(w * 2), (float)h, r, g, b, a);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
        ms.pop();
    }

    /** Полный wireframe-квадрат (box) */
    private void drawWireBox(MatrixStack ms, float x, float y, float z,
                             float w, float h, float r, float g, float b, float a) {
        Matrix4f mat = ms.getLast().getMatrix();
        com.mojang.blaze3d.vertex.BufferBuilder buf =
            com.mojang.blaze3d.vertex.Tessellator.getInstance().getBuffer();

        // 4 вертикальных ребра
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        // bottom loop
        line(buf, mat, x,     y,     z,     x + w, y,     z,     r, g, b, a);
        line(buf, mat, x + w, y,     z,     x + w, y,     z + w, r, g, b, a);
        line(buf, mat, x + w, y,     z + w, x,     y,     z + w, r, g, b, a);
        line(buf, mat, x,     y,     z + w, x,     y,     z,     r, g, b, a);
        // top loop
        line(buf, mat, x,     y + h, z,     x + w, y + h, z,     r, g, b, a);
        line(buf, mat, x + w, y + h, z,     x + w, y + h, z + w, r, g, b, a);
        line(buf, mat, x + w, y + h, z + w, x,     y + h, z + w, r, g, b, a);
        line(buf, mat, x,     y + h, z + w, x,     y + h, z,     r, g, b, a);
        // verticals
        line(buf, mat, x,     y, z,     x,     y + h, z,     r, g, b, a);
        line(buf, mat, x + w, y, z,     x + w, y + h, z,     r, g, b, a);
        line(buf, mat, x + w, y, z + w, x + w, y + h, z + w, r, g, b, a);
        line(buf, mat, x,     y, z + w, x,     y + h, z + w, r, g, b, a);
        com.mojang.blaze3d.vertex.Tessellator.getInstance().draw();
    }

    /** Только угловые скобки (corner lines) */
    private void drawCornerBox(MatrixStack ms, float x, float y, float z,
                               float w, float h, float r, float g, float b, float a) {
        Matrix4f mat = ms.getLast().getMatrix();
        float cx = w * 0.25f; // длина угловой скобки
        float cy = h * 0.25f;

        com.mojang.blaze3d.vertex.BufferBuilder buf =
            com.mojang.blaze3d.vertex.Tessellator.getInstance().getBuffer();
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // Нижние 4 угла
        // FL
        line(buf, mat, x, y, z, x + cx, y, z, r, g, b, a);
        line(buf, mat, x, y, z, x, y, z + cx, r, g, b, a);
        line(buf, mat, x, y, z, x, y + cy, z, r, g, b, a);
        // FR
        line(buf, mat, x+w, y, z, x+w-cx, y, z, r, g, b, a);
        line(buf, mat, x+w, y, z, x+w, y, z+cx, r, g, b, a);
        line(buf, mat, x+w, y, z, x+w, y+cy, z, r, g, b, a);
        // BL
        line(buf, mat, x, y, z+w, x+cx, y, z+w, r, g, b, a);
        line(buf, mat, x, y, z+w, x, y, z+w-cx, r, g, b, a);
        line(buf, mat, x, y, z+w, x, y+cy, z+w, r, g, b, a);
        // BR
        line(buf, mat, x+w, y, z+w, x+w-cx, y, z+w, r, g, b, a);
        line(buf, mat, x+w, y, z+w, x+w, y, z+w-cx, r, g, b, a);
        line(buf, mat, x+w, y, z+w, x+w, y+cy, z+w, r, g, b, a);
        // Верхние 4 угла
        line(buf, mat, x,   y+h, z,   x+cx, y+h, z,   r, g, b, a);
        line(buf, mat, x,   y+h, z,   x,    y+h, z+cx, r, g, b, a);
        line(buf, mat, x,   y+h, z,   x,    y+h-cy, z, r, g, b, a);
        line(buf, mat, x+w, y+h, z,   x+w-cx, y+h, z, r, g, b, a);
        line(buf, mat, x+w, y+h, z,   x+w, y+h, z+cx, r, g, b, a);
        line(buf, mat, x+w, y+h, z,   x+w, y+h-cy, z, r, g, b, a);
        line(buf, mat, x,   y+h, z+w, x+cx, y+h, z+w, r, g, b, a);
        line(buf, mat, x,   y+h, z+w, x,    y+h, z+w-cx, r, g, b, a);
        line(buf, mat, x,   y+h, z+w, x,    y+h-cy, z+w, r, g, b, a);
        line(buf, mat, x+w, y+h, z+w, x+w-cx, y+h, z+w, r, g, b, a);
        line(buf, mat, x+w, y+h, z+w, x+w, y+h, z+w-cx, r, g, b, a);
        line(buf, mat, x+w, y+h, z+w, x+w, y+h-cy, z+w, r, g, b, a);

        com.mojang.blaze3d.vertex.Tessellator.getInstance().draw();
    }

    private static void line(com.mojang.blaze3d.vertex.BufferBuilder buf, Matrix4f mat,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        buf.pos(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(mat, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}


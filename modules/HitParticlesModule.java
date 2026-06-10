package com.yourcheat.modules;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yourcheat.gui.ClickGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.vertex.DefaultVertexFormats;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * HitParticles — при ударе врага появляются снежинки или сердечки.
 *
 * Подпиши: MinecraftForge.EVENT_BUS.register(HitParticlesModule.INSTANCE);
 *
 * Два типа частиц:
 *  TYPE_SNOWFLAKE — рисует 6-лучевую снежинку через GL_LINES
 *  TYPE_HEART     — рисует сердечко через GL_LINE_LOOP
 */
public class HitParticlesModule implements IClientModule {

    public static final HitParticlesModule INSTANCE = new HitParticlesModule();
    private HitParticlesModule() {}

    // ── Настройки ──────────────────────────────────────────────────
    private boolean enabled    = false;
    public float particleSize  = 0.15f;   // размер частицы
    public float lineThickness = 1.5f;    // толщина линий
    public Color color         = new Color(180, 120, 255, 230);
    public boolean useHearts   = false;   // false = снежинки, true = сердечки
    public int count           = 8;       // кол-во частиц за хит

    private static final Random RNG = new Random();
    private final List<Particle> particles = new ArrayList<>();

    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; if (!v) particles.clear(); }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Size",      particleSize,  0.05f, 0.5f, v -> particleSize  = v));
        list.add(new ClickGUI.SliderSetting("Thickness", lineThickness, 0.5f,  4.0f, v -> lineThickness = v));
        list.add(new ClickGUI.SliderSetting("Count",     count,         2,     16,   v -> count = (int)v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        list.add(new ClickGUI.BoolSetting("Hearts (not snowflakes)", useHearts, v -> useHearts = v));
        return list;
    }

    // ── Спавн частиц при хите ─────────────────────────────────────
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // Только если наш игрок нанёс урон
        if (event.getSource() != DamageSource.causePlayerDamage(mc.player)) return;

        LivingEntity victim = event.getEntityLiving();
        double x = victim.getPosX();
        double y = victim.getPosY() + victim.getHeight() * 0.7;
        double z = victim.getPosZ();

        for (int i = 0; i < count; i++) {
            double vx = (RNG.nextDouble() - 0.5) * 0.15;
            double vy = RNG.nextDouble() * 0.12 + 0.04;
            double vz = (RNG.nextDouble() - 0.5) * 0.15;
            float rotation = RNG.nextFloat() * 360f;
            particles.add(new Particle(x, y, z, vx, vy, vz, rotation));
        }
    }

    // ── Рендер ─────────────────────────────────────────────────────
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!enabled || particles.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float pt = event.getPartialTicks();
        Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        ActiveRenderInfo ari = mc.gameRenderer.getActiveRenderInfo();

        // Углы камеры для billboarding
        float yaw   = ari.getYaw();
        float pitch = ari.getPitch();

        MatrixStack ms = event.getMatrixStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(Math.max(1f, lineThickness));

        float r = color.getRed()   / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue()  / 255f;

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.tick();
            if (p.life <= 0) { it.remove(); continue; }

            float alpha = (color.getAlpha() / 255f) * ((float)p.life / p.maxLife);

            double dx = p.x - cam.x;
            double dy = p.y - cam.y;
            double dz = p.z - cam.z;

            ms.push();
            ms.translate(dx, dy, dz);

            // Billboard: повернуть к камере
            ms.rotate(new net.minecraft.util.math.vector.Quaternion(
                new Vector3f(0, 1, 0), -yaw, true));
            ms.rotate(new net.minecraft.util.math.vector.Quaternion(
                new Vector3f(1, 0, 0), pitch, true));

            // Spin-анимация частицы
            ms.rotate(new net.minecraft.util.math.vector.Quaternion(
                new Vector3f(0, 0, 1), p.rotation + p.life * 3f, true));

            Matrix4f mat = ms.getLast().getMatrix();

            if (useHearts) {
                drawHeart(mat, particleSize, r, g, b, alpha);
            } else {
                drawSnowflake(mat, particleSize, r, g, b, alpha);
            }

            ms.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1f);
    }

    /** Рисует 6-лучевую снежинку */
    private void drawSnowflake(Matrix4f mat, float size, float r, float g, float b, float a) {
        com.mojang.blaze3d.vertex.BufferBuilder buf =
            com.mojang.blaze3d.vertex.Tessellator.getInstance().getBuffer();
        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        // 6 основных лучей
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i;
            float ex = (float)(Math.cos(angle) * size);
            float ey = (float)(Math.sin(angle) * size);
            buf.pos(mat, 0, 0, 0).color(r, g, b, a).endVertex();
            buf.pos(mat, ex, ey, 0).color(r, g, b, a).endVertex();

            // ветки на каждом луче (на 2/3 от центра)
            float bx = (float)(Math.cos(angle) * size * 0.6f);
            float by = (float)(Math.sin(angle) * size * 0.6f);
            double ba1 = angle + Math.PI / 4;
            double ba2 = angle - Math.PI / 4;
            float branchLen = size * 0.3f;
            buf.pos(mat, bx, by, 0).color(r, g, b, a).endVertex();
            buf.pos(mat, bx + (float)(Math.cos(ba1) * branchLen),
                        by + (float)(Math.sin(ba1) * branchLen), 0)
               .color(r, g, b, a).endVertex();
            buf.pos(mat, bx, by, 0).color(r, g, b, a).endVertex();
            buf.pos(mat, bx + (float)(Math.cos(ba2) * branchLen),
                        by + (float)(Math.sin(ba2) * branchLen), 0)
               .color(r, g, b, a).endVertex();
        }

        com.mojang.blaze3d.vertex.Tessellator.getInstance().draw();
    }

    /** Рисует сердечко через параметрическую кривую */
    private void drawHeart(Matrix4f mat, float size, float r, float g, float b, float a) {
        int segments = 40;
        com.mojang.blaze3d.vertex.BufferBuilder buf =
            com.mojang.blaze3d.vertex.Tessellator.getInstance().getBuffer();
        buf.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            double t = (2.0 * Math.PI * i / segments) - Math.PI;
            // параметрическое сердечко
            float hx = (float)(size * 16 * Math.pow(Math.sin(t), 3) / 16.0);
            float hy = (float)(size * (13 * Math.cos(t) - 5 * Math.cos(2*t)
                                    - 2 * Math.cos(3*t) - Math.cos(4*t)) / 16.0);
            buf.pos(mat, hx, hy, 0).color(r, g, b, a).endVertex();
        }

        com.mojang.blaze3d.vertex.Tessellator.getInstance().draw();
    }

    // ═══════════════════════════════════════════════════════════════
    // ── Particle ───────────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════════
    private static class Particle {
        double x, y, z;
        double vx, vy, vz;
        float rotation;
        int life, maxLife;

        Particle(double x, double y, double z, double vx, double vy, double vz, float rot) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.rotation = rot;
            this.maxLife = 20 + RNG.nextInt(10); // 20-30 тиков
            this.life = maxLife;
        }

        void tick() {
            x += vx; y += vy; z += vz;
            vy -= 0.005; // гравитация
            vx *= 0.92; vz *= 0.92;
            life--;
        }

        static final Random RNG = new Random();
    }
}


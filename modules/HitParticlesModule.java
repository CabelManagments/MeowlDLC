package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class HitParticlesModule implements IModule {

    private boolean enabled    = false;
    public float particleSize  = 0.15f;
    public float lineThickness = 1.5f;
    public Color color         = new Color(180, 120, 255, 230);
    public boolean useHearts   = false;
    public int count           = 8;

    private static final Random RNG = new Random();
    private final List<Particle> particles = new ArrayList<>();

    @Override public String getName() { return "HitParticles"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; if (!v) particles.clear(); }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Size",      particleSize,  0.05f, 0.5f, v -> particleSize  = v));
        list.add(new ClickGUI.SliderSetting("Thickness", lineThickness, 0.5f,  4.0f, v -> lineThickness = v));
        list.add(new ClickGUI.SliderSetting("Count",     count,         2,     16,   v -> count = Math.round(v)));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        list.add(new ClickGUI.BoolSetting("Hearts", useHearts, v -> useHearts = v));
        return list;
    }

    /** Вызывается из mixin когда игрок наносит урон */
    public void spawnAt(double x, double y, double z) {
        if (!enabled) return;
        for (int i = 0; i < count; i++) {
            double vx = (RNG.nextDouble() - 0.5) * 0.15;
            double vy = RNG.nextDouble() * 0.12 + 0.04;
            double vz = (RNG.nextDouble() - 0.5) * 0.15;
            particles.add(new Particle(x, y, z, vx, vy, vz, RNG.nextFloat() * 360f));
        }
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled || particles.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();

        float pt = ctx.tickCounter().getTickDelta(true);
        Vec3d cam = ctx.camera().getPos();
        float yaw   = ctx.camera().getYaw();
        float pitch = ctx.camera().getPitch();

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();

        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.tick();
            if (p.life <= 0) { it.remove(); continue; }

            float a = (color.getAlpha() / 255f) * ((float) p.life / p.maxLife);
            float r = color.getRed()   / 255f;
            float g = color.getGreen() / 255f;
            float b = color.getBlue()  / 255f;

            ms.push();
            ms.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(-yaw)));
            ms.multiply(new Quaternionf().rotationX((float) Math.toRadians(pitch)));
            ms.multiply(new Quaternionf().rotationZ((float) Math.toRadians(p.rotation + p.life * 3f)));

            VertexConsumer vc = vcp.getBuffer(RenderLayer.getLines());
            Matrix4f mat = ms.peek().getPositionMatrix();
            var entry = ms.peek();

            if (useHearts) drawHeart(vc, mat, entry, ms, particleSize, r, g, b, a);
            else           drawSnowflake(vc, mat, entry, ms, particleSize, r, g, b, a);

            vcp.draw(RenderLayer.getLines());
            ms.pop();
        }
    }

    private void drawSnowflake(VertexConsumer vc, Matrix4f mat, net.minecraft.client.util.math.MatrixStack.Entry entry,
                               MatrixStack ms, float size, float r, float g, float b, float a) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i;
            float ex = (float)(Math.cos(angle) * size);
            float ey = (float)(Math.sin(angle) * size);
            vc.vertex(mat,0,0,0).color(r,g,b,a).normal(entry,0,0,1);
            vc.vertex(mat,ex,ey,0).color(r,g,b,a).normal(entry,0,0,1);

            float bx = (float)(Math.cos(angle) * size * 0.6f);
            float by = (float)(Math.sin(angle) * size * 0.6f);
            float bl = size * 0.3f;
            double ba1 = angle + Math.PI/4, ba2 = angle - Math.PI/4;
            vc.vertex(mat,bx,by,0).color(r,g,b,a).normal(entry,0,0,1);
            vc.vertex(mat,bx+(float)(Math.cos(ba1)*bl),by+(float)(Math.sin(ba1)*bl),0).color(r,g,b,a).normal(entry,0,0,1);
            vc.vertex(mat,bx,by,0).color(r,g,b,a).normal(entry,0,0,1);
            vc.vertex(mat,bx+(float)(Math.cos(ba2)*bl),by+(float)(Math.sin(ba2)*bl),0).color(r,g,b,a).normal(entry,0,0,1);
        }
    }

    private void drawHeart(VertexConsumer vc, Matrix4f mat, net.minecraft.client.util.math.MatrixStack.Entry entry,
                           MatrixStack ms, float size, float r, float g, float b, float a) {
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            double t1 = (2.0 * Math.PI * i / seg) - Math.PI;
            double t2 = (2.0 * Math.PI * (i+1) / seg) - Math.PI;
            float x1 = (float)(size * 16 * Math.pow(Math.sin(t1), 3) / 16.0);
            float y1 = (float)(size * (13*Math.cos(t1)-5*Math.cos(2*t1)-2*Math.cos(3*t1)-Math.cos(4*t1)) / 16.0);
            float x2 = (float)(size * 16 * Math.pow(Math.sin(t2), 3) / 16.0);
            float y2 = (float)(size * (13*Math.cos(t2)-5*Math.cos(2*t2)-2*Math.cos(3*t2)-Math.cos(4*t2)) / 16.0);
            vc.vertex(mat,x1,y1,0).color(r,g,b,a).normal(entry,0,0,1);
            vc.vertex(mat,x2,y2,0).color(r,g,b,a).normal(entry,0,0,1);
        }
    }

    private static class Particle {
        double x, y, z, vx, vy, vz;
        float rotation;
        int life, maxLife;

        Particle(double x, double y, double z, double vx, double vy, double vz, float rot) {
            this.x=x; this.y=y; this.z=z;
            this.vx=vx; this.vy=vy; this.vz=vz;
            this.rotation=rot;
            this.maxLife = 20 + RNG.nextInt(10);
            this.life = maxLife;
        }

        void tick() {
            x+=vx; y+=vy; z+=vz;
            vy -= 0.005; vx *= 0.92; vz *= 0.92;
            life--;
        }
    }
}


package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TargetESPModule implements IModule {

    private boolean enabled = false;
    public float lineWidth   = 1.5f;
    public float spinSpeed   = 2.0f;
    public Color color       = new Color(200, 80, 255);
    public boolean cornerMode = false;

    private float angle = 0f;

    @Override public String getName() { return "TargetESP"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) { enabled = v; }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Line Width", lineWidth, 0.5f, 4.0f, v -> lineWidth = v));
        list.add(new ClickGUI.SliderSetting("Spin Speed", spinSpeed, 0.5f, 8.0f, v -> spinSpeed = v));
        list.add(new ClickGUI.ColorSetting("Color", color, c -> color = c));
        list.add(new ClickGUI.BoolSetting("Corner Mode", cornerMode, v -> cornerMode = v));
        return list;
    }

    public void onRender(WorldRenderContext ctx) {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.targetedEntity == null || !(mc.targetedEntity instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) mc.targetedEntity;
        angle = (angle + spinSpeed) % 360f;

        float pt = ctx.tickCounter().getTickDelta(true);
        double cx = ctx.camera().getPos().x;
        double cy = ctx.camera().getPos().y;
        double cz = ctx.camera().getPos().z;

        double px = target.prevX + (target.getX() - target.prevX) * pt - cx;
        double py = target.prevY + (target.getY() - target.prevY) * pt - cy;
        double pz = target.prevZ + (target.getZ() - target.prevZ) * pt - cz;

        Box bb = target.getBoundingBox();
        float w = (float)(bb.getLengthX() / 2.0 + 0.1);
        float h = (float)(bb.getLengthY() + 0.1);

        MatrixStack ms = ctx.matrixStack();
        ms.push();
        ms.translate(px, py, pz);
        ms.multiply(new Quaternionf().rotationY((float) Math.toRadians(angle)));

        VertexConsumerProvider.Immediate vcp = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLines());
        Matrix4f mat = ms.peek().getPositionMatrix();

        float r = color.getRed()   / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue()  / 255f;
        float a = color.getAlpha() / 255f;

        if (cornerMode) {
            drawCornerBox(ms, vc, -w, 0, -w, w * 2, h, r, g, b, a);
        } else {
            drawWireBox(ms, vc, -w, 0, -w, w * 2, h, r, g, b, a);
        }

        vcp.draw(RenderLayer.getLines());
        ms.pop();
    }

    private void drawWireBox(MatrixStack ms, VertexConsumer vc,
                             float x, float y, float z, float w, float h,
                             float r, float g, float b, float a) {
        Matrix4f mat = ms.peek().getPositionMatrix();
        var nm = ms.peek().getNormalMatrix();
        // bottom
        line(vc, mat, nm, x,   y,   z,   x+w, y,   z,   r,g,b,a);
        line(vc, mat, nm, x+w, y,   z,   x+w, y,   z+w, r,g,b,a);
        line(vc, mat, nm, x+w, y,   z+w, x,   y,   z+w, r,g,b,a);
        line(vc, mat, nm, x,   y,   z+w, x,   y,   z,   r,g,b,a);
        // top
        line(vc, mat, nm, x,   y+h, z,   x+w, y+h, z,   r,g,b,a);
        line(vc, mat, nm, x+w, y+h, z,   x+w, y+h, z+w, r,g,b,a);
        line(vc, mat, nm, x+w, y+h, z+w, x,   y+h, z+w, r,g,b,a);
        line(vc, mat, nm, x,   y+h, z+w, x,   y+h, z,   r,g,b,a);
        // verticals
        line(vc, mat, nm, x,   y,   z,   x,   y+h, z,   r,g,b,a);
        line(vc, mat, nm, x+w, y,   z,   x+w, y+h, z,   r,g,b,a);
        line(vc, mat, nm, x+w, y,   z+w, x+w, y+h, z+w, r,g,b,a);
        line(vc, mat, nm, x,   y,   z+w, x,   y+h, z+w, r,g,b,a);
    }

    private void drawCornerBox(MatrixStack ms, VertexConsumer vc,
                               float x, float y, float z, float w, float h,
                               float r, float g, float b, float a) {
        Matrix4f mat = ms.peek().getPositionMatrix();
        var nm = ms.peek().getNormalMatrix();
        float cx = w * 0.25f, cy = h * 0.25f;
        // bottom corners
        line(vc,mat,nm, x,y,z,       x+cx,y,z,     r,g,b,a); line(vc,mat,nm, x,y,z,     x,y,z+cx,   r,g,b,a); line(vc,mat,nm, x,y,z,   x,y+cy,z,   r,g,b,a);
        line(vc,mat,nm, x+w,y,z,     x+w-cx,y,z,   r,g,b,a); line(vc,mat,nm, x+w,y,z,   x+w,y,z+cx, r,g,b,a); line(vc,mat,nm, x+w,y,z, x+w,y+cy,z, r,g,b,a);
        line(vc,mat,nm, x,y,z+w,     x+cx,y,z+w,   r,g,b,a); line(vc,mat,nm, x,y,z+w,   x,y,z+w-cx, r,g,b,a); line(vc,mat,nm, x,y,z+w, x,y+cy,z+w, r,g,b,a);
        line(vc,mat,nm, x+w,y,z+w,   x+w-cx,y,z+w, r,g,b,a); line(vc,mat,nm, x+w,y,z+w, x+w,y,z+w-cx,r,g,b,a); line(vc,mat,nm, x+w,y,z+w, x+w,y+cy,z+w, r,g,b,a);
        // top corners
        line(vc,mat,nm, x,y+h,z,     x+cx,y+h,z,   r,g,b,a); line(vc,mat,nm, x,y+h,z,     x,y+h,z+cx,   r,g,b,a); line(vc,mat,nm, x,y+h,z,     x,y+h-cy,z,     r,g,b,a);
        line(vc,mat,nm, x+w,y+h,z,   x+w-cx,y+h,z, r,g,b,a); line(vc,mat,nm, x+w,y+h,z,   x+w,y+h,z+cx, r,g,b,a); line(vc,mat,nm, x+w,y+h,z,   x+w,y+h-cy,z,   r,g,b,a);
        line(vc,mat,nm, x,y+h,z+w,   x+cx,y+h,z+w, r,g,b,a); line(vc,mat,nm, x,y+h,z+w,   x,y+h,z+w-cx, r,g,b,a); line(vc,mat,nm, x,y+h,z+w,   x,y+h-cy,z+w,   r,g,b,a);
        line(vc,mat,nm, x+w,y+h,z+w, x+w-cx,y+h,z+w,r,g,b,a);line(vc,mat,nm, x+w,y+h,z+w, x+w,y+h,z+w-cx,r,g,b,a);line(vc,mat,nm, x+w,y+h,z+w, x+w,y+h-cy,z+w, r,g,b,a);
    }

    private static void line(VertexConsumer vc, Matrix4f mat, org.joml.Matrix3f nm,
                              float x1,float y1,float z1, float x2,float y2,float z2,
                              float r,float g,float b,float a) {
        float dx = x2-x1, dy = y2-y1, dz = z2-z1;
        float len = (float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (len == 0) return;
        vc.vertex(mat,x1,y1,z1).color(r,g,b,a).normal(nm,dx/len,dy/len,dz/len);
        vc.vertex(mat,x2,y2,z2).color(r,g,b,a).normal(nm,dx/len,dy/len,dz/len);
    }
}


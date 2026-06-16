package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class KillAuraModule implements IModule {

    private boolean enabled = false;

    // Настройки
    public float range      = 3.5f;
    public float cps        = 9f;    // кликов в секунду
    public int   profile    = 0;     // 0=SpookyTime 1=FunTime 2=HolyWorld 3=Matrix
    public boolean players  = true;
    public boolean mobs     = false;
    public boolean onlyVisible = true;

    // Runtime
    private float currentYaw   = 0f;
    private float currentPitch = 0f;
    private LivingEntity target = null;
    private long lastAttack    = 0;
    private long lastSwing     = 0;
    private int  hitCount      = 0;
    private boolean aiming     = false;
    private float swaySeed     = (float)(Math.random() * 100);

    // SpookyTime sway
    private float swayPhase = 0f;

    private final Random rng = new Random();

    @Override public String getName()           { return "KillAura"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) {
        enabled = v;
        if (!v) { target = null; aiming = false; }
    }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Range",  range, 2.5f, 6.0f, v -> range = v));
        list.add(new ClickGUI.SliderSetting("CPS",    cps,   4f,   20f,  v -> cps   = v));
        list.add(new ClickGUI.BoolSetting("Players",  players, v -> players = v));
        list.add(new ClickGUI.BoolSetting("Mobs",     mobs,    v -> mobs    = v));
        list.add(new ClickGUI.BoolSetting("Only visible", onlyVisible, v -> onlyVisible = v));
        return list;
    }

    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        // Синхронизируем текущие серверные ротации
        currentYaw   = mc.player.getYaw();
        currentPitch = mc.player.getPitch();
        swayPhase    += 0.05f;

        // Ищем цель
        target = findTarget(mc);
        if (target == null) return;

        // Аим-поинт — ближайшая точка хитбокса
        Vec3d aimPoint = getAimPoint(mc.player, target);
        float[] angles = getAnglesTo(mc.player, aimPoint);
        float targetYaw   = angles[0];
        float targetPitch = angles[1];

        // Применяем профиль ротации
        float[] rotated = applyProfile(targetYaw, targetPitch, mc);
        float newYaw   = rotated[0];
        float newPitch = rotated[1];

        // GCD snap
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        newYaw   = gcdSnap(newYaw,   currentYaw,   sensitivity);
        newPitch = gcdSnap(newPitch, currentPitch, sensitivity);

        // Silent rotation — применяем к пакету не меняя камеру
        mc.player.setYaw(newYaw);
        mc.player.setPitch(MathHelper.clamp(newPitch, -90f, 90f));

        // Проверяем можем ли атаковать
        long now = System.currentTimeMillis();
        float delay = 1000f / cpsWithJitter();
        if (now - lastAttack < (long) delay) return;

        // Raycast проверка (не через стену)
        if (onlyVisible && !canSeeTarget(mc.player, target)) return;

        // Угол между взглядом и целью
        float[] lookAngles = getAnglesTo(mc.player, aimPoint);
        float yawDiff = Math.abs(MathHelper.wrapDegrees(newYaw - lookAngles[0]));
        if (yawDiff > 60f) return;

        // Атакуем
        mc.player.swingHand(Hand.MAIN_HAND);
        mc.interactionManager.attackEntity(mc.player, target);
        lastAttack = now;
        hitCount++;
    }

    // ── Профили ротации ─────────────────────────────────────────────
    private float[] applyProfile(float targetYaw, float targetPitch, MinecraftClient mc) {
        float yawDelta   = MathHelper.wrapDegrees(targetYaw   - currentYaw);
        float pitchDelta = MathHelper.wrapDegrees(targetPitch - currentPitch);
        float total      = (float) Math.hypot(yawDelta, pitchDelta);
        if (total == 0) return new float[]{currentYaw, currentPitch};

        boolean canAtk = System.currentTimeMillis() - lastAttack > (1000f / cpsWithJitter());

        return switch (profile) {
            case 1  -> funTimeProfile(yawDelta, pitchDelta, total, canAtk);
            case 2  -> holyWorldProfile(yawDelta, pitchDelta, total, canAtk);
            case 3  -> matrixProfile(yawDelta, pitchDelta, total, canAtk);
            default -> spookyTimeProfile(yawDelta, pitchDelta, total, canAtk);
        };
    }

    /** SpookyTime / Releon — SPAngle */
    private float[] spookyTimeProfile(float yD, float pD, float total, boolean attack) {
        float yawLimit   = Math.min(Math.abs(yD), 74f + rng.nextFloat() * 1.03f);
        float pitchLimit = Math.min(Math.abs(pD), 32.33f);

        float yScale, pScale;
        boolean yReached = Math.abs(yD) >= yawLimit;
        boolean pReached = Math.abs(pD) >= pitchLimit;

        float yMax = yReached ? (65f + rng.nextFloat() * 35f) : (7.7f + rng.nextFloat() * 4.4f);
        float pMax = pReached ? (65f + rng.nextFloat() * 35f) : (7.7f + rng.nextFloat() * 4.4f);

        yScale = Math.min(total, yMax) / total;
        pScale = Math.min(total, pMax) / total;

        if (!yReached) yScale = ease(yScale);
        if (!pReached) pScale = ease(pScale);

        float newYaw   = currentYaw   + yD * yScale;
        float newPitch = currentPitch + pD * pScale;

        // Sway
        long ms = System.currentTimeMillis();
        float sway = (float)(Math.sin((ms % 12000L) / 1200.0 * Math.PI * 2) * 1.15f);
        sway *= gaussianApprox();
        newYaw += sway;

        newPitch = MathHelper.clamp(newPitch, -89f, 89f);
        return new float[]{newYaw, newPitch};
    }

    /** FunTime / Releon 16.04 — FTAngle */
    private float[] funTimeProfile(float yD, float pD, float total, boolean attack) {
        float cap = 130f;
        float yStep  = Math.abs(yD / total) * cap;
        float pStep  = Math.abs(pD / total) * cap;
        float appliedY = MathHelper.clamp(yD, -yStep, yStep);
        float appliedP = MathHelper.clamp(pD, -pStep, pStep);

        float newYaw   = MathHelper.lerp(0.85f, currentYaw,   currentYaw   + appliedY);
        float newPitch = MathHelper.lerp(0.85f, currentPitch, currentPitch + appliedP);

        if (!attack) {
            long ms = System.currentTimeMillis();
            float jitterRange = 18f + rng.nextFloat() * 10f;
            newYaw   += jitterRange * Math.sin(ms / 60.0);
            newPitch += (6f + rng.nextFloat() * 10f) * Math.cos(ms / 60.0);
        }

        newPitch = MathHelper.clamp(newPitch, -89f, 89f);
        return new float[]{newYaw, newPitch};
    }

    /** HolyWorld — HWAngle */
    private float[] holyWorldProfile(float yD, float pD, float total, boolean attack) {
        float speed = attack
            ? 0.86f + rng.nextFloat() * 0.10f
            : 0.1f  + rng.nextFloat() * 0.30f;
        float extraSpeed = speed + rng.nextFloat() * 0.2f;

        float newYaw   = MathHelper.lerp(extraSpeed, currentYaw,   currentYaw   + yD);
        float newPitch = MathHelper.lerp(extraSpeed, currentPitch, currentPitch + pD);

        newPitch = MathHelper.clamp(newPitch, -89f, 89f);
        return new float[]{newYaw, newPitch};
    }

    /** Matrix — MatrixAngle */
    private float[] matrixProfile(float yD, float pD, float total, boolean attack) {
        float speed = attack ? 1.0f : rng.nextFloat() * 0.5f;
        float newYaw   = currentYaw   + yD * speed;
        float newPitch = currentPitch + pD * speed;

        long ms = System.currentTimeMillis();
        float freq = 15f + rng.nextFloat() * 130f;
        newYaw   += rng.nextFloat() * 6f * (float)Math.sin(ms / freq);
        newPitch += rng.nextFloat() * 3f * (float)Math.sin(ms / freq);

        newPitch = MathHelper.clamp(newPitch, -89f, 89f);
        return new float[]{newYaw, newPitch};
    }

    // ── Хелперы ──────────────────────────────────────────────────────

    private float ease(float t) { return t * (0.5f + 0.5f * t); }

    private float gaussianApprox() {
        // Приближение гауссова распределения через сумму uniform
        return (rng.nextFloat() + rng.nextFloat() + rng.nextFloat()) / 3f - 0.5f;
    }

    /** GCD snap — обязательный для легитности */
    private float gcdSnap(float newVal, float oldVal, float sensitivity) {
        float f   = sensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 1.2f * 0.15f;
        float delta = newVal - oldVal;
        delta = Math.round(delta / gcd) * gcd;
        return oldVal + delta;
    }

    /** CPS с гауссовым джиттером (центр=cps, σ≈1.5) */
    private float cpsWithJitter() {
        float jitter = (rng.nextFloat() + rng.nextFloat() - 1f) * 1.5f;
        return Math.max(1f, cps + jitter);
    }

    private float[] getAnglesTo(PlayerEntity from, Vec3d to) {
        Vec3d eye   = from.getEyePos();
        double dx   = to.x - eye.x;
        double dy   = to.y - eye.y;
        double dz   = to.z - eye.z;
        double dist = Math.sqrt(dx*dx + dz*dz);
        float yaw   = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
        return new float[]{yaw, pitch};
    }

    /** Ближайшая точка хитбокса к глазу игрока */
    private Vec3d getAimPoint(PlayerEntity from, LivingEntity target) {
        Vec3d eye = from.getEyePos();
        Box bb    = target.getBoundingBox();
        double cx = MathHelper.clamp(eye.x, bb.minX, bb.maxX);
        double cy = MathHelper.clamp(eye.y, bb.minY, bb.maxY);
        double cz = MathHelper.clamp(eye.z, bb.minZ, bb.maxZ);
        return new Vec3d(cx, cy, cz);
    }

    private boolean canSeeTarget(PlayerEntity from, LivingEntity target) {
        return from.canSee(target);
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        LivingEntity closest = null;
        float minDist = range + 1f;
        Vec3d eye = mc.player.getEyePos();

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (le == mc.player) continue;
            if (!le.isAlive()) continue;
            if (le instanceof PlayerEntity p) {
                if (!players) continue;
                if (p.isCreative()) continue;
            } else {
                if (!mobs) continue;
            }

            float dist = (float) e.squaredDistanceTo(mc.player);
            float rangeSq = range * range;
            if (dist > rangeSq) continue;

            if (dist < minDist) {
                minDist = dist;
                closest = le;
            }
        }
        return closest;
    }
}


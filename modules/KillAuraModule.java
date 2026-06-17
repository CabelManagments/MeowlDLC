package com.yourcheat.modules;

import com.yourcheat.gui.ClickGUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * KillAura с профилем ротации портированным с SPAngle (SpookyTime bypass).
 * Ключевая фича оригинала — "release hold/slowdown": при потере цели ротация
 * НЕ дёргается обратно мгновенно, а сначала замирает на 150мс, потом плавно
 * (ease) уходит к естественному направлению взгляда. Это убирает характерный
 * snap-паттерн который легко детектится.
 */
public class KillAuraModule implements IModule {

    private boolean enabled = false;

    public float range        = 3.5f;
    public float cps          = 9f;
    public boolean players    = true;
    public boolean mobs       = false;
    public boolean onlyVisible = true;

    // ── Константы профиля (из SPAngle) ──────────────────────────────
    private static final long  RELEASE_HOLD_MS     = 150L;
    private static final long  RELEASE_SLOWDOWN_MS = 350L; // у оригинала 2мс — слишком быстро, увеличил для реализма
    private static final float SHAKE_INTENSITY     = 1.15f;
    private static final float SHAKE_SPEED         = 3.0f;
    private static final float EPSILON             = 1.0f;
    private final Random rng = new Random();

    // ── Runtime: ротация ─────────────────────────────────────────────
    private float currentYaw, currentPitch;
    private LivingEntity target = null;

    private boolean releaseHoldActive = false;
    private long    releaseHoldUntil  = 0L;
    private boolean releaseSlowdownActive = false;
    private long    releaseSlowdownStart  = 0L;
    private float[] releaseFromAngle = null;
    private float[] releaseToAngle   = null;
    private boolean hadTargetLastTick = false;

    private long lastAttack = 0;

    @Override public String getName()           { return "KillAura"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) {
        enabled = v;
        if (!v) { target = null; resetRelease(); }
    }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Range", range, 2.5f, 6.0f, v -> range = v));
        list.add(new ClickGUI.SliderSetting("CPS",   cps,   4f,   20f,  v -> cps   = v));
        list.add(new ClickGUI.BoolSetting("Players",     players,     v -> players     = v));
        list.add(new ClickGUI.BoolSetting("Mobs",        mobs,        v -> mobs        = v));
        list.add(new ClickGUI.BoolSetting("Only visible",onlyVisible, v -> onlyVisible = v));
        return list;
    }

    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        currentYaw   = mc.player.getYaw();
        currentPitch = mc.player.getPitch();

        target = findTarget(mc);
        boolean hasTarget = target != null;

        float[] result;
        if (hasTarget) {
            Vec3d aimPoint = getAimPoint(mc.player, target);
            float[] targetAngles = getAnglesTo(mc.player, aimPoint);
            result = limitAngleChange(targetAngles);
        } else {
            // Естественное направление взгляда игрока (текущее, без цели)
            result = handleRelease(new float[]{currentYaw, currentPitch}, hasTarget);
        }

        // GCD snap
        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float newYaw   = gcdSnap(result[0], currentYaw, sensitivity);
        float newPitch = gcdSnap(MathHelper.clamp(result[1], -90f, 90f), currentPitch, sensitivity);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);

        if (!hasTarget) return;

        // Атака
        long now = System.currentTimeMillis();
        float delay = 1000f / cpsWithJitter();
        if (now - lastAttack < (long) delay) return;
        if (onlyVisible && !mc.player.canSee(target)) return;

        mc.player.swingHand(Hand.MAIN_HAND);
        mc.interactionManager.attackEntity(mc.player, target);
        lastAttack = now;
    }

    /**
     * Главная логика ротации в момент когда цель ЕСТЬ — порт SPAngle.limitAngleChange.
     */
    private float[] limitAngleChange(float[] targetAngles) {
        hadTargetLastTick = true;
        resetRelease();

        float yawDelta   = MathHelper.wrapDegrees(targetAngles[0] - currentYaw);
        float pitchDelta = MathHelper.wrapDegrees(targetAngles[1] - currentPitch);
        float totalDelta = (float) Math.hypot(yawDelta, pitchDelta);

        float yawLimit   = Math.min(Math.abs(yawDelta), 74f + randomBetween(0f, 1.0329834f));
        float pitchLimit = Math.min(Math.abs(pitchDelta), 32.334f);

        float newYaw = currentYaw, newPitch = currentPitch;

        if (totalDelta > EPSILON) {
            boolean pitchReached = Math.abs(pitchDelta) >= pitchLimit;
            float pitchMaxStep = pitchReached ? randomBetween(65f, 100f) : randomBetween(7.7f, 12.1f);
            float pitchStep  = Math.min(totalDelta, pitchMaxStep);
            float pitchScale = pitchStep / totalDelta;
            if (!pitchReached) pitchScale = ease(pitchScale);
            newPitch = MathHelper.clamp(currentPitch + pitchDelta * pitchScale, -89f, 90f);

            boolean yawReached = Math.abs(yawDelta) >= yawLimit;
            float yawMaxStep = yawReached ? randomBetween(65f, 100f) : randomBetween(7.7f, 12.1f);
            float yawStep  = Math.min(totalDelta, yawMaxStep);
            float yawScale = yawStep / totalDelta;
            if (!yawReached) yawScale = ease(yawScale);
            newYaw = currentYaw + yawDelta * yawScale;
        }

        // Sway (тряска во время атаки — имитация дрожания руки)
        newYaw += applyShake();

        return new float[]{newYaw, newPitch};
    }

    /**
     * Логика при ОТСУТСТВИИ цели — release hold + slowdown.
     * Порт SPAngle: при потере цели ротация замирает на 150мс,
     * потом плавно (ease) возвращается к естественному взгляду.
     */
    private float[] handleRelease(float[] naturalAngles, boolean hasTarget) {
        long now = System.currentTimeMillis();
        boolean lostTargetThisTick = hadTargetLastTick;

        if (lostTargetThisTick && !releaseHoldActive && !releaseSlowdownActive) {
            releaseHoldActive  = true;
            releaseHoldUntil   = now + RELEASE_HOLD_MS;
            releaseFromAngle   = new float[]{currentYaw, currentPitch};
            releaseToAngle     = naturalAngles.clone();
        }
        hadTargetLastTick = false;

        // Фаза 1: заморозка (hold)
        if (releaseHoldActive && now < releaseHoldUntil) {
            float[] frozen = releaseFromAngle != null ? releaseFromAngle : new float[]{currentYaw, currentPitch};
            return new float[]{frozen[0], frozen[1]};
        }

        // Переход hold → slowdown
        if (releaseHoldActive && !releaseSlowdownActive) {
            releaseSlowdownActive = true;
            releaseSlowdownStart  = now;
        }

        // Нет активной анимации release — просто следуем естественному взгляду
        if (!releaseSlowdownActive) {
            return naturalAngles;
        }

        // Фаза 2: плавный возврат (slowdown)
        float progress = MathHelper.clamp(
                (float)(now - releaseSlowdownStart) / RELEASE_SLOWDOWN_MS, 0f, 1f);
        float eased = ease(progress);

        float[] from = releaseFromAngle != null ? releaseFromAngle : new float[]{currentYaw, currentPitch};
        float[] to   = releaseToAngle   != null ? releaseToAngle   : naturalAngles;

        float interpYaw   = MathHelper.lerpAngleDegrees(eased, from[0], to[0]);
        float interpPitch = MathHelper.clamp(MathHelper.lerp(eased, from[1], to[1]), -89f, 90f);

        if (progress >= 1f) resetRelease();

        return new float[]{interpYaw, interpPitch};
    }

    private void resetRelease() {
        releaseHoldActive = false;
        releaseHoldUntil  = 0L;
        releaseSlowdownActive = false;
        releaseSlowdownStart  = 0L;
        releaseFromAngle = null;
        releaseToAngle   = null;
    }

    /** Тряска руки во время атаки (sin волна с гауссовым шумом) */
    private float applyShake() {
        float time      = (System.currentTimeMillis() % 12000L) / 1200.0f;
        float swayPhase = time * SHAKE_SPEED * (float)(Math.PI * 2);
        return (float)(Math.sin(swayPhase) * SHAKE_INTENSITY * gaussian());
    }

    private float ease(float t) { return t * (0.5f + 0.5f * t); }

    private float gaussian() {
        // Box-Muller приближение через Random.nextGaussian()
        return (float) rng.nextGaussian();
    }

    private float randomBetween(float min, float max) {
        return MathHelper.lerp(rng.nextFloat(), min, max);
    }

    /** GCD snap — обязателен для легитности ротаций */
    private float gcdSnap(float newVal, float oldVal, float sensitivity) {
        float f   = sensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 1.2f * 0.15f;
        float delta = Math.round((newVal - oldVal) / gcd) * gcd;
        return oldVal + delta;
    }

    private float cpsWithJitter() {
        float jitter = (float) rng.nextGaussian() * 1.5f;
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

    private Vec3d getAimPoint(PlayerEntity from, LivingEntity target) {
        Vec3d eye = from.getEyePos();
        Box bb    = target.getBoundingBox();
        double cx = MathHelper.clamp(eye.x, bb.minX, bb.maxX);
        double cy = MathHelper.clamp(eye.y, bb.minY, bb.maxY);
        double cz = MathHelper.clamp(eye.z, bb.minZ, bb.maxZ);
        return new Vec3d(cx, cy, cz);
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        LivingEntity closest = null;
        float minDist = range * range + 1f;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()) continue;
            if (le instanceof PlayerEntity p) {
                if (!players || p.isCreative()) continue;
            } else if (!mobs) continue;

            float dist = (float) e.squaredDistanceTo(mc.player);
            if (dist > range * range) continue;
            if (dist < minDist) { minDist = dist; closest = le; }
        }
        return closest;
    }
}

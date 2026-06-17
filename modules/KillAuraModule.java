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
 * KillAura — порт ротационной логики SPAngle (release hold/slowdown) +
 * человекоподобный тайминг атак (не бьёт максимально быстро, ждёт крит-окно).
 *
 * Крит в Minecraft происходит когда: fallDistance > 0 (игрок падает),
 * !onGround, не на лестнице/в воде, нет slow-falling эффекта.
 * Хороший КиллАура НЕ бьёт спамом — ждёт следующий "естественный" момент
 * атаки, рассчитанный из целевого CPS с гауссовым джиттером, и предпочитает
 * крит-окно если оно скоро наступит (прыжок перед хитом).
 */
public class KillAuraModule implements IModule {

    private boolean enabled = false;

    public float range         = 3.5f;
    public float cps           = 8f;     // целевой CPS (среднее)
    public float cpsVariance   = 1.5f;   // стандартное отклонение
    public boolean players     = true;
    public boolean mobs        = false;
    public boolean onlyVisible = true;
    public boolean critJump    = true;   // подскакивать перед хитом для крита

    private static final long  RELEASE_HOLD_MS     = 150L;
    private static final long  RELEASE_SLOWDOWN_MS = 350L;
    private static final float SHAKE_INTENSITY     = 1.15f;
    private static final float SHAKE_SPEED         = 3.0f;
    private static final float EPSILON             = 1.0f;
    private final Random rng = new Random();

    private float currentYaw, currentPitch;
    private LivingEntity target = null;

    private boolean releaseHoldActive = false;
    private long    releaseHoldUntil  = 0L;
    private boolean releaseSlowdownActive = false;
    private long    releaseSlowdownStart  = 0L;
    private float[] releaseFromAngle = null;
    private float[] releaseToAngle   = null;
    private boolean hadTargetLastTick = false;

    // ── Тайминг атак ──────────────────────────────────────────────
    private long  nextAttackTime  = 0L; // момент следующего запланированного клика
    private long  targetAcquiredAt = 0L; // когда цель появилась (для crit jump таймера)
    private boolean jumpQueued    = false;

    @Override public String getName()           { return "KillAura"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean v) {
        enabled = v;
        if (!v) { target = null; resetRelease(); nextAttackTime = 0L; }
    }

    @Override
    public List<ClickGUI.Setting> getSettings() {
        List<ClickGUI.Setting> list = new ArrayList<>();
        list.add(new ClickGUI.SliderSetting("Range",        range,       2.5f, 6.0f, v -> range       = v));
        list.add(new ClickGUI.SliderSetting("CPS",          cps,         3f,   14f,  v -> cps         = v));
        list.add(new ClickGUI.SliderSetting("CPS Variance", cpsVariance, 0.2f, 3.0f, v -> cpsVariance = v));
        list.add(new ClickGUI.BoolSetting("Players",      players,     v -> players     = v));
        list.add(new ClickGUI.BoolSetting("Mobs",         mobs,        v -> mobs        = v));
        list.add(new ClickGUI.BoolSetting("Only visible", onlyVisible, v -> onlyVisible = v));
        list.add(new ClickGUI.BoolSetting("Crit jump",    critJump,    v -> critJump    = v));
        return list;
    }

    public void tick() {
        if (!enabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        currentYaw   = mc.player.getYaw();
        currentPitch = mc.player.getPitch();

        LivingEntity prevTarget = target;
        target = findTarget(mc);
        boolean hasTarget = target != null;

        // Новая цель — планируем время первого хита и сбрасываем crit-jump таймер
        if (hasTarget && target != prevTarget) {
            targetAcquiredAt = System.currentTimeMillis();
            scheduleNextAttack(true); // первый хит может быть чуть быстрее (реакция)
            jumpQueued = critJump;
        }

        float[] result;
        if (hasTarget) {
            Vec3d aimPoint = getAimPoint(mc.player, target);
            float[] targetAngles = getAnglesTo(mc.player, aimPoint);
            result = limitAngleChange(targetAngles);
        } else {
            result = handleRelease(new float[]{currentYaw, currentPitch});
        }

        float sensitivity = mc.options.getMouseSensitivity().getValue().floatValue();
        float newYaw   = gcdSnap(result[0], currentYaw, sensitivity);
        float newPitch = gcdSnap(MathHelper.clamp(result[1], -90f, 90f), currentPitch, sensitivity);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);

        if (!hasTarget) { nextAttackTime = 0L; return; }
        if (onlyVisible && !mc.player.canSee(target)) return;

        attemptAttack(mc);
    }

    /**
     * Человекоподобный тайминг: атакуем только когда наступило nextAttackTime
     * (запланированное заранее с джиттером), а не спамим каждый тик.
     * Перед хитом — опциональный crit jump.
     */
    private void attemptAttack(MinecraftClient mc) {
        long now = System.currentTimeMillis();

        // Crit jump: если запланирован, прыгаем чуть раньше хита чтобы успеть упасть
        if (jumpQueued && critJump && mc.player.isOnGround()) {
            long timeUntilHit = nextAttackTime - now;
            // Прыгаем если до хита осталось 80-200мс — типичное окно для крита
            if (timeUntilHit > 0 && timeUntilHit < 220) {
                mc.player.jump();
                jumpQueued = false;
            }
        }

        if (now < nextAttackTime) return;

        mc.player.swingHand(Hand.MAIN_HAND);
        mc.interactionManager.attackEntity(mc.player, target);

        scheduleNextAttack(false);
        jumpQueued = critJump; // готовим следующий крит-прыжок
    }

    /** Планирует следующий клик с CPS + гауссовым джиттером (реалистичное распределение) */
    private void scheduleNextAttack(boolean firstHit) {
        float baseDelay = 1000f / cps;
        float jitter = (float) rng.nextGaussian() * (1000f / cps) * (cpsVariance / cps);
        float delay = Math.max(60f, baseDelay + jitter); // минимум 60мс между кликами

        // Первый хит после получения цели — чуть быстрее (имитация реакции игрока)
        if (firstHit) delay *= 0.6f;

        nextAttackTime = System.currentTimeMillis() + (long) delay;
    }

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

        newYaw += applyShake();
        return new float[]{newYaw, newPitch};
    }

    private float[] handleRelease(float[] naturalAngles) {
        long now = System.currentTimeMillis();
        boolean lostTargetThisTick = hadTargetLastTick;

        if (lostTargetThisTick && !releaseHoldActive && !releaseSlowdownActive) {
            releaseHoldActive = true;
            releaseHoldUntil  = now + RELEASE_HOLD_MS;
            releaseFromAngle  = new float[]{currentYaw, currentPitch};
            releaseToAngle    = naturalAngles.clone();
        }
        hadTargetLastTick = false;

        if (releaseHoldActive && now < releaseHoldUntil) {
            float[] frozen = releaseFromAngle != null ? releaseFromAngle : new float[]{currentYaw, currentPitch};
            return new float[]{frozen[0], frozen[1]};
        }

        if (releaseHoldActive && !releaseSlowdownActive) {
            releaseSlowdownActive = true;
            releaseSlowdownStart  = now;
        }

        if (!releaseSlowdownActive) return naturalAngles;

        float progress = MathHelper.clamp((float)(now - releaseSlowdownStart) / RELEASE_SLOWDOWN_MS, 0f, 1f);
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

    private float applyShake() {
        float time      = (System.currentTimeMillis() % 12000L) / 1200.0f;
        float swayPhase = time * SHAKE_SPEED * (float)(Math.PI * 2);
        return (float)(Math.sin(swayPhase) * SHAKE_INTENSITY * rng.nextGaussian());
    }

    private float ease(float t) { return t * (0.5f + 0.5f * t); }

    private float randomBetween(float min, float max) {
        return MathHelper.lerp(rng.nextFloat(), min, max);
    }

    private float gcdSnap(float newVal, float oldVal, float sensitivity) {
        float f   = sensitivity * 0.6f + 0.2f;
        float gcd = f * f * f * 1.2f * 0.15f;
        float delta = Math.round((newVal - oldVal) / gcd) * gcd;
        return oldVal + delta;
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

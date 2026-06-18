package com.yourcheat.gui;

/**
 * Плавная интерполяция числовых значений для HUD (FPS, ping, и т.д.)
 * Порт идеи RollingNumbersRenderer — число "подъезжает" к новому значению
 * вместо мгновенной смены.
 */
public class RollingNumber {

    private float value;
    private float target;
    private final float speed;

    public RollingNumber(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float t) { target = t; }

    public float get() {
        value += (target - value) * speed;
        if (Math.abs(target - value) < 0.05f) value = target;
        return value;
    }

    public int getInt() { return Math.round(get()); }
}


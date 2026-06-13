package com.yourcheat.gui;

public class AnimationUtil {

    private double value = 0;
    private double from = 0;
    private double to = 0;
    private double durationSeconds = 0;
    private long startTime = 0;
    private boolean alive = false;
    private Easing easing = Easing.LINEAR;

    public enum Easing {
        LINEAR, CUBIC_OUT, CUBIC_IN;

        public double apply(double t) {
            return switch (this) {
                case LINEAR -> t;
                case CUBIC_OUT -> 1 - Math.pow(1 - t, 3);
                case CUBIC_IN -> t * t * t;
            };
        }
    }

    public void set(double value) {
        this.value = value;
        this.to = value;
        this.alive = false;
    }

    public void run(double to, double durationSeconds, Easing easing) {
        this.from = this.value;
        this.to = to;
        this.durationSeconds = durationSeconds;
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
        this.alive = true;
    }

    public void update() {
        if (!alive) return;
        long elapsed = System.currentTimeMillis() - startTime;
        double t = Math.min(1.0, elapsed / (durationSeconds * 1000.0));
        double easedT = easing.apply(t);
        value = from + (to - from) * easedT;
        if (t >= 1.0) alive = false;
    }

    public float get() {
        return (float) value;
    }

    public double getToValue() {
        return to;
    }

    public boolean isAlive() {
        return alive;
    }
}


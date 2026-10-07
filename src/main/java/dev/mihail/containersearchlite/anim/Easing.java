package dev.mihail.containersearchlite.anim;

/**
 * Small, Minecraft-free animation helpers. Everything is a pure function of its arguments so it can be unit
 * tested; the callers supply the clock.
 */
public final class Easing {
    private Easing() {
    }

    public static float clamp01(float value) {
        return value < 0f ? 0f : Math.min(value, 1f);
    }

    public static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    /** Fast start, gentle landing. Input and output are in 0..1. */
    public static float easeOutCubic(float t) {
        float inv = 1f - clamp01(t);
        return 1f - inv * inv * inv;
    }

    /**
     * Moves {@code current} towards {@code target} like a spring that is critically damped by time, independent of
     * the frame rate: after {@code timeConstantMs} about 63% of the distance has been covered.
     *
     * @param dtMs milliseconds since the previous call
     */
    public static float approach(float current, float target, float dtMs, float timeConstantMs) {
        if (timeConstantMs <= 0f || dtMs <= 0f) {
            return dtMs <= 0f ? current : target;
        }
        float factor = 1f - (float) Math.exp(-dtMs / timeConstantMs);
        float next = current + (target - current) * factor;
        // Snap when visually finished so that animations really end and callers can stop redrawing.
        return Math.abs(target - next) < 0.005f ? target : next;
    }

    /** Smooth oscillation between 0 and 1 with the given period. */
    public static float pulse(long nowMs, float periodMs) {
        double phase = (nowMs % (long) periodMs) / (double) periodMs;
        return (float) (0.5 + 0.5 * Math.sin(phase * 2.0 * Math.PI));
    }

    /**
     * Progress of a delayed, eased reveal: 0 before {@code startMs + delayMs}, 1 after the duration has passed.
     */
    public static float reveal(long nowMs, long startMs, long delayMs, long durationMs) {
        if (durationMs <= 0) {
            return 1f;
        }
        long elapsed = nowMs - startMs - delayMs;
        if (elapsed <= 0) {
            return 0f;
        }
        return easeOutCubic(elapsed / (float) durationMs);
    }

    /** Linearly blends two {@code 0xRRGGBB} colours; {@code t} is clamped to 0..1. */
    public static int blendRgb(int from, int to, float t) {
        float c = clamp01(t);
        int r = Math.round(lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, c));
        int g = Math.round(lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, c));
        int b = Math.round(lerp(from & 0xFF, to & 0xFF, c));
        return (r << 16) | (g << 8) | b;
    }

    /** Builds an {@code 0xAARRGGBB} int from an alpha in 0..1 and an {@code 0xRRGGBB} colour. */
    public static int argb(float alpha, int rgb) {
        int a = Math.round(clamp01(alpha) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}

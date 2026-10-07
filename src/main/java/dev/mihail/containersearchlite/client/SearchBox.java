package dev.mihail.containersearchlite.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

import dev.mihail.containersearchlite.anim.Easing;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * The animated search field: a pill that rests as a small magnifier icon and smoothly widens when focused or
 * filled, with a glowing border while you type and a counter of how many slots match.
 *
 * <p>The text editing itself is vanilla's {@link EditBox} (cursor, selection, copy and paste, narration). This
 * subclass only replaces the frame around it and moves/resizes the inner field every frame to follow the
 * animation. All drawing uses plain filled rectangles, so there are no textures to ship.
 */
final class SearchBox extends EditBox {
    private static final int PILL_HEIGHT = 13;
    private static final int FIELD_HEIGHT = 9;
    private static final int COLLAPSED_WIDTH = 16;
    private static final int ICON_AREA = 14;
    private static final int COUNTER_AREA = 22;
    private static final int RIGHT_PADDING = 4;
    private static final int PILL_FILL = 0x14151A;
    private static final int IDLE_BORDER = 0x4A4D58;
    private static final int ICON_IDLE = 0xB8BCC8;
    private static final int NO_MATCH_RED = 0xE05555;

    /** Magnifier pixels, relative to the icon origin: a ring plus a diagonal handle. */
    private static final int[][] ICON_PIXELS = buildIcon();

    private final Font font;
    private final IntSupplier matchCount;

    private int pillRight;
    private int pillTop;
    private int fullWidth = 80;

    private float expand;
    private float appear;
    private float glow;
    private long lastFrameMs = -1;

    SearchBox(Font font, Component narration, IntSupplier matchCount) {
        super(font, 0, 0, COLLAPSED_WIDTH, PILL_HEIGHT, narration);
        this.font = font;
        this.matchCount = matchCount;
        // The pill is drawn by us. The vanilla frame would clash with it.
        setBordered(false);
        // Start collapsed or open depending on the initial state; the first frame settles it.
        this.expand = ClientConfig.COLLAPSE_WHEN_IDLE.getAsBoolean() ? 0f : 1f;
        this.appear = ClientConfig.ANIMATIONS.getAsBoolean() ? 0f : 1f;
    }

    /**
     * Sets where the pill sits. {@code right} is the fixed x of its right edge: the pill grows leftwards.
     */
    void place(int right, int top, int width) {
        this.pillRight = right;
        this.pillTop = top;
        this.fullWidth = Math.max(width, COLLAPSED_WIDTH);
        applyGeometry(currentPillWidth(), pillTop);
    }

    private int currentPillWidth() {
        return COLLAPSED_WIDTH + Math.round((fullWidth - COLLAPSED_WIDTH) * Easing.easeOutCubic(expand));
    }

    private boolean showsCounter() {
        return ClientConfig.SHOW_MATCH_COUNT.getAsBoolean() && !getValue().isEmpty();
    }

    /** Positions the inner text field inside the pill, or lets it cover the whole pill while collapsed. */
    private void applyGeometry(int pillWidth, int top) {
        int x = pillRight - pillWidth;
        int fieldWidth = pillWidth - ICON_AREA - (showsCounter() ? COUNTER_AREA : RIGHT_PADDING);
        if (expand > 0.02f && fieldWidth >= 10) {
            setX(x + ICON_AREA);
            setY(top + 2);
            setWidth(fieldWidth);
            setHeight(FIELD_HEIGHT);
        } else {
            // Collapsed: the whole pill is the click target for the magnifier.
            setX(x);
            setY(top);
            setWidth(pillWidth);
            setHeight(PILL_HEIGHT);
        }
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        float dt = lastFrameMs < 0 ? 16f : Math.min(100f, now - lastFrameMs);
        lastFrameMs = now;

        boolean animate = ClientConfig.ANIMATIONS.getAsBoolean();
        boolean wantOpen = !ClientConfig.COLLAPSE_WHEN_IDLE.getAsBoolean() || isFocused() || !getValue().isEmpty();
        float openTarget = wantOpen ? 1f : 0f;
        float glowTarget = isFocused() ? 1f : 0f;
        expand = animate ? Easing.approach(expand, openTarget, dt, 55f) : openTarget;
        appear = animate ? Easing.approach(appear, 1f, dt, 70f) : 1f;
        glow = animate ? Easing.approach(glow, glowTarget, dt, 90f) : glowTarget;

        int pillWidth = currentPillWidth();
        int slide = Math.round((1f - appear) * -5f);
        int top = pillTop + slide;
        applyGeometry(pillWidth, top);

        int accent = ClientConfig.highlightRgb();
        int left = pillRight - pillWidth;

        // Soft outer glow while focused, gently pulsing.
        if (glow > 0.01f) {
            float pulse = animate ? 0.65f + 0.35f * Easing.pulse(now, 1400f) : 1f;
            int glowColor = Easing.argb(glow * pulse * 0.45f * appear, accent);
            graphics.fill(left, top - 1, left + pillWidth, top, glowColor);
            graphics.fill(left, top + PILL_HEIGHT, left + pillWidth, top + PILL_HEIGHT + 1, glowColor);
            graphics.fill(left - 1, top, left, top + PILL_HEIGHT, glowColor);
            graphics.fill(left + pillWidth, top, left + pillWidth + 1, top + PILL_HEIGHT, glowColor);
        }

        int borderRgb = Easing.blendRgb(IDLE_BORDER, accent, glow);
        drawPill(graphics, left, top, pillWidth, PILL_HEIGHT, Easing.argb(0.94f * appear, PILL_FILL), Easing.argb(appear, borderRgb));
        drawIcon(graphics, left + 3, top + 2, Easing.argb(appear, Easing.blendRgb(ICON_IDLE, accent, glow)));

        // The text field is only drawn once it has meaningful room; during the first frames of the
        // expansion the pill alone is shown.
        if (expand > 0.02f && getWidth() >= 10) {
            super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        }

        if (showsCounter() && expand > 0.9f) {
            int count = matchCount.getAsInt();
            String text = Integer.toString(count);
            int color = Easing.argb(appear, count > 0 ? accent : NO_MATCH_RED);
            graphics.text(font, text, left + pillWidth - font.width(text) - 5, top + 3, color);
        }
    }

    /**
     * A rectangle with the four corner pixels cut off. Border and fill never overlap, so translucent colours
     * blend correctly against the screen behind them.
     */
    private static void drawPill(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x + 1, y, x + w - 1, y + 1, border);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, border);
        g.fill(x, y + 1, x + 1, y + h - 1, border);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, border);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private static void drawIcon(GuiGraphicsExtractor g, int x, int y, int color) {
        for (int[] pixel : ICON_PIXELS) {
            g.fill(x + pixel[0], y + pixel[1], x + pixel[0] + 1, y + pixel[1] + 1, color);
        }
    }

    private static int[][] buildIcon() {
        List<int[]> pixels = new ArrayList<>();
        double center = 3.5;
        for (int py = 0; py < 8; py++) {
            for (int px = 0; px < 8; px++) {
                double distance = Math.hypot(px - center, py - center);
                if (distance >= 2.6 && distance <= 3.9) {
                    pixels.add(new int[] { px, py });
                }
            }
        }
        // Handle: a diagonal going down and to the right from the ring.
        for (int i = 0; i < 3; i++) {
            pixels.add(new int[] { 6 + i, 6 + i });
            pixels.add(new int[] { 7 + i, 6 + i });
        }
        return pixels.toArray(new int[0][]);
    }
}

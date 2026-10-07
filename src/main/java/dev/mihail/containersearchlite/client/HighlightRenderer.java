package dev.mihail.containersearchlite.client;

import dev.mihail.containersearchlite.anim.Easing;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the search result overlay on top of the slots of an open container.
 *
 * <p>Called from the {@code ScreenEvent.Render.Foreground} hook, which fires after the slots and their items have
 * been drawn but before the carried item and the tooltip, so highlights sit above the items and never cover a
 * tooltip. Coordinates here are absolute screen coordinates (slot position plus the window origin).
 *
 * <p>Animation: the dimming eases in and out with the query, each match fades in with a small cascade by slot
 * index while its outline "snaps" inwards, and matches pulse gently afterwards.
 */
final class HighlightRenderer {
    private static final int SLOT_SIZE = 16;
    private static final long REVEAL_MS = 190;
    private static final long CASCADE_STEP_MS = 6;
    private static final long CASCADE_MAX_MS = 150;

    private HighlightRenderer() {
    }

    static void render(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, SearchSession session) {
        long now = System.currentTimeMillis();
        session.advance(now);

        int originX = screen.getLeftPos();
        int originY = screen.getTopPos();
        Slot hovered = screen.getHoveredSlot();

        boolean animate = ClientConfig.ANIMATIONS.getAsBoolean();
        boolean dim = ClientConfig.DIM_NON_MATCHING.getAsBoolean();
        boolean outline = ClientConfig.DRAW_OUTLINE.getAsBoolean();
        boolean fill = ClientConfig.DRAW_FILL.getAsBoolean();
        int rgb = ClientConfig.highlightRgb();

        float dimAlpha = ClientConfig.DIM_OPACITY.getAsInt() / 100f * Easing.easeOutCubic(session.dimProgress());
        int dimColor = Easing.argb(dimAlpha, 0x000000);
        float pulse = animate ? 0.8f + 0.2f * Easing.pulse(now, 1100f) : 1f;

        int matches = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (!SearchSession.isSearchable(slot)) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                session.markUnlit(slot.index);
                continue;
            }

            int x = originX + slot.x;
            int y = originY + slot.y;

            if (session.matches(slot)) {
                matches++;
                float reveal = 1f;
                if (animate) {
                    long since = session.litSince(slot.index, now);
                    long delay = Math.min(CASCADE_MAX_MS, slot.index * CASCADE_STEP_MS);
                    reveal = Easing.reveal(now, since, delay, REVEAL_MS);
                } else {
                    session.litSince(slot.index, now);
                }
                if (fill) {
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, Easing.argb(0.27f * reveal * pulse, rgb));
                }
                if (outline) {
                    // The frame starts a few pixels outside the slot and contracts onto it as it fades in.
                    int spread = Math.round((1f - reveal) * 3f);
                    drawOutline(graphics, x, y, spread, Easing.argb(reveal * (0.75f + 0.25f * pulse), rgb));
                }
            } else {
                session.markUnlit(slot.index);
                // Leave the hovered slot crisp so that the item under the cursor is always readable.
                if (dim && slot != hovered && dimAlpha > 0.004f) {
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, dimColor);
                }
            }
        }
        session.setMatchCount(matches);
    }

    /** A one pixel frame around the full 18x18 slot cell (the item sits 1px inside it), grown by {@code spread}. */
    private static void drawOutline(GuiGraphicsExtractor graphics, int x, int y, int spread, int color) {
        int left = x - 1 - spread;
        int top = y - 1 - spread;
        int right = x + SLOT_SIZE + 1 + spread;
        int bottom = y + SLOT_SIZE + 1 + spread;
        graphics.fill(left, top, right, top + 1, color);          // top edge
        graphics.fill(left, bottom - 1, right, bottom, color);    // bottom edge
        graphics.fill(left, top + 1, left + 1, bottom - 1, color); // left edge
        graphics.fill(right - 1, top + 1, right, bottom - 1, color); // right edge
    }
}

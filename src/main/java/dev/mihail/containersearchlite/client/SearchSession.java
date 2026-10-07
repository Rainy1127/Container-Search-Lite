package dev.mihail.containersearchlite.client;

import java.util.HashMap;
import java.util.Map;

import dev.mihail.containersearchlite.anim.Easing;
import dev.mihail.containersearchlite.search.SearchQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The search state attached to one open container screen: the text box, the parsed query, a per-slot cache of
 * match results and the animation state of the highlights. Created when the screen initialises, dropped when it
 * closes. Client thread only.
 */
final class SearchSession {
    private static final int BOX_HEIGHT = 13;
    private static final int MAX_QUERY_LENGTH = 48;

    /** Cached verdict for one slot: a one-item sample of what was tested, and whether it matched. */
    private record CachedMatch(ItemStack sample, boolean matches) {
    }

    private final AbstractContainerScreen<?> screen;
    private final SearchBox box;
    private final Map<Integer, CachedMatch> cache = new HashMap<>();
    /** When each currently matching slot started to match, for the highlight fade-in. Keyed by slot index. */
    private final Map<Integer, Long> litSince = new HashMap<>();
    private SearchQuery query = SearchQuery.EMPTY;

    private int matchCount;
    /** 0 = nothing dimmed, 1 = fully dimmed. Eases towards 1 while a query is active. */
    private float dimProgress;
    private long lastAdvanceMs = -1;

    SearchSession(AbstractContainerScreen<?> screen, String initialText) {
        this.screen = screen;

        Font font = Minecraft.getInstance().font;
        this.box = new SearchBox(font, Component.translatable("containersearchlite.search.title"), this::matchCount);
        this.box.setMaxLength(MAX_QUERY_LENGTH);
        this.box.setHint(Component.translatable("containersearchlite.search.hint"));
        this.box.setCanLoseFocus(true);
        // Set the responder last so that restoring the initial text below does not fire it half-constructed.
        this.box.setValue(initialText);
        this.query = SearchQuery.parse(initialText);
        this.dimProgress = this.query.isEmpty() ? 0f : 1f;
        this.box.setResponder(this::onTextChanged);
        layout();
    }

    AbstractContainerScreen<?> screen() {
        return screen;
    }

    EditBox box() {
        return box;
    }

    SearchQuery query() {
        return query;
    }

    boolean hasActiveQuery() {
        return !query.isEmpty();
    }

    int matchCount() {
        return matchCount;
    }

    void setMatchCount(int count) {
        this.matchCount = count;
    }

    /** True while the dimming is still fading out after the query was cleared, so the overlay must keep drawing. */
    boolean isFading() {
        return dimProgress > 0f;
    }

    float dimProgress() {
        return dimProgress;
    }

    /** Advances the dim animation. Called once per rendered frame. */
    void advance(long nowMs) {
        float dt = lastAdvanceMs < 0 ? 16f : Math.min(100f, nowMs - lastAdvanceMs);
        lastAdvanceMs = nowMs;
        float target = query.isEmpty() ? 0f : 1f;
        dimProgress = ClientConfig.ANIMATIONS.getAsBoolean() ? Easing.approach(dimProgress, target, dt, 70f) : target;
    }

    /** Millisecond timestamp at which a slot started matching, registering it now if it is new. */
    long litSince(int slotIndex, long nowMs) {
        return litSince.computeIfAbsent(slotIndex, index -> nowMs);
    }

    void markUnlit(int slotIndex) {
        litSince.remove(slotIndex);
    }

    private void onTextChanged(String text) {
        query = SearchQuery.parse(text);
        cache.clear();
        litSince.clear();
    }

    /** Places the box in the title row at the top right of the container, or above the window if the title is long. */
    private void layout() {
        Font font = Minecraft.getInstance().font;
        int left = screen.getLeftPos();
        int top = screen.getTopPos();
        int imageWidth = screen.getImageWidth();
        int preferred = ClientConfig.SEARCH_BOX_WIDTH.getAsInt();

        // Vanilla draws the title at x = 8; keep a gap after it and a 6px margin on the right.
        int titleWidth = font.width(screen.getTitle());
        int inlineSpace = imageWidth - 8 - titleWidth - 6 - 6;

        int width;
        int right;
        int y;
        if (inlineSpace >= 48) {
            width = Math.min(preferred, inlineSpace);
            right = left + imageWidth - 6;
            y = top + 2;
        } else if (top >= BOX_HEIGHT + 4) {
            // Not enough room next to the title: sit just above the window instead.
            width = Math.min(preferred, imageWidth - 12);
            right = left + imageWidth;
            y = top - BOX_HEIGHT - 2;
        } else {
            // No room anywhere, fall back to a squeezed inline box.
            width = Math.max(48, Math.min(preferred, inlineSpace));
            right = left + imageWidth - 6;
            y = top + 2;
        }

        box.place(right, y, width);
    }

    void focus() {
        box.setFocused(true);
        screen.setFocused(box);
    }

    void unfocus() {
        box.setFocused(false);
        screen.setFocused(null);
    }

    /** Whether this slot should be treated as a search match. Uses the cache unless the item in the slot changed. */
    boolean matches(Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || query.isEmpty()) {
            return false;
        }

        CachedMatch cached = cache.get(slot.index);
        if (cached != null && ItemStack.isSameItemSameComponents(cached.sample(), stack)) {
            return cached.matches();
        }

        boolean result = query.matches(new StackFacts(stack), ClientConfig.SEARCH_TOOLTIPS.getAsBoolean());
        cache.put(slot.index, new CachedMatch(stack.copyWithCount(1), result));
        return result;
    }

    /** Whether the slot is one the search should consider, honouring the player-inventory option. */
    static boolean isSearchable(Slot slot) {
        if (!slot.isActive()) {
            return false;
        }
        return ClientConfig.INCLUDE_PLAYER_INVENTORY.getAsBoolean() || !(slot.container instanceof Inventory);
    }

    /** Number of slots in the open menu that belong to the container itself rather than the player inventory. */
    static int containerSlotCount(AbstractContainerScreen<?> screen) {
        int count = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof Inventory)) {
                count++;
            }
        }
        return count;
    }
}

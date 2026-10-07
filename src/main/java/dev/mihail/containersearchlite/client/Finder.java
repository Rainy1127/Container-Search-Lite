package dev.mihail.containersearchlite.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import dev.mihail.containersearchlite.finder.Compass;
import dev.mihail.containersearchlite.search.SearchQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Looks through the remembered containers and highlights the nearest ones that hold what the player asked for.
 */
final class Finder {
    private Finder() {
    }

    static boolean isEnabled() {
        return ClientConfig.FINDER_ENABLED.getAsBoolean();
    }

    /** Finds every remembered container that holds the same item as {@code target}. */
    static void locateItem(ItemStack target, Screen origin) {
        if (target.isEmpty()) {
            return;
        }
        locate(stack -> ItemStack.isSameItem(stack, target), origin);
    }

    /** Finds every remembered container with at least one item matching a search-box query. */
    static void locateQuery(SearchQuery query, Screen origin) {
        if (query.isEmpty()) {
            return;
        }
        boolean tooltips = ClientConfig.SEARCH_TOOLTIPS.getAsBoolean();
        locate(stack -> query.matches(new StackFacts(stack), tooltips), origin);
    }

    static void tellNothingToLocate() {
        message(Component.translatable("containersearchlite.finder.nothing"));
    }

    private record Hit(RememberedContainer container, int count, double distanceSqr) {
    }

    private static void locate(Predicate<ItemStack> wanted, Screen origin) {
        if (!isEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        List<Hit> hits = new ArrayList<>();
        for (RememberedContainer container : ContainerMemory.snapshot(level.dimension())) {
            if (!ContainerMemory.stillExists(level, container)) {
                ContainerMemory.forget(level.dimension(), container.pos());
                continue;
            }
            int found = 0;
            for (ItemStack stack : container.contents()) {
                if (wanted.test(stack)) {
                    found += stack.getCount();
                }
            }
            if (found > 0) {
                hits.add(new Hit(container, found, container.pos().distSqr(player.blockPosition())));
            }
        }

        // Leave the screen first so that the result is visible straight away.
        if (origin != null && minecraft.screen == origin && ClientConfig.FINDER_CLOSE_SCREEN.getAsBoolean()) {
            minecraft.setScreen(null);
        }

        if (hits.isEmpty()) {
            FinderEffects.clear();
            message(Component.translatable("containersearchlite.finder.none"));
            return;
        }

        hits.sort(Comparator.comparingDouble(Hit::distanceSqr));
        if (hits.size() > ClientConfig.FINDER_MAX_RESULTS.getAsInt()) {
            hits = new ArrayList<>(hits.subList(0, ClientConfig.FINDER_MAX_RESULTS.getAsInt()));
        }

        long expires = System.currentTimeMillis() + ClientConfig.FINDER_DURATION_SECONDS.getAsInt() * 1000L;
        int total = 0;
        List<FinderEffects.Highlight> highlights = new ArrayList<>();
        for (Hit hit : hits) {
            total += hit.count();
            highlights.add(new FinderEffects.Highlight(hit.container().pos(), hit.container().partner(), expires));
        }
        FinderEffects.show(highlights);

        Hit nearest = hits.get(0);
        Compass direction = Compass.fromOffset(
                nearest.container().pos().getX() + 0.5 - player.getX(),
                nearest.container().pos().getZ() + 0.5 - player.getZ());
        message(Component.translatable("containersearchlite.finder.found", total, hits.size(),
                (int) Math.round(Math.sqrt(nearest.distanceSqr())), FinderEffects.directionName(direction)));
    }

    private static void message(Component text) {
        Minecraft.getInstance().gui.setOverlayMessage(text, false);
    }
}

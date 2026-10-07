package dev.mihail.containersearchlite.client;

import java.util.ArrayList;
import java.util.List;

import dev.mihail.containersearchlite.finder.Compass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * The visible part of the finder: a shimmering column of particles above each highlighted container, plus a
 * distance and direction hint above the hotbar. Particles are used instead of a world-space outline because they
 * need nothing but the stable particle API.
 */
final class FinderEffects {
    /** One highlighted container. {@code partner} is the other half of a double chest, or null. */
    record Highlight(BlockPos pos, BlockPos partner, long expiresAtMs) {
    }

    private static final List<Highlight> ACTIVE = new ArrayList<>();

    private FinderEffects() {
    }

    static void show(List<Highlight> highlights) {
        ACTIVE.clear();
        ACTIVE.addAll(highlights);
    }

    static void clear() {
        ACTIVE.clear();
    }

    /** Called once per client tick while a level is loaded. */
    static void tick(ClientLevel level, LocalPlayer player) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        ACTIVE.removeIf(highlight -> highlight.expiresAtMs() <= now);
        if (ACTIVE.isEmpty()) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime % 3 == 0) {
            for (Highlight highlight : ACTIVE) {
                spawnColumn(level, highlight);
            }
        }
        if (gameTime % 20 == 0 && ClientConfig.FINDER_DIRECTION_HINT.getAsBoolean() && Minecraft.getInstance().screen == null) {
            showHint(player);
        }
    }

    private static void spawnColumn(ClientLevel level, Highlight highlight) {
        BlockPos pos = highlight.pos();
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        if (highlight.partner() != null) {
            x = (pos.getX() + highlight.partner().getX()) / 2.0 + 0.5;
            z = (pos.getZ() + highlight.partner().getZ()) / 2.0 + 0.5;
        }
        double base = pos.getY() + 1.05;

        RandomSource random = level.getRandom();
        for (int i = 0; i < 8; i++) {
            double y = base + i * 0.45 + random.nextDouble() * 0.2;
            double dx = (random.nextDouble() - 0.5) * 0.3;
            double dz = (random.nextDouble() - 0.5) * 0.3;
            // "Always visible" particles are not culled at a distance, so the column can be seen from far away.
            level.addAlwaysVisibleParticle(ParticleTypes.END_ROD, x + dx, y, z + dz, 0.0, 0.01, 0.0);
        }
    }

    private static void showHint(LocalPlayer player) {
        Highlight nearest = null;
        double best = Double.MAX_VALUE;
        for (Highlight highlight : ACTIVE) {
            double distance = highlight.pos().distSqr(player.blockPosition());
            if (distance < best) {
                best = distance;
                nearest = highlight;
            }
        }
        if (nearest == null) {
            return;
        }

        Compass direction = Compass.fromOffset(nearest.pos().getX() + 0.5 - player.getX(), nearest.pos().getZ() + 0.5 - player.getZ());
        Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("containersearchlite.finder.hint",
                (int) Math.round(Math.sqrt(best)), directionName(direction), ACTIVE.size()), false);
    }

    static Component directionName(Compass direction) {
        return Component.translatable("containersearchlite.compass." + direction.key());
    }
}

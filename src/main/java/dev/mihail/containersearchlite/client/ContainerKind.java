package dev.mihail.containersearchlite.client;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;

/** The kinds of block containers the finder can remember, each switchable in the config. */
enum ContainerKind {
    CHEST,
    BARREL,
    SHULKER,
    OTHER;

    /**
     * Classifies a block entity, or returns {@code null} if it is not a container the finder could ever track.
     * Ender chests are deliberately not matched: their contents belong to the player, not to the block.
     */
    static ContainerKind of(BlockEntity entity) {
        if (entity instanceof ChestBlockEntity) {
            return CHEST;
        }
        if (entity instanceof BarrelBlockEntity) {
            return BARREL;
        }
        if (entity instanceof ShulkerBoxBlockEntity) {
            return SHULKER;
        }
        if (entity instanceof Container) {
            return OTHER;
        }
        return null;
    }

    boolean isEnabled() {
        return switch (this) {
            case CHEST -> ClientConfig.FINDER_TRACK_CHESTS.getAsBoolean();
            case BARREL -> ClientConfig.FINDER_TRACK_BARRELS.getAsBoolean();
            case SHULKER -> ClientConfig.FINDER_TRACK_SHULKERS.getAsBoolean();
            case OTHER -> ClientConfig.FINDER_TRACK_OTHER.getAsBoolean();
        };
    }
}

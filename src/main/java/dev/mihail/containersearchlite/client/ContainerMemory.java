package dev.mihail.containersearchlite.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * The client-side memory of containers the player has opened, per dimension.
 *
 * <p>This is the whole reason the finder is fair: it is filled exclusively from menus the player themselves
 * opened (what the server already sent them), so it can never reveal a container they have not looked into.
 * It lives only in RAM and is cleared when the player leaves the world.
 */
final class ContainerMemory {
    /** Upper bound per dimension; the oldest entries are dropped beyond it. */
    private static final int MAX_ENTRIES_PER_DIMENSION = 1024;

    private static final Map<ResourceKey<Level>, LinkedHashMap<BlockPos, RememberedContainer>> DATA = new HashMap<>();

    private ContainerMemory() {
    }

    /**
     * Stores the container slots of {@code menu} as the current contents of the container at {@code clicked}.
     * Does nothing if there is no trackable container there or its kind is switched off in the config.
     */
    static void remember(ClientLevel level, BlockPos clicked, AbstractContainerMenu menu) {
        ContainerKind kind = ContainerKind.of(level.getBlockEntity(clicked));
        if (kind == null || !kind.isEnabled()) {
            return;
        }

        BlockPos pos = clicked.immutable();
        BlockPos partner = partnerOf(level, pos);
        BlockPos key = pos;
        BlockPos other = partner;
        if (partner != null && partner.asLong() < pos.asLong()) {
            key = partner;
            other = pos;
        }

        List<ItemStack> contents = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                contents.add(stack.copy());
            }
        }

        LinkedHashMap<BlockPos, RememberedContainer> map = DATA.computeIfAbsent(level.dimension(), dimension -> new LinkedHashMap<>());
        if (other != null) {
            // Both halves of a double chest are one entry; drop anything stored under the other half.
            map.remove(other);
        }
        map.remove(key); // re-insert so that this entry becomes the newest
        map.put(key, new RememberedContainer(key, other, List.copyOf(contents)));

        Iterator<BlockPos> oldest = map.keySet().iterator();
        while (map.size() > MAX_ENTRIES_PER_DIMENSION && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }

    /** A copy of the remembered containers of one dimension, safe to iterate while the memory changes. */
    static List<RememberedContainer> snapshot(ResourceKey<Level> dimension) {
        Map<BlockPos, RememberedContainer> map = DATA.get(dimension);
        return map == null ? List.of() : new ArrayList<>(map.values());
    }

    static void forget(ResourceKey<Level> dimension, BlockPos pos) {
        Map<BlockPos, RememberedContainer> map = DATA.get(dimension);
        if (map != null) {
            map.remove(pos);
        }
    }

    static void clear() {
        DATA.clear();
    }

    /**
     * Whether the remembered container can still be there. Positions in chunks that are not loaded are given the
     * benefit of the doubt, because the client simply cannot tell.
     */
    static boolean stillExists(ClientLevel level, RememberedContainer container) {
        if (!level.isLoaded(container.pos())) {
            return true;
        }
        return ContainerKind.of(level.getBlockEntity(container.pos())) != null;
    }

    private static BlockPos partnerOf(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return pos.relative(ChestBlock.getConnectedDirection(state)).immutable();
        }
        return null;
    }
}

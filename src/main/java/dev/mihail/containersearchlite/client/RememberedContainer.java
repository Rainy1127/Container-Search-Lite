package dev.mihail.containersearchlite.client;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * What the player saw the last time they closed a container.
 *
 * @param pos the block position this entry is stored under (for a double chest, the half with the lower position)
 * @param partner the other half of a double chest, or {@code null} for every other container
 * @param contents copies of the non-empty stacks that were in the container, never mutated afterwards
 */
record RememberedContainer(BlockPos pos, BlockPos partner, List<ItemStack> contents) {
}

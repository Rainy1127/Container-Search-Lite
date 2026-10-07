package dev.mihail.containersearchlite.client;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.mihail.containersearchlite.search.ItemFacts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * {@link ItemFacts} backed by a real {@link ItemStack}. Every value is computed lazily and at most once, so a
 * plain-text query never pays for tooltip generation.
 */
final class StackFacts implements ItemFacts {
    /** Namespace to lower-case mod display name. Mod metadata never changes while the game runs. */
    private static final Map<String, String> MOD_NAMES = new HashMap<>();

    private final ItemStack stack;
    private final Identifier id;

    private String displayName;
    private String tooltipText;

    StackFacts(ItemStack stack) {
        this.stack = stack;
        this.id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    }

    @Override
    public String displayName() {
        if (displayName == null) {
            displayName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        }
        return displayName;
    }

    @Override
    public String idPath() {
        return id.getPath().replace('_', ' ');
    }

    @Override
    public String namespace() {
        return id.getNamespace();
    }

    @Override
    public String modName() {
        return MOD_NAMES.computeIfAbsent(id.getNamespace(), namespace -> ModList.get()
                .getModContainerById(namespace)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(namespace)
                .toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean hasTagContaining(String text) {
        return stack.typeHolder().tags().anyMatch(tag -> tag.location().toString().contains(text));
    }

    @Override
    public String tooltipText() {
        if (tooltipText == null) {
            List<Component> lines = Screen.getTooltipFromItem(Minecraft.getInstance(), stack);
            StringBuilder builder = new StringBuilder();
            for (Component line : lines) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(line.getString());
            }
            tooltipText = builder.toString().toLowerCase(Locale.ROOT);
        }
        return tooltipText;
    }
}

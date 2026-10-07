package dev.mihail.containersearchlite.search;

/**
 * The searchable facts about one item stack. This is the boundary between the pure search logic and Minecraft:
 * the client package provides an implementation backed by a real {@code ItemStack}, tests provide fakes.
 *
 * <p>Implementations should compute values lazily (tooltips in particular are expensive) and every returned
 * string must already be lower-case ({@link java.util.Locale#ROOT}).
 */
public interface ItemFacts {
    /** Lower-case display name, e.g. {@code "oak log"}. */
    String displayName();

    /** Lower-case registry path with underscores replaced by spaces, e.g. {@code "oak log"}. */
    String idPath();

    /** Lower-case registry namespace, e.g. {@code "minecraft"}. */
    String namespace();

    /** Lower-case display name of the owning mod, e.g. {@code "minecraft"} or {@code "create"}. */
    String modName();

    /** Whether the item is in any tag whose identifier contains {@code text} (lower-case). */
    boolean hasTagContaining(String text);

    /** All tooltip lines joined with {@code '\n'}, lower-case. */
    String tooltipText();
}

package dev.mihail.containersearchlite.client;

import java.util.List;
import java.util.regex.Pattern;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-only configuration ({@code config/containersearchlite-client.toml}).
 *
 * <p>This is registered as {@code ModConfig.Type.CLIENT}, so it is never created on a dedicated server and is
 * never synchronised over the network.
 */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final Pattern COLOR_PATTERN = Pattern.compile("^#?[0-9a-fA-F]{6}$");
    private static final int DEFAULT_COLOR = 0xFFD83A;

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch. When false the search box and the highlighting are not added to any screen.")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue MIN_CONTAINER_SLOTS = BUILDER
            .comment("Only add the search box to screens whose container (not counting the player inventory) has at least",
                    "this many slots. The default of 9 skips furnaces, anvils, hoppers and similar small screens.")
            .defineInRange("minContainerSlots", 9, 1, 256);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_MENUS = BUILDER
            .comment("Menu type ids that never get a search box, for example \"minecraft:crafting\".",
                    "Use this when another mod already provides its own search for a screen.")
            .defineListAllowEmpty("excludedMenus", List.of("minecraft:crafting", "minecraft:crafter"), () -> "minecraft:crafting",
                    ClientConfig::isValidIdentifier);

    public static final ModConfigSpec.BooleanValue INCLUDE_PLAYER_INVENTORY = BUILDER
            .comment("Also highlight matches in the player inventory slots shown below the container.")
            .define("includePlayerInventory", true);

    public static final ModConfigSpec.BooleanValue SEARCH_TOOLTIPS = BUILDER
            .comment("Allow $ terms (for example $sharpness) to search item tooltips such as enchantments and lore.",
                    "Tooltip text is computed once per distinct item and cached, but you can switch it off.")
            .define("searchTooltips", true);

    public static final ModConfigSpec.IntValue SEARCH_BOX_WIDTH = BUILDER
            .comment("Preferred width of the search box in pixels. It shrinks automatically on long container titles.")
            .defineInRange("searchBoxWidth", 80, 40, 200);

    public static final ModConfigSpec.BooleanValue AUTO_FOCUS = BUILDER
            .comment("Focus the search box as soon as a container opens, so you can type immediately.",
                    "While the box is focused, keys like E and the number keys type text instead of acting on the game.")
            .define("autoFocus", false);

    public static final ModConfigSpec.BooleanValue REMEMBER_QUERY = BUILDER
            .comment("Keep the last search text when you open the next container.")
            .define("rememberQuery", false);

    public static final ModConfigSpec.BooleanValue CTRL_F_FOCUSES = BUILDER
            .comment("Pressing Ctrl+F in a container focuses the search box.")
            .define("ctrlFFocusesSearch", true);

    public static final ModConfigSpec.BooleanValue RIGHT_CLICK_CLEARS = BUILDER
            .comment("Right-clicking the search box clears it.")
            .define("rightClickClears", true);

    public static final ModConfigSpec.BooleanValue DRAW_OUTLINE = BUILDER
            .comment("Draw a coloured outline around slots that match the search.")
            .define("drawOutline", true);

    public static final ModConfigSpec.BooleanValue DRAW_FILL = BUILDER
            .comment("Tint slots that match the search with a translucent colour.")
            .define("drawFill", true);

    public static final ModConfigSpec.ConfigValue<String> HIGHLIGHT_COLOR = BUILDER
            .comment("Highlight colour as RRGGBB hex, for example FFD83A (gold) or 55FF55 (green).")
            .define("highlightColor", "FFD83A", o -> o instanceof String s && COLOR_PATTERN.matcher(s).matches());

    public static final ModConfigSpec.BooleanValue DIM_NON_MATCHING = BUILDER
            .comment("Darken the items that do not match, so the matches stand out.")
            .define("dimNonMatching", true);

    public static final ModConfigSpec.IntValue DIM_OPACITY = BUILDER
            .comment("How strongly non-matching items are darkened, in percent.")
            .defineInRange("dimOpacityPercent", 65, 0, 100);

    public static final ModConfigSpec.BooleanValue ANIMATIONS = BUILDER
            .comment("Animate the search box and the highlights (slide-in, expanding box, fading dim, pulsing outline).",
                    "Turn this off for instant, static visuals.")
            .define("animations", true);

    public static final ModConfigSpec.BooleanValue COLLAPSE_WHEN_IDLE = BUILDER
            .comment("Show only a small magnifier icon until you click it, press Ctrl+F or type something.",
                    "When false the box is always shown in full.")
            .define("collapseWhenIdle", true);

    public static final ModConfigSpec.BooleanValue SHOW_MATCH_COUNT = BUILDER
            .comment("Show how many slots match inside the search box.")
            .define("showMatchCount", true);

    // ---- Container finder: remembers containers you opened and points you back to them ----

    public static final ModConfigSpec.BooleanValue FINDER_ENABLED = BUILDER
            .comment("Container finder: remember the contents of containers you open and, on a key press, show where an item is.",
                    "Only containers YOU have opened are ever remembered, so it cannot reveal anything you have not seen.",
                    "Set to false to keep only the in-GUI search (for example for packs that consider the finder too strong).",
                    "Note: this is a client setting, a player can change it. Pack makers can ship a default via the defaultconfigs folder.")
            .define("finderEnabled", true);

    public static final ModConfigSpec.BooleanValue FINDER_TRACK_CHESTS = BUILDER
            .comment("Remember chests and trapped chests.")
            .define("finderTrackChests", true);

    public static final ModConfigSpec.BooleanValue FINDER_TRACK_BARRELS = BUILDER
            .comment("Remember barrels.")
            .define("finderTrackBarrels", true);

    public static final ModConfigSpec.BooleanValue FINDER_TRACK_SHULKERS = BUILDER
            .comment("Remember placed shulker boxes.")
            .define("finderTrackShulkerBoxes", true);

    public static final ModConfigSpec.BooleanValue FINDER_TRACK_OTHER = BUILDER
            .comment("Remember every other block container (furnaces, hoppers, dispensers, modded storage that uses the standard container interface).",
                    "Set finderTrackChests=true and the rest to false for a chests-only finder.")
            .define("finderTrackOtherContainers", true);

    public static final ModConfigSpec.IntValue FINDER_MAX_RESULTS = BUILDER
            .comment("At most this many containers (the nearest ones) are highlighted per search.")
            .defineInRange("finderMaxResults", 6, 1, 32);

    public static final ModConfigSpec.IntValue FINDER_DURATION_SECONDS = BUILDER
            .comment("How long the highlight stays, in seconds.")
            .defineInRange("finderDurationSeconds", 12, 3, 120);

    public static final ModConfigSpec.BooleanValue FINDER_CLOSE_SCREEN = BUILDER
            .comment("Close the open screen when you locate an item from it, so you immediately see the result in the world.")
            .define("finderCloseScreen", true);

    public static final ModConfigSpec.BooleanValue FINDER_DIRECTION_HINT = BUILDER
            .comment("While containers are highlighted, show the distance and direction of the nearest one above the hotbar.")
            .define("finderDirectionHint", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    // Parsed colour cache: the colour is read every frame, so avoid re-parsing the string each time.
    private static String cachedColorText = "";
    private static int cachedColor = DEFAULT_COLOR;

    private ClientConfig() {
    }

    /** The configured highlight colour as {@code 0xRRGGBB}. Falls back to gold if the value is somehow invalid. */
    public static int highlightRgb() {
        String text = HIGHLIGHT_COLOR.get();
        if (!text.equals(cachedColorText)) {
            cachedColorText = text;
            try {
                cachedColor = Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16);
            } catch (NumberFormatException e) {
                cachedColor = DEFAULT_COLOR;
            }
        }
        return cachedColor;
    }

    private static boolean isValidIdentifier(Object value) {
        return value instanceof String text && Identifier.tryParse(text) != null;
    }
}

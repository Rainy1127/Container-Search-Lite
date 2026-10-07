package dev.mihail.containersearchlite;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Shared constants. This class is deliberately free of any client-only imports so that it is always safe to load,
 * on the physical client and on a dedicated server alike.
 *
 * <p>Package layout (the Client/Server split of this mod):
 * <ul>
 *     <li>{@code dev.mihail.containersearchlite} - constants only, side-neutral.</li>
 *     <li>{@code dev.mihail.containersearchlite.search} - pure Java query parsing and matching, no Minecraft imports,
 *     unit-testable.</li>
 *     <li>{@code dev.mihail.containersearchlite.client} - everything that touches screens, widgets or rendering.
 *     Only ever loaded through the {@code Dist.CLIENT} entrypoint.</li>
 * </ul>
 * There is intentionally no server-side code and no networking: the contents of an open container are already
 * synchronised to the client by vanilla, so a search needs nothing from the server.
 */
public final class ContainerSearchLite {
    /** Must match {@code modId} in neoforge.mods.toml and {@code mod_id} in gradle.properties. */
    public static final String MOD_ID = "containersearchlite";
    public static final Logger LOGGER = LogUtils.getLogger();

    private ContainerSearchLite() {
    }
}

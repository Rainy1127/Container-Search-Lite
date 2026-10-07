package dev.mihail.containersearchlite.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import dev.mihail.containersearchlite.ContainerSearchLite;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

/** The key binding of the container finder. Registered on the mod event bus by the client entrypoint. */
final class FinderKeys {
    static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath(ContainerSearchLite.MOD_ID, "main"));

    /** Locate the held or hovered item. Default K: free in vanilla, and it can be rebound in Controls. */
    static final KeyMapping LOCATE = new KeyMapping("key.containersearchlite.locate", GLFW.GLFW_KEY_K, CATEGORY);

    private FinderKeys() {
    }

    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(LOCATE);
    }

    /** Whether a key press inside a screen is the locate key. */
    static boolean isLocateKey(net.minecraft.client.input.KeyEvent keyEvent) {
        return LOCATE.isActiveAndMatches(InputConstants.getKey(keyEvent));
    }
}

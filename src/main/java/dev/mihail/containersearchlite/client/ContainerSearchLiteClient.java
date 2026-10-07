package dev.mihail.containersearchlite.client;

import dev.mihail.containersearchlite.ContainerSearchLite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The one and only entrypoint of the mod.
 *
 * <p>{@code dist = Dist.CLIENT} means FML never even loads this class on a dedicated server, so the mod is
 * inert there: no registries, no networking, no server-side state. That is the whole Client/Server contract.
 */
@Mod(value = ContainerSearchLite.MOD_ID, dist = Dist.CLIENT)
public final class ContainerSearchLiteClient {
    public ContainerSearchLiteClient(IEventBus modEventBus, ModContainer container) {
        // CLIENT configs are never created on a server and never synchronised.
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Adds the "Config" button to this mod's page in the Mods screen. Labels live in en_us.json.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // The key binding is registered on the mod bus ...
        modEventBus.addListener(FinderKeys::onRegisterKeyMappings);
        // ... while screen, interaction and tick events are fired on the game bus. Order matters for the two
        // screen key handlers: the search box must see the key first so that typing never triggers the finder.
        ScreenHooks.register(NeoForge.EVENT_BUS);
        FinderHooks.register(NeoForge.EVENT_BUS);

        ContainerSearchLite.LOGGER.info("Container Search Lite loaded (client only)");
    }
}

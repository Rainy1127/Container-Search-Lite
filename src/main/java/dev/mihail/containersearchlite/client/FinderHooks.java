package dev.mihail.containersearchlite.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Event wiring of the container finder.
 *
 * <p>How a container is remembered: the client is told "open this menu" without being told which block it
 * belongs to. So the right-click on the block is noted first, and when a container screen opens right
 * afterwards, that block is the one being looked into. When the screen is closed, what was inside is stored.
 */
final class FinderHooks {
    /** A right-click older than this many game ticks no longer counts as the cause of a screen opening. */
    private static final long MAX_CLICK_AGE_TICKS = 40;

    private static BlockPos pendingPos;
    private static long pendingTick;

    private static Screen trackedScreen;
    private static BlockPos trackedPos;

    private FinderHooks() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(FinderHooks::onRightClickBlock);
        gameBus.addListener(FinderHooks::onScreenInit);
        gameBus.addListener(FinderHooks::onScreenClosing);
        gameBus.addListener(FinderHooks::onScreenKey);
        gameBus.addListener(FinderHooks::onClientTick);
        gameBus.addListener(FinderHooks::onLoggingOut);
    }

    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || !Finder.isEnabled()) {
            return;
        }
        pendingPos = event.getPos().immutable();
        pendingTick = event.getLevel().getGameTime();
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen == trackedScreen) {
            return; // the same screen initialising again after a window resize
        }
        trackedScreen = null;
        trackedPos = null;

        ClientLevel level = Minecraft.getInstance().level;
        if (!Finder.isEnabled() || level == null || pendingPos == null || !(screen instanceof AbstractContainerScreen<?>)) {
            return;
        }
        if (level.getGameTime() - pendingTick > MAX_CLICK_AGE_TICKS) {
            pendingPos = null;
            return;
        }

        ContainerKind kind = ContainerKind.of(level.getBlockEntity(pendingPos));
        if (kind != null && kind.isEnabled()) {
            trackedScreen = screen;
            trackedPos = pendingPos;
        }
        pendingPos = null;
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        Screen screen = event.getScreen();
        ClientLevel level = Minecraft.getInstance().level;
        if (screen == trackedScreen && trackedPos != null && level != null && screen instanceof AbstractContainerScreen<?> containerScreen) {
            ContainerMemory.remember(level, trackedPos, containerScreen.getMenu());
        }
        if (screen == trackedScreen) {
            trackedScreen = null;
            trackedPos = null;
        }
    }

    /** The locate key inside a container screen: hovered item first, then the text in the search box. */
    private static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!Finder.isEnabled() || !(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !FinderKeys.isLocateKey(event.getKeyEvent())) {
            return;
        }

        Slot hovered = screen.getHoveredSlot();
        if (hovered != null && hovered.hasItem()) {
            Finder.locateItem(hovered.getItem(), screen);
            event.setCanceled(true);
            return;
        }

        SearchSession session = ScreenHooks.sessionFor(screen);
        if (session != null && session.hasActiveQuery()) {
            Finder.locateQuery(session.query(), screen);
            event.setCanceled(true);
        }
    }

    /** The locate key in the world: the item in the main hand, else the one in the off hand. */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        while (FinderKeys.LOCATE.consumeClick()) {
            if (minecraft.screen != null || !Finder.isEnabled()) {
                continue;
            }
            ItemStack held = player.getMainHandItem().isEmpty() ? player.getOffhandItem() : player.getMainHandItem();
            if (held.isEmpty()) {
                Finder.tellNothingToLocate();
            } else {
                Finder.locateItem(held, null);
            }
        }

        FinderEffects.tick(level, player);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ContainerMemory.clear();
        FinderEffects.clear();
        pendingPos = null;
        trackedScreen = null;
        trackedPos = null;
    }
}

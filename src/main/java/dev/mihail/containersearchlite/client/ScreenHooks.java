package dev.mihail.containersearchlite.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Glue between NeoForge screen events and the search session. All handlers run on the client thread and are only
 * ever registered by the {@code Dist.CLIENT} entrypoint, so nothing here can load on a dedicated server.
 */
final class ScreenHooks {
    /** The session of the container screen that is currently open, if it is one we attached to. */
    private static SearchSession session;
    /** The text of the last closed search, used when {@code rememberQuery} is enabled. */
    private static String lastQuery = "";

    private ScreenHooks() {
    }

    static void register(IEventBus gameBus) {
        gameBus.addListener(ScreenHooks::onScreenInit);
        gameBus.addListener(ScreenHooks::onRenderForeground);
        gameBus.addListener(ScreenHooks::onKeyPressed);
        gameBus.addListener(ScreenHooks::onMousePressed);
        gameBus.addListener(ScreenHooks::onScreenClosing);
    }

    /** The session for this screen, or null if we did not attach to it. */
    static SearchSession sessionFor(Screen screen) {
        return session != null && session.screen() == screen ? session : null;
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen) || !isSupported(containerScreen)) {
            session = null;
            return;
        }

        // Screen.init() runs again when the window is resized, with the same screen instance: carry the text
        // and the focus over instead of wiping what the player was typing.
        SearchSession previous = sessionFor(screen);
        String text;
        boolean focused;
        if (previous != null) {
            text = previous.box().getValue();
            focused = previous.box().isFocused();
        } else {
            text = ClientConfig.REMEMBER_QUERY.getAsBoolean() ? lastQuery : "";
            focused = ClientConfig.AUTO_FOCUS.getAsBoolean();
        }

        SearchSession created = new SearchSession(containerScreen, text);
        session = created;
        // Registers the box as a child of the screen: it is then rendered, narrated and receives clicks.
        event.addListener(created.box());
        if (focused) {
            created.focus();
        }
    }

    private static boolean isSupported(AbstractContainerScreen<?> screen) {
        if (!ClientConfig.ENABLED.getAsBoolean()) {
            return false;
        }
        // The creative inventory has its own search tab; adding a second box would be confusing.
        if (screen instanceof CreativeModeInventoryScreen) {
            return false;
        }
        if (SearchSession.containerSlotCount(screen) < ClientConfig.MIN_CONTAINER_SLOTS.getAsInt()) {
            return false;
        }
        return !isExcludedMenu(screen);
    }

    private static boolean isExcludedMenu(AbstractContainerScreen<?> screen) {
        Identifier menuId;
        try {
            menuId = BuiltInRegistries.MENU.getKey(screen.getMenu().getType());
        } catch (RuntimeException e) {
            // Menus created without a registered type throw from getType(); they cannot be on the exclude list.
            return false;
        }
        if (menuId == null) {
            return false;
        }
        String id = menuId.toString();
        return ClientConfig.EXCLUDED_MENUS.get().contains(id);
    }

    private static void onRenderForeground(ScreenEvent.Render.Foreground event) {
        Screen screen = event.getScreen();
        SearchSession active = sessionFor(screen);
        if (active == null || !(active.hasActiveQuery() || active.isFading())) {
            return;
        }
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            HighlightRenderer.render(event.getGuiGraphics(), containerScreen, active);
        }
    }

    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        SearchSession active = sessionFor(event.getScreen());
        if (active == null) {
            return;
        }

        EditBox box = active.box();
        int key = event.getKeyCode();

        if (box.isFocused()) {
            // While typing, nothing may reach the screen: otherwise E closes the container, digits swap hotbar
            // slots and Q drops the hovered item. Escape and Enter only leave the box instead of closing the GUI; Shift+Enter
            // searches the remembered containers.
            boolean enter = key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
            if (enter && event.getKeyEvent().hasShiftDown() && Finder.isEnabled() && active.hasActiveQuery()) {
                // Shift+Enter: look for the typed search in the containers remembered from earlier.
                Finder.locateQuery(active.query(), active.screen());
            } else if (key == GLFW.GLFW_KEY_ESCAPE || enter) {
                active.unfocus();
            } else {
                box.keyPressed(event.getKeyEvent());
            }
            event.setCanceled(true);
            return;
        }

        if (key == GLFW.GLFW_KEY_F && event.getKeyEvent().hasControlDown() && ClientConfig.CTRL_F_FOCUSES.getAsBoolean()) {
            active.focus();
            event.setCanceled(true);
        }
    }

    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        SearchSession active = sessionFor(event.getScreen());
        if (active == null || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT || !ClientConfig.RIGHT_CLICK_CLEARS.getAsBoolean()) {
            return;
        }
        EditBox box = active.box();
        if (box.isMouseOver(event.getMouseX(), event.getMouseY())) {
            box.setValue("");
            event.setCanceled(true);
        }
    }

    private static void onScreenClosing(ScreenEvent.Closing event) {
        SearchSession active = sessionFor(event.getScreen());
        if (active != null) {
            lastQuery = active.box().getValue();
            session = null;
        }
    }
}

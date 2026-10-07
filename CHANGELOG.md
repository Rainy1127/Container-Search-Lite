# Changelog

## 1.1.0

- **Animated search box.** At rest it is a small magnifier icon; it widens smoothly when you click it, press Ctrl+F or type, glows in the highlight colour while focused, and shows how many slots match. Matches fade in with a small cascade and a contracting outline, then pulse gently; the dimming eases in and out. All of it can be switched off (`animations`, `collapseWhenIdle`, `showMatchCount`).
- **Container finder.** The mod now remembers what was inside every container you open. Hold an item (or hover one in any container or inventory screen) and press **K**: the nearest remembered containers that hold it get a shimmering particle column, and a hint above the hotbar shows distance and direction. In a container, **Shift+Enter** in the search box finds the typed search in your remembered containers. Only containers you have opened yourself are remembered, so it never reveals anything you have not seen. Works on vanilla servers; nothing is sent to the server.
- **Balance options for modpack makers.** `finderEnabled` turns the finder off completely (the in-GUI search stays). `finderTrackChests`, `finderTrackBarrels`, `finderTrackShulkerBoxes` and `finderTrackOtherContainers` limit what is remembered, so "chests only" is possible. Also `finderMaxResults`, `finderDurationSeconds`, `finderCloseScreen` and `finderDirectionHint`.
- New key binding "Locate held or hovered item" (default K, rebindable in Controls).
- Russian and English translations for all new options.

## 1.0.0

First release.

- Search box in the title row of every container with 9 or more slots (chests, barrels, shulker boxes, ender chests, modded storage that uses a standard container screen).
- Matching slots get a coloured outline and tint; everything else is dimmed. The slot under the cursor always stays readable.
- Search syntax: plain text for names, `@mod`, `#tag`, `$tooltip`. Several terms combine with AND.
- Press **Ctrl+F** to focus the box; **Esc** or **Enter** leaves it without closing the container; **right-click** the box to clear it.
- While the box is focused, typing never triggers game keys (E, number keys, Q).
- Client-only config with a built-in config screen: colours, dimming, minimum slot count, excluded menu types, tooltip search, remember last search, auto-focus.
- Client-side only. No networking, nothing to install on the server.
- English and Russian translations.

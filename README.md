# Container Search Lite

A small, client-side quality-of-life mod for **NeoForge** (Minecraft **26.1.2**, Java 25).

Open a chest, type what you are looking for, and the matching slots light up while everything else is dimmed. Press one key with an item in your hand and the mod points you to the chests where you stored it.

## Using it

Open any container with a standard container screen and type into the box at the top right, or press **Ctrl+F**.

| You type | It matches |
|---|---|
| `oak` or `oak_log` | item name or registry path |
| `@create` | items from a mod (namespace or display name) |
| `#logs` or `#minecraft:planks` | items in a tag |
| `$sharpness` | text in the tooltip (enchantments, lore) |
| `@minecraft #logs` | several terms: all must match |

Keys: **Ctrl+F** focus, **Esc/Enter** leave the box without closing the GUI, **right-click** the box to clear it.

### Container finder

The mod remembers the contents of every container you close. Then:

- **In the world:** hold an item and press **K**.
- **In any container or inventory screen:** hover an item and press **K**, or press **Shift+Enter** inside the search box to look for the typed search.

The nearest remembered containers that hold the item get a column of particles above them, and the distance and direction are shown above the hotbar. The key can be rebound in Controls.

It only knows containers **you opened yourself** (it is built from what the server already sent you), so it cannot reveal what you have never seen. Memory is kept in RAM and cleared when you leave the world.

### For modpack makers

If you consider the finder too strong for your pack, switch it off. The in-GUI search is unaffected.

```toml
# config/containersearchlite-client.toml
finderEnabled = false          # no finder at all
# or a chests-only finder:
finderEnabled = true
finderTrackChests = true
finderTrackBarrels = false
finderTrackShulkerBoxes = false
finderTrackOtherContainers = false
```

This is a client setting: ship the file with your pack, but a player can edit their own copy. A mod that only runs on the client cannot enforce a rule on players. To keep the GUI search out of some screens use `excludedMenus` and `minContainerSlots`.

Everything is configurable in **Mods > Container Search Lite > Config** (`config/containersearchlite-client.toml`).

## Client / server split

The mod is strictly client-side.

- The only entrypoint is `@Mod(dist = Dist.CLIENT)`, so FML never loads any of the mod's classes on a dedicated server.
- There is no networking, no registry content and no server-side state. A container's contents are already synchronised to the client by vanilla, so searching them needs nothing from the server.
- The config is `ModConfig.Type.CLIENT`: never created on a server, never synchronised.
- The dependencies in `neoforge.mods.toml` are declared with `side="CLIENT"`.

Source layout:

```
dev.mihail.containersearchlite
  ContainerSearchLite        side-neutral constants only
  search/                    pure Java: query parsing and matching, no Minecraft imports, unit tested
  anim/                      pure Java: easing and colour helpers, unit tested
  finder/                    pure Java: compass bearings, unit tested
  client/                    everything that touches screens, widgets, rendering, particles and key bindings
```

## Building

Requirements: **JDK 25** (64-bit) and IntelliJ IDEA 2025.3 or newer.

```
./gradlew build           # jar ends up in build/libs/
./gradlew test            # unit tests for the search logic
./gradlew runClient       # start a dev client
./gradlew runServer       # start a dev dedicated server (accept eula.txt first)
```

The first Gradle sync downloads and decompiles Minecraft and can take a long time.

### Building for Minecraft 26.2

The code uses only APIs that exist unchanged in NeoForge 26.1.2 and 26.2, so the same sources can be built for 26.2 by overriding the version properties on the command line (no file edits):

```
./gradlew build -Pminecraft_version=26.2 "-Pminecraft_version_range=[26.2,26.3)" -Pneo_version=26.2.0.88 "-Pneo_version_range=[26.2.0.0,26.3)"
```

Check the NeoForge version number against the latest one before publishing.

## Testing checklist

See [docs/TESTING.md](../../Users/mindo/Downloads/container-search-lite-1.1.0-src/container-search-lite/docs/TESTING.md).

## License

MIT, see [LICENSE](LICENSE).

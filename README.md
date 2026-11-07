# IrisV

### Overview

IrisV combines an inventory catalogue, recipe browsing, favorites, tooltips and configurable HUD indicators to help players inspect items and prepare crafting without leaving the game interface.

First release: **v1 (1.0.0)**, targeting **Minecraft 1.21.1, NeoForge 21.1.248 and Java 21**. The target is defined in `gradle.properties`, regardless of the local directory name. Successful builds and automated tests do not guarantee every multiplayer or modded-machine workflow.

### Features

- Categorized item catalogue with pagination and search by name, mod or properties.
- Optional theme-aware highlighting of matching items in the player's inventory.
- Saved item and recipe favorites with dedicated search, filters and drag-and-drop ordering.
- Recipes and uses grouped by workstation, ingredient previews and return to the original screen.
- Ingredient transfer requests for supported workstations, subject to server validation; no automatic collection of crafting results.
- Dedicated creative-mode item-giving interactions.
- A compact pinned-recipe ingredient checklist during gameplay.
- Block, entity, item, fluid, inventory and tool-durability information.
- Themes, HUD positions and per-screen-category overlay switches.
- An in-game Wiki with searchable Catalogue, Search, Tooltips, HUD and Recipes sections.
- Integration diagnostics and settings import/export that preserves favorites.

See the in-game Wiki for interactions and shortcuts. Configurable keys appear in Minecraft's controls.

### Installation And Build

Use a Minecraft 1.21.1 instance with NeoForge 21.1.248 and Java 21. Build the project or obtain an authorized JAR, then place it in the instance's `mods` folder. Open **IrisV Options** from the main menu.

For multiplayer, plan to install IrisV on both client and server: the mod registers a network channel for tooltip data. It should not be treated as client-only.

Build from the repository root:

```powershell
.\gradlew.bat build
```

The wrapper supplies Gradle 9.2.1. The first run requires network access to download dependencies and Minecraft resources. Output: `build/libs/irisv-1.0.1.jar`; the filename follows `mod_id` and `mod_version`.

Use `runClient` or `runServer` for development, `test` for automated tests, and `runData` for data generation. On Linux/macOS, use `./gradlew` instead of `.\gradlew.bat`. Server administrators must accept Minecraft's EULA before running a server.

### Settings And Compatibility

Paths are relative to the game directory (`run/` for default development launches):

- `config/irisv/options.json`: settings, favorites and ordering.
- `config/irisv/irisv-settings.json`: settings exchange file.
- `config/irisv/options.before-import.json`: full pre-import backup, including private favorites.
- `logs/latest.log`: detailed logs and errors.

Open **Diagnostics & settings** from General options or the Wiki. Imports require confirmation and preserve favorites. Search text is not persisted; Minecraft key bindings are not exported. **Debug Chat** enables local action messages with duplicate throttling.

Supported vanilla screens depend on enabled categories. Recipes exposed through Minecraft's recipe manager can use the generic preview, but custom machines may require an explicit adapter. **No external adapter is currently bundled or verified.** The diagnostic reports registered integration states, not universal mod compatibility. Pinning supports one recipe for the current session.

Technical documentation: [Compatibility API](docs/mod-compatibility.md) · [Wiki, diagnostics and settings exchange](docs/settings-exchange.md).

### Repository Layout

```text
src/main/java/net/opal/irisv/   Mod code, UI, recipes, networking and compatibility API
src/main/resources/           Textures, translations and mixin configuration
src/main/templates/           Generated mod metadata template
src/test/java/                Automated tests
docs/                         Technical documentation
gradle/                       Gradle wrapper
build.gradle                  Build tasks and dependencies
gradle.properties             Minecraft, NeoForge and mod versions
```

### License

Proprietary project, copyright © 2026 itsnelys. Copying, modification and redistribution require prior written permission. See [LICENSE](LICENSE) for full terms. Third-party materials retain their own rights and licenses; the project template license is preserved in [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).

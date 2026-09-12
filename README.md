# IrisV

Catalogue d'objets, recettes, favoris et informations en jeu pour Minecraft.

**Minecraft 1.21.1 · NeoForge 21.1.248 · Java 21 · 1.0.0**

[Français](#français) | [English](#english)

## Français

### Présentation

IrisV rassemble les outils nécessaires pour consulter les objets, comprendre leurs recettes et préparer une fabrication sans quitter l'interface du jeu. Le mod associe un catalogue d'inventaire à des favoris, des tooltips et des indicateurs HUD configurables.

Première release : **v1 (1.0.0)**. La compilation et les tests automatisés ne remplacent pas les essais en jeu, notamment en multijoueur et avec des machines ajoutées par d'autres mods.

> La version cible est définie dans `gradle.properties`, indépendamment du nom du dossier local.

### Fonctionnalités

- **Catalogue** : objets classés par catégories, pagination et recherche par nom, mod ou propriétés.
- **Recherche d'inventaire** : surlignage optionnel des objets correspondants dans l'inventaire du joueur, aux couleurs du thème.
- **Favoris** : objets et recettes enregistrés, recherche dédiée, filtres et classement par glisser-déposer.
- **Recettes et utilisations** : navigation par poste de fabrication, aperçu des ingrédients et retour à l'interface d'origine.
- **Transfert d'ingrédients** : demande de placement dans les postes pris en charge, sans récupérer automatiquement le résultat. La validation reste côté serveur.
- **Mode créatif** : ajout d'objets à l'inventaire via les interactions dédiées.
- **Recette épinglée** : liste compacte des ingrédients possédés et nécessaires pendant le jeu.
- **Tooltips et HUD** : informations sur les blocs, entités, objets, fluides, inventaires et durabilité des outils.
- **Personnalisation** : thèmes, positions et activation des overlays par catégorie d'écran.
- **Wiki intégré** : rubriques Catalogue, Recherche, Tooltips, HUD et Recettes, avec recherche interne.
- **Diagnostic et réglages** : état des intégrations et export/import des options sans écraser les favoris.

Les interactions et raccourcis sont documentés dans le Wiki en jeu. Les touches configurables se trouvent dans les contrôles Minecraft.

### Installation En Jeu

1. Préparer une instance **Minecraft 1.21.1 avec NeoForge 21.1.248** et Java 21.
2. Compiler le projet ou utiliser un JAR obtenu auprès d'une source autorisée.
3. Placer `irisv-1.0.0.jar` dans le dossier `mods` de l'instance.
4. Lancer le jeu et ouvrir **IrisV Options** depuis le menu principal.

Pour une installation multijoueur, prévoir IrisV sur le client et le serveur : le mod déclare un canal réseau pour les données des tooltips. Ne pas le considérer comme un mod exclusivement client.

### Développement

Le wrapper fournit Gradle **9.2.1** ; aucune installation globale de Gradle n'est nécessaire. Le premier lancement nécessite un accès réseau pour télécharger les dépendances et ressources Minecraft.

Depuis la racine du dépôt, dans PowerShell :

```powershell
.\gradlew.bat build
```

Le build compile le code et exécute les tests. Le JAR est produit dans `build/libs/irisv-1.0.0.jar` ; son nom suit `mod_id` et `mod_version` dans `gradle.properties`.

| Commande PowerShell | Usage |
| --- | --- |
| `.\gradlew.bat runClient` | Lancer le client de développement |
| `.\gradlew.bat runServer` | Lancer le serveur de développement, sans interface graphique |
| `.\gradlew.bat test` | Exécuter les tests automatisés |
| `.\gradlew.bat build` | Compiler, tester et produire le JAR |
| `.\gradlew.bat runData` | Exécuter la génération de données |

Sous Linux/macOS, utiliser `./gradlew` à la place de `.\gradlew.bat`. Le lancement d'un serveur nécessite l'acceptation de l'EULA Minecraft par son administrateur.

### Configuration Et Logs

Les chemins suivants sont relatifs au dossier de l'instance Minecraft, soit `run/` pour les lancements de développement par défaut.

| Fichier | Contenu |
| --- | --- |
| `config/irisv/options.json` | Options, favoris et classement |
| `config/irisv/irisv-settings.json` | Fichier d'échange des réglages |
| `config/irisv/options.before-import.json` | Sauvegarde complète créée avant un import |
| `logs/latest.log` | Journal du jeu et erreurs détaillées |

**Diagnostic et réglages** est accessible depuis les options générales et le Wiki. L'import demande confirmation et conserve les favoris. Le fichier de sauvegarde, contrairement au fichier d'export, contient les favoris personnels. Les textes de recherche ne sont pas sauvegardés et les touches Minecraft ne sont pas incluses dans l'export.

L'option **Debug Chat** active les messages locaux d'IrisV, avec limitation des doublons. Les erreurs techniques détaillées restent dans `latest.log`.

### Compatibilité Et Limites

- Les interfaces vanilla prises en charge comprennent l'inventaire du joueur et les contenants au format standard, selon les catégories activées.
- Les recettes exposées par le gestionnaire de recettes Minecraft peuvent utiliser l'aperçu générique. Cela ne garantit pas la gestion de toutes les machines moddés.
- Une API permet d'enregistrer des adaptateurs pour les écrans et transferts spécifiques. **Aucun adaptateur externe n'est actuellement fourni ni validé.**
- Le diagnostic distingue les intégrations disponibles, désactivées et suspendues après une erreur. « Disponible » ne garantit pas toutes les recettes d'un mod.
- La recette épinglée est limitée à une recette et à la session courante.

Documentation technique : [API de compatibilité](docs/mod-compatibility.md) · [Wiki, diagnostic et échange de réglages](docs/settings-exchange.md).

### Licence

Projet propriétaire, copyright © 2026 itsnelys. Aucune autorisation de copie, modification ou redistribution n'est accordée sans permission écrite. Consulter [LICENSE](LICENSE) pour les conditions complètes. Les éléments tiers restent soumis à leurs propres droits et licences ; la licence du modèle de projet est conservée dans [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt).

## English

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

The wrapper supplies Gradle 9.2.1. The first run requires network access to download dependencies and Minecraft resources. Output: `build/libs/irisv-1.0.0.jar`; the filename follows `mod_id` and `mod_version`.

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

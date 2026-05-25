# IrisV / Minecraft 1.21.4 DevKit

## FR

Mod NeoForge pour *Minecraft 1.21.4* qui ajoute des overlays, tooltips avances et indicateurs HUD pour afficher des informations utiles sur les blocs, entites, objets, fluides et inventaires.

Le projet sert de base de developpement pour `irisv`, avec configuration JSON, providers extensibles, rendu client et synchronisation serveur/client.

### Installation

1. Ouvre le dossier `irisv1.21.4`.
2. Assure-toi d'utiliser Java 21.
3. Lance l'environnement avec Gradle.
4. Utilise la configuration client ou serveur selon le test voulu.
5. Modifie les options dans `config/irisv/options.json` apres le premier lancement.

### Informations projet

| Element | Valeur |
| --- | --- |
| Mod id | `irisv` |
| Nom | `Irisv` |
| Minecraft | `1.21.4` |
| NeoForge | `21.4.156` |
| Java | `21` |
| Package | `net.opal.irisv` |
| Version Gradle | `mod_version=1.0.0` |
| Version affichee en jeu | `IrisV 0.0.1 - Alpha` |

### Commandes Gradle

| Commande | Action |
| --- | --- |
| `gradlew runClient` | Lance un client de test. |
| `gradlew runServer` | Lance un serveur de test sans interface. |
| `gradlew runData` | Lance la generation de data. |
| `gradlew build` | Compile le mod. |

### Fonctionnalites

| Fonction | Description |
| --- | --- |
| Tooltips blocs | Affiche des informations sur le bloc vise. |
| Tooltips entites | Affiche des informations sur certaines entites visees. |
| Tooltips objets au sol | Regroupe et affiche les stacks proches. |
| Inventaires | Affiche un apercu des contenus quand disponible. |
| Fluides | Affiche des informations de stockage/capacite. |
| Indicateurs HUD | Affiche des indicateurs comme armure ou fleches. |
| Menus IrisV | Ajoute des options et overlays de menu. |
| Sync reseau | Synchronise les donnees serveur utiles au client. |

### Structure

| Dossier / Fichier | Role |
| --- | --- |
| `src/main/java/net/opal/irisv` | Code principal du mod. |
| `tooltips` | Systeme de tooltips et donnees affichees. |
| `tooltips/providers` | Providers blocs, entites, fluides et cas specifiques. |
| `tooltips/overlay` | Rendu des overlays de tooltips. |
| `indicators` | Indicateurs HUD. |
| `menu` | Menus et overlays de menu. |
| `network` | Payloads et synchronisation client/serveur. |
| `option` | Chargement et sauvegarde des options JSON. |
| `api` | Interfaces pour providers et accessors. |
| `src/main/resources` | Langues, textures, mixins et assets. |
| `src/main/templates` | Metadata NeoForge generee au build. |

### Options

Le fichier de configuration est cree au premier lancement :

```text
config/irisv/options.json
```

| Option | Valeur par defaut | Description |
| --- | --- | --- |
| `enableDebugChat` | `false` | Active les messages debug en chat. |
| `enableBlockTooltipOverlay` | `false` | Active l'overlay de tooltip bloc. |
| `advancedTooltips` | `true` | Active les tooltips avances. |
| `advancedLiquidStats` | `true` | Active les infos avancees de fluides. |
| `enableIndicators` | `true` | Active les indicateurs HUD. |
| `indicatorPosition` | `2` | Position des indicateurs, gauche/droite. |
| `enableEntityTooltip` | `true` | Active les tooltips d'entites. |
| `themeIndex` | `0` | Theme UI selectionne. |
| `tooltipPosition` | `0` | Position du tooltip. |
| `compactMode` | `false` | Mode compact sans espaces. |

### Providers inclus

| Type | Exemples |
| --- | --- |
| Blocs generiques | Inventaires, redstone, enchantements, panneaux, command blocks. |
| Blocs specifiques | Four, alambic, ruche, composteur, jukebox, pupitre, hopper. |
| Entites | Armor stand, item frame/decoration, end crystal, entites vivantes. |
| Fluides | Stockage, capacite et fluides generiques. |
| Outils requis | Detection et affichage des outils utiles. |
| Modded | Support dedie pour certains comportements custom. |

### Assets

| Asset | Role |
| --- | --- |
| `assets/irisv/textures/gui/arrow_empty.png` | Indicateur fleche vide. |
| `assets/irisv/textures/gui/arrow_full.png` | Indicateur fleche pleine. |
| `assets/irisv/lang/en_us.json` | Textes anglais du menu et du debug. |
| `irisv.mixins.json` | Configuration mixins du mod. |

### Depannage

| Probleme | Solution |
| --- | --- |
| Le mod ne se lance pas | Verifie Java 21, Minecraft 1.21.4 et NeoForge 21.4.156. |
| L'overlay bloc ne s'affiche pas | Active `enableBlockTooltipOverlay` dans `options.json`. |
| Les indicateurs HUD n'apparaissent pas | Verifie `enableIndicators`. |
| Les donnees d'inventaire sont vides | Verifie la synchro serveur/client et les providers. |
| Les textes affichent mal les caracteres | Verifie `en_us.json` et l'encodage UTF-8. |

### Note de patch 0.1

- Premiere version documentee.
- Base NeoForge pour Minecraft 1.21.4.
- Tooltips avances pour blocs, entites, objets, inventaires et fluides.
- Indicateurs HUD et menus IrisV.
- Configuration JSON et synchronisation reseau.

## EN

NeoForge mod for *Minecraft 1.21.4* that adds overlays, advanced tooltips, and HUD indicators to display useful information about blocks, entities, items, fluids, and inventories.

The project is a development base for `irisv`, with JSON configuration, extensible providers, client rendering, and server/client synchronization.

### Installation

1. Open the `irisv1.21.4` folder.
2. Make sure Java 21 is installed.
3. Start the development environment with Gradle.
4. Use the client or server run configuration depending on the test.
5. Edit options in `config/irisv/options.json` after the first launch.

### Project Information

| Element | Value |
| --- | --- |
| Mod id | `irisv` |
| Name | `Irisv` |
| Minecraft | `1.21.4` |
| NeoForge | `21.4.156` |
| Java | `21` |
| Package | `net.opal.irisv` |
| Gradle version | `mod_version=1.0.0` |
| In-game displayed version | `IrisV 0.0.1 - Alpha` |

### Gradle Commands

| Command | Action |
| --- | --- |
| `gradlew runClient` | Starts a test client. |
| `gradlew runServer` | Starts a headless test server. |
| `gradlew runData` | Runs data generation. |
| `gradlew build` | Builds the mod. |

### Features

| Feature | Description |
| --- | --- |
| Block tooltips | Displays information about the targeted block. |
| Entity tooltips | Displays information about supported targeted entities. |
| Ground item tooltips | Groups and displays nearby item stacks. |
| Inventories | Shows content previews when available. |
| Fluids | Displays storage and capacity information. |
| HUD indicators | Shows indicators such as armor or arrows. |
| IrisV menus | Adds options and menu overlays. |
| Network sync | Synchronizes useful server data to the client. |

### Structure

| Folder / File | Role |
| --- | --- |
| `src/main/java/net/opal/irisv` | Main mod source code. |
| `tooltips` | Tooltip system and displayed data. |
| `tooltips/providers` | Block, entity, fluid, and specific-case providers. |
| `tooltips/overlay` | Tooltip overlay rendering. |
| `indicators` | HUD indicators. |
| `menu` | Menus and menu overlays. |
| `network` | Payloads and client/server synchronization. |
| `option` | JSON option loading and saving. |
| `api` | Interfaces for providers and accessors. |
| `src/main/resources` | Language files, textures, mixins, and assets. |
| `src/main/templates` | NeoForge metadata generated during build. |

### Options

The configuration file is created on first launch:

```text
config/irisv/options.json
```

| Option | Default Value | Description |
| --- | --- | --- |
| `enableDebugChat` | `false` | Enables debug chat messages. |
| `enableBlockTooltipOverlay` | `false` | Enables the block tooltip overlay. |
| `advancedTooltips` | `true` | Enables advanced tooltips. |
| `advancedLiquidStats` | `true` | Enables advanced fluid information. |
| `enableIndicators` | `true` | Enables HUD indicators. |
| `indicatorPosition` | `2` | Indicator position, left/right. |
| `enableEntityTooltip` | `true` | Enables entity tooltips. |
| `themeIndex` | `0` | Selected UI theme. |
| `tooltipPosition` | `0` | Tooltip position. |
| `compactMode` | `false` | Compact mode without spacing. |

### Included Providers

| Type | Examples |
| --- | --- |
| Generic blocks | Inventories, redstone, enchantments, signs, command blocks. |
| Specific blocks | Furnace, brewing stand, beehive, composter, jukebox, lectern, hopper. |
| Entities | Armor stand, item frame/decoration, end crystal, living entities. |
| Fluids | Storage, capacity, and generic fluids. |
| Required tools | Detection and display of useful tools. |
| Modded | Dedicated support for some custom behaviors. |

### Assets

| Asset | Role |
| --- | --- |
| `assets/irisv/textures/gui/arrow_empty.png` | Empty arrow indicator. |
| `assets/irisv/textures/gui/arrow_full.png` | Full arrow indicator. |
| `assets/irisv/lang/en_us.json` | English menu and debug text. |
| `irisv.mixins.json` | Mod mixin configuration. |

### Troubleshooting

| Problem | Fix |
| --- | --- |
| Mod does not start | Check Java 21, Minecraft 1.21.4, and NeoForge 21.4.156. |
| Block overlay does not appear | Enable `enableBlockTooltipOverlay` in `options.json`. |
| HUD indicators do not appear | Check `enableIndicators`. |
| Inventory data is empty | Check server/client sync and providers. |
| Text displays broken characters | Check `en_us.json` and UTF-8 encoding. |

### Patch Note 0.1

- First documented release.
- NeoForge base for Minecraft 1.21.4.
- Advanced tooltips for blocks, entities, items, inventories, and fluids.
- HUD indicators and IrisV menus.
- JSON configuration and network synchronization.

---

## License & Author

Provided by **soraanoir/sundae https://github.com/soraanoir**.  
Free to use and modify, provided that credit is given to the author.

# Container Search Highlighter v2.2 — Fabric Mod for Minecraft 26.3

[![Minecraft 26.3](https://img.shields.io/badge/Minecraft-26.3-brightgreen.svg)](https://minecraft.net/)
[![Fabric API](https://img.shields.io/badge/Fabric-0.163.0+26.3-blue.svg)](https://fabricmc.net/)
[![Java 21](https://img.shields.io/badge/Java-21+-orange.svg)](https://adoptium.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Container Search Highlighter** is an intelligent, client-side utility mod for **Minecraft 26.3** on the **Fabric** mod loader. It caches the contents of opened containers, provides an intuitive in-game item selection GUI with icons, and highlights matching containers through walls using neon ESP boxes with zero Z-fighting.

**Developed by:** [LymXT](https://github.com/LymXT)

---

## ✨ Features

### 1. In-Game Item Search GUI (Keybind: `H`)
* **Interactive Visual Browser:** Displays a scrollable list of all Minecraft items with rendered item icons and localized tooltips/names.
* **Instant Filter Bar:** Type any part of an item name or ID (e.g., `diamond`, `golden_apple`) for real-time filtering without lag.
* **One-Click Selection:** Left-click any item to instantly select it, close the GUI, and illuminate all nearby containers storing that item.
* **Quick Dismiss:** Press **`ESC`** or click **"Clear Highlight"** to instantly reset the search and turn off all outlines.

### 2. Persistent Disk Cache (JSON)
* **Automatic Slot Interception:** Automatically scans container slots when opened (regular chests, double chests, ender chests, shulker boxes, barrels, hoppers, dispensers, droppers, and chest minecarts).
* **Multi-World & Server Isolation:** Saves cache files to `config/containersearch/cache_<world_id>.json`, separating singleplayer worlds (`sp_<worldName>`) and multiplayer servers (`mp_<serverIp>`).
* **Immediate Highlighting on Join:** Loads existing cached data upon entering a world or server — search and highlight items immediately without reopening containers.
* **Asynchronous & Thread-Safe I/O:** Disk writes are dispatched on background threads with atomic temp file replacement (`.tmp` -> `.json`), guaranteeing zero frame drops and safe crash resilience.
* **Auto-Cleanup on Block Break:** Automatically removes cached coordinates when a container block is destroyed.

### 3. Anti-Z-Fighting ESP Rendering & ChestESP Compatibility (v2.2)
* **Through-Wall Visibility:** Uses Minecraft 26.3's native Gizmos API (`setAlwaysOnTop()`) within a 64-block radius.
* **Hitbox Expansion (+0.02 Expand):** Automatically expands container bounding boxes by `+0.02` blocks in all directions (`box.inflate(0.02)`), completely eliminating Z-fighting, surface shimmering, and edge clipping when running alongside [ChestESP](https://github.com/LymXT/ChestESP).
* **Vivid Neon Accent:** Distinctive `3.2px` neon green wireframe (`#00FF66`) with soft glowing faces (`0x3000FF66`) that elegantly frames containers over any other visual effects.

---

## 🎮 Controls

| Keybind | Default | Description |
| :--- | :---: | :--- |
| **Open Search GUI** | **`H`** | Opens the visual item selection browser with search bar. |
| **Clear / Close** | **`ESC`** | Clears the active search query and turns off ESP outlines. |

*Keybinds can be remapped in `Options -> Controls -> Key Binds -> Miscellaneous`.*

---

## 📁 Repository Structure

```text
ContainerSearchHighlighter/
├── build.gradle                                    # Loom 1.18.4, Fabric API 0.163.0+26.3
├── gradle.properties                               # Version 2.2, archives_base_name = ContainerSearch
├── settings.gradle                                 # rootProject.name = 'ContainerSearch'
├── LICENSE                                         # MIT License
└── src/
    └── main/
        ├── java/
        │   └── com/example/containersearch/
        │       ├── ContainerSearchClient.java      # Client entrypoint, keybinds & ESP render loop
        │       ├── ContainerTracker.java           # Container interaction listener & slot collector
        │       ├── ContainerCache.java             # Thread-safe persistent JSON cache
        │       ├── ItemInfo.java                   # Item data model & search match logic
        │       ├── RenderUtils.java                # Bounding box geometry (+0.02 expand) & Gizmo renderer
        │       ├── gui/
        │       │   ├── SearchScreen.java           # Search GUI with filter input & item list
        │       │   ├── ItemSelectionList.java      # Scrollable widget displaying item entries
        │       │   └── ItemEntry.java              # Entry renderer for item icon & localized title
        │       └── mixin/
        │           └── AbstractContainerScreenMixin.java # Slot update interception mixin
        └── resources/
            ├── fabric.mod.json                     # Mod metadata (id: "containersearch", version: "2.2")
            ├── containersearch.mixins.json         # Mixin configuration
            └── assets/containersearch/
                ├── icon.png                        # 128x128 mod icon
                └── lang/
                    ├── en_us.json                  # English localization
                    ├── uk_ua.json                  # Ukrainian localization
                    └── ru_ru.json                  # Russian localization
```

---

## 🛠 Building from Source

### Prerequisites
* JDK 21 or higher installed.

### Build
Run Gradle Wrapper in the project directory:

```bash
# Linux / macOS
./gradlew build

# Windows (PowerShell)
.\gradlew build
```

The compiled mod JAR will be located at:
```text
build/libs/ContainerSearch-2.2.jar
```

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — see the LICENSE file for details.
Copyright (c) 2026 [LymXT](https://github.com/LymXT).

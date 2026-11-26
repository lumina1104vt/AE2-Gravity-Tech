# AE2 : Gravity Tech

[繁體中文](#繁體中文)

A gravity-themed addon for [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2): black and white holes, a gravity processing factory, a fusion generator, upgradeable anti-gravity armor, and a twin-star crafting hub.

This branch is for **Minecraft 1.20.1 (Forge)**, version **20.0.0.1-beta** (beta). Mod id: `ae2_gvt`.

## Features

- **Black and white holes**: natural black holes to find, white holes to make, and three crystal lines grown from them.
- **Machines**: gravity processing factory, graviton collector, fusion generator, and generators that power the ME network.
- **Twin-Star Hub**: a hexagram crafting multiblock with its own planner, virtual patterns and pattern terminal.
- **Machine Access Interface**: let the hub feed machines of other mods and take their products back, with blocking modes, hatches and a machine usage screen.
- **Storage and energy**: black hole storage disk, white hole energy cell, FE storage cells.
- **Anti-gravity armor** with module cards.

The in-game guide (GuideME) explains each of these in English, Traditional Chinese and Simplified Chinese.

## Installation

Requires:

- Forge 47.4 or newer
- Applied Energistics 2 (15.4 or newer, below 16)
- GeckoLib
- GuideME

Optional: JEI or EMI (recipes, pattern buttons, bookmarks), Jade, Applied Flux (its FE and cells are used instead of ours).

The libraries of this project (TwinStar engine, Lumina multiblock and others) are bundled inside the mod jar.

Changes of each version are in [`changelogs/`](changelogs).

## Building

Needs JDK 17. `core/` and `lumina-multiblock/` are **private** git submodules, so building needs access to them.

```sh
git clone --recurse-submodules -b 1.20.1 https://github.com/lumina1104vt/AE2-Gravity-Tech.git
cd AE2-Gravity-Tech
./gradlew build
```

The jar is written to `build/libs/`. Run the game for development with `./gradlew runClient`.

| Folder | Contents |
|---|---|
| `src/` | the mod itself |
| `core/` | TwinStar: the crafting engine, its API and its AE2 side (submodule) |
| `lumina-multiblock/` | multiblock structure library (submodule) |
| `lumina-lib/` | shared base libraries |

## License

All Rights Reserved, see [`LICENSE`](LICENSE). `TEMPLATE_LICENSE.txt` covers only the mod template this project started from.

---

## 繁體中文

以重力為主題的 [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) 附屬模組：黑白洞、引力加工工廠、核融合發生器、可升級的反重力裝甲，以及雙星合成中樞。

這個分支適用 **Minecraft 1.20.1（Forge）**，版本 **20.0.0.1-beta**（測試版）。模組 ID：`ae2_gvt`。

### 內容

- **黑洞與白洞**：可以尋找天然黑洞、製作白洞，並從上面長出三條水晶線。
- **機器**：引力加工工廠、引力子收集器、核融合發生器，以及替 ME 網路供電的發電機。
- **雙星調度中樞**：六角星合成多方塊，有自己的規劃演算法、虛擬樣板和樣板終端。
- **機器接入接口**：讓中樞向其他模組的機器送料、收回產物，有阻擋模式、艙口和機器用量畫面。
- **儲存與能源**：黑洞存儲磁盤、白洞能源元件、FE 儲存元件。
- **反重力裝甲**與模組卡。

遊戲內導覽書（GuideME）有英文、繁體中文、簡體中文三種語言的說明。

### 安裝

必需：

- Forge 47.4 以上
- Applied Energistics 2（15.4 以上、16 以下）
- GeckoLib
- GuideME

選用：JEI 或 EMI（配方、樣板按鈕、蒐藏）、Jade、Applied Flux（改用它的 FE 和元件）。

本專案的函式庫（雙星引擎、Lumina 多方塊等）已包在模組 jar 裡。

各版本的更新內容在 [`changelogs/`](changelogs)。

### 建置

需要 JDK 17。`core/` 和 `lumina-multiblock/` 是**私有**的 git 子模組，要有存取權限才能建置。

```sh
git clone --recurse-submodules -b 1.20.1 https://github.com/lumina1104vt/AE2-Gravity-Tech.git
cd AE2-Gravity-Tech
./gradlew build
```

產出的 jar 在 `build/libs/`。開發時用 `./gradlew runClient` 啟動遊戲。

| 資料夾 | 內容 |
|---|---|
| `src/` | 模組本體 |
| `core/` | 雙星：合成引擎、API 和 AE2 端（子模組） |
| `lumina-multiblock/` | 多方塊結構函式庫（子模組） |
| `lumina-lib/` | 共用的基礎函式庫 |

### 授權

保留所有權利（All Rights Reserved），見 [`LICENSE`](LICENSE)。`TEMPLATE_LICENSE.txt` 只適用於本專案起始時使用的模組模板。

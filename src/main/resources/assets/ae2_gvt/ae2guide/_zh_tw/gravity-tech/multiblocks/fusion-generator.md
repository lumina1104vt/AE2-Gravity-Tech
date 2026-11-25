---
navigation:
  parent: ae2_gvt:gravity-tech/multiblocks-index.md
  title: 核融合發生器
---

# 核融合發生器

核融合發生器可以用來製作各種引力科技的物品，另外在執行部分配方時還可以發電。

<RecipeFor id="ae2_gvt:fusion_controller" />

<GameScene zoom="2" interactive={true}>
  <ImportStructure src="../assets/assemblies/fusion_controller.nbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 運作機制

核融合發生器與大部分機器不同，有一套獨特的運作機制。

核融合發生器能否執行配方，取決於發生器內的穩定溫度是否大於或等於配方所需的溫度。

小提示：每個配方所需的溫度，請透過 JEI、EMI 等模組查詢。

## 溫度

溫度是核融合發生器運作的關鍵機制。

核融合發生器可以在主方塊中調整每 tick 輸入的 FE，每 tick 輸入的 FE 越多，發生器內的溫度就越高。

以下是溫度與 FE 的換算公式：

溫度 ＝ 1500 ＋ 0.0005 × FE

小提示：優先使用的是 FE 儲存元件中的 FE，用完之後才會使用 ME 網路中儲存的能量。（如果安裝了應用通量模組，就會優先消耗該模組能源儲存元件當中的 FE。）

## 發電

你沒有看錯，核融合發生器還可以發電（執行黑洞存儲磁盤與白洞能源元件的配方時除外）。

核融合發生器在配方執行完成時，會產出輸入的電量，同時發電量還會受每種配方的能量倍率影響。

以下是發電量的公式：

發電量 ＝ 輸入電量 × 配方的能量倍率

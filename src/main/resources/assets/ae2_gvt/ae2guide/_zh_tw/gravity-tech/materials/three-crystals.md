---
navigation:
  parent: ae2_gvt:gravity-tech/materials-index.md
  title: 三種水晶
  position: 0
item_ids:
  - ae2_gvt:gravity_crystal
  - ae2_gvt:gravity_dust
  - ae2_gvt:gravity_block
  - ae2_gvt:gravity_cluster
  - ae2_gvt:antigravity_crystal
  - ae2_gvt:antigravity_dust
  - ae2_gvt:antigravity_block
  - ae2_gvt:antigravity_cluster
  - ae2_gvt:paradox_crystal
  - ae2_gvt:paradox_dust
  - ae2_gvt:paradox_block
  - ae2_gvt:paradox_cluster
  - ae2_gvt:small_gravity_bud
  - ae2_gvt:medium_gravity_bud
  - ae2_gvt:large_gravity_bud
  - ae2_gvt:small_antigravity_bud
  - ae2_gvt:medium_antigravity_bud
  - ae2_gvt:large_antigravity_bud
  - ae2_gvt:small_paradox_bud
  - ae2_gvt:medium_paradox_bud
  - ae2_gvt:large_paradox_bud
  - ae2_gvt:paradox_crystal_seed
  - ae2_gvt:almost_flawless_budding_paradox
  - ae2_gvt:flawed_budding_paradox
  - ae2_gvt:chipped_budding_paradox
  - ae2_gvt:damaged_budding_paradox
---

# 三種水晶

引力科技共有三種全新的水晶，分別是引力水晶、反引力水晶與悖論水晶，它們的獲取方式與用途各不相同。

## 總覽

<ItemGrid>
  <ItemIcon id="ae2_gvt:gravity_crystal" />
  <ItemIcon id="ae2_gvt:antigravity_crystal" />
  <ItemIcon id="ae2_gvt:paradox_crystal" />
</ItemGrid>

| 水晶 | 母岩 |
|---|---|
| 引力水晶 | 黑洞 |
| 反引力水晶 | 白洞 |
| 悖論水晶 | 悖論水晶母岩 |

## 引力水晶

引力水晶是引力科技中常見的水晶，以引力水晶芽的形式生長在黑洞上。

向黑洞投入任意物品（奇點除外），可以讓黑洞上的晶芽成長；每投一個物品，黑洞上的晶體就成長一次，直到晶芽長成晶簇，就可以挖掘晶簇獲得引力水晶。（注意：挖掉晶芽會掉落引力水晶粉。）

相關物品：

<Row>
<ItemImage id="ae2_gvt:small_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:medium_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:large_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:gravity_cluster" scale="3" /><ItemImage id="ae2_gvt:gravity_crystal" scale="3" /><ItemImage id="ae2_gvt:gravity_dust" scale="3" />
</Row>

加速生長：

催生器可以加快黑洞上晶芽的成長。在黑洞周圍放置催生器，就能提高水晶的生長效率。

## 反引力水晶

反引力水晶是引力科技中第二種常見的水晶，以反引力水晶芽的形式生長在白洞上。

向白洞投入任意物品，可以讓白洞上的晶芽成長；每投一個物品，白洞上的晶體就成長一次，直到晶芽長成晶簇，就可以挖掘晶簇獲得反引力水晶。（注意：挖掉晶芽會掉落反引力水晶粉。）

相關物品：

<Row>
<ItemImage id="ae2_gvt:small_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:medium_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:large_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:antigravity_cluster" scale="3" /><ItemImage id="ae2_gvt:antigravity_crystal" scale="3" /><ItemImage id="ae2_gvt:antigravity_dust" scale="3" />
</Row>

加速生長：

催生器可以加快白洞上晶芽的成長。在白洞周圍放置催生器，就能提高水晶的生長效率。

## 悖論水晶

悖論水晶是引力科技中最難獲得的水晶，必須借助黑洞與白洞才能培育。

擺法示意（白洞、福魯伊克斯水晶塊、黑洞並排）：

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/paradox_setup.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

在中間的福魯伊克斯水晶塊上使用悖論水晶種子，就會生成悖論母岩，破壞晶簇即可獲得悖論水晶。（注意：挖掉晶芽會掉落悖論水晶粉。）

<RecipeFor id="ae2_gvt:paradox_crystal_seed" />

悖論母岩與恩特羅母岩類似，會隨著水晶成長而衰減，最終變回福魯伊克斯水晶塊。

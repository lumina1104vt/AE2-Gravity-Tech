---
navigation:
  parent: ae2_gvt:gravity-tech/materials-index.md
  title: 三种水晶
  position: 0
---

# 三种水晶

引力科技共有三种全新的水晶，分别是引力力水晶、反引力力水晶与悖论水晶，它们的获取方式与用途各不相同。

## 总览

<ItemGrid>
  <ItemIcon id="ae2_gvt:gravity_crystal" />
  <ItemIcon id="ae2_gvt:antigravity_crystal" />
  <ItemIcon id="ae2_gvt:paradox_crystal" />
</ItemGrid>

| 水晶 | 母岩 |
|---|---|
| 引力水晶 | 黑洞 |
| 反引力水晶 | 白洞 |
| 悖论水晶 | 悖论水晶母岩 |

## 引力水晶

引力水晶是引力科技中常见的水晶，以引力水晶芽的形式生长在黑洞上。

向黑洞投入任意物品（除了奇点），可以让黑洞上的晶芽成长；每投一个物品可以让黑洞上的晶体成长一次，直到黑洞上的水晶芽长成晶体簇，就可以挖掘晶簇获得引力水晶。（注意：挖掉晶芽会掉落引力水晶粉。）

相关物品：

<Row>
<ItemImage id="ae2_gvt:small_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:medium_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:large_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:gravity_cluster" scale="3" /><ItemImage id="ae2_gvt:gravity_crystal" scale="3" /><ItemImage id="ae2_gvt:gravity_dust" scale="3" />
</Row>

加速生长：

催生器可以加快黑洞上水晶芽的成长。可在黑洞周围放置催生器来提高水晶生长效率。

## 反引力水晶

反引力水晶是引力科技中第二种常见的水晶，以反引力水晶芽的形式生长在白洞上。

向白洞投入任意物品，可以让白洞上的晶芽成长；每投一个物品可以让白洞上的晶体成长一次，直到白洞上的水晶芽长成晶体簇，就可以挖掘晶簇获得反引力水晶。（注意：挖掉晶芽会掉落反引力水晶粉。）

相关物品：

<Row>
<ItemImage id="ae2_gvt:small_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:medium_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:large_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:antigravity_cluster" scale="3" /><ItemImage id="ae2_gvt:antigravity_crystal" scale="3" /><ItemImage id="ae2_gvt:antigravity_dust" scale="3" />
</Row>

加速生长：

催生器可以加快白洞上水晶芽的成长。可在白洞周围放置催生器来提高水晶生长效率。

## 悖论水晶

悖论水晶是引力科技中最难获取的水晶，需要借助黑洞与白洞才能培育。

摆法示意（白洞 · 福鲁伊克斯水晶块 · 黑洞并排）：

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/paradox_setup.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

在中间的福鲁伊克斯水晶块上使用悖论水晶种子，即可生成悖论母岩，破坏晶簇即可获得悖论水晶（注意：挖掉晶芽会悖论水晶粉。）。

<RecipeFor id="ae2_gvt:paradox_crystal_seed" />

悖论母岩与恩特罗母岩类似会随着水晶成长而衰减，最终变回福鲁伊克斯块。





---
navigation:
  parent: ae2_gvt:gravity-tech/multiblocks-index.md
  title: 核聚变发生器
item_ids:
  - ae2_gvt:fusion_controller
---

# 核聚变发生器

核聚变发生器可以用来制作各种引力科技的物品，另外在运行部分配方可以发电。

<RecipeFor id="ae2_gvt:fusion_controller" />

<GameScene zoom="2" interactive={true}>
  <ImportStructure src="../assets/assemblies/fusion_controller.nbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## 运行机制

核聚变发生器与大部分机器不同，有着一套独特的运行机制。

核聚变发生器运行配方取决于发生器内的稳定温度是否大于等于配方运行所需的温度。

小提示：每个配方的运行所需的温度请通过JEI，EMI等模组查询。

## 温度

温度是核聚变发生器运行的关键机制。

核聚变发生器可在主方块中调整每t输入的FE，每t输入的FE越多发生器中的温度就越高。

以下为温度与FE的换算公式：

温度=1500+0.0005*FE

小提示：优先使用的是FE储存元件中的FE，消耗完后才会使用ME网络中存储的能量。（如果安装了应用通量模组，那么就会优先消耗该模组的能源存储元件当中的FE。）

## 发电

你没有看错，核聚变发生器还可以发电（运行黑洞存储磁盘和白洞能源元件配方时除外）。

核聚变发生器在配方运行完成时，会产出输入的电量，同时发电量还会受每种配方的能量倍率影响。

以下是发电量的公式：

发电量=输入电量*配方的能量倍率

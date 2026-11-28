---
navigation:
  parent: ae2_gvt:gravity-tech/machines.md
  title: "机器连接器"
item_ids:
  - ae2_gvt:machine_link_tool
---

# 机器连接器

<Row>
<ItemImage id="ae2_gvt:machine_link_tool" scale="4" />
<RecipeFor id="ae2_gvt:machine_link_tool" />
</Row>

让机器接入接口（线缆上的贴片，或<ItemLink id="ae2_gvt:machine_access_block" />）从远处代表机器。

## 使用方法

| 操作 | 效果 |
| :--- | :--- |
| 潜行 + 右键接入接口 | 选取它 |
| 右键机器 | 连接你点的那一面；再右键同一台解除 |
| Ctrl + 右键机器 | 一次连接周围所有相同的机器 |
| 潜行 + 右键空气 | 取消选取 |

Ctrl 连接的范围是以点到的机器为中心、半径 16 格，一次最多 64 台（配置文件的 `linkAllRadius`、`linkAllMax`），只会增加不会解除。ME 接口模式仍然最多 9 台。

连接器的右键优先于机器本身，点到有界面的机器也只会连接。拿着它时，选取的接口和被连接的面会发光，并有细线相连。

小提示：机器要在同维度、64 格内且区块已加载。红石脉冲不需要方块，机器会直接收到信号，周围不用留空位。

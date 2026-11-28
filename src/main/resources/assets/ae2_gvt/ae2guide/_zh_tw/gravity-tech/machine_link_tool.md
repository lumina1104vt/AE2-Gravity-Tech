---
navigation:
  parent: ae2_gvt:gravity-tech/machines.md
  title: "機器連結器"
item_ids:
  - ae2_gvt:machine_link_tool
---

# 機器連結器

<Row>
<ItemImage id="ae2_gvt:machine_link_tool" scale="4" />
<RecipeFor id="ae2_gvt:machine_link_tool" />
</Row>

讓機器接入接口（線纜上的貼片，或<ItemLink id="ae2_gvt:machine_access_block" />）從遠端代表機器。

## 使用方法

| 操作 | 效果 |
| :--- | :--- |
| 潛行 + 右鍵接入接口 | 選取它 |
| 右鍵機器 | 連接你點的那一面；再右鍵同一台解除 |
| Ctrl + 右鍵機器 | 一次連接周圍所有相同的機器 |
| 潛行 + 右鍵空氣 | 取消選取 |

Ctrl 連接的範圍是以點到的機器為中心、半徑 16 格，一次最多 64 台（設定檔的 `linkAllRadius`、`linkAllMax`），只會增加不會解除。ME 接口模式仍然最多 9 台。

連結器的右鍵優先於機器本身，點到有畫面的機器也只會連接。拿著它時，選取的接口和被連接的面會發光，並有細線相連。

小提示：機器要在同維度、64 格內且區塊有載入。紅石脈衝不需要方塊，機器會直接收到訊號，周圍不用留空位。

---
navigation:
  parent: ae2_gvt:gravity-tech/machines.md
  title: "Machine Linker"
item_ids:
  - ae2_gvt:machine_link_tool
---

# Machine Linker

<Row>
<ItemImage id="ae2_gvt:machine_link_tool" scale="4" />
<RecipeFor id="ae2_gvt:machine_link_tool" />
</Row>

Lets a machine access interface (the cable part or the <ItemLink id="ae2_gvt:machine_access_block" />) stand for machines at a distance.

## How to use

| Action | Result |
| :--- | :--- |
| Sneak + right click an access interface | Select it |
| Right click a machine | Link the face you clicked; right click the same machine again to unlink |
| Ctrl + right click a machine | Link every identical machine around it at once |
| Sneak + right click the air | Clear the selection |

Ctrl linking covers a radius of 16 blocks around the one clicked, 64 machines at most (`linkAllRadius` and `linkAllMax` in the config). It only adds links. ME interface mode still stops at 9.

The linker wins over a machine's own right click, so machines with screens are only linked. While held, the selected interface and the linked faces glow, joined by thin lines.

Tip: machines must be in the same dimension, within 64 blocks and loaded. The redstone pulse needs no block: the machine simply receives a signal, so leave no space for it.

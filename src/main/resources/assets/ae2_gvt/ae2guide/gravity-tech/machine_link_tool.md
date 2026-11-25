---
navigation:
  parent: ae2_gvt:gravity-tech/machines.md
  title: Machine Linker
item_ids:
  - ae2_gvt:machine_link_tool
---

# Machine Linker

Lets a Machine Access Interface (the one on a cable, or the single block <ItemLink id="ae2_gvt:machine_access_block" />) stand for a machine from a distance.

## How to use

1. Sneak and right-click a Machine Access Interface to select it.
2. Right-click a machine to link the face you clicked to this interface; right-click the same one again to unlink. An interface can link many machines (no limit in provider mode, 9 in ME interface mode).
3. Sneak and right-click the air to cancel the selection.

**Ctrl + right-click a machine**: links every identical machine around it (the same block) at once. For example, with a row of 12 grinders, Ctrl + right-click one of them to link all 12. By default the range is a sphere of radius 16 blocks around the machine you clicked, at most 64 machines at a time; both numbers can be changed in the config (`linkAllRadius`, `linkAllMax`). Machines that are already linked are skipped, and you are told how many were left out. Ctrl + right-click only adds, it never unlinks; a normal right-click still links just one machine. The limit of 9 machines in ME interface mode still applies.

The linker's right-click has priority over the machine's own, so clicking a machine that has a screen only links it. While you hold it, the selected interface glows, the faces of the linked machines glow too, and thin lines connect them.

The machine must be in the same dimension, within 64 blocks, with its chunk loaded. The interface's redstone pulse uses no block: the machine receives a signal as if from a neighbour, so no free space is needed around the machine.

## Crafting

- Machine: Gravity Processing Factory
- Materials:
  - Memory Card ×1
  - Wireless Receiver ×1
  - Anti-gravity Circuit Board ×2

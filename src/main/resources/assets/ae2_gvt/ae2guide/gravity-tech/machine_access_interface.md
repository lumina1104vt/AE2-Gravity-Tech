---
navigation:
  parent: ae2_gvt:gravity-tech/machines.md
  title: Machine Access Interface
item_ids:
  - ae2_gvt:machine_access_interface
---

# Machine Access Interface

An AE2 cable part that attaches to a machine and connects it to the ME network. It uses no channel. (Its block version is <ItemLink id="ae2_gvt:machine_access_block" />, which can bind machines from a distance.)

An interface has two modes, switched at the top right:

- **Provider**: the patterns of the Twin-Star Hub use it to send materials into the machine and take the products back. Every machine has its own blocking mode, insert mode, matching number, sub-block range and redstone pulse.
- **ME Interface**: like an ME Interface, you set the items and amounts to keep for each machine and the network supplies them; the row below shows how many the network has. The hub does not use machines on an interface in this mode.

## The screen

- The top row shows the machines it stands for, one cell each, with an "optional machine" slot under each. Click to select, double-click to highlight it in the world. Switch pages when there are many machines.
- **Apply to all identical machines**: in provider mode, the left toolbar has a button of two overlapping squares (shown only when the list has another machine of the same kind); the number is how many identical machines are in the list. For example, with 12 grinders connected, set the optional machine on one of them and press it: the other 11 are set the same (an empty slot is copied too). "Identical" means the same block; auxiliary hatches are not machines and are not changed.
- Below is the content of the selected machine. In ME interface mode, the gear above each setting cell: left-click sets the amount, Shift + left-click switches the lower row to the amount in the AE network.
- The largest amount you can type is 2147483647.

## Crafting

- Crafting table (shapeless): <ItemLink id="ae2_gvt:machine_access_block" />, one for one. The block and the part can be swapped back, like the ME Interface.

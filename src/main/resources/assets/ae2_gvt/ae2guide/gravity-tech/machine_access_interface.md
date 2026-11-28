---
navigation:
  parent: ae2_gvt:gravity-tech/machines.md
  title: "Machine Access Interface"
item_ids:
  - ae2_gvt:machine_access_interface
---

# Machine Access Interface

<Row>
<ItemImage id="ae2_gvt:machine_access_interface" scale="4" />
<RecipeFor id="ae2_gvt:machine_access_interface" />
</Row>

An AE2 cable part put on a machine to join it to the ME network. It uses no channel. Its block form is the <ItemLink id="ae2_gvt:machine_access_block" />, which binds machines from a distance.

## Two modes

Switched at the top right:

| Mode | What it does |
| :--- | :--- |
| **Provider** | The twin-star hub's patterns use it to send materials into the machine and take products back. Each machine has its own blocking mode, insertion method, pairing number, sub-block range and redstone pulse |
| **ME interface** | Like an ME interface: set the items and amounts each machine should hold and the network keeps them topped up. The hub does not use these machines |

## Screen

- The top row is the machines it stands for, one slot each, with a "machine (optional)" field below. Click to select, double click to highlight it in the world. Pages turn when there are many.
- **Apply to all identical machines**: in provider mode, a button on the left toolbar (two overlapping squares, shown when the list has another identical machine). With 12 crushers linked, set one up and press it: the other 11 get the same settings, empty fields included. "Identical" means the same block; hatches are not changed.
- Below are the settings of the selected machine. In ME interface mode, the gear above a slot: left click sets the amount, Shift + left click shows the amounts in the network instead.
- The largest amount that can be typed is 2147483647.

Tip: the part and the block turn into each other on a crafting table, one for one.

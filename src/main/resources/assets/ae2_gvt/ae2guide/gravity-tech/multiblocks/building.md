---
navigation:
  parent: ae2_gvt:gravity-tech/multiblocks-index.md
  title: Building Multiblocks
  position: 3
item_ids:
  - lumina_multiblock:builder
---

# Building Multiblocks

The large structures of Gravity Tech (for example the [Twin-Star Hub](twin-star-hub.md), the [Fusion Generator](fusion-generator.md) and the [Space Elevator](space-elevator.md)) can be built automatically with the **Multiblock Builder**. It can also take a whole structure down with one click, and take materials straight from the ME network (bind the ME network first).

## Getting the builder

<RecipeFor id="lumina_multiblock:builder" />

## How to use

First put the structure's **controller** in the right place (the builder generates the whole structure around the controller).

| Action | Effect |
| :--- | :--- |
| Sneak + right-click the controller | Start building the whole structure |
| Sneak + right-click the air | Open / switch the builder mode |
| Right-click the air | Stop the current build |

The builder places the structure block by block and tells you at once what is missing (`Missing: %s`), which blocks are in the way (`%s blocks blocked: %s`), and so on.

## Builder modes

In the "Builder Mode" screen you can combine these:

- **Dismantle**: take the whole structure down.
- **Mirror**: build mirrored (some structures do not support it; you are told "This structure cannot be built mirrored").
- **No hatches**: do not place hatch / interface blocks while building.

## Taking materials from the ME network

- Shift + right-click binds the builder to an ME network; its item information then shows "Bound to the AE network of …", and it can take blocks straight from the network.
- When the ME network cannot be reached you are told "Cannot reach the bound ME network, using the inventory only", and only the materials in the player's inventory are used.

## Commands (permission needed)

| Command | Effect |
| :--- | :--- |
| `/lmb list` | List the multiblock structures in the current dimension |
| `/lmb info <controller>` | Show the structure information, facing, parameters and the reason it is not formed for the given controller |
| `/lmb form <controller>` | Make the given controller form its structure directly (for debugging / quick repair) |

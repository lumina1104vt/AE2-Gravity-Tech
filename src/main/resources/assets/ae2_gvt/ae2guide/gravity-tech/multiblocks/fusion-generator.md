---
navigation:
  parent: ae2_gvt:gravity-tech/multiblocks-index.md
  title: Fusion Generator
---

# Fusion Generator

The Fusion Generator is used to craft all kinds of Gravity Tech items, and some of its recipes generate power as they run.

<RecipeFor id="ae2_gvt:fusion_controller" />

<GameScene zoom="2" interactive={true}>
  <ImportStructure src="../assets/assemblies/fusion_controller.nbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

## How it works

The Fusion Generator works differently from most machines.

Whether it can run a recipe depends on whether the stable temperature inside the generator is greater than or equal to the temperature the recipe needs.

Tip: look up the temperature each recipe needs in JEI, EMI or a similar mod.

## Temperature

Temperature is the key mechanic of the Fusion Generator.

On the main block you can set the FE input per tick. The more FE per tick, the higher the temperature inside the generator.

The conversion between temperature and FE:

Temperature = 1500 + 0.0005 × FE

Tip: the FE in FE storage cells is used first, and only when it runs out does it use the energy stored in the ME network. (If Applied Flux is installed, the FE in that mod's energy storage cells is used first.)

## Power generation

Yes, the Fusion Generator can also generate power (except when running the recipes of the Black Hole Storage Disk and the White Hole Energy Cell).

When a recipe finishes, the Fusion Generator outputs the energy that was put in, and the amount is also affected by each recipe's energy multiplier.

The formula for the generated power:

Generated power = input energy × the recipe's energy multiplier

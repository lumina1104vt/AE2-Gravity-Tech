---
navigation:
  parent: ae2_gvt:gravity-tech/materials-index.md
  title: The Three Crystals
  position: 0
item_ids:
  - ae2_gvt:gravity_crystal
  - ae2_gvt:gravity_dust
  - ae2_gvt:gravity_block
  - ae2_gvt:gravity_cluster
  - ae2_gvt:antigravity_crystal
  - ae2_gvt:antigravity_dust
  - ae2_gvt:antigravity_block
  - ae2_gvt:antigravity_cluster
  - ae2_gvt:paradox_crystal
  - ae2_gvt:paradox_dust
  - ae2_gvt:paradox_block
  - ae2_gvt:paradox_cluster
  - ae2_gvt:small_gravity_bud
  - ae2_gvt:medium_gravity_bud
  - ae2_gvt:large_gravity_bud
  - ae2_gvt:small_antigravity_bud
  - ae2_gvt:medium_antigravity_bud
  - ae2_gvt:large_antigravity_bud
  - ae2_gvt:small_paradox_bud
  - ae2_gvt:medium_paradox_bud
  - ae2_gvt:large_paradox_bud
  - ae2_gvt:paradox_crystal_seed
  - ae2_gvt:almost_flawless_budding_paradox
  - ae2_gvt:flawed_budding_paradox
  - ae2_gvt:chipped_budding_paradox
  - ae2_gvt:damaged_budding_paradox
---

# The Three Crystals

Gravity Tech has three brand-new crystals: the gravity crystal, the anti-gravity crystal and the paradox crystal. Each is obtained in a different way and used for something different.

## Overview

<ItemGrid>
  <ItemIcon id="ae2_gvt:gravity_crystal" />
  <ItemIcon id="ae2_gvt:antigravity_crystal" />
  <ItemIcon id="ae2_gvt:paradox_crystal" />
</ItemGrid>

| Crystal | Host rock |
|---|---|
| Gravity crystal | Black hole |
| Anti-gravity crystal | White hole |
| Paradox crystal | Paradox crystal host rock |

## Gravity crystal

The gravity crystal is the common crystal of Gravity Tech. It grows on a black hole as a gravity crystal bud.

Throw any item (except a singularity) into the black hole to make its buds grow; each item makes the crystal on the black hole grow once, until the bud has become a cluster, and you can mine the cluster to get gravity crystals. (Note: mining a bud drops gravity crystal dust.)

Related items:

<Row>
<ItemImage id="ae2_gvt:small_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:medium_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:large_gravity_bud" scale="3" /><ItemImage id="ae2_gvt:gravity_cluster" scale="3" /><ItemImage id="ae2_gvt:gravity_crystal" scale="3" /><ItemImage id="ae2_gvt:gravity_dust" scale="3" />
</Row>

Faster growth:

A Growth Accelerator speeds up the growth of the buds on a black hole. Place accelerators around the black hole to make crystals grow more efficiently.

## Anti-gravity crystal

The anti-gravity crystal is the second common crystal of Gravity Tech. It grows on a white hole as an anti-gravity crystal bud.

Throw any item into the white hole to make its buds grow; each item makes the crystal on the white hole grow once, until the bud has become a cluster, and you can mine the cluster to get anti-gravity crystals. (Note: mining a bud drops anti-gravity crystal dust.)

Related items:

<Row>
<ItemImage id="ae2_gvt:small_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:medium_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:large_antigravity_bud" scale="3" /><ItemImage id="ae2_gvt:antigravity_cluster" scale="3" /><ItemImage id="ae2_gvt:antigravity_crystal" scale="3" /><ItemImage id="ae2_gvt:antigravity_dust" scale="3" />
</Row>

Faster growth:

A Growth Accelerator speeds up the growth of the buds on a white hole. Place accelerators around the white hole to make crystals grow more efficiently.

## Paradox crystal

The paradox crystal is the hardest crystal to get in Gravity Tech. It can only be grown with the help of a black hole and a white hole.

The layout (white hole, fluix block, black hole side by side):

<GameScene zoom="4" interactive={true}>
  <ImportStructure src="../assets/assemblies/paradox_setup.snbt" />
  <IsometricCamera yaw="195" pitch="30" />
</GameScene>

Use a paradox crystal seed on the fluix block in the middle to create paradox host rock; break the cluster to get paradox crystals. (Note: mining a bud drops paradox crystal dust.)

<RecipeFor id="ae2_gvt:paradox_crystal_seed" />

Like a budding quartz block, the paradox host rock decays as the crystal grows, and finally turns back into a fluix block.

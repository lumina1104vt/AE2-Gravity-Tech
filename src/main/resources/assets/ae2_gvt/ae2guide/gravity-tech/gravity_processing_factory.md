---
navigation:
  parent: ae2_gvt:gravity-tech/machines.md
  title: "Gravity Processing Factory"
item_ids:
  - ae2_gvt:gravity_processing_factory
---

# Gravity Processing Factory

<Row>
<BlockImage id="ae2_gvt:gravity_processing_factory" scale="4" />
<RecipeFor id="ae2_gvt:gravity_processing_factory" />
</Row>

A single block machine for general processing. Module cards, system parts, circuits, alloys and hub units are made here.

## How to use

1. Give it power (FE, from any side). Recipes that need gravitons also need it cabled to an ME network that holds them.
2. Put the materials into the 12 input slots; products appear in the 12 output slots. Pipes and hoppers work from any side.
3. A slot holds up to 262,144 items, or as many buckets of a fluid.

## Speed and parallel

| What you add | Effect |
| :--- | :--- |
| AE2 acceleration card (up to 4) | Faster, 5× with all four; the energy per recipe stays the same |
| Chaos matter in the catalyst slot (up to 16) | Each one doubles how many copies run at once; it is not used up |

Tip: a recipe waits on its last tick until the network has the gravitons it needs.

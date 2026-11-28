# Modpack notes / 整合包說明 (1.21.1)

Everything below is datapack JSON; KubeJS can add, change and remove it.
以下全部是資料包 JSON，可以用 KubeJS 新增、修改、移除。

## Gravity Factory recipes / 引力加工工廠配方

`ae2_gvt:gravity_processing`. Items and fluids; `time` in ticks, `energy` in FE for one run;
`gravitons` / `antigravitons` are taken from the ME network. `catalysts` (ingredients) must be in the
factory but are not used up; `network_costs` (AE2 stacks) are taken from the network too.
物品與流體都可以；`time` 是 tick、`energy` 是一次配方的 FE；引力子和反引力子從 ME 網路扣。`catalysts` 是要放著但不消耗的原料；`network_costs` 是另外從網路扣的東西。

```js
ServerEvents.recipes(event => {
  event.custom({
    "type": "ae2_gvt:gravity_processing",
    "ingredients": [{"item": "minecraft:cobblestone", "count": 64}],
    "fluid_ingredients": [{"fluid": "minecraft:lava", "amount": 1000}],
    "results": [{"id": "minecraft:obsidian", "count": 4}],
    "fluid_results": [{"id": "minecraft:water", "amount": 250}],
    "time": 100,
    "energy": 5000,
    "gravitons": 1
  }).id('kubejs:factory_obsidian')
})
```

## Other mods' recipes in the factory / 工廠借用其他模組的配方

`ae2_gvt:factory_borrow` makes the factory run every recipe of one kind. Built in:
一條 `ae2_gvt:factory_borrow` 讓工廠能跑某一種配方的全部內容。內建：

- AE2: `ae2_inscriber_*`, `ae2_charger`
- ExtendedAE: `extendedae_crystal_assembler`, `extendedae_circuit_cutter`
- AdvancedAE: `advanced_ae_reaction`
- AE2 Crystal Science: `ae2cs_crystal_aggregator`, `ae2cs_circuit_etcher`, `ae2cs_crystal_pulverizer`
- Neo ECO AE: `neoecoae_integrated_working_station`
- Data Energistics: `data_energistics_*`
- AE2 Lightning Tech: `ae2lt_lightning_assembly`, `ae2lt_lightning_simulation`, `ae2lt_overload_processing`, `ae2lt_crystal_catalyzer`, `ae2lt_crystal_catalyzer_dust`

A field can be a path, such as `inputs.items`. / 欄位可以寫路徑，例如 `inputs.items`。

| field 欄位 | meaning 意思 |
|---|---|
| `from` | the other recipe's `type` / 別人配方 JSON 裡的 `type` |
| `items`, `fluids` | which fields of that JSON are materials / 哪些欄位是材料 |
| `outputs`, `fluid_outputs` | which fields are results / 哪些欄位是產物 |
| `energy_field`, `energy_scale` | read the energy from this field and multiply / 從這個欄位讀耗能再乘倍率 |
| `energy` | FE when the recipe has no such field / 原配方沒有耗能時用的 FE |
| `time` | ticks per run / 每次配方的 tick |
| `fe_per_graviton` | one graviton for every this much FE, or part of it; default 1000, 0 for none / 每多少 FE 收 1 個引力子（不足也算 1），預設 1000，0 為不收 |
| `gravitons`, `antigravitons` | a fixed number per run instead / 改成每次固定數量 |
| `when`, `has`, `unless` | only recipes whose field has this value / has these fields / has none of these / 只借欄位等於某值、有某欄位、沒有某欄位的配方 |
| `keep` | fields that are materials but not used up / 要放著但不消耗的欄位 |
| `needs` | extra items that must be in the factory, not used up / 工廠裡要另外放著的物品（不消耗） |
| `alternatives` | each field of `items` / `keep` is one ingredient (a list is a choice) / 每個欄位就是一個原料（陣列代表任選其一） |
| `extra_fluids` | fluids the other machine takes without listing them / 原機器會用、但配方沒寫的流體 |
| `network` | other things taken from the ME network (lightning, data flow) / 另外從 ME 網路扣的東西（閃電、數據流） |

Recipes whose result is an AE2 Gravity Tech item, or that need more than 1,000,000,000 FE, are not borrowed.
產物是本模組物品、或耗能超過 10 億 FE 的配方不會被借用。

```js
ServerEvents.recipes(event => {
  // turn one off / 關掉其中一種
  event.remove({ id: 'ae2_gvt:factory_borrow/advanced_ae_reaction' })

  // charge more gravitons / 提高引力子消耗
  event.remove({ id: 'ae2_gvt:factory_borrow/extendedae_circuit_cutter' })
  event.custom({
    type: 'ae2_gvt:factory_borrow',
    from: 'extendedae:circuit_cutter',
    items: ['input'],
    outputs: ['output'],
    energy: 2000,
    gravitons: 8   // instead of 2 / 原本是 2
  }).id('kubejs:borrow_circuit_cutter')
})
```

---
navigation:
  parent: ae2_gvt:index.md
  title: Quantum Controller
  position: 4
item_ids:
  - ae2_gvt:quantum_controller
---

# Quantum Controller

The Quantum Controller is the quantum version of AE2's **ME Controller**: it is used in exactly the same way, forms the same controller structures, accepts FE directly, and can join several networks into one **across dimensions and over long distances**, sharing the channels of the whole network.

<BlockImage id="ae2_gvt:quantum_controller" scale="4" />

## The screen

The main screen opened by a right-click is the same as the ME Controller's and shows the network status; the top right also shows the **remaining channels** (before it joins a network it may read "Channels: unlimited"). The gear on the left opens the **Quantum Controller settings** page, where you set the role and the frequency; the arrow at the top left of the settings page goes back to the main screen.

## Roles

A Quantum Controller has three roles, switched on the settings page:

| Role | Description |
| :--- | :--- |
| **Disabled** | Used as an ordinary ME Controller |
| **Master** | The body of the network of this frequency; **there can be only one for a frequency** |
| **Slave** | Connects to the master of the same frequency and uses the channels of the whole network |

- **Master**: the centre of the whole frequency network; it provides the channels for the network.
- **Slave**: a slave of the same frequency connects to the master and the networks of both sides become one, **however far apart and whichever dimension they are in**, as long as the chunks on both sides are loaded. The cable on the slave's side gets its channels from the slave, but the channels actually used are those of the whole network.
- **Disabled**: just an ordinary ME Controller.

AE2 does not treat a slave as another, separate controller structure, so it **does not trigger AE2's controller conflict**. A slave that is not connected to a master is, on its side, a "network without a controller".

## Frequency

- You name the frequency yourself, in the input field at the top of the settings page.
- **Controllers with the same name connect to each other.**
- The lower part of the settings page lists the frequencies that **loaded** controllers are using, with the number of masters and slaves beside each; click one to use that frequency.
- "Clear frequency" cancels the current frequency setting.

### Status messages

The settings page shows the current link status:

| Message | Meaning |
| :--- | :--- |
| Not linked | No role enabled, or no frequency set |
| Master: %s slaves linked | Working normally, with some slaves attached |
| Linked to the master, using the channels of the whole network | The slave has connected |
| No master found for this frequency | This frequency has no master yet (or it is not loaded) |
| Several masters for this frequency | A frequency conflict; keep only one master |
| Link failed, waiting for the other side to load | The chunk on the far side is not loaded yet |
| No frequency set yet | Name a frequency first |

## Tips

1. Build the controller structure as you would for an ME Controller (it can also be mixed with an existing ME Controller structure).
2. Choose the roles on the settings page: one **Master**, and **Slave** everywhere else.
3. Give them the **same frequency name**.
4. Make sure the chunks on both ends are loaded, and the controllers across dimensions join the same network.

> Tip: since a slave does not count towards AE2's controller conflict, you can use "one master + several slaves" to unify the networks of bases scattered around into one channel pool.

## Crafting

<RecipeFor id="ae2_gvt:quantum_controller" />

## Related chapters

* [Black Holes and White Holes](holes.md)
* [Multiblock Structures](multiblocks-index.md)
* [Black Hole Storage Disk and White Hole Energy Cell](storage-and-energy.md)

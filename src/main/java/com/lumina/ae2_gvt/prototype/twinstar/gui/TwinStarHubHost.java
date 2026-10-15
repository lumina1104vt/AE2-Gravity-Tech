/*
 * PROTOTYPE - Twin-Star Hub GUI, hosted by the prototype controller. Delete with the prototype package.
 */
package com.lumina.ae2_gvt.prototype.twinstar.gui;

import appeng.api.inventories.InternalInventory;

/** What the hub GUI needs from whatever the hub's controller is (currently the prototype block entity). */
public interface TwinStarHubHost {
    /** How many patterns the hub holds; will depend on the black-hole units once those exist. */
    int PATTERN_SLOTS = 72;

    boolean isHubFormed();

    /** The patterns stored in the hub. They live inside the hub; the GUI shows them by their output. */
    InternalInventory getHubPatterns();
}

package com.lumina.ae2_gvt.quantum;

/** What the quantum controller adds to AE2's network status screen (the menu implements it). */
public interface QuantumStatusAccess {
    /** channels left: a count, or QuantumChannels.INFINITE; UNKNOWN when this is not a quantum controller's screen */
    int ae2_gvt$channelsLeft();

    /** asks for the quantum controller's settings page (client: sends the action, server: opens it) */
    void ae2_gvt$openSettings();
}

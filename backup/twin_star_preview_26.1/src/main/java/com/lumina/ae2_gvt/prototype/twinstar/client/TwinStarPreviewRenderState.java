package com.lumina.ae2_gvt.prototype.twinstar.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** PROTOTYPE - one frame's worth of animation values, extracted from the preview block entity. */
public class TwinStarPreviewRenderState extends BlockEntityRenderState {
    boolean formed;         // structure complete - nothing is drawn otherwise
    float seconds;          // continuous time, for scrolling patterns
    double sinceFormed;     // ticks since the formation replay started (huge = fully formed)
    float activity;
    float outerAngle, middleAngle, innerAngle;
    float axisX, axisY, axisZ;  // twin holes: white -> black direction
    float twinClock;        // seconds into the twin holes' merge cycle
    float sphereX, sphereY, sphereZ, shell;   // sphere centre from the master cell, unit-shell radius
}
